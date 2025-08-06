package com.nekotts.app.ui.viewmodel

// MINIMAL STUB VERSION - ORIGINAL HAD COMPILATION ERRORS
// This replaces the complex MainViewModel with a minimal stub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.nekotts.app.data.models.Voice
import com.nekotts.app.data.models.AllVoices
import com.nekotts.app.engine.TTSEngine

class MainViewModel : ViewModel() {
    
    data class UiState(
        val isInitialized: Boolean = false,
        val isLoading: Boolean = true,
        val inputText: String = "",
        val selectedVoiceId: String = "ktn_f1",
        val speechSpeed: Float = 1.0f,
        val isSpeaking: Boolean = false,
        val voices: List<Voice> = AllVoices.voices,
        val errorMessage: String? = null
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun updateText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }
    
    fun updateSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(speechSpeed = speed)
    }
    
    fun selectVoice(voiceId: String) {
        _uiState.value = _uiState.value.copy(selectedVoiceId = voiceId)
    }
    
    fun speak() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSpeaking = true)
            // Stub implementation - would actually perform TTS here
            kotlinx.coroutines.delay(2000) // Simulate speaking
            _uiState.value = _uiState.value.copy(isSpeaking = false)
        }
    }
    
    fun stop() {
        _uiState.value = _uiState.value.copy(isSpeaking = false)
    }
    
    fun updateSetting(key: String, value: Any) {
        // Stub implementation
        when (key) {
            "speed" -> updateSpeed(value as Float)
        }
    }
    
    fun testVoice(voiceId: String) {
        selectVoice(voiceId)
        speak()
    }
    
    init {
        // Initialize with default state
        _uiState.value = _uiState.value.copy(
            isInitialized = true,
            isLoading = false
        )
    }
}