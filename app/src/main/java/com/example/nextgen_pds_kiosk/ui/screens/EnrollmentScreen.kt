package com.example.nextgen_pds_kiosk.ui.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextgen_pds_kiosk.camera.CameraAnalyzer
import com.example.nextgen_pds_kiosk.ml.FaceRecognizer
import com.example.nextgen_pds_kiosk.ui.components.CameraPreview
import com.example.nextgen_pds_kiosk.ui.components.KioskPrimaryButton
import com.example.nextgen_pds_kiosk.ui.components.KioskTopAppBar
import com.example.nextgen_pds_kiosk.viewmodel.EnrollmentState
import com.example.nextgen_pds_kiosk.viewmodel.EnrollmentViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EnrollmentScreen(
    onNavigateBack: () -> Unit,
    viewModel: EnrollmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var adminName by remember { mutableStateOf("") }
    var adminMetadata by remember { mutableStateOf("") }

    val faceRecognizer = remember { FaceRecognizer(context) }
    
    DisposableEffect(Unit) {
        onDispose {
            faceRecognizer.close()
            viewModel.resetState()
        }
    }

    // Camera Analyzer triggers insertion if the state is Idle
    val cameraAnalyzer = remember(faceRecognizer, adminName, adminMetadata) {
        CameraAnalyzer(scope) { bitmap, _ ->
            if (uiState is EnrollmentState.Idle) {
                 viewModel.processAndEnrollFace(bitmap, faceRecognizer, adminName, adminMetadata)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        KioskTopAppBar(
            stepLabel = "ADMINISTRATOR LOGIC: BIOMETRIC REGISTRATION",
            onNavigateBack = onNavigateBack
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxSize()) {
            // Left Column: Camera
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (cameraPermissionState.status.isGranted) {
                        CameraPreview(modifier = Modifier.fillMaxSize(), analyzer = cameraAnalyzer)
                        
                        // Targeting Reticle overlay
                        Box(
                            modifier = Modifier
                                .size(250.dp)
                                .border(4.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                        )
                    } else {
                        Text("Camera Access Required for Enrollment")
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Right Column: Input & Status
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "NEW DATABASE RECORD",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    OutlinedTextField(
                        value = adminName,
                        onValueChange = { adminName = it },
                        label = { Text("Simulated Beneficiary Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = adminMetadata,
                        onValueChange = { adminMetadata = it },
                        label = { Text("Simulated Metadata") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    val statusText = when (uiState) {
                        is EnrollmentState.Idle -> "LOOK AT CAMERA"
                        is EnrollmentState.Processing -> "EXTRACTING TFLITE 128D VECTOR..."
                        is EnrollmentState.Success -> "SQLITE REGISTRATION SUCCESSFUL"
                        is EnrollmentState.Error -> (uiState as EnrollmentState.Error).message
                    }
                    
                    val statusColor = when (uiState) {
                        is EnrollmentState.Success -> Color.Green
                        is EnrollmentState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                    
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColor,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Text("BACK TO KIOSK", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
