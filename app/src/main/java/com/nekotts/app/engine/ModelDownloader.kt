package com.nekotts.app.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads TTS models from remote sources if not available locally
 */
class ModelDownloader(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelDownloader"
        
        // HuggingFace model URLs - Following kokoro-web pattern
        private const val KOKORO_BASE_URL = "https://huggingface.co/onnx-community/Kokoro-82M-ONNX/resolve/main/onnx"
        private const val KOKORO_VOICES_BASE_URL = "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/voices"
        
        // Kokoro has multiple quantization options - we'll use q8f16 for best quality/size tradeoff
        private const val KOKORO_MODEL_URL = "$KOKORO_BASE_URL/model_q8f16.onnx"
        
        private const val CONNECT_TIMEOUT = 30000 // 30 seconds
        private const val READ_TIMEOUT = 60000 // 60 seconds
    }
    
    /**
     * Download interface for progress callbacks
     */
    interface DownloadCallback {
        fun onProgress(downloaded: Long, total: Long)
        fun onComplete(success: Boolean, message: String)
    }
    
    /**
     * Check if models exist in assets or download them
     */
    suspend fun ensureModelsAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelsDir = File(context.filesDir, "models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }
            
            var allAvailable = true
            
            // Kitten model and voices are bundled in assets, no need to download
            val kittenModelAvailable = isModelAvailable("kitten_tts_nano_v0_1.onnx")
            val kittenVoicesAvailable = isModelAvailable("voices.npz")
            
            if (kittenModelAvailable && kittenVoicesAvailable) {
                Log.d(TAG, "Kitten TTS model and voices available from assets")
            } else {
                Log.w(TAG, "Kitten model or voices missing from assets")
                allAvailable = false
            }
            
            // Kokoro model is downloaded on-demand when a Kokoro voice is selected
            // We don't download it here to keep the app size small
            Log.d(TAG, "Kokoro model will be downloaded on-demand when needed")
            
            Log.d(TAG, "Model availability check complete: $allAvailable")
            allAvailable
            
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring models are available", e)
            false
        }
    }
    
    /**
     * Check if a model is available either in assets or downloaded
     */
    private fun isModelAvailable(fileName: String): Boolean {
        return try {
            // First check assets
            context.assets.open("models/$fileName").use {
                Log.d(TAG, "$fileName found in assets")
                true
            }
        } catch (e: Exception) {
            // Then check downloaded files
            val downloadedFile = File(context.filesDir, "models/$fileName")
            val available = downloadedFile.exists() && downloadedFile.length() > 0
            if (available) {
                Log.d(TAG, "$fileName found in downloads")
            } else {
                Log.d(TAG, "$fileName not available")
            }
            available
        }
    }
    
    /**
     * Download a model file from URL
     */
    private suspend fun downloadModel(
        url: String,
        destination: File,
        modelName: String,
        callback: DownloadCallback? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting download of $modelName from $url")
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("User-Agent", "NekoTTS/1.0")
            }
            
            connection.connect()
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Download failed: HTTP ${connection.responseCode}")
                callback?.onComplete(false, "HTTP ${connection.responseCode}")
                return@withContext false
            }
            
            val fileLength = connection.contentLength.toLong()
            Log.d(TAG, "Downloading $modelName: ${fileLength / 1024 / 1024} MB")
            
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        
                        // Report progress
                        callback?.onProgress(downloaded, fileLength)
                        
                        // Log progress every 10MB
                        if (downloaded % (10 * 1024 * 1024) == 0L) {
                            Log.d(TAG, "$modelName: ${downloaded / 1024 / 1024} MB downloaded")
                        }
                    }
                }
            }
            
            Log.d(TAG, "$modelName downloaded successfully: ${destination.length() / 1024 / 1024} MB")
            callback?.onComplete(true, "$modelName downloaded successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download $modelName", e)
            callback?.onComplete(false, "Download failed: ${e.message}")
            
            // Clean up partial download
            if (destination.exists()) {
                destination.delete()
            }
            
            false
        }
    }
    
    /**
     * Download Kokoro model on-demand
     */
    suspend fun downloadKokoroModel(callback: DownloadCallback? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelsDir = File(context.filesDir, "models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }
            
            // Check if already available
            if (isModelAvailable("kokoro-v1.0.onnx")) {
                Log.d(TAG, "Kokoro model already available")
                callback?.onComplete(true, "Kokoro model already available")
                return@withContext true
            }
            
            Log.d(TAG, "Downloading Kokoro model from HuggingFace...")
            return@withContext downloadModel(
                KOKORO_MODEL_URL,
                File(modelsDir, "kokoro-v1.0.onnx"),
                "Kokoro TTS Model",
                callback
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading Kokoro model", e)
            callback?.onComplete(false, "Download failed: ${e.message}")
            false
        }
    }
    
    /**
     * Download individual Kokoro voice file
     */
    suspend fun downloadKokoroVoice(voiceId: String, callback: DownloadCallback? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val voicesDir = File(context.filesDir, "voices")
            if (!voicesDir.exists()) {
                voicesDir.mkdirs()
            }
            
            val voiceFile = File(voicesDir, "$voiceId.bin")
            
            // Check if already downloaded
            if (voiceFile.exists() && voiceFile.length() > 0) {
                Log.d(TAG, "Voice $voiceId already downloaded")
                callback?.onComplete(true, "Voice already available")
                return@withContext true
            }
            
            // Download voice file following kokoro-web pattern
            val voiceUrl = "$KOKORO_VOICES_BASE_URL/$voiceId.bin"
            Log.d(TAG, "Downloading voice $voiceId from $voiceUrl")
            
            return@withContext downloadModel(
                voiceUrl,
                voiceFile,
                "Voice: $voiceId",
                callback
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading voice $voiceId", e)
            callback?.onComplete(false, "Download failed: ${e.message}")
            false
        }
    }
    
    /**
     * Check if a Kokoro voice is downloaded
     */
    fun isKokoroVoiceDownloaded(voiceId: String): Boolean {
        val voiceFile = File(context.filesDir, "voices/$voiceId.bin")
        return voiceFile.exists() && voiceFile.length() > 0
    }
    
    /**
     * Get path to a downloaded Kokoro voice
     */
    fun getKokoroVoicePath(voiceId: String): String? {
        val voiceFile = File(context.filesDir, "voices/$voiceId.bin")
        return if (voiceFile.exists()) voiceFile.absolutePath else null
    }
    
    /**
     * Get downloaded model file path
     */
    fun getModelPath(fileName: String): String? {
        return try {
            // First try assets
            context.assets.open("models/$fileName").use {
                "file:///android_asset/models/$fileName"
            }
        } catch (e: Exception) {
            // Then try downloaded files
            val downloadedFile = File(context.filesDir, "models/$fileName")
            if (downloadedFile.exists()) {
                downloadedFile.absolutePath
            } else {
                null
            }
        }
    }
    
    /**
     * Delete downloaded models to free space
     */
    fun clearDownloadedModels(): Boolean {
        return try {
            val modelsDir = File(context.filesDir, "models")
            if (modelsDir.exists()) {
                val deleted = modelsDir.deleteRecursively()
                Log.d(TAG, "Downloaded models cleared: $deleted")
                deleted
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing downloaded models", e)
            false
        }
    }
    
    /**
     * Get total size of downloaded models
     */
    fun getDownloadedModelsSize(): Long {
        return try {
            val modelsDir = File(context.filesDir, "models")
            if (modelsDir.exists()) {
                modelsDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating downloaded models size", e)
            0L
        }
    }
}