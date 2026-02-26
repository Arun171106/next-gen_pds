package com.example.nextgen_pds_kiosk.voice

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VoiceSetupHelper — Phase W7
 *
 * Manages offline voice engine detection and setup guidance.
 */
@Singleton
class VoiceSetupHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "voice_setup_prefs"
        private const val KEY_OFFLINE_STT_CONFIRMED = "offline_stt_confirmed"
        private const val KEY_OFFLINE_TTS_CONFIRMED = "offline_tts_confirmed"
        const val ERROR_OFFLINE_PACK_MISSING = 12 // SODA language pack not installed
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Whether offline STT requires one-time setup by admin
    private val _needsOfflineSttSetup = MutableStateFlow(!prefs.getBoolean(KEY_OFFLINE_STT_CONFIRMED, false))
    val needsOfflineSttSetup: StateFlow<Boolean> = _needsOfflineSttSetup

    private val _needsOfflineTtsSetup = MutableStateFlow(!prefs.getBoolean(KEY_OFFLINE_TTS_CONFIRMED, false))
    val needsOfflineTtsSetup: StateFlow<Boolean> = _needsOfflineTtsSetup

    /**
     * Called by VoiceManager when SpeechRecognizer returns error 12.
     * Sets the setup flag and persists it so we don't spam the user.
     */
    fun onOfflineSttPackMissing() {
        _needsOfflineSttSetup.value = true
        prefs.edit().putBoolean(KEY_OFFLINE_STT_CONFIRMED, false).apply()
        Log.w("VoiceSetupHelper", "Offline STT language pack missing — admin setup required")
    }

    /** Call this after the offline STT pack is confirmed installed. */
    fun confirmOfflineSttReady() {
        _needsOfflineSttSetup.value = false
        prefs.edit().putBoolean(KEY_OFFLINE_STT_CONFIRMED, true).apply()
    }

    /** Call this after offline TTS is confirmed. */
    fun confirmOfflineTtsReady() {
        _needsOfflineTtsSetup.value = false
        prefs.edit().putBoolean(KEY_OFFLINE_TTS_CONFIRMED, true).apply()
    }

    /**
     * Verifies the device has offline TTS voices installed for the given Locale.
     * Returns true if at least one offline voice is available.
     */
    fun checkOfflineTts(tts: TextToSpeech, targetLocale: Locale): Boolean {
        return try {
            val voices: Set<Voice>? = tts.voices
            val hasOfflineVoice = voices?.any { voice ->
                !voice.isNetworkConnectionRequired &&
                        voice.locale.language == targetLocale.language
            } ?: false

            if (hasOfflineVoice) {
                Log.d("VoiceSetupHelper", "Offline TTS voice available for ${targetLocale.language} ✓")
                confirmOfflineTtsReady()
            } else {
                Log.w("VoiceSetupHelper", "No offline TTS voice found for ${targetLocale.language}")
                _needsOfflineTtsSetup.value = true
            }
            hasOfflineVoice
        } catch (e: Exception) {
            Log.e("VoiceSetupHelper", "TTS voice check failed: ${e.message}")
            false
        }
    }

    /**
     * Opens Android's TTS settings so the admin can download offline voice data.
     * This is a one-time action.
     */
    fun openTtsSettings() {
        try {
            val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("VoiceSetupHelper", "Cannot open TTS settings: ${e.message}")
            // Fallback: open generic TTS settings
            openGenericTtsSettings()
        }
    }

    /**
     * Opens Android's offline speech recognition settings so the admin can
     * download the English offline language pack. One-time setup.
     */
    fun openOfflineSttSettings() {
        try {
            // Opens Google app voice search offline language settings
            val intent = Intent("com.google.android.voicesearch.INSTALL_VOICE_SEARCH_OFFLINE_FILES")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w("VoiceSetupHelper", "Cannot open voice search settings, trying generic: ${e.message}")
            openGenericSpeechSettings()
        }
    }

    private fun openGenericTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("VoiceSetupHelper", "Cannot open generic TTS settings: ${e.message}")
        }
    }

    private fun openGenericSpeechSettings() {
        try {
            val intent = Intent("android.settings.VOICE_INPUT_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("VoiceSetupHelper", "Cannot open speech settings: ${e.message}")
        }
    }

    /** True if both offline STT and TTS are confirmed ready. */
    fun isFullyOffline(): Boolean =
        !_needsOfflineSttSetup.value && !_needsOfflineTtsSetup.value

    /** Check if the device even has the speech recognition service available. */
    fun isSpeechRecognitionAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(context)
}
