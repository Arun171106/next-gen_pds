package com.example.nextgen_pds_kiosk.ui.particles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.example.nextgen_pds_kiosk.ui.theme.ParticleColor
import kotlinx.coroutines.isActive
import kotlin.math.sin
import kotlin.random.Random

// Represents an individual particle in the simulated 3D space
class Particle(
    var x: Float,
    var y: Float,
    var radius: Float,
    var speedX: Float,
    var speedY: Float,
    var baseAlpha: Float,
    var phase: Float
)

@Composable
fun AnimatedParticleBackground(modifier: Modifier = Modifier) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    
    // Hold particles in a stable list so we don't reallocate inside drawing logic
    val particles = remember { mutableListOf<Particle>() }
    
    // Low CPU usage generation: generate once layout dimensions are known
    LaunchedEffect(size) {
        if (size.width > 0 && size.height > 0 && particles.isEmpty()) {
            repeat(80) {
                particles.add(
                    Particle(
                        x = Random.nextFloat() * size.width,
                        y = Random.nextFloat() * size.height,
                        radius = Random.nextFloat() * 3f + 1f, // Size variation for depth
                        speedX = (Random.nextFloat() - 0.5f) * 1.5f, // Horizontal drift
                        speedY = (Random.nextFloat() - 0.5f) * 1.5f - 0.5f, // Upward diagonal drift bias
                        baseAlpha = Random.nextFloat() * 0.4f + 0.1f, // Opacity variation
                        phase = Random.nextFloat() * 2 * Math.PI.toFloat() // Sinusoidal animation phase
                    )
                )
            }
        }
    }
    
    // Driver for the animation drawing loop
    var time by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(Unit) {
        val startTime = System.nanoTime()
        while(isActive) {
            withFrameNanos { frameTimeNanos ->
                // Seconds conversion. Triggers recomposition of any reader
                time = (frameTimeNanos - startTime) / 1_000_000_000f 
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            // Layer 1: Static futuristic background gradient from Design Doc
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .onSizeChanged { size = it }
    ) {
        if (size.width > 0 && size.height > 0) {
            // Layer 2: Animated particle canvas running at 60fps
            Canvas(modifier = Modifier.fillMaxSize()) {
                // By reading time here, compose knows to redraw this scope 60 times a second
                val currentTime = time 
                
                particles.forEach { p ->
                    // Position calculations (Delta approximation via simple constant additions)
                    p.x += p.speedX
                    p.y += p.speedY
                    
                    // Wrap-around bounds checking logic
                    if (p.x < -p.radius) p.x = size.width.toFloat() + p.radius
                    if (p.x > size.width.toFloat() + p.radius) p.x = -p.radius
                    if (p.y < -p.radius) p.y = size.height.toFloat() + p.radius
                    if (p.y > size.height.toFloat() + p.radius) p.y = -p.radius
                    
                    // Twinkle/Depth logic
                    val currentAlpha = (p.baseAlpha + sin(p.phase + currentTime * 2f) * 0.3f)
                        .toFloat().coerceIn(0f, 1f)
                        
                    drawCircle(
                        color = ParticleColor.copy(alpha = currentAlpha),
                        radius = p.radius,
                        center = Offset(p.x, p.y)
                    )
                }
            }
        }
    }
}
