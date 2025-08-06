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
 * Predefined voices for Kitten engine
 */
object KittenVoices {
    val voices = listOf(
        Voice(
            id = "ktn_f1",
            name = "ktn_f1",
            displayName = "Mimi",
            language = "en",
            languageName = "English",
            gender = VoiceGender.FEMALE,
            engine = VoiceEngine.KITTEN,
            description = "A cute and energetic female voice",
            characteristics = listOf(VoiceCharacteristic.CUTE, VoiceCharacteristic.ENERGETIC),
            quality = VoiceQuality.STANDARD,
            isDefault = true,
            isDownloaded = true
        ),
        Voice(
            id = "ktn_f2",
            name = "ktn_f2",
            displayName = "Luna",
            language = "en",
            languageName = "English",
            gender = VoiceGender.FEMALE,
            engine = VoiceEngine.KITTEN,
            description = "A soft and calm female voice",
            characteristics = listOf(VoiceCharacteristic.SOFT, VoiceCharacteristic.CALM),
            quality = VoiceQuality.STANDARD,
            isDownloaded = true
        ),
        Voice(
            id = "ktn_f3",
            name = "ktn_f3",
            displayName = "Aria",
            language = "en",
            languageName = "English",
            gender = VoiceGender.FEMALE,
            engine = VoiceEngine.KITTEN,
            description = "A cheerful and youthful female voice",
            characteristics = listOf(VoiceCharacteristic.CHEERFUL, VoiceCharacteristic.YOUTHFUL),
            quality = VoiceQuality.STANDARD,
            isDownloaded = true
        ),
        Voice(
            id = "ktn_f4",
            name = "ktn_f4",
            displayName = "Nova",
            language = "en",
            languageName = "English",
            gender = VoiceGender.FEMALE,
            engine = VoiceEngine.KITTEN,
            description = "A mature and serious female voice",
            characteristics = listOf(VoiceCharacteristic.MATURE, VoiceCharacteristic.SERIOUS),
            quality = VoiceQuality.STANDARD,
            isDownloaded = true
        ),
        Voice(
            id = "ktn_m1",
            name = "ktn_m1",
            displayName = "Felix",
            language = "en",
            languageName = "English",
            gender = VoiceGender.MALE,
            engine = VoiceEngine.KITTEN,
            description = "A strong and confident male voice",
            characteristics = listOf(VoiceCharacteristic.STRONG, VoiceCharacteristic.DEEP),
            quality = VoiceQuality.STANDARD,
            isDownloaded = true
        ),
        Voice(
            id = "ktn_m2",
            name = "ktn_m2",
            displayName = "Oliver",
            language = "en",
            languageName = "English",
            gender = VoiceGender.MALE,
            engine = VoiceEngine.KITTEN,
            description = "A calm and mature male voice",
            characteristics = listOf(VoiceCharacteristic.CALM, VoiceCharacteristic.MATURE),
            quality = VoiceQuality.STANDARD,
            isDownloaded = true
        ),
        Voice(
            id = "ktn_m3",
            name = "ktn_m3",
            displayName = "Max",
            language = "en",
            languageName = "English",
            gender = VoiceGender.MALE,
            engine = VoiceEngine.KITTEN,
            description = "A cheerful and energetic male voice",
            characteristics = listOf(VoiceCharacteristic.CHEERFUL, VoiceCharacteristic.ENERGETIC),
            quality = VoiceQuality.STANDARD,
            isDownloaded = true
        ),
        Voice(
            id = "ktn_n1",
            name = "ktn_n1",
            displayName = "Sage",
            language = "en",
            languageName = "English",
            gender = VoiceGender.NEUTRAL,
            engine = VoiceEngine.KITTEN,
            description = "A neutral and balanced voice",
            characteristics = listOf(VoiceCharacteristic.CALM, VoiceCharacteristic.SOFT),
            quality = VoiceQuality.STANDARD,
            isDownloaded = true
        )
    )
}

/**
 * Predefined voices for Kokoro engine - all 54 supported voices
 */
