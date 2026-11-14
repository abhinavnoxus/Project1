package com.nexora.app.design

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.interfaceStore: DataStore<Preferences> by preferencesDataStore("nexora_interface")

/**
 * Persistence for interface profiles.
 *
 * Profiles are stored as a JSON array under a single key so an export is exactly the
 * bytes we persist — no bespoke export path that can drift from the storage format.
 */
class InterfaceRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val profilesKey = stringPreferencesKey("profiles_json")
    private val activeIdKey = stringPreferencesKey("active_profile_id")
    private val qualityKey = stringPreferencesKey("quality_override")
    private val onboardedKey = booleanPreferencesKey("onboarded")
    private val lastBackupKey = floatPreferencesKey("last_backup_at")

    private fun Preferences.decode(): ProfileSet {
        val raw = this[profilesKey]
        val stored = if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { json.decodeFromString<List<InterfaceProfile>>(raw) }.getOrNull().orEmpty()
        }
        val merged = builtInProfiles() + stored.filter { s -> builtInProfiles().none { it.id == s.id } }
        val activeId = this[activeIdKey]
        return ProfileSet(
            profiles = merged,
            activeId = activeId?.takeIf { id -> merged.any { it.id == id } } ?: merged.first().id,
            qualityOverride = GlassQuality.fromId(this[qualityKey]),
            onboarded = this[onboardedKey] ?: false,
        )
    }

    private fun MutablePreferences.encode(set: ProfileSet) {
        val custom = set.profiles.filterNot { p ->
            builtInProfiles().any { it.id == p.id } && p == builtInProfiles().first { it.id == p.id }
        }
        this[profilesKey] = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(InterfaceProfile.serializer()), custom)
        this[activeIdKey] = set.activeId
        set.qualityOverride?.let { this[qualityKey] = it.id } ?: remove(qualityKey)
        this[onboardedKey] = set.onboarded
    }

    val profileSet: Flow<ProfileSet> = context.interfaceStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.decode() }

    suspend fun current(): ProfileSet = profileSet.first()

    suspend fun update(block: (ProfileSet) -> ProfileSet) {
        context.interfaceStore.edit { prefs ->
            val next = block(prefs.decode())
            prefs.encode(next)
        }
    }

    suspend fun saveProfile(profile: InterfaceProfile) = update { set ->
        val existing = set.profiles.indexOfFirst { it.id == profile.id }
        val updated = if (existing >= 0) {
            set.profiles.toMutableList().also { it[existing] = profile }
        } else {
            set.profiles + profile
        }
        set.copy(profiles = updated)
    }

    suspend fun duplicate(profileId: String, newName: String): InterfaceProfile? {
        var created: InterfaceProfile? = null
        update { set ->
            val source = set.profiles.firstOrNull { it.id == profileId } ?: return@update set
            val copy = source.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = newName,
                builtIn = false,
                createdAt = System.currentTimeMillis(),
            )
            created = copy
            set.copy(profiles = set.profiles + copy, activeId = copy.id)
        }
        return created
    }

    suspend fun delete(profileId: String) = update { set ->
        if (set.profiles.firstOrNull { it.id == profileId }?.builtIn == true) return@update set
        val remaining = set.profiles.filterNot { it.id == profileId }
        set.copy(
            profiles = remaining,
            activeId = if (set.activeId == profileId) remaining.first().id else set.activeId,
        )
    }

    suspend fun activate(profileId: String) = update { set ->
        if (set.profiles.none { it.id == profileId }) set else set.copy(activeId = profileId)
    }

    suspend fun resetToDefault() = update { set ->
        set.copy(activeId = builtInProfiles().first().id)
    }

    suspend fun toggleReadabilityLock(profileId: String, locked: Boolean) = update { set ->
        set.copy(readabilityLock = locked)
    }

    suspend fun setQualityOverride(quality: GlassQuality?) = update { it.copy(qualityOverride = quality) }

    suspend fun setOnboarded(value: Boolean) = update { it.copy(onboarded = value) }

    suspend fun importProfiles(raw: String): Int {
        val parsed = runCatching { json.decodeFromString<List<InterfaceProfile>>(raw) }.getOrNull() ?: return 0
        if (parsed.isEmpty()) return 0
        update { set ->
            val fresh = parsed.map { it.copy(id = java.util.UUID.randomUUID().toString(), builtIn = false) }
            set.copy(profiles = set.profiles + fresh)
        }
        return parsed.size
    }

    suspend fun exportActive(): String {
        val set = current()
        val active = set.active ?: return "[]"
        return json.encodeToString(kotlinx.serialization.builtins.ListSerializer(InterfaceProfile.serializer()), listOf(active))
    }

    companion object {
        /** Five built-in profiles — one per material. Always present, never deletable. */
        fun builtInProfiles(): List<InterfaceProfile> = GlassMaterial.entries.mapIndexed { index, material ->
            InterfaceProfile(
                id = "builtin_${material.id}",
                name = "NEXORA ${material.displayName}",
                spec = GlassSpecData.preset(material),
                layout = LayoutConfig(
                    clockFace = when (material) {
                        GlassMaterial.Crystal -> ClockFace.Halo.id
                        GlassMaterial.Aurora -> ClockFace.Sundial.id
                        GlassMaterial.Obsidian -> ClockFace.Epoch.id
                        GlassMaterial.Frost -> ClockFace.Minimal.id
                        GlassMaterial.Holographic -> ClockFace.Meridian.id
                    },
                    animationProfile = when (material) {
                        GlassMaterial.Obsidian -> MotionProfile.Calm.id
                        GlassMaterial.Holographic -> MotionProfile.Kinetic.id
                        else -> MotionProfile.Fluid.id
                    },
                ),
                createdAt = 0L,
                builtIn = true,
            )
        }
    }
}

data class ProfileSet(
    val profiles: List<InterfaceProfile> = InterfaceRepository.builtInProfiles(),
    val activeId: String = "builtin_crystal",
    val qualityOverride: GlassQuality? = null,
    val onboarded: Boolean = false,
    val readabilityLock: Boolean = false,
) {
    val active: InterfaceProfile? get() = profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()

    fun spec(reducedMotion: Boolean): GlassSpec = (active?.spec ?: GlassSpecData()).toSpec(reducedMotion)

    fun layout(): LayoutConfig = active?.layout ?: LayoutConfig()
}
