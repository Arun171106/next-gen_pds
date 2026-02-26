package com.example.nextgen_pds_kiosk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.nextgen_pds_kiosk.kiosk.CrashRestartHandler
import com.example.nextgen_pds_kiosk.kiosk.KioskLockManager
import com.example.nextgen_pds_kiosk.navigation.KioskNavHost
import com.example.nextgen_pds_kiosk.ui.particles.AnimatedParticleBackground
import com.example.nextgen_pds_kiosk.ui.theme.NextGenPDS_KioskTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var kioskLockManager: KioskLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Install Watchdog Crash Handler
        CrashRestartHandler.install(this)

        // 2. Immersive Fullscreen (Hide Status Bar & Nav Bar)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        enableEdgeToEdge()
        
        setContent {
            NextGenPDS_KioskTheme(darkTheme = true, dynamicColor = false) {
                // We layer the NavHost ON TOP of the AnimatedParticleBackground
                // so the background seamlessly persists across route changes
                Box(modifier = Modifier.fillMaxSize()) {
                    // Base Layer: Particles
                    AnimatedParticleBackground()
                    
                    // Top Layer: Navigation Content
                    KioskNavHost()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 3. Pin to screen (Lock Task Mode) every time app resumes
        kioskLockManager.startLockTask(this)
    }

    // Attempt to block back presses at the Activity level just in case
    @Deprecated("Deprecated in Java", ReplaceWith("super.onBackPressed()"))
    override fun onBackPressed() {
        if (!kioskLockManager.isLocked.value) {
            super.onBackPressed()
        }
        // If locked, do nothing (block)
    }
}