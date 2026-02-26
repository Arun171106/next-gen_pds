package com.example.nextgen_pds_kiosk.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextgen_pds_kiosk.data.Beneficiary
import com.example.nextgen_pds_kiosk.data.model.FamilyMember
import com.example.nextgen_pds_kiosk.ui.components.KioskPrimaryButton
import com.example.nextgen_pds_kiosk.ui.components.KioskTopAppBar
import com.example.nextgen_pds_kiosk.viewmodel.AuthState
import com.example.nextgen_pds_kiosk.viewmodel.AuthViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.graphics.BitmapFactory

@Composable
fun AuthenticationScreen(
    beneficiaryId: String,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(beneficiaryId) {
        viewModel.loadBeneficiary(beneficiaryId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp, bottom = 48.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KioskTopAppBar(
            stepLabel = "STEP 2 OF 3",
            onNavigateBack = onNavigateBack
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Success Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = com.example.nextgen_pds_kiosk.ui.theme.SuccessGreen,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Identity Verified",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = com.example.nextgen_pds_kiosk.ui.theme.SuccessGreen
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Content based on state
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is AuthState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading smart card details...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is AuthState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
                is AuthState.Success -> {
                    SmartCardView(beneficiary = state.beneficiary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        KioskPrimaryButton(
            text = "CONFIRM & PROCEED",
            enabled = uiState is AuthState.Success,
            onClick = onNavigateNext
        )
    }
}

@Composable
private fun SmartCardView(beneficiary: Beneficiary) {
    // Parse family members from JSON
    val gson = Gson()
    val familyMembers: List<FamilyMember> = remember(beneficiary.membersJson) {
        if (beneficiary.membersJson.isNotBlank()) {
            val type = object : TypeToken<List<FamilyMember>>() {}.type
            try { gson.fromJson(beneficiary.membersJson, type) } catch (e: Exception) { emptyList() }
        } else emptyList()
    }

    // Decode profile photo from ByteArray
    val bitmap = remember(beneficiary.photoData) {
        beneficiary.photoData?.let {
            try { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } catch (e: Exception) { null }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Photo + Name Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Photo
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = beneficiary.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Smart Card: ${beneficiary.smartCardNo.ifBlank { beneficiary.beneficiaryId }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Card Type Badge
                        val badgeColor = when (beneficiary.cardType) {
                            "AAY" -> Color(0xFFD32F2F)
                            "PHH" -> Color(0xFF1565C0)
                            else -> Color(0xFF2E7D32)
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = badgeColor
                        ) {
                            Text(
                                text = beneficiary.cardType.ifBlank { "PHH" },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    // Status chip
                    val statusColor = if (beneficiary.status == "ACTIVE") Color(0xFF2E7D32) else Color(0xFFD32F2F)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = beneficiary.status.ifBlank { "ACTIVE" },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                        )
                    }
                }
            }
        }

        // Contact & Address
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("Contact & Address")
                    if (beneficiary.mobile.isNotBlank()) DataRow("Mobile", beneficiary.mobile)
                    if (beneficiary.address.isNotBlank()) DataRow("Address", beneficiary.address)
                }
            }
        }

        // Monthly Entitlement
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("Monthly Entitlement")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EntitlementItem("Rice", "${beneficiary.riceKg} kg", Color(0xFFFF8F00))
                        EntitlementItem("Wheat", "${beneficiary.wheatKg} kg", Color(0xFFFFA000))
                        EntitlementItem("Sugar", "${beneficiary.sugarKg} kg", Color(0xFF7B1FA2))
                        EntitlementItem("Dal", "${beneficiary.dalKg} kg", Color(0xFF1976D2))
                        if (beneficiary.keroseneL > 0) {
                            EntitlementItem("Kerosene", "${beneficiary.keroseneL} L", Color(0xFFE64A19))
                        }
                    }
                }
            }
        }

        // Family Members
        if (familyMembers.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader("Family Members (${familyMembers.size})")
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            items(familyMembers) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.name.first().uppercaseChar().toString(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${member.relation} · Age ${member.age} · ${member.gender}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = member.aadhaarMasked,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    )
    Divider(modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
}

@Composable
private fun EntitlementItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.65f),
            textAlign = TextAlign.End
        )
    }
}
