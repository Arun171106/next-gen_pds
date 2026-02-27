package com.example.nextgen_pds_kiosk.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
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
import com.example.nextgen_pds_kiosk.ui.components.KioskTopAppBar
import com.example.nextgen_pds_kiosk.ui.theme.*
import com.example.nextgen_pds_kiosk.viewmodel.AuthState
import com.example.nextgen_pds_kiosk.viewmodel.AuthViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Composable
fun CardDetailsScreen(
    cardNo: String,
    onMemberSelected: (cardNo: String, memberId: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedMember by remember { mutableStateOf<FamilyMember?>(null) }

    LaunchedEffect(cardNo) {
        viewModel.loadBeneficiary(cardNo)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 48.dp, bottom = 48.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KioskTopAppBar(stepLabel = "STEP 2 OF 3", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(24.dp))

        when (val s = uiState) {
            is AuthState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GoogleBluePrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading smart card details…", color = TextOnLightSecondary)
                    }
                }
            }
            is AuthState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.message, color = GoogleRedError, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium)
                }
            }
            is AuthState.Success -> {
                val beneficiary = s.beneficiary
                val familyMembers: List<FamilyMember> = remember(beneficiary.membersJson) {
                    if (beneficiary.membersJson.isNotBlank()) {
                        val type = object : TypeToken<List<FamilyMember>>() {}.type
                        try { Gson().fromJson(beneficiary.membersJson, type) } catch (e: Exception) { emptyList() }
                    } else emptyList()
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(0.9f), // Constrain width for cleaner look on large screens
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header card
                    item { SmartCardHeader(beneficiary) }

                    // Monthly entitlements
                    item { EntitlementRow(beneficiary) }

                    // Member selection
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Select Member Collecting Today",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold, color = TextOnLightPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the family member who will verify their face",
                            style = MaterialTheme.typography.bodyLarge.copy(color = TextOnLightSecondary)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Member cards
                    if (familyMembers.isEmpty()) {
                        item {
                            MemberCard(
                                name = beneficiary.name,
                                relation = "Head",
                                age = 0,
                                gender = "",
                                isSelected = selectedMember == null,
                                photoData = beneficiary.photoData,
                                onClick = {
                                    selectedMember = FamilyMember(
                                        "M001", beneficiary.name, "Head", "", 0, ""
                                    )
                                }
                            )
                        }
                    } else {
                        items(familyMembers) { member ->
                            MemberCard(
                                name = member.name,
                                relation = member.relation,
                                age = member.age,
                                gender = member.gender,
                                isSelected = selectedMember?.memberId == member.memberId,
                                photoData = null,
                                onClick = { selectedMember = member }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        selectedMember?.let { member ->
                            onMemberSelected(cardNo, member.memberId)
                        }
                    },
                    enabled = selectedMember != null,
                    modifier = Modifier.height(64.dp).fillMaxWidth(0.6f),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoogleBluePrimary,
                        disabledContainerColor = SurfaceVariantLight,
                        contentColor = Color.White,
                        disabledContentColor = TextOnLightSecondary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                ) {
                    Text(
                        text = if (selectedMember != null)
                            "VERIFY ${selectedMember!!.name.uppercase()}'S FACE →"
                        else "SELECT A MEMBER TO CONTINUE",
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

@Composable
private fun SmartCardHeader(beneficiary: Beneficiary) {
    val bitmap = remember(beneficiary.photoData) {
        beneficiary.photoData?.let {
            try { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } catch (e: Exception) { null }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(SurfaceVariantLight),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(bitmap = bitmap, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Person, null,
                        modifier = Modifier.size(48.dp), tint = TextOnLightSecondary)
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(beneficiary.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = TextOnLightPrimary))
                Text(
                    text = beneficiary.smartCardNo.ifBlank { beneficiary.beneficiaryId },
                    style = MaterialTheme.typography.bodyLarge.copy(color = TextOnLightSecondary)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val badgeColor = when (beneficiary.cardType) {
                        "AAY" -> GoogleRedError
                        "PHH" -> GoogleBlueSecondary
                        else -> GoogleGreenSuccess
                    }
                    Surface(shape = RoundedCornerShape(50), color = badgeColor) {
                        Text(beneficiary.cardType.ifBlank { "PHH" },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }
                    val statusColor = if (beneficiary.status == "ACTIVE") GoogleGreenSuccess else GoogleRedError
                    Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.15f)) {
                        Text(beneficiary.status.ifBlank { "ACTIVE" },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = statusColor))
                    }
                }
            }
        }
        if (beneficiary.address.isNotBlank()) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = SurfaceVariantLight)
            Text(
                text = beneficiary.address,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium.copy(color = TextOnLightSecondary)
            )
        }
    }
}

@Composable
private fun EntitlementRow(beneficiary: Beneficiary) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Monthly Entitlement",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = GoogleBluePrimary))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                // Using solid Google Colors for chips
                if (beneficiary.riceKg > 0) EntitlementChip("Rice", "${beneficiary.riceKg}kg", Color(0xFFF29900))
                if (beneficiary.wheatKg > 0) EntitlementChip("Wheat", "${beneficiary.wheatKg}kg", GoogleYellowWarning)
                if (beneficiary.sugarKg > 0) EntitlementChip("Sugar", "${beneficiary.sugarKg}kg", Color(0xFFA142F4))
                if (beneficiary.dalKg > 0) EntitlementChip("Dal", "${beneficiary.dalKg}kg", GoogleBlueSecondary)
                if (beneficiary.keroseneL > 0) EntitlementChip("Kerosene", "${beneficiary.keroseneL}L", GoogleRedError)
            }
        }
    }
}

@Composable
private fun EntitlementChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f)) {
            Text(value,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = color))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(color = TextOnLightSecondary))
    }
}

@Composable
private fun MemberCard(
    name: String,
    relation: String,
    age: Int,
    gender: String,
    isSelected: Boolean,
    photoData: ByteArray?,
    onClick: () -> Unit
) {
    val bitmap = remember(photoData) {
        photoData?.let { try { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } catch (e: Exception) { null } }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) GoogleBluePrimary else SurfaceVariantLight,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) GoogleBluePrimary.copy(alpha = 0.05f) else SurfaceLight
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape)
                    .background(if (isSelected) GoogleBluePrimary.copy(alpha = 0.15f) else SurfaceVariantLight),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(bitmap = bitmap, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text(
                        text = name.first().uppercaseChar().toString(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) GoogleBluePrimary else TextOnLightSecondary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) GoogleBluePrimary else TextOnLightPrimary))
                Text(
                    text = buildString {
                        append(relation)
                        if (age > 0) append(" · Age $age")
                        if (gender.isNotBlank()) append(" · $gender")
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextOnLightSecondary)
                )
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selected",
                    tint = GoogleBluePrimary, modifier = Modifier.size(32.dp))
            }
        }
    }
}
