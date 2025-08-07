package com.nekotts.app.service

import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.nekotts.app.core.AppSingletons
import com.nekotts.app.engine.AudioProcessor
import com.nekotts.app.engine.TTSEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class NekoTTSService : TextToSpeechService() {
    
    private lateinit var ttsEngine: TTSEngine
    private lateinit var audioProcessor: AudioProcessor
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        private const val TAG = "NekoTTSService"
        private val SUPPORTED_LANGUAGES = setOf(
            "af", "am", "ar", "as", "az", "be", "bg", "bn", "br", "bs", "ca", "cs", "cy", 
            "da", "de", "el", "en", "eo", "es", "et", "eu", "fa", "fi", "fr", "ga", "gl", 
            "gu", "ha", "he", "hi", "hr", "hu", "hy", "id", "is", "it", "ja", "jv", "ka", 
            "kk", "km", "kn", "ko", "la", "lb", "lg", "ln", "lo", "lt", "lv", "mg", "mk", 
            "ml", "mn", "mr", "ms", "mt", "my", "ne", "nl", "nn", "no", "oc", "pa", "pl", 
            "ps", "pt", "ro", "ru", "sk", "sl", "sn", "so", "sq", "sr", "su", "sv", "sw", 
            "ta", "te", "tg", "th", "tk", "tr", "tt", "uk", "ur", "uz", "vi", "yo", "zh"
        )
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NekoTTS Service created")
        
        // Initialize dependencies from singleton manager
        ttsEngine = AppSingletons.getTTSEngine()
        audioProcessor = AppSingletons.getAudioProcessor()
        
        serviceScope.launch {
            val initialized = ttsEngine.initialize()
            Log.d(TAG, "TTS Engine initialization: $initialized")
        }
    }
    
    override fun onGetDefaultVoiceNameFor(lang: String, country: String, variant: String): String {
        Log.d(TAG, "Getting default voice for: $lang-$country-$variant")
        
        // Return appropriate default voice based on language
        return when (lang) {
            "en" -> "expr-voice-2-f" // Default to Kitten TTS for English (updated voice ID)
            else -> lang // Use language code as voice ID for Kokoro voices
        }
    }
    
    override fun onGetLanguage(): Array<String> {
        Log.d(TAG, "Getting supported language")
        return arrayOf("en", "US", "") // Default to English US
    }
    
    override fun onGetVoices(): List<android.speech.tts.Voice> {
        Log.d(TAG, "Getting available voices")
        
        val voices = mutableListOf<android.speech.tts.Voice>()
        
        ttsEngine.getAllVoices().forEach { voice ->
            val locale = when (voice.language) {
                "en" -> Locale.ENGLISH
                "es" -> Locale("es")
                "fr" -> Locale.FRENCH
                "de" -> Locale.GERMAN
                "it" -> Locale.ITALIAN
                "pt" -> Locale("pt")
                "ru" -> Locale("ru")
                "ja" -> Locale.JAPANESE
                "ko" -> Locale.KOREAN
                "zh" -> Locale.CHINESE
                else -> Locale(voice.language)
            }
            
            val features = mutableSetOf<String>()
            features.add("quality_high")
            features.add("latency_normal")
            
            val ttsVoice = android.speech.tts.Voice(
                voice.id,
                locale,
                if (voice.gender == "male") 200 else 100, // Gender constants
                300, // Quality high
                false, // Not network required
                features
            )
            
            voices.add(ttsVoice)
        }
        
        return voices
    }
    
    override fun onIsLanguageAvailable(lang: String, country: String, variant: String): Int {
        Log.d(TAG, "Checking language availability: $lang-$country-$variant")
        
        return when {
            SUPPORTED_LANGUAGES.contains(lang) -> {
                if (lang == "en" && country == "US") {
                    TextToSpeech.LANG_COUNTRY_AVAILABLE
                } else {
                    TextToSpeech.LANG_AVAILABLE
                }
            }
            else -> TextToSpeech.LANG_NOT_SUPPORTED
        }
    }
    
    override fun onLoadLanguage(lang: String, country: String, variant: String): Int {
        Log.d(TAG, "Loading language: $lang-$country-$variant")
        return onIsLanguageAvailable(lang, country, variant)
    }
    
    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        Log.d(TAG, "Synthesizing text: ${request.charSequenceText}")
        
        if (!ttsEngine.isInitialized()) {
            Log.e(TAG, "TTS Engine not initialized")
            callback.error()
            return
        }
        
        val text = request.charSequenceText?.toString() ?: ""
        if (text.isEmpty()) {
            Log.w(TAG, "Empty text provided")
            callback.done()
            return
        }
        
        // Get voice from request
        val voiceName = request.voiceName ?: onGetDefaultVoiceNameFor(
            request.language, request.country, request.variant
        )
        
        // Get speech rate (speed)
        val speed = (request.speechRate / 100f).coerceIn(0.5f, 2.0f)
        
        Log.d(TAG, "Using voice: $voiceName, speed: $speed")
        
        serviceScope.launch {
            try {
                // Start synthesis
                callback.start(22050, AudioManager.STREAM_MUSIC, 1) // Sample rate, stream, channel count
                
                val request = TTSEngine.SynthesisRequest(
                    text = text,
                    voiceId = voiceName,
                    speed = speed
                )
                
                val synthesisResult = ttsEngine.synthesize(request, object : TTSEngine.SynthesisCallback {
                    override fun onStarted() {
                        Log.d(TAG, "Synthesis started")
                    }
                    
                    override fun onProgress(progress: Int, status: String) {
                        Log.d(TAG, "Synthesis progress: $progress% - $status")
                    }
                    
                    override fun onSuccess(result: TTSEngine.SynthesisResult) {
                        Log.d(TAG, "Synthesis completed successfully")
                    }
                    
                    override fun onError(error: String) {
                        Log.e(TAG, "Synthesis error: $error")
                        callback.error()
                    }
                })
                
                if (synthesisResult != null) {
                    // Process audio
                    val processedAudio = audioProcessor.applyFadeInOut(
                        audioProcessor.normalizeAudio(synthesisResult.audioData)
                    )
                    
                    // Convert to WAV format for callback
                    val wavData = audioProcessor.convertToWav(processedAudio)
                    
                    withContext(Dispatchers.Main) {
                        // Send audio data to callback
                        val maxBufferSize = 8192
                        var offset = 44 // Skip WAV header
                        
                        while (offset < wavData.size) {
                            val chunkSize = minOf(maxBufferSize, wavData.size - offset)
                            val chunk = wavData.copyOfRange(offset, offset + chunkSize)
                            
                            if (callback.audioAvailable(chunk, 0, chunk.size) != TextToSpeech.SUCCESS) {
                                Log.e(TAG, "Error sending audio data")
                                break
                            }
                            
                            offset += chunkSize
                        }
                        
                        callback.done()
                    }
                } else {
                    Log.e(TAG, "Synthesis returned null audio data")
                    callback.error()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during synthesis", e)
                callback.error()
            }
        }
    }
    
    override fun onStop() {
        Log.d(TAG, "TTS synthesis stopped")
        // Cancel any ongoing synthesis
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "NekoTTS Service destroyed")
        
        // Cleanup resources
        ttsEngine.cleanup()
        audioProcessor.cleanup()
    }
}