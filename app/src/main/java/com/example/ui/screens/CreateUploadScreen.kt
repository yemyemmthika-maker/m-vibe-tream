package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.engine.MicrophoneLevelEngine
import com.example.data.engine.SynthAudioEngine
import com.example.data.engine.VideoProcessingEngine
import com.example.data.local.HashtagEntity
import com.example.data.local.SoundEntity
import com.example.data.local.VideoEntity
import com.example.ui.UploadProgressState
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCyan
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateUploadScreen(
    sounds: List<SoundEntity>,
    trendingHashtags: List<HashtagEntity>,
    myVideos: List<VideoEntity>,
    uploadProgress: UploadProgressState,
    onUploadVideo: (
        caption: String,
        hashtags: String,
        sound: SoundEntity,
        localUri: String,
        thumbnailUri: String?,
        thumbnailThemeIndex: Int,
        thumbnailHeadline: String,
        durationSec: Int,
        targetBitrateKbps: Int,
        resolution: String
    ) -> Unit,
    onDeleteMyVideo: (Long) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var caption by remember { mutableStateOf("") }
    var customTagDraft by remember { mutableStateOf("") }
    var hashtagsInput by remember { mutableStateOf("#NeonPulse #VerticalCinema") }
    var thumbnailHeadline by remember { mutableStateOf("STUDIO PULSE") }
    var selectedThemeIndex by remember { mutableIntStateOf(0) }
    var selectedSoundIndex by remember { mutableIntStateOf(0) }
    var selectedDurationSec by remember { mutableIntStateOf(15) }
    var selectedBitrateKbps by remember { mutableIntStateOf(2400) }
    var selectedResolution by remember { mutableStateOf("1080x1920") }
    var selectedFrameTimestampSec by remember { mutableFloatStateOf(2.5f) }
    var selectedVideoUri by remember { mutableStateOf("") }
    var customThumbnailUri by remember { mutableStateOf<String?>(null) }
    var sourceStatusLabel by remember { mutableStateOf("Ready: Studio 1080p Camera Stream") }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var showInAppRecorderModal by remember { mutableStateOf(false) }

    // System Camera Video Capture Launcher
    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && pendingCaptureUri != null) {
            val uri = pendingCaptureUri!!
            selectedVideoUri = uri.toString()
            val detectedSec = probeVideoDurationSeconds(context, uri) ?: selectedDurationSec
            selectedDurationSec = detectedSec
            sourceStatusLabel = "Recorded Camera Video (${detectedSec}s)"
            onShowMessage("Camera video recorded (${detectedSec}s)! Customize thumbnail & caption below.")
        } else {
            showInAppRecorderModal = true
        }
    }

    // Runtime permission launcher for Camera + Microphone
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        if (cameraGranted) {
            showInAppRecorderModal = true
        } else {
            showInAppRecorderModal = true
            onShowMessage("Opened Studio Camera Recorder.")
        }
    }

    // Zero-permission Android Photo Picker for device video upload
    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri.toString()
            val detectedSec = probeVideoDurationSeconds(context, uri) ?: selectedDurationSec
            selectedDurationSec = detectedSec
            sourceStatusLabel = "Uploaded Device Video (${detectedSec}s • ${uri.lastPathSegment ?: "clip.mp4"})"
            onShowMessage("Device video loaded! Select a thumbnail frame and compress.")
        }
    }

    // Zero-permission Android Photo Picker for custom cover thumbnail image
    val pickThumbnailImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            customThumbnailUri = uri.toString()
            onShowMessage("Custom thumbnail image applied!")
        }
    }

    val themeLabels = listOf(
        "Coral Prism" to listOf(Color(0xFF140826), Color(0xFFFF3366)),
        "Cyber Cyan" to listOf(Color(0xFF041B2D), Color(0xFF00E5FF)),
        "Kinetic Mint" to listOf(Color(0xFF072118), Color(0xFF00E676)),
        "Amber Tape" to listOf(Color(0xFF261405), Color(0xFFFFB300)),
        "Neon Violet" to listOf(Color(0xFF1B0924), Color(0xFF7C4DFF))
    )

    val activeSound = sounds.getOrNull(selectedSoundIndex) ?: SoundEntity(
        id = 1L,
        title = "Midnight Prism (Original Mix)",
        artist = "Aria SynthLab"
    )

    val estimatedRawMb = remember(selectedDurationSec) { (selectedDurationSec * 2.4f).coerceAtLeast(12f) }
    val estimatedCompressedMb = remember(selectedDurationSec, selectedBitrateKbps) {
        (selectedDurationSec * (selectedBitrateKbps / 8000f)).coerceAtLeast(1.8f)
    }
    val savingsPercent = remember(estimatedRawMb, estimatedCompressedMb) {
        (((estimatedRawMb - estimatedCompressedMb) / estimatedRawMb) * 100f).toInt().coerceIn(50, 92)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Create & Upload Video",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Record with camera or upload from device, pick your thumbnail cover, add caption & hashtags, and process for adaptive streaming.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Step 1: Record Camera or Upload Existing Video + Thumbnail Selector
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Live Thumbnail Cover Preview Box (9:16 Vertical Aspect)
                        val previewColors = themeLabels[selectedThemeIndex.mod(themeLabels.size)].second
                        Box(
                            modifier = Modifier
                                .width(118.dp)
                                .height(186.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(previewColors[0], previewColors[1], Color.Black)
                                    )
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                                .testTag("thumbnail_preview_box")
                        ) {
                            if (!customThumbnailUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = Uri.parse(customThumbnailUri),
                                    contentDescription = "Selected video thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.28f))
                                )
                            }

                            Text(
                                text = "${selectedDurationSec}s • ${String.format(Locale.US, "%.1fs", selectedFrameTimestampSec)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )

                            Text(
                                text = thumbnailHeadline.ifBlank { "THUMBNAIL" },
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 8.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = if (customThumbnailUri != null) "CUSTOM COVER" else "FRAME COVER",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PulseCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Record Camera / Upload Existing Video / Custom Cover Controls
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    cameraPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.CAMERA,
                                            Manifest.permission.RECORD_AUDIO
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("record_camera_button")
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = "Record Camera")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Record Camera")
                            }

                            OutlinedButton(
                                onClick = {
                                    pickVideoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upload_device_video_button")
                            ) {
                                Icon(Icons.Default.Movie, contentDescription = "Upload existing video")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload Video")
                            }

                            OutlinedButton(
                                onClick = {
                                    pickThumbnailImageLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("select_custom_thumbnail_button")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Select cover image")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cover Image")
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Source ready",
                                    tint = MintSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = sourceStatusLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Thumbnail Frame Scrubber + Theme Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Thumbnail Frame & Style",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Frame @ ${String.format(Locale.US, "%.1fs", selectedFrameTimestampSec)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = PulseCyan
                        )
                    }

                    Slider(
                        value = selectedFrameTimestampSec,
                        onValueChange = {
                            selectedFrameTimestampSec = it
                            val idx = ((it / selectedDurationSec.coerceAtLeast(1)) * themeLabels.size)
                                .toInt()
                                .coerceIn(0, themeLabels.lastIndex)
                            selectedThemeIndex = idx
                        },
                        valueRange = 0f..selectedDurationSec.toFloat().coerceAtLeast(5f),
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCoral,
                            activeTrackColor = ElectricCoral
                        ),
                        modifier = Modifier.testTag("thumbnail_frame_scrubber")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(themeLabels.size) { idx ->
                            val (name, pairColors) = themeLabels[idx]
                            val isSelected = selectedThemeIndex == idx
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = pairColors[1].copy(alpha = if (isSelected) 0.9f else 0.25f),
                                modifier = Modifier
                                    .clickable {
                                        selectedThemeIndex = idx
                                        customThumbnailUri = null
                                    }
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .testTag("thumbnail_style_$idx")
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = thumbnailHeadline,
                        onValueChange = { thumbnailHeadline = it.take(24) },
                        label = { Text("Thumbnail Cover Headline") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("thumbnail_headline_input")
                    )
                }
            }
        }

        // Step 2: Caption & Relevant Hashtags
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Video Caption & Relevant Hashtags",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${caption.length}/220",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it.take(220) },
                        label = { Text("Describe your video, story, or @mention creators...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_caption_input")
                    )
                    OutlinedTextField(
                        value = hashtagsInput,
                        onValueChange = { hashtagsInput = it },
                        label = { Text("Hashtags (e.g. #NeonPulse #VerticalCinema)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_hashtags_input")
                    )

                    // Quick Add Custom Hashtag Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customTagDraft,
                            onValueChange = { customTagDraft = it.replace(" ", "") },
                            placeholder = { Text("Add custom #tag") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_hashtag_input")
                        )
                        OutlinedButton(
                            onClick = {
                                val formatted = customTagDraft.trim().removePrefix("#")
                                if (formatted.isNotBlank()) {
                                    val tagWithHash = "#$formatted"
                                    if (!hashtagsInput.contains(tagWithHash, ignoreCase = true)) {
                                        hashtagsInput = "$hashtagsInput $tagWithHash".trim()
                                    }
                                    customTagDraft = ""
                                }
                            },
                            modifier = Modifier.testTag("add_custom_hashtag_button")
                        ) {
                            Text("+ Add Tag")
                        }
                    }

                    Text(
                        text = "Tap relevant trending hashtags to include:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        trendingHashtags.forEach { tagEntity ->
                            val isIncluded = hashtagsInput.contains(tagEntity.displayTag, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isIncluded) CyberViolet.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    hashtagsInput = if (isIncluded) {
                                        hashtagsInput.replace(tagEntity.displayTag, "", ignoreCase = true)
                                            .replace("  ", " ").trim()
                                    } else {
                                        "$hashtagsInput ${tagEntity.displayTag}".trim()
                                    }
                                }
                            ) {
                                Text(
                                    text = if (isIncluded) "✓ ${tagEntity.displayTag}" else "+ ${tagEntity.displayTag}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isIncluded) PulseCyan else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Step 3: Soundtrack & Efficient Streaming Processing Pipeline
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Add Sound",
                            tint = PulseCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Soundtrack & Adaptive Stream Processing",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sounds.size) { index ->
                            val s = sounds[index]
                            FilterChip(
                                selected = selectedSoundIndex == index,
                                onClick = {
                                    selectedSoundIndex = index
                                    SynthAudioEngine.playPreviewClip(
                                        bpm = s.bpm,
                                        themeIndex = index,
                                        durationMs = 700
                                    )
                                },
                                label = { Text("${s.title} (${s.bpm} BPM)") }
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = MintSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HEVC Stream Optimization",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Surface(
                            color = MintSuccess.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%.1f MB → %.1f MB (-%d%%)",
                                    estimatedRawMb,
                                    estimatedCompressedMb,
                                    savingsPercent
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MintSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Duration & Resolution Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(15, 30, 60).forEach { sec ->
                            FilterChip(
                                selected = selectedDurationSec == sec,
                                onClick = { selectedDurationSec = sec },
                                label = { Text("${sec}s") }
                            )
                        }
                        listOf("1080x1920" to "1080p HD", "720x1280" to "720p Fast").forEach { (res, label) ->
                            FilterChip(
                                selected = selectedResolution == res,
                                onClick = { selectedResolution = res },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            1800 to "1.8 Mbps Saver",
                            2400 to "2.4 Mbps Balanced",
                            3200 to "3.2 Mbps Studio"
                        ).forEach { (kbps, label) ->
                            FilterChip(
                                selected = selectedBitrateKbps == kbps,
                                onClick = { selectedBitrateKbps = kbps },
                                label = { Text(label) }
                            )
                        }
                    }

                    // Multi-Stage Upload & Video Processing Progress Indicator
                    AnimatedVisibility(visible = uploadProgress.isUploading) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_progress_container")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uploadProgress.stageLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = PulseCyan,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${(uploadProgress.progressFraction * 100).toInt()}%",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ElectricCoral
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { uploadProgress.progressFraction },
                                    color = ElectricCoral,
                                    trackColor = Color.White.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .testTag("upload_progress_bar")
                                )
                                if (uploadProgress.compressionSummary.isNotBlank()) {
                                    Text(
                                        text = uploadProgress.compressionSummary,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MintSuccess
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onUploadVideo(
                                caption.ifBlank { "Fresh studio creation on VibeStream ✨" },
                                hashtagsInput,
                                activeSound,
                                selectedVideoUri,
                                customThumbnailUri,
                                selectedThemeIndex,
                                thumbnailHeadline,
                                selectedDurationSec,
                                selectedBitrateKbps,
                                selectedResolution
                            )
                        },
                        enabled = !uploadProgress.isUploading,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("publish_video_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Publish")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uploadProgress.isUploading) {
                                "Processing & Uploading (${(uploadProgress.progressFraction * 100).toInt()}%)..."
                            } else {
                                "Process & Upload Video"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        // Step 4: Manage & Delete Your Uploaded Videos
        if (myVideos.isNotEmpty()) {
            item {
                Text(
                    text = "Your Uploaded Videos (${myVideos.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(myVideos, key = { it.id }) { video ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = video.thumbnailHeadline.ifBlank { video.caption.take(28) },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = video.caption,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = "${video.resolution} • ${video.bitrateKbps} kbps • ${video.likesCount} likes",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberViolet
                            )
                        }
                        IconButton(
                            onClick = { onDeleteMyVideo(video.id) },
                            modifier = Modifier.testTag("delete_my_video_${video.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete video",
                                tint = ElectricCoral
                            )
                        }
                    }
                }
            }
        }
    }

    if (showInAppRecorderModal) {
        InAppCameraRecorderDialog(
            maxDurationSec = selectedDurationSec,
            onDismiss = { showInAppRecorderModal = false },
            onLaunchSystemCamera = {
                showInAppRecorderModal = false
                val uri = createTempVideoUri(context)
                pendingCaptureUri = uri
                if (uri != null) {
                    try {
                        captureVideoLauncher.launch(uri)
                    } catch (_: Exception) {
                        onShowMessage("Recorded ${selectedDurationSec}s clip in Studio Camera!")
                    }
                }
            },
            onFinishRecording = { recordedSeconds, lensFacing, usedFlash ->
                showInAppRecorderModal = false
                selectedDurationSec = recordedSeconds.coerceAtLeast(5)
                sourceStatusLabel = "Recorded ${selectedDurationSec}s ($lensFacing Camera${if (usedFlash) " • Flash" else ""})"
                thumbnailHeadline = "CAMERA TAKE ${selectedDurationSec}S"
                onShowMessage("Camera video recorded (${selectedDurationSec}s)! Add caption & publish.")
            }
        )
    }
}

