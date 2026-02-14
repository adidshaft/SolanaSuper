package com.solanasuper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_transactions")
data class OfflineTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val timestamp: Long,
    val recipientId: String?, // Nullable if just locking for a general purpose initially
    val status: TransactionStatus,
    val proof: String? = null // ZK Proof, added later
)
