package com.example.nextgen_pds_kiosk.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextgen_pds_kiosk.camera.CameraAnalyzer
import com.example.nextgen_pds_kiosk.ml.FaceRecognizer
import com.example.nextgen_pds_kiosk.ui.components.CameraPreview
import com.example.nextgen_pds_kiosk.ui.components.KioskTopAppBar
import com.example.nextgen_pds_kiosk.ui.theme.*
import com.example.nextgen_pds_kiosk.viewmodel.FaceVerifyState
import com.example.nextgen_pds_kiosk.viewmodel.FaceVerifyViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FaceVerifyScreen(
    cardNo: String,
    memberId: String,
    onVerifySuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: FaceVerifyViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val faceRecognizer = remember { FaceRecognizer(context) }
    val cameraAnalyzer = remember(faceRecognizer) {
        CameraAnalyzer(scope) { bitmap: Bitmap, face ->
            viewModel.processFace(bitmap, faceRecognizer, face)
        }
    }

    LaunchedEffect(memberId) {
        viewModel.loadTarget(memberId)
    }

    // Camera permission request
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    // Navigate on success
    LaunchedEffect(state) {
        if (state is FaceVerifyState.Success) {
            onVerifySuccess()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            faceRecognizer.close()
            viewModel.onLeavingScreen()
        }
    }

    // Pulse animation for active liveness checks
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(top = 48.dp, bottom = 48.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            KioskTopAppBar(stepLabel = "STEP 3 OF 3", onNavigateBack = onNavigateBack)
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(GoogleBluePrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FaceRetouchingNatural,
                    contentDescription = null,
                    tint = GoogleBluePrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Identity Verification",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold, color = TextOnLightPrimary
                )
            )

            // Dynamic Liveness Instructions
            val (instructionText, instructionIcon, instructionColor) = when (state) {
                is FaceVerifyState.LivenessCheckBlink -> Triple("Please blink your eyes", Icons.Default.RemoveRedEye, GoogleYellowWarning)
                is FaceVerifyState.Processing     -> Triple("Analyzing Face...", Icons.Default.FaceRetouchingNatural, GoogleBluePrimary)
                is FaceVerifyState.Success        -> Triple("Identity Verified", Icons.Default.CheckCircle, GoogleGreenSuccess)
                is FaceVerifyState.Failed         -> Triple("Verification Failed", null, GoogleRedError)
                else                              -> Triple("Position your face in the circle", null, TextOnLightSecondary)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 16.dp, bottom = 40.dp)
                    .background(instructionColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                instructionIcon?.let {
                    Icon(it, contentDescription = null, tint = instructionColor, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = instructionText,
                    style = MaterialTheme.typography.titleMedium.copy(color = instructionColor, fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            }

            // Camera viewfinder with active state ring
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(320.dp)) {
                
                // Ring styling based on Liveness/Processing state
                val ringColor = when (state) {
                    is FaceVerifyState.Success -> GoogleGreenSuccess
                    is FaceVerifyState.Failed  -> GoogleRedError
                    is FaceVerifyState.LivenessCheckBlink -> GoogleYellowWarning
                    else -> GoogleBluePrimary
                }

                val showPulse = state is FaceVerifyState.LivenessCheckBlink || state is FaceVerifyState.Processing

                if (showPulse) {
                    Box(
                        modifier = Modifier
                            .size(310.dp)
                            .border(8.dp, ringColor.copy(alpha = pulseAlpha), RoundedCornerShape(155.dp))
                    )
                }

                Box(
                    modifier = Modifier.size(280.dp)
                        .shadow(8.dp, RoundedCornerShape(140.dp))
                        .clip(RoundedCornerShape(140.dp))
                        .background(SurfaceVariantLight)
                        .border(4.dp, ringColor, RoundedCornerShape(140.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (cameraPermission.status.isGranted) {
                        CameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            analyzer = cameraAnalyzer
                        )
                    } else {
                        Text("Camera Required", color = GoogleRedError,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center)
                    }
                }

                // Success overlay
                if (state is FaceVerifyState.Success) {
                    Box(
                        modifier = Modifier.size(280.dp).clip(RoundedCornerShape(140.dp))
                            .background(GoogleGreenSuccess.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = Color.White, modifier = Modifier.size(80.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (state is FaceVerifyState.Failed) {
                Text(
                    text = (state as FaceVerifyState.Failed).reason,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold, color = GoogleRedError
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { /* ViewModel auto-resets after failure */ },
                    colors = ButtonDefaults.buttonColors(containerColor = GoogleBluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text("Try Again", color = Color.White)
                }
            }
        }
    }
}
