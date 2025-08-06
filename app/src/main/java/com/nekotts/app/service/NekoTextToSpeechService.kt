package com.nekotts.app.service

import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeechService
import android.speech.tts.TextToSpeech
import android.util.Log
import com.nekotts.app.core.AppSingletons
import com.nekotts.app.data.SettingsRepository
import com.nekotts.app.data.VoiceRepository
import com.nekotts.app.data.models.AllVoices
import com.nekotts.app.service.TTSSessionManager
import com.nekotts.app.service.SessionStatus
import com.nekotts.app.service.SessionPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Android TTS Service implementation for Neko TTS
 * This is the main service that integrates with Android's TTS framework
 */
class NekoTextToSpeechService : TextToSpeechService() {
    
    companion object {
        private const val TAG = "NekoTTSService"
        private val SUPPORTED_LANGUAGES = setOf(
            "en", "en-US", "en-GB", 
            "es", "es-ES", 
            "fr", "fr-FR", 
            "de", "de-DE", 
            "it", "it-IT", 
            "pt", "pt-PT", 
            "ja", "ja-JP",
            "ko", "ko-KR",
            "zh", "zh-CN",
            "ru", "ru-RU",
            "ar", "ar-SA",
            "af", "am", "bg", "bn", "br", "bs", "ca", "cs", "cy",
            "da", "el", "eo", "et", "eu", "fa", "fi", "ga", "gl",
            "gu", "ha", "he", "hi", "hr", "hu", "hy", "id", "is",
            "jv", "ka", "kk", "km", "kn", "la", "lb", "lg", "ln",
            "lo", "lt", "lv", "mg", "mk", "ml", "mn", "mr", "ms",
            "mt", "my", "ne", "nl", "nn", "no", "oc", "pa", "pl",
            "ps", "ro", "sk", "sl", "sn", "so", "sq", "sr", "su",
            "sv", "sw", "ta", "te", "tg", "th", "tk", "tr", "tt",
            "uk", "ur", "uz", "vi", "yo"
        )
    }
    
    private lateinit var serviceScope: CoroutineScope
    private lateinit var ttsSessionManager: TTSSessionManager
    private lateinit var voiceRepository: VoiceRepository
    private lateinit var settingsRepository: SettingsRepository
    private var isInitialized = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NekoTextToSpeechService created")
        
