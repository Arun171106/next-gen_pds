package com.example.nextgen_pds_kiosk.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgen_pds_kiosk.data.Beneficiary
import com.example.nextgen_pds_kiosk.data.BeneficiaryRepository
import com.example.nextgen_pds_kiosk.ml.EmbeddingComparator
import com.example.nextgen_pds_kiosk.ml.FaceRecognizer
import com.example.nextgen_pds_kiosk.voice.VoiceManager
import com.google.mlkit.vision.face.Face
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
    
    // Liveness Detection States
    object LivenessCheckBlink : FaceVerifyState()
    
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
    
    // Track Liveness Progress
    private var hasBlinked = false
    private var isMatching = false // Prevents multiple rapid matches once liveness passes
    
    // Anti-spoofing trackers
    private var initialLeftEyeProb: Float? = null
    private var initialRightEyeProb: Float? = null
    private var lastStaticFrames = 0

    // Track Enrollment vs Verification mode
    private var isEnrolling = false

    fun loadTarget(memberId: String) {
        viewModelScope.launch {
            targetBeneficiary = repository.getBeneficiaryById(memberId)
            val hasEmbedding = targetBeneficiary?.embeddingVector?.isNotEmpty() == true
            
            if (!hasEmbedding) {
                // If they don't have a face enrolled, we will do a live enrollment right now
                isEnrolling = true
                _state.value = FaceVerifyState.LivenessCheckBlink
                voiceManager.speak("First time verification. Please blink your eyes to register your face.")
            } else {
                isEnrolling = false
                // Start with Liveness Check for verification
                _state.value = FaceVerifyState.LivenessCheckBlink
                voiceManager.speak("Please look at the camera and blink your eyes to begin.")
            }
        }
    }

    fun processFace(faceBitmap: Bitmap, faceRecognizer: FaceRecognizer, face: Face) {
        if (targetBeneficiary == null || isMatching) return

        viewModelScope.launch(Dispatchers.Default) {
            
            // --- ANTI-SPOOFING CHECKS ---
            // 1. Check for flat surface (photo/printout). A real 3D face typically has some micro-movements.
            val headEulerY = Math.abs(face.headEulerAngleY) // Head turn left/right
            val headEulerZ = Math.abs(face.headEulerAngleZ) // Head tilt
            
            // 2. Check for static image (printout). If probabilities remain exactly identical across frames, it's likely a photo.
            val leftOpen = face.leftEyeOpenProbability
            val rightOpen = face.rightEyeOpenProbability
            
            if (leftOpen != null && rightOpen != null) {
                if (leftOpen == initialLeftEyeProb && rightOpen == initialRightEyeProb) {
                    lastStaticFrames++
                } else {
                    lastStaticFrames = 0
                    initialLeftEyeProb = leftOpen
                    initialRightEyeProb = rightOpen
                }
                
                if (lastStaticFrames > 15) {
                    _state.value = FaceVerifyState.Failed("Spoofing Detected: Static Image")
                    voiceManager.speak("Anti-spoofing triggered. Static image detected.")
                    delay(3000)
                    resetLiveness()
                    return@launch
                }
            }

            // --- LIVENESS CHECKS ---
            if (!hasBlinked) {
                // We enforce a required blink to prove liveness
                val lOpen = leftOpen ?: 1.0f
                val rOpen = rightOpen ?: 1.0f
                
                // Relaxed blink threshold for better UX in poor lighting, but spoofing is still caught by static frame check
                if (lOpen < 0.40f && rOpen < 0.40f) {
                    hasBlinked = true
                    _state.value = FaceVerifyState.Processing
                    voiceManager.speak("Verifying identity.")
                }
                return@launch
            }

            // 2. Similarity Matching or Registration Phase (only runs after liveness passes)
            if (hasBlinked && !isMatching) {
                isMatching = true
                _state.value = FaceVerifyState.Processing
                
                try {
                    val liveEmbedding = faceRecognizer.getEmbedding(faceBitmap)
                    
                    if (liveEmbedding == null) {
                        _state.value = FaceVerifyState.Failed("FaceNet failed to generate embedding. Move closer.")
                        delay(2000)
                        resetLiveness()
                        return@launch
                    }

                    val target = targetBeneficiary!!

                    if (isEnrolling) {
                        // Registration Logic
                        voiceManager.speak("Registering face to database.")
                        
                        // Ultra-low storage compression: Convert Bitmap to WEBP byte array
                        val stream = java.io.ByteArrayOutputStream()
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            faceBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 50, stream)
                        } else {
                            @Suppress("DEPRECATION")
                            faceBitmap.compress(Bitmap.CompressFormat.WEBP, 50, stream)
                        }
                        val photoBytes = stream.toByteArray()

                        // Update existing user with new embedding and photo
                        val updatedTarget = target.copy(
                            embeddingVector = liveEmbedding,
                            photoData = photoBytes
                        )
                        repository.updateBeneficiary(updatedTarget)
                        
                        voiceManager.speak("Face registered successfully. Welcome, ${target.name}.")
                        _state.value = FaceVerifyState.Success

                    } else {
                        // Verification Logic
                        val similarity = EmbeddingComparator.cosineSimilarity(liveEmbedding, target.embeddingVector)
                        android.util.Log.d("FaceVerify", "Similarity for ${target.name}: $similarity")

                        if (similarity >= MATCH_THRESHOLD) {
                            voiceManager.speak("Face verified. Welcome, ${target.name}.")
                            _state.value = FaceVerifyState.Success
                        } else {
                            voiceManager.speak("Face not matched. Please try again.")
                            _state.value = FaceVerifyState.Failed("No match (score: ${String.format("%.2f", similarity)})")
                            // Reset liveness and retry after short delay
                            delay(3000)
                            resetLiveness()
                        }
                    }
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = FaceVerifyState.Failed("Processing error")
                    delay(2000)
                    resetLiveness()
                }
            }
        }
    }
    
    private fun resetLiveness() {
        hasBlinked = false
        isMatching = false
        initialLeftEyeProb = null
        initialRightEyeProb = null
        lastStaticFrames = 0
        _state.value = FaceVerifyState.LivenessCheckBlink
        voiceManager.speak("Let's try again. Please blink your eyes.")
    }

    fun onLeavingScreen() = voiceManager.stopListening()
}
