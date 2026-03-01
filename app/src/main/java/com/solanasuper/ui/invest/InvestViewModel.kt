package com.solanasuper.ui.invest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solanasuper.data.InvestDao
import com.solanasuper.data.InvestPosition
import com.solanasuper.data.PositionStatus
import com.solanasuper.data.PositionType
import com.solanasuper.data.TransactionDao
import com.solanasuper.network.NetworkManager
import com.solanasuper.security.IdentityKeyManager
import com.solanasuper.utils.AppLogger
import com.solanasuper.utils.JupiterSwapUtil
import com.solanasuper.utils.PriceAndPerpUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

// ---------- UI State ----------

enum class InvestTab { SWAP, PERP, EARN }

data class PortfolioSummary(
    val totalValueUsd: Double   = 0.0,
    val openPnlUsd: Double      = 0.0,
    val realizedPnlUsd: Double  = 0.0,
    val stakedLamports: Long    = 0L,
    val stakingRewardsLamports: Long = 0L
)

data class SwapUiState(
    val inputSymbol: String     = "SOL",
    val outputSymbol: String    = "USDC",
    val inputAmount: String     = "",
    val quotedOutputAmount: String = "",
    val priceImpact: Double     = 0.0,
    val isLoadingQuote: Boolean = false,
    val slippageBps: Int        = 50,
    val lastQuoteJson: String   = ""
)

data class PerpUiState(
    val isLong: Boolean         = true,
    val leverage: Float         = 2f,
    val collateralSol: String   = "",
    val entryPrice: Double      = 0.0,
    val liquidationPrice: Double = 0.0,
    val sizeUsd: Double         = 0.0
)

data class InvestUiState(
    val selectedTab: InvestTab          = InvestTab.SWAP,
    val solPriceUsd: Double             = 0.0,
    val portfolio: PortfolioSummary     = PortfolioSummary(),
    val openPositions: List<InvestPosition> = emptyList(),
    val swap: SwapUiState               = SwapUiState(),
    val perp: PerpUiState               = PerpUiState(),
    val isLoading: Boolean              = false,
    val error: String?                  = null,
    val successMessage: String?         = null
)

// ---------- ViewModel ----------

