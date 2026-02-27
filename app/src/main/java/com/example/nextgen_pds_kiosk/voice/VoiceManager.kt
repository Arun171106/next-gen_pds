package com.example.nextgen_pds_kiosk.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class VoiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentParser: IntentParser,
    private val voiceSetupHelper: VoiceSetupHelper
) : TextToSpeech.OnInitListener {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // Android TTS
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _isTtsReadyState = MutableStateFlow(false)
    val isTtsReadyState: StateFlow<Boolean> = _isTtsReadyState
    
    // Dynamic Language Support (Defaults to English)
    private val _currentLocale = MutableStateFlow(Locale.ENGLISH)
    val currentLocale: StateFlow<Locale> = _currentLocale

    // Vosk Offline Speech Recognizer
    private var model: Model? = null
    private var voskRecognizer: Recognizer? = null
    private var isRecognizerBusy = false
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Flag to track if the Model is loaded
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded
    
    // VoiceProcessor stream tracking
    private var audioRecordingJob: Job? = null

    // State Flows exposed to ViewModels/UI
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _assistantResponse = MutableStateFlow("")
    val assistantResponse: StateFlow<String> = _assistantResponse

    // The live command extracted by Phase W4 NLP
    private val _currentIntent = MutableStateFlow<AppIntent?>(null)
    val currentIntent: StateFlow<AppIntent?> = _currentIntent

    // Chat History for Debug Dialog
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory

    // Phase W6 — Shutdown Handling
    // If the user hasn't spoken in IDLE_TIMEOUT_MS ms, the mic shuts off to save battery.
    private val IDLE_TIMEOUT_MS = 60_000L
    private var idleTimeoutJob: Job? = null
    private val _isVoiceIdle = MutableStateFlow(false)
    val isVoiceIdle: StateFlow<Boolean> = _isVoiceIdle

    init {
        tts = TextToSpeech(context, this)
        // Load Vosk Model on background thread
        initVoskModel()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "VoiceManagerTTS") {
                        startListening()
                    }
                }
                override fun onError(utteranceId: String?) {
                    startListening()
                }
            })
            val result = tts?.setLanguage(_currentLocale.value)
            isTtsReady = !(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
            _isTtsReadyState.value = isTtsReady

            // Apply best available offline voice for TTS
            if (isTtsReady) {
                applyBestOfflineVoice()
                voiceSetupHelper.checkOfflineTts(tts!!, _currentLocale.value)
            }
            Log.d("VoiceManager", "TTS initialized. Ready=$isTtsReady")
        } else {
            Log.e("VoiceManager", "TTS initialization failed with status: $status")
            _isTtsReadyState.value = false
        }
    }

    private fun initVoskModel() {
        Log.d("VoiceManager", "Initializing Vosk Offline Model...")
        StorageService.unpack(context, "model-en-us", "model",
            { downloadedModel ->
                model = downloadedModel
                _isModelLoaded.value = true
                Log.d("VoiceManager", "Vosk Offline Model loaded successfully.")
            },
            { exception ->
                Log.e("VoiceManager", "Failed to unpack Vosk model: ${exception.message}")
                _isModelLoaded.value = false
            }
        )
    }

    fun startListening() {
        mainHandler.post {
            if (_isListening.value || isRecognizerBusy) return@post
            if (tts?.isSpeaking == true) return@post
            if (model == null) {
                Log.w("VoiceManager", "Vosk model not yet loaded, postponing listen...")
                scope.launch { delay(1000); startListening() }
                return@post
            }

            try {
                // Vosk expects 16kHz mono 16-bit PCM buffer by default
                voskRecognizer = Recognizer(model, 16000.0f)
                
                _isListening.value = true
                isRecognizerBusy = true
                _isVoiceIdle.value = false
                resetIdleTimer()
                Log.d("VoiceManager", "Vosk Recognizer initialized and listening...")

                // Start consuming audio from the VoiceProcessor
                audioRecordingJob?.cancel()
                audioRecordingJob = scope.launch(Dispatchers.IO) {
                    val voiceProcessor = VoiceProcessor(VoiceStateManager()) // Initialize standalone or pass gracefully
                    voiceProcessor.startRecording().collect { buffer ->
                        if (!_isListening.value) return@collect
                        
                        // voskRecognizer expects ShortArray buffer to be passed directly
                        val isFinal = voskRecognizer?.acceptWaveForm(buffer, buffer.size) ?: false
                        
                        if (isFinal) {
                            val resultJson = voskRecognizer?.result ?: ""
                            processVoskResult(resultJson)
                        } else {
                            // Can optionally extract partial results here to update UI
                            val partialJson = voskRecognizer?.partialResult ?: ""
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VoiceManager", "startListening failed: ${e.message}")
                _isListening.value = false
                isRecognizerBusy = false
            }
        }
    }
    
    private fun processVoskResult(json: String) {
        try {
            // Vosk result structure: {"text": "hello world"}
            val root = org.json.JSONObject(json)
            val text = root.optString("text", "")
            
            if (text.isNotBlank()) {
                Log.d("VoiceManager", "Vosk Recognized: '$text'")
                _recognizedText.value = text
                addChatMessage(ChatMessage(isUser = true, text = text))
                
                // Immediately stop listening so android TTS doesn't feed back into STT
                stopListening()
                
                handleRecognizedSpeech(text)
                resetIdleTimer()
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error parsing Vosk result: ${e.message}")
        }
    }

    fun stopListening() {
        mainHandler.post {
            _isListening.value = false
            isRecognizerBusy = false
            audioRecordingJob?.cancel()
            try {
                voskRecognizer?.close()
                voskRecognizer = null
            } catch (e: Exception) {
                Log.e("VoiceManager", "stopListening error: ${e.message}")
            }
            Log.d("VoiceManager", "Stopped Listening")
        }
    }

    private fun handleRecognizedSpeech(text: String) {
        val parsedIntent = intentParser.parseIntent(text, _currentLocale.value.language)

        if (parsedIntent != AppIntent.UNKNOWN) {
            Log.d("VoiceManager", "Parsed Intent: $parsedIntent for text: '$text'")
            
            // Intercept Language Switching Intents immediately (Global action)
            when (parsedIntent) {
                AppIntent.SWITCH_LANGUAGE_ENGLISH -> {
                    setLanguage(Locale("en"))
                    speak("English selected")
                    return // Do not pass to the UI
                }
                AppIntent.SWITCH_LANGUAGE_HINDI -> {
                    setLanguage(Locale("hi"))
                    speak("Hindi set kardi gayi hai") // "Hindi is set"
                    return // Do not pass to the UI
                }
                AppIntent.SWITCH_LANGUAGE_TAMIL -> {
                    setLanguage(Locale("ta"))
                    speak("Tamil language set") 
                    return // Do not pass to the UI
                }
                else -> {
                    // Normal UI Intents
                    _currentIntent.value = parsedIntent

                    scope.launch {
                        delay(1500)
                        _currentIntent.value = null
                    }
                }
            }
        } else {
            Log.d("VoiceManager", "No intent matched for: '$text'")
        }
    }

    fun speak(text: String) {
        stopListening()

        if (!isTtsReady) {
            Log.e("VoiceManager", "TTS not ready; forcing Mic ON.")
            scope.launch {
                delay(500)
                startListening()
            }
            return
        }

        _assistantResponse.value = text
        addChatMessage(ChatMessage(isUser = false, text = text))
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VoiceManagerTTS")

        // Failsafe: if TTS onDone never fires, restart mic after timeout
        scope.launch {
            delay(8000)
            if (!_isListening.value) {
                Log.w("VoiceManager", "TTS onDone timeout. Restarting mic.")
                startListening()
            }
        }
    }

    fun stopSpeaking() {
        if (tts?.isSpeaking == true) tts?.stop()
    }

    private fun addChatMessage(msg: ChatMessage) {
        val current = _chatHistory.value.toMutableList()
        current.add(msg)
        if (current.size > 50) current.removeAt(0)
        _chatHistory.value = current
    }

    fun onLeavingScreen() {
        stopListening()
        cancelIdleTimer()
    }

    // Phase W6 helpers
    private fun resetIdleTimer() {
        idleTimeoutJob?.cancel()
        idleTimeoutJob = scope.launch {
            delay(IDLE_TIMEOUT_MS)
            Log.d("VoiceManager", "Idle timeout reached. Shutting down mic.")
            stopListening()
            _isVoiceIdle.value = true
        }
    }

    private fun cancelIdleTimer() {
        idleTimeoutJob?.cancel()
        idleTimeoutJob = null
    }

    /** Call from the UI to reactivate the mic after an idle shutdown. */
    fun wakeFromIdle() {
        _isVoiceIdle.value = false
        startListening()
    }

    /** Selects the best available offline TTS voice for the current locale, falls back to default. */
    private fun applyBestOfflineVoice() {
        try {
            val voices = tts?.voices ?: return
            
            val targetLanguage = _currentLocale.value.language
            // Prioritize: offline > quality > target locale match
            val bestVoice = voices
                .filter { !it.isNetworkConnectionRequired && it.locale.language == targetLanguage }
                .minByOrNull { it.quality } // lower value = higher quality in Android Voice API
                
            if (bestVoice != null) {
                tts?.voice = bestVoice
                Log.d("VoiceManager", "Applied offline TTS voice: ${bestVoice.name} for Lang: $targetLanguage")
            } else {
                Log.w("VoiceManager", "No offline TTS voice found for $targetLanguage, using device default")
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "applyBestOfflineVoice error: ${e.message}")
        }
    }
    
    /** 
     * Dynamically switches the active STT and TTS language at runtime.
     * Restarts the microphone if actively listening to apply the new intent extras.
     */
    fun setLanguage(locale: Locale) {
        if (_currentLocale.value == locale) return
        
        Log.i("VoiceManager", "Switching Language to: ${locale.language}")
        _currentLocale.value = locale
        
        // Update TTS Engine
        if (isTtsReady) {
            val result = tts?.setLanguage(locale)
            isTtsReady = !(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
            _isTtsReadyState.value = isTtsReady
            if (isTtsReady) {
                applyBestOfflineVoice()
            }
        }
        
        // If the microphone is active, restart it so the new intent EXTRA_LANGUAGE is applied
        if (_isListening.value) {
            stopListening()
            scope.launch { 
                delay(300) 
                startListening() 
            }
        }
    }

    fun shutdown() {
        stopListening()
        stopSpeaking()
        cancelIdleTimer()
        tts?.shutdown()
        try {
            model?.close()
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error closing Vosk Model: ${e.message}")
        }
    }
}
