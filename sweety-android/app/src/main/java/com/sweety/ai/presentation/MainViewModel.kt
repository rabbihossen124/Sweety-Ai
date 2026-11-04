package com.sweety.ai.presentation

import android.app.Application
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sweety.ai.data.ActionPayload
import com.sweety.ai.data.ApiClient
import com.sweety.ai.data.AssistantState
import com.sweety.ai.data.ChatMessage
import com.sweety.ai.data.ChatSession
import com.sweety.ai.data.PendingConfirmation
import com.sweety.ai.device.DeviceActionManager
import com.sweety.ai.domain.CommandDispatcher
import com.sweety.ai.voice.ContinuousSpeechRecognizer
import com.sweety.ai.voice.MultilingualTextToSpeech
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    private val apiClient = ApiClient(context)
    val deviceManager = DeviceActionManager(context)
    private var tts: MultilingualTextToSpeech? = null
    private var stt: ContinuousSpeechRecognizer? = null

    private lateinit var commandDispatcher: CommandDispatcher

    private val _state = MutableStateFlow(AssistantState.IDLE)
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _currentSessionId = MutableStateFlow("session_default")
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private val _sessions = MutableStateFlow<List<ChatSession>>(
        listOf(
            ChatSession(
                id = "session_default",
                title = "সুইটি অ্যাসিস্ট্যান্ট (মূল আড্ডা)",
                lastMessage = "আসসালামু আলাইকুম রাব্বি স্যার!",
                timestamp = System.currentTimeMillis()
            )
        )
    )
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val sessionMessages = mutableMapOf<String, List<ChatMessage>>()

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
    private var sessionId: String = "session_default"

    init {
        try {
            commandDispatcher = CommandDispatcher(
                deviceManager = deviceManager,
                onConfirmationRequired = { confirm ->
                    _state.value = AssistantState.WAITING_CONFIRMATION
                    _pendingConfirmation.value = confirm
                }
            )

            tts = MultilingualTextToSpeech(
                context = context,
                getBaseUrl = { apiClient.getBaseUrl() }
            )

            stt = ContinuousSpeechRecognizer(
                context = context,
                onResult = { spokenText ->
                    processUserPrompt(spokenText, fromVoice = true)
                },
                onRmsChanged = { rms ->
                    _audioRms.value = rms
                },
                onStateChange = { isListening ->
                    if (isListening && _state.value != AssistantState.SPEAKING && _state.value != AssistantState.THINKING) {
                        _state.value = AssistantState.LISTENING
                    } else if (!isListening && _state.value == AssistantState.LISTENING) {
                        _state.value = AssistantState.IDLE
                    }
                },
                onErrorNotice = { notice ->
                    if (_state.value == AssistantState.LISTENING) {
                        _state.value = AssistantState.IDLE
                    }
                }
            )
        } catch (t: Throwable) {
            Log.e(TAG, "MainViewModel init error: ${t.message}", t)
        }
    }

    fun testCuteVoice() {
        val sample = "আসসালামু আলাইকুম রাব্বি স্যার! আমি আপনার মিষ্টি সুইটি, বলুন আমি আপনাকে কীভাবে সাহায্য করতে পারি?"
        tts?.speak(sample)
    }

    fun onClapWakeUp() {
        viewModelScope.launch(Dispatchers.Main) {
            cancelFollowUpTimer()
            tts?.stop()
            val wakeGreeting = "জি রাব্বি স্যার! সুইটি শুনছি, বলুন কী করতে হবে?"
            val assistantMsg = ChatMessage(text = wakeGreeting, isUser = false)
            _messages.value = _messages.value + assistantMsg

            _state.value = AssistantState.SPEAKING
            tts?.speak(wakeGreeting) {
                viewModelScope.launch(Dispatchers.Main) {
                    _state.value = AssistantState.LISTENING
                    stt?.startListening()
                }
            }
        }
    }

    private fun containsOtherAssistant(text: String): Boolean {
        val lower = text.lowercase().trim()
        val otherAssistants = listOf(
            "alexa", "অ্যালেক্সা", "aleksa",
            "siri", "সিরি",
            "google", "গুগল", "ok google", "hey google",
            "bixby", "বিক্সবি",
            "cortana", "কর্টানা",
            "jarvis", "জার্ভিস"
        )
        return otherAssistants.any { lower.contains(it) }
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
        viewModelScope.launch(Dispatchers.Main) {
            try {
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
            } catch (t: Throwable) {
                Log.e(TAG, "toggleVoiceInput error: ${t.message}")
                _state.value = AssistantState.IDLE
            }
        }
    }

    fun processUserPrompt(prompt: String, fromVoice: Boolean = false) {
        if (prompt.isBlank()) return

        // 1. Other assistant name rejection:
        // "mukdie sweety bole dakbo tokn res korbe ono name ba kasue bole dakle res korbe na"
        if (fromVoice && containsOtherAssistant(prompt)) {
            Log.w(TAG, "Ignoring prompt directed to another assistant: $prompt")
            _state.value = AssistantState.IDLE
            return
        }

        cancelFollowUpTimer()
        try {
            stt?.stopListening()
        } catch (t: Throwable) {}

        // 2. Add user message to chat UI
        val userMsg = ChatMessage(text = prompt, isUser = true)
        _messages.value = _messages.value + userMsg

        // Update active session snippet & title
        val preview = if (prompt.length > 25) prompt.take(25) + "..." else prompt
        _sessions.value = _sessions.value.map { sess ->
            if (sess.id == _currentSessionId.value) {
                val newTitle = if (sess.title.startsWith("কথোপকথন #") || sess.title.startsWith("সুইটি অ্যাসিস্ট্যান্ট")) preview else sess.title
                sess.copy(title = newTitle, lastMessage = prompt, timestamp = System.currentTimeMillis())
            } else sess
        }

        // 3. Set thinking state
        _state.value = AssistantState.THINKING

        viewModelScope.launch(Dispatchers.Main) {
            try {
                // 4. Fast-path local intent matching (~10ms execution without waiting for backend)
                val fastMatch = commandDispatcher.tryFastLocalIntent(prompt)
                if (fastMatch.first && fastMatch.second != null) {
                    val feedback = fastMatch.second!!
                    handleAssistantReply(
                        text = feedback,
                        actions = emptyList(),
                        continueListening = fromVoice,
                        fromVoice = fromVoice
                    )
                    return@launch
                }

                // 5. Send to Node.js / Gemini Backend
                val response = apiClient.sendChatMessage(
                    message = prompt,
                    sessionId = sessionId
                )

                if (response.success) {
                    // Execute any returned tool actions
                    val executedActions = mutableListOf<ActionPayload>()
                    for (action in response.actions) {
                        try {
                            commandDispatcher.dispatchAction(action) { success, feedback ->
                                executedActions.add(action)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Action dispatch exception: ${e.message}")
                        }
                    }

                    handleAssistantReply(
                        text = response.text,
                        actions = executedActions,
                        continueListening = if (fromVoice) response.continueListening else false,
                        fromVoice = fromVoice
                    )
                } else {
                    handleAssistantReply(
                        text = response.text,
                        actions = emptyList(),
                        continueListening = false,
                        fromVoice = fromVoice
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "processUserPrompt error: ${e.message}")
                handleAssistantReply(
                    text = "দুঃখিত রাব্বি স্যার, একটি সমস্যা হয়েছে: ${e.localizedMessage ?: "সার্ভার এরর"}",
                    actions = emptyList(),
                    continueListening = false,
                    fromVoice = fromVoice
                )
            }
        }
    }

    private fun handleAssistantReply(
        text: String,
        actions: List<ActionPayload>,
        continueListening: Boolean,
        fromVoice: Boolean = true
    ) {
        val assistantMsg = ChatMessage(
            text = text,
            isUser = false,
            actions = actions
        )
        _messages.value = _messages.value + assistantMsg

        if (fromVoice) {
            // Speak response via Multilingual TTS
            _state.value = AssistantState.SPEAKING
            try {
                tts?.speak(text) {
                    // On speech done:
                    viewModelScope.launch(Dispatchers.Main) {
                        try {
                            if (continueListening) {
                                startContinuousListeningWindow()
                            } else {
                                _state.value = AssistantState.IDLE
                            }
                        } catch (t: Throwable) {
                            _state.value = AssistantState.IDLE
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "TTS speak error: ${t.message}")
                _state.value = AssistantState.IDLE
            }
        } else {
            // Text chat mode: answered directly in chat
            _state.value = AssistantState.IDLE
        }
    }

    /**
     * Re-arms mic for follow-up turns without repeating wake-word
     */
    private fun startContinuousListeningWindow() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
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
            } catch (t: Throwable) {
                Log.w(TAG, "CountDownTimer startup error: ${t.message}")
                _state.value = AssistantState.IDLE
            }
        }
    }

    private fun cancelFollowUpTimer() {
        try {
            followUpTimer?.cancel()
        } catch (e: Exception) {}
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
        createNewSession()
    }

    fun switchSession(newSessionId: String) {
        if (newSessionId == _currentSessionId.value) return
        cancelFollowUpTimer()
        try { stt?.stopListening() } catch (t: Throwable) {}

        sessionMessages[_currentSessionId.value] = _messages.value
        _currentSessionId.value = newSessionId
        sessionId = newSessionId

        val existing = sessionMessages[newSessionId]
        if (existing != null) {
            _messages.value = existing
        } else {
            val greeting = com.sweety.ai.domain.GreetingHelper.generateWelcomeGreeting("রাব্বি স্যার")
            _messages.value = listOf(ChatMessage(text = greeting, isUser = false))
        }
    }

    fun createNewSession() {
        cancelFollowUpTimer()
        try { stt?.stopListening() } catch (t: Throwable) {}

        sessionMessages[_currentSessionId.value] = _messages.value
        val newId = "session_${System.currentTimeMillis()}"
        val newSession = ChatSession(
            id = newId,
            title = "কথোপকথন #${_sessions.value.size + 1}",
            lastMessage = "নতুন চ্যাট শুরু হয়েছে",
            timestamp = System.currentTimeMillis()
        )
        _sessions.value = listOf(newSession) + _sessions.value
        _currentSessionId.value = newId
        sessionId = newId

        val greeting = com.sweety.ai.domain.GreetingHelper.generateWelcomeGreeting("রাব্বি স্যার")
        _messages.value = listOf(ChatMessage(text = greeting, isUser = false))
    }

    fun deleteSession(targetSessionId: String) {
        if (_sessions.value.size <= 1) return
        val wasCurrent = targetSessionId == _currentSessionId.value
        _sessions.value = _sessions.value.filter { it.id != targetSessionId }
        sessionMessages.remove(targetSessionId)
        if (wasCurrent) {
            val next = _sessions.value.first()
            _currentSessionId.value = next.id
            sessionId = next.id
            _messages.value = sessionMessages[next.id] ?: listOf(
                ChatMessage(text = com.sweety.ai.domain.GreetingHelper.generateWelcomeGreeting("রাব্বি স্যার"), isUser = false)
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelFollowUpTimer()
        try {
            tts?.shutdown()
        } catch (t: Throwable) {}
        try {
            stt?.destroy()
        } catch (t: Throwable) {}
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
