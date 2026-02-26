package com.example.nextgen_pds_kiosk.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import com.example.nextgen_pds_kiosk.viewmodel.DispenserViewModel
import kotlinx.coroutines.delay

@Composable
fun DispensingScreen(
    onNavigateNext: () -> Unit,
    viewModel: DispenserViewModel = hiltViewModel()
) {
    // Intercept hardware back button to prevent escaping the active dispense loop
    androidx.activity.compose.BackHandler(true) {
        // Do nothing (block back navigation)
    }

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
            .padding(top = 48.dp, bottom = 48.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        
        // Dispensing Header
        Text(
            text = "Dispensing...",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Please do not remove the bag until the process is fully completed.",
            style = MaterialTheme.typography.titleLarge.copy(
                color = com.example.nextgen_pds_kiosk.ui.theme.WarningYellow
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Large 3D Progress Ring simulating grains
        Box(
            modifier = Modifier.size(340.dp),
            contentAlignment = Alignment.Center
        ) {
            
            // Background track
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.DarkGray,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            // Foreground animated progress
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 100, easing = LinearEasing),
                label = "progress_animation"
            )
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = com.example.nextgen_pds_kiosk.ui.theme.PrimaryAccent,
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
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "of $targetWeightKg kg",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = "Wheat",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
        
        // Progress percentage readout 
        Text(
            text = String.format("%.0f%%", progress * 100),
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Pause / Resume Control
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
                    containerColor = if (isPaused) com.example.nextgen_pds_kiosk.ui.theme.SuccessGreen else com.example.nextgen_pds_kiosk.ui.theme.WarningYellow
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = if (isPaused) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (isPaused) "RESUME" else "PAUSE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isPaused) Color.White else Color.Black,
                            letterSpacing = 2.sp
                        )
                    )
                }
            }
        }
    }
}
