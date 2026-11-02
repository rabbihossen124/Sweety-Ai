package com.sweety.ai.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat

class ContinuousSpeechRecognizer(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit = {},
    private val onStateChange: (Boolean) -> Unit = {},
    private val onErrorNotice: (String) -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var selectedLanguage: String = "auto"
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            onStateChange(true)
                            Log.d(TAG, "SpeechRecognizer Ready for Speech")
                        }

                        override fun onBeginningOfSpeech() {
                            Log.d(TAG, "Speech input started")
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            onRmsChanged(normalized)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            onRmsChanged(0f)
                            Log.d(TAG, "Speech input ended")
                        }

                        override fun onError(error: Int) {
                            isListening = false
                            onStateChange(false)
                            onRmsChanged(0f)
                            val errorMsg = getErrorMessage(error)
                            Log.w(TAG, "SpeechRecognizer Error: $errorMsg ($error)")

                            when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH,
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                    // User didn't speak or quiet room
                                    onErrorNotice("কিছু শুনতে পাইনি রাব্বি স্যার, আবার বলুন!")
                                }
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                                    onErrorNotice("মাইক্রোফোন পারমিশন অন করা নেই!")
                                }
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                                SpeechRecognizer.ERROR_CLIENT -> {
                                    // Reset recognizer so next tap works cleanly
                                    initRecognizer()
                                }
                                else -> {
                                    // Other network or server glitch
                                }
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            isListening = false
                            onStateChange(false)
                            onRmsChanged(0f)

                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val spokenText = matches?.firstOrNull() ?: ""
                            Log.d(TAG, "Recognized Speech: $spokenText")
                            if (spokenText.isNotBlank()) {
                                onResult(spokenText)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull() ?: ""
                            if (partial.isNotBlank()) {
                                Log.d(TAG, "Partial: $partial")
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing speech recognizer: ${e.message}")
            }
        }
    }

    fun startListening() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            onErrorNotice("মাইক্রোফোন পারমিশন প্রয়োজন!")
            return
        }

        mainHandler.post {
            if (speechRecognizer == null) {
                initRecognizer()
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                when (selectedLanguage) {
                    "bn" -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                    }
                    "hi" -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    }
                    "en" -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                    }
                    else -> {
                        // Auto: default to Bengali Bangladesh with Hindi and English support
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                        putExtra(RecognizerIntent.EXTRA_ADDITIONAL_LANGUAGES, arrayOf("en-US", "hi-IN", "bn-IN"))
                    }
                }
            }

            try {
                speechRecognizer?.startListening(intent)
                isListening = true
                onStateChange(true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening: ${e.message}")
                isListening = false
                onStateChange(false)
                initRecognizer()
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop listening: ${e.message}")
            }
            isListening = false
            onStateChange(false)
        }
    }

    fun setLanguage(lang: String) {
        selectedLanguage = lang
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to destroy recognizer: ${e.message}")
            }
            speechRecognizer = null
            isListening = false
        }
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
        else -> "Unknown speech error ($errorCode)"
    }

    companion object {
        private const val TAG = "ContinuousSTT"
    }
}
