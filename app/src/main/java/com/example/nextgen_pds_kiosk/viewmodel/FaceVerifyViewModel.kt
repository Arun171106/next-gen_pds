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

sealed class FaceVerifyState {
    object Idle : FaceVerifyState()
    object Processing : FaceVerifyState()
    object AutoApproving : FaceVerifyState()   // Dev bypass countdown
    object Success : FaceVerifyState()
    data class Failed(val reason: String) : FaceVerifyState()
}

@HiltViewModel
class FaceVerifyViewModel @Inject constructor(
    private val repository: BeneficiaryRepository,
    val voiceManager: VoiceManager
) : ViewModel() {

    private val _state = MutableStateFlow<FaceVerifyState>(FaceVerifyState.Idle)
    val state: StateFlow<FaceVerifyState> = _state

    private val MATCH_THRESHOLD = 0.72f
    private var targetBeneficiary: Beneficiary? = null

    fun loadTarget(memberId: String) {
        viewModelScope.launch {
            targetBeneficiary = repository.getBeneficiaryById(memberId)
            val hasEmbedding = targetBeneficiary?.embeddingVector?.isNotEmpty() == true
            if (!hasEmbedding) {
                // Dev bypass — no face enrolled, auto-approve after countdown
                _state.value = FaceVerifyState.AutoApproving
                voiceManager.speak("No face enrolled. Auto-approving in 3 seconds.")
                delay(3000)
                if (_state.value == FaceVerifyState.AutoApproving) {
                    _state.value = FaceVerifyState.Success
                }
            }
        }
    }

    fun processFace(faceBitmap: Bitmap, faceRecognizer: FaceRecognizer) {
        if (_state.value != FaceVerifyState.Idle) return
        val target = targetBeneficiary ?: return

        viewModelScope.launch(Dispatchers.Default) {
            _state.value = FaceVerifyState.Processing
            try {
                val liveEmbedding = faceRecognizer.getEmbedding(faceBitmap)
                val similarity = EmbeddingComparator.cosineSimilarity(liveEmbedding, target.embeddingVector)
                android.util.Log.d("FaceVerify", "Similarity for ${target.name}: $similarity")

                if (similarity >= MATCH_THRESHOLD) {
                    voiceManager.speak("Face verified. Welcome, ${target.name}.")
                    _state.value = FaceVerifyState.Success
                } else {
                    voiceManager.speak("Face not matched. Please try again.")
                    _state.value = FaceVerifyState.Failed("No match (score: ${String.format("%.2f", similarity)})")
                    delay(2500)
                    _state.value = FaceVerifyState.Idle
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = FaceVerifyState.Failed("Processing error")
                delay(2000)
                _state.value = FaceVerifyState.Idle
            }
        }
    }

    fun onLeavingScreen() = voiceManager.stopListening()
}
