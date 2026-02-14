package com.solanasuper.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_records")
data class HealthEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "data_type") val dataType: String,
    @ColumnInfo(name = "encrypted_payload") val encryptedPayload: String
)
