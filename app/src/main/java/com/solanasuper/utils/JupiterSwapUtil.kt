package com.solanasuper.utils

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility for Jupiter V6 Swap API (Devnet-compatible)
 * Jupiter quote API is network-agnostic; the token mints determine the chain.
 * We use Devnet wrapped-SOL and USDC mint addresses.
 */
object JupiterSwapUtil {

    private const val TAG = "JupiterSwapUtil"

    // Devnet token mints (official Solana devnet test tokens)
    const val WSOL_MINT  = "So11111111111111111111111111111111111111112"
    const val USDC_MINT  = "Gh9ZwEmdLJ8DscKNTkTqPbNwLNNBjuSzaG9Vp2KGtKJr" // Devnet USDC
    const val JITOSOL_MINT = "J1toso1uCk3RLmjorhTtrVwY9HJ7X8V9yYac6Y7kGCPn"

    private const val QUOTE_API = "https://quote-api.jup.ag/v6/quote"
    private const val SWAP_API  = "https://quote-api.jup.ag/v6/swap"

    data class SwapQuote(
        val inputMint: String,
        val outputMint: String,
        val inputAmount: Long,
        val outputAmount: Long,
        val priceImpactPct: Double,
        val slippageBps: Int,
        val rawQuoteJson: String  // serialized for swap call
    )

    /**
     * Get a swap quote from Jupiter.
     * @param inputMint  The mint address of the token you're selling
     * @param outputMint The mint address of the token you're buying
     * @param amountLamports Amount of input token in smallest unit (lamports for SOL, micro-USDC for USDC)
     * @param slippageBps Slippage in basis points (e.g. 50 = 0.5%)
     */
    @Throws(Exception::class)
    fun getQuote(
        inputMint: String,
        outputMint: String,
        amountLamports: Long,
        slippageBps: Int = 50
    ): SwapQuote {
        val urlStr = "$QUOTE_API?inputMint=$inputMint&outputMint=$outputMint" +
                "&amount=$amountLamports&slippageBps=$slippageBps"

        Log.d(TAG, "Fetching Jupiter quote: $urlStr")

        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15_000
        conn.readTimeout   = 15_000
        conn.setRequestProperty("Accept", "application/json")

        val responseCode = conn.responseCode
        val body = if (responseCode == 200) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "No body"
            throw Exception("Jupiter Quote HTTP $responseCode: $err")
        }

        val json = JSONObject(body)
        val outAmount = json.getLong("outAmount")
        val priceImpact = json.optDouble("priceImpactPct", 0.0)

        Log.i(TAG, "Quote received: $amountLamports → $outAmount | impact=${priceImpact}%")

        return SwapQuote(
            inputMint        = inputMint,
            outputMint       = outputMint,
            inputAmount      = amountLamports,
            outputAmount     = outAmount,
            priceImpactPct   = priceImpact,
            slippageBps      = slippageBps,
            rawQuoteJson     = body
        )
    }

    /**
     * Get a serialized swap transaction from Jupiter.
     * The caller must sign and broadcast this transaction.
     * @param quoteResponse The raw JSON string returned by [getQuote]
     * @param userPublicKey  The Base58 public key of the user's wallet
     * @return Base64-encoded transaction bytes ready for signing
     */
    @Throws(Exception::class)
    fun getSwapTransaction(quoteResponse: String, userPublicKey: String): String {
        val payload = JSONObject().apply {
            put("quoteResponse", JSONObject(quoteResponse))
            put("userPublicKey", userPublicKey)
            put("wrapAndUnwrapSol", true)
            put("computeUnitPriceMicroLamports", 1000) // Priority fee
        }

        Log.d(TAG, "Requesting swap transaction for $userPublicKey")

        val conn = URL(SWAP_API).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 20_000
        conn.readTimeout   = 20_000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.outputStream.use { it.write(payload.toString().toByteArray()) }

        val responseCode = conn.responseCode
        val body = if (responseCode == 200) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "No body"
            throw Exception("Jupiter Swap TX HTTP $responseCode: $err")
        }

        val json = JSONObject(body)
        // Jupiter returns "swapTransaction" as base64
        return json.getString("swapTransaction")
    }
}
