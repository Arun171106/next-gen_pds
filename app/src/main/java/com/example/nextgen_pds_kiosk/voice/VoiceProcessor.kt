package com.example.nextgen_pds_kiosk.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceProcessor @Inject constructor(
    private val stateManager: VoiceStateManager
) {

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    
    // Vosk typically uses a 16000 Hz sample rate
    private val sampleRate = 16000 
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

    @SuppressLint("MissingPermission") // UI handles the RECORD_AUDIO permission flow
    fun startRecording(): Flow<ShortArray> = flow {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, // Changed from VOICE_RECOGNITION to standard MIC for higher device compatibility
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("VoiceProcessor", "AudioRecord initialization failed!")
                return@flow
            }

            // Attempt hardware Noise Suppression if available (wrapped in Try-Catch for bad HALs)
            try {
                val audioSessionId = audioRecord?.audioSessionId
                if (audioSessionId != null && audioSessionId != 0 && NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = NoiseSuppressor.create(audioSessionId)
                    noiseSuppressor?.enabled = true
                    Log.d("VoiceProcessor", "Hardware Noise Suppression Enabled")
                }
            } catch (e: Exception) {
                Log.w("VoiceProcessor", "Failed to init NoiseSuppressor: \${e.message}")
            }

            audioRecord?.startRecording()
            Log.d("VoiceProcessor", "Started Audio Recording")

            val buffer = ShortArray(bufferSize)

            while (currentCoroutineContext().isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                // Respect the DISABLED state: pause PCM stream processing
                if (stateManager.currentState.value == VoiceState.DISABLED) {
                    delay(200) // Sleep to save CPU cycles
                    continue
                }

                val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readResult > 0) {
                    emit(buffer.copyOfRange(0, readResult))
                } else if (readResult < 0) {
                    Log.e("VoiceProcessor", "AudioRecord read error code: $readResult. Breaking recording loop.")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceProcessor", "Recording error: \${e.message}")
        } finally {
            stopRecording()
        }
    }.flowOn(Dispatchers.IO)

    fun stopRecording() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            noiseSuppressor?.release()
            noiseSuppressor = null
            Log.d("VoiceProcessor", "Stopped Audio Recording")
        } catch (e: Exception) {
            Log.e("VoiceProcessor", "Error stopping recording: \${e.message}")
        }
    }
}
