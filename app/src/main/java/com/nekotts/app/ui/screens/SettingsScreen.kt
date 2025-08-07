package com.nekotts.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nekotts.app.data.models.*
import com.nekotts.app.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Show error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // In a real app, show snackbar
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Voice Settings Section
            item {
                SettingsSection(title = "Voice Settings") {
                    VoiceSettingsContent(
                        settings = uiState.settings,
                        availableEngines = uiState.availableEngines,
                        onEngineSelected = viewModel::updateDefaultEngine,
                        onSpeedChanged = viewModel::updateSpeechSpeed,
                        onPitchChanged = viewModel::updateSpeechPitch
                    )
                }
            }
            
            // Audio Settings Section
            item {
                SettingsSection(title = "Audio Settings") {
                    AudioSettingsContent(
                        settings = uiState.settings,
                        availableAudioStreams = uiState.availableAudioStreams,
                        onAudioStreamSelected = viewModel::updateAudioStreamType,
                        onAudioFocusToggle = viewModel::toggleAudioFocus,
                        onAudioFadeToggle = viewModel::toggleAudioFade
                    )
                }
            }
            
            // UI Settings Section
            item {
                SettingsSection(title = "UI Settings") {
                    UISettingsContent(
                        settings = uiState.settings,
                        availableThemes = uiState.availableThemes,
                        onThemeSelected = viewModel::updateThemeMode,
                        onDynamicColorsToggle = viewModel::toggleDynamicColors,
                        onHapticFeedbackToggle = viewModel::toggleHapticFeedback,
                        onAnimationsToggle = viewModel::toggleAnimations
                    )
                }
            }
            
            // Accessibility Settings Section
            item {
                SettingsSection(title = "Accessibility") {
                    AccessibilitySettingsContent(
                        settings = uiState.settings,
                        onHighContrastToggle = viewModel::toggleHighContrast,
                        onLargeTextToggle = viewModel::toggleLargeText,
                        onReduceMotionToggle = viewModel::toggleReduceMotion
                    )
                }
            }
            
            // Performance Settings Section
            item {
                SettingsSection(title = "Performance") {
                    PerformanceSettingsContent(
                        settings = uiState.settings,
                        onGPUAccelerationToggle = viewModel::toggleGPUAcceleration,
                        onBatteryOptimizationToggle = viewModel::toggleBatteryOptimization,
                        onMaxConcurrentSynthesisChanged = viewModel::updateMaxConcurrentSynthesis
                    )
                }
            }
            
            // Privacy Settings Section
            item {
                SettingsSection(title = "Privacy") {
                    PrivacySettingsContent(
                        settings = uiState.settings,
                        onAnalyticsToggle = viewModel::toggleAnalytics,
                        onCrashReportingToggle = viewModel::toggleCrashReporting,
                        onDataCollectionToggle = viewModel::toggleDataCollection
                    )
                }
            }
            
            // Advanced Settings Section
            item {
                SettingsSection(title = "Advanced") {
                    AdvancedSettingsContent(
                        settings = uiState.settings,
                        onDebugModeToggle = viewModel::toggleDebugMode,
                        onVerboseLoggingToggle = viewModel::toggleVerboseLogging
                    )
                }
            }
            
            // Presets Section
            item {
                SettingsSection(title = "Presets") {
                    PresetsContent(
                        onApplyAccessibilityPreset = viewModel::applyAccessibilityPreset,
                        onApplyPerformancePreset = viewModel::applyPerformancePreset,
                        onApplyPrivacyPreset = viewModel::applyPrivacyPreset,
                        onApplyBatteryPreset = viewModel::applyBatteryOptimizedPreset,
                        onResetToDefaults = viewModel::resetToDefaults
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun VoiceSettingsContent(
    settings: Settings,
    availableEngines: List<VoiceEngine>,
    onEngineSelected: (VoiceEngine) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onPitchChanged: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Engine Selection
        Text(
            text = "TTS Engine",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Column(modifier = Modifier.selectableGroup()) {
            availableEngines.forEach { engine ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = settings.defaultEngine == engine,
                            onClick = { onEngineSelected(engine) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.defaultEngine == engine,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = engine.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = when (engine) {
                                VoiceEngine.KITTEN -> "Custom anime-style voices"
                                VoiceEngine.KOKORO -> "Multi-language neural voices"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        
        HorizontalDivider()
        
        // Speech Speed
        Text(
            text = "Speech Speed: ${(settings.speechSpeed * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Slider(
            value = settings.speechSpeed,
            onValueChange = onSpeedChanged,
            valueRange = 0.1f..3.0f,
            steps = 28,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Speech Pitch
        Text(
            text = "Speech Pitch: ${(settings.speechPitch * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Slider(
            value = settings.speechPitch,
            onValueChange = onPitchChanged,
            valueRange = 0.5f..2.0f,
            steps = 14,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AudioSettingsContent(
    settings: Settings,
    availableAudioStreams: List<AudioStreamType>,
    onAudioStreamSelected: (AudioStreamType) -> Unit,
    onAudioFocusToggle: () -> Unit,
    onAudioFadeToggle: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Audio Stream Type
        Text(
            text = "Audio Stream Type",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Column(modifier = Modifier.selectableGroup()) {
            availableAudioStreams.forEach { streamType ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = settings.audioStreamType == streamType,
                            onClick = { onAudioStreamSelected(streamType) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.audioStreamType == streamType,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = streamType.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        HorizontalDivider()
        
        // Audio Focus
        SettingsToggleItem(
            title = "Audio Focus",
            description = "Request audio focus when speaking",
            checked = settings.audioFocusEnabled,
            onToggle = onAudioFocusToggle
        )
        
        // Audio Fade
        SettingsToggleItem(
            title = "Audio Fade",
            description = "Fade in/out audio playback",
            checked = settings.audioFadeEnabled,
            onToggle = onAudioFadeToggle
        )
    }
}

@Composable
fun UISettingsContent(
    settings: Settings,
    availableThemes: List<ThemeMode>,
    onThemeSelected: (ThemeMode) -> Unit,
    onDynamicColorsToggle: () -> Unit,
    onHapticFeedbackToggle: () -> Unit,
    onAnimationsToggle: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Theme Selection
        Text(
            text = "Theme",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Column(modifier = Modifier.selectableGroup()) {
            availableThemes.forEach { theme ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = settings.themeMode == theme,
                            onClick = { onThemeSelected(theme) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.themeMode == theme,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = theme.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        HorizontalDivider()
        
        // UI Toggles
        SettingsToggleItem(
            title = "Dynamic Colors",
            description = "Use system accent colors",
            checked = settings.dynamicColorsEnabled,
            onToggle = onDynamicColorsToggle
        )
        
        SettingsToggleItem(
            title = "Haptic Feedback",
            description = "Vibrate on interactions",
            checked = settings.hapticFeedbackEnabled,
            onToggle = onHapticFeedbackToggle
        )
        
        SettingsToggleItem(
            title = "Animations",
            description = "Enable UI animations",
            checked = settings.animationsEnabled,
            onToggle = onAnimationsToggle
        )
    }
}

@Composable
fun AccessibilitySettingsContent(
    settings: Settings,
    onHighContrastToggle: () -> Unit,
    onLargeTextToggle: () -> Unit,
    onReduceMotionToggle: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsToggleItem(
            title = "High Contrast Mode",
            description = "Increase color contrast for better visibility",
            checked = settings.highContrastMode,
            onToggle = onHighContrastToggle
        )
        
        SettingsToggleItem(
            title = "Large Text Mode",
            description = "Increase text size throughout the app",
            checked = settings.largeTextMode,
            onToggle = onLargeTextToggle
        )
        
        SettingsToggleItem(
            title = "Reduce Motion",
            description = "Minimize animations and transitions",
            checked = settings.reduceMotion,
            onToggle = onReduceMotionToggle
        )
    }
}

@Composable
fun PerformanceSettingsContent(
    settings: Settings,
    onGPUAccelerationToggle: () -> Unit,
    onBatteryOptimizationToggle: () -> Unit,
    onMaxConcurrentSynthesisChanged: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsToggleItem(
            title = "GPU Acceleration",
            description = "Use GPU for faster processing",
            checked = settings.useGPUAcceleration,
            onToggle = onGPUAccelerationToggle
        )
        
        SettingsToggleItem(
            title = "Battery Optimization",
            description = "Reduce power consumption",
            checked = settings.optimizeForBattery,
            onToggle = onBatteryOptimizationToggle
        )
        
        HorizontalDivider()
        
        Text(
            text = "Max Concurrent Synthesis: ${settings.maxConcurrentSynthesis}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Slider(
            value = settings.maxConcurrentSynthesis.toFloat(),
            onValueChange = { onMaxConcurrentSynthesisChanged(it.roundToInt()) },
            valueRange = 1f..3f,
            steps = 1,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Controls how many voice synthesis operations can run simultaneously",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun PrivacySettingsContent(
    settings: Settings,
    onAnalyticsToggle: () -> Unit,
    onCrashReportingToggle: () -> Unit,
    onDataCollectionToggle: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsToggleItem(
            title = "Analytics",
            description = "Help improve the app with usage analytics",
            checked = settings.analyticsEnabled,
            onToggle = onAnalyticsToggle
        )
        
        SettingsToggleItem(
            title = "Crash Reporting",
            description = "Automatically report crashes to help fix bugs",
            checked = settings.crashReportingEnabled,
            onToggle = onCrashReportingToggle
        )
        
        SettingsToggleItem(
            title = "Data Collection",
            description = "Allow collection of usage data",
            checked = settings.dataCollectionEnabled,
            onToggle = onDataCollectionToggle
        )
    }
}

@Composable
fun AdvancedSettingsContent(
    settings: Settings,
    onDebugModeToggle: () -> Unit,
    onVerboseLoggingToggle: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsToggleItem(
            title = "Debug Mode",
            description = "Enable debug features and diagnostics",
            checked = settings.debugMode,
            onToggle = onDebugModeToggle
        )
        
        SettingsToggleItem(
            title = "Verbose Logging",
            description = "Enable detailed application logs",
            checked = settings.verboseLogging,
            onToggle = onVerboseLoggingToggle
        )
    }
}

@Composable
fun PresetsContent(
    onApplyAccessibilityPreset: () -> Unit,
    onApplyPerformancePreset: () -> Unit,
    onApplyPrivacyPreset: () -> Unit,
    onApplyBatteryPreset: () -> Unit,
    onResetToDefaults: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Apply a preset configuration to quickly adjust multiple settings",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onApplyAccessibilityPreset,
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Text("Accessibility", style = MaterialTheme.typography.labelSmall)
                }
            }
            
            OutlinedButton(
                onClick = onApplyPerformancePreset,
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text("Performance", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onApplyPrivacyPreset,
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Text("Privacy", style = MaterialTheme.typography.labelSmall)
                }
            }
            
            OutlinedButton(
                onClick = onApplyBatteryPreset,
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Text("Battery", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onResetToDefaults,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset All Settings")
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
    }
}
