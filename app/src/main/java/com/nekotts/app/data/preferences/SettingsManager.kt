package com.nekotts.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.nekotts.app.data.models.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(
    private val context: Context
) {
    private val sharedPreferences: SharedPreferences = 
        PreferenceManager.getDefaultSharedPreferences(context)
    
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()
    
    companion object {
        private const val KEY_SELECTED_VOICE = "selected_voice"
        private const val KEY_SPEECH_SPEED = "speech_speed"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_AUTO_LANGUAGE_DETECTION = "auto_language_detection"
        private const val KEY_USE_SYSTEM_VOLUME = "use_system_volume"
        private const val KEY_ENABLE_NOTIFICATION_READING = "enable_notification_reading"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_SELECTED_ENGINE = "selected_engine"
        private const val KEY_ENABLE_PROCESS_TEXT = "enable_process_text"
        
        // Default values
        private const val DEFAULT_VOICE = "ktn_f1"
        private const val DEFAULT_SPEED = 1.0f
        private const val DEFAULT_PITCH = 1.0f
        private const val DEFAULT_ENGINE = "kokoro"
    }
    
    fun getSelectedVoice(): String {
        return sharedPreferences.getString(KEY_SELECTED_VOICE, DEFAULT_VOICE) ?: DEFAULT_VOICE
    }
    
    fun setSelectedVoice(voiceId: String) {
        sharedPreferences.edit()
            .putString(KEY_SELECTED_VOICE, voiceId)
            .apply()
    }
    
    fun getSpeechSpeed(): Float {
        return sharedPreferences.getFloat(KEY_SPEECH_SPEED, DEFAULT_SPEED)
    }
    
    fun setSpeechSpeed(speed: Float) {
        sharedPreferences.edit()
            .putFloat(KEY_SPEECH_SPEED, speed.coerceIn(0.5f, 2.0f))
            .apply()
    }
    
    fun getSpeechPitch(): Float {
        return sharedPreferences.getFloat(KEY_SPEECH_PITCH, DEFAULT_PITCH)
    }
    
    fun setSpeechPitch(pitch: Float) {
        sharedPreferences.edit()
            .putFloat(KEY_SPEECH_PITCH, pitch.coerceIn(0.5f, 2.0f))
            .apply()
    }
    
    fun getSelectedEngine(): String {
        return sharedPreferences.getString(KEY_SELECTED_ENGINE, DEFAULT_ENGINE) ?: DEFAULT_ENGINE
    }
    
    fun setSelectedEngine(engine: String) {
        sharedPreferences.edit()
            .putString(KEY_SELECTED_ENGINE, engine)
            .apply()
    }
    
    fun isAutoLanguageDetectionEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_LANGUAGE_DETECTION, true)
    }
    
    fun setAutoLanguageDetectionEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_AUTO_LANGUAGE_DETECTION, enabled)
            .apply()
    }
    
    fun isSystemVolumeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_USE_SYSTEM_VOLUME, true)
    }
    
    fun setSystemVolumeEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_USE_SYSTEM_VOLUME, enabled)
            .apply()
    }
    
    fun isNotificationReadingEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_ENABLE_NOTIFICATION_READING, false)
    }
    
    fun setNotificationReadingEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_ENABLE_NOTIFICATION_READING, enabled)
            .apply()
    }
    
    fun isProcessTextEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_ENABLE_PROCESS_TEXT, true)
    }
    
    fun setProcessTextEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_ENABLE_PROCESS_TEXT, enabled)
            .apply()
    }
    
    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    fun setFirstLaunchCompleted() {
        sharedPreferences.edit()
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .apply()
    }
    
    fun resetToDefaults() {
        sharedPreferences.edit()
            .putString(KEY_SELECTED_VOICE, DEFAULT_VOICE)
            .putFloat(KEY_SPEECH_SPEED, DEFAULT_SPEED)
            .putFloat(KEY_SPEECH_PITCH, DEFAULT_PITCH)
            .putString(KEY_SELECTED_ENGINE, DEFAULT_ENGINE)
            .putBoolean(KEY_AUTO_LANGUAGE_DETECTION, true)
            .putBoolean(KEY_USE_SYSTEM_VOLUME, true)
            .putBoolean(KEY_ENABLE_NOTIFICATION_READING, false)
            .putBoolean(KEY_ENABLE_PROCESS_TEXT, true)
            .apply()
    }
    
    fun exportSettings(): Map<String, Any?> {
        return mapOf(
            KEY_SELECTED_VOICE to getSelectedVoice(),
            KEY_SPEECH_SPEED to getSpeechSpeed(),
            KEY_SPEECH_PITCH to getSpeechPitch(),
            KEY_SELECTED_ENGINE to getSelectedEngine(),
            KEY_AUTO_LANGUAGE_DETECTION to isAutoLanguageDetectionEnabled(),
            KEY_USE_SYSTEM_VOLUME to isSystemVolumeEnabled(),
            KEY_ENABLE_NOTIFICATION_READING to isNotificationReadingEnabled(),
            KEY_ENABLE_PROCESS_TEXT to isProcessTextEnabled()
        )
    }
    
    fun importSettings(settings: Map<String, Any?>) {
        val editor = sharedPreferences.edit()
        
        settings.forEach { (key, value) ->
            when (key) {
                KEY_SELECTED_VOICE -> value?.let { editor.putString(key, it.toString()) }
                KEY_SPEECH_SPEED -> (value as? Float)?.let { editor.putFloat(key, it) }
                KEY_SPEECH_PITCH -> (value as? Float)?.let { editor.putFloat(key, it) }
                KEY_SELECTED_ENGINE -> value?.let { editor.putString(key, it.toString()) }
                KEY_AUTO_LANGUAGE_DETECTION -> (value as? Boolean)?.let { editor.putBoolean(key, it) }
                KEY_USE_SYSTEM_VOLUME -> (value as? Boolean)?.let { editor.putBoolean(key, it) }
                KEY_ENABLE_NOTIFICATION_READING -> (value as? Boolean)?.let { editor.putBoolean(key, it) }
                KEY_ENABLE_PROCESS_TEXT -> (value as? Boolean)?.let { editor.putBoolean(key, it) }
            }
        }
        
        editor.apply()
    }

    /**
     * Load settings from SharedPreferences into a Settings object
     */
    private fun loadSettings(): Settings {
        return Settings(
            selectedVoiceId = getSelectedVoice(),
            speechSpeed = getSpeechSpeed(),
            speechPitch = getSpeechPitch(),
            defaultEngine = com.nekotts.app.data.models.VoiceEngine.valueOf(
                getSelectedEngine().uppercase()
            ),
            analyticsEnabled = true, // Default values for other settings
            crashReportingEnabled = true,
            dataCollectionEnabled = true,
            onboardingCompleted = !isFirstLaunch(),
            isFirstRun = isFirstLaunch(),
            permissionsGranted = true
        )
    }

    /**
     * Update settings using a transform function
     */
    suspend fun updateSettings(transform: (Settings) -> Settings) {
        val currentSettings = _settings.value
        val newSettings = transform(currentSettings).validated()
        
        // Save individual settings back to SharedPreferences
        setSelectedVoice(newSettings.selectedVoiceId)
        setSpeechSpeed(newSettings.speechSpeed)
        setSpeechPitch(newSettings.speechPitch)
        setSelectedEngine(newSettings.defaultEngine.identifier)
        
        if (!newSettings.onboardingCompleted) {
            setFirstLaunchCompleted()
        }
        
        _settings.value = newSettings
    }

    /**
     * Export settings as JSON string
     */
    fun exportSettingsAsString(settings: Settings): String {
        return try {
            // Simple JSON representation using existing map export
            val settingsMap = exportSettings()
            settingsMap.entries.joinToString(", ") { "${it.key}=${it.value}" }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Import settings from JSON string
     */
    fun importSettingsFromString(settingsString: String): Settings {
        return try {
            // Simple parsing - in a real app this would use proper JSON
            val settingsMap = mutableMapOf<String, Any?>()
            settingsString.split(", ").forEach { pair ->
                val (key, value) = pair.split("=", limit = 2)
                settingsMap[key] = value
            }
            
            importSettings(settingsMap)
            loadSettings()
        } catch (e: Exception) {
            Settings() // Return default settings on parse error
        }
    }
}