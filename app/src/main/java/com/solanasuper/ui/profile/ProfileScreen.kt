package com.solanasuper.ui.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanasuper.data.*
import java.text.SimpleDateFormat
import java.util.*

private val SolanaGreen  = Color(0xFF14F195)
private val SolanaPurple = Color(0xFF9945FF)
private val SolanaBlue   = Color(0xFF00C2FF)
private val CardBg       = Color(0xFF0F1117)
private val CardBorder   = Color(0xFF1E2230)

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onShowSnackbar: (String) -> Unit) {
    val activities by viewModel.activities.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val address = viewModel.solanaAddress
    val score = viewModel.sovereigntyScore

    var showMnemonic by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                Text("PROFILE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.35f), letterSpacing = 3.sp)
                Spacer(Modifier.height(4.dp))
                Text("Sovereign Identity", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Sovereignty Score Arc card
        item {
            SovereigntyScoreCard(score = score, address = address, activities = activities, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(16.dp))
        }

        // Wallet Address Card
        item {
            WalletAddressCard(address = address, onCopy = {
                clipboardManager.setText(AnnotatedString(address))
                onShowSnackbar("Address copied to clipboard")
            })
            Spacer(Modifier.height(16.dp))
        }

        // Backup Mnemonic row
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { showMnemonic = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, SolanaPurple.copy(alpha = 0.2f))
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        Modifier.size(44.dp).background(SolanaPurple.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Lock, null, tint = SolanaPurple, modifier = Modifier.size(20.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text("Backup Seed Phrase", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Text("12-word BIP39 mnemonic · Biometric protected", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ArrowForward, null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Activity Ledger Header
        item {
            Text("ACTIVITY LEDGER", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.35f), letterSpacing = 3.sp, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(12.dp))
        }

        if (activities.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Start transacting to build your cryptographic activity ledger.", color = Color.White.copy(alpha = 0.25f), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            items(activities) { ActivityItem(it) }
        }
    }

    if (showMnemonic) {
        MnemonicDialog(viewModel = viewModel, onDismiss = { showMnemonic = false })
    }
}

@Composable
private fun SovereigntyScoreCard(score: Int, address: String, activities: List<ActivityLogEntity>, modifier: Modifier = Modifier) {
    val hasWallet = address != "Addr_Not_Found"
    val hasTx = activities.any { it.type == ActivityType.SOLANA_TX }
    val hasProof = activities.any { it.type == ActivityType.ARCIUM_PROOF }
    val hasIpfs = activities.any { it.type == ActivityType.IPFS_HASH }

    val animScore by animateIntAsState(targetValue = score, animationSpec = tween(1200, easing = FastOutSlowInEasing), label = "score")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, SolanaGreen.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                // Animated arc
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                    val sweep = (animScore / 100f) * 240f
                    androidx.compose.foundation.Canvas(Modifier.size(80.dp)) {
                        val strokeWidth = 8.dp.toPx()
                        val inset = strokeWidth / 2
                        val arcRect = Offset(inset, inset)
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        // Background arc
                        drawArc(color = Color.White.copy(alpha = 0.07f), startAngle = 150f, sweepAngle = 240f, useCenter = false,
                            topLeft = arcRect, size = arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                        // Score arc
                        drawArc(
                            brush = Brush.sweepGradient(listOf(SolanaPurple, SolanaGreen)),
                            startAngle = 150f, sweepAngle = sweep, useCenter = false,
                            topLeft = arcRect, size = arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$animScore", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Text("/100", color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp)
                    }
                }
                Column {
                    Text("SOVEREIGNTY SCORE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), letterSpacing = 2.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when { score >= 80 -> "Sovereign Elite 🔐" ; score >= 50 -> "Identity Verified ✔" ; else -> "Beginner" },
                        color = SolanaGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium
                    )

                    // Seeker Ready badge
                    if (hasWallet) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier.background(SolanaGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(1.dp, SolanaGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🔒 Seeker Ready", style = MaterialTheme.typography.labelSmall, color = SolanaGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            // Score pillars grid
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScorePillar("Wallet", hasWallet, SolanaGreen, Modifier.weight(1f))
                ScorePillar("SOL Tx", hasTx, Color(0xFF00C2FF), Modifier.weight(1f))
                ScorePillar("ZK Proof", hasProof, SolanaPurple, Modifier.weight(1f))
                ScorePillar("IPFS", hasIpfs, SolanaBlue, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ScorePillar(label: String, achieved: Boolean, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                color = if (achieved) color.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(1.dp, if (achieved) color.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (achieved) Icons.Default.CheckCircle else Icons.Default.Menu,
            null,
            tint = if (achieved) color else Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (achieved) color else Color.White.copy(alpha = 0.3f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WalletAddressCard(address: String, onCopy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SOLANA PUBLIC KEY", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.35f), letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onCopy).padding(12.dp)
            ) {
                Text(
                    address,
                    color = SolanaGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MnemonicDialog(viewModel: ProfileViewModel, onDismiss: () -> Unit) {
    var authenticated by remember { mutableStateOf(false) }
    val words = viewModel.getMnemonic()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D1117),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFFDB622), modifier = Modifier.size(20.dp))
                Text("Seed Phrase Backup", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (!authenticated) {
                Column {
                    Text("⚠️ Anyone with your seed phrase controls your wallet. Never share it with anyone.", color = Color(0xFFFDB622), style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { authenticated = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SolanaGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("I Understand, Reveal Phrase", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            } else if (words != null) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text("Write these 12 words in order on paper:", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    words.chunked(3).forEachIndexed { rowIdx, group ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            group.forEachIndexed { colIdx, word ->
                                val num = rowIdx * 3 + colIdx + 1
                                Box(
                                    Modifier.weight(1f).background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp)).padding(8.dp)
                                ) {
                                    Column {
                                        Text("$num", style = MaterialTheme.typography.labelSmall, color = SolanaGreen, fontWeight = FontWeight.Bold)
                                        Text(word, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        if (rowIdx < 3) Spacer(Modifier.height(8.dp))
                    }
                }
            } else {
                Text("No wallet found. Create a wallet first.", color = Color(0xFFCF6679))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = SolanaGreen, fontWeight = FontWeight.Bold) } }
    )
}

@Composable
private fun ActivityItem(activity: ActivityLogEntity) {
    val (color, label, icon) = when (activity.type) {
        ActivityType.SOLANA_TX -> Triple(SolanaPurple, "Solana Transaction", Icons.Default.Send)
        ActivityType.ARCIUM_PROOF -> Triple(SolanaGreen, "Arcium ZK Proof", Icons.Default.Lock)
        ActivityType.IPFS_HASH -> Triple(SolanaBlue, "IPFS Data Storage", Icons.Default.Share)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)) }
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(activity.timestamp)), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.3f))
            }
            Spacer(Modifier.height(2.dp))
            Text(activity.hashValue, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.35f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
