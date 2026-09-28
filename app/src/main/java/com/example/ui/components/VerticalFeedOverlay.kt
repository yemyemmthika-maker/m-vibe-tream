package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CommentEntity
import com.example.data.local.UserEntity
import com.example.data.local.VideoEntity
import com.example.ui.screens.VideoPlaybackStatus
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.PulseCyan
import kotlinx.coroutines.delay

/**
 * Full-screen layout overlay for a vertical video feed item.
 * Positions the interactive action rail (Like, Comment, Share, Audio, Report, Sound Disc)
 * vertically on the right side of the screen, and creator/video metadata on the bottom-left.
 */
@Composable
fun VerticalFeedOverlay(
    video: VideoEntity,
    playbackStatus: VideoPlaybackStatus,
    formattedTime: String,
    playbackProgress: Float,
    isPlaying: Boolean,
    isAudioMuted: Boolean,
    isLiked: Boolean,
    isFollowingCreator: Boolean,
    isOwnVideo: Boolean,
    nextPreloadedUrl: String?,
    onToggleLike: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenComments: () -> Unit,
    onShare: () -> Unit,
    onToggleMute: () -> Unit,
    onReport: () -> Unit,
    onOpenCreator: () -> Unit,
    onClickHashtag: (String) -> Unit,
    onClickSound: () -> Unit,
    modifier: Modifier = Modifier,
    comments: List<CommentEntity>? = null,
    currentUser: UserEntity? = null,
    onSubmitComment: ((String, CommentEntity?) -> Unit)? = null,
    onToggleLikeComment: ((CommentEntity) -> Unit)? = null,
    onDeleteComment: ((CommentEntity) -> Unit)? = null,
    onReportComment: ((CommentEntity) -> Unit)? = null,
    onOpenCommentUserProfile: ((Long) -> Unit)? = null
) {
    var effectiveAudioMuted by remember(isAudioMuted) { mutableStateOf(isAudioMuted) }
    var isBuiltInCommentsSheetOpen by remember(video.id) { mutableStateOf(false) }

    val displayedCommentsCount = remember(video.commentsCount, comments, video.id) {
        if (comments != null) {
            val matchingCount = comments.count { it.videoId == video.id }
            if (matchingCount > 0) matchingCount else video.commentsCount
        } else {
            video.commentsCount
        }
    }

    val effectiveVideo = remember(video, displayedCommentsCount) {
        if (video.commentsCount == displayedCommentsCount) {
            video
        } else {
            video.copy(commentsCount = displayedCommentsCount)
        }
    }

    val handleToggleMute = {
        effectiveAudioMuted = !effectiveAudioMuted
        onToggleMute()
    }

    val handleOpenComments = {
        if (comments != null) {
            isBuiltInCommentsSheetOpen = true
        }
        onOpenComments()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("vertical_feed_overlay_${video.id}")
    ) {
        // Volume toggle button on the video player to allow users to mute or unmute playback easily
        VideoPlayerVolumeToggleButton(
            isAudioMuted = effectiveAudioMuted,
            onToggleMute = handleToggleMute,
            videoId = video.id,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 14.dp, start = 16.dp)
        )

        // Small overlay label in the corner of the video player displaying video duration in M:SS format
        VideoDurationCornerOverlay(
            durationSec = video.durationSec,
            videoId = video.id,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 16.dp)
        )

        // Bottom-left creator metadata, caption, hashtags, sound marquee & stream status
        BottomLeftVideoMetadata(
            video = effectiveVideo,
            playbackStatus = playbackStatus,
            formattedTime = formattedTime,
            isFollowingCreator = isFollowingCreator,
            isOwnVideo = isOwnVideo,
            nextPreloadedUrl = nextPreloadedUrl,
            onToggleFollow = onToggleFollow,
            onOpenCreator = onOpenCreator,
            onClickHashtag = onClickHashtag,
            onClickSound = onClickSound,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.76f)
                .padding(start = 16.dp, end = 8.dp, bottom = 20.dp)
        )

        // Right-side vertical interactive overlay column (Like, Comment, Share, Audio, Report, Sound)
        RightSideVideoActionRail(
            video = effectiveVideo,
            isPlaying = isPlaying,
            isAudioMuted = effectiveAudioMuted,
            isLiked = isLiked,
            isFollowingCreator = isFollowingCreator,
            isOwnVideo = isOwnVideo,
            onToggleLike = onToggleLike,
            onToggleFollow = onToggleFollow,
            onOpenComments = handleOpenComments,
            onShare = onShare,
            onToggleMute = handleToggleMute,
            onReport = onReport,
            onOpenCreator = onOpenCreator,
            onClickSound = onClickSound,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 22.dp)
        )

        // Slim bottom playback progress bar visually indicating current video playback progress
        VideoPlaybackProgressBar(
            playbackProgress = playbackProgress,
            videoId = video.id,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Built-in Comments Bottom Sheet when comments are passed directly to VerticalFeedOverlay
        if (comments != null && isBuiltInCommentsSheetOpen) {
            CommentsBottomSheet(
                video = effectiveVideo,
                comments = comments,
                currentUser = currentUser,
                onDismiss = { isBuiltInCommentsSheetOpen = false },
                onSubmitComment = { text, parent -> onSubmitComment?.invoke(text, parent) },
                onToggleLikeComment = { comment -> onToggleLikeComment?.invoke(comment) },
                onDeleteComment = { comment -> onDeleteComment?.invoke(comment) },
                onReportComment = { comment -> onReportComment?.invoke(comment) },
                onOpenUserProfile = { uid ->
                    isBuiltInCommentsSheetOpen = false
                    onOpenCommentUserProfile?.invoke(uid)
                }
            )
        }
    }
}

