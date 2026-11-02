package com.sweety.ai.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sweety.ai.data.AssistantState
import com.sweety.ai.presentation.theme.CyanGlow
import com.sweety.ai.presentation.theme.DarkVoid
import com.sweety.ai.presentation.theme.ElectricCyan
import com.sweety.ai.presentation.theme.NeonBlue
import com.sweety.ai.presentation.theme.PulseRed

@Composable
fun GlowingMicButton(
    state: AssistantState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = state == AssistantState.LISTENING
    val isSpeaking = state == AssistantState.SPEAKING
    val isThinking = state == AssistantState.THINKING

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening || isSpeaking) 1.35f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing glow aura
        if (isListening || isSpeaking || isThinking) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        if (isListening) ElectricCyan.copy(alpha = pulseAlpha)
                        else NeonBlue.copy(alpha = pulseAlpha)
                    )
            )
        }

        // Inner glowing ring
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isListening) PulseRed.copy(alpha = 0.25f) else ElectricCyan.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = if (isListening) listOf(PulseRed, CyanGlow)
                        else listOf(ElectricCyan, NeonBlue)
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (state) {
                AssistantState.LISTENING -> Icons.Filled.Stop
                AssistantState.SPEAKING -> Icons.Filled.GraphicEq
                AssistantState.THINKING -> Icons.Filled.GraphicEq
                else -> Icons.Filled.Mic
            }

            val iconColor = when (state) {
                AssistantState.LISTENING -> PulseRed
                AssistantState.SPEAKING -> CyanGlow
                else -> ElectricCyan
            }

            Icon(
                imageVector = icon,
                contentDescription = "Microphone",
                tint = iconColor,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

