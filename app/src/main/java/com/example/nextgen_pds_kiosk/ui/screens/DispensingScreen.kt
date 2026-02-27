package com.example.nextgen_pds_kiosk.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextgen_pds_kiosk.ui.theme.*
import com.example.nextgen_pds_kiosk.viewmodel.DispenserViewModel
import kotlinx.coroutines.delay

@Composable
fun DispensingScreen(
    onNavigateNext: () -> Unit,
    viewModel: DispenserViewModel = hiltViewModel()
) {
    // Intercept hardware back button to prevent escaping the active dispense loop
    androidx.activity.compose.BackHandler(true) {}

    // Dummy progression state simulating ESP32 load cell feedback
    var currentWeightKg by remember { mutableFloatStateOf(0f) }
    val targetWeightKg = 5f // Hardcoded target for layout testing
    val progress = currentWeightKg / targetWeightKg
    
    // Hardware control state
    var isPaused by remember { mutableStateOf(false) }

    // Simulate dummy hardware dispensing over 5 seconds
    LaunchedEffect(isPaused) {
        if (!isPaused) {
            while (currentWeightKg < targetWeightKg && !isPaused) {
                delay(100)
                currentWeightKg += 0.1f
                if (currentWeightKg >= targetWeightKg) {
                    currentWeightKg = targetWeightKg
                    delay(1000) // Brief pause to show 100% completion before moving
                    onNavigateNext()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 48.dp, bottom = 48.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        
        // Dispensing Header
        Text(
            text = "Dispensing...",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextOnLightPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Please do not remove your bag until the process is fully completed.",
            style = MaterialTheme.typography.titleLarge.copy(
                color = TextOnLightSecondary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Large Progress Ring
        Box(
            modifier = Modifier.size(340.dp),
            contentAlignment = Alignment.Center
        ) {
            
            // Background track (Light gray)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = SurfaceVariantLight,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            // Foreground animated progress (Google Blue)
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 100, easing = LinearEasing),
                label = "progress_animation"
            )
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = GoogleBluePrimary,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Central readouts
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.1f", currentWeightKg),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextOnLightPrimary
                    )
                )
                Text(
                    text = "of $targetWeightKg kg",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = TextOnLightSecondary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Wheat",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = GoogleBluePrimary
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        // Progress percentage readout 
        Text(
            text = String.format("%.0f%% completed", progress * 100),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = GoogleGreenSuccess // Maps well to completion
            )
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        // Pause / Resume Control Pill
        if (currentWeightKg < targetWeightKg) {
            Button(
                onClick = { 
                    isPaused = !isPaused
                    if (isPaused) {
                        viewModel.pauseDispensing()
                    } else {
                        viewModel.resumeDispensing()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) GoogleBluePrimary else SurfaceVariantLight,
                    contentColor = if (isPaused) Color.White else TextOnLightPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(if (isPaused) 4.dp else 0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (isPaused) "RESUME" else "PAUSE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}
