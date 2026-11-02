package com.sweety.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
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
    var voicePitch: Float = 1.25f // 1.25f is the sweet, cute, youthful female companion pitch
    var voiceSpeed: Float = 1.04f // Lively, conversational pace

    private val banglaBDLocale = Locale("bn", "BD")
    private val banglaINLocale = Locale("bn", "IN")
    private val hindiLocale = Locale("hi", "IN")
    private val englishLocale = Locale.US

    init {
        // Initialize with Google TTS engine if available for highest quality natural human voices
        val preferredEngine = "com.google.android.tts"
        tts = TextToSpeech(context, { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.setSpeechRate(voiceSpeed)
                tts?.setPitch(voicePitch)
                setupUtteranceListener()
                pickBestFemaleVoiceForCurrentLocale()
                Log.d(TAG, "TTS initialized with Cute Female Voice profile (${tts?.voice?.name})")
                onInitComplete(true)
            } else {
                Log.e(TAG, "Failed to initialize TTS with code: $status")
                onInitComplete(false)
            }
        }, preferredEngine)
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

        // 1. Determine optimal locale
        val targetLocale = when {
            containsBangla(text) -> selectBestBanglaLocale()
            containsHindi(text) -> hindiLocale
            else -> englishLocale
        }

        val result = tts?.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Target locale $targetLocale not supported, attempting fallback")
            // If Bangla BD is not downloaded, try Bangla India
            if (targetLocale == banglaBDLocale) {
                tts?.setLanguage(banglaINLocale)
            } else {
                tts?.language = Locale.getDefault()
            }
        }

        // 2. Apply sweet cute female acoustic parameters
        tts?.setPitch(voicePitch)
        tts?.setSpeechRate(voiceSpeed)

        // 3. Find and activate high-quality female voice
        selectFemaleVoice(tts?.language ?: targetLocale)

        val utteranceId = "sweety_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun selectBestBanglaLocale(): Locale {
        // Test if Bangladesh locale data is available, else prefer India Bengali
        val bdCheck = tts?.isLanguageAvailable(banglaBDLocale)
        return if (bdCheck == TextToSpeech.LANG_AVAILABLE || bdCheck == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
            banglaBDLocale
        } else {
            banglaINLocale
        }
    }

    private fun pickBestFemaleVoiceForCurrentLocale() {
        val currentLoc = tts?.language ?: banglaBDLocale
        selectFemaleVoice(currentLoc)
    }

    /**
     * Finds and applies the sweetest natural female voice in Google TTS
     */
    private fun selectFemaleVoice(locale: Locale) {
        try {
            val voices = tts?.voices ?: return
            val lang = locale.language.lowercase()

            // Filter voices for this language
            val langVoices = voices.filter { it.locale.language.lowercase() == lang }
            if (langVoices.isEmpty()) return

            // Search priority:
            // 1. Explicit female keywords or Google's known Bengali female codes: ban, bin, bid
            // 2. Google US/English female codes: sfg, iom, tpd
            // 3. Any voice containing "female" / "woman"
            // 4. Must NOT contain "male", "bic", "man", "boy", "guy"
            val femaleVoice = langVoices.firstOrNull { voice ->
                val name = voice.name.lowercase()
                val isExplicitMale = name.contains("male") || name.contains("-bic-") || name.contains("man") || name.contains("guy")
                if (isExplicitMale) return@firstOrNull false

                val isCuteFemale = name.contains("female") ||
                        name.contains("woman") ||
                        name.contains("-fem-") ||
                        name.contains("-ban-") || // Google Bengali Female BD
                        name.contains("-bin-") || // Google Bengali Female IN 1
                        name.contains("-bid-") || // Google Bengali Female IN 2
                        name.contains("-sfg-") || // Google US Female
                        name.contains("-iom-") || // Google US Female
                        name.contains("-hie-") || // Google Hindi Female
                        name.contains("-hia-") || // Google Hindi Female
                        voice.features?.any { it.contains("female", ignoreCase = true) } == true

                isCuteFemale
            } ?: langVoices.firstOrNull { voice ->
                val name = voice.name.lowercase()
                !name.contains("male") && !name.contains("-bic-")
            }

            if (femaleVoice != null) {
                tts?.voice = femaleVoice
                Log.d(TAG, "Selected Sweet Female Voice: ${femaleVoice.name}")
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
