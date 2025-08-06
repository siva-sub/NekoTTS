package com.nekotts.app.service

import android.content.Context
import android.media.AudioManager
import android.media.AudioTrack
import android.media.AudioAttributes
import android.media.AudioFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nekotts.app.data.VoiceRepository
import com.nekotts.app.data.SettingsRepository
import com.nekotts.app.data.models.AllVoices
import com.nekotts.app.data.models.VoiceEngine
import com.nekotts.app.engine.KittenEngine
import com.nekotts.app.engine.KokoroEngine
import com.nekotts.app.engine.AudioProcessor
import com.nekotts.app.core.AppSingletons
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

enum class SessionStatus {
    IDLE,
    PREPARING,
    SPEAKING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class SessionPriority {
    LOW,
    NORMAL, 
    HIGH,
    URGENT
}

data class TTSSession(
    val id: String,
    val text: String,
    val voiceId: String,
    val status: SessionStatus,
    val progress: Float = 0f,
    val error: String? = null,
    val isCompleted: Boolean = false,
    val priority: SessionPriority = SessionPriority.NORMAL,
    val isActive: Boolean = false,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val audioTrack: AudioTrack? = null,
    val audioData: FloatArray? = null,
    val sampleRate: Int = 24000
) {
    val duration: Long?
        get() = if (startedAt != null && completedAt != null) {
            completedAt - startedAt
        } else null
        
    val isFinished: Boolean
        get() = status in listOf(SessionStatus.COMPLETED, SessionStatus.FAILED, SessionStatus.CANCELLED)
}

/**
 * Manages TTS synthesis sessions with audio playback
 */
class TTSSessionManager(
    private val context: Context
) : CoroutineScope {
    
    companion object {
        private const val TAG = "TTSSessionManager"
        private const val MAX_CONCURRENT_SESSIONS = 3
        private const val SESSION_TIMEOUT_MS = 30000L
    }
    
    private val supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Main + supervisorJob
    
    private val sessionIdGenerator = AtomicInteger(0)
    private val activeSessions = ConcurrentHashMap<String, TTSSession>()
    private val sessionJobs = ConcurrentHashMap<String, Job>()
    
    private val _sessionsFlow = MutableStateFlow<List<TTSSession>>(emptyList())
    val sessions: StateFlow<List<TTSSession>> = _sessionsFlow
    val activeSessionsFlow: StateFlow<List<TTSSession>> = _sessionsFlow
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioProcessor = AudioProcessor()
    
    // Lazy initialization of engines from AppSingletons
    private val kittenEngine: KittenEngine by lazy { AppSingletons.getKittenEngine() }
    private val kokoroEngine: KokoroEngine by lazy { AppSingletons.getKokoroEngine() }
    private val voiceRepository: VoiceRepository by lazy { AppSingletons.getVoiceRepository() }
    private val settingsRepository: SettingsRepository by lazy { AppSingletons.getSettingsRepository() }
    
    init {
        Log.d(TAG, "TTSSessionManager initialized")
        startSessionCleanup()
    }
    
    /**
     * Creates a new TTS session
     */
    suspend fun createSession(
        text: String,
        voiceId: String,
        speed: Float = 1.0f,
        pitch: Float = 1.0f,
        priority: SessionPriority = SessionPriority.NORMAL
    ): TTSSession {
        val sessionId = "tts_${sessionIdGenerator.incrementAndGet()}_${System.currentTimeMillis()}"
        
        val session = TTSSession(
            id = sessionId,
            text = text.trim(),
            voiceId = voiceId,
            status = SessionStatus.IDLE,
            priority = priority,
            speed = speed.coerceIn(0.1f, 3.0f),
            pitch = pitch.coerceIn(0.1f, 3.0f)
        )
        
        activeSessions[sessionId] = session
        updateSessionsFlow()
        
        Log.d(TAG, "Created session $sessionId for voice $voiceId")
        return session
    }
    
    /**
     * Starts TTS synthesis and playback for a session
     */
    suspend fun startSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = activeSessions[sessionId] 
                ?: return@withContext Result.failure(IllegalArgumentException("Session not found: $sessionId"))
            
            if (session.status != SessionStatus.IDLE) {
                return@withContext Result.failure(IllegalStateException("Session $sessionId is not in IDLE state"))
            }
            
            // Check concurrent session limit
            val currentActive = activeSessions.values.count { it.status == SessionStatus.SPEAKING }
            if (currentActive >= MAX_CONCURRENT_SESSIONS) {
                return@withContext Result.failure(IllegalStateException("Too many concurrent sessions"))
            }
            
            Log.d(TAG, "Starting session $sessionId")
            
            // Update session status
            updateSession(sessionId) { 
                it.copy(
                    status = SessionStatus.PREPARING, 
                    startedAt = System.currentTimeMillis(),
                    isActive = true
                )
            }
            
            // Start synthesis job
            val job = launch {
                synthesizeAndPlay(sessionId)
            }
            
            sessionJobs[sessionId] = job
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start session $sessionId", e)
            updateSession(sessionId) { 
                it.copy(
                    status = SessionStatus.FAILED,
                    error = e.message,
                    isActive = false
                )
            }
            Result.failure(e)
        }
    }
    
    /**
     * Synthesizes audio and plays it
     */
    private suspend fun synthesizeAndPlay(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            val session = activeSessions[sessionId] ?: return@withContext
            
            Log.d(TAG, "Synthesizing audio for session $sessionId")
            
            // Get voice info
            val voice = AllVoices.getVoiceById(session.voiceId) 
                ?: AllVoices.getDefaultVoice()
            
            // Update progress
            updateSession(sessionId) { it.copy(progress = 0.2f) }
            
            // Synthesize using appropriate engine
            val audioData = when (voice.engine) {
                VoiceEngine.KOKORO -> {
                    val request = KokoroEngine.SynthesisRequest(
                        text = session.text,
                        voiceId = session.voiceId,
                        speed = session.speed
                    )
                    kokoroEngine.synthesize(request)?.audioData
                }
                VoiceEngine.KITTEN -> {
                    val request = KittenEngine.SynthesisRequest(
                        text = session.text,
                        voiceId = session.voiceId,
                        speed = session.speed,
                        pitch = session.pitch
                    )
                    kittenEngine.synthesize(request)?.audioData
                }
            }
            
            if (audioData == null || audioData.isEmpty()) {
                throw IllegalStateException("No audio data generated")
            }
            
            Log.d(TAG, "Audio synthesized for session $sessionId: ${audioData.size} samples")
            
            // Update session with audio data
            updateSession(sessionId) { 
                it.copy(
                    status = SessionStatus.SPEAKING,
                    progress = 0.5f,
                    audioData = audioData
                )
            }
            
            // Play audio
            playAudio(sessionId, audioData, session.sampleRate)
            
        } catch (e: Exception) {
            Log.e(TAG, "Synthesis failed for session $sessionId", e)
            updateSession(sessionId) { 
                it.copy(
                    status = SessionStatus.FAILED,
                    error = e.message ?: "Synthesis failed",
                    isActive = false,
                    completedAt = System.currentTimeMillis()
                )
            }
        }
    }
    
    /**
     * Plays synthesized audio
     */
    private suspend fun playAudio(sessionId: String, audioData: FloatArray, sampleRate: Int) = withContext(Dispatchers.IO) {
        var audioTrack: AudioTrack? = null
        
        try {
            Log.d(TAG, "Playing audio for session $sessionId")
            
            // Convert float audio to 16-bit PCM
            val pcmData = FloatArray(audioData.size)
            audioData.copyInto(pcmData)
            
            val shortData = ShortArray(pcmData.size)
            for (i in pcmData.indices) {
                shortData[i] = (pcmData[i] * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            
            // Create audio track
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate, 
                AudioFormat.CHANNEL_OUT_MONO, 
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(bufferSize, shortData.size * 2))
                .build()
            
            // Update session with audio track
            updateSession(sessionId) { 
                it.copy(audioTrack = audioTrack, progress = 0.7f)
            }
            
            audioTrack.play()
            
            // Write audio data in chunks
            val chunkSize = 4096
            var offset = 0
            val totalSamples = shortData.size
            
            while (offset < totalSamples && !isSessionCancelled(sessionId)) {
                val remainingSamples = totalSamples - offset
                val samplesToWrite = minOf(chunkSize, remainingSamples)
                
                val written = audioTrack.write(shortData, offset, samplesToWrite)
                if (written < 0) {
                    throw IllegalStateException("AudioTrack write error: $written")
                }
                
                offset += written
                
                // Update progress
                val progress = 0.7f + (0.3f * offset / totalSamples)
                updateSession(sessionId) { it.copy(progress = progress) }
                
                // Small delay to prevent tight loop
                delay(1)
            }
            
            // Wait for playback to finish
            if (!isSessionCancelled(sessionId)) {
                // Estimate playback duration and wait
                val playbackDurationMs = (shortData.size * 1000L) / sampleRate
                delay(minOf(playbackDurationMs, 5000)) // Max 5 seconds wait
            }
            
            Log.d(TAG, "Audio playback completed for session $sessionId")
            
            // Mark session as completed
            updateSession(sessionId) { 
                it.copy(
                    status = SessionStatus.COMPLETED,
                    progress = 1.0f,
                    isActive = false,
                    completedAt = System.currentTimeMillis()
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Audio playback failed for session $sessionId", e)
            updateSession(sessionId) { 
                it.copy(
                    status = SessionStatus.FAILED,
                    error = "Playback failed: ${e.message}",
                    isActive = false,
                    completedAt = System.currentTimeMillis()
                )
            }
        } finally {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing audio track", e)
            }
        }
    }
    
    /**
     * Pauses a session
     */
    suspend fun pauseSession(sessionId: String) {
        updateSession(sessionId) { session ->
            if (session.status == SessionStatus.SPEAKING) {
                session.audioTrack?.pause()
                session.copy(status = SessionStatus.PAUSED)
            } else {
                session
            }
        }
        Log.d(TAG, "Session $sessionId paused")
    }
    
    /**
     * Stops and cancels a session
     */
    suspend fun stopSession(sessionId: String) {
        sessionJobs[sessionId]?.cancel("Session stopped")
        sessionJobs.remove(sessionId)
        
        updateSession(sessionId) { session ->
            session.audioTrack?.stop()
            session.audioTrack?.release()
            session.copy(
                status = SessionStatus.CANCELLED,
                isActive = false,
                completedAt = System.currentTimeMillis(),
                audioTrack = null
            )
        }
        
        Log.d(TAG, "Session $sessionId stopped")
    }
    
    /**
     * Stops all active synthesis
     */
    fun stopAllSynthesis() {
        Log.d(TAG, "Stopping all synthesis")
        
        launch {
            sessionJobs.values.forEach { it.cancel("Stop all synthesis") }
            sessionJobs.clear()
            
            activeSessions.values.filter { it.isActive }.forEach { session ->
                try {
                    session.audioTrack?.stop()
                    session.audioTrack?.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping session ${session.id}", e)
                }
            }
            
            val cancelledSessions = activeSessions.mapValues { (_, session) ->
                if (session.isActive) {
                    session.copy(
                        status = SessionStatus.CANCELLED,
                        isActive = false,
                        completedAt = System.currentTimeMillis(),
                        audioTrack = null
                    )
                } else {
                    session
                }
            }
            
            activeSessions.clear()
            activeSessions.putAll(cancelledSessions)
            updateSessionsFlow()
        }
    }
    
    /**
     * Gets a specific session
     */
    fun getSession(sessionId: String): TTSSession? = activeSessions[sessionId]
    
    /**
     * Gets all active sessions
     */
    fun getActiveSessions(): List<TTSSession> = activeSessions.values.filter { it.isActive }
    
    /**
     * Gets all sessions
     */
    fun getAllSessions(): List<TTSSession> = activeSessions.values.toList()
    
    private fun updateSession(sessionId: String, update: (TTSSession) -> TTSSession) {
        activeSessions[sessionId]?.let { session ->
            activeSessions[sessionId] = update(session)
            updateSessionsFlow()
        }
    }
    
    private fun updateSessionsFlow() {
        _sessionsFlow.value = activeSessions.values.sortedByDescending { it.createdAt }
    }
    
    private fun isSessionCancelled(sessionId: String): Boolean {
        return activeSessions[sessionId]?.status == SessionStatus.CANCELLED
    }
    
    /**
     * Cleanup old completed sessions
     */
    private fun startSessionCleanup() {
        launch {
            while (true) {
                try {
                    delay(60000) // Clean every minute
                    
                    val now = System.currentTimeMillis()
                    val oldSessions = activeSessions.values.filter { session ->
                        session.isFinished && 
                        (session.completedAt ?: session.createdAt) < (now - 300000) // 5 minutes old
                    }
                    
                    oldSessions.forEach { session ->
                        activeSessions.remove(session.id)
                        sessionJobs.remove(session.id)?.cancel()
                    }
                    
                    if (oldSessions.isNotEmpty()) {
                        updateSessionsFlow()
                        Log.d(TAG, "Cleaned up ${oldSessions.size} old sessions")
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Session cleanup error", e)
                }
            }
        }
    }
    
    /**
     * Gets session manager stats
     */
    fun getStats(): Map<String, Any> {
        val sessions = activeSessions.values
        return mapOf(
            "totalSessions" to sessions.size,
            "activeSessions" to sessions.count { it.isActive },
            "completedSessions" to sessions.count { it.status == SessionStatus.COMPLETED },
            "failedSessions" to sessions.count { it.status == SessionStatus.FAILED },
            "cancelledSessions" to sessions.count { it.status == SessionStatus.CANCELLED },
            "averageProcessingTime" to sessions.mapNotNull { it.duration }.let { durations ->
                if (durations.isEmpty()) 0.0 else durations.average()
            },
            "maxConcurrentSessions" to MAX_CONCURRENT_SESSIONS
        )
    }
    
    /**
     * Cleanup all resources
     */
    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up TTSSessionManager")
            
            stopAllSynthesis()
            supervisorJob.cancel()
            audioProcessor.cleanup()
            
            activeSessions.clear()
            sessionJobs.clear()
            
            Log.d(TAG, "TTSSessionManager cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error", e)
        }
    }
}