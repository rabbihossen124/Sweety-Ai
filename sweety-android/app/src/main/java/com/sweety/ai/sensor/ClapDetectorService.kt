package com.sweety.ai.sensor

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sweety.ai.MainActivity
import com.sweety.ai.SweetyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class ClapDetectorService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private var firstClapTimestamp = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        startClapListening()
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, SweetyApp.CHANNEL_ID)
            .setContentTitle("Sweety AI Clap Listener")
            .setContentText("Sweety is listening for 2-claps to wake up.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startClapListening() {
        if (isRecording) return

        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize <= 0) {
            Log.e(TAG, "Invalid buffer size for audio record")
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            audioRecord?.startRecording()
            isRecording = true

            serviceScope.launch {
                val buffer = ShortArray(bufferSize / 2)
                while (isActive && isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        processAudioChunk(buffer, read)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing RECORD_AUDIO permission for ClapDetector: ${e.message}")
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ClapDetector: ${e.message}")
            stopSelf()
        }
    }

    private fun processAudioChunk(buffer: ShortArray, readSize: Int) {
        var sum = 0.0
        for (i in 0 until readSize) {
            sum += buffer[i] * buffer[i]
        }
        val rms = sqrt(sum / readSize)

        // Clap threshold check
        if (rms > CLAP_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (firstClapTimestamp == 0L) {
                // First clap detected!
                firstClapTimestamp = now
            } else {
                val diff = now - firstClapTimestamp
                if (diff in MIN_CLAP_INTERVAL_MS..MAX_CLAP_INTERVAL_MS) {
                    // Double clap detected successfully!
                    Log.d(TAG, "DOUBLE CLAP DETECTED! Waking up Sweety AI...")
                    firstClapTimestamp = 0L
                    triggerWakeUp()
                } else if (diff > MAX_CLAP_INTERVAL_MS) {
                    // Reset, this becomes the new first clap
                    firstClapTimestamp = now
                }
            }
        }
    }

    private fun triggerWakeUp() {
        // 1. Haptic vibration
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator?.vibrate(150)
        }

        // 2. Wake screen up
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = pm?.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SweetyAI:ClapWakeLock"
        )
        wakeLock?.acquire(3000)

        // 3. Open MainActivity with voice listening activated
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(EXTRA_CLAP_TRIGGERED, true)
        }
        startActivity(launchIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up AudioRecord: ${e.message}")
        }
        audioRecord = null
        serviceJob.cancel()
    }

    companion object {
        private const val TAG = "ClapDetectorService"
        private const val NOTIFICATION_ID = 2001
        const val EXTRA_CLAP_TRIGGERED = "extra_clap_triggered"

        private const val CLAP_THRESHOLD = 17500.0 // Sound intensity spike
        private const val MIN_CLAP_INTERVAL_MS = 200L // 0.2s minimum between claps
        private const val MAX_CLAP_INTERVAL_MS = 750L // 0.75s maximum window
    }
}

