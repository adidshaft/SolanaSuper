package com.solanasuper.ui.health

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanasuper.data.HealthEntity
import com.solanasuper.ui.health.DecryptedHealthRecord

@Composable
fun HealthScreen(
    viewModel: HealthViewModel,
    onUnlock: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val uiEvent = viewModel.uiEvent.collectAsState(initial = null)
    
    // Edit Dialog State
    var showEditDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<DecryptedHealthRecord?>(null) }
    var editValue by remember { mutableStateOf("") }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Handle UI Events (e.g. Proof Generated toast)
    // Handle UI Events (e.g. Proof Generated toast & Share Action)
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { msg ->
            if (msg.startsWith("SHARE_PROOF|")) {
                val shareText = msg.removePrefix("SHARE_PROOF|")
                val sendIntent: android.content.Intent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                val shareIntent = android.content.Intent.createChooser(sendIntent, "Share ZK Proof")
                context.startActivity(shareIntent)
            } else {
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    if (state.mpcState != com.solanasuper.ui.state.ArciumComputationState.IDLE) {
        com.solanasuper.ui.components.MpcLoadingOverlay(state.mpcState)
    } else if (state.isLocked) {
        // Locked State - Minimalist Center Lock
        Box(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { viewModel.unlockVault() }, // Use VM method
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Text(
                    text = "Health Vault",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Tap icon to unlock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
                
                if (state.error != null) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = state.error!!,
                        color = Color(0xFFCF6679),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    } else {
        // Unlocked State (List of Records)
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                item {
                    Text(
                        text = "Medical Records",
                        style = MaterialTheme.typography.headlineMedium, 
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                items(state.records) { record ->
                    HealthRecordItem(
                        record = record,
                        onEdit = { 
                            editingRecord = record
                            editValue = record.description
                            showEditDialog = true
                        },
                        onShare = { viewModel.shareRecord(record.id) }
                    )
                }
            }
        }
        
        if (showEditDialog && editingRecord != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Update ${editingRecord!!.title}") },
                text = {
                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        label = { Text("Value / Details") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateRecord(editingRecord!!.id, editValue)
                        showEditDialog = false
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun HealthRecordItem(
    record: DecryptedHealthRecord,
    onEdit: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = record.title.first().toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
        
        Spacer(modifier = Modifier.size(20.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = record.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 2
            )
             Spacer(modifier = Modifier.height(4.dp))
             Text(
                text = "Last Updated: ${java.text.SimpleDateFormat("MMM dd, HH:mm").format(java.util.Date(record.date))}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray.copy(alpha = 0.5f)
            )
        }
        
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White.copy(alpha = 0.7f))
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Default.Share, contentDescription = "Share ZK Proof", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
