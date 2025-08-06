package com.nekotts.app.service

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import com.nekotts.app.data.VoiceRepository
import com.nekotts.app.data.SettingsRepository
import com.nekotts.app.data.models.AllVoices
import com.nekotts.app.data.models.VoiceEngine
import com.nekotts.app.engine.KittenEngine
import com.nekotts.app.engine.KokoroEngine
import com.nekotts.app.engine.TTSEngine
import com.nekotts.app.utils.Constants
import com.nekotts.app.core.AppSingletons
import com.nekotts.app.utils.NotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.coroutines.CoroutineContext

/**
 * Main TTS service that integrates with Android's TextToSpeech framework
 */
class NekoTextToSpeechService : TextToSpeechService(), CoroutineScope {
    
    companion object {
        private const val TAG = "NekoTTSService"
    }
    
    private lateinit var voiceRepository: VoiceRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var ttsSessionManager: TTSSessionManager
    
    private var kittenEngine: KittenEngine? = null
    private var kokoroEngine: KokoroEngine? = null
    private var audioManager: AudioManager? = null
    private var notificationManager: NotificationManager? = null
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val serviceJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + serviceJob
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NekoTTS Service created")
        
        // Initialize dependencies from singleton manager
        voiceRepository = AppSingletons.getVoiceRepository()
        settingsRepository = AppSingletons.getSettingsRepository()
        ttsSessionManager = AppSingletons.getTTSSessionManager()
        
