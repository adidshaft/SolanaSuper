package com.solanasuper.ui.income

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanasuper.network.NetworkManager

private val SolanaGreen  = Color(0xFF14F195)
private val SolanaPurple = Color(0xFF9945FF)
private val CardBg       = Color(0xFF0F1117)
private val CardBorder   = Color(0xFF1E2230)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(viewModel: IncomeViewModel) {
    val state by viewModel.state.collectAsState()
    val isLive by NetworkManager.isLiveMode.collectAsState()

    var showSendOptions by remember { mutableStateOf(false) }
    var showAddressDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var scannedAddress by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = LocalFocusManager.current

    // Biometric Signing Observer
    LaunchedEffect(Unit) {
        viewModel.signRequest.collect { payload ->
            try {
                val activity = context as? androidx.fragment.app.FragmentActivity
                if (activity != null) {
                    val signature = viewModel.identityManager.signTransaction(activity, payload)
                    viewModel.broadcastTransaction(signature)
                } else {
                    android.widget.Toast.makeText(context, "Error: Context is not FragmentActivity", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Signing Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto-Refresh on Resume
    val lifecycleOwner = context as androidx.lifecycle.LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showQrScanner) {
        com.solanasuper.ui.components.QrScanner(onQrCodeScanned = { address ->
            showQrScanner = false
            scannedAddress = address
            showAddressDialog = true
        })
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("WALLET", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.35f), letterSpacing = 3.sp)
                    Text("Income & Assets", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
                // Live/Sim pill
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isLive) SolanaGreen.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(50)
                        )
                        .border(1.dp, if (isLive) SolanaGreen.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                        .clickable { NetworkManager.toggleMode() }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(if (isLive) SolanaGreen else Color(0xFF00C2FF)))
                        Text(if (isLive) "LIVE" else "SIM", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                }
            }
        }

        // Balance Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF0D1B2A), Color(0xFF0A1628), SolanaPurple.copy(alpha = 0.12f))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(24.dp))
            ) {
                Column(Modifier.padding(28.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.size(36.dp).background(SolanaGreen.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("◎", color = SolanaGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("SOLANA BALANCE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), letterSpacing = 2.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    if (state.isLoading && state.balance == 0.0) {
                        CircularProgressIndicator(color = SolanaGreen, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "${String.format("%.4f", state.balance)} SOL",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.claimUbi() },
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SolanaGreen.copy(alpha = 0.15f), contentColor = SolanaGreen),
                        border = BorderStroke(1.dp, SolanaGreen.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isLive) "Claim Airdrop" else "Claim UBI (Sim)", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WalletActionButton(Icons.Default.Send, "Send", SolanaGreen, Modifier.weight(1f)) { showSendOptions = true }
                WalletActionButton(Icons.Default.Add, "Receive", Color(0xFF00C2FF), Modifier.weight(1f)) { viewModel.startReceiving() }
                WalletActionButton(Icons.Default.Refresh, "Refresh", SolanaPurple, Modifier.weight(1f)) { viewModel.refresh() }
            }
            Spacer(Modifier.height(24.dp))
        }

        // P2P Section
        item {
            AnimatedVisibility(
                visible = state.p2pStatus != PeerStatus.IDLE,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                P2PCard(state, onConfirm = { viewModel.confirmConnection() }, onReject = { viewModel.rejectConnection() }, onStop = { viewModel.stopP2P() }, onSend = { viewModel.sendP2P(it) }, focusManager = focusManager)
            }
            if (state.p2pStatus != PeerStatus.IDLE) Spacer(Modifier.height(16.dp))
        }

        // Error Banner
        if (state.status is UiStatus.Error) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .background(Color(0xFF3D0B0B), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFCF6679), modifier = Modifier.size(18.dp))
                    Text((state.status as UiStatus.Error).message, color = Color(0xFFCF6679), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // Transactions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RECENT ACTIVITY", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.35f), letterSpacing = 3.sp)
                if (state.isLoading) CircularProgressIndicator(Modifier.size(16.dp), color = SolanaGreen, strokeWidth = 2.dp)
            }
            Spacer(Modifier.height(12.dp))
        }

        // Transaction List
        if (state.transactions.isEmpty() && !state.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No transactions yet.\nClaim an airdrop to get started.", color = Color.White.copy(alpha = 0.3f), textAlign = TextAlign.Center, lineHeight = 22.sp)
                }
            }
        } else {
            items(state.transactions) { tx -> PremiumTransactionItem(tx) }
        }

        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text(
                "v1.0.0 · SovereignLifeOS · Encrypted",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.15f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }

    // 3-Way Send Modal
    if (showSendOptions) {
        ModalBottomSheet(onDismissRequest = { showSendOptions = false }, sheetState = sheetState, containerColor = Color(0xFF0D1117), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
                Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(20.dp))
                Text("Send SOL", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Choose how to send your payment", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.4f))
                Spacer(Modifier.height(24.dp))
                SendMethodRow(Icons.Default.Edit, "Paste Address", "Enter Base58 address manually", SolanaGreen) { showSendOptions = false; showAddressDialog = true }
                SendMethodRow(Icons.Default.Search, "Scan QR Code", "Use camera to scan a Solana address", Color(0xFF00C2FF)) { showSendOptions = false; showQrScanner = true }
                SendMethodRow(Icons.Default.Settings, "Send Offline (P2P)", "Bluetooth mesh — no internet needed", SolanaPurple) { showSendOptions = false; viewModel.startSending() }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // Address Send Dialog
    if (showAddressDialog) {
        var address by remember(scannedAddress) { mutableStateOf(scannedAddress) }
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            containerColor = Color(0xFF0D1117),
            shape = RoundedCornerShape(20.dp),
            title = { Text("Send SOL", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = address, onValueChange = { address = it },
                        label = { Text("Recipient Address") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SolanaGreen, unfocusedBorderColor = CardBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = SolanaGreen, focusedLabelColor = SolanaGreen),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )
                    OutlinedTextField(
                        value = amount, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                        label = { Text("Amount (SOL)") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SolanaGreen, unfocusedBorderColor = CardBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = SolanaGreen, focusedLabelColor = SolanaGreen)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull()
                        if (amt != null && address.isNotBlank()) { viewModel.prepareTransaction(address, amt); showAddressDialog = false }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolanaGreen),
                    enabled = amount.toDoubleOrNull() != null && address.isNotBlank()
                ) { Text("Sign & Send", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddressDialog = false }) { Text("Cancel", color = Color.White.copy(alpha = 0.5f)) }
            }
        )
    }
}

