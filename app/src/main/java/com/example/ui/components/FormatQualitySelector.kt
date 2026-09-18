package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Sd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadFormat
import com.example.data.model.VideoQuality
import com.example.ui.theme.CrimsonPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormatQualitySelector(
    selectedFormat: DownloadFormat,
    selectedQuality: VideoQuality,
    onFormatSelected: (DownloadFormat) -> Unit,
    onQualitySelected: (VideoQuality) -> Unit,
    durationSeconds: Long = 180L,
    sizeProvider: ((DownloadFormat, VideoQuality) -> Long)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "1. FORMAT WÄHLEN",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Format Switcher Tabs (MP4 vs MP3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isMp4 = selectedFormat == DownloadFormat.MP4
            val isMp3 = selectedFormat == DownloadFormat.MP3

            val mp4Bg by animateColorAsState(
                targetValue = if (isMp4) CrimsonPrimary else Color.Transparent,
                animationSpec = tween(200),
                label = "mp4_bg"
            )
            val mp3Bg by animateColorAsState(
                targetValue = if (isMp3) CrimsonPrimary else Color.Transparent,
                animationSpec = tween(200),
                label = "mp3_bg"
            )

            // Video (MP4) Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(mp4Bg)
                    .clickable {
                        onFormatSelected(DownloadFormat.MP4)
                        if (selectedQuality.isAudioOnly) {
                            onQualitySelected(VideoQuality.RES_720P)
                        }
                    }
                    .padding(vertical = 12.dp)
                    .testTag("tab_format_mp4"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video",
                        tint = if (isMp4) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Video (MP4)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isMp4) FontWeight.Bold else FontWeight.Medium,
                        color = if (isMp4) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Audio (MP3) Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(mp3Bg)
                    .clickable {
                        onFormatSelected(DownloadFormat.MP3)
                        if (!selectedQuality.isAudioOnly) {
                            onQualitySelected(VideoQuality.AUDIO_320K)
                        }
                    }
                    .padding(vertical = 12.dp)
                    .testTag("tab_format_mp3"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "Audio",
                        tint = if (isMp3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Audio (MP3)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isMp3) FontWeight.Bold else FontWeight.Medium,
                        color = if (isMp3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (selectedFormat == DownloadFormat.MP4) "2. QUALITÄT WÄHLEN (360p - 1080p)" else "2. AUDIO-QUALITÄT WÄHLEN",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quality Options Flow
        val qualities = if (selectedFormat == DownloadFormat.MP4) {
            VideoQuality.videoQualities()
        } else {
            VideoQuality.audioQualities()
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            qualities.forEach { quality ->
                val isSelected = selectedQuality == quality
                val approxBytes = sizeProvider?.invoke(selectedFormat, quality)
                    ?: (quality.approxBitrateKbps * 1000L * durationSeconds.coerceAtLeast(1L) / 8)
                val approxSizeStr = if (approxBytes < 1024 * 1024) {
                    val kb = approxBytes / 1024f
                    String.format("%.0f KB", kb)
                } else {
                    val mb = approxBytes / (1024f * 1024f)
                    String.format("%.1f MB", mb)
                }

                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) CrimsonPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    label = "quality_border"
                )
                val cardBg by animateColorAsState(
                    targetValue = if (isSelected) CrimsonPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    label = "quality_bg"
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onQualitySelected(quality) }
                        .testTag("chip_quality_${quality.name}")
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Ausgewählt",
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Icon(
                                        imageVector = if (quality.height >= 720 || quality.approxBitrateKbps >= 320) Icons.Default.HighQuality else Icons.Default.Sd,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                Text(
                                    text = quality.resolution,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) CrimsonPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "ca. $approxSizeStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
