# TECH STACK — OFFLINE MULTILINGUAL VOICE ASSISTANT

PROJECT: Smart FPS Kiosk Voice Control System

## Design Reference

Primary UI/UX Source:

https://www.figma.com/make/qQWX63RuvK5lAIGSQjwT1E/Futuristic-3D-Kiosk-App?t=uhDkDV7Nt9SOBH9S-1

All voice interactions must integrate with the UI screens, navigation flows, and interaction elements defined in this Figma design.


# 1. Core Philosophy

The voice assistant must:

* Operate fully offline
* Support multilingual regional users
* Handle accent variability
* Work in noisy environments
* Execute kiosk actions reliably
* Integrate with existing Android MVVM architecture

Primary languages:

* English
* Tamil

Expandable languages:

* Hindi
* Telugu
* Kannada
* Malayalam

---

# 2. Speech Recognition Engine (Offline)

Primary Engine:

Vosk Speech Recognition (Open Source)

Reasons:

* Fully offline
* Lightweight models available
* Supports multiple languages
* Good performance on low-end hardware
* Custom vocabulary support

Alternative (optional future):

Whisper.cpp (offline) — heavier but more accurate

---

# 3. Natural Language Processing (NLP/NLU)

Offline NLU Strategy:

Hybrid Intent Recognition:

1. Rule-based intent matching
2. Keyword extraction
3. Lightweight ML classification (optional)

Libraries:

Kotlin NLP processing (custom)
OpenNLP (optional)
TensorFlow Lite text classification (optional future)

---

# 4. Text-to-Speech (TTS)

Android Built-in TTS Engine

Offline voices available for:

* English
* Tamil

Future upgrade:

Coqui TTS (offline open source)

---

# 5. Audio Processing

Android AudioRecord API

Features:

* Noise suppression
* Voice activity detection
* Low latency streaming

Optional enhancement:

RNNoise (noise reduction library)

---

# 6. Voice Wake System

Two modes:

Mode 1:

Tap microphone button (MVP)

Mode 2 (Future):

Wake word detection using Porcupine (offline)

---

# 7. Architecture Integration

Modules:

voice/
SpeechRecognizer.kt
VoiceProcessor.kt
IntentParser.kt
VoiceController.kt
TTSManager.kt

viewmodel/
VoiceViewModel.kt

---

# 8. Navigation Integration

Voice commands trigger:

Navigation events
Button clicks
Workflow actions

Via existing ViewModel layer.

---

# 9. Supported Commands Examples

English:

* "Scan my card"
* "Show rice quota"
* "Next"
* "Confirm"
* "Start dispensing"

Tamil:

* "என் கார்டு ஸ்கேன் செய்"
* "அரிசி எவ்வளவு உள்ளது"
* "அடுத்து"
* "உறுதி செய்"

---

# 10. Accent Handling Strategy

Approaches:

* Multiple pronunciation variants
* Phonetic keyword matching
* Confidence threshold logic
* Intent clustering

---

# 11. Performance Requirements

Speech latency:

< 1 second response

Recognition accuracy:

> 85% expected in quiet environment

CPU usage:

Minimal impact on UI performance

---

# 12. Testing Tools

Audio playback simulation
Unit tests for intent parsing
Manual speech tests
Noise environment testing

---

# 13. Development Tools

Android Studio
Vosk Model Downloader
Audio recording tools

---

END OF TECH STACK
