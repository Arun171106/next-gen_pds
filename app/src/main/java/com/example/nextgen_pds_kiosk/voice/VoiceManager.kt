package com.example.nextgen_pds_kiosk.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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

    // Android SpeechRecognizer — MUST be created and used on the Main thread
    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecognizerBusy = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // Offline mode: true by default, flips to false if offline pack is missing (error 12)
    private var useOfflineMode = true

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
        // SpeechRecognizer MUST be created on the Main thread
        mainHandler.post { initSpeechRecognizer() }
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

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("VoiceManager", "Speech Recognition is NOT available on this device!")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                isRecognizerBusy = true
                _isVoiceIdle.value = false
                resetIdleTimer()
                Log.d("VoiceManager", "SpeechRecognizer: Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("VoiceManager", "SpeechRecognizer: Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("VoiceManager", "SpeechRecognizer: End of speech")
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing RECORD_AUDIO permission"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error — no internet"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    VoiceSetupHelper.ERROR_OFFLINE_PACK_MISSING -> "Offline language pack not installed"
                    else -> "Unknown error $error"
                }
                Log.w("VoiceManager", "SpeechRecognizer error: $errorMsg")

                isRecognizerBusy = false
                _isListening.value = false

                when (error) {
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    {
                        Log.d("VoiceManager", "Network error, falling back to System default routing.")
                        useOfflineMode = false
                        scope.launch { delay(1000); startListening() }
                    }
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // Offline pack not working; fall back to online
                        useOfflineMode = false
                        scope.launch { delay(1000); startListening() }
                    }
                    VoiceSetupHelper.ERROR_OFFLINE_PACK_MISSING -> {
                        // Offline pack not installed — notify admin
                        voiceSetupHelper.onOfflineSttPackMissing()
                        Log.w("VoiceManager", "Offline STT missing — admin needs to install offline language pack")
                        useOfflineMode = false
                        scope.launch { delay(1000); startListening() }
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        // Don't restart — needs user action
                        Log.e("VoiceManager", "RECORD_AUDIO permission missing — cannot restart")
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        // Destroy and recreate the recognizer to clear the busy state
                        mainHandler.post {
                            speechRecognizer?.destroy()
                            speechRecognizer = null
                            initSpeechRecognizer()
                        }
                        scope.launch { delay(1000); startListening() }
                    }
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        // Normal: user was silent or speech didn't match. Restart quickly.
                        scope.launch { delay(500); startListening() }
                    }
                    else -> {
                        // Auto-restart for all other transient errors, with enough delay to release hardware
                        scope.launch { delay(1000); startListening() }
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                isRecognizerBusy = false
                _isListening.value = false

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                Log.d("VoiceManager", "SpeechRecognizer result: '$text'")

                if (text.isNotBlank()) {
                    _recognizedText.value = text
                    addChatMessage(ChatMessage(isUser = true, text = text))
                    handleRecognizedSpeech(text)
                    resetIdleTimer() // Reset idle countdown when user actually speaks
                }

                // Auto-restart for continuous listening
                // Delay must be long enough for Android to release the audio focus
                scope.launch {
                    delay(800)
                    if (!_isListening.value && !_isVoiceIdle.value) {
                        startListening()
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull() ?: ""
                if (partial.isNotBlank()) {
                    _recognizedText.value = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        Log.d("VoiceManager", "Android SpeechRecognizer initialized successfully")
    }

    fun startListening() {
        mainHandler.post {
            if (_isListening.value || isRecognizerBusy) return@post
            if (tts?.isSpeaking == true) return@post

            if (speechRecognizer == null) {
                Log.e("VoiceManager", "SpeechRecognizer is null, reinitializing...")
                initSpeechRecognizer()
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, _currentLocale.value.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // Silence detection tuning — makes recognition end faster
                putExtra("android.speech.extra.DICTATION_MODE", false)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
                // Only request offline if a pack is actually known to be available
                if (useOfflineMode) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
            }

            try {
                speechRecognizer?.startListening(intent)
                Log.d("VoiceManager", "startListening() called on Main thread")
            } catch (e: Exception) {
                Log.e("VoiceManager", "startListening failed: ${e.message}")
                _isListening.value = false
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            _isListening.value = false
            isRecognizerBusy = false
            try {
                speechRecognizer?.stopListening()
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
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error destroying SpeechRecognizer: ${e.message}")
        }
    }
}
