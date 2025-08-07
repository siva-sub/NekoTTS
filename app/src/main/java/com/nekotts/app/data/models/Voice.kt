package com.nekotts.app.data.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a TTS voice with all its properties
 */
data class Voice(
    val id: String,
    val name: String,
    val displayName: String,
    val language: String,
    val languageName: String,
    val gender: VoiceGender,
    val engine: VoiceEngine,
    val description: String = "",
    val isDownloaded: Boolean = false,
    val sampleAudioUrl: String? = null,
    val characteristics: List<VoiceCharacteristic> = emptyList(),
    val quality: VoiceQuality = VoiceQuality.STANDARD,
    val isDefault: Boolean = false,
    val isEnabled: Boolean = true
) {
    /**
     * Returns a friendly display name combining voice name and language
     */
    val fullDisplayName: String
        get() = "$displayName ($languageName)"

    /**
     * Returns the icon for this voice based on gender
     */
    val icon: ImageVector
        get() = when (gender) {
            VoiceGender.FEMALE -> Icons.Default.Person
            VoiceGender.MALE -> Icons.Default.Person
            VoiceGender.NEUTRAL -> Icons.Default.Person
            VoiceGender.OTHER -> Icons.Default.Person
        }

    /**
     * Returns a color-coded accent based on the voice characteristics
     */
    val accentColor: Long
        get() = when {
            characteristics.contains(VoiceCharacteristic.CUTE) -> 0xFFFFB3BA
            characteristics.contains(VoiceCharacteristic.DEEP) -> 0xFF6C5CE7
            characteristics.contains(VoiceCharacteristic.ENERGETIC) -> 0xFFFD79A8
            characteristics.contains(VoiceCharacteristic.CALM) -> 0xFF74B9FF
            characteristics.contains(VoiceCharacteristic.YOUTHFUL) -> 0xFFA29BFE
            gender == VoiceGender.FEMALE -> 0xFFE17055
            gender == VoiceGender.MALE -> 0xFF0984E3
            else -> 0xFF00B894
        }
}

/**
 * Voice gender enumeration
 */
enum class VoiceGender(val displayName: String) {
    FEMALE("Female"),
    MALE("Male"),
    NEUTRAL("Neutral"),
    OTHER("Other")
}

/**
 * TTS Engine type
 */
enum class VoiceEngine(val displayName: String, val identifier: String) {
    KOKORO("Kokoro", "kokoro"),
    KITTEN("Kitten", "kitten")
}

/**
 * Voice quality levels
 */
enum class VoiceQuality(val displayName: String, val description: String) {
    LOW("Low", "Basic quality, smaller model size"),
    STANDARD("Standard", "Good quality, balanced performance"),
    HIGH("High", "Excellent quality, larger model size"),
    PREMIUM("Premium", "Studio quality, requires more resources")
}

/**
 * Voice characteristics that describe the voice personality
 */
enum class VoiceCharacteristic(val displayName: String, val emoji: String) {
    CUTE("Cute", "😸"),
    DEEP("Deep", "🎭"),
    ENERGETIC("Energetic", "⚡"),
    CALM("Calm", "😌"),
    YOUTHFUL("Youthful", "✨"),
    MATURE("Mature", "🎩"),
    CHEERFUL("Cheerful", "😊"),
    SERIOUS("Serious", "🧐"),
    SOFT("Soft", "🌸"),
    STRONG("Strong", "💪")
}

/**
 * Predefined voices for Kitten engine - based on actual available models
 */
