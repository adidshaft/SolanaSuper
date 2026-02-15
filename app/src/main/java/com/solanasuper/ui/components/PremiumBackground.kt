package com.solanasuper.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun PremiumBackground(
    content: @Composable () -> Unit
) {
    // Animated Mesh Gradient
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    // Animate gradient offset to create a fluid effect
    val targetOffset = 1000f
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = targetOffset,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    val deepBlack = Color(0xFF050505)
    val darkGray = Color(0xFF121212)
    val solanaGreen = Color(0xFF14F195)
    val solanaPurple = Color(0xFF9945FF)

    val brush = Brush.linearGradient(
        colors = listOf(
            deepBlack,
            darkGray,
            solanaPurple.copy(alpha = 0.15f),
            solanaGreen.copy(alpha = 0.1f),
            deepBlack
        ),
        start = Offset(0f, 0f),
        end = Offset(offset, offset)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
            .background(Color.Black.copy(alpha = 0.2f)) // Overlay for depth
    ) {
        content()
    }
}
