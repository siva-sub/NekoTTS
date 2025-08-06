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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Repository for managing TTS voices
 */
class VoiceRepository(
    private val context: Context,
    private val settingsManager: SettingsManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _voices = MutableStateFlow(AllVoices.voices)
    private val _selectedVoice = MutableStateFlow<Voice?>(null)
    private val _downloadingVoices = MutableStateFlow<Set<String>>(emptySet())
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    
    val voices: StateFlow<List<Voice>> = _voices.asStateFlow()
    val selectedVoice: StateFlow<Voice?> = _selectedVoice.asStateFlow()
    val downloadingVoices: StateFlow<Set<String>> = _downloadingVoices.asStateFlow()
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()
    
    init {
        // Initialize with default voice
        _selectedVoice.value = AllVoices.getDefaultVoice()
        
        // Load selected voice from settings
        scope.launch {
            settingsManager.settings.map { it.selectedVoiceId }
                .collect { voiceId ->
                    _selectedVoice.value = AllVoices.getVoiceById(voiceId) ?: AllVoices.getDefaultVoice()
                }
        }
    }
    
    /**
     * Gets all available voices
     */
    fun getAllVoices(): Flow<List<Voice>> = _voices
    
    /**
     * Gets voices filtered by criteria
     */
    fun getFilteredVoices(
        engine: VoiceEngine? = null,
        language: String? = null,
        gender: VoiceGender? = null,
        onlyDownloaded: Boolean = false
    ): Flow<List<Voice>> = _voices.map { voices ->
        voices.filter { voice ->
            (engine == null || voice.engine == engine) &&
            (language == null || voice.language == language) &&
            (gender == null || voice.gender == gender) &&
            (!onlyDownloaded || voice.isDownloaded)
        }
    }
    
    /**
     * Gets downloaded voices only
     */
    fun getDownloadedVoices(): Flow<List<Voice>> = 
        _voices.map { voices -> voices.filter { it.isDownloaded } }
    
    /**
     * Gets voices by engine
     */
    fun getVoicesByEngine(engine: VoiceEngine): Flow<List<Voice>> =
        _voices.map { voices -> voices.filter { it.engine == engine } }
    
    /**
     * Gets voices by language
     */
    fun getVoicesByLanguage(language: String): Flow<List<Voice>> =
        _voices.map { voices -> voices.filter { it.language == language } }
    
    /**
     * Gets available languages
     */
    fun getAvailableLanguages(): Flow<List<String>> =
        _voices.map { voices -> voices.map { it.language }.distinct().sorted() }
    
    /**
     * Gets available engines
     */
    fun getAvailableEngines(): Flow<List<VoiceEngine>> =
        _voices.map { voices -> voices.map { it.engine }.distinct() }
    
    /**
     * Selects a voice and saves to settings
     */
    suspend fun selectVoice(voiceId: String) {
        val voice = AllVoices.getVoiceById(voiceId)
        if (voice != null) {
            _selectedVoice.value = voice
            settingsManager.updateSettings { it.copy(selectedVoiceId = voiceId) }
        }
    }
    
    /**
     * Gets the currently selected voice
     */
    fun getSelectedVoice(): Flow<Voice?> = _selectedVoice
    
    /**
     * Checks if a voice is available (downloaded or built-in)
     */
    fun isVoiceAvailable(voiceId: String): Boolean {
        val voice = AllVoices.getVoiceById(voiceId)
        return voice?.isDownloaded == true
    }
    
    /**
     * Downloads a voice (simulated for Kokoro voices)
     */
    suspend fun downloadVoice(voiceId: String): Result<Unit> {
        return try {
            val voice = AllVoices.getVoiceById(voiceId) ?: return Result.failure(
                IllegalArgumentException("Voice not found: $voiceId")
            )
            
            if (voice.engine == VoiceEngine.KITTEN) {
                // Kitten voices are already built-in
                return Result.success(Unit)
            }
            
            // Add to downloading set
            _downloadingVoices.value = _downloadingVoices.value + voiceId
            _downloadProgress.value = _downloadProgress.value + (voiceId to 0f)
            
            // Simulate download progress
            for (progress in 0..100 step 10) {
                _downloadProgress.value = _downloadProgress.value + (voiceId to progress / 100f)
                kotlinx.coroutines.delay(100) // Simulate download time
            }
            
            // Mark as downloaded
            val updatedVoices = _voices.value.map { v ->
                if (v.id == voiceId) v.copy(isDownloaded = true) else v
            }
            _voices.value = updatedVoices
            
            // Remove from downloading
            _downloadingVoices.value = _downloadingVoices.value - voiceId
            _downloadProgress.value = _downloadProgress.value - voiceId
            
            Result.success(Unit)
        } catch (e: Exception) {
            _downloadingVoices.value = _downloadingVoices.value - voiceId
            _downloadProgress.value = _downloadProgress.value - voiceId
            Result.failure(e)
        }
    }
    
    /**
     * Deletes a downloaded voice
     */
    suspend fun deleteVoice(voiceId: String): Result<Unit> {
        return try {
            val voice = AllVoices.getVoiceById(voiceId) ?: return Result.failure(
                IllegalArgumentException("Voice not found: $voiceId")
            )
            
            if (voice.engine == VoiceEngine.KITTEN) {
                // Can't delete built-in Kitten voices
                return Result.failure(IllegalStateException("Cannot delete built-in voice"))
            }
            
            // Mark as not downloaded
            val updatedVoices = _voices.value.map { v ->
                if (v.id == voiceId) v.copy(isDownloaded = false) else v
            }
            _voices.value = updatedVoices
            
            // If this was the selected voice, switch to default
            if (_selectedVoice.value?.id == voiceId) {
                selectVoice(AllVoices.getDefaultVoice().id)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cancels voice download
     */
    suspend fun cancelDownload(voiceId: String) {
        _downloadingVoices.value = _downloadingVoices.value - voiceId
        _downloadProgress.value = _downloadProgress.value - voiceId
    }
    
    /**
     * Gets the download progress for a voice
     */
    fun getDownloadProgress(voiceId: String): Flow<Float> =
        _downloadProgress.map { it[voiceId] ?: 0f }
    
    /**
     * Checks if a voice is currently downloading
     */
    fun isDownloading(voiceId: String): Flow<Boolean> =
        _downloadingVoices.map { it.contains(voiceId) }
    
    /**
     * Gets voice recommendations based on current selection and usage
     */
    fun getVoiceRecommendations(limit: Int = 3): Flow<List<Voice>> =
        combine(_selectedVoice, _voices) { selected, voices ->
            voices.filter { voice ->
                voice.id != selected?.id && (
                    voice.language == selected?.language ||
                    voice.gender == selected?.gender ||
                    voice.characteristics.intersect(selected?.characteristics ?: emptyList()).isNotEmpty()
                )
            }.take(limit)
        }
    
    /**
     * Searches voices by name or description
     */
    fun searchVoices(query: String): Flow<List<Voice>> =
        _voices.map { voices ->
            if (query.isBlank()) {
                voices
            } else {
                voices.filter { voice ->
                    voice.displayName.contains(query, ignoreCase = true) ||
                    voice.description.contains(query, ignoreCase = true) ||
                    voice.languageName.contains(query, ignoreCase = true) ||
                    voice.characteristics.any { it.displayName.contains(query, ignoreCase = true) }
                }
            }
        }
    
    /**
     * Gets voices grouped by language
     */
    fun getVoicesGroupedByLanguage(): Flow<Map<String, List<Voice>>> =
        _voices.map { voices ->
            voices.groupBy { it.languageName }
        }
    
    /**
     * Gets voices grouped by engine
     */
    fun getVoicesGroupedByEngine(): Flow<Map<VoiceEngine, List<Voice>>> =
        _voices.map { voices ->
            voices.groupBy { it.engine }
        }
    
    /**
     * Refreshes voice data (useful for checking updates)
     */
    suspend fun refreshVoices() {
        // In a real implementation, this would fetch from a server
        _voices.value = AllVoices.voices
    }
    
    /**
     * Gets voice statistics
     */
    fun getVoiceStatistics(): Flow<VoiceStatistics> =
        _voices.map { voices ->
            VoiceStatistics(
                totalVoices = voices.size,
                downloadedVoices = voices.count { it.isDownloaded },
                availableLanguages = voices.map { it.language }.distinct().size,
                kittenVoices = voices.count { it.engine == VoiceEngine.KITTEN },
                kokoroVoices = voices.count { it.engine == VoiceEngine.KOKORO },
                femaleVoices = voices.count { it.gender == VoiceGender.FEMALE },
                maleVoices = voices.count { it.gender == VoiceGender.MALE },
                neutralVoices = voices.count { it.gender == VoiceGender.NEUTRAL }
            )
        }
}

/**
 * Voice repository statistics
 */
data class VoiceStatistics(
    val totalVoices: Int,
    val downloadedVoices: Int,
    val availableLanguages: Int,
    val kittenVoices: Int,
    val kokoroVoices: Int,
    val femaleVoices: Int,
    val maleVoices: Int,
    val neutralVoices: Int
) {
    val downloadPercentage: Float
        get() = if (totalVoices > 0) (downloadedVoices.toFloat() / totalVoices) * 100f else 0f
}