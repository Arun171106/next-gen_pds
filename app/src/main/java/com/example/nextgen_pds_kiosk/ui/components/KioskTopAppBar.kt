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
import androidx.compose.ui.draw.shadow
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
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Light themed circular back button with shadow
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(48.dp)
                .background(SurfaceLight, RoundedCornerShape(14.dp))
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go Back",
                tint = TextOnLightPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Clean light step label pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(SurfaceVariantLight)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = stepLabel,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = TextOnLightSecondary,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}
