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
 * Predefined voices for Kokoro engine (multilingual)
 */
object KokoroVoices {
    val voices = listOf(
        // English voices
        Voice(
            id = "kokoro_en_f1",
            name = "kokoro_en_f1",
            displayName = "Sakura",
            language = "en",
            languageName = "English",
            gender = VoiceGender.FEMALE,
            engine = VoiceEngine.KOKORO,
            description = "High-quality English female voice with natural intonation",
            characteristics = listOf(VoiceCharacteristic.SOFT, VoiceCharacteristic.MATURE),
            quality = VoiceQuality.HIGH,
            isDownloaded = false
        ),
        Voice(
            id = "kokoro_en_m1",
            name = "kokoro_en_m1",
            displayName = "Takeshi",
            language = "en",
            languageName = "English",
            gender = VoiceGender.MALE,
            engine = VoiceEngine.KOKORO,
            description = "High-quality English male voice with clear pronunciation",
            characteristics = listOf(VoiceCharacteristic.DEEP, VoiceCharacteristic.SERIOUS),
            quality = VoiceQuality.HIGH,
            isDownloaded = false
        ),
        // Japanese voices
        Voice(
            id = "kokoro_ja_f1",
            name = "kokoro_ja_f1",
            displayName = "Yuki",
            language = "ja",
            languageName = "Japanese",
            gender = VoiceGender.FEMALE,
            engine = VoiceEngine.KOKORO,
            description = "Natural Japanese female voice",
            characteristics = listOf(VoiceCharacteristic.CUTE, VoiceCharacteristic.CHEERFUL),
            quality = VoiceQuality.HIGH,
            isDownloaded = false
        ),
        // Spanish voices
        Voice(
            id = "kokoro_es_f1",
            name = "kokoro_es_f1",
            displayName = "María",
            language = "es",
            languageName = "Spanish",
            gender = VoiceGender.FEMALE,
            engine = VoiceEngine.KOKORO,
            description = "Native Spanish female voice",
            characteristics = listOf(VoiceCharacteristic.ENERGETIC, VoiceCharacteristic.CHEERFUL),
            quality = VoiceQuality.HIGH,
            isDownloaded = false
        ),
        // French voices
        Voice(
            id = "kokoro_fr_f1",
            name = "kokoro_fr_f1",
            displayName = "Amélie",
            language = "fr",
            languageName = "French",
            gender = VoiceGender.FEMALE,
            engine = VoiceEngine.KOKORO,
            description = "Elegant French female voice",
            characteristics = listOf(VoiceCharacteristic.SOFT, VoiceCharacteristic.MATURE),
            quality = VoiceQuality.HIGH,
            isDownloaded = false
        ),
        // German voices
        Voice(
            id = "kokoro_de_m1",
            name = "kokoro_de_m1",
            displayName = "Hans",
            language = "de",
            languageName = "German",
            gender = VoiceGender.MALE,
            engine = VoiceEngine.KOKORO,
            description = "Clear German male voice",
            characteristics = listOf(VoiceCharacteristic.STRONG, VoiceCharacteristic.SERIOUS),
            quality = VoiceQuality.HIGH,
            isDownloaded = false
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