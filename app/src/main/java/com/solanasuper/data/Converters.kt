package com.solanasuper.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(status: TransactionStatus): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(value: String): TransactionStatus {
        return TransactionStatus.valueOf(value)
    }
}
