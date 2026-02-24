package com.solanasuper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PositionType { PERP_LONG, PERP_SHORT, SWAP, STAKE }
enum class PositionStatus { OPEN, CLOSED, PENDING }

@Entity(tableName = "invest_positions")
data class InvestPosition(
    @PrimaryKey val id: String,
    val type: PositionType,
    val status: PositionStatus,
    val assetSymbol: String,         // e.g. "SOL", "USDC", "jitoSOL"
    val entryPrice: Double,           // USD price at open
    val currentPrice: Double = 0.0,
    val sizeUsd: Double,              // notional size in USD
    val collatLamports: Long,         // collateral for perps, input tokens for swaps
    val leverage: Double = 1.0,       // 1.0 for spot/stake, n for perp
    val isLong: Boolean = true,       // direction for perps
    val openTimestamp: Long = System.currentTimeMillis(),
    val closeTimestamp: Long? = null,
    val realizedPnlUsd: Double = 0.0,
    val txSignature: String? = null,
    val outputAmountRaw: Long = 0L,   // for swap: output token amount received
    val outputSymbol: String = "",    // for swap: output token symbol
    val stakingApy: Double = 0.0      // for stake positions: annual APY in %
)
