package com.solanasuper.data

data class OfflineCommitment(
    val sender: String, // Base58 Public Key
    val recipient: String, // Base58 Public Key
    val amountLamports: Long,
    val timestamp: Long,
    val signature: String? = null // Ed25519 Signature of (sender + recipient + amount + timestamp)
)
