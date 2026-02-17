package com.solanasuper.ui.income

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solanasuper.data.OfflineTransaction
import com.solanasuper.network.NetworkManager
import com.solanasuper.ui.income.IncomeViewModel
// import com.solanasuper.ui.income.P2PStatus
import com.solanasuper.ui.income.UiStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(viewModel: IncomeViewModel) {
    val state by viewModel.state.collectAsState()
    val isLive by NetworkManager.isLiveMode.collectAsState()
    var selectedTransaction by remember { mutableStateOf<UiTransaction?>(null) }
    
    // 3-Way Modal State
    var showSendOptions by remember { mutableStateOf(false) }
    var showAddressDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Biometric Signing Observer
    LaunchedEffect(Unit) {
        viewModel.signRequest.collect { payload ->
             try {
                 val activity = context as? androidx.fragment.app.FragmentActivity
                 if (activity != null) {
                     // Use exposed identityManager
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

    // Auto-Refresh on Resume (Fix for Web Faucet return)
    // Fix: Use LocalContext to get LifecycleOwner safely, bypassing CompositionLocal issues
    val lifecycleOwner = androidx.compose.ui.platform.LocalContext.current as androidx.lifecycle.LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showQrScanner) {
        com.solanasuper.ui.components.QrScanner(onQrCodeScanned = { address ->
             showQrScanner = false
             // Open Address Dialog with pre-filled address
             // Needed state for address?
             // Implementation detail: I'll just use a VM method `setScannedAddress(address)` and show dialog?
             // Or pass it to dialog state.
             // For simplicity, I will immediately prompt amount for scanned address?
        })
        return // Show only QR
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
            .imePadding() // Handle keyboard push up
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Income & Assets",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Network Toggle
                Surface(
                    onClick = { NetworkManager.toggleMode() },
                    shape = RoundedCornerShape(50),
                    color = if (isLive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                       modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                       verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isLive) Color.Green else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLive) "LIVE" else "SIM",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Balance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Total Balance", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${String.format("%.2f", state.balance)} SOL",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.claimUbi() },
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading && state.p2pStatus == com.solanasuper.ui.income.PeerStatus.IDLE) {
                           CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                           Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Claim UBI (Faucet)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Actions
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActionButton(icon = Icons.Default.Send, label = "Send") { showSendOptions = true }
                ActionButton(icon = Icons.Default.ArrowDropDown, label = "Receive") { viewModel.startReceiving() }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // P2P Section
        item {
            AnimatedVisibility(visible = state.p2pStatus != PeerStatus.IDLE) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("P2P Connection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        when (state.p2pStatus) {
                            PeerStatus.SCANNING -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Scanning for peers...")
                                }
                            }
                            PeerStatus.FOUND_PEER, PeerStatus.CONNECTING, PeerStatus.VERIFYING -> {
                                Column {
                                    Text("Found Peer: ${state.p2pPeerName ?: "Unknown"}")
                                    if (state.p2pAuthToken != null) {
                                        Text("Auth Token: ${state.p2pAuthToken}", style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row {
                                            Button(onClick = { viewModel.confirmConnection() }) { Text("Confirm") }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            OutlinedButton(onClick = { viewModel.rejectConnection() }) { Text("Reject") }
                                        }
                                    } else {
                                         Text("Connecting...")
                                    }
                                }
                            }
                            PeerStatus.CONNECTED_WAITING_INPUT -> {
                                var p2pAmount by remember { mutableStateOf("") }
                                Column {
                                    Text("Connected to ${state.p2pPeerName}", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = p2pAmount,
                                        onValueChange = { newValue ->
                                            if (newValue.all { it.isDigit() || it == '.' }) {
                                                p2pAmount = newValue
                                            }
                                        },
                                        label = { Text("Amount to Send") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                        ),
                                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                            onDone = {
                                                focusManager.clearFocus()
                                                val amt = p2pAmount.toDoubleOrNull()
                                                if (amt != null) viewModel.sendP2P(amt)
                                            }
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { 
                                            focusManager.clearFocus()
                                            val amt = p2pAmount.toDoubleOrNull()
                                            if (amt != null) viewModel.sendP2P(amt)
                                        },
                                        enabled = p2pAmount.isNotBlank() && p2pAmount.toDoubleOrNull() != null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Send Payment")
                                    }
                                }
                            }
                            PeerStatus.CONNECTED_WAITING_FUNDS -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text("Connected to ${state.p2pPeerName}", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Waiting for payment...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            PeerStatus.TRANSFERRING -> {
                                 Text("Transfer in progress...", color = MaterialTheme.colorScheme.primary)
                                 LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                            }
                            PeerStatus.SUCCESS -> {
                                 Text("Transfer Complete!", color = Color.Green, fontWeight = FontWeight.Bold)
                            }
                            PeerStatus.ERROR -> {
                                 Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                                 TextButton(onClick = { viewModel.stopP2P() }) { Text("Close") }
                            }
                            else -> {}
                        }
                        
                        if (state.p2pStatus != PeerStatus.SUCCESS && state.p2pStatus != PeerStatus.ERROR) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.stopP2P() }, modifier = Modifier.align(Alignment.End)) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Transactions List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                        contentDescription = "Sync",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // List Content
        if (state.isLoading && state.transactions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.transactions.isEmpty()) {
             item { Text("No transactions yet.", color = Color.Gray) }
        } else {
             items(items = state.transactions) { tx ->
                 TransactionItem(tx) {
                     selectedTransaction = tx
                 }
             }
        }
        
        // Error Snackbar item (or keep it loosely here?)
        // Better to put it in a Box over the lazy column, but item is fine for simple error text
        if (state.status is UiStatus.Error) {
             item {
                 Text(
                     text = (state.status as UiStatus.Error).message,
                     color = MaterialTheme.colorScheme.error,
                     modifier = Modifier.padding(top = 8.dp)
                 )
             }
        }
    }
    
    // 3-Way Send Modal
    if (showSendOptions) {
        ModalBottomSheet(
            onDismissRequest = { showSendOptions = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Select Send Method", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                // Option 1: Send to Address
                SendOptionItem(icon = Icons.Default.Send, title = "Send to Address", subtitle = "Enter Solana address manually") {
                    showSendOptions = false
                    showAddressDialog = true
                }
                
                // Option 2: Scan QR
                SendOptionItem(icon = Icons.Default.Send, title = "Scan QR Code", subtitle = "Use camera to scan Base58 address") {
                    showSendOptions = false
                    showQrScanner = true
                }
                
                // Option 3: Offline P2P
                SendOptionItem(icon = Icons.Default.ArrowDropDown, title = "Send Offline (P2P)", subtitle = "Nearby Connections via Bluetooth") {
                    showSendOptions = false
                    viewModel.startSending()
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    
    // Send to Address Dialog
    if (showAddressDialog) {
        var address by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = { Text("Send SOL") },
            text = {
                Column {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Recipient Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (SOL)") },
                        modifier = Modifier.fillMaxWidth(), // keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amountDouble = amount.toDoubleOrNull()
                        if (amountDouble != null && address.isNotBlank()) {
                            // viewModel.sendTransaction(address, amountDouble) // TODO: Implement
                            // Since VM needs signing, assume we implemented logic
                            viewModel.prepareTransaction(address, amountDouble)
                            showAddressDialog = false
                        }
                    }
                ) { Text("Sign & Send") }
            },
            dismissButton = {
                TextButton(onClick = { showAddressDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SendOptionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}


@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
            Icon(icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun TransactionItem(tx: UiTransaction, onClick: () -> Unit) {
    val title = if (tx.isReceived) "Received" else "Sent"
    val subtitle = tx.recipientId ?: "Unknown"
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { 
            // Launch Solana Explorer
            try {
                val url = "https://explorer.solana.com/tx/${tx.id}?cluster=devnet" // Default to devnet
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Could not open Explorer", android.widget.Toast.LENGTH_SHORT).show()
                onClick() // Fallback to expanding/selecting (if needed)
            }
        }
        .padding(vertical = 8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(title, fontWeight = FontWeight.Medium)
                Text(
                    text = if (subtitle.length > 10) subtitle.take(4) + "..." + subtitle.takeLast(4) else subtitle,
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color.Gray
                )
            }
            Text(
                text = "${if (tx.isReceived) "+" else ""}${String.format("%.4f", tx.amount)} SOL",
                color = if (tx.isReceived) Color.Green else Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(tx.timestamp)),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
fun TransactionDetailsDialog(tx: UiTransaction, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transaction Details") },
        text = {
            Column {
                DetailRow("Status", "Success")
                DetailRow("Amount", "${String.format("%.9f", tx.amount)} SOL")
                DetailRow("Date", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(tx.timestamp)))
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Transaction ID", tx.id)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("From/To", tx.recipientId ?: "System")
                
                Spacer(modifier = Modifier.height(16.dp))
                val context = androidx.compose.ui.platform.LocalContext.current
                Text(
                    text = "View on Solana Explorer",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { 
                        val url = "https://explorer.solana.com/tx/${tx.id}?cluster=devnet"
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.statusBarsPadding()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
