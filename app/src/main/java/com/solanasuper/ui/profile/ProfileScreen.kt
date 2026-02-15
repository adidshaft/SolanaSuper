package com.solanasuper.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanasuper.data.ActivityLogEntity
import com.solanasuper.data.ActivityType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onShowSnackbar: (String) -> Unit
) {
    val activities by viewModel.activities.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val address = viewModel.solanaAddress

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // HEADER
        Text(
            text = "Cryptographic Profile",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // ADDRESS BLOCK (Minimalist)
        Text(
            text = "SOLANA PUBLIC KEY",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .clickable {
                    clipboardManager.setText(AnnotatedString(address))
                    onShowSnackbar("Address copied to clipboard")
                }
                .padding(16.dp)
        ) {
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = Color(0xFF14F195), // Solana Green
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // ACTIVITY LIST HEADER
        Text(
            text = "ACTIVITY LEDGER",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        // LIST
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(activities) { activity ->
                ActivityItem(activity)
            }
            
            if (activities.isEmpty()) {
                item {
                    Text(
                        text = "No cryptographic activity recorded yet.",
                        color = Color.White.copy(alpha = 0.3f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityItem(activity: ActivityLogEntity) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when(activity.type) {
                        ActivityType.SOLANA_TX -> "Tx"
                        ActivityType.ARCIUM_PROOF -> "Zk"
                        ActivityType.IPFS_HASH -> "D"
                    },
                    color = when(activity.type) {
                        ActivityType.SOLANA_TX -> Color(0xFF9945FF) // Purple
                        ActivityType.ARCIUM_PROOF -> Color(0xFF14F195) // Green
                        ActivityType.IPFS_HASH -> Color.Cyan
                    },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when(activity.type) {
                            ActivityType.SOLANA_TX -> "Solana Transaction"
                            ActivityType.ARCIUM_PROOF -> "Arcium Computation"
                            ActivityType.IPFS_HASH -> "IPFS Data Storage"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    Text(
                        text = formatTime(activity.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = activity.hashValue,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
