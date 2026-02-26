package com.example.nextgen_pds_kiosk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgen_pds_kiosk.data.model.DefaultHardwareResponse
import com.example.nextgen_pds_kiosk.data.api.DispenserApiService
import com.example.nextgen_pds_kiosk.voice.VoiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

sealed class DispenserState {
    object Idle : DispenserState()
    object Taring : DispenserState()
    object Ready : DispenserState()
    data class Dispensing(val currentWeight: Float, val targetWeight: Float) : DispenserState()
    object Completed : DispenserState()
    data class Error(val message: String) : DispenserState()
}

@HiltViewModel
class DispenserViewModel @Inject constructor(
    private val apiService: DispenserApiService,
    val voiceManager: VoiceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<DispenserState>(DispenserState.Idle)
    val uiState: StateFlow<DispenserState> = _uiState

    private var targetWeightKg: Float = 0f

    fun tareScale() {
        viewModelScope.launch {
            _uiState.value = DispenserState.Taring
            voiceManager.speak("Taring the scale. Please wait.")
            try {
                val response = apiService.tareScale()
                if (response.isSuccessful && response.body()?.status == "success") {
                    _uiState.value = DispenserState.Ready
                    voiceManager.speak("Scale is ready. You can now place your container.")
                } else {
                    _uiState.value = DispenserState.Error("Tare failed. Check hardware connection.")
                    voiceManager.speak("Tare failed. Please check the hardware connection.")
                }
            } catch (e: IOException) {
                e.printStackTrace()
                _uiState.value = DispenserState.Error("Network error: Could not reach ESP32.")
                voiceManager.speak("Network error: Could not reach ESP32.")
            } catch (e: HttpException) {
                e.printStackTrace()
                _uiState.value = DispenserState.Error("HTTP Error: ${e.code()}")
                voiceManager.speak("Hardware returned an HTTP Error.")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = DispenserState.Error("Unexpected Tare Error.")
            }
        }
    }

    fun startDispensing(targetKg: Float) {
        if (_uiState.value is DispenserState.Dispensing) return
        
        _uiState.value = DispenserState.Dispensing(0f, targetKg)
        targetWeightKg = targetKg
        
        voiceManager.speak("Starting motor. Target weight is $targetKg kilograms. Please keep your hands clear of the dispensing area.")

        viewModelScope.launch {
            try {
                val response = apiService.startDispensing(targetKg)
                if (response.isSuccessful && response.body()?.status == "success") {
                    _uiState.value = DispenserState.Completed
                    voiceManager.speak("Dispensing completed. Please remove your container.")
                } else {
                    _uiState.value = DispenserState.Error("Dispensing rejected by hardware.")
                    voiceManager.speak("Dispensing failed. Please try again.")
                }
            } catch (e: IOException) {
                e.printStackTrace()
                _uiState.value = DispenserState.Error("Network error during dispense.")
            } catch (e: HttpException) {
                e.printStackTrace()
                _uiState.value = DispenserState.Error("HTTP Error: ${e.code()} during dispense.")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = DispenserState.Error("Unexpected Dispense Error.")
            }
        }
    }
    
    fun pauseDispensing() {
        viewModelScope.launch {
            try {
                apiService.pauseDispensing()
                voiceManager.speak("Dispensing paused.")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resumeDispensing() {
        viewModelScope.launch {
            try {
                apiService.resumeDispensing()
                voiceManager.speak("Resuming dispensing.")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun resetState() {
        _uiState.value = DispenserState.Idle
    }
    
    fun onLeavingScreen() {
        voiceManager.stopListening()
    }
}