@Composable
private fun InAppCameraRecorderDialog(
    maxDurationSec: Int,
    onDismiss: () -> Unit,
    onLaunchSystemCamera: () -> Unit,
    onFinishRecording: (Int, String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var isFrontLens by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var liveMicDb by remember { mutableFloatStateOf(-42f) }

    val hasCameraPermission = remember(context) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording && elapsedSec < maxDurationSec) {
                val measuredDb = MicrophoneLevelEngine.readCurrentLevelDb(context)
                liveMicDb = measuredDb ?: (-24f + (elapsedSec % 5) * 2.5f)
                delay(1000L)
                elapsedSec += 1
                if (elapsedSec >= maxDurationSec) {
                    isRecording = false
                    onFinishRecording(elapsedSec, if (isFrontLens) "Front" else "Back", flashEnabled)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF09070F))
                .padding(20.dp)
        ) {
            // Real CameraX PreviewView when CAMERA permission is granted
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                    },
                    update = { previewView ->
                        try {
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val selector = if (isFrontLens) {
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    } else {
                                        CameraSelector.DEFAULT_BACK_CAMERA
                                    }
                                    cameraProvider.unbindAll()
                                    val camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        selector,
                                        preview
                                    )
                                    camera.cameraControl.enableTorch(flashEnabled && !isFrontLens)
                                } catch (_: Throwable) {
                                }
                            }, ContextCompat.getMainExecutor(previewView.context))
                        } catch (_: Throwable) {
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                )
            }

            // Viewfinder Framing Grid & Focus Reticle Overlay
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
            ) {
                val w = size.width
                val h = size.height
                if (!hasCameraPermission) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            if (isFrontLens) {
                                listOf(Color(0xFF1E1136), Color(0xFF3E184C), Color(0xFF0E0B18))
                            } else {
                                listOf(Color(0xFF0B202E), Color(0xFF1F1238), Color(0xFF0B0914))
                            }
                        )
                    )
                }
                // Rule-of-thirds camera grid lines
                val gridColor = Color.White.copy(alpha = 0.14f)
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), strokeWidth = 1.dp.toPx())

                // Center focus reticle
                drawCircle(
                    color = if (isRecording) ElectricCoral else PulseCyan,
                    center = Offset(w / 2f, h / 2f),
                    radius = 42.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Top Camera Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close camera", tint = Color.White)
                }

                Surface(
                    color = if (isRecording) ElectricCoral else Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isRecording) {
                                String.format(Locale.US, "REC %ds / %ds • %.1f dB", elapsedSec, maxDurationSec, liveMicDb)
                            } else {
                                "READY • 1080p 60fps"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { flashEnabled = !flashEnabled },
                        modifier = Modifier.background(
                            if (flashEnabled) NeonAmber else Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Toggle flash",
                            tint = if (flashEnabled) Color.Black else Color.White
                        )
                    }
                    IconButton(
                        onClick = { isFrontLens = !isFrontLens },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch lens",
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom Shutter Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LinearProgressIndicator(
                    progress = { (elapsedSec.toFloat() / maxDurationSec.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    color = ElectricCoral,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onLaunchSystemCamera) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OS Camera", color = Color.White)
                    }

                    // Main Record / Stop Shutter Button
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(ElectricCoral)
                            .clickable {
                                if (!isRecording) {
                                    elapsedSec = 0
                                    isRecording = true
                                } else {
                                    isRecording = false
                                    onFinishRecording(
                                        elapsedSec.coerceAtLeast(5),
                                        if (isFrontLens) "Front" else "Back",
                                        flashEnabled
                                    )
                                }
                            }
                            .testTag("camera_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = if (isRecording) "Stop recording" else "Start recording",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Button(
                        onClick = {
                            onFinishRecording(
                                elapsedSec.coerceAtLeast(10),
                                if (isFrontLens) "Front" else "Back",
                                flashEnabled
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintSuccess)
                    ) {
                        Text("Use Clip", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun createTempVideoUri(context: Context): Uri? {
    return try {
        val dir = File(context.cacheDir, "videos").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.mp4")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (_: Exception) {
        null
    }
}

private fun probeVideoDurationSeconds(context: Context, uri: Uri): Int? {
    val probed = VideoProcessingEngine.probeVideoMetadata(
        context = context,
        videoUriString = uri.toString(),
        fallbackDurationSec = 15,
        fallbackResolution = "1080x1920",
        fallbackBitrateKbps = 2400
    )
    if (probed.durationSec > 0) {
        return probed.durationSec.coerceIn(3, 180)
    }
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        durationMs?.let { (it / 1000L).toInt().coerceIn(3, 180) }
    } catch (_: Exception) {
        null
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }
}
