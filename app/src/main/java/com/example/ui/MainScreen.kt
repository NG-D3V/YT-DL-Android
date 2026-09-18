package com.example.ui

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.engine.YtDlpExtractorEngine
import com.example.ui.components.DownloadItemCard
import com.example.ui.components.MediaPlayerDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.VideoPreviewCard
import com.example.ui.components.formatBytes
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allDownloads by viewModel.downloads.collectAsStateWithLifecycle()
    val filteredDownloads by viewModel.filteredDownloads.collectAsStateWithLifecycle()

    val activeDownloadsCount = remember(allDownloads) {
        allDownloads.count {
            it.status == DownloadStatus.DOWNLOADING ||
            it.status == DownloadStatus.QUEUED ||
            it.status == DownloadStatus.PAUSED
        }
    }

    // Handle snackbar messages
    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearInfoMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CrimsonPrimary,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "YT Downloader",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "yt-dlp Core (Lokal)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Active Downloads pill
                    if (activeDownloadsCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = CrimsonPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Downloading,
                                    contentDescription = null,
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$activeDownloadsCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonPrimary
                                )
                            }
                        }
                    }

                    // Settings Button
                    IconButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier.testTag("btn_top_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Einstellungen"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. URL Input & Action Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "YouTube Video / Audio herunterladen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Link einfügen für lokale MP4- & MP3-Konvertierung",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // URL TextField
                        OutlinedTextField(
                            value = uiState.urlInput,
                            onValueChange = { viewModel.onUrlChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_youtube_url"),
                            placeholder = { Text("https://www.youtube.com/watch?v=...") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    viewModel.analyzeUrl()
                                }
                            ),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (uiState.urlInput.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                focusManager.clearFocus()
                                                viewModel.onUrlChange("")
                                            },
                                            modifier = Modifier.size(32.dp).testTag("btn_clear_url")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Löschen",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Clipboard Paste Button
                                    IconButton(
                                        onClick = {
                                            focusManager.clearFocus()
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = clipboard.primaryClip
                                            if (clip != null && clip.itemCount > 0) {
                                                val text = clip.getItemAt(0).text?.toString() ?: ""
                                                if (text.isNotEmpty()) {
                                                    viewModel.pasteAndAnalyze(text)
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(36.dp).testTag("btn_paste_url")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Einfügen",
                                            tint = CrimsonPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CrimsonPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Analyze Button
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                viewModel.analyzeUrl()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_analyze_url"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !uiState.isAnalyzing && uiState.urlInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                        ) {
                            if (uiState.isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analysiere mit yt-dlp...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Video analysieren",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Demo Quick Clips
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Schnelltest Beispiele:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            YtDlpExtractorEngine.DEMO_VIDEOS.forEachIndexed { index, (demoUrl, demoTitle) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.pasteAndAnalyze(demoUrl)
                                        }
                                        .testTag("chip_demo_$index")
                                ) {
                                    Text(
                                        text = demoTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Analysis Error Notice
                        if (uiState.analysisError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ErrorRed.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = ErrorRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = uiState.analysisError ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ErrorRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Video Preview & Quality Selector (If parsed)
            if (uiState.parsedMetadata != null) {
                item {
                    VideoPreviewCard(
                        metadata = uiState.parsedMetadata!!,
                        selectedFormat = uiState.selectedFormat,
                        selectedQuality = uiState.selectedQuality,
                        downloadPathName = viewModel.getDownloadDirectoryDisplayName(),
                        sizeProvider = { fmt, q -> viewModel.getEstimatedSizeBytes(fmt, q) },
                        onFormatChange = { viewModel.selectFormat(it) },
                        onQualityChange = { viewModel.selectQuality(it) },
                        onSelectLocation = { viewModel.openSettings() },
                        onStartDownload = { viewModel.startDownload() }
                    )
                }
            }

            // 3. Downloads Management Section Header & Filters
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Downloads (${allDownloads.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (allDownloads.isNotEmpty()) {
                            Text(
                                text = "${formatBytes(allDownloads.sumOf { it.fileSize.takeIf { s -> s > 0 } ?: it.downloadedBytes })} belegt",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search bar
                    if (allDownloads.isNotEmpty()) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_search_downloads"),
                            placeholder = { Text("Downloads durchsuchen...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Löschen")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CrimsonPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Filter Tabs Flow
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DownloadFilter.entries.forEach { filter ->
                            val isSelected = uiState.filterTab == filter
                            val count = when (filter) {
                                DownloadFilter.ALL -> allDownloads.size
                                DownloadFilter.ACTIVE -> activeDownloadsCount
                                DownloadFilter.COMPLETED -> allDownloads.count { it.status == DownloadStatus.COMPLETED }
                                DownloadFilter.VIDEOS -> allDownloads.count { it.format == com.example.data.model.DownloadFormat.MP4 }
                                DownloadFilter.AUDIO -> allDownloads.count { it.format == com.example.data.model.DownloadFormat.MP3 }
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFilterTab(filter) },
                                label = { Text("${filter.title} ($count)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("filter_chip_${filter.name}")
                            )
                        }
                    }
                }
            }

            // 4. Downloads List Items
            if (filteredDownloads.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty()) "Keine Downloads für '${uiState.searchQuery}'" else "Noch keine Downloads vorhanden",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Füge oben einen YouTube-Link ein, um Video (MP4) oder Audio (MP3) herunterzuladen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredDownloads, key = { it.id }) { download ->
                    DownloadItemCard(
                        download = download,
                        onPause = { viewModel.pauseDownload(it) },
                        onResume = { viewModel.resumeDownload(it) },
                        onCancel = { viewModel.cancelDownload(it) },
                        onDelete = { viewModel.deleteDownload(it) },
                        onPlay = { viewModel.openPlayer(it) },
                        onShare = { item ->
                            shareDownloadedMedia(context, item)
                        }
                    )
                }
            }
        }

        // In-App Media Player Dialog
        uiState.playingDownload?.let { download ->
            MediaPlayerDialog(
                download = download,
                onDismiss = { viewModel.closePlayer() },
                onShare = { shareDownloadedMedia(context, it) }
            )
        }

        // Settings Dialog
        if (uiState.showSettings) {
            val totalBytes = remember(allDownloads) {
                allDownloads.sumOf { it.fileSize.takeIf { s -> s > 0 } ?: it.downloadedBytes }
            }
            SettingsDialog(
                downloadCount = allDownloads.size,
                totalStorageBytes = totalBytes,
                downloadPath = viewModel.getDownloadDirectoryPath(),
                storageLocations = viewModel.getStorageLocationOptions(),
                selectedLocationKey = viewModel.getSelectedStorageLocationKey(),
                onSelectLocation = { viewModel.setStorageLocation(it) },
                onPickCustomFolder = { uri, name -> viewModel.setCustomDirectoryUri(uri, name) },
                onClearAll = { viewModel.clearAllDownloads() },
                onDismiss = { viewModel.closeSettings() }
            )
        }
    }
}

fun shareDownloadedMedia(context: Context, download: DownloadEntity) {
    if (download.localFilePath.isNotEmpty()) {
        val file = File(download.localFilePath)
        if (file.exists()) {
            runCatching {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = download.mimeType.ifEmpty { if (download.format == com.example.data.model.DownloadFormat.MP4) "video/mp4" else "audio/mpeg" }
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, download.title)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Heruntergeladene Datei teilen"))
                return
            }
        }
    }

    // Fallback share source URL
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${download.title}\n${download.sourceUrl}")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Video-Link teilen"))
}
