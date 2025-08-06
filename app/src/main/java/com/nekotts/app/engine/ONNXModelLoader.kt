package com.nekotts.app.engine

import ai.onnxruntime.*
import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ONNXModelLoader(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ONNXModelLoader"
        private const val KOKORO_MODEL_NAME = "kokoro-v1.0.onnx"
        private const val KITTEN_MODEL_NAME = "kitten_tts_nano_v0_1.onnx"
        private const val VOICES_FILE_NAME = "voices-v1.0.bin"
    }
    
    private var ortEnvironment: OrtEnvironment? = null
    private var kokoroSession: OrtSession? = null
    private var kittenSession: OrtSession? = null
    private var voiceEmbeddings: Map<String, FloatArray> = emptyMap()
    
    data class ModelInfo(
        val name: String,
        val version: String,
        val inputNames: List<String>,
        val outputNames: List<String>,
        val inputShapes: Map<String, LongArray>,
        val outputShapes: Map<String, LongArray>
    )
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing ONNX Model Loader")
            
            // Initialize ONNX Runtime environment
            ortEnvironment = OrtEnvironment.getEnvironment()
            
            // Prepare models directory
            val modelsDir = File(context.filesDir, "models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }
            
            // Copy models from assets if needed
            ensureModelsExist(modelsDir)
            
            // Load Kokoro model
            val kokoroModelFile = File(modelsDir, KOKORO_MODEL_NAME)
            if (kokoroModelFile.exists()) {
                kokoroSession = createOptimizedSession(kokoroModelFile.absolutePath, "kokoro")
                Log.d(TAG, "Kokoro model loaded successfully")
            } else {
                Log.w(TAG, "Kokoro model file not found")
            }
            
            // Load Kitten TTS model
            val kittenModelFile = File(modelsDir, KITTEN_MODEL_NAME)
            if (kittenModelFile.exists()) {
                kittenSession = createOptimizedSession(kittenModelFile.absolutePath, "kitten")
                Log.d(TAG, "Kitten TTS model loaded successfully")
            } else {
                Log.w(TAG, "Kitten TTS model file not found")
            }
            
            // Load voice embeddings
            loadVoiceEmbeddings(modelsDir)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ONNX Model Loader", e)
            false
        }
    }
    
    private suspend fun ensureModelsExist(modelsDir: File) = withContext(Dispatchers.IO) {
        val modelFiles = listOf(KOKORO_MODEL_NAME, KITTEN_MODEL_NAME, VOICES_FILE_NAME)
        
        for (modelFile in modelFiles) {
            val targetFile = File(modelsDir, modelFile)
            if (!targetFile.exists()) {
                copyModelFromAssets(modelFile, targetFile)
            }
        }
    }
    
    private suspend fun copyModelFromAssets(fileName: String, targetFile: File) = withContext(Dispatchers.IO) {
        try {
            context.assets.open("models/$fileName").use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Copied $fileName from assets to ${targetFile.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "Could not copy $fileName from assets: ${e.message}")
        }
    }
    
    private fun createOptimizedSession(modelPath: String, modelType: String): OrtSession? {
        return try {
            val sessionOptions = OrtSession.SessionOptions().apply {
                // Optimize for inference
                setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL)
                setInterOpNumThreads(1)
                setIntraOpNumThreads(Math.max(1, Runtime.getRuntime().availableProcessors() - 1))
                
                // Memory optimizations
                setMemoryPatternOptimization(true)
                // setSessionLogSeverityLevel(OrtLoggingLevel.ORT_LOGGING_LEVEL_WARNING) // Method not available
                
                // CPU optimizations
                addCPU(false) // Disable arena-based memory allocation for better memory usage
            }
            
            ortEnvironment?.createSession(modelPath, sessionOptions)?.also {
                Log.d(TAG, "$modelType model session created with optimizations")
                logModelInfo(it, modelType)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create optimized session for $modelType", e)
            null
        }
    }
    
    private fun logModelInfo(session: OrtSession, modelType: String) {
        try {
            val inputInfo = session.inputInfo
            val outputInfo = session.outputInfo
            
            Log.d(TAG, "$modelType model info:")
            Log.d(TAG, "  Input names: ${inputInfo.keys}")
            Log.d(TAG, "  Output names: ${outputInfo.keys}")
            
            inputInfo.forEach { (name, info) ->
                // Log.d(TAG, "  Input '$name': shape=${info.shape?.contentToString()}, type=${info.type}")
            }
            
            outputInfo.forEach { (name, info) ->
                // Log.d(TAG, "  Output '$name': shape=${info.shape?.contentToString()}, type=${info.type}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not log model info for $modelType", e)
        }
    }
    
    private suspend fun loadVoiceEmbeddings(modelsDir: File) = withContext(Dispatchers.IO) {
        val voicesFile = File(modelsDir, VOICES_FILE_NAME)
        if (!voicesFile.exists()) {
            Log.w(TAG, "Voice embeddings file not found")
            return@withContext
        }
        
        try {
            val voiceMap = mutableMapOf<String, FloatArray>()
            
            voicesFile.inputStream().use { inputStream ->
                // Read the binary voice embeddings file
                // Format: [voice_count:int32][voice_id_length:int32][voice_id:utf8][embedding_size:int32][embedding:float32[]]
                val buffer = ByteBuffer.wrap(inputStream.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
                
                val voiceCount = buffer.int
                Log.d(TAG, "Loading $voiceCount voice embeddings")
                
                repeat(voiceCount) {
                    val voiceIdLength = buffer.int
                    val voiceIdBytes = ByteArray(voiceIdLength)
                    buffer.get(voiceIdBytes)
                    val voiceId = String(voiceIdBytes, Charsets.UTF_8)
                    
                    val embeddingSize = buffer.int
                    val embedding = FloatArray(embeddingSize)
                    repeat(embeddingSize) { i ->
                        embedding[i] = buffer.float
                    }
                    
                    voiceMap[voiceId] = embedding
                    Log.v(TAG, "Loaded voice '$voiceId' with embedding size $embeddingSize")
                }
            }
            
            voiceEmbeddings = voiceMap
            Log.d(TAG, "Successfully loaded ${voiceEmbeddings.size} voice embeddings")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load voice embeddings", e)
        }
    }
    
    fun getKokoroSession(): OrtSession? = kokoroSession
    
    fun getKittenSession(): OrtSession? = kittenSession
    
    fun getOrtEnvironment(): OrtEnvironment? = ortEnvironment
    
    fun getVoiceEmbedding(voiceId: String): FloatArray? = voiceEmbeddings[voiceId]
    
    fun getAllVoiceIds(): Set<String> = voiceEmbeddings.keys
    
    fun hasVoiceEmbedding(voiceId: String): Boolean = voiceEmbeddings.containsKey(voiceId)
    
    fun getModelInfo(modelType: String): ModelInfo? {
        val session = when (modelType.lowercase()) {
            "kokoro" -> kokoroSession
            "kitten" -> kittenSession
            else -> null
        } ?: return null
        
        return try {
            val inputInfo = session.inputInfo
            val outputInfo = session.outputInfo
            
            ModelInfo(
                name = modelType,
                version = "1.0",
                inputNames = inputInfo.keys.toList(),
                outputNames = outputInfo.keys.toList(),
                inputShapes = emptyMap(), // inputInfo.mapValues { it.value.shape ?: longArrayOf() },
                outputShapes = emptyMap() // outputInfo.mapValues { it.value.shape ?: longArrayOf() }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get model info for $modelType", e)
            null
        }
    }
    
    fun isModelLoaded(modelType: String): Boolean {
        return when (modelType.lowercase()) {
            "kokoro" -> kokoroSession != null
            "kitten" -> kittenSession != null
            else -> false
        }
    }
    
    fun isInitialized(): Boolean = ortEnvironment != null
    
    fun cleanup() {
        try {
            kokoroSession?.close()
            kittenSession?.close()
            ortEnvironment?.close()
            
            kokoroSession = null
            kittenSession = null
            ortEnvironment = null
            voiceEmbeddings = emptyMap()
            
            Log.d(TAG, "ONNX Model Loader cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    data class InferenceResult(
        val audioData: FloatArray,
        val sampleRate: Int,
        val duration: Float,
        val processingTimeMs: Long
    )
    
    suspend fun runInference(
        session: OrtSession,
        inputs: Map<String, OnnxTensor>,
        outputName: String = "audio"
    ): InferenceResult? = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            
            // Run inference
            val outputs = session.run(inputs)
            val processingTime = System.currentTimeMillis() - startTime
            
            // Extract audio data  
            val audioTensor = outputs[outputName]
            val audioData = if (audioTensor != null) {
                try {
                    // Try to extract tensor value - ONNX Runtime 1.16.3 API
                    when (val tensorValue = (audioTensor as OnnxTensor).floatBuffer.array()) {
                        is FloatArray -> tensorValue
                        else -> {
                            Log.e(TAG, "Unexpected tensor format")
                            null
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting audio from tensor", e)
                    null
                }
            } else {
                Log.e(TAG, "Audio tensor is null")
                null
            }
            
            // Clean up
            outputs.close()
            
            audioData?.let { audio ->
                InferenceResult(
                    audioData = audio,
                    sampleRate = 24000, // Default for Kokoro
                    duration = audio.size / 24000.0f,
                    processingTimeMs = processingTime
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            null
        }
    }
}