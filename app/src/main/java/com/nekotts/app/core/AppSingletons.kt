package com.nekotts.app.core

import android.content.Context
import com.nekotts.app.data.SettingsRepository
import com.nekotts.app.data.VoiceRepository
import com.nekotts.app.data.preferences.SettingsManager
import com.nekotts.app.engine.AudioProcessor
import com.nekotts.app.engine.KittenEngine
import com.nekotts.app.engine.KokoroEngine
import com.nekotts.app.engine.KokoroSynthesizer
import com.nekotts.app.engine.ModelDownloader
import com.nekotts.app.engine.ONNXModelLoader
import com.nekotts.app.engine.Phonemizer
import com.nekotts.app.engine.TTSEngine
import com.nekotts.app.service.TTSSessionManager

/**
 * Simple singleton manager to replace Hilt dependency injection
 */
object AppSingletons {
    
    private var applicationContext: Context? = null
    
    // Settings
    private val _settingsManager: SettingsManager by lazy {
        SettingsManager(requireContext())
    }
    
    private val _settingsRepository: SettingsRepository by lazy {
        SettingsRepository(requireContext(), _settingsManager)
    }
    
    // Voice Repository
    private val _voiceRepository: VoiceRepository by lazy {
        VoiceRepository(requireContext(), _settingsManager)
    }
    
    // Audio & Engine Components
    private val _audioProcessor: AudioProcessor by lazy {
        AudioProcessor()
    }
    
    private val _phonemizer: Phonemizer by lazy {
        Phonemizer(requireContext())
    }
    
    private val _onnxModelLoader: ONNXModelLoader by lazy {
        ONNXModelLoader(requireContext())
    }
    
    private val _kokoroEngine: KokoroEngine by lazy {
        KokoroEngine(requireContext(), _onnxModelLoader, _phonemizer, _audioProcessor)
    }
    
    private val _kittenEngine: KittenEngine by lazy {
        KittenEngine(requireContext(), _onnxModelLoader, _audioProcessor, _phonemizer)
    }
    
    private val _kokoroSynthesizer: KokoroSynthesizer by lazy {
        KokoroSynthesizer(requireContext(), _onnxModelLoader)
    }
    
    private val _modelDownloader: ModelDownloader by lazy {
        ModelDownloader(requireContext())
    }
    
    private val _ttsEngine: TTSEngine by lazy {
        TTSEngine(
            context = requireContext(),
            modelLoader = _onnxModelLoader,
            kokoroEngine = _kokoroEngine,
            kittenEngine = _kittenEngine,
            audioProcessor = _audioProcessor,
            phonemizer = _phonemizer
        )
    }
    
    // Session Manager
    private val _ttsSessionManager: TTSSessionManager by lazy {
        TTSSessionManager(requireContext())
    }
    
    /**
     * Initialize with application context
     */
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
    
    private fun requireContext(): Context {
        return applicationContext 
            ?: throw IllegalStateException("AppSingletons not initialized. Call init() first.")
    }
    
    /**
     * Get the application context
     */
    fun getContext(): Context = requireContext()
    
    // Public accessors
    fun getSettingsManager(): SettingsManager = _settingsManager
    fun getSettingsRepository(): SettingsRepository = _settingsRepository
    fun getVoiceRepository(): VoiceRepository = _voiceRepository
    fun getAudioProcessor(): AudioProcessor = _audioProcessor
    fun getPhonemizer(): Phonemizer = _phonemizer
    fun getONNXModelLoader(): ONNXModelLoader = _onnxModelLoader
    fun getKokoroEngine(): KokoroEngine = _kokoroEngine
    fun getKittenEngine(): KittenEngine = _kittenEngine
    fun getKokoroSynthesizer(): KokoroSynthesizer = _kokoroSynthesizer
    fun getModelDownloader(): ModelDownloader = _modelDownloader
    fun getTTSEngine(): TTSEngine = _ttsEngine
    fun getTTSSessionManager(): TTSSessionManager = _ttsSessionManager
}