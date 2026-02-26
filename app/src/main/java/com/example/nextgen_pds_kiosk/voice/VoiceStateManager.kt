package com.example.nextgen_pds_kiosk.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class VoiceState {
    IDLE,       // Low-power continuous listening (for wake word only)
    ACTIVE,     // Triggered after wake word (full speech recognition enabled)
    DISABLED    // Triggered by shutdown word (microphone processing stops)
}

@Singleton
class VoiceStateManager @Inject constructor() {
    private val _currentState = MutableStateFlow(VoiceState.IDLE)
    val currentState: StateFlow<VoiceState> = _currentState

    fun transitionToActive() {
        if (_currentState.value == VoiceState.IDLE) {
            _currentState.value = VoiceState.ACTIVE
        }
    }

    fun transitionToIdle() {
        if (_currentState.value == VoiceState.ACTIVE || _currentState.value == VoiceState.DISABLED) {
            _currentState.value = VoiceState.IDLE
        }
    }

    fun transitionToDisabled() {
        if (_currentState.value == VoiceState.ACTIVE || _currentState.value == VoiceState.IDLE) {
            _currentState.value = VoiceState.DISABLED
        }
    }
}
