package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CommentEntity
import com.example.data.local.SoundEntity
import com.example.data.local.UserEntity
import com.example.data.local.VideoEntity
import com.example.ui.FeedMode
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.InteractiveVideoCanvas
import com.example.ui.components.VerticalFeedOverlay
import com.example.ui.components.formatVideoDurationMss
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.PulseCyan
import kotlinx.coroutines.delay

enum class VideoPlaybackStatus {
    BUFFERING,
    PLAYING,
    PAUSED
}

@Composable
fun HomeFeedScreen(
    videos: List<VideoEntity>,
    currentUser: UserEntity?,
    feedMode: FeedMode,
    likedVideoIds: Set<Long>,
    followingUserIds: Set<Long>,
    activeHashtagFilter: String?,
    activeSoundFilter: SoundEntity?,
    activeCommentVideo: VideoEntity?,
    comments: List<CommentEntity>,
    sounds: List<SoundEntity>,
    onSelectFeedMode: (FeedMode) -> Unit,
    onClearFilter: () -> Unit,
    onToggleLike: (VideoEntity) -> Unit,
    onToggleFollow: (Long) -> Unit,
    onOpenComments: (VideoEntity) -> Unit,
    onCloseComments: () -> Unit,
    onSubmitComment: (String, CommentEntity?) -> Unit,
    onToggleLikeComment: (CommentEntity) -> Unit,
    onDeleteComment: (CommentEntity) -> Unit,
    onShareVideo: (VideoEntity) -> Unit,
    onReportVideo: (VideoEntity) -> Unit,
    onReportComment: (CommentEntity) -> Unit,
    onOpenUserProfile: (Long) -> Unit,
    onSelectHashtag: (String) -> Unit,
    onSelectSound: (SoundEntity) -> Unit,
    onNavigateToDiscover: () -> Unit,
    onOpenAdminDashboard: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0B14))
    ) {
        if (videos.isEmpty()) {
            EmptyFeedView(
                feedMode = feedMode,
                hasFilter = activeHashtagFilter != null || activeSoundFilter != null,
                onSwitchToForYou = {
                    onClearFilter()
                    onSelectFeedMode(FeedMode.FOR_YOU)
                },
                onNavigateToDiscover = onNavigateToDiscover
            )
        } else {
            val liveVideos = remember(videos, activeCommentVideo, comments) {
                videos.map { v ->
                    val liveCount = comments.count { it.videoId == v.id }
                    if (liveCount > 0 && liveCount != v.commentsCount) {
                        v.copy(commentsCount = liveCount)
                    } else {
                        v
                    }
                }
            }
            VerticalVideoFeed(
                videos = liveVideos,
                currentUser = currentUser,
                likedVideoIds = likedVideoIds,
                followingUserIds = followingUserIds,
                sounds = sounds,
                onToggleLike = onToggleLike,
                onToggleFollow = onToggleFollow,
                onOpenComments = onOpenComments,
                onShareVideo = onShareVideo,
                onReportVideo = onReportVideo,
                onOpenUserProfile = onOpenUserProfile,
                onSelectHashtag = onSelectHashtag,
                onSelectSound = onSelectSound
            )
        }

        // Top Floating Header: Following | For You + Search + Admin quick icon
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                if (currentUser?.isAdmin == true) {
                    IconButton(
                        onClick = onOpenAdminDashboard,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.38f), CircleShape)
                            .testTag("top_admin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Dashboard",
                            tint = PulseCyan
                        )
                    }
                }

                // Following / For You Switcher
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    FeedTabPill(
                        title = "Following",
                        selected = feedMode == FeedMode.FOLLOWING,
                        tag = "tab_following",
                        onClick = { onSelectFeedMode(FeedMode.FOLLOWING) }
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(Color.White.copy(alpha = 0.28f))
                    )
                    FeedTabPill(
                        title = "For You",
                        selected = feedMode == FeedMode.FOR_YOU,
                        tag = "tab_for_you",
                        onClick = { onSelectFeedMode(FeedMode.FOR_YOU) }
                    )
                }

                IconButton(
                    onClick = onNavigateToDiscover,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.38f), CircleShape)
                        .testTag("top_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search and Discover",
                        tint = Color.White
                    )
                }
            }

            // Active Filter Banner (Hashtag or Sound)
            if (activeHashtagFilter != null || activeSoundFilter != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = CyberViolet.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (activeHashtagFilter != null) {
                                "Filtered by $activeHashtagFilter"
                            } else {
                                "Sound: ${activeSoundFilter?.title}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear filter",
                            tint = Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onClearFilter() }
                        )
                    }
                }
            }
        }

        // Comments Modal Bottom Sheet
        if (activeCommentVideo != null) {
            val liveActiveVideo = remember(activeCommentVideo, comments) {
                val liveCount = comments.count { it.videoId == activeCommentVideo.id }
                if (liveCount > 0 && liveCount != activeCommentVideo.commentsCount) {
                    activeCommentVideo.copy(commentsCount = liveCount)
                } else {
                    activeCommentVideo
                }
            }
            CommentsBottomSheet(
                video = liveActiveVideo,
                comments = comments,
                currentUser = currentUser,
                onDismiss = onCloseComments,
                onSubmitComment = onSubmitComment,
                onToggleLikeComment = onToggleLikeComment,
                onDeleteComment = onDeleteComment,
                onReportComment = onReportComment,
                onOpenUserProfile = { uid ->
                    onCloseComments()
                    onOpenUserProfile(uid)
                }
            )
        }
    }
}

