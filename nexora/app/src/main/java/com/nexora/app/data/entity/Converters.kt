package com.nexora.app.data.entity

import androidx.room.TypeConverter
import java.time.DayOfWeek

class Converters {
    @TypeConverter fun dayOfWeekToString(day: DayOfWeek): String = day.name
    @TypeConverter fun stringToDayOfWeek(value: String): DayOfWeek = DayOfWeek.valueOf(value)
}
