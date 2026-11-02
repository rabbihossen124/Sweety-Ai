package com.sweety.ai.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sweety.ai.R
import com.sweety.ai.data.AssistantState
import com.sweety.ai.presentation.components.ActionPill
import com.sweety.ai.presentation.components.AvatarVideoPlayer
import com.sweety.ai.presentation.components.ChatMessageBubble
import com.sweety.ai.presentation.components.ConfirmationDialog
import com.sweety.ai.presentation.components.GlowingMicButton
import com.sweety.ai.presentation.components.NeonVisualizerWave
import com.sweety.ai.presentation.theme.AccentSuccess
import com.sweety.ai.presentation.theme.AccentWarning
import com.sweety.ai.presentation.theme.CardBackground
import com.sweety.ai.presentation.theme.CyanGlow
import com.sweety.ai.presentation.theme.DarkVoid
import com.sweety.ai.presentation.theme.DeepNavy
import com.sweety.ai.presentation.theme.ElectricCyan
import com.sweety.ai.presentation.theme.NeonBlue
import com.sweety.ai.presentation.theme.PulseRed
import com.sweety.ai.presentation.theme.SurfaceDark
import com.sweety.ai.presentation.theme.TextDim
import com.sweety.ai.presentation.theme.TextPrimary
import com.sweety.ai.presentation.theme.TextSecondary
import com.sweety.ai.presentation.theme.Typography

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val countdown by viewModel.countdownSeconds.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var serverUrlInput by remember { mutableStateOf(viewModel.getServerUrl()) }

    val listState = rememberLazyListState()

    // Auto-scroll to the bottom when messages update
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkVoid)
    ) {
        // High-clarity Sweety AI Companion Background Art (Bright & Vivid)
        Image(
            painter = painterResource(id = R.drawable.sweety_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.85f // High clarity so Rabbi Sir's photo and poster are vivid and sharp!
        )

        // Gradient dark scrim overlay ONLY at top and bottom edges so the middle artwork is 100% visible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkVoid.copy(alpha = 0.45f),
                            Color.Transparent,
                            DarkVoid.copy(alpha = 0.65f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (state) {
                                        AssistantState.LISTENING -> PulseRed
                                        AssistantState.SPEAKING -> CyanGlow
                                        AssistantState.THINKING -> AccentWarning
                                        else -> AccentSuccess
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SWEETY AI",
                            style = Typography.headlineMedium.copy(
                                letterSpacing = 2.sp,
                                color = TextPrimary
                            )
                        )
                    }

                    val statusText = when (state) {
                        AssistantState.LISTENING -> if (countdown > 0) "LISTENING (FOLLOW-UP ${countdown}s)" else "LISTENING..."
                        AssistantState.SPEAKING -> "SPEAKING..."
                        AssistantState.THINKING -> "PROCESSING INTENT..."
                        AssistantState.ACTION_EXECUTING -> "EXECUTING COMMAND..."
                        AssistantState.WAITING_CONFIRMATION -> "AWAITING CONFIRMATION"
                        else -> "SYSTEM READY"
                    }

                    Text(
                        text = statusText,
                        style = Typography.labelSmall.copy(
                            color = when (state) {
                                AssistantState.LISTENING -> PulseRed
                                AssistantState.SPEAKING -> CyanGlow
                                AssistantState.THINKING -> AccentWarning
                                else -> ElectricCyan
                            }
                        ),
                        modifier = Modifier.padding(start = 18.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ➕ New Chat Button
                    Button(
                        onClick = { viewModel.startNewChat() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan.copy(alpha = 0.22f)
                        ),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "New Chat",
                            tint = ElectricCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "New Chat",
                            style = Typography.labelSmall.copy(color = ElectricCyan, fontSize = 11.sp)
                        )
                    }

                    // Language Switcher Pill
                    val langLabel = when (selectedLanguage) {
                        "bn" -> "বাংলা"
                        "hi" -> "हिन्दी"
                        "en" -> "ENG"
                        else -> "AUTO"
                    }
                    TextButton(
                        onClick = {
                            val nextLang = when (selectedLanguage) {
                                "auto" -> "bn"
                                "bn" -> "hi"
                                "hi" -> "en"
                                else -> "auto"
                            }
                            viewModel.setLanguage(nextLang)
                        }
                    ) {
                        Text(
                            text = "🌐 $langLabel",
                            style = Typography.labelSmall.copy(color = NeonBlue)
                        )
                    }

                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear Chat",
                            tint = TextDim
                        )
                    }

                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = ElectricCyan
                        )
                    }
                }
            }

            // Quick Action Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionPill("⚡ Flashlight", onClick = {
                    viewModel.processUserPrompt("Flashlight toggle")
                })
                ActionPill("▶️ YouTube", onClick = {
                    viewModel.processUserPrompt("Open YouTube")
                })
                ActionPill("🔋 Battery", onClick = {
                    viewModel.processUserPrompt("Check battery level")
                })
                ActionPill("🔒 Lock Screen", onClick = {
                    viewModel.processUserPrompt("Lock phone")
                })
                ActionPill("💬 WhatsApp", onClick = {
                    viewModel.processUserPrompt("Open WhatsApp")
                })
            }

            // Holographic Animated Video Avatar of Sweety AI (Expands live when speaking / listening!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                AvatarVideoPlayer(
                    state = state,
                    onAvatarClick = { viewModel.toggleVoiceInput() }
                )
            }

            // Neon Wave Audio Visualizer
            NeonVisualizerWave(
                state = state,
                audioLevel = audioRms,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // Conversation Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(message = msg)
                }
            }

            // Continuous Follow-Up Listening Notification Bar
            AnimatedVisibility(
                visible = state == AssistantState.LISTENING && countdown > 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeepNavy)
                        .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎙️ Sweety শুনছে... পরবর্তী কাজ বলুন (${countdown}s বাকি)",
                        style = Typography.labelSmall.copy(color = ElectricCyan, fontSize = 11.sp)
                    )
                }
            }

            // Bottom Control Area: Mic + Text Input (Glassmorphic)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark.copy(alpha = 0.70f))
                    .border(
                        width = 0.8.dp,
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, ElectricCyan.copy(alpha = 0.35f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Central Glowing Mic Button
                GlowingMicButton(
                    state = state,
                    onClick = { viewModel.toggleVoiceInput() }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Text Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                "কথা বলুন বা লিখুন...",
                                color = TextDim,
                                style = Typography.bodyMedium
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardBackground.copy(alpha = 0.75f),
                            unfocusedContainerColor = CardBackground.copy(alpha = 0.60f),
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.processUserPrompt(textInput)
                                textInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(ElectricCyan, NeonBlue)))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = DarkVoid
                        )
                    }
                }
            }
        }

        // Consequential Action Confirmation Modal
        pendingConfirmation?.let { confirm ->
            ConfirmationDialog(
                confirmation = confirm,
                onDismiss = { viewModel.dismissConfirmation() }
            )
        }

        // Backend URL Settings Dialog
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Backend Server URL", color = TextPrimary) },
                text = {
                    Column {
                        Text(
                            "Android Emulator: http://10.0.2.2:5000\nBlueStacks or Physical Device: http://<YOUR_PC_IP>:5000",
                            style = Typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = serverUrlInput,
                            onValueChange = { serverUrlInput = it },
                            label = { Text("Server URL") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.setServerUrl(serverUrlInput)
                            showSettingsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                    ) {
                        Text("Save", color = DarkVoid)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Cancel", color = TextDim)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}

