package com.example.nextgen_pds_kiosk.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Switched to Light Theme for a clean, professional "Google App" look
private val KioskLightColorScheme = lightColorScheme(
    primary = GoogleBluePrimary,
    secondary = GoogleBlueSecondary,
    tertiary = GoogleGreenSuccess,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextOnLightPrimary,
    onSurface = TextOnLightPrimary,
    onSurfaceVariant = TextOnLightSecondary,
    error = GoogleRedError,
    onError = Color.White
)

@Composable
fun NextGenPDS_KioskTheme(
    // Force light mode for standard professional aesthetic
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = KioskLightColorScheme
    val view = LocalView.current
    
    // Set status bar colors
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            // Set text icons to be dark (true) because the background is now light
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}