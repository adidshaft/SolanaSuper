package com.solanasuper.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(status: TransactionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): TransactionStatus = TransactionStatus.valueOf(value)

    @TypeConverter
    fun fromActivityType(type: ActivityType): String = type.name

    @TypeConverter
    fun toActivityType(value: String): ActivityType = ActivityType.valueOf(value)

    @TypeConverter
    fun fromPositionType(type: PositionType): String = type.name

    @TypeConverter
    fun toPositionType(value: String): PositionType = PositionType.valueOf(value)

    @TypeConverter
    fun fromPositionStatus(status: PositionStatus): String = status.name

    @TypeConverter
    fun toPositionStatus(value: String): PositionStatus = PositionStatus.valueOf(value)
}
