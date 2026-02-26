package com.example.nextgen_pds_kiosk.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MetricCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        Column {
            Text(title, color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = Color(0xFF4DB6AC), fontSize = 12.sp)
        }
    }
}

@Composable
fun SimpleBarChart(
    data: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF4DB6AC)
) {
    if (data.isEmpty() || data.size != labels.size) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No Chart Data Available", color = Color.Gray)
        }
        return
    }

    val maxDataValue = data.maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = (canvasWidth / (data.size * 2f)) // Space out bars

            // Draw Y-Axis guides
            val lines = 4
            for (i in 0..lines) {
                val y = canvasHeight - (i * (canvasHeight / lines))
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 2f
                )
            }

            // Draw Bars
            data.forEachIndexed { index, value ->
                val barHeight = (value / maxDataValue) * canvasHeight
                val startX = (index * barWidth * 2f) + (barWidth / 2f)

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(startX, canvasHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }
        }

        // Labels Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SimplePieChart(
    filledPercentage: Float,
    label: String,
    modifier: Modifier = Modifier,
    fillColor: Color = Color(0xFFE57373)
) {
    val percentage = filledPercentage.coerceIn(0f, 100f)
    val sweepAngle = (percentage / 100f) * 360f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 40f
            
            // Draw background track
            drawArc(
                color = Color.White.copy(alpha = 0.1f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(strokeWidth)
            )

            // Draw filled percentage
            drawArc(
                color = fillColor,
                startAngle = -90f, // Start at 12 o'clock
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${percentage.toInt()}%", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.Gray, fontSize = 12.sp)
        }
    }
}
