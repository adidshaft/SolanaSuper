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
import androidx.compose.ui.unit.dp
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
    onCancelP2P: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Balance Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = "Total Balance",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$${state.balance}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSendOffline,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
            ) {
                Text("Send Offline")
            }
            
            Button(
                onClick = onReceiveOffline,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
            ) {
                Text("Receive Offline")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onClaimUbi,
            modifier = Modifier.fillMaxWidth().height(48.dp),
             shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha=0.1f))
        ) {
            Text("Claim UBI Demo")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start),
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.transactions) { tx ->
                TransactionItem(tx)
            }
        }
    }

    if (state.p2pStatus != P2PStatus.IDLE) {
        P2POverlay(
            status = state.p2pStatus,
            peerName = state.p2pPeerName,
            error = state.error,
            onCancel = onCancelP2P
        )
    }
}

@Composable
fun P2POverlay(
    status: P2PStatus,
    peerName: String?,
    error: String?,
    onCancel: () -> Unit
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val (title, color) = when (status) {
                    P2PStatus.SCANNING -> "Searching for devices..." to Color.Yellow
                    P2PStatus.FOUND_PEER -> "Device Found!" to Color.Cyan
                    P2PStatus.CONNECTING -> "Connecting..." to Color.Magenta
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
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

@Composable
fun TransactionItem(tx: OfflineTransaction) {
    val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Transfer", fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(text = date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                val amountColor = if (tx.amount > 0) Color(0xFF03DAC5) else Color.Red
                val prefix = if (tx.amount > 0) "+" else ""
                Text(text = "$prefix$${tx.amount}", fontWeight = FontWeight.Bold, color = amountColor)
                Text(text = tx.status.name, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