/**
 * Core vertical video Feed component powered by VerticalPager with next-video preloading
 * (`beyondViewportPageCount = 1`) and automatic page-visibility video player playback logic.
 */
@Composable
fun VerticalVideoFeed(
    videos: List<VideoEntity>,
    currentUser: UserEntity?,
    likedVideoIds: Set<Long>,
    followingUserIds: Set<Long>,
    sounds: List<SoundEntity>,
    onToggleLike: (VideoEntity) -> Unit,
    onToggleFollow: (Long) -> Unit,
    onOpenComments: (VideoEntity) -> Unit,
    onShareVideo: (VideoEntity) -> Unit,
    onReportVideo: (VideoEntity) -> Unit,
    onOpenUserProfile: (Long) -> Unit,
    onSelectHashtag: (String) -> Unit,
    onSelectSound: (SoundEntity) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { videos.size })
    var isAudioMuted by remember { mutableStateOf(false) }

    VerticalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier
            .fillMaxSize()
            .testTag("vertical_video_pager")
    ) { pageIndex ->
        val video = videos[pageIndex]
        val isSettledActivePage = pagerState.currentPage == pageIndex && !pagerState.isScrollInProgress
        val nextPlaceholderUrl = videos.getOrNull(pageIndex + 1)?.videoUri

        val activeSoundBpm = remember(video.soundId, sounds) {
            sounds.find { it.id == video.soundId }?.bpm ?: 124
        }

        SingleVerticalVideoPage(
            video = video,
            soundBpm = activeSoundBpm,
            isActivePage = isSettledActivePage,
            isAudioMuted = isAudioMuted,
            onToggleMute = { isAudioMuted = !isAudioMuted },
            isLiked = video.id in likedVideoIds,
            isFollowingCreator = video.creatorId in followingUserIds,
            isOwnVideo = currentUser?.id == video.creatorId,
            nextPreloadedUrl = nextPlaceholderUrl,
            onToggleLike = { onToggleLike(video) },
            onToggleFollow = { onToggleFollow(video.creatorId) },
            onOpenComments = { onOpenComments(video) },
            onShare = { onShareVideo(video) },
            onReport = { onReportVideo(video) },
            onOpenCreator = { onOpenUserProfile(video.creatorId) },
            onClickHashtag = onSelectHashtag,
            onClickSound = {
                val sound = sounds.find { it.id == video.soundId }
                if (sound != null) onSelectSound(sound)
            }
        )
    }
}

@Composable
private fun FeedTabPill(
    title: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag(tag)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.62f)
            )
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(if (selected) 24.dp else 0.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ElectricCoral)
        )
    }
}

