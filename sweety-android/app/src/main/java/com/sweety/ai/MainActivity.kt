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

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requiredPermissions = listOf(
        Manifest.permission.RECORD_AUDIO
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable display and wake-up over lock screen
        setupLockScreenVisibility()

        // Check and request runtime microphone permission
        checkAndRequestPermissions()

        setContent {
            SweetyAITheme {
                MainScreen(viewModel = viewModel)
            }
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