@Composable
private fun WalletActionButton(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(52.dp)
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun SendMethodRow(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.4f))
        }
        Icon(Icons.Default.ArrowForward, null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun P2PCard(
    state: IncomeUiState,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onStop: () -> Unit,
    onSend: (Double) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A0F)),
        border = BorderStroke(1.dp, SolanaGreen.copy(alpha = 0.25f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(SolanaGreen))
                Text("P2P MESH", style = MaterialTheme.typography.labelSmall, color = SolanaGreen, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                if (state.p2pStatus != PeerStatus.SUCCESS && state.p2pStatus != PeerStatus.ERROR) {
                    TextButton(onClick = onStop, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text("Cancel", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            when (state.p2pStatus) {
                PeerStatus.SCANNING -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = SolanaGreen, strokeWidth = 2.dp)
                        Text("Scanning for nearby peers via Bluetooth...", color = Color.White.copy(alpha = 0.7f))
                    }
                }
                PeerStatus.FOUND_PEER, PeerStatus.CONNECTING, PeerStatus.VERIFYING -> {
                    Text("Peer Found: ${state.p2pPeerName ?: "Unknown"}", color = Color.White, fontWeight = FontWeight.SemiBold)
                    if (state.p2pAuthToken != null) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp)).padding(12.dp)
                        ) {
                            Column {
                                Text("Auth Token", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                                Text(state.p2pAuthToken, color = SolanaGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = SolanaGreen), shape = RoundedCornerShape(10.dp)) { Text("Confirm", color = Color.Black, fontWeight = FontWeight.Bold) }
                            OutlinedButton(onClick = onReject, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))) { Text("Reject", color = Color.White.copy(alpha = 0.6f)) }
                        }
                    }
                }
                PeerStatus.CONNECTED_WAITING_INPUT -> {
                    var p2pAmount by remember { mutableStateOf("") }
                    Text("Connected to ${state.p2pPeerName}", color = SolanaGreen, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = p2pAmount, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) p2pAmount = it },
                        label = { Text("Amount (SOL)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); p2pAmount.toDoubleOrNull()?.let { onSend(it) } }),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SolanaGreen, unfocusedBorderColor = CardBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = SolanaGreen, focusedLabelColor = SolanaGreen)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { focusManager.clearFocus(); p2pAmount.toDoubleOrNull()?.let { onSend(it) } }, modifier = Modifier.fillMaxWidth(), enabled = p2pAmount.toDoubleOrNull() != null, colors = ButtonDefaults.buttonColors(containerColor = SolanaGreen), shape = RoundedCornerShape(10.dp)) {
                        Text("Send Payment", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                PeerStatus.CONNECTED_WAITING_FUNDS -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = SolanaGreen, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.height(8.dp))
                        Text("Waiting for payment from ${state.p2pPeerName}...", color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    }
                }
                PeerStatus.SUCCESS -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = SolanaGreen, modifier = Modifier.size(20.dp))
                        Text("Transfer Complete!", color = SolanaGreen, fontWeight = FontWeight.Bold)
                    }
                }
                PeerStatus.ERROR -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFCF6679), modifier = Modifier.size(20.dp))
                        Text(state.error ?: "An error occurred", color = Color(0xFFCF6679))
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun PremiumTransactionItem(tx: UiTransaction) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isReceive = tx.isReceived
    val color = if (isReceive) SolanaGreen else Color(0xFFCF6679)
    val sign = if (isReceive) "+" else "-"
    val isPending = tx.status == com.solanasuper.data.TransactionStatus.PENDING_SYNC

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!isPending) {
                    try {
                        val url = "https://explorer.solana.com/tx/${tx.id}?cluster=devnet"
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                    } catch (e: Exception) {}
                }
            }
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (isReceive) Icons.Default.Add else Icons.Default.Send, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(if (isReceive) "Received" else "Sent", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            val sub = tx.recipientId ?: "Unknown"
            Text(
                if (isPending) "⏳ Pending sync..." else if (sub.length > 12) sub.take(6) + "..." + sub.takeLast(4) else sub,
                color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$sign${String.format("%.4f", tx.amount)} SOL", color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(
                java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(tx.timestamp)),
                color = Color.White.copy(alpha = 0.3f), style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