        // Initialize system services
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        
        // Start as foreground service for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                NotificationHelper.createForegroundServiceNotification(this)
            )
        }
        
        // Initialize wake lock
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NekoTTS::SynthesisWakeLock"
        )
        
        // Initialize engines asynchronously
        launch {
            initializeEngines()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "NekoTTS Service destroyed")
        
        // Release wake lock if held
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        
        // Stop foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        
        // Cleanup - engines don't have cleanup methods, they are managed by AppSingletons
        // No explicit cleanup needed
        serviceJob.cancel()
    }
    
    private suspend fun initializeEngines() {
        try {
            // Get engines from AppSingletons - they handle initialization
            kittenEngine = AppSingletons.getKittenEngine()
            kokoroEngine = AppSingletons.getKokoroEngine()
            
            Log.d(TAG, "TTS engines initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TTS engines", e)
        }
    }
    
    override fun onGetLanguage(): Array<String> {
        // Return supported languages
        return Constants.SUPPORTED_LANGUAGES.toTypedArray()
    }
    
    override fun onIsLanguageAvailable(lang: String, country: String, variant: String): Int {
        val languageCode = if (country.isNotEmpty()) "${lang}_${country}" else lang
        
        return when {
            Constants.SUPPORTED_LANGUAGES.contains(lang) -> {
                if (hasVoiceForLanguage(lang)) {
                    TextToSpeech.LANG_AVAILABLE
                } else {
                    TextToSpeech.LANG_NOT_SUPPORTED
                }
            }
            else -> TextToSpeech.LANG_NOT_SUPPORTED
        }
    }
    
    override fun onLoadLanguage(lang: String, country: String, variant: String): Int {
        return onIsLanguageAvailable(lang, country, variant)
    }
    
    override fun onStop() {
        Log.d(TAG, "Stopping TTS synthesis")
        ttsSessionManager.stopAllSynthesis()
    }
    
    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        val text = request.charSequenceText?.toString() ?: ""
        
        if (text.isEmpty()) {
            callback.error()
            return
        }
        
        if (text.length > Constants.MAX_TEXT_LENGTH) {
            Log.w(TAG, "Text length exceeds maximum: ${text.length}")
            callback.error()
            return
        }
        
        // Handle synthesis asynchronously
        launch {
            try {
                synthesizeTextAsync(text, request, callback)
            } catch (e: Exception) {
                Log.e(TAG, "Synthesis failed", e)
                callback.error()
            }
        }
    }
    
    private suspend fun synthesizeTextAsync(
        text: String,
        request: SynthesisRequest,
        callback: SynthesisCallback
    ) {
        // Acquire wake lock for synthesis
        wakeLock?.takeIf { !it.isHeld }?.acquire(30000) // 30 second timeout
        
        try {
            // Update notification to show active synthesis
            notificationManager?.let { nm ->
                NotificationHelper.updateForegroundServiceNotification(
                    this, nm, isActive = true, currentText = text
                )
            }
            
            val settings = settingsRepository.getCurrentSettings().first()
            val voice = voiceRepository.getSelectedVoice().first()
                ?: AllVoices.getDefaultVoice()
            
            Log.d(TAG, "Synthesizing: '$text' with voice: ${voice.displayName}")
            
            // Get the appropriate engine
            val engine = when (voice.engine) {
                VoiceEngine.KITTEN -> kittenEngine
                VoiceEngine.KOKORO -> kokoroEngine
            } ?: run {
                Log.e(TAG, "Engine not available for voice: ${voice.engine}")
                callback.error()
                return
            }
            
            // Apply speech parameters
            val speed = if (request.speechRate > 0f) request.speechRate.toFloat() else settings.speechSpeed
            val pitch = if (request.pitch > 0f) request.pitch.toFloat() else settings.speechPitch
            
            // Start synthesis
            callback.start(Constants.SAMPLE_RATE, AudioManager.STREAM_MUSIC, 1)
            
            // Synthesize audio using appropriate engine methods
            val audioData = when (voice.engine) {
                VoiceEngine.KITTEN -> {
                    val kittenRequest = com.nekotts.app.engine.KittenEngine.SynthesisRequest(
                        text = text,
                        voiceId = voice.id,
                        speed = speed,
                        pitch = pitch
                    )
                    (engine as com.nekotts.app.engine.KittenEngine).synthesize(kittenRequest)?.audioData
                }
                VoiceEngine.KOKORO -> {
                    val kokoroRequest = com.nekotts.app.engine.KokoroEngine.SynthesisRequest(
                        text = text,
                        voiceId = voice.id,
                        speed = speed
                    )
                    (engine as com.nekotts.app.engine.KokoroEngine).synthesize(kokoroRequest)?.audioData
                }
            }
            
            if (audioData != null && audioData.isNotEmpty()) {
                // Stream audio data to callback
                streamAudioData(audioData, callback, settings.audioFadeEnabled)
                callback.done()
                
                Log.d(TAG, "Synthesis completed successfully")
            } else {
                Log.e(TAG, "No audio data generated")
                callback.error()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during synthesis", e)
            callback.error()
        } finally {
            // Release wake lock
            wakeLock?.takeIf { it.isHeld }?.release()
            
            // Update notification to idle state
            notificationManager?.let { nm ->
                NotificationHelper.updateForegroundServiceNotification(
                    this, nm, isActive = false
                )
            }
        }
    }
    
    private fun streamAudioData(
        audioData: FloatArray,
        callback: SynthesisCallback,
        fadeEnabled: Boolean
    ) {
        val sampleRate = Constants.SAMPLE_RATE
        val bufferSize = Constants.AUDIO_BUFFER_SIZE
        
        // Convert float audio to 16-bit PCM
        val pcmData = ByteArray(audioData.size * 2)
        var pcmIndex = 0
        
        for (i in audioData.indices) {
            var sample = audioData[i]
            
            // Apply fading
            if (fadeEnabled) {
                val fadeLength = (Constants.FADE_DURATION_MS * sampleRate) / 1000
                when {
                    i < fadeLength -> {
                        // Fade in
                        sample *= (i.toFloat() / fadeLength)
                    }
                    i > audioData.size - fadeLength -> {
                        // Fade out
                        val fadeOutPos = audioData.size - i
                        sample *= (fadeOutPos.toFloat() / fadeLength)
                    }
                }
            }
            
            // Convert to 16-bit PCM
            val pcmSample = (sample * Short.MAX_VALUE).toInt().coerceIn(
                Short.MIN_VALUE.toInt(),
                Short.MAX_VALUE.toInt()
            ).toShort()
            
            pcmData[pcmIndex++] = (pcmSample.toInt() and 0xFF).toByte()
            pcmData[pcmIndex++] = ((pcmSample.toInt() shr 8) and 0xFF).toByte()
        }
        
        // Stream data in chunks
        var offset = 0
        while (offset < pcmData.size) {
            val chunkSize = minOf(bufferSize, pcmData.size - offset)
            val success = callback.audioAvailable(pcmData, offset, chunkSize) == TextToSpeech.SUCCESS
            
            if (!success) {
                Log.w(TAG, "Audio streaming interrupted")
                break
            }
            
            offset += chunkSize
        }
    }
    
    override fun onGetVoices(): List<android.speech.tts.Voice> {
        val voices = mutableListOf<android.speech.tts.Voice>()
        
        runBlocking {
            val availableVoices = voiceRepository.getDownloadedVoices().first()
            
            for (voice in availableVoices) {
                try {
                    val ttsVoice = android.speech.tts.Voice(
                        voice.id,
                        java.util.Locale(voice.language),
                        when (voice.quality) {
                            com.nekotts.app.data.models.VoiceQuality.LOW -> android.speech.tts.Voice.QUALITY_LOW
                            com.nekotts.app.data.models.VoiceQuality.STANDARD -> android.speech.tts.Voice.QUALITY_NORMAL
                            com.nekotts.app.data.models.VoiceQuality.HIGH -> android.speech.tts.Voice.QUALITY_HIGH
                            com.nekotts.app.data.models.VoiceQuality.PREMIUM -> android.speech.tts.Voice.QUALITY_VERY_HIGH
                        },
                        0, // latency
                        true, // requiresNetworkConnection
                        setOf() // features
                    )
                    voices.add(ttsVoice)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to create TTS voice for ${voice.id}", e)
                }
            }
        }
        
        return voices
    }
    
    override fun onGetDefaultVoiceNameFor(lang: String, country: String, variant: String): String {
        // Try to find a voice for the requested language
        val languageVoices = AllVoices.getVoicesByLanguage(lang)
        
        return if (languageVoices.isNotEmpty()) {
            // Prefer downloaded voices
            val downloadedVoices = languageVoices.filter { it.isDownloaded }
            (downloadedVoices.firstOrNull() ?: languageVoices.first()).id
        } else {
            // Fallback to default voice
            AllVoices.getDefaultVoice().id
        }
    }
    
    private fun hasVoiceForLanguage(language: String): Boolean {
        return AllVoices.getVoicesByLanguage(language).any { it.isDownloaded }
    }
    
    /**
     * Handle TTS parameters from the request
     */
    private fun extractTTSParams(request: SynthesisRequest): TTSParams {
        val params = request.params
        
        return TTSParams(
            speed = params.getFloat("rate", 1.0f),
            pitch = params.getFloat("pitch", 1.0f),
            volume = params.getFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f),
            streamType = params.getInt(
                TextToSpeech.Engine.KEY_PARAM_STREAM,
                AudioManager.STREAM_MUSIC
            ),
            utteranceId = params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID),
            sessionId = params.getString(TextToSpeech.Engine.KEY_PARAM_SESSION_ID)
        )
    }
    
    /**
     * Handle audio focus for TTS playback
     */
    private fun requestAudioFocus(): Boolean {
        return try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                @Suppress("NewApi")
                val focusRequest = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(audioAttributes)
                    .setWillPauseWhenDucked(true)
                    .build()
                
                audioManager?.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                true // Skip audio focus for older versions
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to request audio focus", e)
            true // Continue anyway
        }
    }
    
    /**
     * Release audio focus
     */
    private fun releaseAudioFocus() {
        try {
            // In a real implementation, you'd store the focus request and abandon it here
            Log.d(TAG, "Released audio focus")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release audio focus", e)
        }
    }
}

/**
 * TTS parameters extracted from synthesis request
 */
private data class TTSParams(
    val speed: Float,
    val pitch: Float,
    val volume: Float,
    val streamType: Int,
    val utteranceId: String?,
    val sessionId: String?
)