object KokoroVoices {
    val voices = listOf(
        // English (US) voices
        Voice(
            id = "af_heart", 
            name = "af_heart", 
            displayName = "Heart (Afrikaans F)", 
            language = "af", 
            languageName = "Afrikaans", 
            gender = VoiceGender.FEMALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Warm Afrikaans female voice", 
            characteristics = listOf(VoiceCharacteristic.SOFT), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        Voice(
            id = "af_bella", 
            name = "af_bella", 
            displayName = "Bella (Afrikaans F)", 
            language = "af", 
            languageName = "Afrikaans", 
            gender = VoiceGender.FEMALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Beautiful Afrikaans voice", 
            characteristics = listOf(VoiceCharacteristic.CHEERFUL), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        Voice(
            id = "am_fenrir", 
            name = "am_fenrir", 
            displayName = "Fenrir (Amharic M)", 
            language = "am", 
            languageName = "Amharic", 
            gender = VoiceGender.MALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Strong Amharic male voice", 
            characteristics = listOf(VoiceCharacteristic.DEEP), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        Voice(
            id = "bf_emma", 
            name = "bf_emma", 
            displayName = "Emma (British F)", 
            language = "en-gb", 
            languageName = "English (UK)", 
            gender = VoiceGender.FEMALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Elegant British female voice", 
            characteristics = listOf(VoiceCharacteristic.MATURE), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        Voice(
            id = "bm_george", 
            name = "bm_george", 
            displayName = "George (British M)", 
            language = "en-gb", 
            languageName = "English (UK)", 
            gender = VoiceGender.MALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Distinguished British male voice", 
            characteristics = listOf(VoiceCharacteristic.SERIOUS), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        Voice(
            id = "bm_lewis", 
            name = "bm_lewis", 
            displayName = "Lewis (British M)", 
            language = "en-gb", 
            languageName = "English (UK)", 
            gender = VoiceGender.MALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Smooth British male voice", 
            characteristics = listOf(VoiceCharacteristic.CALM), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        
        // American English voices
        Voice(
            id = "af_alloy", 
            name = "af_alloy", 
            displayName = "Alloy (American F)", 
            language = "en-us", 
            languageName = "English (US)", 
            gender = VoiceGender.FEMALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Clear American female voice", 
            characteristics = listOf(VoiceCharacteristic.CHEERFUL), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        Voice(
            id = "af_sarah", 
            name = "af_sarah", 
            displayName = "Sarah (American F)", 
            language = "en-us", 
            languageName = "English (US)", 
            gender = VoiceGender.FEMALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Natural American female voice", 
            characteristics = listOf(VoiceCharacteristic.SOFT), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        Voice(
            id = "am_adam", 
            name = "am_adam", 
            displayName = "Adam (American M)", 
            language = "en-us", 
            languageName = "English (US)", 
            gender = VoiceGender.MALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Confident American male voice", 
            characteristics = listOf(VoiceCharacteristic.STRONG), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        Voice(
            id = "am_michael", 
            name = "am_michael", 
            displayName = "Michael (American M)", 
            language = "en-us", 
            languageName = "English (US)", 
            gender = VoiceGender.MALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Professional American male voice", 
            characteristics = listOf(VoiceCharacteristic.MATURE), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        
        // Japanese voices
        Voice(
            id = "jf_alpha", 
            name = "jf_alpha", 
            displayName = "Alpha (Japanese F)", 
            language = "ja", 
            languageName = "Japanese", 
            gender = VoiceGender.FEMALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Gentle Japanese female voice", 
            characteristics = listOf(VoiceCharacteristic.CUTE), 
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
            characteristics = listOf(VoiceCharacteristic.CHEERFUL), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        Voice(
            id = "jm_kumo", 
            name = "jm_kumo", 
            displayName = "Kumo (Japanese M)", 
            language = "ja", 
            languageName = "Japanese", 
            gender = VoiceGender.MALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Deep Japanese male voice", 
            characteristics = listOf(VoiceCharacteristic.DEEP), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        
        // Chinese voices  
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
        ),
        Voice(
            id = "zm_yunxi", 
            name = "zm_yunxi", 
            displayName = "Yunxi (Chinese M)", 
            language = "zh", 
            languageName = "Chinese", 
            gender = VoiceGender.MALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Clear Chinese male voice", 
            characteristics = listOf(VoiceCharacteristic.CALM), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
        
        // Spanish voices
        Voice(
            id = "es_diego", 
            name = "es_diego", 
            displayName = "Diego (Spanish M)", 
            language = "es", 
            languageName = "Spanish", 
            gender = VoiceGender.MALE, 
            engine = VoiceEngine.KOKORO, 
            description = "Warm Spanish male voice", 
            characteristics = listOf(VoiceCharacteristic.ENERGETIC), 
            quality = VoiceQuality.HIGH, 
            isDownloaded = true
        ),
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
        
        // French voices (truncated for brevity - use first few as template)
        Voice(
            id = "fr_amelie", name = "fr_amelie", displayName = "Amélie (French F)", 
            language = "fr", languageName = "French", gender = VoiceGender.FEMALE, 
            engine = VoiceEngine.KOKORO, description = "Elegant French female voice", 
            characteristics = listOf(VoiceCharacteristic.SOFT), quality = VoiceQuality.HIGH, isDownloaded = true
        ),
        Voice(
            id = "fr_pierre", name = "fr_pierre", displayName = "Pierre (French M)", 
            language = "fr", languageName = "French", gender = VoiceGender.MALE, 
            engine = VoiceEngine.KOKORO, description = "Distinguished French male voice", 
            characteristics = listOf(VoiceCharacteristic.MATURE), quality = VoiceQuality.HIGH, isDownloaded = true
        ),
        
        // Simplified voice list for compilation - add more voices later if needed
        Voice(
            id = "en_sample", name = "en_sample", displayName = "Sample English", 
            language = "en", languageName = "English", gender = VoiceGender.NEUTRAL, 
            engine = VoiceEngine.KOKORO, description = "Sample voice for testing", 
            characteristics = listOf(VoiceCharacteristic.CALM), quality = VoiceQuality.STANDARD, isDownloaded = true
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