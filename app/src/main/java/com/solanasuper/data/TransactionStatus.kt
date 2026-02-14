package com.solanasuper.data

enum class TransactionStatus {
    LOCKED,     // Funds are locally reserved/deducted
    PENDING,    // Proof generated, ready/waiting for sync
    CONFIRMED,  // On-chain confirmation received
    FAILED      // Failed to submit or invalid
}
