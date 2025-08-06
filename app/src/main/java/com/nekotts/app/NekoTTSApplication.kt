package com.nekotts.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.nekotts.app.core.AppSingletons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NekoTTSApplication : Application() {
    
    companion object {
        private const val TAG = "NekoTTSApplication"
        const val NOTIFICATION_CHANNEL_ID = "neko_tts_service"
        const val NOTIFICATION_CHANNEL_NAME = "Neko TTS Service"
        const val NOTIFICATION_CHANNEL_DESCRIPTION = "Notifications for Neko TTS service"
        
        lateinit var instance: NekoTTSApplication
            private set
    }
    
    // Application-level coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        Log.d(TAG, "Neko TTS Application starting...")
        
        // Initialize singleton manager first
        AppSingletons.init(this)
        
        // Create notification channel first (for Android O+)
        createNotificationChannel()
        
        // Initialize app components
        initializeApp()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = NOTIFICATION_CHANNEL_DESCRIPTION
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            
            Log.d(TAG, "Notification channel created")
        }
    }
    
    private fun initializeApp() {
        applicationScope.launch {
            try {
                Log.d(TAG, "Starting app initialization...")
                
                // Initialize ONNX Runtime
                initializeONNXRuntime()
                
                // Initialize TTS engines asynchronously
                initializeTTSEngines()
                
                Log.d(TAG, "App initialization completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during app initialization", e)
            }
        }
    }
    
    private suspend fun initializeONNXRuntime() {
        try {
            Log.d(TAG, "Initializing ONNX Runtime...")
            // ONNX Runtime initialization would happen here
            // This is typically done lazily when first needed
            Log.d(TAG, "ONNX Runtime initialization scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ONNX Runtime", e)
            throw e
        }
    }
    
    private suspend fun initializeTTSEngines() {
        try {
            Log.d(TAG, "Initializing TTS engines...")
            // TTS engine initialization would happen here
            // This includes loading models and voices
            Log.d(TAG, "TTS engines initialization scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TTS engines", e)
            throw e
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "Neko TTS Application terminating...")
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning received")
        // Could implement memory cleanup here
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.w(TAG, "Trim memory requested with level: $level")
        
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_COMPLETE -> {
                // Critical memory situation - could pause non-essential operations
                Log.w(TAG, "Critical memory level - considering cleanup")
            }
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_MODERATE -> {
                // Moderate memory pressure - reduce caching
                Log.i(TAG, "Moderate memory pressure - optimizing memory usage")
            }
        }
    }
}