/**
 * Right-side vertical interaction column containing interactive buttons for
 * Liking, Commenting, and Sharing a video, along with creator avatar and audio controls.
 */
@Composable
fun RightSideVideoActionRail(
    video: VideoEntity,
    isPlaying: Boolean,
    isAudioMuted: Boolean,
    isLiked: Boolean,
    isFollowingCreator: Boolean,
    isOwnVideo: Boolean,
    onToggleLike: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenComments: () -> Unit,
    onShare: () -> Unit,
    onToggleMute: () -> Unit,
    onReport: () -> Unit,
    onOpenCreator: () -> Unit,
    onClickSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val discTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val discRotation by discTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_angle"
    )

    var sharePulseTrigger by remember { mutableIntStateOf(0) }
    var isRecentlyShared by remember { mutableStateOf(false) }

    LaunchedEffect(sharePulseTrigger) {
        if (sharePulseTrigger > 0) {
            isRecentlyShared = true
            delay(900)
            isRecentlyShared = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF161224).copy(alpha = 0.52f),
                        Color(0xFF0D0B14).copy(alpha = 0.68f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        CyberViolet.copy(alpha = 0.38f)
                    )
                ),
                shape = RoundedCornerShape(30.dp)
            )
            .padding(horizontal = 6.dp, vertical = 10.dp)
            .testTag("right_action_rail_${video.id}")
    ) {
        CreatorAvatar(
            username = video.creatorUsername,
            colorHex = video.creatorAvatarHex,
            size = 48.dp,
            isVerified = video.creatorVerified,
            showFollowBadge = !isOwnVideo,
            isFollowing = isFollowingCreator,
            onAvatarClick = onOpenCreator,
            onFollowClick = onToggleFollow
        )

        // Interactive Like Button
        FeedActionButton(
            icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = formatCompactCount(video.likesCount),
            contentDescription = if (isLiked) {
                stringResource(R.string.action_unlike)
            } else {
                stringResource(R.string.action_like)
            },
            tint = if (isLiked) ElectricCoral else Color.White,
            isActive = isLiked,
            activeContainerColor = ElectricCoral.copy(alpha = 0.22f),
            testTag = "like_button_${video.id}",
            onClick = onToggleLike
        )

        // Interactive Comment Icon Button displaying count of existing comments
        VideoCommentIconButton(
            videoId = video.id,
            commentsCount = video.commentsCount,
            onClick = onOpenComments
        )

        // Interactive Share Button (triggers Android system share sheet intent + callback)
        FeedActionButton(
            icon = Icons.Default.Share,
            label = formatCompactCount(video.sharesCount),
            contentDescription = stringResource(R.string.action_share),
            tint = if (isRecentlyShared) PulseCyan else Color.White,
            isActive = isRecentlyShared,
            activeContainerColor = PulseCyan.copy(alpha = 0.22f),
            testTag = "share_button_${video.id}",
            onClick = {
                sharePulseTrigger++
                launchVideoShareIntent(context, video)
                onShare()
            }
        )

        // Audio Mute/Unmute Toggle
        FeedActionButton(
            icon = if (isAudioMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            label = if (isAudioMuted) "Muted" else "Audio",
            contentDescription = if (isAudioMuted) {
                stringResource(R.string.action_unmute)
            } else {
                stringResource(R.string.action_mute)
            },
            tint = if (isAudioMuted) Color.White.copy(alpha = 0.65f) else PulseCyan,
            isActive = !isAudioMuted,
            activeContainerColor = PulseCyan.copy(alpha = 0.14f),
            testTag = "mute_button_${video.id}",
            onClick = onToggleMute
        )

        // Content Report Button
        FeedActionButton(
            icon = Icons.Default.Flag,
            label = "Report",
            contentDescription = stringResource(R.string.action_report),
            tint = Color.White.copy(alpha = 0.78f),
            isActive = false,
            activeContainerColor = Color.Transparent,
            testTag = "report_video_button_${video.id}",
            onClick = onReport
        )

        // Rotating sound vinyl disc
        Surface(
            shape = CircleShape,
            color = Color(0xFF1B1829),
            modifier = Modifier
                .size(48.dp)
                .minimumInteractiveComponentSize()
                .rotate(if (isPlaying) discRotation else 0f)
                .border(2.dp, CyberViolet, CircleShape)
                .clickable { onClickSound() }
                .testTag("sound_disc_${video.id}")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Sound track",
                    tint = PulseCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Comment icon button on the video overlay that displays the count of existing comments
 * and opens the comments bottom sheet when tapped.
 */
@Composable
fun VideoCommentIconButton(
    videoId: Long,
    commentsCount: Int,
    onClick: () -> Unit,
    isActive: Boolean = false
) {
    FeedActionButton(
        icon = Icons.Default.ChatBubble,
        label = formatCompactCount(commentsCount),
        contentDescription = stringResource(R.string.action_comment),
        tint = if (isActive) PulseCyan else Color.White,
        isActive = isActive,
        activeContainerColor = CyberViolet.copy(alpha = 0.22f),
        testTag = "comment_button_$videoId",
        labelTestTag = "comment_count_$videoId",
        onClick = onClick
    )
}

@Composable
fun FeedActionButton(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    tint: Color,
    isActive: Boolean = false,
    activeContainerColor: Color = Color.White.copy(alpha = 0.12f),
    testTag: String,
    labelTestTag: String? = null,
    onClick: () -> Unit
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var bounceActive by remember { mutableStateOf(false) }

    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            bounceActive = true
            delay(180)
            bounceActive = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (bounceActive) 1.24f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "action_button_scale"
    )

    val animatedTint by animateColorAsState(
        targetValue = tint,
        animationSpec = tween(durationMillis = 180),
        label = "action_button_tint"
    )

    val circleBgColor by animateColorAsState(
        targetValue = if (isActive) activeContainerColor else Color.White.copy(alpha = 0.08f),
        animationSpec = tween(durationMillis = 180),
        label = "action_button_bg"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(width = 54.dp, height = 56.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                tapCount++
                onClick()
            }
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(circleBgColor)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = animatedTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color.White,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
            ),
            maxLines = 1,
            modifier = if (labelTestTag != null) Modifier.testTag(labelTestTag) else Modifier
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BottomLeftVideoMetadata(
    video: VideoEntity,
    playbackStatus: VideoPlaybackStatus,
    formattedTime: String,
    isFollowingCreator: Boolean,
    isOwnVideo: Boolean,
    nextPreloadedUrl: String?,
    onToggleFollow: () -> Unit,
    onOpenCreator: () -> Unit,
    onClickHashtag: (String) -> Unit,
    onClickSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Creator username & Follow pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "@${video.creatorUsername}",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.clickable { onOpenCreator() }
            )
            if (video.creatorVerified) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified",
                    tint = PulseCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (!isOwnVideo) {
                Surface(
                    color = if (isFollowingCreator) Color.White.copy(alpha = 0.18f) else ElectricCoral,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .clickable { onToggleFollow() }
                        .testTag("inline_follow_button_${video.id}")
                ) {
                    Text(
                        text = if (isFollowingCreator) "Following" else "Follow",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Video Caption
        Text(
            text = video.caption,
            style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.94f)),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        // Tappable Hashtags
        val tags = video.hashtags.split(" ").filter { it.isNotBlank() }
        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tags.forEach { tag ->
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelLarge.copy(color = PulseCyan),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { onClickHashtag(tag) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Sound / Music Track Info Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable { onClickSound() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Original sound",
                tint = ElectricCoral,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${video.soundTitle} • ${video.soundArtist}",
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Player Timecode & Placeholder Video Stream URL Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = MintSuccess.copy(alpha = 0.18f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${playbackStatus.name} • $formattedTime",
                    style = MaterialTheme.typography.labelMedium,
                    color = MintSuccess,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text = video.videoUri.substringAfterLast("/"),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.58f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (nextPreloadedUrl != null) {
                Text(
                    text = "• Preloaded: ${nextPreloadedUrl.substringAfterLast("/")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = PulseCyan.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