        try {
            serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            
            // Initialize dependencies
            AppSingletons.init(applicationContext)
            ttsSessionManager = AppSingletons.getTTSSessionManager()
            voiceRepository = AppSingletons.getVoiceRepository()
            settingsRepository = AppSingletons.getSettingsRepository()
            
            isInitialized = true
            Log.d(TAG, "NekoTextToSpeechService initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize NekoTextToSpeechService", e)
            isInitialized = false
        }
    }
    
    override fun onGetLanguage(): Array<String> {
        Log.d(TAG, "onGetLanguage called")
        // Return default language (English US)
        return arrayOf("en", "US", "")
    }
    
    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        Log.d(TAG, "onIsLanguageAvailable: $lang-$country-$variant")
        
        if (!isInitialized) {
            Log.w(TAG, "Service not initialized")
            return TextToSpeech.LANG_NOT_SUPPORTED
        }
        
        val languageCode = buildString {
            if (!lang.isNullOrEmpty()) {
                append(lang.lowercase())
                if (!country.isNullOrEmpty()) {
                    append("-")
                    append(country.uppercase())
                }
            }
        }
        
        return when {
            languageCode.isEmpty() -> {
                Log.d(TAG, "Empty language code, defaulting to English")
                TextToSpeech.LANG_AVAILABLE
            }
            SUPPORTED_LANGUAGES.contains(languageCode) -> {
                Log.d(TAG, "Language $languageCode is supported")
                TextToSpeech.LANG_AVAILABLE
            }
            SUPPORTED_LANGUAGES.contains(lang?.lowercase()) -> {
                Log.d(TAG, "Language $lang is supported (without country)")
                TextToSpeech.LANG_AVAILABLE
            }
            languageCode.startsWith("en") -> {
                Log.d(TAG, "English variant $languageCode supported")
                TextToSpeech.LANG_AVAILABLE
            }
            else -> {
                Log.w(TAG, "Language $languageCode not supported")
                TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }
    
    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        Log.d(TAG, "onLoadLanguage: $lang-$country-$variant")
        
        if (!isInitialized) {
            Log.e(TAG, "Service not initialized")
            return TextToSpeech.LANG_MISSING_DATA
        }
        
        val result = onIsLanguageAvailable(lang, country, variant)
        
        return if (result == TextToSpeech.LANG_AVAILABLE) {
            Log.d(TAG, "Language loaded successfully")
            TextToSpeech.LANG_AVAILABLE
        } else {
            Log.w(TAG, "Language data missing")
            TextToSpeech.LANG_MISSING_DATA
        }
    }
    
    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        Log.d(TAG, "onSynthesizeText: '${request.charSequenceText?.toString()?.take(50)}...' (${request.charSequenceText?.length} chars)")
        
        if (!isInitialized) {
            Log.e(TAG, "Service not initialized")
            callback.error()
            return
        }
        
        if (request.charSequenceText.isNullOrEmpty()) {
            Log.w(TAG, "Empty text provided for synthesis")
            callback.error()
            return
        }
        
        val text = request.charSequenceText.toString().trim()
        if (text.isEmpty()) {
            Log.w(TAG, "Text is empty after trimming")
            callback.error()
            return
        }
        
        if (text.length > 10000) {
            Log.w(TAG, "Text too long: ${text.length} characters, truncating")
        }
        
        val finalText = text.take(10000) // Limit text length
        
        serviceScope.launch {
            try {
                synthesizeTextAsync(finalText, request, callback)
            } catch (e: Exception) {
                Log.e(TAG, "Error during synthesis", e)
                callback.error()
            }
        }
    }
    
    private suspend fun synthesizeTextAsync(
        text: String,
        request: SynthesisRequest,
        callback: SynthesisCallback
    ) {
        try {
            Log.d(TAG, "Starting async synthesis for: '${text.take(50)}...'")
            
            // Get current settings and voice
            val settings = settingsRepository.getCurrentSettings().first()
            val selectedVoice = try {
                voiceRepository.getSelectedVoice().first() ?: AllVoices.getDefaultVoice()
            } catch (e: Exception) {
                Log.w(TAG, "Error getting selected voice, using default", e)
                AllVoices.getDefaultVoice()
            }
            
            Log.d(TAG, "Using voice: ${selectedVoice.displayName}, speed: ${settings.speechSpeed}")
            
            // Initialize synthesis callback
            val audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT
            val sampleRate = 24000
            val channels = 1
            
            callback.start(audioFormat, sampleRate, channels)
            Log.d(TAG, "TTS synthesis started with format=$audioFormat, rate=$sampleRate")
            
            // Create TTS session with high priority for system requests
            val session = ttsSessionManager.createSession(
                text = text,
                voiceId = selectedVoice.id,
                speed = settings.speechSpeed,
                pitch = settings.speechPitch,
                priority = SessionPriority.HIGH
            )
            
            Log.d(TAG, "Created TTS session: ${session.id}")
            
            // Start synthesis
            val result = ttsSessionManager.startSession(session.id)
            
            if (result.isFailure) {
                Log.e(TAG, "Failed to start TTS session", result.exceptionOrNull())
                callback.error()
                return
            }
            
            // Monitor session progress and stream audio
            var sessionCompleted = false
            var lastProgress = 0f
            var audioStreamed = false
            
            while (!sessionCompleted) {
                val currentSession = ttsSessionManager.getSession(session.id)
                
                if (currentSession == null) {
                    Log.w(TAG, "Session disappeared: ${session.id}")
                    if (!audioStreamed) {
                        callback.error()
                    } else {
                        callback.done()
                    }
                    return
                }
                
                when (currentSession.status) {
                    SessionStatus.PREPARING -> {
                        Log.d(TAG, "Session preparing...")
                        kotlinx.coroutines.delay(50)
                    }
                    
                    SessionStatus.SPEAKING -> {
                        // Report progress if available
                        if (currentSession.progress > lastProgress) {
                            lastProgress = currentSession.progress
                            val progressPercent = (currentSession.progress * 100).toInt()
                            Log.d(TAG, "Synthesis progress: $progressPercent%")
                        }
                        
                        kotlinx.coroutines.delay(100)
                    }
                    
                    SessionStatus.COMPLETED -> {
                        Log.d(TAG, "Synthesis completed successfully")
                        
                        // Stream final audio data
                        currentSession.audioData?.let { audioData ->
                            Log.d(TAG, "Streaming ${audioData.size} final audio samples")
                            
                            val audioBytes = floatArrayToBytes(audioData)
                            
                            val bytesWritten = callback.audioAvailable(audioBytes, 0, audioBytes.size)
                            if (bytesWritten > 0) {
                                audioStreamed = true
                                Log.d(TAG, "Successfully streamed $bytesWritten bytes of audio")
                            } else {
                                Log.w(TAG, "Audio streaming failed: $bytesWritten")
                            }
                        }
                        
                        callback.done()
                        sessionCompleted = true
                    }
                    
                    SessionStatus.FAILED -> {
                        Log.e(TAG, "Synthesis failed: ${currentSession.error}")
                        callback.error()
                        sessionCompleted = true
                    }
                    
                    SessionStatus.CANCELLED -> {
                        Log.d(TAG, "Synthesis cancelled")
                        callback.error()
                        sessionCompleted = true
                    }
                    
                    else -> {
                        Log.d(TAG, "Session status: ${currentSession.status}")
                        kotlinx.coroutines.delay(100)
                    }
                }
                
                // Safety timeout
                if (System.currentTimeMillis() - session.createdAt > 30000) {
                    Log.w(TAG, "Synthesis timeout, completing")
                    callback.done()
                    sessionCompleted = true
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Synthesis failed", e)
            callback.error()
        }
    }
    
    private fun floatArrayToBytes(floatArray: FloatArray): ByteArray {
        val bytes = ByteArray(floatArray.size * 2) // 16-bit PCM
        
        for (i in floatArray.indices) {
            // Convert float [-1.0, 1.0] to 16-bit signed integer
            val sample = (floatArray[i] * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
            
            // Write as little-endian
            val byteIndex = i * 2
            bytes[byteIndex] = (sample.toInt() and 0xFF).toByte()
            bytes[byteIndex + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        
        return bytes
    }
    
    override fun onStop() {
        Log.d(TAG, "onStop called - stopping all synthesis")
        
        try {
            if (isInitialized) {
                ttsSessionManager.stopAllSynthesis()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping synthesis", e)
        }
    }
    
    override fun onDestroy() {
        Log.d(TAG, "NekoTextToSpeechService destroyed")
        
        try {
            if (isInitialized) {
                ttsSessionManager.stopAllSynthesis()
                ttsSessionManager.cleanup()
            }
            serviceScope.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error during service cleanup", e)
        }
        
        super.onDestroy()
    }
    
    /**
     * Returns the languages supported by this TTS engine
     */
    fun getSupportedLanguages(): Set<String> = SUPPORTED_LANGUAGES
    
    /**
     * Gets service information for debugging
     */
    fun getServiceInfo(): Map<String, Any> = runBlocking {
        try {
            if (!isInitialized) {
                return@runBlocking mapOf(
                    "serviceName" to "NekoTTS",
                    "version" to "1.0.0",
                    "isInitialized" to false,
                    "error" to "Service not initialized"
                )
            }
            
            val settings = settingsRepository.getCurrentSettings().first()
            val voices = voiceRepository.getAllVoices().first()
            val selectedVoice = voiceRepository.getSelectedVoice().first()
            
            mapOf(
                "serviceName" to "NekoTTS",
                "version" to "1.0.0",
                "supportedLanguages" to SUPPORTED_LANGUAGES.size,
                "availableVoices" to voices.size,
                "selectedVoice" to (selectedVoice?.displayName ?: "Default"),
                "speechSpeed" to settings.speechSpeed,
                "speechPitch" to settings.speechPitch,
                "isInitialized" to true,
                "sessionStats" to ttsSessionManager.getStats()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting service info", e)
            mapOf(
                "error" to (e.message ?: "Unknown error"),
                "serviceName" to "NekoTTS",
                "version" to "1.0.0",
                "isInitialized" to isInitialized
            )
        }
    }
}