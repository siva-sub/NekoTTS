package com.nekotts.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nekotts.app.data.models.*
import com.nekotts.app.ui.viewmodel.VoicesViewModel
import com.nekotts.app.ui.viewmodel.ViewMode
import com.nekotts.app.ui.viewmodel.VoiceSortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicesScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: VoicesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        if (uiState.voices.isEmpty()) {
            viewModel.refreshVoices()
        }
    }
    
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
                title = { 
                    Column {
                        Text("Voices")
                        Text(
                            text = "${uiState.filteredVoices.size} of ${uiState.voices.size} voices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFilters() }) {
                        Icon(
                            imageVector = if (uiState.showFilters) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = if (uiState.showFilters) "Hide Filters" else "Show Filters"
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.setViewMode(
                                if (uiState.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                            )
                        }
                    ) {
                        Icon(
                            imageVector = if (uiState.viewMode == ViewMode.GRID) Icons.Default.List else Icons.Default.Menu,
                            contentDescription = if (uiState.viewMode == ViewMode.GRID) "List View" else "Grid View"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                label = { Text("Search voices...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Filters Panel
            if (uiState.showFilters) {
                FilterPanel(
                    uiState = uiState,
                    onEngineSelected = viewModel::selectEngine,
                    onLanguageSelected = viewModel::selectLanguage,
                    onGenderSelected = viewModel::selectGender,
                    onSortOptionSelected = viewModel::setSortOption,
                    onToggleDownloadedOnly = viewModel::toggleOnlyDownloaded,
                    onClearFilters = viewModel::clearFilters
                )
            }
            
            // Loading State
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading voices...")
                    }
                }
            } else {
                // Voice List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(uiState.filteredVoices) { voice ->
                        VoiceItem(
                            voice = voice,
                            isSelected = uiState.selectedVoice?.id == voice.id,
                            isDownloading = uiState.downloadingVoices.contains(voice.id),
                            downloadProgress = uiState.downloadProgress[voice.id] ?: 0f,
                            isPreviewing = uiState.previewingVoiceId == voice.id && uiState.isPreviewPlaying,
                            onVoiceSelected = { viewModel.selectVoice(voice.id) },
                            onDownload = { viewModel.downloadVoice(voice.id) },
                            onDelete = { viewModel.deleteVoice(voice.id) },
                            onPreview = { viewModel.previewVoice(voice.id) },
                            onStopPreview = { viewModel.stopPreview() },
                            onCancelDownload = { viewModel.cancelDownload(voice.id) },
                            viewMode = uiState.viewMode
                        )
                    }
                    
                    if (uiState.filteredVoices.isEmpty() && !uiState.isLoading) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No voices found",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Text(
                                        text = "Try adjusting your search or filters",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TextButton(
                                        onClick = viewModel::clearFilters
                                    ) {
                                        Text("Clear Filters")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterPanel(
    uiState: com.nekotts.app.ui.viewmodel.VoicesViewModel.UiState,
    onEngineSelected: (VoiceEngine?) -> Unit,
    onLanguageSelected: (String?) -> Unit,
    onGenderSelected: (VoiceGender?) -> Unit,
    onSortOptionSelected: (VoiceSortOption) -> Unit,
    onToggleDownloadedOnly: () -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearFilters) {
                    Text("Clear All")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Engine Filter
            FilterChipGroup(
                title = "Engine",
                options = uiState.availableEngines.map { it.displayName },
                selectedOption = uiState.selectedEngine?.displayName,
                onOptionSelected = { option ->
                    onEngineSelected(uiState.availableEngines.find { it.displayName == option })
                }
            )
            
            // Language Filter
            FilterChipGroup(
                title = "Language",
                options = uiState.availableLanguages.map { lang ->
                    when (lang) {
                        "en" -> "English"
                        "en-us" -> "English (US)"
                        "en-gb" -> "English (UK)"
                        "ja" -> "Japanese"
                        "zh" -> "Chinese"
                        "es" -> "Spanish"
                        "fr" -> "French"
                        "de" -> "German"
                        "it" -> "Italian"
                        "pt" -> "Portuguese"
                        "ru" -> "Russian"
                        "ko" -> "Korean"
                        "af" -> "Afrikaans"
                        "am" -> "Amharic"
                        else -> lang.uppercase()
                    }
                },
                selectedOption = uiState.selectedLanguage?.let { selectedLang ->
                    when (selectedLang) {
                        "en" -> "English"
                        "en-us" -> "English (US)"
                        "en-gb" -> "English (UK)"
                        "ja" -> "Japanese"
                        "zh" -> "Chinese"
                        "es" -> "Spanish"
                        "fr" -> "French"
                        "de" -> "German"
                        "it" -> "Italian"
                        "pt" -> "Portuguese"
                        "ru" -> "Russian"
                        "ko" -> "Korean"
                        "af" -> "Afrikaans"
                        "am" -> "Amharic"
                        else -> selectedLang.uppercase()
                    }
                },
                onOptionSelected = { option ->
                    val langCode = when (option) {
                        "English" -> "en"
                        "English (US)" -> "en-us"
                        "English (UK)" -> "en-gb"
                        "Japanese" -> "ja"
                        "Chinese" -> "zh"
                        "Spanish" -> "es"
                        "French" -> "fr"
                        "German" -> "de"
                        "Italian" -> "it"
                        "Portuguese" -> "pt"
                        "Russian" -> "ru"
                        "Korean" -> "ko"
                        "Afrikaans" -> "af"
                        "Amharic" -> "am"
                        else -> option?.lowercase()
                    }
                    onLanguageSelected(langCode)
                }
            )
            
            // Gender Filter
            FilterChipGroup(
                title = "Gender",
                options = VoiceGender.values().map { it.displayName },
                selectedOption = uiState.selectedGender?.displayName,
                onOptionSelected = { option ->
                    onGenderSelected(VoiceGender.values().find { it.displayName == option })
                }
            )
            
            // Sort Options
            FilterChipGroup(
                title = "Sort By",
                options = VoiceSortOption.values().map { it.displayName },
                selectedOption = uiState.sortBy.displayName,
                onOptionSelected = { option ->
                    onSortOptionSelected(VoiceSortOption.values().find { it.displayName == option } ?: VoiceSortOption.NAME)
                },
                singleSelection = true
            )
            
            // Downloaded Only Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleDownloadedOnly() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.onlyDownloaded,
                    onCheckedChange = { onToggleDownloadedOnly() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Show only downloaded voices")
            }
        }
    }
}

@Composable
fun FilterChipGroup(
    title: String,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String?) -> Unit,
    singleSelection: Boolean = false
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.take(3).forEach { option ->
                FilterChip(
                    selected = selectedOption == option,
                    onClick = {
                        if (singleSelection) {
                            onOptionSelected(option)
                        } else {
                            onOptionSelected(if (selectedOption == option) null else option)
                        }
                    },
                    label = { Text(option) }
                )
            }
            if (options.size > 3) {
                FilterChip(
                    selected = false,
                    onClick = { /* Show more dialog */ },
                    label = { Text("+${options.size - 3}") }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceItem(
    voice: Voice,
    isSelected: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    isPreviewing: Boolean,
    onVoiceSelected: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onPreview: () -> Unit,
    onStopPreview: () -> Unit,
    onCancelDownload: () -> Unit,
    viewMode: ViewMode
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVoiceSelected() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Voice Name
                        Text(
                            text = voice.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        if (voice.isDefault) {
                            Spacer(modifier = Modifier.width(8.dp))
                            AssistChip(
                                onClick = { },
                                label = { Text("Default", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Language and Engine
                    Text(
                        text = "${voice.languageName} • ${voice.engine.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    // Description
                    if (voice.description.isNotEmpty()) {
                        Text(
                            text = voice.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Characteristics
                    if (voice.characteristics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            voice.characteristics.take(3).forEach { characteristic ->
                                AssistChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = "${characteristic.emoji} ${characteristic.displayName}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }
                }
                
                // Action Buttons
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Download status
                    when {
                        isDownloading -> {
                            CircularProgressIndicator(
                                progress = downloadProgress,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            IconButton(
                                onClick = onCancelDownload,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Download",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        voice.isDownloaded -> {
                            Row {
                                IconButton(
                                    onClick = if (isPreviewing) onStopPreview else onPreview,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewing) Icons.Default.Clear else Icons.Default.PlayArrow,
                                        contentDescription = if (isPreviewing) "Stop Preview" else "Preview",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onDelete,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        else -> {
                            IconButton(
                                onClick = onDownload,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Download",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    // Voice quality indicator
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (voice.quality) {
                                    VoiceQuality.LOW -> Color(0xFFFF9800)
                                    VoiceQuality.STANDARD -> Color(0xFF2196F3)
                                    VoiceQuality.HIGH -> Color(0xFF4CAF50)
                                    VoiceQuality.PREMIUM -> Color(0xFF9C27B0)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = voice.quality.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}