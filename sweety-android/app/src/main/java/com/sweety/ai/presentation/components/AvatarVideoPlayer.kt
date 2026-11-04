package com.sweety.ai.presentation.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.sweety.ai.R
import com.sweety.ai.data.AssistantState
import com.sweety.ai.presentation.theme.CyanGlow
import com.sweety.ai.presentation.theme.DarkVoid
import com.sweety.ai.presentation.theme.ElectricCyan
import com.sweety.ai.presentation.theme.NeonBlue
import com.sweety.ai.presentation.theme.PulseRed
import com.sweety.ai.presentation.theme.Typography

/**
 * Premium Holographic AI Avatar with Live Video Playback.
 * Uses TextureView (NOT SurfaceView) to ensure 100% crash-free GPU rendering
 * and circular clipping with Jetpack Compose on Android 14 / Nothing Phone 3a.
 */
@Composable
fun AvatarVideoPlayer(
    state: AssistantState,
    onAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    sizeDp: Dp = 100.dp,
    showStatusPill: Boolean = true
) {
    val context = LocalContext.current
    val isSpeaking = state == AssistantState.SPEAKING
    val isListening = state == AssistantState.LISTENING
    val isThinking = state == AssistantState.THINKING
    val isActive = isSpeaking || isListening || isThinking

    // Smooth reactive scale
    val avatarScale by animateFloatAsState(
        targetValue = when {
            isSpeaking -> 1.15f
            isListening -> 1.18f
            isThinking -> 1.08f
            else -> 1.0f
        },
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "avatar_reactive_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "hologram_anims")

    // Continuous breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = if (isActive) 1.08f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 900 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_pulse"
    )

    // Sci-fi orbital halo rotation
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 4000 else 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rotation"
    )

    // Dynamic aura glow intensity
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = if (isSpeaking) 0.85f else if (isListening) 0.75f else 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val primaryColor = when {
        isListening -> PulseRed
        isSpeaking -> ElectricCyan
        isThinking -> NeonBlue
        else -> ElectricCyan
    }

    val secondaryColor = when {
        isListening -> Color(0xFFFF8A80)
        isSpeaking -> CyanGlow
        isThinking -> ElectricCyan
        else -> NeonBlue
    }

    // Live Video Player using TextureView (100% crash-free)
    val mediaPlayer = remember { MediaPlayer() }
    var isVideoReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        try {
            val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.sweety_avatar}")
            mediaPlayer.setDataSource(context, videoUri)
            mediaPlayer.isLooping = true
            mediaPlayer.setVolume(0f, 0f)
            mediaPlayer.setOnPreparedListener {
                isVideoReady = true
                if (isActive) {
                    try { mediaPlayer.start() } catch (e: Exception) {}
                }
            }
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            Log.w("AvatarVideoPlayer", "Could not initialize avatar video: ${e.message}")
        }

        onDispose {
            try {
                mediaPlayer.stop()
            } catch (e: Exception) {}
            try {
                mediaPlayer.reset()
                mediaPlayer.release()
            } catch (e: Exception) {}
        }
    }

    // React to speaking / listening states to start or pause avatar video
    LaunchedEffect(isActive, isVideoReady) {
        if (isVideoReady) {
            try {
                if (isActive) {
                    if (!mediaPlayer.isPlaying) {
                        mediaPlayer.start()
                    }
                } else {
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                    }
                }
            } catch (e: Exception) {
                Log.w("AvatarVideoPlayer", "Player start/pause error: ${e.message}")
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp)
                .scale(avatarScale * breathingPulse)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            // 1. Outermost Glowing Radial Energy Field
            Box(
                modifier = Modifier
                    .size(sizeDp * 0.96f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = glowAlpha),
                                secondaryColor.copy(alpha = glowAlpha * 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 2. Futuristic Cybernetic Rotating Orbit Ring
            Box(
                modifier = Modifier
                    .size(sizeDp * 0.88f)
                    .rotate(orbitRotation)
                    .clip(CircleShape)
                    .border(
                        width = 1.8.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                primaryColor,
                                Color.Transparent,
                                secondaryColor,
                                Color.Transparent,
                                primaryColor
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // 3. Counter-rotating Accent Ring
            Box(
                modifier = Modifier
                    .size(sizeDp * 0.80f)
                    .rotate(-orbitRotation * 1.5f)
                    .clip(CircleShape)
                    .border(
                        width = 1.2.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                secondaryColor.copy(alpha = 0.6f),
                                Color.Transparent,
                                primaryColor.copy(alpha = 0.8f)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // 4. Central Live Video & Hologram Portrait Container
            Box(
                modifier = Modifier
                    .size(sizeDp * 0.72f)
                    .clip(CircleShape)
                    .background(DarkVoid)
                    .border(
                        width = 2.5.dp,
                        brush = Brush.linearGradient(
                            listOf(primaryColor, secondaryColor)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Static high-res background fallback
                Image(
                    painter = painterResource(id = R.drawable.ic_sweety_logo),
                    contentDescription = "Sweety AI Portrait",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Live Video Player via TextureView (smoothly animated live in real-time)
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(
                                    surfaceTexture: SurfaceTexture,
                                    width: Int,
                                    height: Int
                                ) {
                                    try {
                                        val surface = Surface(surfaceTexture)
                                        mediaPlayer.setSurface(surface)
                                        if (isActive && isVideoReady) {
                                            try { mediaPlayer.start() } catch (e: Exception) {}
                                        }
                                    } catch (e: Exception) {
                                        Log.w("AvatarVideoPlayer", "TextureView surface attach error: ${e.message}")
                                    }
                                }

                                override fun onSurfaceTextureSizeChanged(
                                    surface: SurfaceTexture,
                                    width: Int,
                                    height: Int
                                ) {}

                                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                    try {
                                        mediaPlayer.setSurface(null)
                                    } catch (e: Exception) {}
                                    return true
                                }

                                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )

                // Holographic scanline sheen overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    primaryColor.copy(alpha = 0.12f),
                                    Color.Transparent,
                                    primaryColor.copy(alpha = 0.08f)
                                )
                            )
                        )
                )
            }
        }

        // Live Dynamic Status Pill
        if (showStatusPill) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(primaryColor.copy(alpha = 0.18f))
                    .border(
                        width = 1.dp,
                        color = primaryColor.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isSpeaking -> "🔊 সুইটি কথা বলছে..."
                            isListening -> "🎙️ সুইটি শুনছি, বলুন রাব্বি স্যার..."
                            isThinking -> "🧠 ভাবছি..."
                            else -> "✨ Sweety AI Online (ট্যাপ করে বলুন)"
                        },
                        style = Typography.labelSmall.copy(
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
