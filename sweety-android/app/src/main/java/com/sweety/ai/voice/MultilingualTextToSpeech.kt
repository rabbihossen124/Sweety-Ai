package com.sweety.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class MultilingualTextToSpeech(
    context: Context,
    private val onInitComplete: (Boolean) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var onSpeechDoneCallback: (() -> Unit)? = null

    private val banglaLocale = Locale("bn", "BD")
    private val hindiLocale = Locale("hi", "IN")
    private val englishLocale = Locale.US

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.setSpeechRate(1.02f) // Cheerful, conversational rate
                tts?.setPitch(1.32f)      // Sweet, cute female voice pitch
                setupUtteranceListener()
                Log.d(TAG, "TTS initialized with Cute Female Voice profile")
                onInitComplete(true)
            } else {
                Log.e(TAG, "Failed to initialize TTS")
                onInitComplete(false)
            }
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "Speech started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "Speech completed: $utteranceId")
                onSpeechDoneCallback?.invoke()
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Speech error on: $utteranceId")
                onSpeechDoneCallback?.invoke()
            }
        })
    }

    /**
     * Speaks the provided text and auto-switches language between Bangla, Hindi, and English
     */
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isInitialized || text.isBlank()) {
            onDone?.invoke()
            return
        }

        this.onSpeechDoneCallback = onDone

        // Auto-detect language based on Unicode character blocks
        val localeToUse = when {
            containsBangla(text) -> banglaLocale
            containsHindi(text) -> hindiLocale
            else -> englishLocale
        }

        val result = tts?.setLanguage(localeToUse)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Locale $localeToUse missing or not supported, falling back to default")
            tts?.language = Locale.getDefault()
        }

        // Apply sweet, cute female voice characteristics
        tts?.setPitch(1.32f)
        tts?.setSpeechRate(1.02f)
        applyFemaleVoiceIfAvailable(localeToUse)

        val utteranceId = "sweety_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun applyFemaleVoiceIfAvailable(locale: Locale) {
        try {
            val voices = tts?.voices ?: return
            val femaleVoice = voices.firstOrNull { voice ->
                voice.locale.language == locale.language &&
                (voice.name.lowercase().contains("female") ||
                 voice.name.lowercase().contains("woman") ||
                 voice.name.lowercase().contains("f0") ||
                 voice.name.lowercase().contains("-f-") ||
                 voice.features?.any { it.contains("female", ignoreCase = true) } == true)
            }
            if (femaleVoice != null) {
                tts?.voice = femaleVoice
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set custom female voice: ${e.message}")
        }
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun containsBangla(text: String): Boolean {
        for (char in text) {
            val code = char.code
            if (code in 0x0980..0x09FF) return true
        }
        return false
    }

    private fun containsHindi(text: String): Boolean {
        for (char in text) {
            val code = char.code
            if (code in 0x0900..0x097F) return true
        }
        return false
    }

    companion object {
        private const val TAG = "SweetyTTS"
    }
}