@Composable
private fun SingleVerticalVideoPage(
    video: VideoEntity,
    soundBpm: Int = 124,
    isActivePage: Boolean,
    isAudioMuted: Boolean,
    onToggleMute: () -> Unit,
    isLiked: Boolean,
    isFollowingCreator: Boolean,
    isOwnVideo: Boolean,
    nextPreloadedUrl: String?,
    onToggleLike: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenComments: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit,
    onOpenCreator: () -> Unit,
    onClickHashtag: (String) -> Unit,
    onClickSound: () -> Unit
) {
    var playbackStatus by remember(video.id) { mutableStateOf(VideoPlaybackStatus.PLAYING) }
    var showDoubleTapHeart by remember { mutableStateOf(false) }
    var playbackProgress by remember(video.id) { mutableFloatStateOf(0f) }

    // Automatically reset and start playback when page becomes visible
    LaunchedEffect(isActivePage, video.id) {
        if (isActivePage) {
            playbackStatus = VideoPlaybackStatus.BUFFERING
            delay(140L)
            playbackStatus = VideoPlaybackStatus.PLAYING
        } else {
            playbackStatus = VideoPlaybackStatus.PAUSED
            playbackProgress = 0f
        }
    }

    val isPlaying = isActivePage && playbackStatus == VideoPlaybackStatus.PLAYING

    // Video player playback loop progression
    LaunchedEffect(isPlaying, video.durationSec) {
        if (isPlaying) {
            val stepMs = 100L
            val totalSteps = (video.durationSec.coerceAtLeast(5) * 1000L) / stepMs
            while (true) {
                delay(stepMs)
                playbackProgress = (playbackProgress + 1f / totalSteps) % 1f
            }
        }
    }

    LaunchedEffect(showDoubleTapHeart) {
        if (showDoubleTapHeart) {
            delay(650)
            showDoubleTapHeart = false
        }
    }

    val elapsedSec = (playbackProgress * video.durationSec.coerceAtLeast(5)).toInt()
    val formattedTime = "${formatVideoDurationMss(elapsedSec)} / ${formatVideoDurationMss(video.durationSec)}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(video.id, isLiked, playbackStatus) {
                detectTapGestures(
                    onTap = {
                        playbackStatus = if (playbackStatus == VideoPlaybackStatus.PAUSED) {
                            VideoPlaybackStatus.PLAYING
                        } else {
                            VideoPlaybackStatus.PAUSED
                        }
                    },
                    onDoubleTap = {
                        showDoubleTapHeart = true
                        if (!isLiked) onToggleLike()
                    }
                )
            }
            .testTag("video_surface_${video.id}")
    ) {
        InteractiveVideoCanvas(
            themeIndex = video.thumbnailThemeIndex,
            headline = video.thumbnailHeadline,
            videoUri = video.videoUri,
            isPlaying = isPlaying,
            isActivePage = isActivePage,
            isAudioMuted = isAudioMuted,
            soundBpm = soundBpm
        )

        // Brief buffering spinner when switching streams
        AnimatedVisibility(
            visible = isActivePage && playbackStatus == VideoPlaybackStatus.BUFFERING,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator(
                color = ElectricCoral,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
        }

        // Tap-to-pause center indicator
        AnimatedVisibility(
            visible = playbackStatus == VideoPlaybackStatus.PAUSED && isActivePage,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video Paused - Tap to Play",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        // Double-tap heart burst animation
        AnimatedVisibility(
            visible = showDoubleTapHeart,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Liked",
                tint = ElectricCoral,
                modifier = Modifier.size(112.dp)
            )
        }

        // Vertical Video Feed Overlay with right-side interactive buttons (Like, Comment, Share)
        VerticalFeedOverlay(
            video = video,
            playbackStatus = playbackStatus,
            formattedTime = formattedTime,
            playbackProgress = playbackProgress,
            isPlaying = isPlaying,
            isAudioMuted = isAudioMuted,
            isLiked = isLiked,
            isFollowingCreator = isFollowingCreator,
            isOwnVideo = isOwnVideo,
            nextPreloadedUrl = nextPreloadedUrl,
            onToggleLike = onToggleLike,
            onToggleFollow = onToggleFollow,
            onOpenComments = onOpenComments,
            onShare = onShare,
            onToggleMute = onToggleMute,
            onReport = onReport,
            onOpenCreator = onOpenCreator,
            onClickHashtag = onClickHashtag,
            onClickSound = onClickSound
        )
    }
}

@Composable
private fun EmptyFeedView(
    feedMode: FeedMode,
    hasFilter: Boolean,
    onSwitchToForYou: () -> Unit,
    onNavigateToDiscover: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (hasFilter) "No videos match this filter"
            else if (feedMode == FeedMode.FOLLOWING) "No videos from followed accounts yet"
            else "No videos in feed",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Explore trending creators on Discover or switch back to the For You feed.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onSwitchToForYou,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral)
            ) {
                Text("Explore For You")
            }
            Button(
                onClick = onNavigateToDiscover,
                colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
            ) {
                Text("Discover Creators")
            }
        }
    }
}
