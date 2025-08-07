package com.nekotts.app.engine

import ai.onnxruntime.OnnxTensor
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Kokoro TTS synthesizer implementing the complete pipeline from reference implementations
 */
class KokoroSynthesizer(
    private val context: Context,
    private val modelLoader: ONNXModelLoader
) {
    companion object {
        private const val TAG = "KokoroSynthesizer"
        private const val SAMPLE_RATE = 24000
        private const val STYLE_DIM = 256
        private const val MAX_PHONEME_LENGTH = 510
        
        // Complete vocabulary from Kokoro reference implementation
        private val VOCAB: Map<String, Int> by lazy {
            val pad = "$"
            val punctuation = ";:,.!?¡¿—…\"«»\"\" "
            val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
            val lettersIpa = "ɑɐɒæɓʙβɔɕçɗɖðʤəɘɚɛɜɝɞɟʄɡɠɢʛɦɧħɥʜɨɪʝɭɬɫɮʟɱɯɰŋɳɲɴøɵɸθœɶʘɹɺɾɻʀʁɽʂʃʈʧʉʊʋⱱʌɣɤʍχʎʏʑʐʒʔʡʕʢǀǁǂǃˈˌːˑʼʴʰʱʲʷˠˤ˞↓↑→↗↘'̩'ᵻ"
            
            val symbols = mutableListOf<String>()
            symbols.add(pad)
            symbols.addAll(punctuation.map { it.toString() })
            symbols.addAll(letters.map { it.toString() })
            symbols.addAll(lettersIpa.map { it.toString() })
            
            val vocab = mutableMapOf<String, Int>()
            symbols.forEachIndexed { index, symbol ->
                vocab[symbol] = index
            }
            vocab
        }
        
        // English phoneme mappings for basic phonemization
        private val ENGLISH_PHONEME_MAP = mapOf(
            'a' to 'ə',
            'e' to 'ɛ',
            'i' to 'ɪ',
            'o' to "oʊ",
            'u' to 'ʌ',
            "th" to 'θ',
            "sh" to 'ʃ',
            "ch" to "tʃ",
            "ng" to 'ŋ',
            'j' to "dʒ",
            'r' to 'ɹ',
            "er" to 'ɝ',
            "ar" to "ɑɹ",
            "or" to "ɔɹ",
            "ir" to "ɪɹ",
            "ur" to "ʊɹ"
        )
        
        // Common word phoneme mappings
        private val COMMON_WORD_PHONEMES = mapOf(
            "hello" to "hɛˈloʊ",
            "world" to "wˈɝld",
            "this" to "ðˈɪs",
            "is" to "ˈɪz",
            "a" to "ə",
            "test" to "tˈɛst",
            "of" to "ʌv",
            "the" to "ðə",
            "kokoro" to "kˈoʊkəɹoʊ",
            "text" to "tˈɛkst",
            "to" to "tˈuː",
            "speech" to "spˈiːtʃ",
            "system" to "sˈɪstəm",
            "running" to "ɹˈʌnɪŋ",
            "on" to "ˈɑːn",
            "with" to "wˈɪð",
            "and" to "ænd"
        )
    }
    
    data class SynthesisRequest(
        val text: String,
        val voiceId: String,
        val speed: Float = 1.0f
    )
    
    data class SynthesisResult(
        val audioData: FloatArray,
        val sampleRate: Int,
        val duration: Float,
        val processingTimeMs: Long,
        val voiceUsed: String,
        val textLength: Int,
        val tokensProcessed: Int
    )
    
    /**
     * Normalize text for processing (from reference implementation)
     */
    private fun normalizeText(text: String): String {
        var normalized = text.trim()
        
        // Replace multiple spaces with single space
        normalized = normalized.replace(Regex("\\s+"), " ")
        
        // Replace curly quotes with straight quotes
        normalized = normalized.replace(Regex("[\u2018\u2019]"), "'")
        normalized = normalized.replace(Regex("[\u201C\u201D]"), "\"")
        
        // Replace other special characters
        normalized = normalized.replace("…", "...")
        
        return normalized
    }
    
    /**
     * Basic phonemization following Kokoro reference
     */
    private fun phonemize(text: String): String {
        val normalizedText = normalizeText(text)
        val words = normalizedText.split(Regex("\\s+"))
        
        val phonemizedWords = words.map { word ->
            phonemizeWord(word)
        }
        
        return phonemizedWords.joinToString(" ")
    }
    
    /**
     * Phonemize a single word
     */
    private fun phonemizeWord(word: String): String {
        // Check for pre-defined phoneme mapping
        val lowerWord = word.lowercase().replace(Regex("[.,!?;:'\"]*"), "")
        
        COMMON_WORD_PHONEMES[lowerWord]?.let { phoneme ->
            return phoneme
        }
        
        // Simple character-by-character phonemization
        var phonemes = ""
        var i = 0
        
        while (i < word.length) {
            var matched = false
            
            // Check for digraphs first
            if (i < word.length - 1) {
                val digraph = word.substring(i, i + 2).lowercase()
                ENGLISH_PHONEME_MAP[digraph]?.let { phoneme ->
                    phonemes += phoneme
                    i += 2
                    matched = true
                }
            }
            
            if (!matched) {
                // Single character phonemes
                val char = word[i].lowercase().first()
                ENGLISH_PHONEME_MAP[char]?.let { phoneme ->
                    phonemes += phoneme
                } ?: run {
                    when {
                        char.isLetter() -> phonemes += char
                        char in ".,!?;:'\"" -> phonemes += char
                    }
                }
                i++
            }
        }
        
        // Add stress marker for longer words
        if (phonemes.length > 2 && !phonemes.contains(Regex("[.,!?;:'\"]*"))) {
            val firstVowelIndex = phonemes.indexOfFirst { char ->
                char in "ɑɐɒæəɘɚɛɜɝɞɨɪʊʌɔoeiuaɑː"
            }
            if (firstVowelIndex >= 0) {
                phonemes = phonemes.substring(0, firstVowelIndex) + "ˈ" + 
                          phonemes.substring(firstVowelIndex)
            }
        }
        
        return phonemes
    }
    
    /**
     * Tokenize phonemized text using Kokoro vocabulary
     */
    private fun tokenize(text: String): List<Int> {
        // If input is regular text, phonemize it first
        val phonemes = if (!text.contains(Regex("[ɑɐɒæəɘɚɛɜɝɞɨɪʊʌɔˈˌː]"))) {
            phonemize(text)
        } else {
            text
        }
        
        Log.d(TAG, "Phonemized text: $phonemes")
        
        val tokens = mutableListOf<Int>()
        
        // Add start token (0)
        tokens.add(0)
        
        // Convert each character to token
        for (char in phonemes) {
            val charStr = char.toString()
            VOCAB[charStr]?.let { token ->
                tokens.add(token)
            } ?: run {
                Log.w(TAG, "Character not in vocabulary: '$char' (code: ${char.code})")
            }
        }
        
        // Add end token (0)
        tokens.add(0)
        
        return tokens
    }
    
    /**
     * Get voice data for the specified voice ID
     */
    private suspend fun getVoiceData(voiceId: String): FloatArray? = withContext(Dispatchers.IO) {
        return@withContext modelLoader.getVoiceEmbedding(voiceId)
    }
    
    /**
     * Main synthesis function following Kokoro reference implementation
     */
    suspend fun synthesize(request: SynthesisRequest): SynthesisResult? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Kokoro synthesis for voice: ${request.voiceId}")
            val startTime = System.currentTimeMillis()
            
            // Check model availability
            val session = modelLoader.getKokoroSession()
            if (session == null) {
                Log.e(TAG, "Kokoro model session not available")
                return@withContext null
            }
            
            val ortEnv = modelLoader.getEnvironment()
            if (ortEnv == null) {
                Log.e(TAG, "ONNX Runtime environment not available")
                return@withContext null
            }
            
            // 1. Tokenize the input text
            val tokens = tokenize(request.text)
            val numTokens = minOf(maxOf(tokens.size - 2, 0), 509)
            
            Log.d(TAG, "Tokenized text to ${tokens.size} tokens (using $numTokens for voice data)")
            
            // 2. Get voice style data
            val voiceData = getVoiceData(request.voiceId)
            if (voiceData == null) {
                Log.e(TAG, "Voice data not found for: ${request.voiceId}")
                return@withContext null
            }
            
            // Extract style data based on token count (following reference)
            val offset = numTokens * STYLE_DIM
            val styleData = if (offset + STYLE_DIM <= voiceData.size) {
                voiceData.sliceArray(offset until (offset + STYLE_DIM))
            } else {
                // Fallback to beginning of voice data
                voiceData.sliceArray(0 until minOf(STYLE_DIM, voiceData.size))
            }
            
            Log.d(TAG, "Extracted ${styleData.size} style features")
            
            // 3. Prepare input tensors
            val inputs = mutableMapOf<String, OnnxTensor>()
            
            // Input IDs tensor
            try {
                val tokensArray = tokens.map { it.toLong() }.toLongArray()
                inputs["input_ids"] = OnnxTensor.createTensor(
                    ortEnv,
                    java.nio.LongBuffer.wrap(tokensArray),
                    longArrayOf(1, tokens.size.toLong())
                )
            } catch (error: Exception) {
                Log.w(TAG, "Failed to create int64 tensor, trying with int32: $error")
                val tokensArray = tokens.toIntArray()
                inputs["input_ids"] = OnnxTensor.createTensor(
                    ortEnv,
                    java.nio.IntBuffer.wrap(tokensArray),
                    longArrayOf(1, tokens.size.toLong())
                )
            }
            
            // Style tensor
            val styleArray = Array(1) { styleData }
            inputs["style"] = OnnxTensor.createTensor(ortEnv, styleArray)
            
            // Speed tensor
            val speedValue = request.speed.coerceIn(0.5f, 2.0f)
            inputs["speed"] = OnnxTensor.createTensor(
                ortEnv,
                java.nio.FloatBuffer.wrap(floatArrayOf(speedValue)),
                longArrayOf(1)
            )
            
            Log.d(TAG, "Running inference with inputs: tokens=${tokens.size}, style=${styleData.size}, speed=$speedValue")
            
            // 4. Run inference
            val outputs = session.run(inputs)
            
            // Clean up input tensors
            inputs.values.forEach { it.close() }
            
            if (outputs.size() == 0) {
                Log.e(TAG, "No outputs from ONNX session")
                return@withContext null
            }
            
            // 5. Extract waveform output
            val waveformTensor = outputs.get("waveform") ?: outputs.iterator().next().value
            val waveform = when (waveformTensor) {
                is OnnxTensor -> {
                    val tensorData = waveformTensor.floatBuffer
                    val audioArray = FloatArray(tensorData.remaining())
                    tensorData.get(audioArray)
                    audioArray
                }
                else -> {
                    Log.e(TAG, "Unexpected output tensor type: ${waveformTensor.javaClass}")
                    return@withContext null
                }
            }
            
            // Clean up output tensors
            for ((_, tensor) in outputs) {
                tensor.close()
            }
            
            Log.d(TAG, "Generated waveform with ${waveform.size} samples")
            
            val processingTime = System.currentTimeMillis() - startTime
            val duration = waveform.size.toFloat() / SAMPLE_RATE
            
            Log.d(TAG, "Synthesis completed in ${processingTime}ms, duration: ${duration}s")
            
            SynthesisResult(
                audioData = waveform,
                sampleRate = SAMPLE_RATE,
                duration = duration,
                processingTimeMs = processingTime,
                voiceUsed = request.voiceId,
                textLength = request.text.length,
                tokensProcessed = numTokens
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Synthesis failed", e)
            null
        }
    }
    
    /**
     * Convert Float32Array to WAV format (similar to reference implementation)
     */
    fun floatArrayToWav(floatArray: FloatArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
        // Convert float array to Int16Array (16-bit PCM)
        val numSamples = floatArray.size
        val shortArray = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            // Convert float in range [-1, 1] to short in range [-32768, 32767]
            val sample = (floatArray[i] * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            shortArray[i] = sample
        }
        
        // Create WAV header
        val headerLength = 44
        val dataLength = shortArray.size * 2 // 2 bytes per sample
        val totalLength = headerLength + dataLength
        
        val wavData = ByteArray(totalLength)
        
        // WAV file header
        // "RIFF" chunk descriptor
        wavData[0] = 'R'.code.toByte()
        wavData[1] = 'I'.code.toByte()
        wavData[2] = 'F'.code.toByte()
        wavData[3] = 'F'.code.toByte()
        
        // Chunk size (little endian)
        val chunkSize = 36 + dataLength
        wavData[4] = (chunkSize and 0xFF).toByte()
        wavData[5] = ((chunkSize shr 8) and 0xFF).toByte()
        wavData[6] = ((chunkSize shr 16) and 0xFF).toByte()
        wavData[7] = ((chunkSize shr 24) and 0xFF).toByte()
        
        // "WAVE" format
        wavData[8] = 'W'.code.toByte()
        wavData[9] = 'A'.code.toByte()
        wavData[10] = 'V'.code.toByte()
        wavData[11] = 'E'.code.toByte()
        
        // "fmt " subchunk
        wavData[12] = 'f'.code.toByte()
        wavData[13] = 'm'.code.toByte()
        wavData[14] = 't'.code.toByte()
        wavData[15] = ' '.code.toByte()
        
        // Subchunk size (16 for PCM)
        wavData[16] = 16
        wavData[17] = 0
        wavData[18] = 0
        wavData[19] = 0
        
        // Audio format (1 = PCM)
        wavData[20] = 1
        wavData[21] = 0
        
        // Number of channels (1 = mono)
        wavData[22] = 1
        wavData[23] = 0
        
        // Sample rate (little endian)
        wavData[24] = (sampleRate and 0xFF).toByte()
        wavData[25] = ((sampleRate shr 8) and 0xFF).toByte()
        wavData[26] = ((sampleRate shr 16) and 0xFF).toByte()
        wavData[27] = ((sampleRate shr 24) and 0xFF).toByte()
        
        // Byte rate (sample rate * channels * bytes per sample)
        val byteRate = sampleRate * 1 * 2
        wavData[28] = (byteRate and 0xFF).toByte()
        wavData[29] = ((byteRate shr 8) and 0xFF).toByte()
        wavData[30] = ((byteRate shr 16) and 0xFF).toByte()
        wavData[31] = ((byteRate shr 24) and 0xFF).toByte()
        
        // Block align (channels * bytes per sample)
        wavData[32] = 2
        wavData[33] = 0
        
        // Bits per sample
        wavData[34] = 16
        wavData[35] = 0
        
        // "data" subchunk
        wavData[36] = 'd'.code.toByte()
        wavData[37] = 'a'.code.toByte()
        wavData[38] = 't'.code.toByte()
        wavData[39] = 'a'.code.toByte()
        
        // Subchunk size (little endian)
        wavData[40] = (dataLength and 0xFF).toByte()
        wavData[41] = ((dataLength shr 8) and 0xFF).toByte()
        wavData[42] = ((dataLength shr 16) and 0xFF).toByte()
        wavData[43] = ((dataLength shr 24) and 0xFF).toByte()
        
        // Write audio data (little endian)
        for (i in shortArray.indices) {
            val sample = shortArray[i].toInt()
            val byteIndex = headerLength + i * 2
            wavData[byteIndex] = (sample and 0xFF).toByte()
            wavData[byteIndex + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        
        return wavData
    }
    
    /**
     * Get synthesizer info
     */
    fun getSynthesizerInfo(): Map<String, Any> {
        return mapOf(
            "name" to "Kokoro TTS Synthesizer",
            "version" to "1.0",
            "sampleRate" to SAMPLE_RATE,
            "maxPhonemeLength" to MAX_PHONEME_LENGTH,
            "styleDim" to STYLE_DIM,
            "vocabularySize" to VOCAB.size,
            "supportedFeatures" to listOf("speed_control", "voice_styles", "multilingual")
        )
    }
}