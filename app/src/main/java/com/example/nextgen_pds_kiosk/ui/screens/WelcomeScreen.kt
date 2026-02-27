package com.example.nextgen_pds_kiosk.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import com.example.nextgen_pds_kiosk.voice.AppIntent
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextgen_pds_kiosk.R
import com.example.nextgen_pds_kiosk.ui.components.VoiceDebugDialog
import com.example.nextgen_pds_kiosk.ui.theme.*
import com.example.nextgen_pds_kiosk.viewmodel.WelcomeViewModel

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    onNavigateNext: () -> Unit,
    onNavigateAdmin: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    val dbCount by viewModel.beneficiaryCount.collectAsState()
    val context = LocalContext.current

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasAudioPermission = isGranted }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    var hasGreeted by remember { mutableStateOf(false) }
    val isTtsReady by viewModel.voiceManager.isTtsReadyState.collectAsState()

    LaunchedEffect(hasAudioPermission, isTtsReady) {
        if (hasAudioPermission && isTtsReady && !hasGreeted) {
            hasGreeted = true
            viewModel.voiceManager.speak("Welcome to Smart PDS Kiosk. Please say Start or tap the screen to begin.")
        }
    }

    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val chatHistory by viewModel.voiceManager.chatHistory.collectAsState()
    val currentIntent by viewModel.voiceManager.currentIntent.collectAsState()
    val currentLocale by viewModel.voiceManager.currentLocale.collectAsState()
    var showChatDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentIntent) {
        when (currentIntent) {
            AppIntent.NAVIGATE_NEXT -> onNavigateNext()
            else -> {}
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.voiceManager.onLeavingScreen() }
    }

    if (showChatDialog) {
        VoiceDebugDialog(
            chatHistory = chatHistory,
            isListening = isListening,
            onDismissRequest = { showChatDialog = false }
        )
    }

    // Pulsing animation for mic ring
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )
    val micAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = if (isListening) 0.3f else 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_alpha"
    )

    // Solid Light Background (Google Aesthetic)
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Admin settings button (top-right) ──
        IconButton(
            onClick = onNavigateAdmin,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(28.dp)
                .size(48.dp)
                .background(SurfaceLight, CircleShape)
                .shadow(elevation = 2.dp, shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Admin Settings",
                tint = TextOnLightSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // ── Language Selector (top-left) ──
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(28.dp)
                .background(SurfaceLight, RoundedCornerShape(16.dp))
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val languages = listOf("en" to "EN", "hi" to "HI", "ta" to "TA")
            languages.forEach { (langCode, label) ->
                val isSelected = currentLocale.language == langCode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) GoogleBluePrimary.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable {
                            val newLocale = Locale(langCode)
                            viewModel.voiceManager.setLanguage(newLocale)
                            val msg = when (langCode) {
                                "hi" -> "Hindi shuru"
                                "ta" -> "Tamil arambam"
                                else -> "English selected"
                            }
                            viewModel.voiceManager.speak(msg)
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) GoogleBluePrimary else TextOnLightSecondary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // ── Main Content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Logo without distracting glows, just clean and sharp
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .combinedClickable(
                        onClick = onNavigateNext,
                        onLongClick = { showChatDialog = true }
                    ),
                tint = GoogleBluePrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Headline
            Text(
                text = "Smart PDS Kiosk",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold, // Cleaner font weight
                    color = TextOnLightPrimary,
                    letterSpacing = (-0.5).sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Next-Gen Public Distribution System",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = TextOnLightSecondary,
                    fontWeight = FontWeight.Normal
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            // ── START Button (Solid Google Pill) ──
            Button(
                onClick = onNavigateNext,
                modifier = Modifier
                    .height(64.dp)
                    .widthOutlined(300.dp), // Fallback size, filled below
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoogleBluePrimary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
                contentPadding = PaddingValues(horizontal = 48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "START",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Voice hint text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(SurfaceVariantLight, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = null,
                    tint = if (isListening) GoogleBluePrimary else TextOnLightSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isListening) "Listening… say \"Start\"" else "Tap mic below to activate voice",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isListening) GoogleBluePrimary else TextOnLightSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // ── Bottom Footer ──
        Text(
            text = "NextGen PDS v1.0  •  Offline Mode",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextOnLightSecondary.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp
            )
        )

        // ── DB Status pill (bottom-left) ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp)
                .background(SurfaceLight, CircleShape)
                .border(1.dp, SurfaceVariantLight, CircleShape)
                .shadow(1.dp, CircleShape)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(modifier = Modifier.size(10.dp).background(GoogleGreenSuccess, CircleShape))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "DB Active · $dbCount enrolled",
                color = TextOnLightSecondary,
                style = MaterialTheme.typography.labelLarge
            )
        }

        // ── Pulsing Mic FAB (bottom-right) ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(28.dp)
        ) {
            // Pulse ring based on light theme
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(micPulse)
                        .background(GoogleBluePrimary.copy(alpha = micAlpha), CircleShape)
                        .align(Alignment.Center)
                )
            }
            FloatingActionButton(
                onClick = { showChatDialog = true },
                containerColor = if (isListening) GoogleBluePrimary else SurfaceLight,
                contentColor = if (isListening) Color.White else GoogleBluePrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Voice Logs",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// Helper for fixed width buttons without breaking imports
fun Modifier.widthOutlined(width: androidx.compose.ui.unit.Dp) = this.requiredWidthIn(min = width)
