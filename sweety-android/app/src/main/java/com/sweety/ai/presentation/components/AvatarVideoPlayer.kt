package com.sweety.ai.presentation.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.sweety.ai.R
import com.sweety.ai.data.AssistantState
import com.sweety.ai.presentation.theme.CyanGlow
import com.sweety.ai.presentation.theme.ElectricCyan
import com.sweety.ai.presentation.theme.NeonBlue
import com.sweety.ai.presentation.theme.PulseRed
import com.sweety.ai.presentation.theme.Typography

@Composable
fun AvatarVideoPlayer(
    state: AssistantState,
    onAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoUri = remember {
        try {
            Uri.parse("android.resource://${context.packageName}/${R.raw.sweety_avatar}")
        } catch (e: Exception) {
            null
        }
    }

    val isSpeaking = state == AssistantState.SPEAKING
    val isListening = state == AssistantState.LISTENING
    val isActive = isSpeaking || isListening

    // GPU-accelerated scaling (NEVER resizes the SurfaceView layout, preventing 100% of crashes!)
    val avatarScale by animateFloatAsState(
        targetValue = if (isActive) 1.45f else 1.0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "avatar_gpu_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSpeaking) 1.12f else if (isListening) 1.06f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 800 else 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (isSpeaking) 0.90f else 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isVideoPrepared by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (isActive && isVideoPrepared) {
            try {
                videoViewRef?.start()
            } catch (e: Exception) {
                // Ignore transient video playback errors safely
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .scale(avatarScale)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            // Outer Holographic Aura Rings
            Box(
                modifier = Modifier
                    .size(135.dp)
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
                    .size(115.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(
                        width = if (isActive) 3.dp else 2.dp,
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
                if (videoUri != null) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                try {
                                    setVideoURI(videoUri)
                                    setOnPreparedListener { mp ->
                                        isVideoPrepared = true
                                        mp.isLooping = true
                                        mp.setVolume(0f, 0f)
                                        try {
                                            start()
                                        } catch (e: Exception) {}
                                    }
                                    setOnErrorListener { _, _, _ ->
                                        true // Prevent default error dialog
                                    }
                                    videoViewRef = this
                                } catch (e: Exception) {
                                    // Safe fallback
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

        // Live Voice Status Pill when Active
        if (isActive) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSpeaking) CyanGlow.copy(alpha = 0.20f) else PulseRed.copy(alpha = 0.20f))
                    .border(
                        width = 1.dp,
                        color = if (isSpeaking) CyanGlow else PulseRed,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isSpeaking) "🔊 সুইটি কথা বলছে..." else "🎙️ সুইটি শুনছি, বলুন রাব্বি স্যার...",
                    style = Typography.labelSmall.copy(
                        color = if (isSpeaking) CyanGlow else PulseRed,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
