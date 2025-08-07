package com.nekotts.app.utils

object Constants {
    // App Information
    const val APP_NAME = "Neko TTS"
    const val APP_VERSION = "1.0.0"
    const val APP_PACKAGE = "com.nekotts.app"
    
    // TTS Engine Constants
    const val TTS_ENGINE_NAME = "NekoTTS"
    const val TTS_ENGINE_LABEL = "Neko Text-to-Speech"
    const val SAMPLE_RATE = 22050
    const val AUDIO_FORMAT_16BIT = 16
    
    // Model Information
    const val KOKORO_MODEL_SIZE_MB = 310
    const val KITTEN_MODEL_SIZE_MB = 25
    const val KOKORO_VOICES_COUNT = 54
    const val KITTEN_VOICES_COUNT = 8
    
    // File Names
    const val KOKORO_MODEL_FILE = "kokoro-v1.0.onnx"
    const val KITTEN_MODEL_FILE = "kitten_tts_nano_v0_1.onnx"
    const val VOICES_FILE = "voices.bin"
    
    // Directories
    const val MODELS_DIR = "models"
    const val CACHE_DIR = "cache"
    
    // Default Settings
    const val DEFAULT_VOICE_ID = "expr-voice-2-f"
    const val DEFAULT_SPEECH_SPEED = 1.0f
    const val DEFAULT_SPEECH_PITCH = 1.0f
    const val MIN_SPEECH_SPEED = 0.5f
    const val MAX_SPEECH_SPEED = 2.0f
    const val MIN_SPEECH_PITCH = 0.5f
    const val MAX_SPEECH_PITCH = 2.0f
    
    // Intent Actions
    const val ACTION_PROCESS_TEXT = "android.intent.action.PROCESS_TEXT"
    const val EXTRA_PROCESS_TEXT = "android.intent.extra.PROCESS_TEXT"
    const val EXTRA_PROCESS_TEXT_READONLY = "android.intent.extra.PROCESS_TEXT_READONLY"
    
    // Notification Channels
    const val TTS_NOTIFICATION_CHANNEL_ID = "neko_tts_channel"
    const val TTS_NOTIFICATION_CHANNEL_NAME = "Neko TTS"
    const val TTS_NOTIFICATION_ID = 1001
    
    // Error Messages
    const val ERROR_INITIALIZATION_FAILED = "Failed to initialize TTS engine"
    const val ERROR_SYNTHESIS_FAILED = "Speech synthesis failed"
    const val ERROR_MODEL_NOT_FOUND = "TTS model not found"
    const val ERROR_VOICE_NOT_FOUND = "Voice not found"
    const val ERROR_AUDIO_PLAYBACK_FAILED = "Audio playback failed"
    
    // Success Messages
    const val SUCCESS_INITIALIZATION = "TTS engine initialized successfully"
    const val SUCCESS_SYNTHESIS = "Speech synthesis completed"
    
    // Limits
    const val MAX_TEXT_LENGTH = 5000
    const val MAX_SYNTHESIS_DURATION_MS = 60000 // 1 minute
    const val MODEL_INPUT_LENGTH = 512
    
    // Audio Processing
    const val FADE_DURATION_MS = 50
    const val AUDIO_BUFFER_SIZE = 8192
    
    // Supported Languages (Kokoro)
    val SUPPORTED_LANGUAGES = setOf(
        "af", "am", "ar", "as", "az", "be", "bg", "bn", "br", "bs", "ca", "cs", "cy",
        "da", "de", "el", "en", "eo", "es", "et", "eu", "fa", "fi", "fr", "ga", "gl",
        "gu", "ha", "he", "hi", "hr", "hu", "hy", "id", "is", "it", "ja", "jv", "ka",
        "kk", "km", "kn", "ko", "la", "lb", "lg", "ln", "lo", "lt", "lv", "mg", "mk",
        "ml", "mn", "mr", "ms", "mt", "my", "ne", "nl", "nn", "no", "oc", "pa", "pl",
        "ps", "pt", "ro", "ru", "sk", "sl", "sn", "so", "sq", "sr", "su", "sv", "sw",
        "ta", "te", "tg", "th", "tk", "tr", "tt", "uk", "ur", "uz", "vi", "yo", "zh"
    )
    
    // Language Names
    val LANGUAGE_NAMES = mapOf(
        "af" to "Afrikaans",
        "am" to "Amharic",
        "ar" to "Arabic",
        "as" to "Assamese",
        "az" to "Azerbaijani",
        "be" to "Belarusian",
        "bg" to "Bulgarian",
        "bn" to "Bengali",
        "br" to "Breton",
        "bs" to "Bosnian",
        "ca" to "Catalan",
        "cs" to "Czech",
        "cy" to "Welsh",
        "da" to "Danish",
        "de" to "German",
        "el" to "Greek",
        "en" to "English",
        "eo" to "Esperanto",
        "es" to "Spanish",
        "et" to "Estonian",
        "eu" to "Basque",
        "fa" to "Persian",
        "fi" to "Finnish",
        "fr" to "French",
        "ga" to "Irish",
        "gl" to "Galician",
        "gu" to "Gujarati",
        "ha" to "Hausa",
        "he" to "Hebrew",
        "hi" to "Hindi",
        "hr" to "Croatian",
        "hu" to "Hungarian",
        "hy" to "Armenian",
        "id" to "Indonesian",
        "is" to "Icelandic",
        "it" to "Italian",
        "ja" to "Japanese",
        "jv" to "Javanese",
        "ka" to "Georgian",
        "kk" to "Kazakh",
        "km" to "Khmer",
        "kn" to "Kannada",
        "ko" to "Korean",
        "la" to "Latin",
        "lb" to "Luxembourgish",
        "lg" to "Luganda",
        "ln" to "Lingala",
        "lo" to "Lao",
        "lt" to "Lithuanian",
        "lv" to "Latvian",
        "mg" to "Malagasy",
        "mk" to "Macedonian",
        "ml" to "Malayalam",
        "mn" to "Mongolian",
        "mr" to "Marathi",
        "ms" to "Malay",
        "mt" to "Maltese",
        "my" to "Myanmar",
        "ne" to "Nepali",
        "nl" to "Dutch",
        "nn" to "Norwegian Nynorsk",
        "no" to "Norwegian",
        "oc" to "Occitan",
        "pa" to "Punjabi",
        "pl" to "Polish",
        "ps" to "Pashto",
        "pt" to "Portuguese",
        "ro" to "Romanian",
        "ru" to "Russian",
        "sk" to "Slovak",
        "sl" to "Slovenian",
        "sn" to "Shona",
        "so" to "Somali",
        "sq" to "Albanian",
        "sr" to "Serbian",
        "su" to "Sundanese",
        "sv" to "Swedish",
        "sw" to "Swahili",
        "ta" to "Tamil",
        "te" to "Telugu",
        "tg" to "Tajik",
        "th" to "Thai",
        "tk" to "Turkmen",
        "tr" to "Turkish",
        "tt" to "Tatar",
        "uk" to "Ukrainian",
        "ur" to "Urdu",
        "uz" to "Uzbek",
        "vi" to "Vietnamese",
        "yo" to "Yoruba",
        "zh" to "Chinese"
    )
}