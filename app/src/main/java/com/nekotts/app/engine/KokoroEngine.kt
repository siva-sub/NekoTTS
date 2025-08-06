package com.nekotts.app.engine

import ai.onnxruntime.OnnxTensor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context

class KokoroEngine(
    private val context: Context,
    private val modelLoader: ONNXModelLoader,
    private val phonemizer: Phonemizer,
    private val audioProcessor: AudioProcessor
) {
    
    companion object {
        private const val TAG = "KokoroEngine"
        private const val SAMPLE_RATE = 24000
        private const val MODEL_CONTEXT_WINDOW = 512
        private const val TOKENS_PER_CHUNK = MODEL_CONTEXT_WINDOW - 2 // Leave space for start/end tokens
        private const val VOICE_EMBEDDING_SIZE = 256
    }
    
    data class Voice(
        val id: String,
        val name: String,
        val language: String,
        val country: String,
        val gender: String
    )
    
    // All 54 supported Kokoro voices
    private val kokoroVoices = listOf(
        Voice("af", "Afrikaans Female", "af", "ZA", "female"),
        Voice("am", "Amharic Female", "am", "ET", "female"),
        Voice("ar", "Arabic Male", "ar", "SA", "male"),
        Voice("as", "Assamese Female", "as", "IN", "female"),
        Voice("az", "Azerbaijani Male", "az", "AZ", "male"),
        Voice("be", "Belarusian Female", "be", "BY", "female"),
        Voice("bg", "Bulgarian Female", "bg", "BG", "female"),
        Voice("bn", "Bengali Female", "bn", "BD", "female"),
        Voice("br", "Breton Male", "br", "FR", "male"),
        Voice("bs", "Bosnian Female", "bs", "BA", "female"),
        Voice("ca", "Catalan Female", "ca", "ES", "female"),
        Voice("cs", "Czech Female", "cs", "CZ", "female"),
        Voice("cy", "Welsh Female", "cy", "GB", "female"),
        Voice("da", "Danish Female", "da", "DK", "female"),
        Voice("de", "German Female", "de", "DE", "female"),
        Voice("el", "Greek Female", "el", "GR", "female"),
        Voice("en", "English Female", "en", "US", "female"),
        Voice("eo", "Esperanto Female", "eo", "XX", "female"),
        Voice("es", "Spanish Female", "es", "ES", "female"),
        Voice("et", "Estonian Female", "et", "EE", "female"),
        Voice("eu", "Basque Female", "eu", "ES", "female"),
        Voice("fa", "Persian Female", "fa", "IR", "female"),
        Voice("fi", "Finnish Female", "fi", "FI", "female"),
        Voice("fr", "French Female", "fr", "FR", "female"),
        Voice("ga", "Irish Female", "ga", "IE", "female"),
        Voice("gl", "Galician Female", "gl", "ES", "female"),
        Voice("gu", "Gujarati Female", "gu", "IN", "female"),
        Voice("ha", "Hausa Female", "ha", "NG", "female"),
        Voice("he", "Hebrew Female", "he", "IL", "female"),
        Voice("hi", "Hindi Female", "hi", "IN", "female"),
        Voice("hr", "Croatian Female", "hr", "HR", "female"),
        Voice("hu", "Hungarian Female", "hu", "HU", "female"),
        Voice("hy", "Armenian Female", "hy", "AM", "female"),
        Voice("id", "Indonesian Female", "id", "ID", "female"),
        Voice("is", "Icelandic Male", "is", "IS", "male"),
        Voice("it", "Italian Female", "it", "IT", "female"),
        Voice("ja", "Japanese Female", "ja", "JP", "female"),
        Voice("jv", "Javanese Male", "jv", "ID", "male"),
        Voice("ka", "Georgian Female", "ka", "GE", "female"),
        Voice("kk", "Kazakh Female", "kk", "KZ", "female"),
        Voice("km", "Khmer Female", "km", "KH", "female"),
        Voice("kn", "Kannada Female", "kn", "IN", "female"),
        Voice("ko", "Korean Female", "ko", "KR", "female"),
        Voice("la", "Latin Female", "la", "VA", "female"),
        Voice("lb", "Luxembourgish Female", "lb", "LU", "female"),
        Voice("lg", "Luganda Female", "lg", "UG", "female"),
        Voice("ln", "Lingala Female", "ln", "CD", "female"),
        Voice("lo", "Lao Female", "lo", "LA", "female"),
        Voice("lt", "Lithuanian Female", "lt", "LT", "female"),
        Voice("lv", "Latvian Female", "lv", "LV", "female"),
        Voice("mg", "Malagasy Female", "mg", "MG", "female"),
        Voice("mk", "Macedonian Female", "mk", "MK", "female"),
        Voice("ml", "Malayalam Female", "ml", "IN", "female"),
        Voice("mn", "Mongolian Female", "mn", "MN", "female"),
        Voice("mr", "Marathi Female", "mr", "IN", "female"),
        Voice("ms", "Malay Female", "ms", "MY", "female"),
        Voice("mt", "Maltese Female", "mt", "MT", "female"),
        Voice("my", "Myanmar Female", "my", "MM", "female"),
        Voice("ne", "Nepali Female", "ne", "NP", "female"),
        Voice("nl", "Dutch Female", "nl", "NL", "female"),
        Voice("nn", "Norwegian Nynorsk Female", "nn", "NO", "female"),
        Voice("no", "Norwegian Female", "no", "NO", "female"),
        Voice("oc", "Occitan Female", "oc", "FR", "female"),
        Voice("pa", "Punjabi Female", "pa", "IN", "female"),
        Voice("pl", "Polish Female", "pl", "PL", "female"),
        Voice("ps", "Pashto Female", "ps", "AF", "female"),
        Voice("pt", "Portuguese Female", "pt", "BR", "female"),
        Voice("ro", "Romanian Female", "ro", "RO", "female"),
        Voice("ru", "Russian Female", "ru", "RU", "female"),
        Voice("sk", "Slovak Female", "sk", "SK", "female"),
        Voice("sl", "Slovenian Female", "sl", "SI", "female"),
        Voice("sn", "Shona Female", "sn", "ZW", "female"),
        Voice("so", "Somali Female", "so", "SO", "female"),
        Voice("sq", "Albanian Female", "sq", "AL", "female"),
        Voice("sr", "Serbian Female", "sr", "RS", "female"),
        Voice("su", "Sundanese Male", "su", "ID", "male"),
        Voice("sv", "Swedish Female", "sv", "SE", "female"),
        Voice("sw", "Swahili Female", "sw", "TZ", "female"),
        Voice("ta", "Tamil Female", "ta", "IN", "female"),
        Voice("te", "Telugu Female", "te", "IN", "female"),
        Voice("tg", "Tajik Female", "tg", "TJ", "female"),
        Voice("th", "Thai Female", "th", "TH", "female"),
        Voice("tk", "Turkmen Female", "tk", "TM", "female"),
        Voice("tr", "Turkish Female", "tr", "TR", "female"),
        Voice("tt", "Tatar Female", "tt", "RU", "female"),
        Voice("uk", "Ukrainian Female", "uk", "UA", "female"),
        Voice("ur", "Urdu Female", "ur", "PK", "female"),
        Voice("uz", "Uzbek Female", "uz", "UZ", "female"),
        Voice("vi", "Vietnamese Female", "vi", "VN", "female"),
        Voice("yo", "Yoruba Female", "yo", "NG", "female"),
        Voice("zh", "Chinese Female", "zh", "CN", "female")
    )
    
    data class TextChunk(
        val type: String, // "text" or "silence"
        val content: String,
        val tokens: List<Int>? = null,
        val durationSeconds: Float = 0f
    )
    
    data class SynthesisRequest(
        val text: String,
        val voiceId: String,
        val speed: Float = 1.0f,
        val language: String? = null
    )
    
    data class SynthesisResult(
        val audioData: FloatArray,
        val sampleRate: Int,
        val duration: Float,
        val chunks: Int,
        val processingTimeMs: Long
    )
    
    suspend fun synthesize(request: SynthesisRequest): SynthesisResult? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Kokoro synthesis for voice: ${request.voiceId}")
            val startTime = System.currentTimeMillis()
            
            // Validate voice
            val voice = getVoice(request.voiceId)
            if (voice == null) {
                Log.e(TAG, "Voice not found: ${request.voiceId}")
                return@withContext null
            }
            
            // Get model session
            val session = modelLoader.getKokoroSession()
            if (session == null) {
                Log.e(TAG, "Kokoro model session not available")
                return@withContext null
            }
            
            // Get voice embedding
            val voiceEmbedding = modelLoader.getVoiceEmbedding(request.voiceId)
            if (voiceEmbedding == null) {
                Log.e(TAG, "Voice embedding not found for: ${request.voiceId}")
                return@withContext null
            }
            
            // Preprocess text into chunks
            val language = request.language ?: voice.language
            val chunks = preprocessText(request.text, language)
            
            Log.d(TAG, "Text preprocessed into ${chunks.size} chunks")
            
            // Synthesize each chunk
            val waveforms = mutableListOf<FloatArray>()
            var totalSamples = 0
            
            for ((index, chunk) in chunks.withIndex()) {
                Log.d(TAG, "Processing chunk $index: ${chunk.type}")
                
                val chunkAudio = when (chunk.type) {
                    "silence" -> {
                        val silenceLength = (chunk.durationSeconds * SAMPLE_RATE).toInt()
                        Log.d(TAG, "Creating silence: ${chunk.durationSeconds}s ($silenceLength samples)")
                        FloatArray(silenceLength) // Silence
                    }
                    "text" -> {
                        if (chunk.tokens == null || chunk.tokens.isEmpty()) {
                            Log.w(TAG, "Skipping chunk with no tokens")
                            continue
                        }
                        synthesizeTextChunk(chunk.tokens, voiceEmbedding)
                    }
                    else -> {
                        Log.w(TAG, "Unknown chunk type: ${chunk.type}")
                        continue
                    }
                } ?: continue
                
                waveforms.add(chunkAudio)
                totalSamples += chunkAudio.size
            }
            
            if (waveforms.isEmpty()) {
                Log.e(TAG, "No audio generated from any chunks")
                return@withContext null
            }
            
            // Concatenate all waveforms
            val finalAudio = FloatArray(totalSamples)
            var offset = 0
            
            for (waveform in waveforms) {
                System.arraycopy(waveform, 0, finalAudio, offset, waveform.size)
                offset += waveform.size
            }
            
            // Apply speed adjustment if needed
            val processedAudio = if (request.speed != 1.0f) {
                audioProcessor.changeSpeed(finalAudio, request.speed)
            } else {
                finalAudio
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "Synthesis completed in ${processingTime}ms, generated ${processedAudio.size} samples")
            
            SynthesisResult(
                audioData = processedAudio,
                sampleRate = SAMPLE_RATE,
                duration = processedAudio.size.toFloat() / SAMPLE_RATE,
                chunks = chunks.size,
                processingTimeMs = processingTime
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Synthesis failed", e)
            null
        }
    }
    
    private suspend fun preprocessText(text: String, language: String): List<TextChunk> = withContext(Dispatchers.IO) {
        val chunks = mutableListOf<TextChunk>()
        
        // Split text by silence markers and punctuation
        val segments = text.split(Regex("\\s*[.!?]+\\s*|\\s*<break\\s+time=\"([0-9.]+)s\"\\s*/?>\\s*"))
        
        for (segment in segments) {
            when {
                segment.trim().isEmpty() -> continue
                
                segment.contains("<break") -> {
                    // Extract break duration
                    val breakMatch = Regex("<break\\s+time=\"([0-9.]+)s\"\\s*/?>").find(segment)
                    val duration = breakMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0.5f
                    chunks.add(TextChunk("silence", "", null, duration))
                }
                
                else -> {
                    // Phonemize text segment
                    val phonemes = phonemizer.phonemize(segment.trim(), language)
                    if (phonemes.isNotEmpty()) {
                        val tokens = phonemizer.tokenize(phonemes)
                        if (tokens.isNotEmpty()) {
                            // Split into model-sized chunks if needed
                            val tokenChunks = tokens.chunked(TOKENS_PER_CHUNK)
                            for (tokenChunk in tokenChunks) {
                                chunks.add(TextChunk("text", segment, tokenChunk))
                            }
                        }
                    }
                }
            }
        }
        
        // Add default pause between sentences if none specified
        if (chunks.size > 1) {
            val result = mutableListOf<TextChunk>()
            for (i in chunks.indices) {
                result.add(chunks[i])
                if (i < chunks.size - 1 && chunks[i].type == "text" && chunks[i + 1].type == "text") {
                    result.add(TextChunk("silence", "", null, 0.2f))
                }
            }
            result
        } else {
            chunks
        }
    }
    
    private suspend fun synthesizeTextChunk(tokens: List<Int>, voiceEmbedding: FloatArray): FloatArray? = withContext(Dispatchers.IO) {
        try {
            val session = modelLoader.getKokoroSession() ?: return@withContext null
            val ortEnv = modelLoader.getOrtEnvironment() ?: return@withContext null
            
            // Prepare input tensors
            val inputs = mutableMapOf<String, OnnxTensor>()
            
            // Add padding tokens (start and end)
            val paddedTokens = listOf(0) + tokens + listOf(0)
            val tokensArray = paddedTokens.map { it.toLong() }.toLongArray()
            
            // Input IDs tensor - use buffer
            val tokensBuffer = java.nio.LongBuffer.wrap(tokensArray)
            inputs["input_ids"] = OnnxTensor.createTensor(
                ortEnv,
                tokensBuffer,
                longArrayOf(1, paddedTokens.size.toLong())
            )
            
            // Voice style/embedding tensor
            // Kokoro expects voice embeddings with shape [510, 1, 256] for full context
            // We need to replicate the embedding for each token position
            val contextLength = 510
            val styleEmbedding = Array(contextLength) { Array(1) { voiceEmbedding } }
            inputs["style"] = OnnxTensor.createTensor(ortEnv, styleEmbedding)
            
            // Speed tensor (fixed at 1.0 for model, speed adjustment done in post-processing)
            val speedBuffer = java.nio.FloatBuffer.wrap(floatArrayOf(1.0f))
            inputs["speed"] = OnnxTensor.createTensor(
                ortEnv,
                speedBuffer,
                longArrayOf(1)
            )
            
            // Run inference
            val result = modelLoader.runInference(session, inputs, "audio")
            
            // Clean up input tensors
            inputs.values.forEach { it.close() }
            
            result?.audioData
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to synthesize text chunk", e)
            null
        }
    }
    
    fun getVoices(): List<Voice> = kokoroVoices
    
    fun getVoice(voiceId: String): Voice? = kokoroVoices.find { it.id == voiceId }
    
    fun getVoicesByLanguage(language: String): List<Voice> = kokoroVoices.filter { it.language == language }
    
    fun getSupportedLanguages(): Set<String> = kokoroVoices.map { it.language }.toSet()
    
    fun isVoiceSupported(voiceId: String): Boolean = modelLoader.hasVoiceEmbedding(voiceId)
    
    suspend fun warmup(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Warming up Kokoro engine")
            
            // Test synthesis with a short text
            val testRequest = SynthesisRequest(
                text = "Hello",
                voiceId = "en",
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
    
    fun getEngineInfo(): Map<String, Any> {
        return mapOf(
            "name" to "Kokoro TTS",
            "version" to "1.0",
            "sampleRate" to SAMPLE_RATE,
            "contextWindow" to MODEL_CONTEXT_WINDOW,
            "voiceEmbeddingSize" to VOICE_EMBEDDING_SIZE,
            "supportedVoices" to kokoroVoices.size,
            "supportedLanguages" to getSupportedLanguages().size,
            "modelLoaded" to modelLoader.isModelLoaded("kokoro")
        )
    }
}