package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.AppConfig
import com.example.R
import com.example.data.engine.SynthAudioEngine
import com.example.data.local.VideoEntity
import com.example.ui.ReportTarget
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCyan
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

fun parseHexColor(hex: String, fallback: Color = ElectricCoral): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val longVal = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(longVal or 0xFF000000L)
        } else {
            Color(longVal)
        }
    } catch (_: Exception) {
        fallback
    }
}

fun formatCompactCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000f)
        else -> count.toString()
    }
}

fun formatCompactLong(count: Long): String {
    return when {
        count >= 1_000_000L -> String.format(Locale.US, "%.1fM", count / 1_000_000f)
        count >= 1_000L -> String.format(Locale.US, "%.1fK", count / 1_000f)
        else -> count.toString()
    }
}

/**
 * Formats a video duration in seconds into M:SS format (e.g., 15 -> "0:15", 65 -> "1:05", 125 -> "2:05").
 */
fun formatVideoDurationMss(durationSec: Int): String {
    val safeSeconds = durationSec.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

/**
 * Resolves the primary shareable link for a video.
 */
fun buildVideoShareLink(video: VideoEntity): String {
    return video.cdnStreamUrl.ifBlank {
        video.videoUri.ifBlank { "https://vibestream.social/videos/${video.id}" }
    }
}

/**
 * Formats the share message body containing creator handle, caption, and shareable video link(s).
 */
fun buildVideoShareText(video: VideoEntity): String {
    val primaryLink = buildVideoShareLink(video)
    val streamLink = video.videoUri.takeIf { it.isNotBlank() && it != primaryLink }
    return buildString {
        append("Watch @${video.creatorUsername}'s video on VibeStream")
        if (video.caption.isNotBlank()) {
            append(": \"${video.caption}\"")
        }
        append("\n")
        append(primaryLink)
        if (streamLink != null) {
            append("\nStream: ")
            append(streamLink)
        }
    }
}

/**
 * Creates a standard Android ACTION_SEND share intent for sharing a video link with others.
 */
fun buildVideoShareIntent(video: VideoEntity): Intent {
    val shareLink = buildVideoShareLink(video)
    val shareText = buildVideoShareText(video)
    return Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "VibeStream Video by @${video.creatorUsername}")
        putExtra(Intent.EXTRA_TITLE, video.caption.ifBlank { "Watch @${video.creatorUsername} on VibeStream" })
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra("extra_video_id", video.id)
        putExtra("extra_video_url", shareLink)
    }
}

/**
 * Triggers the Android system share sheet chooser for sharing a video link with other apps.
 */
fun launchVideoShareIntent(context: Context, video: VideoEntity): Intent {
    val sendIntent = buildVideoShareIntent(video)
    val chooserIntent = Intent.createChooser(sendIntent, "Share video link").apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    try {
        context.startActivity(chooserIntent)
    } catch (_: Exception) {
        try {
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        } catch (_: Exception) {
        }
    }
    return chooserIntent
}

/**
 * Small overlay label in the corner of the video player that displays the video duration in M:SS format.
 */
@Composable
fun VideoDurationCornerOverlay(
    durationSec: Int,
    videoId: Long? = null,
    testTagPrefix: String = "video_duration_overlay",
    modifier: Modifier = Modifier
) {
    val formattedDuration = formatVideoDurationMss(durationSec)
    val tag = if (videoId != null) "${testTagPrefix}_$videoId" else testTagPrefix
    Text(
        text = formattedDuration,
        style = MaterialTheme.typography.labelMedium.copy(
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        ),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.68f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(tag)
    )
}

/**
 * Volume toggle button on the video player to allow users to mute or unmute video playback easily.
 */
