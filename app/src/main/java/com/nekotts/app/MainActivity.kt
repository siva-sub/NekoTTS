package com.nekotts.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nekotts.app.ui.screens.HomeScreen
import com.nekotts.app.ui.screens.MainScreen
import com.nekotts.app.ui.screens.SettingsScreen
import com.nekotts.app.ui.screens.VoicesScreen
import com.nekotts.app.ui.theme.NekoTTSTheme
import com.nekotts.app.utils.NotificationHelper

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    // Permission launcher for notification permissions
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Notification permission granted: $isGranted")
        if (!isGranted) {
            Log.w(TAG, "Notification permission denied - notifications may not work properly")
        }
    }
    
    // Battery optimization exemption launcher
    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Battery optimization settings closed")
        checkBatteryOptimizationStatus()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this)
        
        // Request necessary permissions
        requestRequiredPermissions()
        
        // Request battery optimization exemption
        requestBatteryOptimizationExemption()
        
        // Handle intent for TTS settings
        handleTtsIntent()
        
        setContent {
            NekoTTSTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("main") {
                            MainScreen(
                                navController = navController,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                onNavigateToVoices = { navController.navigate("voices") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        composable("voices") {
                            VoicesScreen(
                                navController = navController,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                navController = navController,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun handleTtsIntent() {
        // Handle TTS engine configuration intent
        if (intent?.action == TextToSpeech.Engine.ACTION_TTS_DATA_INSTALLED) {
            // TTS data installed, we can proceed
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTtsIntent()
    }
    
    /**
     * Request notification permission for Android 13+
     */
    private fun requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if notification permission is already granted
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasNotificationPermission) {
                Log.d(TAG, "Requesting notification permission")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Log.d(TAG, "Notification permission already granted")
            }
        } else {
            Log.d(TAG, "Notification permission not required for this Android version")
        }
    }
    
    /**
     * Request battery optimization exemption
     */
    private fun requestBatteryOptimizationExemption() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val packageName = packageName
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.d(TAG, "Requesting battery optimization exemption")
                
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    batteryOptimizationLauncher.launch(intent)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to request battery optimization exemption", e)
                    // Fallback to general battery optimization settings
                    try {
                        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        batteryOptimizationLauncher.launch(fallbackIntent)
                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed to open battery optimization settings", e2)
                    }
                }
            } else {
                Log.d(TAG, "Battery optimization exemption already granted")
            }
        } else {
            Log.d(TAG, "Battery optimization not available for this Android version")
        }
    }
    
    /**
     * Check and log current battery optimization status
     */
    private fun checkBatteryOptimizationStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            val isIgnoring = powerManager.isIgnoringBatteryOptimizations(packageName)
            Log.d(TAG, "Battery optimization exemption status: $isIgnoring")
            
            if (!isIgnoring) {
                Log.w(TAG, "App is still subject to battery optimization - background TTS may be affected")
            }
        }
    }
}