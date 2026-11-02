package com.sweety.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class ContinuousSpeechRecognizer(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit = {},
    private val onStateChange: (Boolean) -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var selectedLanguage: String = "auto" // 'auto', 'bn-BD', 'hi-IN', 'en-US'

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition is not available on this device")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    onStateChange(true)
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    // Forward audio power dB (-2 to 10 typical) to UI wave visualizer
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    onRmsChanged(normalized)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    onRmsChanged(0f)
                }

                override fun onError(error: Int) {
                    isListening = false
                    onStateChange(false)
                    onRmsChanged(0f)
                    val errorMsg = getErrorMessage(error)
                    Log.w(TAG, "SpeechRecognizer Error: $errorMsg ($error)")

                    // Do not crash, simply notify or reset
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        // User remained silent
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    onStateChange(false)
                    onRmsChanged(0f)

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull() ?: ""
                    if (spokenText.isNotBlank()) {
                        onResult(spokenText)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            initRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            when (selectedLanguage) {
                "bn" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                "hi" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                "en" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                else -> {
                    // Multilingual support
                    putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("bn-BD", "hi-IN", "en-US"))
                }
            }
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening: ${e.message}")
            isListening = false
            onStateChange(false)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop listening: ${e.message}")
        }
        isListening = false
        onStateChange(false)
    }

    fun setLanguage(lang: String) {
        selectedLanguage = lang
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }

    private fun getErrorMessage(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
        else -> "Unknown speech error"
    }

    companion object {
        private const val TAG = "ContinuousSTT"
    }
}

