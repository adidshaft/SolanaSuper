package com.solanasuper.ui.governance

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanasuper.ui.components.MpcLoadingOverlay
import com.solanasuper.ui.state.ArciumComputationState

private val SolanaGreen  = Color(0xFF14F195)
private val SolanaPurple = Color(0xFF9945FF)
private val SolanaBlue   = Color(0xFF00C2FF)
private val CardBg = Color(0xFF0F1117)
private val CardBorder = Color(0xFF1E2230)

@Composable
fun GovernanceScreen(viewModel: GovernanceViewModel) {
    val state by viewModel.state.collectAsState()

    if (state.mpcState != ArciumComputationState.IDLE) {
        MpcLoadingOverlay(state.mpcState)
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                Text(
                    "DEMOCRACY",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.35f),
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Governance",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Vote anonymously via Zero-Knowledge Proofs. Your identity stays sovereign.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    lineHeight = 22.sp
                )
            }
        }

        itemsIndexed(state.proposals) { index, proposal ->
            ProposalCard(
                proposal = proposal,
                isActive = index == state.activeProposalIndex,
                onClick = { viewModel.selectProposal(index) },
                onVoteYes = { viewModel.initiateVote("YES") },
                onVoteNo = { viewModel.initiateVote("NO") },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Vote status message
        if (state.voteStatus.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    val isError = state.voteStatus.contains("Error") || state.voteStatus.contains("Failed") || state.voteStatus.contains("Rejected")
                    Text(
                        text = state.voteStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isError) Color(0xFFCF6679) else SolanaGreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProposalCard(
    proposal: Proposal,
    isActive: Boolean,
    onClick: () -> Unit,
    onVoteYes: () -> Unit,
    onVoteNo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalVotes = (proposal.yesVotes + proposal.noVotes).toFloat().coerceAtLeast(1f)
    val yesPct = proposal.yesVotes / totalVotes
    val noPct = proposal.noVotes / totalVotes

    val borderColor by animateColorAsState(
        targetValue = if (isActive) SolanaGreen.copy(alpha = 0.4f) else CardBorder,
        label = "border"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Proposal label badge
                Box(
                    modifier = Modifier
                        .background(SolanaGreen.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(proposal.label, style = MaterialTheme.typography.labelSmall, color = SolanaGreen, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                if (proposal.hasVoted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = SolanaGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Voted ${proposal.userVote}", style = MaterialTheme.typography.labelSmall, color = SolanaGreen)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(proposal.title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(proposal.description, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f), lineHeight = 22.sp)

            // Expanded voting UI
            AnimatedVisibility(visible = isActive) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    // Vote tally bars
                    VoteTallyBar("YES", proposal.yesVotes, yesPct, SolanaGreen)
                    Spacer(Modifier.height(8.dp))
                    VoteTallyBar("NO", proposal.noVotes, noPct, Color(0xFFCF6679))
                    Spacer(Modifier.height(20.dp))

                    if (!proposal.hasVoted) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onVoteYes,
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SolanaGreen)
                            ) {
                                Text("VOTE YES", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                            }
                            OutlinedButton(
                                onClick = onVoteNo,
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFCF6679).copy(alpha = 0.6f))
                            ) {
                                Text("VOTE NO", color = Color(0xFFCF6679), fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Voting requires biometric auth + ZK proof generation",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.3f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoteTallyBar(label: String, count: Int, fraction: Float, barColor: Color) {
    val animFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "bar"
    )
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = barColor, fontWeight = FontWeight.Bold)
            Text("$count votes · ${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animFraction)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.7f), barColor)),
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}
