package com.sweety.ai.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.net.URLEncoder
import java.util.Locale

class MultilingualTextToSpeech(
    private val context: Context,
    private val getBaseUrl: () -> String = { "https://sweety-ai-three.vercel.app" },
    private val onInitComplete: (Boolean) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var mediaPlayer: MediaPlayer? = null
    private var onSpeechDoneCallback: (() -> Unit)? = null

    // Sweet female pitch tuning for local fallback
    var voicePitch: Float = 1.25f
    var voiceSpeed: Float = 1.04f

    private val banglaLocale = Locale("bn", "BD")
    private val banglaIndiaLocale = Locale("bn", "IN")
    private val hindiLocale = Locale("hi", "IN")
    private val englishLocale = Locale.US

    init {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true
                    tts?.setSpeechRate(voiceSpeed)
                    tts?.setPitch(voicePitch)
                    setupUtteranceListener()
                    applyFemaleVoice(banglaLocale)
                    Log.d(TAG, "Local TTS initialized as fallback")
                    onInitComplete(true)
                } else {
                    onInitComplete(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing local TTS: ${e.message}")
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onSpeechDoneCallback?.invoke()
            }
            override fun onError(utteranceId: String?) {
                onSpeechDoneCallback?.invoke()
            }
        })
    }

    /**
     * Speaks the text using Google's Neural Cloud Voice (Chat Experience),
     * falling back to local TTS seamlessly if offline.
     */
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (text.isBlank()) {
            onDone?.invoke()
            return
        }

        stop()
        this.onSpeechDoneCallback = onDone

        // 1. Attempt Google Neural Cloud Voice streaming (Super realistic, natural human voice)
        try {
            val lang = when {
                containsBangla(text) -> "bn"
                containsHindi(text) -> "hi"
                else -> "en"
            }

            val cleanText = text.replace(Regex("[*#_`~]"), "").trim()
            val encodedText = URLEncoder.encode(cleanText, "UTF-8")
            val base = getBaseUrl().trimEnd('/')
            val streamUrl = "$base/api/assistant/tts?text=$encodedText&lang=$lang"

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .build()
                )
                setDataSource(streamUrl)
                setOnPreparedListener { mp ->
                    try {
                        mp.start()
                    } catch (e: Exception) {
                        speakLocalFallback(text, onDone)
                    }
                }
                setOnCompletionListener {
                    onSpeechDoneCallback?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "Cloud TTS stream error ($what, $extra), switching to local TTS fallback")
                    speakLocalFallback(text, onDone)
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud TTS stream setup error: ${e.message}")
            speakLocalFallback(text, onDone)
        }
    }

    private fun speakLocalFallback(text: String, onDone: (() -> Unit)?) {
        if (!isTtsInitialized || tts == null) {
            onDone?.invoke()
            return
        }

        val targetLocale = when {
            containsBangla(text) -> banglaLocale
            containsHindi(text) -> hindiLocale
            else -> englishLocale
        }

        try {
            val result = tts?.setLanguage(targetLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                if (targetLocale == banglaLocale) {
                    tts?.setLanguage(banglaIndiaLocale)
                } else {
                    tts?.language = Locale.getDefault()
                }
            }

            tts?.setPitch(voicePitch)
            tts?.setSpeechRate(voiceSpeed)
            applyFemaleVoice(targetLocale)

            val utteranceId = "sweety_${System.currentTimeMillis()}"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            Log.e(TAG, "Local TTS fallback failed: ${e.message}")
            onDone?.invoke()
        }
    }

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

                name.contains("female") ||
                        name.contains("woman") ||
                        name.contains("-fem-") ||
                        name.contains("-ban-") ||
                        name.contains("-bin-") ||
                        name.contains("-bid-") ||
                        name.contains("-sfg-") ||
                        name.contains("-iom-") ||
                        name.contains("-hie-") ||
                        name.contains("-hia-")
            }

            if (bestFemale != null) {
                tts?.voice = bestFemale
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun stop() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.reset()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {}

        try {
            if (isTtsInitialized) {
                tts?.stop()
            }
        } catch (e: Exception) {}
    }

    fun shutdown() {
        stop()
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
