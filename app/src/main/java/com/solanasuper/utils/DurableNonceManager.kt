package com.solanasuper.utils

import android.util.Log
import com.solanasuper.data.NonceAccount
import com.solanasuper.data.NonceAccountDao
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Manages a Solana Durable Nonce account so the app can sign transactions
 * completely offline and broadcast them later — no blockhash expiry.
 *
 * ── Lifecycle ──────────────────────────────────────────────────────────────
 *
 *  [First run / ONLINE]
 *    ensureNonceAccount()
 *      → Calls SystemProgram.createAccount + SystemProgram.initializeNonceAccount
 *      → Stores NonceAccount in Room DB
 *
 *  [Offline send]
 *    buildOfflineTransferMessage(recipient, lamports)
 *      → Gets stored nonce from DB (does NOT hit network)
 *      → Returns a message built with the durable nonce instead of a blockhash
 *      → Caller signs this with biometrics → stores fully-signed tx bytes in DB
 *
 *  [Back online — SyncWorker]
 *    advanceNonceAndBroadcast(signedTxBytes)
 *      → Sends the pre-signed tx to RPC (sendTransaction)
 *      → On success: fetches the new nonce value from chain and saves it
 *
 * ── Why the nonce must be "advanced" after each use ────────────────────────
 *  Each nonce value can only be used ONCE. The AdvanceNonce instruction inside
 *  the signed transaction automatically increments it. After broadcast we just
 *  re-query the chain to learn the new value.
 *
 * ── Devnet note ────────────────────────────────────────────────────────────
 *  createNonceAccount sends a real on-chain transaction on Devnet.
 *  We fund it with a minimum rent-exempt amount (~0.00144768 SOL).
 *  The wallet must have at least that balance on Devnet.
 */
object DurableNonceManager {

    private const val TAG = "DurableNonceManager"

    // Minimum lamports for a nonce account to be rent-exempt (128 bytes).
    const val NONCE_ACCOUNT_RENT_LAMPORTS = 1_500_000L   // 0.0015 SOL — safe margin

    // Solana system program constants
    private val SYSTEM_PROGRAM_ID = Base58.decode("11111111111111111111111111111111")
    // SysVar Rent and Blockhashes
    private val SYSVAR_RECENT_BLOCKHASHES = Base58.decode("SysvarRecentB1ockHashes11111111111111111111")
    private val SYSVAR_RENT              = Base58.decode("SysvarRent111111111111111111111111111111111")

    // -------------------------------------------------------------------
    // Public helpers used by IncomeViewModel to orchestrate nonce setup
    // via the biometric channel
    // -------------------------------------------------------------------

    /**
     * Returns the Base58 public key of the deterministic nonce account for [walletAddressB58].
     * This is computed entirely offline — no network.
     */
    fun derivedNoncePubkeyB58(walletAddressB58: String): String {
        val (pubkeyBytes, _) = generateDeterministicKeypair(walletAddressB58 + "_nonce")
        return Base58.encode(pubkeyBytes)
    }

    /**
     * Returns the 32-byte seed of the deterministic nonce account keypair.
     * Used by the ViewModel to sign the self-sig portion of the setup tx.
     */
    fun derivedNonceSeed32(walletAddressB58: String): ByteArray {
        val (_, seed32) = generateDeterministicKeypair(walletAddressB58 + "_nonce")
        return seed32
    }

