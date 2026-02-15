package com.solanasuper.ui.income

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Total Balance",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Huge Balance Text
        Text(
            text = "$${state.balance}",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onSendOffline,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14F195))
            ) {
                Text("Send", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            
            Button(
                onClick = onReceiveOffline,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Text("Receive", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onClaimUbi,
            enabled = !state.isClaiming,
            modifier = Modifier.fillMaxWidth().height(56.dp),
             shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.transparent,
                disabledContainerColor = Color.transparent
            )
        ) {
            if (state.isClaiming) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp).padding(end = 8.dp),
                    strokeWidth = 2.dp
                )
                Text("Requesting Airdrop...", color = Color.White.copy(alpha = 0.7f))
            } else {
                Text("Claim UBI Demo", color = Color.White.copy(alpha = 0.5f))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Activity",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.Start),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(32.dp) // Generous spacing
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
            .background(Color.Black.copy(alpha = 0.95f)) // Almost opaque for focus
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ... (Keeping P2P Logic but simplified visuals)
            val (title, color) = when (status) {
                P2PStatus.SCANNING -> "Searching..." to Color.Yellow
                P2PStatus.FOUND_PEER -> "Found Peer" to Color.Cyan
                P2PStatus.VERIFYING -> "Verify Code" to Color.Magenta
                P2PStatus.CONNECTING -> "Connecting..." to Color.Cyan
                P2PStatus.CONNECTED -> "Connected" to Color.Blue
                P2PStatus.TRANSFERRING -> "Sending..." to Color.Blue
                P2PStatus.SUCCESS -> "Sent!" to Color.Green
                P2PStatus.ERROR -> "Failed" to Color.Red
                else -> "Processing..." to Color.White
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                color = color,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (status == P2PStatus.VERIFYING && authToken != null) {
                Text(
                    text = authToken,
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp
                )
                Spacer(modifier = Modifier.height(48.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onReject,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f).height(64.dp),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Text("Reject", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                        modifier = Modifier.weight(1f).height(64.dp),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Text("Accept", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                 if (peerName != null && status != P2PStatus.ERROR) {
                    Text(
                        text = peerName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                if (status == P2PStatus.ERROR && error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Text(
                        text = if (status == P2PStatus.SUCCESS || status == P2PStatus.ERROR) "Done" else "Cancel",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(tx: OfflineTransaction) {
    val date = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(tx.timestamp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if(tx.amount > 0) "↓" else "↑",
                    color = Color.White,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            Column {
                Text(text = if(tx.amount > 0) "Received" else "Sent", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 16.sp)
                Text(text = date, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.5f))
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            val amountColor = if (tx.amount > 0) Color(0xFF14F195) else Color.White
            val prefix = if (tx.amount > 0) "+" else ""
            Text(text = "$prefix$${org.abs(tx.amount)}", fontWeight = FontWeight.Bold, color = amountColor, fontSize = 18.sp)
        }
    }
}

// Helper to fix Math.abs ambiguity
object org {
    fun abs(value: Double): Double = if (value < 0) -value else value
}
