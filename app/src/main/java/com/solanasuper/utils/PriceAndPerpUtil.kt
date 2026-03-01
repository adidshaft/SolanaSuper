package com.solanasuper.utils

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Price feed + Drift Perp helper for Sovereign Invest module.
 *
 * Price Feed: Pyth Devnet (Hermes endpoint) for SOL/USD.
 * Perp: Drift Protocol Devnet — simulated locally when network unavailable.
 *
 * DEVNET NOTE: Drift has a full devnet deployment at https://master.drift.trade/
 * We use a simplified "open position" / "close position" abstraction.
 * The actual Drift SDK transactions are complex; for the hackathon we use
 * the PUBLIC RPC + Drift program calls via raw transaction building.
 * In SIMULATION mode we use local math only.
 */
object PriceAndPerpUtil {

    private const val TAG = "PriceAndPerpUtil"

    // Pyth Hermes — works for devnet price IDs
    private const val PYTH_HERMES = "https://hermes.pyth.network/v2/updates/price/latest"
    private const val SOL_PRICE_ID = "0xef0d8b6fda2ceba41da15d4095d1da392a0d2f8ed0c6c7bc0f4cfac8c280b56d"

    // ---- Price Feed ----

    /**
     * Fetch latest SOL/USD price from Pyth Hermes.
     * Falls back to a stale estimate if network is unavailable.
     */
    fun getSolPriceUsd(): Double {
        return try {
            val urlStr = "$PYTH_HERMES?ids[]=$SOL_PRICE_ID"
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8_000
            conn.readTimeout   = 8_000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Connection", "close")

            if (conn.responseCode != 200) {
                Log.w(TAG, "Pyth price feed returned ${conn.responseCode}, using fallback")
                return FALLBACK_SOL_PRICE
            }

            val body  = conn.inputStream.bufferedReader().use { it.readText() }
            val json  = JSONObject(body)
            val parsed = json.getJSONArray("parsed")
            if (parsed.length() == 0) return FALLBACK_SOL_PRICE

            val priceObj = parsed.getJSONObject(0).getJSONObject("price")
            val rawPrice = priceObj.getString("price").toLong()
            val exponent = priceObj.getInt("expo")

            val price = rawPrice * Math.pow(10.0, exponent.toDouble())
            Log.i(TAG, "Live SOL price: $$price")
            price
        } catch (e: Exception) {
            Log.w(TAG, "Price feed failed: ${e.message}. Using fallback $$FALLBACK_SOL_PRICE")
            FALLBACK_SOL_PRICE
        }
    }

    // ---- Perp Math (local simulation / Drift abstraction) ----

    /**
     * Calculate unrealized P&L for a perp position.
     * @param entryPrice    Price at which position was opened (USD)
     * @param currentPrice  Current price (USD)
     * @param sizeUsd       Notional size in USD
     * @param isLong        True = long, False = short
     * @param leverage      Leverage multiplier (1x–10x)
     */
    fun calcUnrealizedPnl(
        entryPrice: Double,
        currentPrice: Double,
        sizeUsd: Double,
        isLong: Boolean,
        leverage: Double
    ): Double {
        val priceDelta = if (isLong) {
            (currentPrice - entryPrice) / entryPrice
        } else {
            (entryPrice - currentPrice) / entryPrice
        }
        return priceDelta * sizeUsd * leverage
    }

    /**
     * Calculate liquidation price for a leveraged position.
     * Simple formula: entry ± (1/leverage) * entry
     */
    fun calcLiquidationPrice(entryPrice: Double, leverage: Double, isLong: Boolean): Double {
        val margin = entryPrice / leverage
        return if (isLong) entryPrice - margin else entryPrice + margin
    }

    /**
     * Calculate annualized return for staking position.
     * JitoSOL tracks ~8% APY average based on MEV + staking rewards.
     * On devnet we use a hardcoded simulation rate.
     */
    fun calcStakingApyPct(): Double = JITOSOL_SIMULATED_APY

    /**
     * Calculate accrued staking rewards for a given stake.
     * @param stakedLamports  Amount staked in lamports
     * @param openTimestamp   When the stake was opened (ms)
     * @param solPrice        Current SOL/USD price
     * @return Accrued rewards in lamports
     */
    fun calcAccruedStakeRewards(stakedLamports: Long, openTimestamp: Long, solPrice: Double): Long {
        val daysOpen = (System.currentTimeMillis() - openTimestamp) / (1000.0 * 60 * 60 * 24)
        val dailyRate = JITOSOL_SIMULATED_APY / 100.0 / 365.0
        return (stakedLamports * dailyRate * daysOpen).toLong()
    }

    private const val FALLBACK_SOL_PRICE  = 150.0  // Conservative fallback
    private const val JITOSOL_SIMULATED_APY = 7.82 // JitoSOL historical target
}
