package com.sweety.ai.presentation

import android.app.Application
import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sweety.ai.data.ActionPayload
import com.sweety.ai.data.ApiClient
import com.sweety.ai.data.AssistantState
import com.sweety.ai.data.ChatMessage
import com.sweety.ai.data.PendingConfirmation
import com.sweety.ai.device.DeviceActionManager
import com.sweety.ai.domain.CommandDispatcher
import com.sweety.ai.voice.ContinuousSpeechRecognizer
import com.sweety.ai.voice.MultilingualTextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    private val apiClient = ApiClient()
    val deviceManager = DeviceActionManager(context)
    private var tts: MultilingualTextToSpeech? = null
    private var stt: ContinuousSpeechRecognizer? = null

    private lateinit var commandDispatcher: CommandDispatcher

    private val _state = MutableStateFlow(AssistantState.IDLE)
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = com.sweety.ai.domain.GreetingHelper.generateWelcomeGreeting("রাব্বি স্যার"),
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("auto")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(0)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    private var followUpTimer: CountDownTimer? = null
    private var sessionId: String = "session_${System.currentTimeMillis()}"

    init {
        commandDispatcher = CommandDispatcher(
            deviceManager = deviceManager,
            onConfirmationRequired = { confirm ->
                _state.value = AssistantState.WAITING_CONFIRMATION
                _pendingConfirmation.value = confirm
            }
        )

        tts = MultilingualTextToSpeech(context)
        stt = ContinuousSpeechRecognizer(
            context = context,
            onResult = { spokenText ->
                processUserPrompt(spokenText)
            },
            onRmsChanged = { rms ->
                _audioRms.value = rms
            },
            onStateChange = { isListening ->
                if (isListening && _state.value != AssistantState.SPEAKING && _state.value != AssistantState.THINKING) {
                    _state.value = AssistantState.LISTENING
                }
            }
        )
    }

    fun setServerUrl(url: String) {
        apiClient.setBaseUrl(url)
    }

    fun getServerUrl(): String = apiClient.getBaseUrl()

    fun setLanguage(lang: String) {
        _selectedLanguage.value = lang
        stt?.setLanguage(lang)
    }

    fun toggleVoiceInput() {
        if (_state.value == AssistantState.LISTENING) {
            cancelFollowUpTimer()
            stt?.stopListening()
            _state.value = AssistantState.IDLE
        } else {
            cancelFollowUpTimer()
            tts?.stop()
            _state.value = AssistantState.LISTENING
            stt?.startListening()
        }
    }

    fun processUserPrompt(prompt: String) {
        if (prompt.isBlank()) return

        cancelFollowUpTimer()
        stt?.stopListening()

        // 1. Add user message to chat UI
        val userMsg = ChatMessage(text = prompt, isUser = true)
        _messages.value = _messages.value + userMsg

        // 2. Set thinking state
        _state.value = AssistantState.THINKING

        viewModelScope.launch {
            // 3. Fast-path local intent matching (~10ms execution without waiting for backend)
            val fastMatch = commandDispatcher.tryFastLocalIntent(prompt)
            if (fastMatch.first && fastMatch.second != null) {
                val feedback = fastMatch.second!!
                handleAssistantReply(feedback, actions = emptyList(), continueListening = true)
                return@launch
            }

            // 4. Send to Node.js / Gemini Backend
            val response = apiClient.sendChatMessage(
                message = prompt,
                sessionId = sessionId
            )

            if (response.success) {
                // Execute any returned tool actions
                val executedActions = mutableListOf<ActionPayload>()
                for (action in response.actions) {
                    commandDispatcher.dispatchAction(action) { success, feedback ->
                        executedActions.add(action)
                    }
                }

                handleAssistantReply(
                    text = response.text,
                    actions = executedActions,
                    continueListening = response.continueListening
                )
            } else {
                handleAssistantReply(
                    text = response.text,
                    actions = emptyList(),
                    continueListening = false
                )
            }
        }
    }

    private fun handleAssistantReply(
        text: String,
        actions: List<ActionPayload>,
        continueListening: Boolean
    ) {
        val assistantMsg = ChatMessage(
            text = text,
            isUser = false,
            actions = actions
        )
        _messages.value = _messages.value + assistantMsg

        // Speak response via Multilingual TTS
        _state.value = AssistantState.SPEAKING
        tts?.speak(text) {
            // On speech done:
            if (continueListening) {
                startContinuousListeningWindow()
            } else {
                _state.value = AssistantState.IDLE
            }
        }
    }

    /**
     * Re-arms mic for follow-up turns without repeating wake-word
     */
    private fun startContinuousListeningWindow() {
        _state.value = AssistantState.LISTENING
        stt?.startListening()

        // 7-second countdown timer for next chained command
        cancelFollowUpTimer()
        followUpTimer = object : CountDownTimer(7000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _countdownSeconds.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                _countdownSeconds.value = 0
                if (_state.value == AssistantState.LISTENING) {
                    stt?.stopListening()
                    _state.value = AssistantState.IDLE
                }
            }
        }.start()
    }

    private fun cancelFollowUpTimer() {
        followUpTimer?.cancel()
        followUpTimer = null
        _countdownSeconds.value = 0
    }

    fun dismissConfirmation() {
        _pendingConfirmation.value = null
        _state.value = AssistantState.IDLE
    }

    fun clearChat() {
        startNewChat()
    }

    fun startNewChat() {
        cancelFollowUpTimer()
        stt?.stopListening()
        val greeting = com.sweety.ai.domain.GreetingHelper.generateWelcomeGreeting("রাব্বি স্যার")
        _messages.value = listOf(
            ChatMessage(
                text = greeting,
                isUser = false
            )
        )
        sessionId = "session_${System.currentTimeMillis()}"
        _state.value = AssistantState.SPEAKING
        tts?.speak(greeting) {
            startContinuousListeningWindow()
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelFollowUpTimer()
        tts?.shutdown()
        stt?.destroy()
    }
}

