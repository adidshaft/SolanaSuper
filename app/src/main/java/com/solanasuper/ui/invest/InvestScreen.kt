package com.solanasuper.ui.invest

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanasuper.data.InvestPosition
import com.solanasuper.data.PositionType
import com.solanasuper.network.NetworkManager
import com.solanasuper.utils.PriceAndPerpUtil

// --------- Design Tokens ---------
private val SolanaGreen  = Color(0xFF14F195)
private val SolanaPurple = Color(0xFF9945FF)
private val SolanaBlue   = Color(0xFF00C2FF)
private val BullGold     = Color(0xFFF4A921)
private val BearRed      = Color(0xFFCF6679)
private val CardBg       = Color(0xFF0F1117)
private val CardBorder   = Color(0xFF1E2230)
private val BgDark       = Color(0xFF060810)

@Composable
fun InvestScreen(viewModel: InvestViewModel) {
    val state by viewModel.state.collectAsState()
    val isLive by NetworkManager.isLiveMode.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Biometric sign observer
    LaunchedEffect(Unit) {
        viewModel.signRequest.collect { (actionId, payload) ->
            try {
                val activity = context as? androidx.fragment.app.FragmentActivity
                if (activity != null) {
                    val signature = viewModel.identityManager.signTransaction(activity, payload)
                    viewModel.onActionSigned(actionId, signature)
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Auth Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ── Header ──
        item {
            InvestHeader(state, isLive)
        }

        // ── Tab Switcher ──
        item {
            SovereignTabBar(
                selected = state.selectedTab,
                onSelect = viewModel::selectTab
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Status Messages ──
        state.error?.let { err ->
            item {
                StatusBanner(err, isError = true)
                Spacer(Modifier.height(8.dp))
            }
        }
        state.successMessage?.let { msg ->
            item {
                StatusBanner(msg, isError = false)
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Content per tab ──
        item {
            AnimatedContent(
                targetState = state.selectedTab,
                transitionSpec = { fadeIn() + slideInHorizontally() togetherWith fadeOut() + slideOutHorizontally() },
                label = "tab"
            ) { tab ->
                when (tab) {
                    InvestTab.SWAP -> SwapCard(state, viewModel, isLive)
                    InvestTab.PERP -> PerpCard(state, viewModel, isLive)
                    InvestTab.EARN -> EarnCard(state, viewModel, isLive)
                }
            }
        }

        // ── Open Positions ──
        if (state.openPositions.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    "OPEN POSITIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.35f),
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(10.dp))
            }
            items(state.openPositions) { pos ->
                PositionCard(pos, state.solPriceUsd) { viewModel.closePosition(pos.id) }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Loading ──
        if (state.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = SolanaGreen, modifier = Modifier.size(36.dp), strokeWidth = 2.dp)
                        Text("Executing sovereign transaction...", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Text(
                "Powered by Jupiter · Pyth · Drift | Devnet",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.12f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)
            )
        }
    }
}

// ── Header / Portfolio Card ──
@Composable
private fun InvestHeader(state: InvestUiState, isLive: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0B0F1A),
                        SolanaPurple.copy(alpha = 0.18f),
                        SolanaGreen.copy(alpha = 0.10f),
                        Color(0xFF0B0F1A)
                    )
                )
            )
            .border(1.dp, SolanaPurple.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("SOVEREIGN EXCHANGE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.35f), letterSpacing = 3.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Invest", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("SOL/USD", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                    Text(
                        "$ ${String.format("%.2f", state.solPriceUsd)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = SolanaGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider(color = Color.White.copy(alpha = 0.06f))
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PortfolioMetric("Open P&L", formatPnl(state.portfolio.openPnlUsd), pnlColor(state.portfolio.openPnlUsd), Modifier.weight(1f))
                PortfolioMetric("Realized P&L", formatPnl(state.portfolio.realizedPnlUsd), pnlColor(state.portfolio.realizedPnlUsd), Modifier.weight(1f))
                PortfolioMetric("Staked", "${String.format("%.3f", state.portfolio.stakedLamports / 1_000_000_000.0)} SOL", SolanaPurple, Modifier.weight(1f))
            }

            if (state.portfolio.stakingRewardsLamports > 0) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(6.dp).background(SolanaGreen, CircleShape))
                    Text(
                        "Accrued Staking Rewards: ${String.format("%.6f", state.portfolio.stakingRewardsLamports / 1_000_000_000.0)} SOL",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolanaGreen.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PortfolioMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

// ── Tab Bar ──
@Composable
private fun SovereignTabBar(selected: InvestTab, onSelect: (InvestTab) -> Unit) {
    val tabs = listOf(
        InvestTab.SWAP to ("⚡ SWAP" to SolanaGreen),
        InvestTab.PERP to ("📈 PERP" to BullGold),
        InvestTab.EARN to ("💰 EARN" to SolanaPurple)
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (tab, labelColor) ->
            val (label, color) = labelColor
            val isSelected = selected == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) color.copy(alpha = 0.15f) else CardBg)
                    .border(1.dp, if (isSelected) color.copy(alpha = 0.4f) else CardBorder, RoundedCornerShape(12.dp))
                    .clickable { onSelect(tab) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) color else Color.White.copy(alpha = 0.4f),
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ── Swap Card ──
@Composable
private fun SwapCard(state: InvestUiState, viewModel: InvestViewModel, isLive: Boolean) {
    val swap = state.swap

    GlassCard(Modifier.padding(horizontal = 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Token Swap", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                if (isLive) "Live Jupiter V6 • Devnet" else "Simulation Mode",
                style = MaterialTheme.typography.labelSmall,
                color = (if (isLive) SolanaGreen else SolanaBlue).copy(alpha = 0.8f)
            )

            // Input token
            TokenAmountField(
                label    = "You Pay (${swap.inputSymbol})",
                value    = swap.inputAmount,
                onChange = viewModel::onSwapInputChanged,
                color    = SolanaGreen
            )

            // Flip button
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(CardBorder, CircleShape)
                        .border(1.dp, SolanaGreen.copy(alpha = 0.3f), CircleShape)
                        .clickable { viewModel.flipSwapDirection() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Refresh, null, tint = SolanaGreen, modifier = Modifier.size(18.dp))
                }
            }

            // Output token / Quote
            Column {
                Text("You Receive (${swap.outputSymbol})", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    if (swap.isLoadingQuote) {
                        CircularProgressIndicator(Modifier.size(20.dp).align(Alignment.CenterStart), color = SolanaGreen, strokeWidth = 2.dp)
                    } else {
                        Text(
                            swap.quotedOutputAmount.ifBlank { "—" },
                            style = MaterialTheme.typography.titleLarge,
                            color = if (swap.quotedOutputAmount.isNotBlank()) SolanaGreen else Color.White.copy(alpha = 0.2f),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (swap.priceImpact > 0.1) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "⚠ Price Impact: ${String.format("%.2f", swap.priceImpact)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = BearRed.copy(alpha = 0.8f)
                    )
                }
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick  = { viewModel.fetchSwapQuote() },
                    modifier = Modifier.weight(1f),
                    enabled  = swap.inputAmount.toDoubleOrNull() != null,
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.dp, SolanaGreen.copy(alpha = 0.4f))
                ) {
                    Text("Get Quote", color = SolanaGreen, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick  = { viewModel.executeSwap() },
                    modifier = Modifier.weight(1f),
                    enabled  = swap.quotedOutputAmount.isNotBlank() && !state.isLoading,
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = SolanaGreen, disabledContainerColor = SolanaGreen.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("Swap", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
            }

            Text(
                "Slippage: ${swap.slippageBps / 100.0}% · Requires biometric signature",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.25f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Perp Card ──
@Composable
private fun PerpCard(state: InvestUiState, viewModel: InvestViewModel, isLive: Boolean) {
    val perp = state.perp
    val isLong = perp.isLong
    val directionColor = if (isLong) BullGold else BearRed

    GlassCard(Modifier.padding(horizontal = 16.dp), borderColor = directionColor.copy(alpha = 0.2f)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Perpetuals (SOL-PERP)", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        if (isLive) "Drift Protocol • Devnet" else "Simulation Mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = (if (isLive) BullGold else SolanaBlue).copy(alpha = 0.8f)
                    )
                }
                Text(
                    "Entry: $${String.format("%.2f", state.solPriceUsd)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            // Direction selector
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DirectionButton("🟢 LONG", isLong, BullGold, Modifier.weight(1f)) { viewModel.setPerpDirection(true) }
                DirectionButton("🔴 SHORT", !isLong, BearRed, Modifier.weight(1f)) { viewModel.setPerpDirection(false) }
            }

            // Leverage slider
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Leverage", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.sp)
                    Text(
                        "${String.format("%.1f", perp.leverage)}x",
                        style = MaterialTheme.typography.labelLarge,
                        color = directionColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Slider(
                    value    = perp.leverage,
                    onValueChange = viewModel::onPerpLeverageChanged,
                    valueRange = 1f..10f,
                    steps  = 17,
                    colors = SliderDefaults.colors(
                        thumbColor       = directionColor,
                        activeTrackColor = directionColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("1x", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f))
                    Text("10x", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f))
                }
            }

            // Collateral input
            TokenAmountField("Collateral (SOL)", perp.collateralSol, viewModel::onPerpCollateralChanged, directionColor)

            // Position metrics
            if (perp.sizeUsd > 0) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(directionColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .border(1.dp, directionColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PerpMetricRow("Notional Size", "$ ${String.format("%.2f", perp.sizeUsd)}")
                    PerpMetricRow("Entry Price", "$ ${String.format("%.2f", perp.entryPrice)}")
                    PerpMetricRow(
                        "Liq. Price",
                        "$ ${String.format("%.2f", perp.liquidationPrice)}",
                        BearRed.copy(alpha = 0.9f)
                    )
                }
            }

            Button(
                onClick  = { viewModel.openPerpPosition() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = perp.collateralSol.toDoubleOrNull() != null && !state.isLoading,
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = directionColor,
                    disabledContainerColor = directionColor.copy(alpha = 0.15f)
                )
            ) {
                Text(
                    "${if (isLong) "🟢 OPEN LONG" else "🔴 OPEN SHORT"} · ${String.format("%.1f", perp.leverage)}x",
                    color = if (isLong) Color.Black else Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            Text(
                "Sovereign warning: Leverage amplifies losses. Your keys, your risk.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.22f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Earn Card ──
@Composable
private fun EarnCard(state: InvestUiState, viewModel: InvestViewModel, isLive: Boolean) {
    var stakeAmountStr by remember { mutableStateOf("") }
    val apy = PriceAndPerpUtil.calcStakingApyPct()
    val stakeAmount = stakeAmountStr.toDoubleOrNull() ?: 0.0
    val projectedYearlyUsdc = stakeAmount * state.solPriceUsd * (apy / 100.0)

    GlassCard(Modifier.padding(horizontal = 16.dp), borderColor = SolanaPurple.copy(alpha = 0.2f)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("Liquid Staking", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        if (isLive) "jitoSOL (Simulated APY)" else "Simulation Mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolanaPurple.copy(alpha = 0.8f)
                    )
                }
                // APY badge
                Box(
                    Modifier
                        .background(SolanaGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .border(1.dp, SolanaGreen.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("APY", style = MaterialTheme.typography.labelSmall, color = SolanaGreen.copy(alpha = 0.7f), fontSize = 9.sp)
                        Text("${String.format("%.2f", apy)}%", style = MaterialTheme.typography.titleMedium, color = SolanaGreen, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // What you get
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(SolanaPurple.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .border(1.dp, SolanaPurple.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PerpMetricRow("Asset", "Jito Staked SOL (jitoSOL)")
                PerpMetricRow("Rewards", "MEV + Proof-of-Stake")
                PerpMetricRow("Liquidity", "Instant unstake (liquid)")
                PerpMetricRow("Risk", "Smart contract (low)")
            }

            TokenAmountField("Stake Amount (SOL)", stakeAmountStr, { stakeAmountStr = it }, SolanaPurple)

            if (stakeAmount > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(SolanaGreen.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Projected Earnings", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.45f))
                        Text(
                            "≈ ${String.format("%.2f", projectedYearlyUsdc)} USD / year",
                            style = MaterialTheme.typography.titleMedium,
                            color = SolanaGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "≈ ${String.format("%.4f", stakeAmount * (apy / 100.0))} SOL / year",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Button(
                onClick  = { viewModel.stakeSOL(stakeAmount) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = stakeAmount > 0.001 && !state.isLoading,
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SolanaPurple, disabledContainerColor = SolanaPurple.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Default.Star, null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Stake SOL → jitoSOL", color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ── Open Position Card ──
@Composable
private fun PositionCard(position: InvestPosition, solPrice: Double, onClose: () -> Unit) {
    val isPerp = position.type == PositionType.PERP_LONG || position.type == PositionType.PERP_SHORT
    val isStake = position.type == PositionType.STAKE

    val pnl = when {
        isPerp  -> PriceAndPerpUtil.calcUnrealizedPnl(
            position.entryPrice,
            solPrice.takeIf { it > 0 } ?: position.entryPrice,
            position.sizeUsd, position.isLong, position.leverage
        )
        isStake -> {
            val rewards = PriceAndPerpUtil.calcAccruedStakeRewards(position.collatLamports, position.openTimestamp, solPrice)
            (rewards / 1_000_000_000.0) * solPrice
        }
        else -> 0.0
    }
    val typeColor = when {
        position.type == PositionType.PERP_LONG  -> BullGold
        position.type == PositionType.PERP_SHORT -> BearRed
        position.type == PositionType.STAKE      -> SolanaPurple
        else                                     -> SolanaBlue
    }
    val typeLabel = when (position.type) {
        PositionType.PERP_LONG  -> "⬆ LONG"
        PositionType.PERP_SHORT -> "⬇ SHORT"
        PositionType.STAKE      -> "💰 STAKE"
        PositionType.SWAP       -> "⚡ SWAP"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, typeColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            Modifier
                .size(44.dp)
                .background(typeColor.copy(alpha = 0.12f), CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(typeLabel.take(1), color = typeColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(position.assetSymbol, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                if (isPerp) {
                    Box(
                        Modifier.background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)
                    ) { Text("${String.format("%.1f", position.leverage)}x", style = MaterialTheme.typography.labelSmall, color = typeColor, fontWeight = FontWeight.Bold) }
                }
            }
            if (isPerp) {
                Text("Entry $${String.format("%.2f", position.entryPrice)}", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall)
            }
            if (isStake) {
                Text("${String.format("%.3f", position.collatLamports / 1_000_000_000.0)} SOL @ ${String.format("%.2f", position.stakingApy)}% APY",
                    color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatPnl(pnl), color = pnlColor(pnl), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onClose,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = BearRed.copy(alpha = 0.8f))
            ) {
                Text("Close", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Shared Composables ──

@Composable
private fun GlassCard(modifier: Modifier = Modifier, borderColor: Color = CardBorder, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors = listOf(CardBg, Color(0xFF0A0E18))))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun TokenAmountField(label: String, value: String, onChange: (String) -> Unit, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onChange(it) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = androidx.compose.ui.text.TextStyle(
                color       = Color.White,
                fontWeight  = FontWeight.Bold,
                fontSize    = 18.sp,
                fontFamily  = FontFamily.Monospace
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = color,
                unfocusedBorderColor = CardBorder,
                cursorColor          = color,
                focusedContainerColor   = Color.White.copy(alpha = 0.02f),
                unfocusedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun DirectionButton(label: String, isSelected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
            .border(1.dp, if (isSelected) color.copy(alpha = 0.5f) else CardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (isSelected) color else Color.White.copy(alpha = 0.35f), fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal)
    }
}

@Composable
private fun PerpMetricRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.4f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusBanner(message: String, isError: Boolean) {
    val color = if (isError) BearRed else SolanaGreen
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            if (isError) Icons.Default.Warning else Icons.Default.CheckCircle,
            null, tint = color, modifier = Modifier.size(18.dp)
        )
        Text(message, color = color, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}

// ── Helpers ──
private fun pnlColor(pnl: Double) = when {
    pnl > 0  -> SolanaGreen
    pnl < 0  -> BearRed
    else     -> Color.White.copy(alpha = 0.5f)
}

private fun formatPnl(pnl: Double): String {
    val abs = kotlin.math.abs(pnl)
    return if (pnl >= 0) "+$${String.format("%.2f", abs)}" else "-$${String.format("%.2f", abs)}"
}
