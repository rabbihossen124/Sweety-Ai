package com.sweety.ai.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sweety.ai.data.AssistantState
import com.sweety.ai.presentation.theme.CyanGlow
import com.sweety.ai.presentation.theme.ElectricCyan
import com.sweety.ai.presentation.theme.NeonBlue
import kotlin.math.sin

@Composable
fun NeonVisualizerWave(
    state: AssistantState,
    audioLevel: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        val amplitude = when (state) {
            AssistantState.LISTENING -> (15f + audioLevel * 35f).coerceAtMost(height / 2 - 4)
            AssistantState.SPEAKING -> (20f).coerceAtMost(height / 2 - 4)
            AssistantState.THINKING -> (10f)
            else -> 4f // Gentle idle breathing
        }

        // 1. Primary Glowing Wave
        val path1 = Path()
        val path2 = Path()

        for (x in 0..width.toInt() step 4) {
            val progress = x / width
            val envelope = sin(progress * Math.PI.toFloat()) // Dampen at ends
            val y1 = centerY + sin(progress * 10f + phase) * amplitude * envelope
            val y2 = centerY + sin(progress * 8f - phase) * (amplitude * 0.7f) * envelope

            if (x == 0) {
                path1.moveTo(x.toFloat(), y1)
                path2.moveTo(x.toFloat(), y2)
            } else {
                path1.lineTo(x.toFloat(), y1)
                path2.lineTo(x.toFloat(), y2)
            }
        }

        val primaryBrush = Brush.horizontalGradient(
            colors = listOf(NeonBlue, ElectricCyan, CyanGlow)
        )
        val secondaryBrush = Brush.horizontalGradient(
            colors = listOf(CyanGlow.copy(alpha = 0.5f), NeonBlue.copy(alpha = 0.5f))
        )

        // Draw secondary glow layer
        drawPath(
            path = path2,
            brush = secondaryBrush,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw primary sharp neon line
        drawPath(
            path = path1,
            brush = primaryBrush,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