    /**
     * Builds the CreateAccount + InitializeNonceAccount message.
     * Public so IncomeViewModel can build it and send it through the biometric channel.
     */
    suspend fun buildNonceSetupMessage(
        walletAddressB58: String,
        rpcUrl: String
    ): ByteArray = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val blockhash   = Base58.decode(fetchBlockhash(rpcUrl))
        val authorityPk = Base58.decode(walletAddressB58)
        val noncePk     = Base58.decode(derivedNoncePubkeyB58(walletAddressB58))
        buildCreateNonceAccountMessage(authorityPk, noncePk, NONCE_ACCOUNT_RENT_LAMPORTS, blockhash)
    }

    /** Public wrapper so IncomeViewModel can produce the nonce account's self-signature. */
    fun signWithSeed(seed32: ByteArray, message: ByteArray): ByteArray =
        signWithSeedInternal(seed32, message)

    /** Reads the current nonce value from chain. Public for ViewModel use. */
    suspend fun fetchNonceValuePublic(rpcUrl: String, noncePubkeyB58: String): String =
        fetchNonceValue(rpcUrl, noncePubkeyB58)

    // -------------------------------------------------------------------
    // 2. BUILD OFFLINE TRANSFER MESSAGE  (fully offline, uses cached nonce)
    // -------------------------------------------------------------------

    /**
     * Constructs a Solana transaction message using the stored durable nonce.
     * This call is FULLY OFFLINE — it only reads the local DB.
     *
     * The returned bytes are ready to be signed with the user's Ed25519 key.
     * After signing, call [encodeSignedOfflineTx] and store the result.
     *
     * @throws Exception if no valid nonce account is found in DB.
     */
    suspend fun buildOfflineTransferMessage(
        fromPubkeyB58: String,
        toPubkeyB58:   String,
        lamports:      Long,
        dao:           NonceAccountDao
    ): ByteArray {
        val nonceRecord = dao.get()
            ?: throw Exception("No nonce account found. Go online first to set up offline transfers.")

        if (!nonceRecord.isValid)
            throw Exception("Nonce account needs refresh. Connect to network once to reset.")

        val fromPubkey  = Base58.decode(fromPubkeyB58)
        val toPubkey    = Base58.decode(toPubkeyB58)
        val noncePubkey = Base58.decode(nonceRecord.nonceAccountPubkey)
        val nonceBytes  = Base58.decode(nonceRecord.currentNonce)
        val authorityPubkey = Base58.decode(nonceRecord.authorityPubkey)

        // A durable nonce transaction MUST have an AdvanceNonceAccount instruction
        // as the FIRST instruction, then the actual transfer.
        return buildDurableTransferMessage(
            fromPubkey      = fromPubkey,
            toPubkey        = toPubkey,
            lamports        = lamports,
            noncePubkey     = noncePubkey,
            nonceAuthority  = authorityPubkey,
            durableNonce    = nonceBytes
        )
    }

    /**
     * Encodes a fully-signed offline transaction ready for broadcast.
     * Store these bytes in [OfflineTransaction.signedPayload] (as Base64).
     */
    fun encodeSignedOfflineTx(signature: ByteArray, message: ByteArray): String {
        val txBytes = SolanaUtil.encodeTransaction(signature, message)
        return android.util.Base64.encodeToString(txBytes, android.util.Base64.NO_WRAP)
    }

    // -------------------------------------------------------------------
    // 3. BROADCAST + ADVANCE NONCE  (online, called by SyncWorker)
    // -------------------------------------------------------------------

    /**
     * Broadcasts a pre-signed offline transaction and then fetches the new
     * nonce value from chain so the account is ready for the next offline send.
     *
     * @param rpcUrl        Devnet RPC endpoint
     * @param signedTxB64   Base64-encoded fully-signed transaction bytes (from DB)
     * @param dao           Room DAO to update the nonce after broadcast
     * @return On-chain transaction signature string
     */
    suspend fun broadcastOfflineTx(
        rpcUrl:      String,
        signedTxB64: String,
        dao:         NonceAccountDao
    ): String {
        AppLogger.i(TAG, "Broadcasting pre-signed offline tx…")

        val response = postRpc(rpcUrl, buildSendTxJson(signedTxB64))
        val json     = JSONObject(response)

        if (json.has("error")) {
            val errMsg = json.get("error").toString()
            // Nonce was already consumed (double-spend or previous success)
            if (errMsg.contains("BlockhashNotFound") || errMsg.contains("InvalidNonce")) {
                AppLogger.w(TAG, "Nonce already consumed — marking nonce as stale, need refresh.")
                dao.setValid(false)
            }
            throw Exception("Broadcast failed: $errMsg")
        }

        val txSig = json.getString("result")
        AppLogger.i(TAG, "Offline tx broadcast success: $txSig")

        // Refresh the nonce value from chain (the AdvanceNonce ix mutated it)
        try {
            val nonceRecord = dao.get()
            if (nonceRecord != null) {
                kotlinx.coroutines.delay(2_000) // Brief wait for chain state
                val newNonce = fetchNonceValue(rpcUrl, nonceRecord.nonceAccountPubkey)
                dao.updateNonce(newNonce)
                AppLogger.i(TAG, "Nonce advanced to: $newNonce")
            }
        } catch (e: Exception) {
            // Non-fatal — the tx was broadcast. We'll refresh nonce on next online session.
            AppLogger.w(TAG, "Nonce refresh failed (non-fatal): ${e.message}")
            dao.setValid(false)
        }

        return txSig
    }

    /**
     * Refreshes the stored nonce value from chain.
     * Call this when [NonceAccount.isValid] is false before the next offline send.
     */
    suspend fun refreshNonce(rpcUrl: String, dao: NonceAccountDao) {
        val record = dao.get() ?: return
        try {
            val fresh = fetchNonceValue(rpcUrl, record.nonceAccountPubkey)
            dao.updateNonce(fresh)
            dao.setValid(true)
            AppLogger.i(TAG, "Nonce refreshed: $fresh")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Nonce refresh failed", e)
        }
    }

    // -------------------------------------------------------------------
    // Message builders (binary Solana wire format)
    // -------------------------------------------------------------------

    /**
     * Builds the message for creating + initializing a nonce account.
     *
     * Solana requires TWO signers for this transaction:
     *   [0] authority  — fee payer, will be the nonce account's authority
     *   [1] nonceAccount keypair — new account must sign its own creation
     *
     * Header:
     *   numRequiredSignatures      = 2   (authority + nonce account)
     *   numReadonlySignedAccounts  = 0
     *   numReadonlyUnsignedAccounts = 3  (SystemProgram, SysvarRecentBlockhashes, SysvarRent)
     *
     * Account table order matches signature slots:
     *   [0] authority               — signer, writable
     *   [1] nonceAccount            — signer, writable
     *   [2] SystemProgram           — readonly unsigned
     *   [3] SysvarRecentBlockhashes — readonly unsigned
     *   [4] SysvarRent              — readonly unsigned
     */
    private fun buildCreateNonceAccountMessage(
        authorityPubkey: ByteArray,
        noncePubkey:     ByteArray,
        rentLamports:    Long,
        recentBlockhash: ByteArray
    ): ByteArray {
        val out = java.io.ByteArrayOutputStream()

        // Header: 2 required signers, 0 read-only signed, 3 read-only unsigned
        out.write(2); out.write(0); out.write(3)

        // Account table (5 accounts)
        out.write(SolanaUtil.encodeLength(5))
        out.write(authorityPubkey)           // [0] signer + writable
        out.write(noncePubkey)               // [1] signer + writable
        out.write(SYSTEM_PROGRAM_ID)         // [2] readonly unsigned
        out.write(SYSVAR_RECENT_BLOCKHASHES) // [3] readonly unsigned
        out.write(SYSVAR_RENT)               // [4] readonly unsigned

        // Recent blockhash (32 bytes)
        out.write(recentBlockhash)

        // Instruction count = 2
        out.write(SolanaUtil.encodeLength(2))

        // ── Instruction 0: SystemProgram::CreateAccount ──
        // Program index: 2 (SystemProgram)
        out.write(2)
        // Account indices: [authority(0), nonceAccount(1)]
        out.write(SolanaUtil.encodeLength(2)); out.write(0); out.write(1)
        // Data: CreateAccount discriminator(u32=0) | lamports(u64) | space(u64=80) | owner(32 bytes)
        val createData = java.io.ByteArrayOutputStream()
        createData.write(SolanaUtil.intToLittleEndian(0))           // CreateAccount = 0
        createData.write(SolanaUtil.longToLittleEndian(rentLamports))
        createData.write(SolanaUtil.longToLittleEndian(80L))         // nonce account = 80 bytes
        createData.write(SYSTEM_PROGRAM_ID)                          // owned by System Program
        out.write(SolanaUtil.encodeLength(createData.size()))
        out.write(createData.toByteArray())

        // ── Instruction 1: SystemProgram::InitializeNonceAccount ──
        // Program index: 2 (SystemProgram)
        out.write(2)
        // Account indices: [nonceAccount(1), sysvar_recent_blockhashes(3), sysvar_rent(4)]
        out.write(SolanaUtil.encodeLength(3)); out.write(1); out.write(3); out.write(4)
        // Data: InitializeNonceAccount discriminator(u32=6) | authority_pubkey(32 bytes)
        val initData = java.io.ByteArrayOutputStream()
        initData.write(SolanaUtil.intToLittleEndian(6))              // InitializeNonceAccount = 6
        initData.write(authorityPubkey)
        out.write(SolanaUtil.encodeLength(initData.size()))
        out.write(initData.toByteArray())

        return out.toByteArray()
    }

    /**
     * Builds a durable-nonce transfer message:
     *   Instruction 0: AdvanceNonceAccount  — MANDATORY as first instruction
     *   Instruction 1: Transfer             — the actual SOL move
     *
     * The "recentBlockhash" field in the message header is set to the nonce value.
     */
    private fun buildDurableTransferMessage(
        fromPubkey:     ByteArray,  // sender (signer, writable)
        toPubkey:       ByteArray,  // recipient (writable)
        lamports:       Long,
        noncePubkey:    ByteArray,  // nonce account (writable)
        nonceAuthority: ByteArray,  // must == fromPubkey for our use case
        durableNonce:   ByteArray   // 32 bytes — replaces recentBlockhash
    ): ByteArray {
        val out = java.io.ByteArrayOutputStream()

        // Header: 1 required signer, 0 ro-signed, 2 ro-unsigned (SystemProgram, SysvarRecentBlockhashes)
        out.write(1); out.write(0); out.write(2)

        // Accounts: [sender/authority(0), recipient(1), nonceAccount(2), SystemProgram(3), SysvarRecentBlockhashes(4)]
        // sender = nonceAuthority for our single-key wallet
        out.write(SolanaUtil.encodeLength(5))
        out.write(fromPubkey)
        out.write(toPubkey)
        out.write(noncePubkey)
        out.write(SYSTEM_PROGRAM_ID)
        out.write(SYSVAR_RECENT_BLOCKHASHES)

        // "recentBlockhash" = durable nonce value (32 bytes)
        out.write(durableNonce)

        // 2 Instructions
        out.write(SolanaUtil.encodeLength(2))

        // ── Instruction 0: AdvanceNonceAccount ──
        out.write(3) // SystemProgram index
        // Accounts: [nonceAccount(2, writable), sysvar_recent_blockhashes(4, readonly), authority(0, signer)]
        out.write(SolanaUtil.encodeLength(3)); out.write(2); out.write(4); out.write(0)
        // Data: discriminator u32 = 4 (AdvanceNonceAccount)
        val advData = SolanaUtil.intToLittleEndian(4)
        out.write(SolanaUtil.encodeLength(advData.size))
        out.write(advData)

        // ── Instruction 1: Transfer ──
        out.write(3) // SystemProgram index
        // Accounts: [sender(0, signer+writable), recipient(1, writable)]
        out.write(SolanaUtil.encodeLength(2)); out.write(0); out.write(1)
        // Data: discriminator u32 = 2 + lamports u64
        val transferData = java.io.ByteArrayOutputStream()
        transferData.write(SolanaUtil.intToLittleEndian(2)) // Transfer = 2
        transferData.write(SolanaUtil.longToLittleEndian(lamports))
        out.write(SolanaUtil.encodeLength(transferData.size()))
        out.write(transferData.toByteArray())

        return out.toByteArray()
    }

    // -------------------------------------------------------------------
    // Network helpers
    // -------------------------------------------------------------------

    /** Fetches the latest blockhash from the RPC node. */
    suspend fun fetchBlockhash(rpcUrl: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val body = """{"jsonrpc":"2.0","id":1,"method":"getLatestBlockhash","params":[{"commitment":"finalized"}]}"""
        val response = postRpc(rpcUrl, body)
        val json = JSONObject(response)
        if (json.has("error")) throw Exception("Blockhash fetch failed: ${json.get("error")}")
        json.getJSONObject("result").getJSONObject("value").getString("blockhash")
    }

    /**
     * Reads the current nonce value from a nonce account on-chain.
     * Retries automatically as RPC nodes may lag behind block propagation.
     */
    private suspend fun fetchNonceValue(rpcUrl: String, noncePubkeyB58: String): String =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val body = """{"jsonrpc":"2.0","id":1,"method":"getAccountInfo","params":["$noncePubkeyB58",{"encoding":"jsonParsed"}]}"""
            
            for (attempt in 1..8) {
                try {
                    val response = postRpc(rpcUrl, body)
                    val json = JSONObject(response)
                    if (json.has("error")) throw Exception("getAccountInfo failed: ${json.get("error")}")
                    
                    val value = json.getJSONObject("result").optJSONObject("value")
                    if (value != null) {
                        val dataObj = value.optJSONObject("data") ?: return@withContext value.getString("data") 
                        val info = dataObj.getJSONObject("parsed").getJSONObject("info")
                        
                        // Solana nonce value is the "blockhash" inside the info object
                        return@withContext info.optString("blockhash").takeIf { it.isNotEmpty() }
                            ?: info.optString("nonce").takeIf { it.isNotEmpty() }
                            ?: info.getJSONObject("nonce").getString("nonce") // Fallback
                    }
                } catch (e: Exception) {
                    if (attempt == 8) throw e
                }
                
                // Account not found or request failed, wait and retry
                kotlinx.coroutines.delay(2000)
            }
            throw Exception("Nonce account not found on chain after 16 seconds")
        }

    private fun buildSendTxJson(base64Tx: String) =
        """{"jsonrpc":"2.0","id":1,"method":"sendTransaction","params":["$base64Tx",{"encoding":"base64","skipPreflight":false,"preflightCommitment":"processed"}]}"""

    private fun postRpc(url: String, body: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 20_000
        conn.readTimeout    = 20_000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Connection", "close")
        conn.outputStream.use { it.write(body.toByteArray()) }
        val code = conn.responseCode
        return if (code == 200) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "no body"
            throw Exception("RPC HTTP $code: $err")
        }
    }

    // -------------------------------------------------------------------
    // Deterministic keypair for the nonce account
    // -------------------------------------------------------------------

    /**
     * Derives a deterministic Ed25519 keypair from a UTF-8 seed string.
     * The nonce account keypair is always recoverable from the wallet identity.
     * Returns Pair<publicKeyBytes(32), seed32>
     */
    private fun generateDeterministicKeypair(seed: String): Pair<ByteArray, ByteArray> {
        val seed32   = java.security.MessageDigest.getInstance("SHA-256")
            .digest(seed.toByteArray(Charsets.UTF_8))
        val spec     = net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable.getByName(
            net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable.ED_25519
        )
        val privSpec = net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec(seed32, spec)
        val privKey  = net.i2p.crypto.eddsa.EdDSAPrivateKey(privSpec)
        val pubKey   = net.i2p.crypto.eddsa.EdDSAPublicKey(
            net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec(privKey.a, spec)
        )
        return Pair(pubKey.abyte, seed32)
    }

    /**
     * Signs [message] with a raw 32-byte Ed25519 seed (no Android Keystore / no biometric).
     * Used only for the nonce account's self-signature during creation.
     */
    private fun signWithSeedInternal(seed32: ByteArray, message: ByteArray): ByteArray {
        val spec     = net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable.getByName(
            net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable.ED_25519
        )
        val privSpec = net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec(seed32, spec)
        val privKey  = net.i2p.crypto.eddsa.EdDSAPrivateKey(privSpec)
        val signer   = java.security.Signature.getInstance("NONEwithEdDSA", "EdDSA")
        signer.initSign(privKey)
        signer.update(message)
        val rawSig = signer.sign()
        // EdDSA library may return DER-encoded sig — decode to raw 64 bytes if needed
        return if (rawSig.size == 64) rawSig else decodeDerToRaw64(rawSig)
    }

    /**
     * Converts a DER-encoded ECDSA/EdDSA signature to raw 64-byte R||S format.
     * EdDSA is NOT DER by default, but this is a safety net.
     */
    private fun decodeDerToRaw64(der: ByteArray): ByteArray {
        // If truly EdDSA (net.i2p.crypto.eddsa library), sign() returns raw 64 bytes.
        // This function is a no-op pass-through guard.
        if (der.size == 64) return der
        throw IllegalArgumentException("Unexpected signature length ${der.size} — expected 64 bytes")
    }
}