@Composable
fun VideoPlayerVolumeToggleButton(
    isAudioMuted: Boolean,
    onToggleMute: () -> Unit,
    videoId: Long? = null,
    showLabel: Boolean = true,
    testTagPrefix: String = "volume_toggle_button",
    modifier: Modifier = Modifier
) {
    val tag = if (videoId != null) "${testTagPrefix}_$videoId" else testTagPrefix
    val statusLabel = if (isAudioMuted) "Muted" else "Sound On"
    val actionDescription = if (isAudioMuted) {
        stringResource(R.string.action_unmute)
    } else {
        stringResource(R.string.action_mute)
    }

    val containerColor by animateColorAsState(
        targetValue = if (isAudioMuted) {
            Color.Black.copy(alpha = 0.72f)
        } else {
            PulseCyan.copy(alpha = 0.22f)
        },
        animationSpec = tween(durationMillis = 180),
        label = "volume_toggle_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isAudioMuted) {
            Color.White.copy(alpha = 0.25f)
        } else {
            PulseCyan.copy(alpha = 0.65f)
        },
        animationSpec = tween(durationMillis = 180),
        label = "volume_toggle_border"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isAudioMuted) Color.White.copy(alpha = 0.75f) else PulseCyan,
        animationSpec = tween(durationMillis = 180),
        label = "volume_toggle_tint"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable { onToggleMute() }
            .padding(horizontal = if (showLabel) 12.dp else 10.dp, vertical = 8.dp)
            .testTag(tag)
    ) {
        Icon(
            imageVector = if (isAudioMuted) {
                Icons.AutoMirrored.Filled.VolumeOff
            } else {
                Icons.AutoMirrored.Filled.VolumeUp
            },
            contentDescription = actionDescription,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        if (showLabel) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Slim progress bar positioned at the bottom of the video player that visually indicates
 * the playback progress (0f..1f) of the current video.
 */
@Composable
fun VideoPlaybackProgressBar(
    playbackProgress: Float,
    videoId: Long? = null,
    testTagPrefix: String = "video_progress_bar",
    modifier: Modifier = Modifier
) {
    val clampedProgress = playbackProgress.coerceIn(0f, 1f)
    val percentInt = (clampedProgress * 100f).toInt()
    val tag = if (videoId != null) "${testTagPrefix}_$videoId" else testTagPrefix

    LinearProgressIndicator(
        progress = { clampedProgress },
        color = ElectricCoral,
        trackColor = Color.White.copy(alpha = 0.22f),
        strokeCap = StrokeCap.Butt,
        gapSize = 0.dp,
        drawStopIndicator = {},
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .semantics {
                contentDescription = "Video playback progress"
                stateDescription = "$percentInt%"
            }
            .testTag(tag)
    )
}

@Composable
fun CreatorAvatar(
    username: String,
    colorHex: String,
    avatarUri: String? = null,
    size: Dp = 48.dp,
    isVerified: Boolean = false,
    showFollowBadge: Boolean = false,
    isFollowing: Boolean = false,
    onAvatarClick: (() -> Unit)? = null,
    onFollowClick: (() -> Unit)? = null
) {
    val baseColor = parseHexColor(colorHex)
    val initials = username.removePrefix("@").take(2).uppercase()

    Box(
        modifier = Modifier.size(size + if (showFollowBadge) 10.dp else 0.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(baseColor, baseColor.copy(alpha = 0.65f), CyberViolet)
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                .let { mod ->
                    if (onAvatarClick != null) mod.clickable { onAvatarClick() } else mod
                },
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUri.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(avatarUri),
                    contentDescription = "Profile picture for @$username",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.34f).sp
                    )
                )
            }
        }

        if (isVerified && !showFollowBadge) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Verified creator",
                tint = PulseCyan,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color(0xFF0D0B14), CircleShape)
            )
        }

        if (showFollowBadge && onFollowClick != null) {
            Surface(
                shape = CircleShape,
                color = if (isFollowing) Color(0xFF231F36) else ElectricCoral,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(22.dp)
                    .border(1.5.dp, Color.White, CircleShape)
                    .clickable { onFollowClick() }
                    .testTag("feed_follow_badge")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = if (isFollowing) "Following" else "Follow creator",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Renders both a procedural high-contrast vertical visual canvas (acting as instant thumbnail &
 * synchronized visualizer) AND an embedded Android VideoView when a valid video URI is active.
 */
@Composable
fun InteractiveVideoCanvas(
    themeIndex: Int,
    headline: String,
    videoUri: String,
    isPlaying: Boolean,
    isActivePage: Boolean,
    isAudioMuted: Boolean = false,
    soundBpm: Int = 124,
    modifier: Modifier = Modifier
) {
    val palettes = listOf(
        listOf(Color(0xFF140826), Color(0xFFFF3366), Color(0xFF7C4DFF)),
        listOf(Color(0xFF041B2D), Color(0xFF00E5FF), Color(0xFF7C4DFF)),
        listOf(Color(0xFF072118), Color(0xFF00E676), Color(0xFF00B0FF)),
        listOf(Color(0xFF261405), Color(0xFFFFB300), Color(0xFFFF3366)),
        listOf(Color(0xFF1B0924), Color(0xFFFF4081), Color(0xFF00E5FF)),
        listOf(Color(0xFF1A1A26), Color(0xFF9E9E9E), Color(0xFF607D8B))
    )
    val colors = palettes[themeIndex.mod(palettes.size)]

    // Real hardware PCM AudioTrack soundtrack synthesis synchronized with video playback & mute toggle
    LaunchedEffect(isAudioMuted) {
        SynthAudioEngine.setMuted(isAudioMuted)
    }

    DisposableEffect(isActivePage, isPlaying, themeIndex, soundBpm) {
        if (isActivePage && isPlaying) {
            SynthAudioEngine.startLoop(
                bpm = soundBpm,
                themeIndex = themeIndex,
                isMuted = isAudioMuted
            )
        } else if (isActivePage && !isPlaying) {
            SynthAudioEngine.stop()
        }
        onDispose {
            if (isActivePage) {
                SynthAudioEngine.stop()
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "video_pulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val animatedPhase = if (isPlaying && isActivePage) phase else 1.2f

    Box(modifier = modifier.fillMaxSize()) {
        // High-FPS Procedural Vertical Visual Canvas & Thumbnail Layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors[0],
                        colors[1].copy(alpha = 0.45f),
                        colors[2].copy(alpha = 0.38f),
                        Color(0xFF08070C)
                    )
                )
            )

            // Animated neon orbital rings and sound-reactive wave bars
            val centerX = w * (0.5f + 0.12f * cos(animatedPhase))
            val centerY = h * (0.42f + 0.08f * sin(animatedPhase))

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors[1].copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = w * 0.65f
                ),
                center = Offset(centerX, centerY),
                radius = w * 0.65f
            )

            drawCircle(
                color = colors[2].copy(alpha = 0.35f),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = w * (0.28f + 0.04f * sin(animatedPhase * 2f)),
                style = Stroke(width = 4.dp.toPx())
            )

            // Equalizer pulse bars across center
            val barCount = 18
            val totalWidth = w * 0.72f
            val startX = (w - totalWidth) / 2f
            val spacing = totalWidth / barCount
            for (i in 0 until barCount) {
                val wave = sin(animatedPhase * 2f + i * 0.45f)
                val barHeight = (24f + 72f * ((wave + 1f) / 2f)).dp.toPx()
                drawRoundRect(
                    color = if (i % 2 == 0) colors[1].copy(alpha = 0.65f) else colors[2].copy(alpha = 0.65f),
                    topLeft = Offset(startX + i * spacing, h * 0.46f - barHeight / 2f),
                    size = androidx.compose.ui.geometry.Size(spacing * 0.55f, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
            }
        }

        // Embedded hardware Android VideoView for real local/captured/streamed MP4 video URIs
        val isLocalPlayableVideo = remember(videoUri) {
            videoUri.startsWith("content://") ||
                (videoUri.startsWith("file://") && videoUri.endsWith(".mp4", ignoreCase = true))
        }
        if (isActivePage && isLocalPlayableVideo) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        try {
                            setVideoURI(Uri.parse(videoUri))
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                val vol = if (isAudioMuted) 0f else 1f
                                mp.setVolume(vol, vol)
                                if (isPlaying) {
                                    start()
                                }
                            }
                            setOnErrorListener { _, _, _ -> true }
                        } catch (_: Throwable) {
                        }
                    }
                },
                update = { view ->
                    try {
                        if (isPlaying && !view.isPlaying) {
                            view.start()
                        } else if (!isPlaying && view.isPlaying) {
                            view.pause()
                        }
                    } catch (_: Throwable) {
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top-center subtle visual headline watermark for thumbnail clarity
        if (headline.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .background(Color.Black.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 1.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bottom gradient scrim for readable overlay text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )
    }
}

@Composable
fun ReportContentDialog(
    target: ReportTarget,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(AppConfig.REPORT_REASONS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = "Report content",
                tint = NeonAmber
            )
        },
        title = {
            Text(
                text = "Report ${target.targetType.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = target.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                AppConfig.REPORT_REASONS.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                modifier = Modifier.testTag("confirm_report_button")
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
