package com.example.nextgen_pds_kiosk.ui.theme

import androidx.compose.ui.graphics.Color

// Google Material 3 Professional Aesthetic Options
val GoogleBluePrimary = Color(0xFF1A73E8)
val GoogleBlueSecondary = Color(0xFF4285F4)
val GoogleGreenSuccess = Color(0xFF1E8E3E)
val GoogleRedError = Color(0xFFEA4335)
val GoogleYellowWarning = Color(0xFFFBBC04)

// Light Mode Surface Colors
val BackgroundLight = Color(0xFFF8F9FA) // Standard Off-White Panel String
val SurfaceLight = Color(0xFFFFFFFF) // Pure White Cards
val SurfaceVariantLight = Color(0xFFE8EAED) // Used for unselected chips/borders

// Text Colors
val TextOnLightPrimary = Color(0xFF202124) // Very dark gray, almost black
val TextOnLightSecondary = Color(0xFF5F6368) // Standard gray for subtitles

// Keep legacy references for compile compatibility temporarily while we migrate
val PrimaryAccent = GoogleBluePrimary
val SecondaryAccent = GoogleBlueSecondary
val TertiaryAccent = GoogleGreenSuccess
val DarkBackground = BackgroundLight
val SurfaceColor = SurfaceLight
val SurfaceVariant = SurfaceVariantLight
val TextPrimary = TextOnLightPrimary
val TextSecondary = TextOnLightSecondary
val SuccessGreen = GoogleGreenSuccess
val ErrorRed = GoogleRedError
val WarningYellow = GoogleYellowWarning
val ParticleColor = Color.Transparent // Disable particles by making them transparent