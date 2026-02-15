package com.solanasuper.ui.income

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanasuper.data.OfflineTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IncomeScreen(
    state: IncomeState = IncomeState(),
    onClaimUbi: () -> Unit = {},
    onSendOffline: () -> Unit = {},
    onReceiveOffline: () -> Unit = {},
    onCancelP2P: () -> Unit = {},
    onConfirmP2P: () -> Unit = {},
    onRejectP2P: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Balance Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.05f)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = "Total Balance",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "$${state.balance}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onSendOffline,
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
            ) {
                Text("Send Offline", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            
            Button(
                onClick = onReceiveOffline,
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
            ) {
                Text("Receive Offline", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onClaimUbi,
            enabled = !state.isClaiming,
            modifier = Modifier.fillMaxWidth().height(56.dp),
             shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha=0.08f),
                disabledContainerColor = Color.White.copy(alpha=0.05f)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            if (state.isClaiming) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.height(24.dp).padding(end = 8.dp),
                    strokeWidth = 2.dp
                )
                Text("Requesting Airdrop...", color = Color.White.copy(alpha = 0.7f))
            } else {
                Text("Claim UBI Demo (Devnet)", color = Color.White.copy(alpha = 0.9f))
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.Start),
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(state.transactions) { tx ->
                TransactionItem(tx)
            }
        }
    }

    AnimatedVisibility(
        visible = state.p2pStatus != P2PStatus.IDLE,
        enter = fadeIn() + slideInVertically()
    ) {
        P2POverlay(
            status = state.p2pStatus,
            peerName = state.p2pPeerName,
            authToken = state.p2pAuthToken,
            error = state.error,
            onCancel = onCancelP2P,
            onConfirm = onConfirmP2P,
            onReject = onRejectP2P
        )
    }
}

@Composable
fun P2POverlay(
    status: P2PStatus,
    peerName: String?,
    authToken: String? = null,
    error: String?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit = {},
    onReject: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E).copy(alpha = 0.9f)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val (title, color) = when (status) {
                    P2PStatus.SCANNING -> "Searching for devices..." to Color.Yellow
                    P2PStatus.FOUND_PEER -> "Device Found!" to Color.Cyan
                    P2PStatus.VERIFYING -> "Verify Connection" to Color.Magenta
                    P2PStatus.CONNECTING -> "Connecting..." to Color.Cyan
                    P2PStatus.CONNECTED -> "Secure Link Established" to Color.Blue
                    P2PStatus.TRANSFERRING -> "Transferring Secure Data..." to Color.Blue
                    P2PStatus.SUCCESS -> "Transfer Successful!" to Color.Green
                    P2PStatus.ERROR -> "Transfer Failed" to Color.Red
                    else -> "Processing..." to Color.White
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (status == P2PStatus.VERIFYING && authToken != null) {
                    Text(
                        text = "Peer Authentication Code:",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = authToken,
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onReject,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reject")
                        }
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.7f)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Accept")
                        }
                    }
                } else {
                    if (peerName != null && status != P2PStatus.ERROR) {
                        Text(
                            text = peerName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                    }

                    if (status == P2PStatus.ERROR && error != null) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red.copy(alpha = 0.8f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (status != P2PStatus.SUCCESS && status != P2PStatus.ERROR) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = color,
                            modifier = Modifier.padding(16.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (status == P2PStatus.SUCCESS || status == P2PStatus.ERROR) 
                                Color(0xFF03DAC5) else Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (status == P2PStatus.SUCCESS || status == P2PStatus.ERROR) "Done" else "Cancel",
                            color = if (status == P2PStatus.SUCCESS || status == P2PStatus.ERROR) Color.Black else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(tx: OfflineTransaction) {
    val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Transfer", fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Text(text = date, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
            }
            Column(horizontalAlignment = Alignment.End) {
                val amountColor = if (tx.amount > 0) Color(0xFF03DAC5) else Color(0xFFCF6679)
                val prefix = if (tx.amount > 0) "+" else ""
                Text(text = "$prefix$${tx.amount}", fontWeight = FontWeight.Bold, color = amountColor, style = MaterialTheme.typography.titleMedium)
                Text(text = tx.status.name, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}