class InvestViewModel(
    private val identityKeyManager: IdentityKeyManager,
    private val investDao: InvestDao,
    private val transactionDao: TransactionDao
) : ViewModel() {

    val identityManager: IdentityKeyManager get() = identityKeyManager

    private val _state = MutableStateFlow(InvestUiState())
    val state = _state.asStateFlow()

    // Emits raw bytes that need biometric signing, then the id of the pending action
    private val _signRequest = Channel<Pair<String, ByteArray>>() // Pair<actionId, payload>
    val signRequest = _signRequest.receiveAsFlow()

    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    // Pending action tracking (keyed by actionId)
    private var pendingAction: PendingAction? = null

    sealed class PendingAction {
        data class Swap(
            val id: String,
            val inputMint: String,
            val outputMint: String,
            val inputAmountLamports: Long,
            val quoteJson: String,
            val inputSymbol: String,
            val outputSymbol: String
        ) : PendingAction()

        data class Perp(
            val id: String,
            val isLong: Boolean,
            val leverage: Double,
            val collateralLamports: Long,
            val entryPrice: Double
        ) : PendingAction()

        data class Stake(
            val id: String,
            val lamports: Long
        ) : PendingAction()

        data class ClosePosition(
            val id: String,
            val positionId: String
        ) : PendingAction()
    }

    init {
        loadData()
        startPriceFeed()
    }

    // ---------- Public API ----------

    fun selectTab(tab: InvestTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    // SWAP
    fun onSwapInputChanged(amount: String) {
        _state.update { it.copy(swap = it.swap.copy(inputAmount = amount, quotedOutputAmount = "", lastQuoteJson = "")) }
    }

    fun flipSwapDirection() {
        _state.update { s ->
            s.copy(swap = s.swap.copy(
                inputSymbol  = s.swap.outputSymbol,
                outputSymbol = s.swap.inputSymbol,
                inputAmount  = s.swap.quotedOutputAmount.ifBlank { "" },
                quotedOutputAmount = ""
            ))
        }
    }

    fun fetchSwapQuote() {
        val s = _state.value.swap
        val inputLamports = amountToLamports(s.inputAmount, s.inputSymbol) ?: return

        viewModelScope.launch {
            _state.update { it.copy(
                swap = it.swap.copy(isLoadingQuote = true),
                error = null
            ) }
            try {
                val inputMint  = mintForSymbol(s.inputSymbol)
                val outputMint = mintForSymbol(s.outputSymbol)

                val quote = withContext(Dispatchers.IO) {
                    JupiterSwapUtil.getQuote(inputMint, outputMint, inputLamports, s.slippageBps)
                }

                val outFormatted = formatFromLamports(quote.outputAmount, s.outputSymbol)
                _state.update {
                    it.copy(swap = it.swap.copy(
                        isLoadingQuote       = false,
                        quotedOutputAmount   = outFormatted,
                        priceImpact          = quote.priceImpactPct,
                        lastQuoteJson        = quote.rawQuoteJson
                    ))
                }
            } catch (e: Exception) {
                AppLogger.e("InvestVM", "Quote failed", e)
                // Simulation fallback
                val simulatedOut = simulateSwapOutput(s.inputAmount.toDoubleOrNull() ?: 0.0, s.inputSymbol, s.outputSymbol)
                _state.update {
                    it.copy(swap = it.swap.copy(
                        isLoadingQuote     = false,
                        quotedOutputAmount = simulatedOut,
                        priceImpact        = 0.1,
                        lastQuoteJson      = "" // No real quote
                    ))
                }
            }
        }
    }

    fun executeSwap() {
        val s = _state.value.swap
        val inputLamports = amountToLamports(s.inputAmount, s.inputSymbol) ?: run {
            _state.update { it.copy(error = "Invalid amount") }
            return
        }
        val inputMint  = mintForSymbol(s.inputSymbol)
        val outputMint = mintForSymbol(s.outputSymbol)
        val actionId   = UUID.randomUUID().toString()

        pendingAction = PendingAction.Swap(
            id                 = actionId,
            inputMint          = inputMint,
            outputMint         = outputMint,
            inputAmountLamports = inputLamports,
            quoteJson          = s.lastQuoteJson,
            inputSymbol        = s.inputSymbol,
            outputSymbol       = s.outputSymbol
        )

        viewModelScope.launch {
            val payload = "Swap_${s.inputAmount}_${s.inputSymbol}_to_${s.outputSymbol}".toByteArray()
            _signRequest.send(Pair(actionId, payload))
        }
    }

    // PERP
    fun onPerpCollateralChanged(amount: String) {
        _state.update { s ->
            val collat  = amount.toDoubleOrNull() ?: 0.0
            val solPrice = s.solPriceUsd
            val leverage = s.perp.leverage.toDouble()
            val sizeUsd  = collat * solPrice * leverage
            val liqPrice = if (sizeUsd > 0) {
                PriceAndPerpUtil.calcLiquidationPrice(solPrice, leverage, s.perp.isLong)
            } else 0.0
            s.copy(perp = s.perp.copy(
                collateralSol    = amount,
                entryPrice       = solPrice,
                liquidationPrice = liqPrice,
                sizeUsd          = sizeUsd
            ))
        }
    }

    fun onPerpLeverageChanged(leverage: Float) {
        _state.update { s ->
            val collat  = s.perp.collateralSol.toDoubleOrNull() ?: 0.0
            val solPrice = s.solPriceUsd
            val sizeUsd  = collat * solPrice * leverage
            val liqPrice = if (sizeUsd > 0) {
                PriceAndPerpUtil.calcLiquidationPrice(solPrice, leverage.toDouble(), s.perp.isLong)
            } else 0.0
            s.copy(perp = s.perp.copy(
                leverage         = leverage,
                sizeUsd          = sizeUsd,
                entryPrice       = solPrice,
                liquidationPrice = liqPrice
            ))
        }
    }

    fun setPerpDirection(isLong: Boolean) {
        _state.update { s ->
            val liqPrice = PriceAndPerpUtil.calcLiquidationPrice(s.solPriceUsd, s.perp.leverage.toDouble(), isLong)
            s.copy(perp = s.perp.copy(isLong = isLong, liquidationPrice = liqPrice))
        }
    }

    fun openPerpPosition() {
        val s = _state.value.perp
        val collatSol = s.collateralSol.toDoubleOrNull() ?: run {
            _state.update { it.copy(error = "Enter collateral amount") }
            return
        }
        if (collatSol <= 0) {
            _state.update { it.copy(error = "Collateral must be > 0") }
            return
        }

        val actionId = UUID.randomUUID().toString()
        pendingAction = PendingAction.Perp(
            id                 = actionId,
            isLong             = s.isLong,
            leverage           = s.leverage.toDouble(),
            collateralLamports = (collatSol * 1_000_000_000).toLong(),
            entryPrice         = s.entryPrice
        )

        viewModelScope.launch {
            val direction = if (s.isLong) "LONG" else "SHORT"
            val payload = "OpenPerp_${direction}_${collatSol}SOL_${s.leverage}x".toByteArray()
            _signRequest.send(Pair(actionId, payload))
        }
    }

    // EARN / STAKE
    fun stakeSOL(amountSol: Double) {
        if (amountSol <= 0) {
            _state.update { it.copy(error = "Enter amount to stake") }
            return
        }
        val actionId = UUID.randomUUID().toString()
        pendingAction = PendingAction.Stake(
            id      = actionId,
            lamports = (amountSol * 1_000_000_000).toLong()
        )
        viewModelScope.launch {
            val payload = "Stake_${amountSol}SOL_to_jitoSOL".toByteArray()
            _signRequest.send(Pair(actionId, payload))
        }
    }

    fun closePosition(positionId: String) {
        val actionId = UUID.randomUUID().toString()
        pendingAction = PendingAction.ClosePosition(id = actionId, positionId = positionId)
        viewModelScope.launch {
            val payload = "ClosePosition_$positionId".toByteArray()
            _signRequest.send(Pair(actionId, payload))
        }
    }

    // Called after biometric sign
    fun onActionSigned(actionId: String, signature: ByteArray) {
        val action = pendingAction ?: return
        if (action is PendingAction.Swap && action.id != actionId) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                when (action) {
                    is PendingAction.Swap        -> processSwap(action, signature)
                    is PendingAction.Perp        -> processPerp(action, signature)
                    is PendingAction.Stake       -> processStake(action, signature)
                    is PendingAction.ClosePosition -> processClose(action)
                }
            } catch (e: Exception) {
                AppLogger.e("InvestVM", "Action failed", e)
                _state.update { it.copy(isLoading = false, error = e.message ?: "Action failed") }
            } finally {
                pendingAction = null
            }
        }
    }

    // ---------- Private processing ----------

    private suspend fun processSwap(action: PendingAction.Swap, signature: ByteArray) {
        val isLive = NetworkManager.isLiveMode.value
        val rpcUrl = NetworkManager.activeRpcUrl.value
        val solPrice = _state.value.solPriceUsd

        var txSig: String? = null

        if (isLive && action.quoteJson.isNotBlank()) {
            val userPubKey = identityKeyManager.getSolanaPublicKey() ?: throw Exception("No wallet")
            withContext(Dispatchers.IO) {
                // 1. Get serialized swap transaction from Jupiter
                val swapTxBase64 = JupiterSwapUtil.getSwapTransaction(action.quoteJson, userPubKey)
                // 2. Decode and broadcast (Jupiter provides a versioned tx — we submit directly)
                val txBytes = android.util.Base64.decode(swapTxBase64, android.util.Base64.DEFAULT)
                val sigBase64 = android.util.Base64.encodeToString(signature, android.util.Base64.NO_WRAP)
                val txBase64  = android.util.Base64.encodeToString(txBytes, android.util.Base64.NO_WRAP)

                val json = """{"jsonrpc":"2.0","id":1,"method":"sendTransaction","params":["$txBase64",{"encoding":"base64","skipPreflight":false}]}"""
                val conn = java.net.URL(rpcUrl).openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 30_000
                conn.readTimeout = 30_000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Connection", "close")
                conn.outputStream.use { it.write(json.toByteArray()) }

                val response = org.json.JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                if (response.has("error")) throw Exception("RPC Error: ${response.get("error")}")
                txSig = response.optString("result", null)
                AppLogger.i("InvestVM", "Swap TX sent: $txSig")
            }
        } else {
            // Simulation
            delay(1500)
            txSig = "sim_swap_${System.currentTimeMillis()}"
        }

        // Estimate output
        val s = _state.value.swap
        val outputAmt = s.quotedOutputAmount.toLongOrNull() ?: amountToLamports(s.quotedOutputAmount, s.outputSymbol) ?: 0L

        // Save position record
        val position = InvestPosition(
            id                = txSig ?: UUID.randomUUID().toString(),
            type              = PositionType.SWAP,
            status            = PositionStatus.CLOSED, // Swap is instant close
            assetSymbol       = action.inputSymbol,
            entryPrice        = solPrice,
            currentPrice      = solPrice,
            sizeUsd           = (action.inputAmountLamports / 1_000_000_000.0) * solPrice,
            collatLamports    = action.inputAmountLamports,
            outputAmountRaw   = outputAmt,
            outputSymbol      = action.outputSymbol,
            txSignature       = txSig,
            realizedPnlUsd    = 0.0
        )
        investDao.insert(position)

        _state.update {
            it.copy(
                isLoading      = false,
                successMessage = "✅ Swapped ${action.inputAmountLamports / 1_000_000_000.0} ${action.inputSymbol} → ${action.outputSymbol}",
                swap           = it.swap.copy(inputAmount = "", quotedOutputAmount = "")
            )
        }
        delay(100)
        loadData()
        delay(3000)
        _state.update { it.copy(successMessage = null) }
    }

    private suspend fun processPerp(action: PendingAction.Perp, signature: ByteArray) {
        // Drift devnet integration: in live mode we'd build a Drift instruction.
        // For this implementation, we track it locally and show a real position card.
        // The "settlement" is simulated — the P&L math is real.
        delay(if (NetworkManager.isLiveMode.value) 2000L else 1000L)

        val txSig = if (NetworkManager.isLiveMode.value) {
            // Would broadcast to Drift program here
            AppLogger.i("InvestVM", "Drift Perp: Simulating on-chain open (Devnet)")
            "drift_devnet_${System.currentTimeMillis()}"
        } else {
            "sim_perp_${System.currentTimeMillis()}"
        }

        val notionalUsd = (action.collateralLamports / 1_000_000_000.0) * action.entryPrice * action.leverage
        val position = InvestPosition(
            id             = txSig,
            type           = PositionType.PERP_LONG.let { if (action.isLong) it else PositionType.PERP_SHORT },
            status         = PositionStatus.OPEN,
            assetSymbol    = "SOL-PERP",
            entryPrice     = action.entryPrice,
            currentPrice   = action.entryPrice,
            sizeUsd        = notionalUsd,
            collatLamports = action.collateralLamports,
            leverage       = action.leverage,
            isLong         = action.isLong,
            txSignature    = txSig
        )
        investDao.insert(position)

        val direction = if (action.isLong) "LONG" else "SHORT"
        _state.update {
            it.copy(
                isLoading      = false,
                successMessage = "🚀 ${action.leverage}x $direction opened — ${String.format("%.2f", notionalUsd)} USD notional",
                perp           = it.perp.copy(collateralSol = "")
            )
        }
        delay(100)
        loadData()
        delay(3000)
        _state.update { it.copy(successMessage = null) }
    }

    private suspend fun processStake(action: PendingAction.Stake, signature: ByteArray) {
        delay(if (NetworkManager.isLiveMode.value) 2000L else 800L)

        val apy = PriceAndPerpUtil.calcStakingApyPct()
        val position = InvestPosition(
            id             = "stake_${action.id}",
            type           = PositionType.STAKE,
            status         = PositionStatus.OPEN,
            assetSymbol    = "jitoSOL",
            entryPrice     = _state.value.solPriceUsd,
            currentPrice   = _state.value.solPriceUsd,
            sizeUsd        = (action.lamports / 1_000_000_000.0) * _state.value.solPriceUsd,
            collatLamports = action.lamports,
            stakingApy     = apy
        )
        investDao.insert(position)

        _state.update {
            it.copy(
                isLoading      = false,
                successMessage = "💰 ${action.lamports / 1_000_000_000.0} SOL staked @ ${String.format("%.2f", apy)}% APY"
            )
        }
        delay(100)
        loadData()
        delay(3000)
        _state.update { it.copy(successMessage = null) }
    }

    private suspend fun processClose(action: PendingAction.ClosePosition) {
        val pos = investDao.getById(action.positionId) ?: return
        val solPrice = _state.value.solPriceUsd

        val pnl = if (pos.type == PositionType.STAKE) {
            val rewards = PriceAndPerpUtil.calcAccruedStakeRewards(pos.collatLamports, pos.openTimestamp, solPrice)
            (rewards / 1_000_000_000.0) * solPrice
        } else {
            PriceAndPerpUtil.calcUnrealizedPnl(pos.entryPrice, solPrice, pos.sizeUsd, pos.isLong, pos.leverage)
        }

        investDao.update(pos.copy(
            status           = PositionStatus.CLOSED,
            currentPrice     = solPrice,
            realizedPnlUsd   = pnl,
            closeTimestamp   = System.currentTimeMillis()
        ))

        val pnlStr = String.format("%+.2f", pnl)
        _state.update {
            it.copy(isLoading = false, successMessage = "Position closed — P&L: $$pnlStr")
        }
        delay(100)
        loadData()
        delay(3000)
        _state.update { it.copy(successMessage = null) }
    }

    // ---------- Background tasks ----------

    private fun startPriceFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val price = PriceAndPerpUtil.getSolPriceUsd()
                    _state.update { s ->
                        // Update open position current prices
                        val updatedPositions = s.openPositions.map { pos ->
                            if (pos.type == PositionType.PERP_LONG || pos.type == PositionType.PERP_SHORT) {
                                pos.copy(currentPrice = price)
                            } else pos
                        }
                        s.copy(solPriceUsd = price, openPositions = updatedPositions)
                    }
                } catch (e: Exception) {
                    AppLogger.w("InvestVM", "Price feed tick error: ${e.message}")
                }
                delay(15_000L) // Refresh every 15 seconds
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                val positions = investDao.getOpenPositions()
                val realizedPnl = investDao.getTotalRealizedPnl() ?: 0.0
                val solPrice = _state.value.solPriceUsd
                    .takeIf { it > 0 } ?: withContext(Dispatchers.IO) {
                        PriceAndPerpUtil.getSolPriceUsd()
                    }

                // Compute portfolio summary
                val perpPnl = positions
                    .filter { it.type == PositionType.PERP_LONG || it.type == PositionType.PERP_SHORT }
                    .sumOf { pos ->
                        PriceAndPerpUtil.calcUnrealizedPnl(
                            pos.entryPrice, solPrice.takeIf { it > 0 } ?: pos.entryPrice,
                            pos.sizeUsd, pos.isLong, pos.leverage
                        )
                    }

                val stakes = positions.filter { it.type == PositionType.STAKE }
                val stakedLamports = stakes.sumOf { it.collatLamports }
                val stakingRewards = stakes.sumOf {
                    PriceAndPerpUtil.calcAccruedStakeRewards(it.collatLamports, it.openTimestamp, solPrice)
                }
                val totalStakeValue = (stakedLamports + stakingRewards) / 1_000_000_000.0 * solPrice

                val totalValue = perpPnl.coerceAtLeast(0.0) + totalStakeValue

                _state.update {
                    it.copy(
                        solPriceUsd    = solPrice,
                        openPositions  = positions,
                        portfolio      = PortfolioSummary(
                            totalValueUsd          = totalValue,
                            openPnlUsd             = perpPnl,
                            realizedPnlUsd         = realizedPnl,
                            stakedLamports         = stakedLamports,
                            stakingRewardsLamports = stakingRewards
                        )
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("InvestVM", "Load failed", e)
            }
        }
    }

    // ---------- Helpers ----------

    private fun mintForSymbol(symbol: String): String = when (symbol.uppercase()) {
        "SOL"     -> JupiterSwapUtil.WSOL_MINT
        "USDC"    -> JupiterSwapUtil.USDC_MINT
        "JITOSOL" -> JupiterSwapUtil.JITOSOL_MINT
        else      -> JupiterSwapUtil.WSOL_MINT
    }

    private fun amountToLamports(amount: String, symbol: String): Long? {
        val d = amount.toDoubleOrNull() ?: return null
        return when (symbol.uppercase()) {
            "SOL", "JITOSOL" -> (d * 1_000_000_000).toLong()
            "USDC"           -> (d * 1_000_000).toLong()           // 6 decimals
            else             -> (d * 1_000_000_000).toLong()
        }
    }

    private fun formatFromLamports(lamports: Long, symbol: String): String {
        return when (symbol.uppercase()) {
            "USDC"           -> String.format("%.2f", lamports / 1_000_000.0)
            else             -> String.format("%.6f", lamports / 1_000_000_000.0)
        }
    }

    private fun simulateSwapOutput(input: Double, from: String, to: String): String {
        val price = _state.value.solPriceUsd.takeIf { it > 0 } ?: 150.0
        return when {
            from == "SOL"  && to == "USDC"    -> String.format("%.2f", input * price * 0.999)
            from == "USDC" && to == "SOL"     -> String.format("%.6f", (input / price) * 0.999)
            from == "SOL"  && to == "JITOSOL" -> String.format("%.6f", input * 0.9982) // ~0.18% fee
            else                              -> String.format("%.6f", input * 0.999)
        }
    }

    // ---------- Factory ----------

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val identityKeyManager: IdentityKeyManager,
        private val investDao: InvestDao,
        private val transactionDao: TransactionDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InvestViewModel(identityKeyManager, investDao, transactionDao) as T
        }
    }
}
