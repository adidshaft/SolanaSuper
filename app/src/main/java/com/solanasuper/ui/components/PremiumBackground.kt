package com.solanasuper.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin
import kotlin.math.cos

private val DeepBlack = Color(0xFF020408)
private val DarkGray  = Color(0xFF0D1117)
private val SolanaGreen  = Color(0xFF14F195)
private val SolanaPurple = Color(0xFF9945FF)
private val SolanaBlue   = Color(0xFF00C2FF)

@Composable
fun PremiumBackground(
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    // Orb 1 — green pulse (slow)
    val orb1Phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb1"
    )
    // Orb 2 — purple drift (medium)
    val orb2Phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb2"
    )
    // Orb 3 — blue breathe (fast)
    val orb3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.04f, targetValue = 0.12f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb3"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Orb 1: Green — top-left area, orbiting slowly
            val orb1X = w * 0.15f + w * 0.12f * cos(orb1Phase)
            val orb1Y = h * 0.20f + h * 0.10f * sin(orb1Phase)
            drawOrb(Offset(orb1X, orb1Y), w * 0.55f, SolanaGreen, 0.08f)

            // Orb 2: Purple — bottom-right
            val orb2X = w * 0.80f + w * 0.10f * cos(orb2Phase + 1f)
            val orb2Y = h * 0.70f + h * 0.08f * sin(orb2Phase)
            drawOrb(Offset(orb2X, orb2Y), w * 0.65f, SolanaPurple, 0.07f)

            // Orb 3: Blue — center-right, breathing
            drawOrb(Offset(w * 0.85f, h * 0.35f), w * 0.40f, SolanaBlue, orb3Alpha)
        }
        content()
    }
}

private fun DrawScope.drawOrb(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
