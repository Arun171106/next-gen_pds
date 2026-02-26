package com.example.nextgen_pds_kiosk.voice

import javax.inject.Inject
import javax.inject.Singleton

enum class AppIntent {
    NAVIGATE_NEXT,
    NAVIGATE_BACK,
    START_DISPENSING,
    INCREASE_QUANTITY,
    DECREASE_QUANTITY,
    SWITCH_LANGUAGE_ENGLISH,
    SWITCH_LANGUAGE_HINDI,
    SWITCH_LANGUAGE_TAMIL,
    UNKNOWN
}

@Singleton
class IntentParser @Inject constructor() {

    // Simple rule-based offline Keyword matching arrays
    // Supports English, Hindi (Hinglish + Devanagari), and Tamil.

    // 1. "Next", "Start", "Continue"
    private val nextKeywords = listOf(
        "next", "continue", "start", "proceed", "go to next", "begin",
        "தொடரவும்", "அடுத்தது", "ஆரம்பி", // Tamil
        "shuru", "aage", "chalo", "shuru karein", "aage badho", "शुरू", "आगे" // Hindi
    )

    // 2. "Back", "Cancel", "Return"
    private val backKeywords = listOf(
        "back", "go back", "return", "cancel", "previous",
        "பின்னால்", "திரும்பு", "ரத்துசெய்", // Tamil
        "peeche", "wapas", "khatam", "wapas jao", "peeche jao", "रद्द", "वापस" // Hindi
    )

    // 3. "Dispense", "Give me"
    private val dispenseKeywords = listOf(
        "dispense", "give me", "start dispensing", "drop", "provide",
        "வழங்கு", "கொடு", "ரிலீஸ்", // Tamil (Vazhangu, Kodu, Release)
        "do", "nikalo", "pradan karein", "anaj nikalo", "दीजिए", "निकालो" // Hindi
    )

    // 4. "Increase", "More"
    private val increaseKeywords = listOf(
        "more", "increase", "add", "plus", "extra",
        "அதிகம்", "கூட்டு", "இன்னும்", // Tamil
        "zyada", "aur", "badhao", "jyada", "ज़्यादा", "और" // Hindi
    )

    // 5. "Decrease", "Less"
    private val decreaseKeywords = listOf(
        "less", "decrease", "subtract", "minus", "reduce",
        "குறை", "கழி", "கம்மி", // Tamil
        "kam", "ghatao", "kam karo", "कम" // Hindi
    )

    // 6. Language Switchers (Highest Priority)
    // English variations 
    // "English" written in native Tamil: இங்கிலீஷ்
    // "English" written in native Hindi: इंग्लिश
    private val englishKeywords = listOf(
        "english", "speak in english", "switch to english", 
        "angrezi", "angrezi mein", "angreji", "अंग्रेज़ी", "अंग्रेजी",
        "angilam", "ஆங்கிலம்", "english pesu", 
        "இங்கிலீஷ்", "इंग्लिश"
    )
    
    // Hindi variations 
    // "Hindi" written in native Tamil: ஹிந்தி or இந்தி
    private val hindiKeywords = listOf(
        "hindi", "हिंदी", "speak in hindi", "switch to hindi", 
        "hindi mein", "hindi bolo", "hindi shuru",
        "indhi", "இந்தி", "ஹிந்தி"
    )
    
    // Tamil variations 
    // "Tamil" written in native Hindi: तमिल
    private val tamilKeywords = listOf(
        "tamil", "தமிழ்", "speak in tamil", "switch to tamil", "thamizh",
        "tamil mein", "tamil pesu", "tamil shuru",
        "तमिल"
    )

    fun parseIntent(text: String, currentLanguage: String = "en"): AppIntent {
        val normalizedString = text.lowercase().trim()

        if (normalizedString.isBlank()) return AppIntent.UNKNOWN

        // 1. Language Switches (Highest Priority — can intercept across any screen)
        when {
            englishKeywords.any { normalizedString.contains(it) } -> return AppIntent.SWITCH_LANGUAGE_ENGLISH
            hindiKeywords.any { normalizedString.contains(it) } -> return AppIntent.SWITCH_LANGUAGE_HINDI
            tamilKeywords.any { normalizedString.contains(it) } -> return AppIntent.SWITCH_LANGUAGE_TAMIL
        }

        // 2. Strict Exact Match (High Priority)
        when {
            nextKeywords.contains(normalizedString) -> return AppIntent.NAVIGATE_NEXT
            backKeywords.contains(normalizedString) -> return AppIntent.NAVIGATE_BACK
            dispenseKeywords.contains(normalizedString) -> return AppIntent.START_DISPENSING
            increaseKeywords.contains(normalizedString) -> return AppIntent.INCREASE_QUANTITY
            decreaseKeywords.contains(normalizedString) -> return AppIntent.DECREASE_QUANTITY
        }

        // 3. Fuzzy Overlap Match (If the user speaks a long sentence "Please go to the next screen")
        if (nextKeywords.any { normalizedString.contains(it) }) return AppIntent.NAVIGATE_NEXT
        if (backKeywords.any { normalizedString.contains(it) }) return AppIntent.NAVIGATE_BACK
        if (dispenseKeywords.any { normalizedString.contains(it) }) return AppIntent.START_DISPENSING
        if (increaseKeywords.any { normalizedString.contains(it) }) return AppIntent.INCREASE_QUANTITY
        if (decreaseKeywords.any { normalizedString.contains(it) }) return AppIntent.DECREASE_QUANTITY

        // 4. Fallback
        return AppIntent.UNKNOWN
    }
}
