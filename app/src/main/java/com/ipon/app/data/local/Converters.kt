package com.ipon.app.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromRecurrenceFrequency(frequency: RecurrenceFrequency): String = frequency.name

    @TypeConverter
    fun toRecurrenceFrequency(value: String): RecurrenceFrequency = RecurrenceFrequency.valueOf(value)

    @TypeConverter
    fun fromMoodRating(mood: MoodRating): String = mood.name

    @TypeConverter
    fun toMoodRating(value: String): MoodRating = MoodRating.valueOf(value)
}
