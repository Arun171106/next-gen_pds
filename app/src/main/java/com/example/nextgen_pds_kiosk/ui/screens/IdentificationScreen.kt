package com.example.nextgen_pds_kiosk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextgen_pds_kiosk.R
import com.example.nextgen_pds_kiosk.camera.CameraAnalyzer
import com.example.nextgen_pds_kiosk.ml.FaceRecognizer
import com.example.nextgen_pds_kiosk.ui.components.AnimatedLottieView
import com.example.nextgen_pds_kiosk.ui.components.CameraPreview
import com.example.nextgen_pds_kiosk.ui.components.KioskTopAppBar
import com.example.nextgen_pds_kiosk.ui.components.VoiceDebugDialog
import com.example.nextgen_pds_kiosk.voice.AppIntent
import com.example.nextgen_pds_kiosk.viewmodel.IdentificationState
import com.example.nextgen_pds_kiosk.viewmodel.IdentificationViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun IdentificationScreen(
    onNavigateNext: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: IdentificationViewModel = hiltViewModel()
) {
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    
    // Initialize FaceRecognizer (loads the TFLite model from assets)
    val faceRecognizer = remember { FaceRecognizer(context) }
    
    // Clean up TFLite memory and Voice Engine when leaving the screen
    DisposableEffect(Unit) {
        // Start listening the moment we enter the Scan Screen
        viewModel.voiceManager.startListening()
        
        onDispose {
            faceRecognizer.close()
            viewModel.resetState()
            viewModel.onLeavingScreen() // Stops voice overlap
        }
    }

    // Voice Chat Dialog State
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val chatHistory by viewModel.voiceManager.chatHistory.collectAsState()
    val currentIntent by viewModel.voiceManager.currentIntent.collectAsState()
    var showChatDialog by remember { mutableStateOf(false) }

    // Listen for Voice Intents
    LaunchedEffect(currentIntent) {
        if (currentIntent == AppIntent.NAVIGATE_BACK) {
            onNavigateBack()
        }
    }

    if (showChatDialog) {
        VoiceDebugDialog(
            chatHistory = chatHistory,
            isListening = isListening,
            onDismissRequest = { showChatDialog = false }
        )
    }

    // Prepare the CameraX Analyzer logic
    val cameraAnalyzer = remember(faceRecognizer) {
        CameraAnalyzer(scope) { bitmap, _ ->
            // Incoming cropped faces are passed to the ViewModel for Inference
            viewModel.processFace(bitmap, faceRecognizer)
        }
    }

    // Automatically navigate to Auth screen upon successful face match
    LaunchedEffect(uiState) {
        if (uiState is IdentificationState.Success) {
            val beneficiaryId = (uiState as IdentificationState.Success).beneficiary.beneficiaryId
            onNavigateNext(beneficiaryId)
            viewModel.resetState()
        }
    }

    // Request permission when screen loads if not already granted
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 48.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        KioskTopAppBar(
            stepLabel = "STEP 1 OF 3",
            onNavigateBack = onNavigateBack
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Identification Instruction Module
        Text(
            text = "Initiate Recognition",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Please look directly at the camera to identify your account and ration allocation.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Large Scanning Action Card - Replaced with Live Lottie Animation
        Card(
            modifier = Modifier
                .size(320.dp)
                .combinedClickable(
                    onClick = { onNavigateNext("DUMMY_ID") },
                    onLongClick = { showChatDialog = true } // Long press opens Voice Debug Dialog
                ),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Background Layer: Live Camera Feed with Analysis
                if (cameraPermissionState.status.isGranted) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        analyzer = cameraAnalyzer
                    )
                    
                    // Semi-transparent dark overlay to keep the typography readable
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                    )
                } else {
                    // Fallback if permission denied
                    Text(
                        text = "Camera Access Required",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Mid Layer: Scanning Radar Lottie Engine
                AnimatedLottieView(
                    rawResId = R.raw.dummy_radar_scan,
                    modifier = Modifier.fillMaxSize(0.6f)
                )

                // Foreground instruction overlay handling UI States
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(100.dp))
                    
                    val statusText = when (uiState) {
                        is IdentificationState.Idle -> "SCANNING"
                        is IdentificationState.Processing -> "VERIFYING BIOMETRICS..."
                        is IdentificationState.Success -> "MATCH FOUND"
                        is IdentificationState.Failed -> (uiState as IdentificationState.Failed).reason
                    }
                    
                    val statusColor = when (uiState) {
                        is IdentificationState.Success -> Color.Green
                        is IdentificationState.Failed -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.headlineLarge.copy(
                           fontWeight = FontWeight.Bold,
                           color = statusColor
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

        // Voice Debug Dialog Trigger FAB
        FloatingActionButton(
            onClick = { showChatDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp),
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Open Voice Debug Logs"
            )
        }
    }
}
