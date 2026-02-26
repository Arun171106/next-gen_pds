package com.example.nextgen_pds_kiosk.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgen_pds_kiosk.data.Beneficiary
import com.example.nextgen_pds_kiosk.data.BeneficiaryRepository
import com.example.nextgen_pds_kiosk.ml.FaceRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class EnrollmentState {
    object Idle : EnrollmentState()
    object Processing : EnrollmentState()
    object Success : EnrollmentState()
    data class Error(val message: String) : EnrollmentState()
}

@HiltViewModel
class EnrollmentViewModel @Inject constructor(
    private val repository: BeneficiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EnrollmentState>(EnrollmentState.Idle)
    val uiState: StateFlow<EnrollmentState> = _uiState

    fun processAndEnrollFace(croppedFace: Bitmap, recognizer: FaceRecognizer, name: String, metadata: String) {
        if (_uiState.value is EnrollmentState.Processing) return
        
        _uiState.value = EnrollmentState.Processing
        
        viewModelScope.launch {
            try {
                // Generate the 128D embedding vector specifically using the user's uploaded name
                val embedding = recognizer.getEmbedding(croppedFace)
                
                if (embedding != null) {
                    
                    // Ultra-low storage compression: Convert Bitmap to WEBP byte array
                    val stream = java.io.ByteArrayOutputStream()
                    // Use WEBP or JPEG for extreme reduction in DB blob size (~5-15 KB)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        croppedFace.compress(Bitmap.CompressFormat.WEBP_LOSSY, 50, stream)
                    } else {
                        @Suppress("DEPRECATION")
                        croppedFace.compress(Bitmap.CompressFormat.WEBP, 50, stream)
                    }
                    val photoBytes = stream.toByteArray()
                    
                    val newBeneficiary = Beneficiary(
                        beneficiaryId = java.util.UUID.randomUUID().toString(),
                        name = name.ifBlank { "Admin Test User" },
                        metadata = metadata.ifBlank { "Registered via Admin Console" },
                        embeddingVector = embedding,
                        photoData = photoBytes
                    )
                    
                    repository.addBeneficiary(newBeneficiary)
                    _uiState.value = EnrollmentState.Success
                } else {
                    _uiState.value = EnrollmentState.Error("FaceNet failed to generate embedding. Move closer.")
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = EnrollmentState.Error("Fatal SQLite error during insertion.")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = EnrollmentState.Idle
    }
}
