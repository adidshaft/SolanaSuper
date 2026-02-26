package com.solanasuper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists a Solana Durable Nonce account linked to this wallet.
 *
 * A nonce account stores a value that replaces the `recentBlockhash` in a
 * transaction. Unlike a regular blockhash (which expires in ~2 minutes), a
 * nonce is only consumed (invalidated) when you broadcast the transaction that
 * uses it. This makes it possible to:
 *  1. Build + sign a SOL transfer completely offline.
 *  2. Store the fully-signed transaction bytes locally.
 *  3. Broadcast it hours or days later when connectivity is restored.
 *
 * One nonce account per wallet is sufficient for sequential offline sends.
 * After each broadcast we advance the nonce (AdvanceNonce instruction) so the
 * account is ready for the next offline transaction.
 */
@Entity(tableName = "nonce_accounts")
data class NonceAccount(
    @PrimaryKey val id: Int = 1,             // Singleton row
    val nonceAccountPubkey: String,           // Base58 address of the nonce account
    val currentNonce: String,                 // Base58 value of the current nonce (= "durable blockhash")
    val authorityPubkey: String,              // Must match our wallet public key
    val createdAtMs: Long = System.currentTimeMillis(),
    val lastRefreshedMs: Long = System.currentTimeMillis(),
    val isValid: Boolean = true               // false if we suspect the nonce is stale
)
