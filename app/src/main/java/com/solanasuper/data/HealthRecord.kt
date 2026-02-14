
package com.solanasuper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_records")
data class HealthRecord(
    @PrimaryKey val id: String,
    val type: String,
    val data: String // Encrypted data payload or raw data (if DB is encrypted)
)
