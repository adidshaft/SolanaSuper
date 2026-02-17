package com.solanasuper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_transactions")
data class OfflineTransaction(
    @PrimaryKey val id: String, // Changed to String to match "tx_001"
    val amount: Long,
    val timestamp: Long,
    val recipientId: String?, // Nullable if just locking for a general purpose initially
    val status: TransactionStatus,
    val proof: String? = null, // ZK Proof, added later
    val isLiveBroadcastPending: Boolean = false,
    val signedPayload: String? = null
)
