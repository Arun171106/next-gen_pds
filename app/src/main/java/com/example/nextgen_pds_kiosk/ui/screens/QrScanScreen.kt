package com.example.nextgen_pds_kiosk.ui.screens

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextgen_pds_kiosk.ui.components.KioskTopAppBar
import com.example.nextgen_pds_kiosk.ui.theme.*
import com.example.nextgen_pds_kiosk.viewmodel.QrScanState
import com.example.nextgen_pds_kiosk.viewmodel.QrScanViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScanScreen(
    onCardFound: (cardNo: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: QrScanViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var torchEnabled by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // Scanning corner animation
    val infiniteTransition = rememberInfiniteTransition(label = "scan_anim")
    val scanAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "corner_alpha"
    )

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    // Navigate when card found
    LaunchedEffect(state) {
        if (state is QrScanState.Found) {
            val cardNo = (state as QrScanState.Found).beneficiary.beneficiaryId
            onCardFound(cardNo)
            viewModel.resetScan()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 48.dp, start = 32.dp, end = 32.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            KioskTopAppBar(stepLabel = "STEP 1 OF 3", onNavigateBack = onNavigateBack)
            Spacer(modifier = Modifier.height(24.dp))

            // Professional light branding
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(GoogleBluePrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = GoogleBluePrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Scan Ration Card",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold, color = TextOnLightPrimary
                )
            )
            Text(
                text = "Hold your ration card QR code inside the frame",
                style = MaterialTheme.typography.bodyLarge.copy(color = TextOnLightSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
            )

            // Camera viewfinder with clean white card wrap
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceVariantLight),
                contentAlignment = Alignment.Center
            ) {
                if (cameraPermission.status.isGranted) {
                    val barcodeScanner = remember { BarcodeScanning.getClient() }
                    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val provider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { ia ->
                                        ia.setAnalyzer(analysisExecutor) { imageProxy ->
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null) {
                                                val img = InputImage.fromMediaImage(
                                                    mediaImage,
                                                    imageProxy.imageInfo.rotationDegrees
                                                )
                                                barcodeScanner.process(img)
                                                    .addOnSuccessListener { barcodes ->
                                                        barcodes.firstOrNull { it.valueType == Barcode.TYPE_TEXT || it.rawValue != null }
                                                            ?.rawValue
                                                            ?.let { raw -> viewModel.onQrDecoded(raw) }
                                                    }
                                                    .addOnCompleteListener { imageProxy.close() }
                                            } else {
                                                imageProxy.close()
                                            }
                                        }
                                    }
                                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                                try {
                                    provider.unbindAll()
                                    val cam = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                                    cameraControl = cam
                                } catch (e: Exception) { e.printStackTrace() }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Torch control
                    LaunchedEffect(torchEnabled) {
                        cameraControl?.cameraControl?.enableTorch(torchEnabled)
                    }

                    // Animated scanning corners overlay (Light Mode Colors)
                    Box(modifier = Modifier.fillMaxSize()) {
                        val cornerColor = when (state) {
                            is QrScanState.Found -> GoogleGreenSuccess
                            is QrScanState.NotFound -> GoogleRedError
                            else -> GoogleBluePrimary.copy(alpha = scanAlpha)
                        }
                        val cornerSize = 40.dp
                        val strokeWidth = 5.dp

                        // Top-left corner
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                            Box(modifier = Modifier.size(cornerSize).border(
                                width = strokeWidth, color = cornerColor,
                                shape = RoundedCornerShape(topStart = 12.dp)
                            ).clip(RoundedCornerShape(topStart = 12.dp)))
                        }
                        // Top-right corner
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                            Box(modifier = Modifier.size(cornerSize).border(
                                width = strokeWidth, color = cornerColor,
                                shape = RoundedCornerShape(topEnd = 12.dp)
                            ))
                        }
                        // Bottom-left corner
                        Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                            Box(modifier = Modifier.size(cornerSize).border(
                                width = strokeWidth, color = cornerColor,
                                shape = RoundedCornerShape(bottomStart = 12.dp)
                            ))
                        }
                        // Bottom-right corner
                        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                            Box(modifier = Modifier.size(cornerSize).border(
                                width = strokeWidth, color = cornerColor,
                                shape = RoundedCornerShape(bottomEnd = 12.dp)
                            ))
                        }
                    }
                } else {
                    Text("Camera permission required", color = GoogleRedError,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Status message
            val (statusText, statusColor) = when (state) {
                is QrScanState.Idle, QrScanState.Scanning ->
                    "Scanning…" to TextOnLightSecondary
                is QrScanState.Found ->
                    "✓ Card found! Loading…" to GoogleGreenSuccess
                is QrScanState.NotFound ->
                    "Card not registered: ${(state as QrScanState.NotFound).cardNo}" to GoogleRedError
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold, color = statusColor
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            if (state is QrScanState.NotFound) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.resetScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoogleBluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text("Try Again", color = Color.White)
                }
            }
        }

        // Torch FAB using solid aesthetic
        if (cameraPermission.status.isGranted) {
            FloatingActionButton(
                onClick = { torchEnabled = !torchEnabled },
                modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp),
                containerColor = if (torchEnabled) GoogleBluePrimary else SurfaceLight,
                contentColor = if (torchEnabled) Color.White else GoogleBluePrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Torch"
                )
            }
        }
    }
}
