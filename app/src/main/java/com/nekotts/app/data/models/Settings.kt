package com.nekotts.app.data.models

import com.nekotts.app.utils.Constants

/**
 * Application settings data class
 */
data class Settings(
    // Voice settings
    val selectedVoiceId: String = Constants.DEFAULT_VOICE_ID,
    val speechSpeed: Float = Constants.DEFAULT_SPEECH_SPEED,
    val speechPitch: Float = Constants.DEFAULT_SPEECH_PITCH,
    
    // Audio settings
    val audioStreamType: AudioStreamType = AudioStreamType.MUSIC,
    val audioFocusEnabled: Boolean = true,
    val audioFadeEnabled: Boolean = true,
    val audioFadeDurationMs: Int = Constants.FADE_DURATION_MS,
    
    // UI settings
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorsEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    val showVoiceCharacteristics: Boolean = true,
    
    // TTS settings
    val defaultEngine: VoiceEngine = VoiceEngine.KITTEN,
    val autoDownloadVoices: Boolean = false,
    val cacheEnabled: Boolean = true,
    val maxCacheSizeMB: Int = 100,
    val synthesisTimeoutMs: Int = Constants.MAX_SYNTHESIS_DURATION_MS,
    
    // Privacy settings
    val analyticsEnabled: Boolean = true,
    val crashReportingEnabled: Boolean = true,
    val usageStatisticsEnabled: Boolean = true,
    val dataCollectionEnabled: Boolean = true,
    
    // Accessibility settings
    val highContrastMode: Boolean = false,
    val largeTextMode: Boolean = false,
    val reduceMotion: Boolean = false,
    val screenReaderOptimized: Boolean = false,
    
    // Advanced settings
    val debugMode: Boolean = false,
    val verboseLogging: Boolean = false,
    val modelPreloadingEnabled: Boolean = true,
    val backgroundProcessingEnabled: Boolean = true,
    
    // Notification settings
    val notificationsEnabled: Boolean = true,
    val synthesisProgressNotifications: Boolean = false,
    val errorNotifications: Boolean = true,
    val downloadProgressNotifications: Boolean = true,
    
    // Language settings
    val preferredLanguage: String = "en",
    val automaticLanguageDetection: Boolean = true,
    val fallbackLanguage: String = "en",
    
    // Performance settings
    val maxConcurrentSynthesis: Int = 1,
    val useGPUAcceleration: Boolean = true,
    val optimizeForBattery: Boolean = false,
    val backgroundSynthesisEnabled: Boolean = true,
    
    // Text processing settings
    val maxTextLength: Int = Constants.MAX_TEXT_LENGTH,
    val autoBreakLongTexts: Boolean = true,
    val ssmlProcessingEnabled: Boolean = true,
    val punctuationHandling: PunctuationHandling = PunctuationHandling.NORMAL,
    
    // App behavior
    val keepScreenOnDuringSynthesis: Boolean = false,
    val pauseOnHeadphonesDisconnect: Boolean = true,
    val resumeOnHeadphonesConnect: Boolean = false,
    val showQuickActions: Boolean = true,
    
    // First run
    val isFirstRun: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val permissionsGranted: Boolean = false,
    
    // Version tracking
    val lastUsedVersion: String = Constants.APP_VERSION,
    val settingsVersion: Int = 1
) {
    /**
     * Validates the current settings and returns a corrected version if needed
     */
    fun validated(): Settings = copy(
        speechSpeed = speechSpeed.coerceIn(Constants.MIN_SPEECH_SPEED, Constants.MAX_SPEECH_SPEED),
        speechPitch = speechPitch.coerceIn(Constants.MIN_SPEECH_PITCH, Constants.MAX_SPEECH_PITCH),
        maxCacheSizeMB = maxCacheSizeMB.coerceAtLeast(10),
        maxTextLength = maxTextLength.coerceIn(100, 10000),
        audioFadeDurationMs = audioFadeDurationMs.coerceIn(0, 1000),
        maxConcurrentSynthesis = maxConcurrentSynthesis.coerceIn(1, 3)
    )
    
    /**
     * Returns true if the settings indicate this is a first-time user
     */
    val isNewUser: Boolean
        get() = isFirstRun && !onboardingCompleted
    
    /**
     * Returns true if the user has completed the basic setup
     */
    val isSetupComplete: Boolean
        get() = onboardingCompleted && permissionsGranted
        
    /**
     * Returns true if accessibility features are enabled
     */
    val hasAccessibilityFeatures: Boolean
        get() = highContrastMode || largeTextMode || reduceMotion || screenReaderOptimized
}

