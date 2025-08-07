package com.nekotts.app.service

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.nekotts.app.R
import com.nekotts.app.data.VoiceRepository
import com.nekotts.app.data.SettingsRepository
import com.nekotts.app.core.AppSingletons
import com.nekotts.app.service.TTSSessionManager
import com.nekotts.app.utils.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Activity that handles "Read Aloud" functionality from other apps
 * Shows a floating dialog while processing text
 */
class ProcessTextActivity : Activity() {
    
    private lateinit var voiceRepository: VoiceRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var ttsSessionManager: TTSSessionManager
    
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var processingDialog: Dialog? = null
    
    companion object {
        private const val TAG = "ProcessTextActivity"
        const val EXTRA_PROCESS_TEXT = Intent.EXTRA_PROCESS_TEXT
        const val EXTRA_PROCESS_TEXT_READONLY = Intent.EXTRA_PROCESS_TEXT_READONLY
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Initialize dependencies from singleton manager with error handling
            voiceRepository = AppSingletons.getVoiceRepository()
            settingsRepository = AppSingletons.getSettingsRepository()
            ttsSessionManager = AppSingletons.getTTSSessionManager()
            
            Log.d(TAG, "ProcessTextActivity started")
            
            // Get the selected text from the intent
            val selectedText = intent.getCharSequenceExtra(EXTRA_PROCESS_TEXT)?.toString()
            
            if (selectedText.isNullOrEmpty()) {
                Log.w(TAG, "No text provided for processing")
                showToast("No text selected")
                finish()
                return
            }
            
            if (selectedText.length > Constants.MAX_TEXT_LENGTH) {
                Log.w(TAG, "Text too long: ${selectedText.length} characters")
                showToast("Text too long (max ${Constants.MAX_TEXT_LENGTH} characters)")
                finish()
                return
            }
            
            Log.d(TAG, "Processing text: ${selectedText.take(100)}...")
            
            // Show floating processing dialog
            showProcessingDialog(selectedText)
            
            // Process the text with TTS
            processText(selectedText)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ProcessTextActivity", e)
            showToast("TTS service not available: ${e.message}")
            finish()
        }
    }
    
    private fun showProcessingDialog(text: String) {
        processingDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            
            // Create layout
            val layout = LinearLayout(this@ProcessTextActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 48, 48, 48)
                gravity = Gravity.CENTER
                
                // Progress bar
                addView(ProgressBar(this@ProcessTextActivity).apply {
                    isIndeterminate = true
                    indeterminateTintList = ContextCompat.getColorStateList(
                        this@ProcessTextActivity, 
                        R.color.neko_purple
                    )
                })
                
                // Title text
                addView(TextView(this@ProcessTextActivity).apply {
                    this.text = "Reading with Neko TTS..."
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding(0, 32, 0, 16)
                    setTextColor(ContextCompat.getColor(this@ProcessTextActivity, R.color.neko_navy))
                })
                
                // Preview text
                addView(TextView(this@ProcessTextActivity).apply {
                    this.text = "\"${text.take(100)}${if (text.length > 100) "..." else ""}\""
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, 16)
                    setTextColor(ContextCompat.getColor(this@ProcessTextActivity, R.color.neko_light_purple))
                })
                
                // Cancel hint
                addView(TextView(this@ProcessTextActivity).apply {
                    this.text = "Tap outside to cancel"
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setTextColor(ContextCompat.getColor(this@ProcessTextActivity, android.R.color.darker_gray))
                })
            }
            
            setContentView(layout)
            
            // Style the dialog
            window?.let { window ->
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window.setLayout(
                    (resources.displayMetrics.widthPixels * 0.8).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                window.setGravity(Gravity.CENTER)
                
                // Make it floating
                val params = window.attributes
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                params.dimAmount = 0.3f
                window.attributes = params
            }
            
            setCanceledOnTouchOutside(true)
            setOnCancelListener {
                Log.d(TAG, "Processing cancelled by user")
                activityScope.cancel()
                finish()
            }
            
            show()
        }
    }
    
    private fun processText(text: String) {
        activityScope.launch {
            try {
                // Get current settings and voice
                val settings = settingsRepository.getCurrentSettings().first()
                val selectedVoice = voiceRepository.getSelectedVoice().first()
                
                if (selectedVoice == null) {
                    showToast("No voice selected")
                    finish()
                    return@launch
                }
                
                Log.d(TAG, "Using voice: ${selectedVoice.displayName}, speed: ${settings.speechSpeed}")
                
                // Create TTS session
                val session = ttsSessionManager.createSession(
                    text,
                    selectedVoice.id,
                    settings.speechSpeed,
                    settings.speechPitch,
                    SessionPriority.HIGH
                )
                
                // Monitor session progress
                val sessionJob = launch {
                    while (isActive) {
                        val currentSession = ttsSessionManager.getSession(session.id)
                        if (currentSession?.isCompleted == true) {
                            when (currentSession.status) {
                                SessionStatus.COMPLETED -> {
                                    showToast("Reading completed")
                                    Log.d(TAG, "TTS session completed successfully")
                                }
                                SessionStatus.FAILED -> {
                                    showToast("Reading failed: ${currentSession.error}")
                                    Log.e(TAG, "TTS session failed: ${currentSession.error}")
                                }
                                SessionStatus.CANCELLED -> {
                                    Log.d(TAG, "TTS session cancelled")
                                }
                                else -> {
                                    Log.w(TAG, "Unexpected session status: ${currentSession.status}")
                                }
                            }
                            break
                        }
                        delay(500)
                    }
                }
                
                // Start the session
                val result = ttsSessionManager.startSession(session.id)
                if (result.isFailure) {
                    showToast("Failed to start reading: ${result.exceptionOrNull()?.message}")
                    Log.e(TAG, "Failed to start TTS session", result.exceptionOrNull())
                    finish()
                    return@launch
                }
                
                // Wait for session to complete
                sessionJob.join()
                
                // Clean up and close
                delay(1000) // Brief delay to show completion message
                
            } catch (e: CancellationException) {
                Log.d(TAG, "Processing cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing text", e)
                showToast("Error: ${e.message}")
            } finally {
                processingDialog?.dismiss()
                finish()
            }
        }
    }
    
    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ProcessTextActivity destroyed")
        
        processingDialog?.dismiss()
        activityScope.cancel()
    }
    
    override fun onPause() {
        super.onPause()
        // Keep the activity running in background while TTS is active
        // The session manager will handle the actual TTS processing
    }
}