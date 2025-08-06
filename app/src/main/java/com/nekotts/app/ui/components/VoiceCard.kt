package com.nekotts.app.ui.components

// MINIMAL STUB VERSION - ORIGINAL HAD COMPILATION ERRORS
// This replaces the complex VoiceCard component with a minimal stub

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nekotts.app.data.models.Voice
import com.nekotts.app.engine.TTSEngine

// For Voice data model
@Composable
fun VoiceCard(
    voice: Voice,
    isSelected: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    onSelect: () -> Unit = {},
    onDownload: () -> Unit = {},
    onPreview: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = voice.displayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${voice.languageName} - ${voice.gender.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                if (voice.isDownloaded) {
                    IconButton(onClick = onPreview) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Preview"
                        )
                    }
                }
            }
            
            if (voice.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = voice.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Button(
                    onClick = onSelect,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isSelected) "Selected" else "Select")
                }
                if (!voice.isDownloaded && voice.engine.displayName == "Kokoro") {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = onDownload) {
                        Text("Download")
                    }
                }
            }
        }
    }
}

// For TTSEngine.Voice
@Composable  
fun VoiceCard(
    voice: TTSEngine.Voice,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = voice.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${voice.language} - ${voice.gender}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (voice.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = voice.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSelected) "Selected" else "Select")
            }
        }
    }
}

// Stub for compact voice card
@Composable
fun CompactVoiceCard(
    voice: Voice,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onPreview: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Simple implementation
    VoiceCard(
        voice = voice,
        isSelected = isSelected,
        onSelect = onSelect,
        onPreview = onPreview,
        modifier = modifier
    )
}