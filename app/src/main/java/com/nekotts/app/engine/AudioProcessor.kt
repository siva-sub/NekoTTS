package com.nekotts.app.engine

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

class AudioProcessor {
    
    companion object {
        private const val TAG = "AudioProcessor"
        private const val DEFAULT_SAMPLE_RATE = 22050
        private const val KOKORO_SAMPLE_RATE = 24000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val SILENCE_THRESHOLD = 0.01f
        private const val MIN_AUDIO_LENGTH_MS = 50
    }
    
    private var audioTrack: AudioTrack? = null
    
    fun createAudioTrack(sampleRate: Int = DEFAULT_SAMPLE_RATE): AudioTrack {
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT)
        
        return AudioTrack.Builder()
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 2) // Double buffer for smoother playback
            .build()
    }
    
    suspend fun playAudio(audioData: FloatArray, sampleRate: Int = DEFAULT_SAMPLE_RATE, speed: Float = 1.0f) = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "Playing audio with ${audioData.size} samples at ${sampleRate}Hz, speed $speed")
            
            // Apply speed modification first
            val speedAdjustedData = if (speed != 1.0f) {
                changeSpeed(audioData, speed)
            } else {
                audioData
            }
            
            // Convert float array to 16-bit PCM
            val pcmData = floatToPcm16(speedAdjustedData)
            
            // Create and play audio
            val track = createAudioTrack(sampleRate)
            track.play()
            
            // Write audio data in chunks for smooth playback
            val chunkSize = 1024
            var offset = 0
            
            while (offset < pcmData.size) {
                val remainingBytes = (pcmData.size - offset) * 2
                val bytesToWrite = minOf(chunkSize * 2, remainingBytes)
                val samplesChunk = bytesToWrite / 2
                
                val bytesWritten = track.write(pcmData, offset, samplesChunk)
                offset += samplesChunk
                
                if (bytesWritten < 0) {
                    Log.w(TAG, "Audio write error: $bytesWritten")
                    break
                }
            }
            
            // Wait for playback to complete
            track.flush()
            track.stop()
            track.release()
            
            Log.d(TAG, "Audio playback completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
            audioTrack?.release()
        }
    }
    
    private fun floatToPcm16(floatArray: FloatArray): ShortArray {
        val pcmData = ShortArray(floatArray.size)
        for (i in floatArray.indices) {
            // Clamp to [-1.0, 1.0] and convert to 16-bit
            val sample = floatArray[i].coerceIn(-1.0f, 1.0f)
            pcmData[i] = (sample * Short.MAX_VALUE).roundToInt().toShort()
        }
        return pcmData
    }
    
    fun changeSpeed(audioData: FloatArray, speed: Float): FloatArray {
        if (speed == 1.0f) return audioData
        
        val clampedSpeed = speed.coerceIn(0.5f, 2.0f)
        if (clampedSpeed == 1.0f) return audioData
        
        val outputSize = (audioData.size / clampedSpeed).roundToInt()
        val outputData = FloatArray(outputSize)
        
        // Use linear interpolation for better quality
        for (i in outputData.indices) {
            val sourceIndex = i * clampedSpeed
            val sourceIndexInt = sourceIndex.toInt()
            val fraction = sourceIndex - sourceIndexInt
            
            if (sourceIndexInt < audioData.size - 1) {
                // Linear interpolation between two samples
                val sample1 = audioData[sourceIndexInt]
                val sample2 = audioData[sourceIndexInt + 1]
                outputData[i] = sample1 + fraction * (sample2 - sample1)
            } else if (sourceIndexInt < audioData.size) {
                outputData[i] = audioData[sourceIndexInt]
            }
        }
        
        return outputData
    }
    
    fun convertToWav(audioData: FloatArray, sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val pcmData = floatToPcm16(audioData)
        return createWavFile(pcmData, sampleRate)
    }
    
    private fun createWavFile(pcmData: ShortArray, sampleRate: Int): ByteArray {
        val dataSize = pcmData.size * 2 // 16-bit samples
        val fileSize = 36 + dataSize
        
        val wav = ByteArray(44 + dataSize)
        var offset = 0
        
        // RIFF header
        wav[offset++] = 'R'.code.toByte()
        wav[offset++] = 'I'.code.toByte()
        wav[offset++] = 'F'.code.toByte()
        wav[offset++] = 'F'.code.toByte()
        
        // File size
        writeInt(wav, offset, fileSize)
        offset += 4
        
        // WAVE header
        wav[offset++] = 'W'.code.toByte()
        wav[offset++] = 'A'.code.toByte()
        wav[offset++] = 'V'.code.toByte()
        wav[offset++] = 'E'.code.toByte()
        
        // fmt subchunk
        wav[offset++] = 'f'.code.toByte()
        wav[offset++] = 'm'.code.toByte()
        wav[offset++] = 't'.code.toByte()
        wav[offset++] = ' '.code.toByte()
        
        // fmt subchunk size
        writeInt(wav, offset, 16)
        offset += 4
        
        // Audio format (PCM)
        writeShort(wav, offset, 1)
        offset += 2
        
        // Number of channels
        writeShort(wav, offset, 1)
        offset += 2
        
        // Sample rate
        writeInt(wav, offset, sampleRate)
        offset += 4
        
        // Byte rate
        writeInt(wav, offset, sampleRate * 2)
        offset += 4
        
        // Block align
        writeShort(wav, offset, 2)
        offset += 2
        
        // Bits per sample
        writeShort(wav, offset, 16)
        offset += 2
        
        // data subchunk
        wav[offset++] = 'd'.code.toByte()
        wav[offset++] = 'a'.code.toByte()
        wav[offset++] = 't'.code.toByte()
        wav[offset++] = 'a'.code.toByte()
        
        // Data size
        writeInt(wav, offset, dataSize)
        offset += 4
        
        // PCM data
        for (sample in pcmData) {
            writeShort(wav, offset, sample.toInt())
            offset += 2
        }
        
        return wav
    }
    
    private fun writeInt(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buffer[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buffer[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }
    
    private fun writeShort(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }
    
    fun normalizeAudio(audioData: FloatArray): FloatArray {
        val maxAmplitude = audioData.maxOfOrNull { abs(it) } ?: 1.0f
        
        return if (maxAmplitude > 0.0f && maxAmplitude != 1.0f) {
            val normalizationFactor = 0.95f / maxAmplitude // Leave some headroom
            audioData.map { it * normalizationFactor }.toFloatArray()
        } else {
            audioData
        }
    }
    
    fun applyFadeInOut(audioData: FloatArray, fadeMs: Int = 50): FloatArray {
        if (audioData.isEmpty()) return audioData
        
        val sampleRate = DEFAULT_SAMPLE_RATE // Use appropriate sample rate
        val fadeLength = (sampleRate * fadeMs / 1000).coerceAtMost(audioData.size / 4)
        if (fadeLength <= 0) return audioData
        
        val processedData = audioData.copyOf()
        
        // Fade in
        for (i in 0 until fadeLength) {
            val fadeMultiplier = sin(PI * i / (2 * fadeLength)).toFloat()
            processedData[i] *= fadeMultiplier
        }
        
        // Fade out
        val startFadeOut = audioData.size - fadeLength
        for (i in startFadeOut until audioData.size) {
            val fadeMultiplier = sin(PI * (audioData.size - i) / (2 * fadeLength)).toFloat()
            processedData[i] *= fadeMultiplier
        }
        
        return processedData
    }
    
    fun trimSilence(audioData: FloatArray, threshold: Float = SILENCE_THRESHOLD): FloatArray {
        if (audioData.isEmpty()) return audioData
        
        // Find start of audio (first non-silent sample)
        var start = 0
        for (i in audioData.indices) {
            if (abs(audioData[i]) > threshold) {
                start = i
                break
            }
        }
        
        // Find end of audio (last non-silent sample)
        var end = audioData.size - 1
        for (i in audioData.indices.reversed()) {
            if (abs(audioData[i]) > threshold) {
                end = i
                break
            }
        }
        
        // Ensure minimum audio length
        val minLength = DEFAULT_SAMPLE_RATE * MIN_AUDIO_LENGTH_MS / 1000
        if (end - start < minLength) {
            val center = (start + end) / 2
            start = maxOf(0, center - minLength / 2)
            end = minOf(audioData.size - 1, start + minLength)
        }
        
        return if (start < end) {
            audioData.copyOfRange(start, end + 1)
        } else {
            audioData
        }
    }
    
    fun resample(audioData: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (fromRate == toRate) return audioData
        
        val ratio = toRate.toDouble() / fromRate
        val outputSize = (audioData.size * ratio).roundToInt()
        val outputData = FloatArray(outputSize)
        
        for (i in outputData.indices) {
            val sourceIndex = i / ratio
            val sourceIndexInt = sourceIndex.toInt()
            val fraction = sourceIndex - sourceIndexInt
            
            if (sourceIndexInt < audioData.size - 1) {
                // Linear interpolation
                val sample1 = audioData[sourceIndexInt]
                val sample2 = audioData[sourceIndexInt + 1]
                outputData[i] = (sample1 + fraction * (sample2 - sample1)).toFloat()
            } else if (sourceIndexInt < audioData.size) {
                outputData[i] = audioData[sourceIndexInt]
            }
        }
        
        return outputData
    }
    
    fun applyGain(audioData: FloatArray, gainDb: Float): FloatArray {
        if (gainDb == 0.0f) return audioData
        
        val gainLinear = 10.0.pow(gainDb / 20.0).toFloat()
        return audioData.map { it * gainLinear }.toFloatArray()
    }
    
    fun concatenateAudio(vararg audioArrays: FloatArray): FloatArray {
        val totalLength = audioArrays.sumOf { it.size }
        val result = FloatArray(totalLength)
        
        var offset = 0
        for (array in audioArrays) {
            System.arraycopy(array, 0, result, offset, array.size)
            offset += array.size
        }
        
        return result
    }
    
    fun addSilence(durationMs: Int, sampleRate: Int = DEFAULT_SAMPLE_RATE): FloatArray {
        val sampleCount = (sampleRate * durationMs / 1000.0).roundToInt()
        return FloatArray(sampleCount) // All zeros = silence
    }
    
    fun getAudioInfo(audioData: FloatArray, sampleRate: Int = DEFAULT_SAMPLE_RATE): Map<String, Any> {
        val duration = audioData.size.toFloat() / sampleRate
        val maxAmplitude = audioData.maxOfOrNull { abs(it) } ?: 0.0f
        val rms = sqrt(audioData.map { it * it }.average()).toFloat()
        
        return mapOf(
            "samples" to audioData.size,
            "duration" to duration,
            "sampleRate" to sampleRate,
            "maxAmplitude" to maxAmplitude,
            "rmsLevel" to rms,
            "channels" to 1,
            "bitDepth" to 32 // float32
        )
    }
    
    fun cleanup() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            Log.d(TAG, "AudioProcessor cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}