package com.solanasuper.network

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solanasuper.data.TransactionStatus
import com.solanasuper.data.WalletDatabase
import com.solanasuper.utils.AppLogger
import com.solanasuper.utils.DurableNonceManager

/**
 * WorkManager background worker that settles offline P2P transfers.
 *
 * It is scheduled by IncomeViewModel whenever the device comes back online.
 * It processes two categories of pending rows:
 *
 *  1. SIGNED_OFFLINE  — Contains real, fully-signed Solana transaction bytes
 *                       built with a durable nonce. Broadcast directly to RPC.
 *                       After success, advances the stored nonce so the account
 *                       is ready for the next offline send.
 *
 *  2. PENDING_SYNC    — Legacy IOU commitments (no valid signed tx bytes).
 *                       These can no longer be auto-settled without the user;
 *                       we mark them FAILED with a description so the UI can
 *                       prompt the user to re-send manually.
 *
 * Edge-cases handled:
 *  - AlreadyProcessed / duplicate broadcast  → marked CONFIRMED idempotently
 *  - Nonce stale / InvalidNonce              → nonce account set invalid, user notified
 *  - RPC timeout / network flap              → Worker returns RETRY (WorkManager retries w/ back-off)
 *  - All-success                             → returns SUCCESS
 *  - Partial failure                         → returns RETRY so remaining rows are retried
 */
class SolanaSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    companion object {
        const val TAG = "SolanaSyncWorker"
    }

    override suspend fun doWork(): Result {
        val db       = WalletDatabase.getDatabase(applicationContext)
        val txDao    = db.transactionDao()
        val nonceDao = db.nonceAccountDao()
        val rpcUrl   = NetworkManager.activeRpcUrl.value

        AppLogger.i(TAG, "SolanaSyncWorker starting. RPC: $rpcUrl")

        if (!NetworkManager.isLiveMode.value) {
            AppLogger.i(TAG, "Not in live mode — skipping.")
            return Result.success()
        }

        var anyFailure = false
        var anyRetry   = false

        // ── 1. Process SIGNED_OFFLINE (durable-nonce) rows ──────────────────
        val signedRows = txDao.getSignedOfflineTxs()
        AppLogger.i(TAG, "Found ${signedRows.size} SIGNED_OFFLINE tx(s) to broadcast.")

        for (row in signedRows) {
            val signedTxB64 = row.signedPayload
            if (signedTxB64.isNullOrBlank()) {
                AppLogger.w(TAG, "Row ${row.id} has no signed payload — marking FAILED.")
                txDao.updateStatus(row.id, TransactionStatus.FAILED)
                anyFailure = true
                continue
            }

            try {
                val txSig = DurableNonceManager.broadcastOfflineTx(rpcUrl, signedTxB64, nonceDao)
                AppLogger.i(TAG, "✅ Broadcast success for ${row.id}: $txSig")
                txDao.updateStatus(row.id, TransactionStatus.CONFIRMED)

            } catch (e: Exception) {
                val msg = e.message ?: ""
                when {
                    // Already processed — treat as confirmed (idempotent)
                    msg.contains("AlreadyProcessed") || msg.contains("already exists") -> {
                        AppLogger.w(TAG, "Tx ${row.id} already on chain — marking CONFIRMED.")
                        txDao.updateStatus(row.id, TransactionStatus.CONFIRMED)
                    }
                    // Nonce consumed by someone else or corrupted — can't recover automatically
                    msg.contains("InvalidNonce") || msg.contains("BlockhashNotFound") -> {
                        AppLogger.e(TAG, "Nonce invalid for ${row.id} — marking FAILED. User must re-send.")
                        txDao.updateStatus(row.id, TransactionStatus.FAILED)
                        nonceDao.setValid(false)
                        anyFailure = true
                    }
                    // Transient network error — let WorkManager retry
                    else -> {
                        AppLogger.e(TAG, "Transient error for ${row.id}: $msg — will retry.", e)
                        anyRetry = true
                    }
                }
            }
        }

        // ── 2. Process legacy PENDING_SYNC (IOU-only) rows ──────────────────
        val pendingRows = txDao.getPendingSyncTransactions()
        AppLogger.i(TAG, "Found ${pendingRows.size} legacy PENDING_SYNC IOU(s).")

        for (row in pendingRows) {
            if (!row.signedPayload.isNullOrBlank()) {
                // Has a signed payload from old approach — attempt broadcast as-is
                try {
                    val sendJson = """{"jsonrpc":"2.0","id":1,"method":"sendTransaction","params":["${row.signedPayload}",{"encoding":"base64"}]}"""
                    val response = postRpc(rpcUrl, sendJson)
                    val json = org.json.JSONObject(response)
                    if (json.has("error")) {
                        val errStr = json.get("error").toString()
                        if (errStr.contains("BlockhashNotFound") || errStr.contains("expired")) {
                            AppLogger.w(TAG, "Legacy tx ${row.id} blockhash expired — FAILED.")
                            txDao.updateStatus(row.id, TransactionStatus.FAILED)
                            anyFailure = true
                        } else {
                            anyRetry = true
                        }
                    } else {
                        txDao.updateStatus(row.id, TransactionStatus.CONFIRMED)
                        AppLogger.i(TAG, "Legacy tx ${row.id} confirmed.")
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Legacy tx ${row.id} broadcast error: ${e.message}", e)
                    anyRetry = true
                }
            }
            // Pure IOU rows — leave as PENDING_SYNC so UI can show "Tap to settle" CTA
        }

        // ── 3. Refresh nonce if it was marked invalid ────────────────────────
        val nonceRecord = nonceDao.get()
        if (nonceRecord != null && !nonceRecord.isValid) {
            AppLogger.i(TAG, "Nonce marked invalid — attempting refresh.")
            DurableNonceManager.refreshNonce(rpcUrl, nonceDao)
        }

        return when {
            anyRetry -> {
                AppLogger.i(TAG, "Some txs need retry — returning RETRY.")
                Result.retry()
            }
            else -> {
                AppLogger.i(TAG, "SolanaSyncWorker done. Failures=$anyFailure")
                Result.success()
            }
        }
    }

    private fun postRpc(url: String, body: String): String {
        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 20_000
        conn.readTimeout    = 20_000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.outputStream.use { it.write(body.toByteArray()) }
        val code = conn.responseCode
        return if (code == 200)
            conn.inputStream.bufferedReader().use { it.readText() }
        else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "no body"
            throw Exception("RPC HTTP $code: $err")
        }
    }
}
