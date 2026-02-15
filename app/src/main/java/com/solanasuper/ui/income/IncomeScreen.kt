package com.solanasuper.ui.income

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanasuper.data.OfflineTransaction
import com.solanasuper.data.TransactionStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun IncomeScreen(
    state: IncomeState,
    onClaimUbi: () -> Unit,
    onSendOffline: () -> Unit,
    onReceiveOffline: () -> Unit,
    onCancelP2P: () -> Unit,
    onConfirmP2P: () -> Unit,
    onRejectP2P: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                "Income",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Hero Balance
            Text(
                text = "$${state.balance}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 64.sp
            )
            
            Text(
                text = "Universal Basic Income",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Claim UBI Button
                Button(
                    onClick = onClaimUbi,
                    enabled = !state.isClaiming,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF14F195),
                        contentColor = Color.Black
                    )
                ) {
                   Text("Claim UBI")
                }

                // Offline Actions
                Button(
                    onClick = onSendOffline,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                     colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF333333),
                        contentColor = Color.White
                    )
                ) {
                    Text("Send")
                }
                 Button(
                    onClick = onReceiveOffline,
                    modifier = Modifier.weight(1f).height(56.dp),
                     shape = RoundedCornerShape(16.dp),
                     colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF333333),
                        contentColor = Color.White
                    )
                ) {
                    Text("Receive")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Recent Transactions List
            Text(
                "Recent Activity",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.transactions) { transaction ->
                     TransactionItem(transaction)
                }
            }
        }
        
        // P2P Overlay
         if (state.p2pStatus != com.solanasuper.ui.income.P2PStatus.IDLE) {
            P2POverlay(
                status = state.p2pStatus,
                onCancel = onCancelP2P,
                onConfirm = onConfirmP2P,
                onReject = onRejectP2P
            )
        }
    }
}

@Composable
fun TransactionItem(transaction: OfflineTransaction) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
             Text(
                text = if (transaction.amount > 0) "Received" else "Sent",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(transaction.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        
        Text(
            text = (if (transaction.amount > 0) "+" else "") + "$${org.abs(transaction.amount)}",
            style = MaterialTheme.typography.titleLarge,
            color = if (transaction.amount > 0) Color(0xFF14F195) else Color.White,
             fontWeight = FontWeight.Bold
        )
    }
}

// Helper to fix Math.abs ambiguity
object org {
    fun abs(value: Long): Long = if (value < 0) -value else value
}

@Composable
fun P2POverlay(
    status: com.solanasuper.ui.income.P2PStatus,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(enabled = false) {}, // Block clicks
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
             modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = when (status) {
                    com.solanasuper.ui.income.P2PStatus.DISCOVERING -> "Searching for peers..."
                    com.solanasuper.ui.income.P2PStatus.CONNECTING -> "Connecting..."
                    com.solanasuper.ui.income.P2PStatus.CONNECTED -> "Peer Connected"
                    com.solanasuper.ui.income.P2PStatus.TRANSFERRING -> "Transferring..."
                     com.solanasuper.ui.income.P2PStatus.SUCCESS -> "Transfer Complete!"
                    com.solanasuper.ui.income.P2PStatus.ERROR -> "Transfer Failed"
                    else -> ""
                },
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            if (status == com.solanasuper.ui.income.P2PStatus.CONNECTED) {
                 Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onReject,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Reject")
                    }
                    Button(
                        onClick = onConfirm,
                         colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14F195))
                    ) {
                        Text("Confirm")
                    }
                }
            } else {
                 Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                ) {
                    if (status == com.solanasuper.ui.income.P2PStatus.SUCCESS || status == com.solanasuper.ui.income.P2PStatus.ERROR) {
                        Text("Close", color = Color.White)
                    } else {
                         Text("Cancel", color = Color.Red)
                    }
                }
            }
        }
    }
}
