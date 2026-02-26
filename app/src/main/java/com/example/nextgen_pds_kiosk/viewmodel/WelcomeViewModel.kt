package com.example.nextgen_pds_kiosk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgen_pds_kiosk.data.BeneficiaryRepository
import com.example.nextgen_pds_kiosk.voice.VoiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    repository: BeneficiaryRepository,
    val voiceManager: VoiceManager
) : ViewModel() {

    // Converts the Flow<List<Beneficiary>> into a Flow<Int> representing the count 
    // and exposes it as a StateFlow for Compose to observe easily.
    val beneficiaryCount: StateFlow<Int> = repository.getAllBeneficiariesFlow()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun playWelcomeVoice() {
        voiceManager.speak("Welcome to Smart PDS. Please touch anywhere to begin the dispensing process.")
        // Optionally auto-start listening after welcome:
        // Note: The precise moment to start listening will be improved in Phase W4
    }
    
    fun onLeavingScreen() {
       voiceManager.stopListening()
    }
}
