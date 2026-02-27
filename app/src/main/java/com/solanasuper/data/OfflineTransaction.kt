package com.solanasuper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_transactions")
data class OfflineTransaction(
    @PrimaryKey val id: String,
    val amount: Long,
    val timestamp: Long,
    val recipientId: String?,
    val status: TransactionStatus,
    val proof: String? = null,
    val isLiveBroadcastPending: Boolean = false,
    /** Base64-encoded fully-signed Solana transaction bytes, ready to broadcast. */
    val signedPayload: String? = null,
    /** The durable nonce value that was embedded in this transaction's message. */
    val nonceUsed: String? = null,
    /** JSON of the OfflineCommitment exchanged over Bluetooth — kept for audit trail. */
    val peerSignedCommitmentJson: String? = null
)

