package com.solanasuper.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanasuper.ui.state.ArciumComputationState

@Composable
fun MpcLoadingOverlay(
    state: ArciumComputationState
) {
    if (state == ArciumComputationState.IDLE) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(
                    color = Color(0xFF1E1E1E).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(40.dp)
        ) {
            // Animated Icon based on state
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 2 }).togetherWith(fadeOut())
                },
                label = "icon_transition"
            ) { targetState ->
                val iconData: Pair<ImageVector, Color> = when (targetState) {
                    ArciumComputationState.GENERATING_LOCAL_PROOF -> Icons.Default.Lock to Color(0xFF03DAC5)
                    ArciumComputationState.SUBMITTING_TO_ARCIUM_MXE -> Icons.Default.Share to Color(0xFFBB86FC)
                    ArciumComputationState.COMPUTING_IN_MXE -> Icons.Default.Settings to Color(0xFFCF6679)
                    ArciumComputationState.COMPUTATION_CALLBACK -> Icons.Default.Lock to Color(0xFF03DAC5)
                    ArciumComputationState.COMPLETED -> Icons.Default.CheckCircle to Color.Green
                    ArciumComputationState.FAILED -> Icons.Default.Lock to Color.Red
                    else -> Icons.Default.Lock to Color.Gray
                }
                
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(iconData.second.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (targetState == ArciumComputationState.COMPLETED) {
                        Icon(
                            imageVector = iconData.first,
                            contentDescription = null,
                            tint = iconData.second,
                            modifier = Modifier.size(40.dp)
                        )
                    } else if (targetState != ArciumComputationState.FAILED) {
                        CircularProgressIndicator(
                            color = iconData.second,
                            modifier = Modifier.size(80.dp),
                            strokeWidth = 3.dp
                        )
                        Icon(
                            imageVector = iconData.first,
                            contentDescription = null,
                            tint = iconData.second,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                         Icon(
                            imageVector = iconData.first,
                            contentDescription = null,
                            tint = iconData.second,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Arcium Secure Compute",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = state.message,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 2 }).togetherWith(fadeOut())
                },
                label = "text_transition"
            ) { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
