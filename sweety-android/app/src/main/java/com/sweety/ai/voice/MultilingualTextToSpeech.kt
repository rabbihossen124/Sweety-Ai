package com.sweety.ai.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
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
    private var isPlayerPreparing = false
    private val playerLock = Any()

    private var onSpeechDoneCallback: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

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
                mainHandler.post {
                    if (status == TextToSpeech.SUCCESS) {
                        isTtsInitialized = true
                        try {
                            tts?.setSpeechRate(voiceSpeed)
                            tts?.setPitch(voicePitch)
                            setupUtteranceListener()
                            applyFemaleVoice(banglaLocale)
                        } catch (e: Exception) {
                            Log.w(TAG, "Local TTS post-init config warning: ${e.message}")
                        }
                        Log.d(TAG, "Local TTS initialized successfully")
                        onInitComplete(true)
                    } else {
                        onInitComplete(false)
                    }
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
                dispatchSpeechDone()
            }
            override fun onError(utteranceId: String?) {
                dispatchSpeechDone()
            }
        })
    }

    private fun dispatchSpeechDone() {
        mainHandler.post {
            try {
                onSpeechDoneCallback?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking speech done callback: ${e.message}")
            }
        }
    }

    /**
     * Speaks the text using Google's Neural Cloud Voice (Chat Experience),
     * falling back to local TTS seamlessly if offline or on network error.
     */
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (text.isBlank()) {
            mainHandler.post { onDone?.invoke() }
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

            synchronized(playerLock) {
                val player = MediaPlayer()
                isPlayerPreparing = true
                mediaPlayer = player

                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                player.setDataSource(streamUrl)

                player.setOnPreparedListener { mp ->
                    synchronized(playerLock) {
                        isPlayerPreparing = false
                        if (mediaPlayer === mp) {
                            try {
                                mp.start()
                            } catch (e: Exception) {
                                Log.w(TAG, "MediaPlayer start failed, fallback to local TTS: ${e.message}")
                                speakLocalFallback(text, onDone)
                            }
                        }
                    }
                }

                player.setOnCompletionListener {
                    synchronized(playerLock) {
                        isPlayerPreparing = false
                    }
                    dispatchSpeechDone()
                }

                player.setOnErrorListener { mp, what, extra ->
                    Log.w(TAG, "Cloud TTS stream error ($what, $extra), switching to local TTS fallback")
                    synchronized(playerLock) {
                        isPlayerPreparing = false
                        try {
                            mp.reset()
                            mp.release()
                        } catch (e: Exception) {}
                        if (mediaPlayer === mp) {
                            mediaPlayer = null
                        }
                    }
                    speakLocalFallback(text, onDone)
                    true
                }

                player.prepareAsync()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud TTS stream setup error: ${e.message}")
            speakLocalFallback(text, onDone)
        }
    }

    private fun speakLocalFallback(text: String, onDone: (() -> Unit)?) {
        mainHandler.post {
            if (!isTtsInitialized || tts == null) {
                onDone?.invoke()
                return@post
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
            // Ignore safely
        }
    }

    fun stop() {
        synchronized(playerLock) {
            try {
                mediaPlayer?.let { mp ->
                    if (!isPlayerPreparing) {
                        try {
                            if (mp.isPlaying) {
                                mp.stop()
                            }
                        } catch (e: Exception) {}
                    }
                    try {
                        mp.reset()
                    } catch (e: Exception) {}
                    try {
                        mp.release()
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {}
            mediaPlayer = null
            isPlayerPreparing = false
        }

        try {
            if (isTtsInitialized) {
                tts?.stop()
            }
        } catch (e: Exception) {}
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {}
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
