package com.example.nextgen_pds_kiosk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextgen_pds_kiosk.ui.theme.*

@Composable
fun KioskTopAppBar(
    stepLabel: String,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bordered back button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceVariant)
                .border(1.dp, PrimaryAccent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go Back",
                tint = PrimaryAccent,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Step label pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(SurfaceVariant)
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        colors = listOf(PrimaryAccent.copy(alpha = 0.4f), SecondaryAccent.copy(alpha = 0.3f))
                    ),
                    RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = stepLabel,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = PrimaryAccent,
                    letterSpacing = 2.sp
                )
            )
        }
    }
}