object KittenVoices {
    val voices = listOf(
        Voice(
            id = "expr-voice-2-f",
            name = "expr-voice-2-f",
            displayName = "Luna (Female)",
            language = "en",
            languageName = "English",
            gender = VoiceGender.FEMALE,
            engine = VoiceEngine.KITTEN,
            description = "Expressive female voice",
            characteristics = listOf(VoiceCharacteristic.SOFT, VoiceCharacteristic.CALM),
            quality = VoiceQuality.STANDARD,
            isDefault = true,
            isDownloaded = true
        ),
        Voice(
            id = "expr-voice-2-m",
            name = "expr-voice-2-m",
            displayName = "Felix (Male)",
            language = "en",
            languageName = "English",
            gender = VoiceGender.MALE,
            engine = VoiceEngine.KITTEN,
            description = "Expressive male voice",
            characteristics = listOf(VoiceCharacteristic.STRONG, VoiceCharacteristic.DEEP),
            quality = VoiceQuality.STANDARD,
            isDownloaded = true
        ),
        Voice(
            id = "expr-voice-3-f",
            name = "expr-voice-3-f",
            displayName = "Aria (Female)",
            language = "en",
            languageName = "English",
            gender = VoiceGender.FEMALE,
            engine = VoiceEngine.KITTEN,
            description = "Cheerful female voice",
            characteristics = listOf(VoiceCharacteristic.CHEERFUL, VoiceCharacteristic.YOUTHFUL),
            quality = VoiceQuality.STANDARD,
            isDownloaded = true
        ),
        Voice(
            id = "expr-voice-3-m",
            name = "expr-voice-3-m",
            displayName = "Oliver (Male)",
            language = "en",
            languageName = "English",
            gender = VoiceGender.MALE,
            engine = VoiceEngine.KITTEN,
            description = "Calm male voice",
            characteristics = listOf(VoiceCharacteristic.CALM, VoiceCharacteristic.MATURE),
            quality = VoiceQuality.STANDARD,
            isDownloaded = true
        ),
        Voice(
            id = "expr-voice-4-f",
            name = "expr-voice-4-f",
            displayName = "Nova (Female)",
            language = "en",
            languageName = "English",
            gender = VoiceGender.FEMALE,
            engine = VoiceEngine.KITTEN,
            description = "Mature female voice",
            characteristics = listOf(VoiceCharacteristic.MATURE, VoiceCharacteristic.SERIOUS),
            quality = VoiceQuality.STANDARD,
            isDownloaded = true
        ),
        Voice(
            id = "expr-voice-4-m",
            name = "expr-voice-4-m",
            displayName = "Max (Male)",
            language = "en",
            languageName = "English",
            gender = VoiceGender.MALE,
            engine = VoiceEngine.KITTEN,
            description = "Energetic male voice",
            characteristics = listOf(VoiceCharacteristic.CHEERFUL, VoiceCharacteristic.ENERGETIC),
            quality = VoiceQuality.STANDARD,
            isDownloaded = true
        ),
        Voice(
            id = "expr-voice-5-f",
            name = "expr-voice-5-f",
            displayName = "Mimi (Female)",
            language = "en",
            languageName = "English",
            gender = VoiceGender.FEMALE,
            engine = VoiceEngine.KITTEN,
            description = "Cute female voice",
            characteristics = listOf(VoiceCharacteristic.CUTE, VoiceCharacteristic.SOFT),
            quality = VoiceQuality.STANDARD,
            isDownloaded = true
        ),
        Voice(
            id = "expr-voice-5-m",
            name = "expr-voice-5-m",
            displayName = "Sage (Male)",
            language = "en",
            languageName = "English",
            gender = VoiceGender.MALE,
            engine = VoiceEngine.KITTEN,
            description = "Balanced male voice",
            characteristics = listOf(VoiceCharacteristic.CALM, VoiceCharacteristic.SOFT),
            quality = VoiceQuality.STANDARD,
            isDownloaded = true
        )
    )
}

/**
 * Predefined voices for Kokoro engine - simplified popular voices
 */
object KokoroVoices {
    val voices = listOf(
        // Popular English voices
        Voice(
            id = "af_heart", 
            name = "af_heart", 
            displayName = "Heart (English F)", 
            language = "en", 
            languageName = "English", 
            gender = VoiceGender.FEMALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Warm English female voice", 
            characteristics = listOf(VoiceCharacteristic.SOFT), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        Voice(
            id = "af_bella", 
            name = "af_bella", 
            displayName = "Bella (English F)", 
            language = "en", 
            languageName = "English", 
            gender = VoiceGender.FEMALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Beautiful English voice", 
            characteristics = listOf(VoiceCharacteristic.CHEERFUL), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        Voice(
            id = "am_adam", 
            name = "am_adam", 
            displayName = "Adam (English M)", 
            language = "en", 
            languageName = "English", 
            gender = VoiceGender.MALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Confident English male voice", 
            characteristics = listOf(VoiceCharacteristic.STRONG), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        Voice(
            id = "am_michael", 
            name = "am_michael", 
            displayName = "Michael (English M)", 
            language = "en", 
            languageName = "English", 
            gender = VoiceGender.MALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Professional English male voice", 
            characteristics = listOf(VoiceCharacteristic.MATURE), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        
        // A few international voices
        Voice(
            id = "es_maria", 
            name = "es_maria", 
            displayName = "María (Spanish F)", 
            language = "es", 
            languageName = "Spanish", 
            gender = VoiceGender.FEMALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Vibrant Spanish female voice", 
            characteristics = listOf(VoiceCharacteristic.CHEERFUL), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        Voice(
            id = "fr_amelie", 
            name = "fr_amelie", 
            displayName = "Amélie (French F)", 
            language = "fr", 
            languageName = "French", 
            gender = VoiceGender.FEMALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Elegant French female voice", 
            characteristics = listOf(VoiceCharacteristic.SOFT), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        Voice(
            id = "jf_ayako", 
            name = "jf_ayako", 
            displayName = "Ayako (Japanese F)", 
            language = "ja", 
            languageName = "Japanese", 
            gender = VoiceGender.FEMALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Sweet Japanese female voice", 
            characteristics = listOf(VoiceCharacteristic.CUTE), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        Voice(
            id = "zf_xiaobei", 
            name = "zf_xiaobei", 
            displayName = "Xiaobei (Chinese F)", 
            language = "zh", 
            languageName = "Chinese", 
            gender = VoiceGender.FEMALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Melodic Chinese female voice", 
            characteristics = listOf(VoiceCharacteristic.SOFT), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        )
    )
}

/**
 * All available voices combined
 */
object AllVoices {
    val voices = KittenVoices.voices + KokoroVoices.voices
    
    fun getVoiceById(id: String): Voice? = voices.find { it.id == id }
    
    fun getVoicesByLanguage(language: String): List<Voice> = 
        voices.filter { it.language == language }
    
    fun getVoicesByEngine(engine: VoiceEngine): List<Voice> = 
        voices.filter { it.engine == engine }
    
    fun getDownloadedVoices(): List<Voice> = 
        voices.filter { it.isDownloaded }
    
    fun getDefaultVoice(): Voice = 
        voices.find { it.isDefault } ?: KittenVoices.voices.first()
}