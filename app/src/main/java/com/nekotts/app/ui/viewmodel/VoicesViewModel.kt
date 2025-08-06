package com.nekotts.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nekotts.app.data.VoiceRepository
import com.nekotts.app.data.VoiceStatistics
import com.nekotts.app.data.SettingsRepository
import com.nekotts.app.data.models.*
import com.nekotts.app.core.AppSingletons
import com.nekotts.app.service.TTSSessionManager
import com.nekotts.app.service.TTSSession
import com.nekotts.app.service.SessionPriority
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VoicesViewModel : ViewModel() {
    
    private val voiceRepository: VoiceRepository = AppSingletons.getVoiceRepository()
    private val settingsRepository: SettingsRepository = AppSingletons.getSettingsRepository()
    private val ttsSessionManager: TTSSessionManager = AppSingletons.getTTSSessionManager()

    data class UiState(
        val isLoading: Boolean = true,
        val voices: List<Voice> = emptyList(),
        val filteredVoices: List<Voice> = emptyList(),
        val selectedVoice: Voice? = null,
        val downloadedVoices: List<Voice> = emptyList(),
        val downloadingVoices: Set<String> = emptySet(),
        val downloadProgress: Map<String, Float> = emptyMap(),
        val searchQuery: String = "",
        val selectedEngine: VoiceEngine? = null,
        val selectedLanguage: String? = null,
        val selectedGender: VoiceGender? = null,
        val onlyDownloaded: Boolean = false,
        val sortBy: VoiceSortOption = VoiceSortOption.NAME,
        val viewMode: ViewMode = ViewMode.GRID,
        val availableLanguages: List<String> = emptyList(),
        val availableEngines: List<VoiceEngine> = emptyList(),
        val voiceStatistics: VoiceStatistics? = null,
        val error: String? = null,
        val isPreviewPlaying: Boolean = false,
        val previewingVoiceId: String? = null,
        val showFilters: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            try {
                // Combine all voice-related data flows
                combine(
                    voiceRepository.getAllVoices(),
                    voiceRepository.getSelectedVoice(),
                    voiceRepository.getDownloadedVoices(),
                    voiceRepository.downloadingVoices,
                    voiceRepository.downloadProgress,
                    voiceRepository.getAvailableLanguages(),
                    voiceRepository.getAvailableEngines(),
                    voiceRepository.getVoiceStatistics(),
                    ttsSessionManager.activeSessionsFlow
                ) { flows ->
                    val voices = flows[0] as List<Voice>
                    val selectedVoice = flows[1] as Voice?
                    val downloadedVoices = flows[2] as List<Voice>
                    val downloading = flows[3] as Set<String>
                    val progress = flows[4] as Map<String, Float>
                    val languages = flows[5] as List<String>
                    val engines = flows[6] as List<VoiceEngine>
                    val statistics = flows[7] as VoiceStatistics?
                    val activeSessions = flows[8] as List<TTSSession>
                    
                    val previewSession = activeSessions.find { session ->
                        session.priority == SessionPriority.NORMAL
                    }
                    
                    _uiState.value.copy(
                        isLoading = false,
                        voices = voices,
                        selectedVoice = selectedVoice,
                        downloadedVoices = downloadedVoices,
                        downloadingVoices = downloading,
                        downloadProgress = progress,
                        availableLanguages = languages,
                        availableEngines = engines,
                        voiceStatistics = statistics,
                        isPreviewPlaying = previewSession?.isActive ?: false,
                        previewingVoiceId = if (previewSession?.isActive == true) {
                            previewSession.voiceId
                        } else null
                    )
                }.collect { newState ->
                    _uiState.value = newState
                    applyFilters()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load voices"
                )
            }
        }
    }

    private fun applyFilters() {
        val currentState = _uiState.value
        var filteredVoices = currentState.voices

        // Apply search filter
        if (currentState.searchQuery.isNotBlank()) {
            filteredVoices = filteredVoices.filter { voice ->
                voice.displayName.contains(currentState.searchQuery, ignoreCase = true) ||
                voice.description.contains(currentState.searchQuery, ignoreCase = true) ||
                voice.languageName.contains(currentState.searchQuery, ignoreCase = true) ||
                voice.characteristics.any { 
                    it.displayName.contains(currentState.searchQuery, ignoreCase = true) 
                }
            }
        }

        // Apply engine filter
        currentState.selectedEngine?.let { engine ->
            filteredVoices = filteredVoices.filter { it.engine == engine }
        }

        // Apply language filter
        currentState.selectedLanguage?.let { language ->
            filteredVoices = filteredVoices.filter { it.language == language }
        }

        // Apply gender filter
        currentState.selectedGender?.let { gender ->
            filteredVoices = filteredVoices.filter { it.gender == gender }
        }

        // Apply downloaded filter
        if (currentState.onlyDownloaded) {
            filteredVoices = filteredVoices.filter { it.isDownloaded }
        }

        // Apply sorting
        filteredVoices = when (currentState.sortBy) {
            VoiceSortOption.NAME -> filteredVoices.sortedBy { it.displayName }
            VoiceSortOption.LANGUAGE -> filteredVoices.sortedBy { it.languageName }
            VoiceSortOption.GENDER -> filteredVoices.sortedBy { it.gender.displayName }
            VoiceSortOption.ENGINE -> filteredVoices.sortedBy { it.engine.displayName }
            VoiceSortOption.QUALITY -> filteredVoices.sortedByDescending { it.quality.ordinal }
            VoiceSortOption.DOWNLOADED_FIRST -> {
                filteredVoices.sortedWith(
                    compareByDescending<Voice> { it.isDownloaded }
                        .thenBy { it.displayName }
                )
            }
        }

        _uiState.value = _uiState.value.copy(filteredVoices = filteredVoices)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun selectEngine(engine: VoiceEngine?) {
        _uiState.value = _uiState.value.copy(selectedEngine = engine)
        applyFilters()
    }

    fun selectLanguage(language: String?) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
        applyFilters()
    }

    fun selectGender(gender: VoiceGender?) {
        _uiState.value = _uiState.value.copy(selectedGender = gender)
        applyFilters()
    }

    fun toggleOnlyDownloaded() {
        _uiState.value = _uiState.value.copy(onlyDownloaded = !_uiState.value.onlyDownloaded)
        applyFilters()
    }

    fun setSortOption(sortBy: VoiceSortOption) {
        _uiState.value = _uiState.value.copy(sortBy = sortBy)
        applyFilters()
    }

    fun setViewMode(viewMode: ViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = viewMode)
    }

    fun toggleFilters() {
        _uiState.value = _uiState.value.copy(showFilters = !_uiState.value.showFilters)
    }

    fun selectVoice(voiceId: String) {
        viewModelScope.launch {
            try {
                voiceRepository.selectVoice(voiceId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to select voice: ${e.message}"
                )
            }
        }
    }

    fun downloadVoice(voiceId: String) {
        viewModelScope.launch {
            try {
                val result = voiceRepository.downloadVoice(voiceId)
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Download failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Download failed: ${e.message}"
                )
            }
        }
    }

    fun deleteVoice(voiceId: String) {
        viewModelScope.launch {
            try {
                val result = voiceRepository.deleteVoice(voiceId)
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Delete failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Delete failed: ${e.message}"
                )
            }
        }
    }

    fun cancelDownload(voiceId: String) {
        viewModelScope.launch {
            try {
                voiceRepository.cancelDownload(voiceId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Cancel failed: ${e.message}"
                )
            }
        }
    }

    fun previewVoice(voiceId: String) {
        viewModelScope.launch {
            try {
                // Stop any current preview
                stopPreview()

                val voice = AllVoices.getVoiceById(voiceId)
                if (voice == null) {
                    _uiState.value = _uiState.value.copy(error = "Voice not found")
                    return@launch
                }

                val testText = getPreviewText(voice)
                val settings = settingsRepository.getCurrentSettings().first()

                // Create preview session
                val session = ttsSessionManager.createSession(
                    text = testText,
                    voiceId = voiceId,
                    speed = settings.speechSpeed,
                    pitch = settings.speechPitch,
                    priority = SessionPriority.NORMAL
                )

                val result = ttsSessionManager.startSession(session.id)
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Preview failed: ${result.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Preview failed: ${e.message}"
                )
            }
        }
    }

    fun stopPreview() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.isPreviewPlaying && currentState.previewingVoiceId != null) {
                // Find and stop the preview session
                val activeSessions = ttsSessionManager.getActiveSessions()
                val previewSession = activeSessions.find { session ->
                    session.voiceId == currentState.previewingVoiceId &&
                    session.priority == SessionPriority.NORMAL
                }
                previewSession?.let { session ->
                    ttsSessionManager.stopSession(session.id)
                }
            }
        }
    }

    fun refreshVoices() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                voiceRepository.refreshVoices()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Refresh failed: ${e.message}"
                )
            }
        }
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            selectedEngine = null,
            selectedLanguage = null,
            selectedGender = null,
            onlyDownloaded = false
        )
        applyFilters()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun getPreviewText(voice: Voice): String {
        return when (voice.language) {
            "en" -> "Hello! This is ${voice.displayName}. I'm a ${voice.gender.displayName.lowercase()} voice with ${voice.characteristics.joinToString(" and ") { it.displayName.lowercase() }} characteristics."
            "ja" -> "こんにちは！私は${voice.displayName}です。よろしくお願いします。"
            "es" -> "¡Hola! Soy ${voice.displayName}. Encantado de conocerte."
            "fr" -> "Bonjour! Je suis ${voice.displayName}. Enchanté de vous rencontrer."
            "de" -> "Hallo! Ich bin ${voice.displayName}. Freut mich, Sie kennenzulernen."
            "it" -> "Ciao! Sono ${voice.displayName}. Piacere di conoscerti."
            "pt" -> "Olá! Eu sou ${voice.displayName}. Prazer em conhecê-lo."
            "ru" -> "Привет! Меня зовут ${voice.displayName}. Приятно познакомиться."
            "zh" -> "你好！我是${voice.displayName}。很高兴见到你。"
            "ko" -> "안녕하세요! 저는 ${voice.displayName}입니다. 만나서 반갑습니다."
            else -> "Hello! This is ${voice.displayName}. Nice to meet you!"
        }
    }

    fun getVoicesByEngine(engine: VoiceEngine): List<Voice> {
        return _uiState.value.voices.filter { it.engine == engine }
    }

    fun getVoicesByLanguage(language: String): List<Voice> {
        return _uiState.value.voices.filter { it.language == language }
    }

    fun getDownloadedVoicesCount(): Int {
        return _uiState.value.downloadedVoices.size
    }

    fun getTotalVoicesCount(): Int {
        return _uiState.value.voices.size
    }

    fun isVoiceDownloading(voiceId: String): Boolean {
        return _uiState.value.downloadingVoices.contains(voiceId)
    }

    fun getDownloadProgress(voiceId: String): Float {
        return _uiState.value.downloadProgress[voiceId] ?: 0f
    }

    fun isVoiceSelected(voiceId: String): Boolean {
        return _uiState.value.selectedVoice?.id == voiceId
    }

    fun isVoicePreviewing(voiceId: String): Boolean {
        return _uiState.value.previewingVoiceId == voiceId
    }
}

/**
 * Voice sorting options
 */
enum class VoiceSortOption(val displayName: String) {
    NAME("Name"),
    LANGUAGE("Language"),
    GENDER("Gender"),
    ENGINE("Engine"),
    QUALITY("Quality"),
    DOWNLOADED_FIRST("Downloaded First")
}

/**
 * View mode options
 */
enum class ViewMode {
    GRID,
    LIST
}