package com.nexora.app.domain.model

import kotlinx.serialization.Serializable

/** A wall-clock time in local time, minute precision. Stored as minutes-from-midnight. */
@Serializable
data class TimeOfDay(val minuteOfDay: Int) : Comparable<TimeOfDay> {

    init {
        require(minuteOfDay in 0..1439) { "minuteOfDay out of range: $minuteOfDay" }
    }

    val hour: Int get() = minuteOfDay / 60
    val minute: Int get() = minuteOfDay % 60

    override fun compareTo(other: TimeOfDay): Int = minuteOfDay.compareTo(other.minuteOfDay)

    fun plusMinutes(delta: Int): TimeOfDay {
        val total = Math.floorMod(minuteOfDay + delta, 1440)
        return TimeOfDay(total)
    }

    /** Signed distance in minutes from [other], in the range -720..720 (shortest way round). */
    fun circularDistanceTo(other: TimeOfDay): Int {
        var diff = other.minuteOfDay - minuteOfDay
        if (diff > 720) diff -= 1440
        if (diff < -720) diff += 1440
        return diff
    }

    fun format12h(): String {
        val h12 = if (hour % 12 == 0) 12 else hour % 12
        val suffix = if (hour < 12) "AM" else "PM"
        return "%d:%02d %s".format(h12, minute, suffix)
    }

    fun format24h(): String = "%02d:%02d".format(hour, minute)

    companion object {
        fun of(hour: Int, minute: Int): TimeOfDay = TimeOfDay(hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59))
        fun fromTotalMinutes(value: Int): TimeOfDay = TimeOfDay(Math.floorMod(value, 1440))

        /** Parses "HH:mm"; returns null rather than throwing on user input. */
        fun parse(value: String): TimeOfDay? {
            val parts = value.trim().split(":")
            if (parts.size != 2) return null
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            if (h !in 0..23 || m !in 0..59) return null
            return of(h, m)
        }
    }
}

/** Days of week as a bitmask so repetition is a single column in the database. */
@JvmInline
@Serializable
value class WeekdayMask(val bits: Int = 0) {
    /**
     * Bit 0 = Monday … bit 6 = Sunday.
     *
     * `DayOfWeek.value` is 1-based (Monday = 1 … Sunday = 7), so it must be shifted down
     * by one. Using the raw value put Sunday on bit 7, where no preset mask reaches it —
     * which silently prevented every Sunday alarm from ever firing.
     */
    private fun bitFor(day: java.time.DayOfWeek): Int = 1 shl (day.value - 1)

    fun contains(day: java.time.DayOfWeek): Boolean = bits and bitFor(day) != 0
    fun with(day: java.time.DayOfWeek, on: Boolean = true): WeekdayMask {
        val mask = bitFor(day)
        return WeekdayMask(if (on) bits or mask else bits and mask.inv())
    }

    val isEmpty: Boolean get() = bits == 0
    val isEveryDay: Boolean get() = bits == 0b1111111

    fun label(): String = when {
        bits == 0 -> "Once"
        isEveryDay -> "Every day"
        bits == WeekdayMask.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY).bits -> "Weekdays"
        bits == WeekdayMask.of(java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY).bits -> "Weekends"
        else -> java.time.DayOfWeek.entries.filter { contains(it) }
            .joinToString(" ") { it.name.take(3).lowercase().replaceFirstChar(Char::uppercase) }
    }

    companion object {
        val None = WeekdayMask(0)
        val EveryDay = WeekdayMask(0b1111111)

        fun of(vararg days: java.time.DayOfWeek): WeekdayMask {
            var b = 0
            days.forEach { b = b or (1 shl (it.value - 1)) }
            return WeekdayMask(b)
        }
    }
}
