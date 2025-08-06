package com.nekotts.app.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TTSEngine(
    private val context: Context,
    private val modelLoader: ONNXModelLoader,
    private val kokoroEngine: KokoroEngine,
    private val kittenEngine: KittenEngine,
    private val audioProcessor: AudioProcessor,
    private val phonemizer: Phonemizer
) {
    
    companion object {
        private const val TAG = "TTSEngine"
    }
    
    // Unified voice data structure
    data class Voice(
        val id: String,
        val name: String,
        val language: String,
        val gender: String,
        val style: String = "neutral",
        val engine: String, // "kokoro" or "kitten"
        val description: String = ""
    )
    
    // Engine types
    enum class EngineType {
        KOKORO, KITTEN, AUTO
    }
    
    data class SynthesisRequest(
        val text: String,
        val voiceId: String,
        val speed: Float = 1.0f,
        val pitch: Float = 1.0f,
        val emotion: Float = 0.5f,
        val outputFormat: OutputFormat = OutputFormat.FLOAT_ARRAY,
        val language: String? = null,
        val engineType: EngineType = EngineType.AUTO
    )
    
    data class SynthesisResult(
        val audioData: FloatArray,
        val sampleRate: Int,
        val duration: Float,
        val voiceUsed: String,
        val engineUsed: String,
        val processingTimeMs: Long,
        val textLength: Int,
        val format: OutputFormat
    )
    
    enum class OutputFormat {
        FLOAT_ARRAY, WAV_BYTES, PCM_16
    }
    
    interface SynthesisCallback {
        fun onStarted()
        fun onProgress(progress: Int, status: String)
        fun onSuccess(result: SynthesisResult)
        fun onError(error: String)
    }
    
    private var isInitialized = false
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing unified TTS Engine")
            
            // Initialize model loader first
            val modelLoaderInit = modelLoader.initialize()
            if (!modelLoaderInit) {
                Log.e(TAG, "Failed to initialize model loader")
                return@withContext false
            }
            
            // Warmup engines
            val kokoroWarmup = kokoroEngine.warmup()
            val kittenWarmup = kittenEngine.warmup()
            
            isInitialized = kokoroWarmup || kittenWarmup
            
            Log.d(TAG, "TTS Engine initialized: Kokoro=${if (kokoroWarmup) "OK" else "FAIL"}, Kitten=${if (kittenWarmup) "OK" else "FAIL"}")
            
            isInitialized
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TTS Engine", e)
            false
        }
    }
    
    suspend fun synthesize(request: SynthesisRequest, callback: SynthesisCallback? = null): SynthesisResult? = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            Log.e(TAG, "Engine not initialized")
            callback?.onError("Engine not initialized")
            return@withContext null
        }
        
        try {
            callback?.onStarted()
            callback?.onProgress(10, "Determining engine...")
            
            // Determine which engine to use
            val engineType = determineEngine(request.voiceId, request.engineType)
            val voice = getVoice(request.voiceId)
            
            if (voice == null) {
                Log.e(TAG, "Voice not found: ${request.voiceId}")
                callback?.onError("Voice not found: ${request.voiceId}")
                return@withContext null
            }
            
            callback?.onProgress(30, "Preprocessing text...")
            
            // Preprocess text
            val processedText = phonemizer.preprocessTextForTTS(request.text)
            
            val modifiedRequest = request.copy(text = processedText)
            
            callback?.onProgress(50, "Synthesizing audio...")
            
            // Synthesize using appropriate engine
            val result = when (engineType) {
                EngineType.KOKORO -> {
                    val kokoroRequest = KokoroEngine.SynthesisRequest(
                        text = modifiedRequest.text,
                        voiceId = modifiedRequest.voiceId,
                        speed = modifiedRequest.speed,
                        language = modifiedRequest.language
                    )
                    
                    kokoroEngine.synthesize(kokoroRequest)?.let { kokoroResult ->
                        SynthesisResult(
                            audioData = kokoroResult.audioData,
                            sampleRate = kokoroResult.sampleRate,
                            duration = kokoroResult.duration,
                            voiceUsed = modifiedRequest.voiceId,
                            engineUsed = "kokoro",
                            processingTimeMs = kokoroResult.processingTimeMs,
                            textLength = modifiedRequest.text.length,
                            format = OutputFormat.FLOAT_ARRAY
                        )
                    }
                }
                
                EngineType.KITTEN -> {
                    val kittenRequest = KittenEngine.SynthesisRequest(
                        text = modifiedRequest.text,
                        voiceId = modifiedRequest.voiceId,
                        speed = modifiedRequest.speed,
                        pitch = modifiedRequest.pitch,
                        emotion = modifiedRequest.emotion
                    )
                    
                    kittenEngine.synthesize(kittenRequest)?.let { kittenResult ->
                        SynthesisResult(
                            audioData = kittenResult.audioData,
                            sampleRate = kittenResult.sampleRate,
                            duration = kittenResult.duration,
                            voiceUsed = modifiedRequest.voiceId,
                            engineUsed = "kitten",
                            processingTimeMs = kittenResult.processingTimeMs,
                            textLength = modifiedRequest.text.length,
                            format = OutputFormat.FLOAT_ARRAY
                        )
                    }
                }
                
                EngineType.AUTO -> {
                    Log.e(TAG, "AUTO engine type should have been resolved")
                    callback?.onError("Engine type resolution failed")
                    return@withContext null
                }
            }
            
            if (result == null) {
                Log.e(TAG, "Synthesis failed for engine: $engineType")
                callback?.onError("Synthesis failed")
                return@withContext null
            }
            
            callback?.onProgress(80, "Post-processing audio...")
            
            // Apply post-processing
            val processedResult = postProcessResult(result, request)
            
            callback?.onProgress(100, "Complete")
            callback?.onSuccess(processedResult)
            
            processedResult
            
        } catch (e: Exception) {
            Log.e(TAG, "Synthesis failed", e)
            callback?.onError("Synthesis failed: ${e.message}")
            null
        }
    }
    
    private fun determineEngine(voiceId: String, requested: EngineType): EngineType {
        return when (requested) {
            EngineType.AUTO -> {
                when {
                    voiceId.startsWith("ktn_") -> EngineType.KITTEN
                    kokoroEngine.isVoiceSupported(voiceId) -> EngineType.KOKORO
                    kittenEngine.isVoiceSupported(voiceId) -> EngineType.KITTEN
                    else -> EngineType.KOKORO // Default fallback
                }
            }
            else -> requested
        }
    }
    
    private suspend fun postProcessResult(result: SynthesisResult, request: SynthesisRequest): SynthesisResult = withContext(Dispatchers.IO) {
        var audioData = result.audioData
        
        // Apply additional processing if needed
        audioData = audioProcessor.normalizeAudio(audioData)
        audioData = audioProcessor.applyFadeInOut(audioData, 25)
        
        // Convert to requested format
        val finalResult = when (request.outputFormat) {
            OutputFormat.FLOAT_ARRAY -> result.copy(audioData = audioData)
            OutputFormat.WAV_BYTES -> {
                // Convert to WAV bytes (stored as FloatArray for interface compatibility)
                val wavBytes = audioProcessor.convertToWav(audioData, result.sampleRate)
                result.copy(
                    audioData = FloatArray(wavBytes.size) { wavBytes[it].toFloat() },
                    format = OutputFormat.WAV_BYTES
                )
            }
            OutputFormat.PCM_16 -> {
                // Convert to 16-bit PCM (stored as FloatArray for interface compatibility)
                val pcmData = audioData.map { (it * Short.MAX_VALUE).toInt().toShort() }
                result.copy(
                    audioData = FloatArray(pcmData.size) { pcmData[it].toFloat() },
                    format = OutputFormat.PCM_16
                )
            }
        }
        
        finalResult
    }
    
    fun getAllVoices(): List<Voice> {
        val allVoices = mutableListOf<Voice>()
        
        // Add Kokoro voices
        kokoroEngine.getVoices().forEach { kokoroVoice ->
            allVoices.add(Voice(
                id = kokoroVoice.id,
                name = kokoroVoice.name,
                language = kokoroVoice.language,
                gender = kokoroVoice.gender,
                style = "neutral",
                engine = "kokoro",
                description = "${kokoroVoice.name} (${kokoroVoice.country})"
            ))
        }
        
        // Add Kitten voices
        kittenEngine.getVoices().forEach { kittenVoice ->
            allVoices.add(Voice(
                id = kittenVoice.id,
                name = kittenVoice.name,
                language = "en",
                gender = kittenVoice.gender,
                style = kittenVoice.style,
                engine = "kitten",
                description = kittenVoice.description
            ))
        }
        
        return allVoices
    }
    
    fun getVoice(voiceId: String): Voice? = getAllVoices().find { it.id == voiceId }
    
    fun getVoicesByLanguage(language: String): List<Voice> = getAllVoices().filter { it.language == language }
    
    fun getVoicesByEngine(engine: String): List<Voice> = getAllVoices().filter { it.engine == engine }
    
    fun getVoicesByGender(gender: String): List<Voice> = getAllVoices().filter { it.gender.equals(gender, ignoreCase = true) }
    
    fun getSupportedLanguages(): Set<String> = getAllVoices().map { it.language }.toSet()
    
    fun getEngineInfo(): Map<String, Any> {
        return mapOf(
            "name" to "NekoTTS Unified Engine",
            "version" to "1.0",
            "engines" to mapOf(
                "kokoro" to kokoroEngine.getEngineInfo(),
                "kitten" to kittenEngine.getEngineInfo()
            ),
            "totalVoices" to getAllVoices().size,
            "supportedLanguages" to getSupportedLanguages().size,
            "supportedFormats" to OutputFormat.values().map { it.name },
            "features" to listOf(
                "multi_engine",
                "auto_engine_selection",
                "voice_mixing",
                "speed_control",
                "pitch_control",
                "emotion_control",
                "audio_processing",
                "multiple_output_formats"
            ),
            "phonemizer" to phonemizer.getPhonemizationInfo(),
            "audioProcessor" to mapOf(
                "supportedOperations" to listOf(
                    "speed_adjustment", "pitch_shifting", "normalization",
                    "fade_in_out", "silence_trimming", "resampling", "wav_conversion"
                )
            ),
            "initialized" to isInitialized
        )
    }
    
    suspend fun playAudio(audioData: FloatArray, sampleRate: Int = 22050) = withContext(Dispatchers.Main) {
        audioProcessor.playAudio(audioData, sampleRate)
    }
    
    suspend fun saveAudioToFile(audioData: FloatArray, sampleRate: Int, filePath: String) = withContext(Dispatchers.IO) {
        try {
            val wavBytes = audioProcessor.convertToWav(audioData, sampleRate)
            val file = java.io.File(filePath)
            file.writeBytes(wavBytes)
            Log.d(TAG, "Audio saved to: $filePath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save audio", e)
            false
        }
    }
    
    fun getAudioInfo(audioData: FloatArray, sampleRate: Int = 22050): Map<String, Any> {
        return audioProcessor.getAudioInfo(audioData, sampleRate)
    }
    
    suspend fun benchmarkEngines(): Map<String, Map<String, Long>> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, Map<String, Long>>()
        
        try {
            Log.d(TAG, "Running engine benchmarks...")
            
            results["kokoro"] = mapOf("warmup" to measureTime { kokoroEngine.warmup() })
            results["kitten"] = kittenEngine.benchmarkVoices()
            
        } catch (e: Exception) {
            Log.e(TAG, "Benchmark failed", e)
        }
        
        results
    }
    
    private suspend fun measureTime(block: suspend () -> Boolean): Long {
        val startTime = System.currentTimeMillis()
        block()
        return System.currentTimeMillis() - startTime
    }
    
    fun isInitialized(): Boolean = isInitialized
    
    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up TTS Engine")
            
            audioProcessor.cleanup()
            modelLoader.cleanup()
            
            isInitialized = false
            
            Log.d(TAG, "TTS Engine cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}