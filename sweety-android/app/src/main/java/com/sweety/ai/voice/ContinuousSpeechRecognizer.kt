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
        mainHandler.post {
            try {
                ensureRecognizerReady()
            } catch (t: Throwable) {
                Log.e(TAG, "Init ensureRecognizerReady error: ${t.message}")
            }
        }
    }

    private fun ensureRecognizerReady(): Boolean {
        if (speechRecognizer != null) return true

        return try {
            val available = SpeechRecognizer.isRecognitionAvailable(context)
            if (!available) {
                Log.w(TAG, "Speech recognition is marked not available on this device")
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        try {
                            isListening = true
                            onStateChange(true)
                            Log.d(TAG, "SpeechRecognizer Ready for Speech")
                        } catch (t: Throwable) {
                            Log.e(TAG, "onReadyForSpeech error: ${t.message}")
                        }
                    }

                    override fun onBeginningOfSpeech() {
                        try {
                            Log.d(TAG, "Speech input started")
                        } catch (t: Throwable) {}
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        try {
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            onRmsChanged(normalized)
                        } catch (t: Throwable) {}
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        try {
                            onRmsChanged(0f)
                            Log.d(TAG, "Speech input ended")
                        } catch (t: Throwable) {}
                    }

                    override fun onError(error: Int) {
                        try {
                            isListening = false
                            onStateChange(false)
                            onRmsChanged(0f)
                            val errorMsg = getErrorMessage(error)
                            Log.w(TAG, "SpeechRecognizer Error: $errorMsg ($error)")

                            when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH,
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                    onErrorNotice("কিছু শুনতে পাইনি রাব্বি স্যার, আবার বলুন!")
                                }
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                                    onErrorNotice("মাইক্রোফোন পারমিশন প্রয়োজন!")
                                }
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                                SpeechRecognizer.ERROR_CLIENT -> {
                                    try {
                                        speechRecognizer?.cancel()
                                    } catch (t: Throwable) {}
                                }
                            }
                        } catch (t: Throwable) {
                            Log.e(TAG, "onError callback exception: ${t.message}")
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        try {
                            isListening = false
                            onStateChange(false)
                            onRmsChanged(0f)

                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val spokenText = matches?.firstOrNull() ?: ""
                            Log.d(TAG, "Recognized Speech: $spokenText")
                            if (spokenText.isNotBlank()) {
                                onResult(spokenText)
                            }
                        } catch (t: Throwable) {
                            Log.e(TAG, "onResults callback error: ${t.message}")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        try {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull() ?: ""
                            if (partial.isNotBlank()) {
                                Log.d(TAG, "Partial recognition: $partial")
                            }
                        } catch (t: Throwable) {}
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to create SpeechRecognizer safely: ${t.message}")
            speechRecognizer = null
            false
        }
    }

    fun startListening() {
        val hasPermission = try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } catch (t: Throwable) {
            false
        }

        if (!hasPermission) {
            onErrorNotice("মাইক্রোফোন পারমিশন প্রয়োজন!")
            return
        }

        mainHandler.post {
            try {
                if (!ensureRecognizerReady()) {
                    onErrorNotice("মাইক্রোফোন চালু করা যায়নি!")
                    isListening = false
                    onStateChange(false)
                    return@post
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    // Patient listening: allows Rabbi Sir to speak long sentences and pause without being interrupted
                    putExtra("android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 4000L)
                    putExtra("android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 3000L)
                    putExtra("android.speech.extras.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 3000L)

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
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                            // Correct Type: MUST be ArrayList<String> for RecognizerIntent Bundle
                            putExtra(
                                "android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES",
                                arrayListOf("en-US", "hi-IN", "bn-IN")
                            )
                        }
                    }
                }

                speechRecognizer?.startListening(intent)
                isListening = true
                onStateChange(true)
            } catch (t: Throwable) {
                Log.e(TAG, "startListening safe catch: ${t.message}", t)
                isListening = false
                onStateChange(false)
                onErrorNotice("মাইক্রোফোন প্রস্তুত করা যায়নি, আবার চেষ্টা করুন!")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (t: Throwable) {
                Log.e(TAG, "stopListening safe catch: ${t.message}")
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
            } catch (t: Throwable) {
                Log.e(TAG, "destroy safe catch: ${t.message}")
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
