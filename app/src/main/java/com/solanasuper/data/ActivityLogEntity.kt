package com.solanasuper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ActivityType {
    SOLANA_TX,
    ARCIUM_PROOF,
    IPFS_HASH
}

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val type: ActivityType,
    val hashValue: String
)
