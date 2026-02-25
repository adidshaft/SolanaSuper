package com.solanasuper.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solanasuper.ui.state.ArciumComputationState

@Composable
fun MpcLoadingOverlay(state: ArciumComputationState) {
    if (state == ArciumComputationState.IDLE) return

    val infiniteTransition = rememberInfiniteTransition(label = "mpc_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring"
    )

    // Determine step number for display
    val (stepNum, totalSteps) = when (state) {
        ArciumComputationState.GENERATING_LOCAL_PROOF -> 1 to 4
        ArciumComputationState.SUBMITTING_TO_ARCIUM_MXE -> 2 to 4
        ArciumComputationState.COMPUTING_IN_MXE -> 3 to 4
        ArciumComputationState.COMPUTATION_CALLBACK -> 4 to 4
        ArciumComputationState.COMPLETED -> 4 to 4
        ArciumComputationState.FAILED -> 0 to 4
        else -> 0 to 4
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(
                    color = Color(0xFF0F1117).copy(alpha = 0.97f),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF14F195).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 40.dp, vertical = 44.dp)
        ) {
            // Pulsing ring + icon
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 2 }).togetherWith(fadeOut())
                },
                label = "icon_transition"
            ) { targetState ->
                val iconData: Pair<ImageVector, Color> = when (targetState) {
                    ArciumComputationState.GENERATING_LOCAL_PROOF -> Icons.Default.Lock to Color(0xFF14F195)
                    ArciumComputationState.SUBMITTING_TO_ARCIUM_MXE -> Icons.Default.Share to Color(0xFF9945FF)
                    ArciumComputationState.COMPUTING_IN_MXE -> Icons.Default.Settings to Color(0xFF00C2FF)
                    ArciumComputationState.COMPUTATION_CALLBACK -> Icons.Default.Lock to Color(0xFF14F195)
                    ArciumComputationState.COMPLETED -> Icons.Default.CheckCircle to Color(0xFF14F195)
                    ArciumComputationState.FAILED -> Icons.Default.Lock to Color(0xFFCF6679)
                    else -> Icons.Default.Lock to Color.Gray
                }
                val isCompleted = targetState == ArciumComputationState.COMPLETED
                val isFailed = targetState == ArciumComputationState.FAILED
                val isActive = !isCompleted && !isFailed

                Box(contentAlignment = Alignment.Center) {
                    // Outer pulse ring (only when active)
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .scale(pulseScale)
                                .border(
                                    width = 1.5.dp,
                                    color = iconData.second.copy(alpha = ringAlpha * 0.4f),
                                    shape = CircleShape
                                )
                        )
                    }
                    // Progress ring
                    if (isActive) {
                        CircularProgressIndicator(
                            color = iconData.second,
                            modifier = Modifier.size(88.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                    // Icon circle
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(iconData.second.copy(alpha = 0.10f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconData.first,
                            contentDescription = null,
                            tint = iconData.second,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "ARCIUM SECURE COMPUTE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(10.dp))

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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            if (stepNum > 0) {
                Spacer(Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    for (i in 1..totalSteps) {
                        val filled = i <= stepNum
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .background(
                                    color = if (filled) Color(0xFF14F195) else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Step $stepNum of $totalSteps",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.35f)
                )
            }
        }
    }
}