/**
 * Theme mode options
 */
enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default")
}

/**
 * Audio stream types for TTS output
 */
enum class AudioStreamType(val displayName: String, val streamType: Int) {
    MUSIC("Music", 3), // AudioManager.STREAM_MUSIC
    ALARM("Alarm", 4), // AudioManager.STREAM_ALARM
    NOTIFICATION("Notification", 5), // AudioManager.STREAM_NOTIFICATION
    SYSTEM("System", 1), // AudioManager.STREAM_SYSTEM
    VOICE_CALL("Voice Call", 0) // AudioManager.STREAM_VOICE_CALL
}

/**
 * Punctuation handling modes
 */
enum class PunctuationHandling(val displayName: String, val description: String) {
    IGNORE("Ignore", "Skip all punctuation marks"),
    NORMAL("Normal", "Pause briefly for punctuation"),
    ENHANCED("Enhanced", "Speak punctuation names"),
    CUSTOM("Custom", "Use custom punctuation rules")
}

/**
 * Default settings presets
 */
object SettingsPresets {
    
    val accessibility = Settings(
        highContrastMode = true,
        largeTextMode = true,
        reduceMotion = true,
        screenReaderOptimized = true,
        speechSpeed = 0.8f,
        hapticFeedbackEnabled = false,
        animationsEnabled = false,
        punctuationHandling = PunctuationHandling.ENHANCED
    )
    
    val performance = Settings(
        animationsEnabled = false,
        dynamicColorsEnabled = false,
        cacheEnabled = true,
        maxCacheSizeMB = 200,
        useGPUAcceleration = true,
        modelPreloadingEnabled = true,
        backgroundProcessingEnabled = true,
        optimizeForBattery = false
    )
    
    val privacy = Settings(
        analyticsEnabled = false,
        crashReportingEnabled = false,
        usageStatisticsEnabled = false,
        dataCollectionEnabled = false,
        debugMode = false,
        verboseLogging = false
    )
    
    val batteryOptimized = Settings(
        optimizeForBattery = true,
        useGPUAcceleration = false,
        animationsEnabled = false,
        modelPreloadingEnabled = false,
        backgroundProcessingEnabled = false,
        maxConcurrentSynthesis = 1,
        synthesisProgressNotifications = false,
        keepScreenOnDuringSynthesis = false
    )
    
    val developer = Settings(
        debugMode = true,
        verboseLogging = true,
        synthesisProgressNotifications = true,
        showVoiceCharacteristics = true,
        maxConcurrentSynthesis = 2
    )
}

/**
 * Settings validation and migration utilities
 */
object SettingsValidator {
    
    fun validate(settings: Settings): Settings = settings.validated()
    
    fun migrate(settings: Settings, fromVersion: Int, toVersion: Int): Settings {
        var migratedSettings = settings
        
        // Migration logic for different versions
        if (fromVersion < 1 && toVersion >= 1) {
            // Migration from version 0 to 1
            migratedSettings = migratedSettings.copy(
                settingsVersion = 1,
                // Add any migration logic here
            )
        }
        
        return migratedSettings.copy(settingsVersion = toVersion)
    }
    
    fun isValidVoiceId(voiceId: String): Boolean {
        return AllVoices.getVoiceById(voiceId) != null
    }
    
    fun getRecommendedSettings(): Settings {
        return Settings().validated()
    }
}