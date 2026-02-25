package com.solanasuper.ui.health

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanasuper.ui.components.MpcLoadingOverlay
import com.solanasuper.ui.state.ArciumComputationState

private val SolanaGreen  = Color(0xFF14F195)
private val SolanaPurple = Color(0xFF9945FF)
private val SolanaBlue   = Color(0xFF00C2FF)
private val CardBg       = Color(0xFF0F1117)
private val CardBorder   = Color(0xFF1E2230)

@Composable
fun HealthScreen(viewModel: HealthViewModel, onUnlock: () -> Unit = {}) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // UI Events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { msg ->
            if (msg.startsWith("SHARE_PROOF|")) {
                val shareText = msg.removePrefix("SHARE_PROOF|")
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share ZK Proof"))
            } else {
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    when {
        state.mpcState != ArciumComputationState.IDLE -> MpcLoadingOverlay(state.mpcState)
        state.isLocked -> LockedVaultScreen(state.error, onUnlock = { viewModel.unlockVault() })
        else -> UnlockedVaultScreen(state.records, viewModel)
    }
}

@Composable
private fun LockedVaultScreen(error: String?, onUnlock: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "lock")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f, targetValue = 0.22f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            // Pulsing ring
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(ringScale)
                        .border(2.dp, SolanaGreen.copy(alpha = ringAlpha), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale((ringScale + 1f) / 2f)
                        .border(1.dp, SolanaGreen.copy(alpha = ringAlpha * 0.5f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(SolanaGreen.copy(alpha = 0.07f))
                        .border(1.5.dp, SolanaGreen.copy(alpha = 0.3f), CircleShape)
                        .clickable { onUnlock() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, "Unlock", tint = SolanaGreen, modifier = Modifier.size(36.dp))
                }
            }

            Spacer(Modifier.height(36.dp))
            Text("Health Vault", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Biometric auth · ZK verification · IPFS storage", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.4f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text("Tap the lock to unlock", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.25f))

            if (error != null) {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 32.dp).background(Color(0xFF3D0B0B), RoundedCornerShape(12.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFCF6679), modifier = Modifier.size(16.dp))
                    Text(error, color = Color(0xFFCF6679), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun UnlockedVaultScreen(records: List<DecryptedHealthRecord>, viewModel: HealthViewModel) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<DecryptedHealthRecord?>(null) }
    var editValue by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) + slideInVertically(spring(stiffness = Spring.StiffnessLow))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item {
                    Column(Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                        Text("HEALTH VAULT", style = MaterialTheme.typography.labelSmall, color = SolanaGreen.copy(alpha = 0.6f), letterSpacing = 3.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Medical Records", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(SolanaGreen))
                            Text("${records.size} records · IPFS + ZK Protected", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                }

                items(records) { record ->
                    HealthRecordCard(
                        record = record,
                        onEdit = { editingRecord = record; editValue = record.description; showEditDialog = true },
                        onShare = { viewModel.shareRecord(record.id) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // FAB — Add Record
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            containerColor = SolanaGreen,
            contentColor = Color.Black,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Text("Add Record", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Edit Dialog
    if (showEditDialog && editingRecord != null) {
        RecordDialog(
            title = "Update ${editingRecord!!.title}",
            initialValue = editValue,
            onConfirm = { value -> viewModel.updateRecord(editingRecord!!.id, value); showEditDialog = false },
            onDismiss = { showEditDialog = false }
        )
    }

    // Add Dialog
    if (showAddDialog) {
        AddRecordDialog(
            onConfirm = { title, desc, category -> viewModel.addRecord(title, desc, category); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun HealthRecordCard(record: DecryptedHealthRecord, onEdit: () -> Unit, onShare: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val (color, icon) = when (record.type) {
        "Vital" -> SolanaGreen to Icons.Default.Favorite
        "Vaccine", "Vaccination" -> Color(0xFF00C2FF) to Icons.Default.Lock
        else -> SolanaPurple to Icons.Default.Info
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(color.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(record.title, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Text(record.type, color = color.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp)) }
                IconButton(onClick = onShare) { Icon(Icons.Default.Share, null, tint = SolanaGreen, modifier = Modifier.size(18.dp)) }
            }

            Spacer(Modifier.height(12.dp))
            Text(record.description, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Updated ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.date))}",
                    style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.25f)
                )
                // IPFS badge
                if (record.ipfsCid != null) {
                    Box(
                        modifier = Modifier
                            .background(SolanaBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .border(1.dp, SolanaBlue.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .clickable {
                                val url = "https://ipfs.io/ipfs/${record.ipfsCid}"
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = SolanaBlue, modifier = Modifier.size(11.dp))
                            Text("IPFS", style = MaterialTheme.typography.labelSmall, color = SolanaBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordDialog(title: String, initialValue: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D1117),
        shape = RoundedCornerShape(20.dp),
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = value, onValueChange = { value = it },
                label = { Text("Value / Details") }, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SolanaGreen, unfocusedBorderColor = CardBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = SolanaGreen, focusedLabelColor = SolanaGreen)
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(value) }, colors = ButtonDefaults.buttonColors(containerColor = SolanaGreen)) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.5f)) } }
    )
}

@Composable
private fun AddRecordDialog(onConfirm: (String, String, String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Vital") }
    val categories = listOf("Vital", "Vaccination", "Prescription", "General")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D1117),
        shape = RoundedCornerShape(20.dp),
        title = { Text("Add Health Record", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title (e.g. Blood Type)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SolanaGreen, unfocusedBorderColor = CardBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = SolanaGreen, focusedLabelColor = SolanaGreen))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Value / Details") }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SolanaGreen, unfocusedBorderColor = CardBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = SolanaGreen, focusedLabelColor = SolanaGreen))
                Text("Category", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat, onClick = { category = cat }, label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SolanaGreen.copy(alpha = 0.2f), selectedLabelColor = SolanaGreen),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = category == cat, selectedBorderColor = SolanaGreen.copy(alpha = 0.4f), borderColor = CardBorder)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, description, category) }, enabled = title.isNotBlank() && description.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = SolanaGreen)) {
                Text("Add Record", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.5f)) } }
    )
}
