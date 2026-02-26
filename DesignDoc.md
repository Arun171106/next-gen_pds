# DESIGN DOCUMENT — VOICE ASSISTANT SYSTEM

PROJECT: Smart FPS Voice Assistant

## Source Design

Figma File:

https://www.figma.com/make/qQWX63RuvK5lAIGSQjwT1E/Futuristic-3D-Kiosk-App?t=uhDkDV7Nt9SOBH9S-1

The voice assistant must control and navigate the UI components defined in this design.


# 1. System Architecture

User Speech
↓
Audio Capture
↓
Speech Recognition (Vosk)
↓
Text Output
↓
Intent Parsing (NLP)
↓
Action Mapping
↓
UI / Hardware Execution
↓
Voice Feedback (TTS)

---

# 2. Modules

Audio Module:

Handles microphone recording.

Speech Module:

Converts audio to text.

NLP Module:

Extracts intent and entities.

Command Module:

Maps intent to app actions.

TTS Module:

Speaks responses.

---

# 3. Intent Recognition Strategy

Hybrid approach:

Rule-based + keyword matching.

Example:

"Show rice quota"

Keywords:

show + rice

Intent:

DISPLAY_QUOTA

---

# 4. Language Detection

Approaches:

Manual language toggle (MVP)
Auto detection (future)

---

# 5. Command Execution Mapping

Intent → ViewModel Event

Example:

NAVIGATE_NEXT → navigationController.navigate()

DISPENSE_START → hardware.start()

---

# 6. Development Loop

Step 1:

Audio capture working.

Step 2:

Speech recognition working.

Step 3:

Intent parsing working.

Step 4:

Command execution working.

Step 5:

Voice response working.

Step 6:

Integration testing.

---

# 7. Testing Strategy

Test languages:

English
Tamil

Test conditions:

Quiet room
Background noise
Different accents
Male / female voices

---

# 8. Failure Handling

If speech unclear:

Ask repeat.

If command unknown:

Show help suggestions.

If recognition fails:

Fallback touch interface.

---

# 9. Optimization

Use streaming recognition
Limit model size
Reduce CPU load

---

# 10. Completion Criteria

Assistant can:

Navigate entire app
Execute commands
Provide voice feedback
Work offline

---

END OF DESIGN DOCUMENT
