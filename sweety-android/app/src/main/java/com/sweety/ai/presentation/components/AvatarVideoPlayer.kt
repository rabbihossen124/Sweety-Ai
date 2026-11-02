package com.sweety.ai.presentation.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sweety.ai.R
import com.sweety.ai.data.AssistantState
import com.sweety.ai.presentation.theme.CyanGlow
import com.sweety.ai.presentation.theme.ElectricCyan
import com.sweety.ai.presentation.theme.NeonBlue
import com.sweety.ai.presentation.theme.PulseRed

@Composable
fun AvatarVideoPlayer(
    state: AssistantState,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp
) {
    val context = LocalContext.current
    val videoUri = remember {
        Uri.parse("android.resource://${context.packageName}/${R.raw.sweety_avatar}")
    }

    val isSpeaking = state == AssistantState.SPEAKING
    val isListening = state == AssistantState.LISTENING

    val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSpeaking) 1.12f else if (isListening) 1.08f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 900 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isSpeaking) 0.85f else 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier.size(size + 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Holographic Aura Rings
        Box(
            modifier = Modifier
                .size(size + 16.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    if (isSpeaking) Brush.radialGradient(
                        listOf(CyanGlow.copy(alpha = glowAlpha), Color.Transparent)
                    )
                    else if (isListening) Brush.radialGradient(
                        listOf(PulseRed.copy(alpha = glowAlpha), Color.Transparent)
                    )
                    else Brush.radialGradient(
                        listOf(ElectricCyan.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
        )

        // Main Video Container with Neon Sci-Fi Border
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.Black)
                .border(
                    width = 2.5.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            ElectricCyan,
                            NeonBlue,
                            CyanGlow,
                            if (isSpeaking) PulseRed else ElectricCyan
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(videoUri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            // Mute video audio so Sweety's cute TTS voice plays clearly!
                            mp.setVolume(0f, 0f)
                            start()
                        }
                        setOnErrorListener { _, _, _ ->
                            // Silently recover if video format has issue on older device
                            true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

