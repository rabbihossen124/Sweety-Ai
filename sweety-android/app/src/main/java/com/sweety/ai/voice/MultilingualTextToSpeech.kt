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

    // Cute voice customization parameters
    var voicePitch: Float = 1.25f // Sweet, cute, youthful female pitch
    var voiceSpeed: Float = 1.04f // Lively, conversational pace

    private val banglaLocale = Locale("bn", "BD")
    private val banglaIndiaLocale = Locale("bn", "IN")
    private val hindiLocale = Locale("hi", "IN")
    private val englishLocale = Locale.US

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.setSpeechRate(voiceSpeed)
                tts?.setPitch(voicePitch)
                setupUtteranceListener()
                applyFemaleVoice(banglaLocale)
                Log.d(TAG, "TTS initialized with Cute Female Voice profile")
                onInitComplete(true)
            } else {
                Log.e(TAG, "Failed to initialize TTS with code: $status")
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
     * Speaks the provided text in a sweet, cute female tone
     */
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isInitialized || text.isBlank()) {
            onDone?.invoke()
            return
        }

        this.onSpeechDoneCallback = onDone

        // 1. Determine target language
        val targetLocale = when {
            containsBangla(text) -> banglaLocale
            containsHindi(text) -> hindiLocale
            else -> englishLocale
        }

        val result = tts?.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback for Bengali
            if (targetLocale == banglaLocale) {
                tts?.setLanguage(banglaIndiaLocale)
            } else {
                tts?.language = Locale.getDefault()
            }
        }

        // 2. Apply sweet cute female acoustic parameters
        tts?.setPitch(voicePitch)
        tts?.setSpeechRate(voiceSpeed)

        // 3. Find and activate high-quality female voice
        applyFemaleVoice(targetLocale)

        val utteranceId = "sweety_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Finds and applies the sweetest natural female voice in Google TTS
     */
    private fun applyFemaleVoice(targetLocale: Locale) {
        try {
            val voices = tts?.voices ?: return
            val targetLang = targetLocale.language.lowercase()

            val bestFemale = voices.firstOrNull { voice ->
                val vLang = voice.locale?.language?.lowercase() ?: ""
                if (vLang != targetLang) return@firstOrNull false

                val name = voice.name.lowercase()
                val isMale = name.contains("male") || name.contains("-bic-") || name.contains("man") || name.contains("guy")
                if (isMale) return@firstOrNull false

                val isFemale = name.contains("female") ||
                        name.contains("woman") ||
                        name.contains("-fem-") ||
                        name.contains("-ban-") || // Google Bengali Female BD
                        name.contains("-bin-") || // Google Bengali Female IN 1
                        name.contains("-bid-") || // Google Bengali Female IN 2
                        name.contains("-sfg-") || // Google US Female
                        name.contains("-iom-") || // Google US Female
                        name.contains("-hie-") || // Google Hindi Female
                        name.contains("-hia-")

                isFemale
            }

            if (bestFemale != null) {
                tts?.voice = bestFemale
                Log.d(TAG, "Selected Sweet Female Voice: ${bestFemale.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Voice selection notice: ${e.message}")
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
