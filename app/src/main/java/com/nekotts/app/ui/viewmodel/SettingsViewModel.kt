package com.nekotts.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nekotts.app.data.SettingsRepository
import com.nekotts.app.data.models.*
import com.nekotts.app.core.AppSingletons
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    
    private val settingsRepository: SettingsRepository = AppSingletons.getSettingsRepository()
    
    data class UiState(
        val isLoading: Boolean = true,
        val settings: Settings = Settings(),
        val availableEngines: List<VoiceEngine> = VoiceEngine.values().toList(),
        val availableThemes: List<ThemeMode> = ThemeMode.values().toList(),
        val availableAudioStreams: List<AudioStreamType> = AudioStreamType.values().toList(),
        val availablePunctuationHandling: List<PunctuationHandling> = PunctuationHandling.values().toList(),
        val availableLanguages: List<String> = listOf("en", "ja", "es", "fr", "de", "it", "pt", "ru", "zh", "ko"),
        val error: String? = null,
        val isSaving: Boolean = false
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        initialize()
    }
    
    private fun initialize() {
        viewModelScope.launch {
            settingsRepository.getCurrentSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    settings = settings
                )
            }
        }
    }
    
    // Voice Settings
    fun updateSpeechSpeed(speed: Float) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                settingsRepository.setSpeechSpeed(speed)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update speech speed: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
    
    fun updateSpeechPitch(pitch: Float) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                settingsRepository.setSpeechPitch(pitch)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update speech pitch: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
    
    fun updateDefaultEngine(engine: VoiceEngine) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                settingsRepository.setDefaultEngine(engine)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update engine: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
    
    fun updateSelectedVoice(voiceId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                settingsRepository.setSelectedVoice(voiceId)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to select voice: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
    
    // Audio Settings
    fun updateAudioStreamType(streamType: AudioStreamType) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                settingsRepository.setAudioStreamType(streamType)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update audio stream: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
    
    fun toggleAudioFocus() {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                settingsRepository.setAudioFocusEnabled(!currentSettings.audioFocusEnabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle audio focus: ${e.message}"
                )
            }
        }
    }
    
    fun toggleAudioFade() {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                settingsRepository.setAudioFadeEnabled(!currentSettings.audioFadeEnabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle audio fade: ${e.message}"
                )
            }
        }
    }
    
    // UI Settings
    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                settingsRepository.setThemeMode(mode)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update theme: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
    
    fun toggleDynamicColors() {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                settingsRepository.setDynamicColorsEnabled(!currentSettings.dynamicColorsEnabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle dynamic colors: ${e.message}"
                )
            }
        }
    }
    
    fun toggleHapticFeedback() {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                settingsRepository.setHapticFeedbackEnabled(!currentSettings.hapticFeedbackEnabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle haptic feedback: ${e.message}"
                )
            }
        }
    }
    
    fun toggleAnimations() {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                settingsRepository.setAnimationsEnabled(!currentSettings.animationsEnabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle animations: ${e.message}"
                )
            }
        }
    }
    
    // Privacy Settings
    fun toggleAnalytics() {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                settingsRepository.setAnalyticsEnabled(!currentSettings.analyticsEnabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle analytics: ${e.message}"
                )
            }
        }
    }
    
    fun toggleCrashReporting() {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                settingsRepository.setCrashReportingEnabled(!currentSettings.crashReportingEnabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle crash reporting: ${e.message}"
                )
            }
        }
    }
    
    fun toggleDataCollection() {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                settingsRepository.setDataCollectionEnabled(!currentSettings.dataCollectionEnabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle data collection: ${e.message}"
                )
            }
        }
    }
    
    // Accessibility Settings
    fun toggleHighContrast() {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                settingsRepository.setHighContrastMode(!currentSettings.highContrastMode)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle high contrast: ${e.message}"
                )
            }
        }
    }
    
    fun toggleLargeText() {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                settingsRepository.setLargeTextMode(!currentSettings.largeTextMode)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle large text: ${e.message}"
                )
            }
        }
    }
    
    fun toggleReduceMotion() {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                settingsRepository.setReduceMotion(!currentSettings.reduceMotion)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle reduce motion: ${e.message}"
                )
            }
        }
    }
    
    // Performance Settings
    fun toggleGPUAcceleration() {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                settingsRepository.setUseGPUAcceleration(!currentSettings.useGPUAcceleration)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle GPU acceleration: ${e.message}"
                )
            }
        }
    }
    
    fun toggleBatteryOptimization() {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                settingsRepository.setOptimizeForBattery(!currentSettings.optimizeForBattery)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle battery optimization: ${e.message}"
                )
            }
        }
    }
    
    fun updateMaxConcurrentSynthesis(count: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                settingsRepository.setMaxConcurrentSynthesis(count)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update concurrent synthesis: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
    
    // Advanced Settings
    fun toggleDebugMode() {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                settingsRepository.setDebugMode(!currentSettings.debugMode)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle debug mode: ${e.message}"
                )
            }
        }
    }
    
    fun toggleVerboseLogging() {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                settingsRepository.setVerboseLogging(!currentSettings.verboseLogging)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle verbose logging: ${e.message}"
                )
            }
        }
    }
    
    // Preset Actions
    fun applyAccessibilityPreset() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                settingsRepository.applyPreset(SettingsPresets.accessibility)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to apply accessibility preset: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
    
    fun applyPerformancePreset() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                settingsRepository.applyPreset(SettingsPresets.performance)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to apply performance preset: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
    
    fun applyPrivacyPreset() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                settingsRepository.applyPreset(SettingsPresets.privacy)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to apply privacy preset: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
    
    fun applyBatteryOptimizedPreset() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                settingsRepository.applyPreset(SettingsPresets.batteryOptimized)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to apply battery optimized preset: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
    
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                settingsRepository.resetToDefaults()
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to reset settings: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
    
    // Language Settings
    fun updatePreferredLanguage(language: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                settingsRepository.updateSettings { it.copy(preferredLanguage = language) }
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update language: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
    
    // Text Processing Settings
    fun updatePunctuationHandling(handling: PunctuationHandling) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                settingsRepository.updateSettings { it.copy(punctuationHandling = handling) }
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update punctuation handling: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun exportSettings(): String? {
        return try {
            // In a real implementation, this would be a suspend function
            // For now, returning a simple JSON representation
            "Settings export placeholder"
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Export failed: ${e.message}")
            null
        }
    }
    
    fun importSettings(settingsData: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                val result = settingsRepository.importSettings(settingsData)
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Import failed: ${result.exceptionOrNull()?.message}",
                        isSaving = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Import failed: ${e.message}",
                    isSaving = false
                )
            }
        }
    }
}