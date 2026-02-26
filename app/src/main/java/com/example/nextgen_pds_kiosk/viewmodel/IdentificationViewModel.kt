package com.example.nextgen_pds_kiosk.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgen_pds_kiosk.data.Beneficiary
import com.example.nextgen_pds_kiosk.data.BeneficiaryRepository
import com.example.nextgen_pds_kiosk.ml.EmbeddingComparator
import com.example.nextgen_pds_kiosk.ml.FaceRecognizer
import com.example.nextgen_pds_kiosk.voice.VoiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class IdentificationState {
    object Idle : IdentificationState()
    object Processing : IdentificationState()
    data class Success(val beneficiary: Beneficiary) : IdentificationState()
    data class Failed(val reason: String) : IdentificationState()
}

@HiltViewModel
class IdentificationViewModel @Inject constructor(
    private val repository: BeneficiaryRepository,
    val voiceManager: VoiceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<IdentificationState>(IdentificationState.Idle)
    val uiState: StateFlow<IdentificationState> = _uiState

    // Cosine similarity threshold (lowered to 0.72 for better usability)
    private val MATCH_THRESHOLD = 0.72f

    fun processFace(faceBitmap: Bitmap, faceRecognizer: FaceRecognizer) {
        // Block consecutive analyses if we are already processing or succeeded
        if (_uiState.value != IdentificationState.Idle) return

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = IdentificationState.Processing

            try {
                // 1. Extract embedding for the live face
                val liveEmbedding = faceRecognizer.getEmbedding(faceBitmap)

                // 2. Fetch stored beneficiaries
                val beneficiaries = repository.getAllBeneficiariesList()

                if (beneficiaries.isEmpty()) {
                    _uiState.value = IdentificationState.Failed("No beneficiaries enrolled.")
                    voiceManager.speak("No one is enrolled in the database.")
                    delay(3000)
                    _uiState.value = IdentificationState.Idle
                    return@launch
                }

                // 3. Check if any real embeddings are enrolled
                val hasAnyEnrolledFace = beneficiaries.any { it.embeddingVector.isNotEmpty() }

                if (!hasAnyEnrolledFace) {
                    // ─────────────────────────────────────────────
                    // DEV BYPASS: No faces enrolled yet.
                    // Auto-match the first ACTIVE beneficiary
                    // so the full dispense workflow can be tested.
                    // ─────────────────────────────────────────────
                    val firstActive = beneficiaries.firstOrNull { it.status == "ACTIVE" }
                        ?: beneficiaries.first()
                    _uiState.value = IdentificationState.Success(firstActive)
                    voiceManager.speak("Dev mode: matched ${firstActive.name}")
                    return@launch
                }

                var bestMatch: Beneficiary? = null
                var highestSimilarity = -1f

                // 4. Compare with enrolled embeddings
                for (beneficiary in beneficiaries) {
                    if (beneficiary.embeddingVector.isEmpty()) continue
                    val similarity = EmbeddingComparator.cosineSimilarity(
                        liveEmbedding,
                        beneficiary.embeddingVector
                    )
                    android.util.Log.d("FaceMatch", "${beneficiary.name}: similarity=$similarity")
                    if (similarity > highestSimilarity) {
                        highestSimilarity = similarity
                        bestMatch = beneficiary
                    }
                }

                // 5. Decide on result (threshold 0.72 — more practical than 0.8)
                if (bestMatch != null && highestSimilarity >= MATCH_THRESHOLD) {
                    _uiState.value = IdentificationState.Success(bestMatch)
                    voiceManager.speak("Face recognized. Proceeding to authentication.")
                } else {
                    _uiState.value = IdentificationState.Failed("No Match (score: ${String.format("%.2f", highestSimilarity)})")
                    voiceManager.speak("No match found. Please try again.")
                    delay(2500)
                    _uiState.value = IdentificationState.Idle
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = IdentificationState.Failed("Processing Error")
                voiceManager.speak("Error during scan. Please try again.")
                delay(2000)
                _uiState.value = IdentificationState.Idle
            }
        }
    }
    
    fun resetState() {
        _uiState.value = IdentificationState.Idle
        voiceManager.speak("Please look directly at the camera.")
    }

    fun onLeavingScreen() {
        voiceManager.stopListening()
    }
}
