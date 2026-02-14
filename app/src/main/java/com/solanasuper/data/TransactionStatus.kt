package com.solanasuper.data

enum class TransactionStatus {
    AVAILABLE,          // Mock status for initial balance
    LOCKED_PENDING_P2P, // Funds locked, waiting for P2P transfer
    LOCKED_SYNCING,     // P2P done, syncing to Solana
    CONFIRMED,          // On-chain confirmation
    FAILED,             // Failed transaction
    SPENT               // Final state after confirmation
}
