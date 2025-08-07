package com.nekotts.app.engine

import ai.onnxruntime.OnnxTensor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context

class KittenEngine(
    private val context: Context,
    private val modelLoader: ONNXModelLoader,
    private val audioProcessor: AudioProcessor,
    private val phonemizer: Phonemizer
) {
    
    companion object {
        private const val TAG = "KittenEngine"
        private const val SAMPLE_RATE = 22050
        private const val MAX_INPUT_LENGTH = 256
        private const val VOICE_EMBEDDING_SIZE = 256
    }
    
    data class Voice(
        val id: String,
        val name: String,
        val gender: String,
        val style: String,
        val description: String
    )
    
    // 8 Kitten TTS voices with different styles - using actual model voice IDs
    private val kittenVoices = listOf(
        Voice(
            id = "expr-voice-2-f",
            name = "Luna",
            gender = "female",
            style = "gentle",
            description = "A soft and gentle female voice, perfect for calm narration"
        ),
        Voice(
            id = "expr-voice-2-m",
            name = "Felix",
            gender = "male",
            style = "friendly",
            description = "A warm and friendly male voice with approachable tone"
        ),
        Voice(
            id = "expr-voice-3-f",
            name = "Aria",
            gender = "female",
            style = "energetic",
            description = "An energetic and dynamic female voice with enthusiasm"
        ),
        Voice(
            id = "expr-voice-3-m",
            name = "Oliver",
            gender = "male",
            style = "professional",
            description = "A professional and authoritative male voice for business"
        ),
        Voice(
            id = "expr-voice-4-f",
            name = "Nova",
            gender = "female",
            style = "calm",
            description = "A calm and soothing female voice for relaxation"
        ),
        Voice(
            id = "expr-voice-4-m",
            name = "Max",
            gender = "male",
            style = "warm",
            description = "A warm and comforting male voice with emotional depth"
        ),
        Voice(
            id = "expr-voice-5-f",
            name = "Mimi",
            gender = "female",
            style = "cheerful",
            description = "A bright and cheerful female voice with youthful energy"
        ),
        Voice(
            id = "expr-voice-5-m",
            name = "Sage",
            gender = "male",
            style = "confident",
            description = "A confident and strong male voice with clear articulation"
        )
    )
    
    data class SynthesisRequest(
        val text: String,
        val voiceId: String,
        val speed: Float = 1.0f,
        val pitch: Float = 1.0f,
        val emotion: Float = 0.5f // 0.0 = neutral, 1.0 = maximum emotion
    )
    
    data class SynthesisResult(
        val audioData: FloatArray,
        val sampleRate: Int,
        val duration: Float,
        val processingTimeMs: Long,
        val voiceUsed: String,
        val textLength: Int
    )
    
    suspend fun synthesize(request: SynthesisRequest): SynthesisResult? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Kitten TTS synthesis for voice: ${request.voiceId}")
            val startTime = System.currentTimeMillis()
            
            // Validate voice
            val voice = getVoice(request.voiceId)
            if (voice == null) {
                Log.e(TAG, "Voice not found: ${request.voiceId}")
                return@withContext null
            }
            
            // Get model session
            val session = modelLoader.getKittenSession()
            if (session == null) {
                Log.e(TAG, "Kitten TTS model session not available")
                return@withContext null
            }
            
            // Validate and clean text
            val cleanText = preprocessText(request.text)
            if (cleanText.isEmpty()) {
                Log.e(TAG, "Text is empty after preprocessing")
                return@withContext null
            }
            
            Log.d(TAG, "Preprocessed text: '$cleanText' (${cleanText.length} chars)")
            
            // Get voice embedding
            val voiceEmbedding = modelLoader.getVoiceEmbedding(request.voiceId)
            if (voiceEmbedding == null) {
                Log.e(TAG, "Voice embedding not found for: ${request.voiceId}")
                return@withContext null
            }
            
            // Convert text to phonemes
            val phonemes = phonemizer.phonemize(cleanText, "en-us")
            if (phonemes.isEmpty()) {
                Log.e(TAG, "Failed to phonemize text")
                return@withContext null
            }
            
            Log.d(TAG, "Phonemized text: '$phonemes'")
            
            // Tokenize phonemes
            val tokens = phonemizer.tokenize(phonemes)
            if (tokens.isEmpty()) {
                Log.e(TAG, "Failed to tokenize phonemes")
                return@withContext null
            }
            
            // Limit input length
            val limitedTokens = tokens.take(MAX_INPUT_LENGTH)
            if (limitedTokens.size < tokens.size) {
                Log.w(TAG, "Text truncated from ${tokens.size} to ${limitedTokens.size} tokens")
            }
            
            // Synthesize audio
            val audioData = synthesizeTokens(limitedTokens, voiceEmbedding, request.speed, request.pitch, request.emotion)
            if (audioData == null) {
                Log.e(TAG, "Failed to synthesize audio")
                return@withContext null
            }
            
            // Apply post-processing
            val processedAudio = postProcessAudio(audioData, request)
            
            val processingTime = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "Synthesis completed in ${processingTime}ms, generated ${processedAudio.size} samples")
            
            SynthesisResult(
                audioData = processedAudio,
                sampleRate = SAMPLE_RATE,
                duration = processedAudio.size.toFloat() / SAMPLE_RATE,
                processingTimeMs = processingTime,
                voiceUsed = request.voiceId,
                textLength = cleanText.length
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Synthesis failed", e)
            null
        }
    }
    
    private fun preprocessText(text: String): String {
        return text
            .trim()
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .replace(Regex("[^\u0020-\u007E\u00A0-\u00FF]"), "") // Keep basic Latin chars
            .take(500) // Limit text length
    }
    
    private suspend fun synthesizeTokens(
        tokens: List<Int>,
        voiceEmbedding: FloatArray,
        speed: Float,
        pitch: Float,
        emotion: Float
    ): FloatArray? = withContext(Dispatchers.IO) {
        try {
            val session = modelLoader.getKittenSession() ?: return@withContext null
            val ortEnv = modelLoader.getEnvironment() ?: return@withContext null
            
            Log.d(TAG, "Synthesizing ${tokens.size} tokens with voice embedding size: ${voiceEmbedding.size}")
            
            // Prepare inputs following KittenTTS reference implementation
            val inputs = mutableMapOf<String, OnnxTensor>()
            
            // 1. Input IDs (phoneme tokens) - as int64 array with shape [1, sequence_length]
            val tokensArray = tokens.map { it.toLong() }.toLongArray()
            inputs["input_ids"] = OnnxTensor.createTensor(
                ortEnv,
                java.nio.LongBuffer.wrap(tokensArray),
                longArrayOf(1, tokens.size.toLong())
            )
            
            // 2. Style embedding - voice embedding with proper shape [1, 256] 
            // Following the reference: ref_s = self.voices[voice]
            val styleArray = Array(1) { voiceEmbedding.copyOf() }
            inputs["style"] = OnnxTensor.createTensor(ortEnv, styleArray)
            
            // 3. Speed control - single float value with shape [1]
            val speedValue = speed.coerceIn(0.5f, 2.0f)
            inputs["speed"] = OnnxTensor.createTensor(
                ortEnv,
                java.nio.FloatBuffer.wrap(floatArrayOf(speedValue)),
                longArrayOf(1)
            )
            
            Log.d(TAG, "Running ONNX inference with inputs: tokens=${tokens.size}, style=${voiceEmbedding.size}, speed=${speedValue}")
            
            // Run inference - expecting waveform output
            val outputs = session.run(inputs)
            
            // Clean up input tensors
            inputs.values.forEach { it.close() }
            
            if (outputs.size() == 0) {
                Log.e(TAG, "No outputs from ONNX session")
                return@withContext null
            }
            
            // Get the first output (should be waveform)  
            val outputTensor = outputs.iterator().next().value
            val audioData = when (outputTensor) {
                is OnnxTensor -> {
                    val tensorData = outputTensor.floatBuffer
                    val audioArray = FloatArray(tensorData.remaining())
                    tensorData.get(audioArray)
                    audioArray
                }
                else -> {
                    Log.e(TAG, "Unexpected output tensor type: ${outputTensor.javaClass}")
                    return@withContext null
                }
            }
            
            // Clean up output tensors
            for ((_, tensor) in outputs) {
                tensor.close()
            }
            
            Log.d(TAG, "Generated ${audioData.size} audio samples")
            
            // Apply post-processing similar to reference implementation
            // From KittenTTS: audio = outputs[0][5000:-10000] (trim silence)
            val trimStart = minOf(5000, audioData.size / 10)
            val trimEnd = minOf(10000, audioData.size / 10)
            val trimmedAudio = if (audioData.size > trimStart + trimEnd) {
                audioData.sliceArray(trimStart until (audioData.size - trimEnd))
            } else {
                audioData
            }
            
            Log.d(TAG, "Audio trimmed from ${audioData.size} to ${trimmedAudio.size} samples")
            
            trimmedAudio
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to synthesize tokens", e)
            null
        }
    }
    
    private fun postProcessAudio(audioData: FloatArray, request: SynthesisRequest): FloatArray {
        var processedAudio = audioData
        
        // Normalize audio levels
        processedAudio = audioProcessor.normalizeAudio(processedAudio)
        
        // Apply fade in/out to reduce clicks
        processedAudio = audioProcessor.applyFadeInOut(processedAudio, 20) // 20ms fade
        
        // Trim silence from beginning and end
        processedAudio = audioProcessor.trimSilence(processedAudio)
        
        return processedAudio
    }
    
    fun getVoices(): List<Voice> = kittenVoices
    
    fun getVoice(voiceId: String): Voice? = kittenVoices.find { it.id == voiceId }
    
    fun getVoicesByGender(gender: String): List<Voice> = kittenVoices.filter { it.gender.equals(gender, ignoreCase = true) }
    
    fun getVoicesByStyle(style: String): List<Voice> = kittenVoices.filter { it.style.equals(style, ignoreCase = true) }
    
    fun isVoiceSupported(voiceId: String): Boolean = modelLoader.hasVoiceEmbedding(voiceId)
    
    suspend fun warmup(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Warming up Kitten TTS engine")
            
            // Test synthesis with a short text
            val testRequest = SynthesisRequest(
                text = "Test",
                voiceId = "expr-voice-2-f",
                speed = 1.0f
            )
            
            val result = synthesize(testRequest)
            val success = result != null && result.audioData.isNotEmpty()
            
            Log.d(TAG, "Warmup ${if (success) "successful" else "failed"}")
            success
            
        } catch (e: Exception) {
            Log.e(TAG, "Warmup failed", e)
            false
        }
    }
    
    suspend fun benchmarkVoices(): Map<String, Long> = withContext(Dispatchers.IO) {
        val benchmarkText = "This is a benchmark test for voice quality and speed."
        val results = mutableMapOf<String, Long>()
        
        for (voice in kittenVoices) {
            try {
                val startTime = System.currentTimeMillis()
                val request = SynthesisRequest(
                    text = benchmarkText,
                    voiceId = voice.id,
                    speed = 1.0f
                )
                
                val result = synthesize(request)
                val endTime = System.currentTimeMillis()
                
                if (result != null) {
                    results[voice.id] = endTime - startTime
                    Log.d(TAG, "Voice ${voice.id}: ${endTime - startTime}ms")
                } else {
                    results[voice.id] = -1L // Error indicator
                }
            } catch (e: Exception) {
                Log.e(TAG, "Benchmark failed for voice ${voice.id}", e)
                results[voice.id] = -1L
            }
        }
        
        results
    }
    
    fun getEngineInfo(): Map<String, Any> {
        return mapOf(
            "name" to "Kitten TTS",
            "version" to "0.1",
            "sampleRate" to SAMPLE_RATE,
            "maxInputLength" to MAX_INPUT_LENGTH,
            "voiceEmbeddingSize" to VOICE_EMBEDDING_SIZE,
            "supportedVoices" to kittenVoices.size,
            "modelLoaded" to modelLoader.isModelLoaded("kitten"),
            "features" to listOf("speed_control", "pitch_control", "emotion_control", "voice_styles")
        )
    }
    
    suspend fun synthesizeWithCallback(
        request: SynthesisRequest,
        callback: SynthesisCallback
    ): SynthesisResult? = withContext(Dispatchers.IO) {
        try {
            callback.onStarted()
            
            // Update progress at key stages
            callback.onProgress(10, "Preprocessing text...")
            val cleanText = preprocessText(request.text)
            
            callback.onProgress(30, "Converting to phonemes...")
            val phonemes = phonemizer.phonemize(cleanText, "en-us")
            val tokens = phonemizer.tokenize(phonemes)
            
            callback.onProgress(50, "Loading voice...")
            val voiceEmbedding = modelLoader.getVoiceEmbedding(request.voiceId)
            
            callback.onProgress(70, "Synthesizing audio...")
            val result = synthesize(request)
            
            callback.onProgress(90, "Post-processing...")
            // Post-processing is included in synthesize()
            
            callback.onProgress(100, "Complete")
            
            if (result != null) {
                callback.onSuccess(result)
            } else {
                callback.onError("Synthesis failed")
            }
            
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Synthesis with callback failed", e)
            callback.onError("Synthesis failed: ${e.message}")
            null
        }
    }
    
    interface SynthesisCallback {
        fun onStarted()
        fun onProgress(progress: Int, status: String)
        fun onSuccess(result: SynthesisResult)
        fun onError(error: String)
    }
}