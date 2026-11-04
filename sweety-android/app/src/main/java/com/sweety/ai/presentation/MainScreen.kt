package com.sweety.ai.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sweety.ai.R
import com.sweety.ai.data.AssistantState
import com.sweety.ai.presentation.components.ActionPill
import com.sweety.ai.presentation.components.AvatarVideoPlayer
import com.sweety.ai.presentation.components.ChatMessageBubble
import com.sweety.ai.presentation.components.ConfirmationDialog
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
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val countdown by viewModel.countdownSeconds.collectAsState()

    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var serverUrlInput by remember { mutableStateOf(viewModel.getServerUrl()) }

    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Keyboard detection for adaptive UI
    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    val isKeyboardOpen = imeInsets.getBottom(density) > 0

    // Auto-scroll to the bottom when messages update
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // ModalNavigationDrawer provides ChatGPT / Messenger styled sliding Chat List
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DeepNavy,
                drawerContentColor = TextPrimary,
                modifier = Modifier.width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "💬 চ্যাট হিস্ট্রি",
                            style = Typography.headlineMedium.copy(
                                fontSize = 18.sp,
                                color = ElectricCyan,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Text(
                        text = "রাব্বি স্যারের সেভ করা চ্যাট তালিকা",
                        style = Typography.labelSmall.copy(color = TextDim, fontSize = 11.sp),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // + New Chat button (ChatGPT style)
                    Button(
                        onClick = {
                            viewModel.createNewSession()
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "New Chat",
                            tint = DarkVoid,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+ নতুন চ্যাট শুরু করুন",
                            color = DarkVoid,
                            style = Typography.labelMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.8.dp)
                            .background(ElectricCyan.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Sessions List (Messenger / Facebook style)
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions, key = { it.id }) { session ->
                            val isSelected = session.id == currentSessionId
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) ElectricCyan.copy(alpha = 0.22f)
                                        else CardBackground.copy(alpha = 0.55f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.2.dp else 0.6.dp,
                                        color = if (isSelected) ElectricCyan else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.switchSession(session.id)
                                        coroutineScope.launch { drawerState.close() }
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.title,
                                            style = Typography.labelMedium.copy(
                                                color = if (isSelected) ElectricCyan else TextPrimary,
                                                fontSize = 13.sp
                                            ),
                                            maxLines = 1
                                        )
                                        if (session.lastMessage.isNotBlank()) {
                                            Text(
                                                text = session.lastMessage,
                                                style = Typography.bodySmall.copy(
                                                    color = TextDim,
                                                    fontSize = 11.sp
                                                ),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    if (sessions.size > 1) {
                                        IconButton(
                                            onClick = { viewModel.deleteSession(session.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteSweep,
                                                contentDescription = "Delete Session",
                                                tint = TextDim,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        // Main Screen Content Canvas
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
                alpha = 0.85f
            )

            // Gradient dark scrim overlay ONLY at top and bottom edges
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DarkVoid.copy(alpha = 0.50f),
                                Color.Transparent,
                                DarkVoid.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {

                // 1. Top Header Bar (With Chat History Drawer Hamburger Button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // ☰ Chat History Drawer Button
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Chat History",
                                tint = ElectricCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
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
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SWEETY AI",
                                    style = Typography.headlineMedium.copy(
                                        letterSpacing = 2.sp,
                                        color = TextPrimary,
                                        fontSize = 17.sp
                                    )
                                )
                            }

                            val statusText = when (state) {
                                AssistantState.LISTENING -> if (countdown > 0) "LISTENING (${countdown}s)" else "LISTENING..."
                                AssistantState.SPEAKING -> "SPEAKING..."
                                AssistantState.THINKING -> "THINKING..."
                                AssistantState.ACTION_EXECUTING -> "EXECUTING..."
                                AssistantState.WAITING_CONFIRMATION -> "CONFIRMATION NEEDED"
                                else -> "NOTHING PHONE (3A) READY"
                            }

                            Text(
                                text = statusText,
                                style = Typography.labelSmall.copy(
                                    color = when (state) {
                                        AssistantState.LISTENING -> PulseRed
                                        AssistantState.SPEAKING -> CyanGlow
                                        AssistantState.THINKING -> AccentWarning
                                        else -> ElectricCyan
                                    },
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(start = 15.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // ➕ New Chat Quick Button
                        Button(
                            onClick = { viewModel.createNewSession() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan.copy(alpha = 0.22f)
                            ),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "New Chat",
                                tint = ElectricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "New",
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
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text(
                                text = "🌐 $langLabel",
                                style = Typography.labelSmall.copy(color = NeonBlue, fontSize = 11.sp)
                            )
                        }

                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 2. Comprehensive Quick Control Action Pills (YouTube, Wi-Fi, Bluetooth, Data, Light, Call, File)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionPill("⚡ Flashlight", onClick = {
                        viewModel.processUserPrompt("Flashlight toggle")
                    })
                    ActionPill("▶️ YouTube Song", onClick = {
                        viewModel.processUserPrompt("ইউটিউবে গান চালাও")
                    })
                    ActionPill("📶 Wi-Fi", onClick = {
                        viewModel.processUserPrompt("wifi on")
                    })
                    ActionPill("🔵 Bluetooth", onClick = {
                        viewModel.processUserPrompt("bluetooth on")
                    })
                    ActionPill("🌐 Mobile Data", onClick = {
                        viewModel.processUserPrompt("ডাটা অন")
                    })
                    ActionPill("📁 Share File", onClick = {
                        viewModel.deviceManager.shareFile()
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

                // 3. Full-Screen Chat Conversation Canvas (Takes full height & width!)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(message = msg)
                    }
                }

                // 4. Continuous Follow-Up Listening Notification Bar
                AnimatedVisibility(
                    visible = state == AssistantState.LISTENING && countdown > 0,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepNavy)
                            .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎙️ Sweety শুনছে... পরবর্তী কাজ বলুন (${countdown}s বাকি)",
                            style = Typography.labelSmall.copy(color = ElectricCyan, fontSize = 11.sp)
                        )
                    }
                }

                // 5. Bottom Interactive Live Avatar Video + Keyboard Adaptive Input Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark.copy(alpha = 0.85f))
                        .border(
                            width = 0.8.dp,
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, ElectricCyan.copy(alpha = 0.40f), Color.Transparent)
                            ),
                            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                        )
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Video Avatar in the central mic position (collapses automatically when keyboard opens!)
                    AnimatedVisibility(
                        visible = !isKeyboardOpen,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            AvatarVideoPlayer(
                                state = state,
                                onAvatarClick = { viewModel.toggleVoiceInput() },
                                sizeDp = 105.dp,
                                showStatusPill = true
                            )

                            // Subtle dynamic voice visualizer wave underneath avatar when active
                            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                                NeonVisualizerWave(
                                    state = state,
                                    audioLevel = audioRms,
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(30.dp)
                                )
                            }
                        }
                    }

                    // Text Input + Actions Row (Matches keyboard position seamlessly)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 📎 File Attachment / Share Button (Audio, Video, Files)
                        IconButton(
                            onClick = { viewModel.deviceManager.shareFile() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CardBackground.copy(alpha = 0.70f))
                                .border(0.8.dp, ElectricCyan.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share File",
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // When keyboard is open, show compact voice mic button on the side
                        if (isKeyboardOpen) {
                            IconButton(
                                onClick = { viewModel.toggleVoiceInput() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (state == AssistantState.LISTENING) PulseRed
                                        else ElectricCyan.copy(alpha = 0.25f)
                                    )
                                    .border(1.dp, ElectricCyan, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = "Voice Input",
                                    tint = if (state == AssistantState.LISTENING) Color.White else ElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = {
                                Text(
                                    "সুইটিকে লিখুন বা গান/অ্যাপ সার্চ করুন...",
                                    color = TextDim,
                                    style = Typography.bodyMedium.copy(fontSize = 12.sp)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp)),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CardBackground.copy(alpha = 0.85f),
                                unfocusedContainerColor = CardBackground.copy(alpha = 0.65f),
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    viewModel.processUserPrompt(textInput, fromVoice = false)
                                    textInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(ElectricCyan, NeonBlue)))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = "Send",
                                tint = DarkVoid,
                                modifier = Modifier.size(19.dp)
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
                                "Android Emulator: http://10.0.2.2:5000\nNothing Phone (3a) / Physical Device: http://<YOUR_PC_IP>:5000",
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
}
