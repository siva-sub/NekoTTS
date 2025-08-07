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
        
        // HuggingFace model URLs
        private const val KITTEN_MODEL_URL = "https://huggingface.co/KittenML/kitten-tts-nano-0.1/resolve/main/kitten_tts_nano_v0_1.onnx"
        private const val KITTEN_VOICES_URL = "https://huggingface.co/KittenML/kitten-tts-nano-0.1/resolve/main/voices.npz"
        
        private const val KOKORO_MODEL_URL = "https://huggingface.co/onnx-community/Kokoro-82M-ONNX/resolve/main/onnx/model_q8f16.onnx"
        private const val KOKORO_VOICES_URL = "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/voices-v1.0.bin"
        
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
            
            // Check Kitten model
            if (!isModelAvailable("kitten_tts_nano_v0_1.onnx")) {
                Log.d(TAG, "Kitten model not available, attempting download")
                allAvailable = allAvailable && downloadModel(
                    KITTEN_MODEL_URL,
                    File(modelsDir, "kitten_tts_nano_v0_1.onnx"),
                    "Kitten TTS Model"
                )
            }
            
            // Check Kitten voices
            if (!isModelAvailable("voices.npz")) {
                Log.d(TAG, "Kitten voices not available, attempting download")
                allAvailable = allAvailable && downloadModel(
                    KITTEN_VOICES_URL,
                    File(modelsDir, "voices.npz"),
                    "Kitten Voices"
                )
            }
            
            // Check Kokoro model (optional, as it's large)
            if (!isModelAvailable("kokoro-v1.0.onnx")) {
                Log.d(TAG, "Kokoro model not available - using lightweight version")
                allAvailable = allAvailable && downloadModel(
                    KOKORO_MODEL_URL,
                    File(modelsDir, "kokoro-v1.0.onnx"),
                    "Kokoro TTS Model"
                )
            }
            
            // Check Kokoro voices
            if (!isModelAvailable("voices-v1.0.bin")) {
                Log.d(TAG, "Kokoro voices not available, attempting download")
                allAvailable = allAvailable && downloadModel(
                    KOKORO_VOICES_URL,
                    File(modelsDir, "voices-v1.0.bin"),
                    "Kokoro Voices"
                )
            }
            
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
     * Download models with progress callback
     */
    suspend fun downloadModelsWithProgress(callback: DownloadCallback): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelsDir = File(context.filesDir, "models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }
            
            val models = listOf(
                Triple("kitten_tts_nano_v0_1.onnx", KITTEN_MODEL_URL, "Kitten TTS Model"),
                Triple("voices.npz", KITTEN_VOICES_URL, "Kitten Voices"),
                Triple("kokoro-v1.0.onnx", KOKORO_MODEL_URL, "Kokoro TTS Model"),
                Triple("voices-v1.0.bin", KOKORO_VOICES_URL, "Kokoro Voices")
            )
            
            var allSuccessful = true
            
            models.forEachIndexed { index, (fileName, url, displayName) ->
                if (!isModelAvailable(fileName)) {
                    val success = downloadModel(
                        url,
                        File(modelsDir, fileName),
                        displayName,
                        object : DownloadCallback {
                            override fun onProgress(downloaded: Long, total: Long) {
                                callback.onProgress(downloaded, total)
                            }
                            
                            override fun onComplete(success: Boolean, message: String) {
                                callback.onComplete(success, "$displayName: $message")
                            }
                        }
                    )
                    allSuccessful = allSuccessful && success
                }
            }
            
            allSuccessful
            
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading models with progress", e)
            callback.onComplete(false, "Download failed: ${e.message}")
            false
        }
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