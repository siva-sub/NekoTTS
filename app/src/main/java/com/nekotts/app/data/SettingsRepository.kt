package com.nekotts.app.data

import android.content.Context
import com.nekotts.app.data.models.*
import com.nekotts.app.data.preferences.SettingsManager
import com.nekotts.app.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Repository for managing application settings
 */
class SettingsRepository(
    private val context: Context,
    private val settingsManager: SettingsManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _currentSettings = MutableStateFlow(Settings())
    
    val settings: StateFlow<Settings> = _currentSettings.asStateFlow()
    
    init {
        // Load settings from preferences
        scope.launch {
            settingsManager.settings.collect { settings ->
                _currentSettings.value = settings.validated()
            }
        }
    }
    
    /**
     * Gets current settings
     */
    fun getCurrentSettings(): Flow<Settings> = settings
    
    /**
     * Updates settings
     */
    suspend fun updateSettings(update: (Settings) -> Settings) {
        val currentSettings = _currentSettings.value
        val newSettings = update(currentSettings).validated()
        _currentSettings.value = newSettings
        settingsManager.updateSettings { newSettings }
    }
    
    /**
     * Resets settings to default
     */
    suspend fun resetToDefaults() {
        val defaultSettings = Settings().validated()
        _currentSettings.value = defaultSettings
        settingsManager.updateSettings { defaultSettings }
    }
    
    /**
     * Applies a settings preset
     */
    suspend fun applyPreset(preset: Settings) {
        val validatedPreset = preset.validated()
        _currentSettings.value = validatedPreset
        settingsManager.updateSettings { validatedPreset }
    }
    
    // Voice Settings
    suspend fun setSelectedVoice(voiceId: String) {
        if (SettingsValidator.isValidVoiceId(voiceId)) {
            updateSettings { it.copy(selectedVoiceId = voiceId) }
        }
    }
    
    suspend fun setSpeechSpeed(speed: Float) {
        val validSpeed = speed.coerceIn(Constants.MIN_SPEECH_SPEED, Constants.MAX_SPEECH_SPEED)
        updateSettings { it.copy(speechSpeed = validSpeed) }
    }
    
    suspend fun setSpeechPitch(pitch: Float) {
        val validPitch = pitch.coerceIn(Constants.MIN_SPEECH_PITCH, Constants.MAX_SPEECH_PITCH)
        updateSettings { it.copy(speechPitch = validPitch) }
    }
    
    suspend fun setDefaultEngine(engine: VoiceEngine) {
        updateSettings { it.copy(defaultEngine = engine) }
    }
    
    // Audio Settings
    suspend fun setAudioStreamType(streamType: AudioStreamType) {
        updateSettings { it.copy(audioStreamType = streamType) }
    }
    
    suspend fun setAudioFocusEnabled(enabled: Boolean) {
        updateSettings { it.copy(audioFocusEnabled = enabled) }
    }
    
    suspend fun setAudioFadeEnabled(enabled: Boolean) {
        updateSettings { it.copy(audioFadeEnabled = enabled) }
    }
    
    // UI Settings
    suspend fun setThemeMode(mode: ThemeMode) {
        updateSettings { it.copy(themeMode = mode) }
    }
    
    suspend fun setDynamicColorsEnabled(enabled: Boolean) {
        updateSettings { it.copy(dynamicColorsEnabled = enabled) }
    }
    
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        updateSettings { it.copy(hapticFeedbackEnabled = enabled) }
    }
    
    suspend fun setAnimationsEnabled(enabled: Boolean) {
        updateSettings { it.copy(animationsEnabled = enabled) }
    }
    
    // Privacy Settings
    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        updateSettings { it.copy(analyticsEnabled = enabled) }
    }
    
    suspend fun setCrashReportingEnabled(enabled: Boolean) {
        updateSettings { it.copy(crashReportingEnabled = enabled) }
    }
    
    suspend fun setDataCollectionEnabled(enabled: Boolean) {
        updateSettings { it.copy(dataCollectionEnabled = enabled) }
    }
    
    // Accessibility Settings
    suspend fun setHighContrastMode(enabled: Boolean) {
        updateSettings { it.copy(highContrastMode = enabled) }
    }
    
    suspend fun setLargeTextMode(enabled: Boolean) {
        updateSettings { it.copy(largeTextMode = enabled) }
    }
    
    suspend fun setReduceMotion(enabled: Boolean) {
        updateSettings { it.copy(reduceMotion = enabled) }
    }
    
    // Performance Settings
    suspend fun setUseGPUAcceleration(enabled: Boolean) {
        updateSettings { it.copy(useGPUAcceleration = enabled) }
    }
    
    suspend fun setOptimizeForBattery(enabled: Boolean) {
        updateSettings { it.copy(optimizeForBattery = enabled) }
    }
    
    suspend fun setMaxConcurrentSynthesis(count: Int) {
        val validCount = count.coerceIn(1, 3)
        updateSettings { it.copy(maxConcurrentSynthesis = validCount) }
    }
    
    // Advanced Settings
    suspend fun setDebugMode(enabled: Boolean) {
        updateSettings { it.copy(debugMode = enabled) }
    }
    
    suspend fun setVerboseLogging(enabled: Boolean) {
        updateSettings { it.copy(verboseLogging = enabled) }
    }
    
    // First Run Settings
    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        updateSettings { it.copy(onboardingCompleted = completed, isFirstRun = false) }
    }
    
    suspend fun setPermissionsGranted(granted: Boolean = true) {
        updateSettings { it.copy(permissionsGranted = granted) }
    }
    
    /**
     * Gets specific setting values as flows
     */
    fun getSelectedVoiceId(): Flow<String> = settings.map { it.selectedVoiceId }
    fun getSpeechSpeed(): Flow<Float> = settings.map { it.speechSpeed }
    fun getSpeechPitch(): Flow<Float> = settings.map { it.speechPitch }
    fun getThemeMode(): Flow<ThemeMode> = settings.map { it.themeMode }
    fun isDynamicColorsEnabled(): Flow<Boolean> = settings.map { it.dynamicColorsEnabled }
    fun isHapticFeedbackEnabled(): Flow<Boolean> = settings.map { it.hapticFeedbackEnabled }
    fun isAnimationsEnabled(): Flow<Boolean> = settings.map { it.animationsEnabled }
    fun isAnalyticsEnabled(): Flow<Boolean> = settings.map { it.analyticsEnabled }
    fun isOnboardingCompleted(): Flow<Boolean> = settings.map { it.onboardingCompleted }
    fun isFirstRun(): Flow<Boolean> = settings.map { it.isFirstRun }
    
    /**
     * Exports settings to a string (for backup/sharing)
     */
    suspend fun exportSettings(): String {
        val currentSettings = settings.first()
        return settingsManager.exportSettingsAsString(currentSettings)
    }
    
    /**
     * Imports settings from a string (for restore)
     */
    suspend fun importSettings(settingsString: String): Result<Unit> {
        return try {
            val importedSettings = settingsManager.importSettingsFromString(settingsString)
            val validatedSettings = importedSettings.validated()
            _currentSettings.value = validatedSettings
            settingsManager.updateSettings { validatedSettings }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets settings optimized for accessibility
     */
    fun getAccessibilitySettings(): Settings {
        return SettingsPresets.accessibility
    }
    
    /**
     * Gets settings optimized for performance
     */
    fun getPerformanceSettings(): Settings {
        return SettingsPresets.performance
    }
    
    /**
     * Gets settings optimized for battery life
     */
    fun getBatteryOptimizedSettings(): Settings {
        return SettingsPresets.batteryOptimized
    }
    
    /**
     * Gets privacy-focused settings
     */
    fun getPrivacySettings(): Settings {
        return SettingsPresets.privacy
    }
    
    /**
     * Validates current settings and fixes any issues
     */
    suspend fun validateAndFixSettings() {
        updateSettings { it.validated() }
    }
    
    /**
     * Migrates settings from older versions
     */
    suspend fun migrateIfNeeded() {
        val currentSettings = settings.first()
        val currentVersion = currentSettings.settingsVersion
        val targetVersion = 1 // Current settings version
        
        if (currentVersion < targetVersion) {
            val migratedSettings = SettingsValidator.migrate(currentSettings, currentVersion, targetVersion)
            _currentSettings.value = migratedSettings
            settingsManager.updateSettings { migratedSettings }
        }
    }
    
    /**
     * Gets recommended settings based on device capabilities
     */
    suspend fun getRecommendedSettings(): Settings {
        // In a real implementation, this would analyze device capabilities
        return SettingsValidator.getRecommendedSettings()
    }
    
    /**
     * Gets settings summary for display
     */
    fun getSettingsSummary(): Flow<SettingsSummary> = settings.map { settings ->
        SettingsSummary(
            selectedVoice = AllVoices.getVoiceById(settings.selectedVoiceId)?.displayName ?: "Unknown",
            speechSpeed = settings.speechSpeed,
            speechPitch = settings.speechPitch,
            themeMode = settings.themeMode.displayName,
            isDynamicColors = settings.dynamicColorsEnabled,
            isHapticFeedback = settings.hapticFeedbackEnabled,
            isAnalytics = settings.analyticsEnabled,
            isAccessibilityEnabled = settings.hasAccessibilityFeatures,
            isFirstRun = settings.isFirstRun,
            settingsVersion = settings.settingsVersion
        )
    }
}

/**
 * Settings summary for display purposes
 */
data class SettingsSummary(
    val selectedVoice: String,
    val speechSpeed: Float,
    val speechPitch: Float,
    val themeMode: String,
    val isDynamicColors: Boolean,
    val isHapticFeedback: Boolean,
    val isAnalytics: Boolean,
    val isAccessibilityEnabled: Boolean,
    val isFirstRun: Boolean,
    val settingsVersion: Int
)