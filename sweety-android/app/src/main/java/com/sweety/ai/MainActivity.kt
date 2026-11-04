package com.sweety.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.sweety.ai.presentation.MainScreen
import com.sweety.ai.presentation.MainViewModel
import com.sweety.ai.presentation.theme.SweetyAITheme
import com.sweety.ai.sensor.ClapDetectorService

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requiredPermissions = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            startClapDetectorService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable display and wake-up over lock screen
        setupLockScreenVisibility()

        // Check and request runtime microphone and notification permissions
        checkAndRequestPermissions()

        // Start 2-clap background listener if permission is already granted
        startClapDetectorService()

        // Handle 2-clap launch trigger
        handleClapTrigger(intent)

        setContent {
            SweetyAITheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleClapTrigger(intent)
    }

    private fun handleClapTrigger(intent: Intent?) {
        val clapTriggered = intent?.getBooleanExtra(ClapDetectorService.EXTRA_CLAP_TRIGGERED, false) == true
        if (clapTriggered) {
            viewModel.onClapWakeUp()
        }
    }

    private fun startClapDetectorService() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                val serviceIntent = Intent(this, ClapDetectorService::class.java)
                ContextCompat.startForegroundService(this, serviceIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start ClapDetectorService: ${e.message}")
        }
    }

    private fun setupLockScreenVisibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}
