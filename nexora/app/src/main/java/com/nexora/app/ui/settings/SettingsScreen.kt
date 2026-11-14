package com.nexora.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.app.design.GlassQuality
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.GlassSpecData
import com.nexora.app.design.NexoraTheme
import com.nexora.app.platform.DeviceProfile
import com.nexora.app.ui.common.GlassButton
import com.nexora.app.ui.common.GlassPanel
import com.nexora.app.ui.common.NexoraSwitch
import com.nexora.app.ui.common.PillTag
import com.nexora.app.ui.common.SectionHeader
import com.nexora.app.ui.common.SettingsRow

@Composable
fun SettingsScreen(
    spec: GlassSpec,
    device: DeviceProfile,
    quality: GlassQuality,
    reducedMotion: Boolean,
    calendarGranted: Boolean,
    onQuality: (GlassQuality?) -> Unit,
    onReducedMotion: (Boolean) -> Unit,
    onUpdateSpec: ((GlassSpecData) -> GlassSpecData) -> Unit,
    onAlarmHealth: () -> Unit,
    onRequestCalendar: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestBattery: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("Settings", style = MaterialTheme.typography.headlineMedium, color = spec.textColor)
                Text("NEXORA ${com.nexora.app.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
            }
        }

        item {
            GlassPanel(
                title = "Performance profile",
                subtitle = "Chosen from your device's measured capability",
            ) {
                GlassQuality.entries.forEach { tier ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { onQuality(tier) }
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(tier.displayName, style = MaterialTheme.typography.bodyLarge, color = spec.textColor)
                            Text(tier.blurb, style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
                        }
                        if (quality == tier) PillTag("Active", highlighted = true)
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (quality != device.recommendedQuality()) {
                    GlassButton(
                        "Use recommended (${device.recommendedQuality().displayName})",
                        { onQuality(null) },
                        tonal = true,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Recommended for this device: ${device.recommendedQuality().displayName}. " +
                        "Your choice is remembered.",
                    style = MaterialTheme.typography.labelSmall,
                    color = spec.secondaryTextColor,
                )
            }
        }

        item {
            GlassPanel(title = "Interface") {
                SettingsRow(
                    "Open NEXORA Studio",
                    subtitle = "Materials, layout, motion, profiles",
                    icon = Icons.Rounded.Palette,
                    onClick = onOpenStudio,
                    trailing = { Icon(Icons.Rounded.ChevronRight, null, tint = spec.secondaryTextColor) },
                )
                SettingsRow(
                    "Dark theme",
                    icon = Icons.Rounded.DarkMode,
                    trailing = {
                        NexoraSwitch(spec.darkTheme) { on -> onUpdateSpec { it.copy(darkTheme = on) } }
                    },
                )
                SettingsRow(
                    "Haptics",
                    icon = Icons.Rounded.Vibration,
                    trailing = {
                        NexoraSwitch(spec.haptics) { on -> onUpdateSpec { it.copy(haptics = on) } }
                    },
                )
                SettingsRow(
                    "Reduced motion",
                    subtitle = "Follows the system setting and disables decorative animation",
                    icon = Icons.Rounded.Speed,
                    trailing = {
                        NexoraSwitch(reducedMotion, onReducedMotion)
                    },
                )
            }
        }

        item {
            GlassPanel(title = "Reliability") {
                SettingsRow(
                    "Alarm Health Centre",
                    subtitle = "Notification permission, exact alarms, next alarm",
                    icon = Icons.Rounded.Notifications,
                    onClick = onAlarmHealth,
                    trailing = { Icon(Icons.Rounded.ChevronRight, null, tint = spec.secondaryTextColor) },
                )
                SettingsRow(
                    "Notification settings",
                    subtitle = if (com.nexora.app.alarm.NotificationHelper.canPost(androidx.compose.ui.platform.LocalContext.current)) "Enabled" else "Disabled — alarms cannot notify",
                    onClick = onRequestNotifications,
                )
                SettingsRow(
                    "Battery optimisation",
                    subtitle = "Exempt NEXORA so Realme UI cannot freeze alarms",
                    icon = Icons.Rounded.BatterySaver,
                    onClick = onRequestBattery,
                )
            }
        }

        item {
            GlassPanel(title = "Calendar") {
                SettingsRow(
                    "Calendar access",
                    subtitle = when {
                        calendarGranted -> "Granted — reading today's events locally"
                        else -> "Not granted — agenda stays empty"
                    },
                    icon = Icons.Rounded.CalendarMonth,
                    onClick = onRequestCalendar,
                    trailing = { PillTag(if (calendarGranted) "On" else "Off", highlighted = calendarGranted) },
                )
            }
        }

        item {
            GlassPanel(title = "Privacy") {
                SettingsRow(
                    "How NEXORA handles your data",
                    icon = Icons.Rounded.Memory,
                    onClick = onOpenPrivacy,
                    trailing = { Icon(Icons.Rounded.ChevronRight, null, tint = spec.secondaryTextColor) },
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "All alarms, tasks, routines, focus sessions and interface profiles are stored in a local " +
                        "database on this device. NEXORA has no network code: nothing is uploaded, and no " +
                        "analytics or crash reporting library is included.",
                    style = MaterialTheme.typography.bodySmall,
                    color = spec.secondaryTextColor,
                )
            }
        }

        item {
            GlassPanel(title = "Device") {
                Text(device.summary(), style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
            }
        }
    }
}

@Composable
fun PrivacyScreen(spec: GlassSpec) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Privacy", style = MaterialTheme.typography.headlineMedium, color = spec.textColor) }
        item {
            GlassPanel(title = "Local by design") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "No internet permission is declared in the manifest. The app is physically unable to send data off the device.",
                        "Calendar access is optional and read-only: NEXORA queries today's events and never inserts, updates or deletes.",
                        "Alarms, tasks, routines, focus sessions and themes live in a local Room database and DataStore file.",
                        "No analytics SDK, no crash reporter, no advertising ID, no tracking of any kind.",
                        "Backups: Android's own backup may include the local database if the user enables it in system settings.",
                        "You can delete everything by clearing app storage in system settings.",
                    ).forEach { line ->
                        Row {
                            Text("•", color = spec.accentColor)
                            Spacer(Modifier.height(0.dp))
                            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                            Text(line, style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Spacer(modifier: Modifier) = androidx.compose.foundation.layout.Spacer(modifier)
