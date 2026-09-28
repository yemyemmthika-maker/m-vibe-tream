package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FeedMode
import com.example.ui.MainTab
import com.example.ui.OverlayScreen
import com.example.ui.VibeViewModel
import com.example.ui.components.ReportContentDialog
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CreateUploadScreen
import com.example.ui.screens.DirectMessageChatScreen
import com.example.ui.screens.DiscoverSearchScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.VibeStreamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vibeViewModel: VibeViewModel = viewModel()
            val isDark by vibeViewModel.isDarkTheme.collectAsStateWithLifecycle()

            VibeStreamTheme(darkTheme = isDark) {
                VibeStreamApp(viewModel = vibeViewModel)
            }
        }
    }
}

@Composable
fun VibeStreamApp(viewModel: VibeViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val feedMode by viewModel.feedMode.collectAsStateWithLifecycle()
    val overlayScreen by viewModel.overlayScreen.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val feedVideos by viewModel.feedVideos.collectAsStateWithLifecycle()
    val allVideos by viewModel.allVideosForAdmin.collectAsStateWithLifecycle()
    val likedVideoIds by viewModel.likedVideoIds.collectAsStateWithLifecycle()
    val followingUserIds by viewModel.followingUserIds.collectAsStateWithLifecycle()
    val followUiState by viewModel.followUiState.collectAsStateWithLifecycle()
    val hashtags by viewModel.hashtags.collectAsStateWithLifecycle()
    val sounds by viewModel.sounds.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val selectedNotificationFilter by viewModel.selectedNotificationFilter.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()
    val allReports by viewModel.allReports.collectAsStateWithLifecycle()
    val activeCommentVideo by viewModel.activeCommentVideo.collectAsStateWithLifecycle()
    val activeVideoComments by viewModel.activeVideoComments.collectAsStateWithLifecycle()
    val activeReportTarget by viewModel.activeReportTarget.collectAsStateWithLifecycle()
    val selectedHashtagFilter by viewModel.selectedHashtagFilter.collectAsStateWithLifecycle()
    val selectedSoundFilter by viewModel.selectedSoundFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedProfileUserId by viewModel.selectedProfileUserId.collectAsStateWithLifecycle()
    val selectedChatPeerId by viewModel.selectedChatPeerId.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val saveableStateHolder = rememberSaveableStateHolder()

    // Intercept back navigation whenever not on the root Home tab or when an overlay/modal is open
    val shouldInterceptBack = overlayScreen != OverlayScreen.NONE ||
        activeTab != MainTab.HOME ||
        activeCommentVideo != null ||
        activeReportTarget != null

    BackHandler(enabled = shouldInterceptBack) {
        viewModel.navigateBack()
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val unreadInboxCount = remember(notifications, messages, currentUser) {
        val unreadNotifs = notifications.count { !it.isRead }
        val unreadMsgs = messages.count { !it.isRead && it.receiverId == currentUser?.id }
        unreadNotifs + unreadMsgs
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (overlayScreen == OverlayScreen.NONE) {
                BottomNavigationBar(
                    activeTab = activeTab,
                    unreadInboxCount = unreadInboxCount,
                    onSelectTab = { viewModel.selectTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (overlayScreen) {
                OverlayScreen.AUTH_SCREEN -> {
                    AuthScreen(
                        allUsers = allUsers,
                        currentUser = currentUser,
                        isFirebaseConfigured = viewModel.isFirebaseConfigured,
                        onBack = if (currentUser != null) ({ viewModel.closeOverlay() }) else null,
                        onLogin = { u, p -> viewModel.login(u, p) },
                        onSignUp = { u, d, e, p, admin -> viewModel.signUp(u, d, e, p, admin) },
                        onGoogleSignIn = { ctx -> viewModel.signInWithGoogle(ctx) },
                        onResetPassword = { u, p -> viewModel.resetPassword(u, p) },
                        onLogout = { viewModel.logout() }
                    )
                }

                OverlayScreen.SETTINGS -> {
                    val user = currentUser
                    if (user != null) {
                        SettingsScreen(
                            currentUser = user,
                            isDarkTheme = isDarkTheme,
                            blockedUsers = blockedUsers,
                            onBack = { viewModel.closeOverlay() },
                            onToggleDarkTheme = { viewModel.toggleTheme() },
                            onUpdateSettings = { viewModel.updateUserSettings(it) },
                            onChangePassword = { viewModel.changePassword(it) },
                            onUnblockUser = { viewModel.unblockUser(it) },
                            onSubmitProblemReport = { text ->
                                viewModel.openReportDialog("PLATFORM_FEEDBACK", user.id, text)
                            },
                            onOpenAdminDashboard = { viewModel.openAdminDashboard() },
                            onLogout = { viewModel.logout() },
                            onDeleteAccount = { viewModel.deleteCurrentAccount() }
                        )
                    }
                }

                OverlayScreen.ADMIN_DASHBOARD -> {
                    AdminDashboardScreen(
                        users = allUsers,
                        videos = allVideos,
                        reports = allReports,
                        onBack = { viewModel.closeOverlay() },
                        onUpdateReport = { report, status, notes ->
                            viewModel.adminUpdateReport(report, status, notes)
                        },
                        onSetVideoModeration = { video, status ->
                            viewModel.adminSetVideoModeration(video, status)
                        },
                        onSetUserStatus = { user, status ->
                            viewModel.adminSetUserStatus(user, status)
                        }
                    )
                }

                OverlayScreen.VIEW_USER_PROFILE -> {
                    val targetUser = allUsers.find { it.id == selectedProfileUserId }
                    if (targetUser != null) {
                        val userVideos = allVideos.filter {
                            it.creatorId == targetUser.id && it.moderationStatus != "REMOVED"
                        }
                        ProfileScreen(
                            profileUser = targetUser,
                            isOwnProfile = targetUser.id == currentUser?.id,
                            isFollowing = targetUser.id in followingUserIds,
                            userVideos = userVideos,
                            likedVideos = emptyList(),
                            onBack = { viewModel.closeOverlay() },
                            onToggleFollow = { viewModel.toggleFollow(targetUser.id) },
                            onOpenMessage = { viewModel.openChatThread(targetUser.id) },
                            onUpdateProfile = { d, u, b, c, uri -> viewModel.updateProfile(d, u, b, c, uri) },
                            onDeleteVideo = { viewModel.deleteVideo(it) },
                            onOpenSettings = { viewModel.openSettings() },
                            onOpenAdminDashboard = { viewModel.openAdminDashboard() },
                            onOpenAuthSwitcher = { viewModel.openAuthScreen() },
                            onBlockUser = { viewModel.blockUser(targetUser) },
                            onReportUser = {
                                viewModel.openReportDialog(
                                    targetType = "USER",
                                    targetId = targetUser.id,
                                    summary = "Account @${targetUser.username} (${targetUser.displayName})"
                                )
                            },
                            onPlayVideo = {
                                viewModel.closeOverlay()
                                viewModel.selectTab(MainTab.HOME)
                            },
                            followUiState = followUiState,
                            onToggleFollowCreator = { viewModel.toggleFollow(it) },
                            onOpenCreatorProfile = { viewModel.openUserProfile(it) },
                            firestoreEngine = viewModel.firestoreProfileEngine
                        )
                    } else {
                        viewModel.closeOverlay()
                    }
                }

                OverlayScreen.CHAT_THREAD -> {
                    val peer = allUsers.find { it.id == selectedChatPeerId }
                    val me = currentUser
                    if (peer != null && me != null) {
                        DirectMessageChatScreen(
                            peerUser = peer,
                            currentUser = me,
                            messages = messages,
                            onBack = { viewModel.closeOverlay() },
                            onSendMessage = { text -> viewModel.sendDirectMessage(peer, text) },
                            onBlockPeer = { viewModel.blockUser(peer) },
                            onReportPeer = {
                                viewModel.openReportDialog(
                                    targetType = "USER",
                                    targetId = peer.id,
                                    summary = "Direct Message account @${peer.username}"
                                )
                            },
                            onOpenPeerProfile = { viewModel.openUserProfile(peer.id) }
                        )
                    } else {
                        viewModel.closeOverlay()
                    }
                }

                OverlayScreen.NONE -> {
                    // State-based view switcher for main tabs (Home, Discover, Create, Inbox, Profile)
                    Crossfade(
                        targetState = activeTab,
                        animationSpec = tween(durationMillis = 180),
                        label = "main_tab_state_switcher"
                    ) { currentTab ->
                        saveableStateHolder.SaveableStateProvider(currentTab.name) {
                            when (currentTab) {
                                MainTab.HOME -> {
                                    HomeFeedScreen(
                                        videos = feedVideos,
                                        currentUser = currentUser,
                                        feedMode = feedMode,
                                        likedVideoIds = likedVideoIds,
                                        followingUserIds = followingUserIds,
                                        activeHashtagFilter = selectedHashtagFilter,
                                        activeSoundFilter = selectedSoundFilter,
                                        activeCommentVideo = activeCommentVideo,
                                        comments = activeVideoComments,
                                        sounds = sounds,
                                        onSelectFeedMode = { viewModel.setFeedMode(it) },
                                        onClearFilter = {
                                            viewModel.setHashtagFilter(null)
                                            viewModel.setSoundFilter(null)
                                        },
                                        onToggleLike = { viewModel.toggleLikeVideo(it) },
                                        onToggleFollow = { viewModel.toggleFollow(it) },
                                        onOpenComments = { viewModel.openComments(it) },
                                        onCloseComments = { viewModel.closeComments() },
                                        onSubmitComment = { text, parent -> viewModel.submitComment(text, parent) },
                                        onToggleLikeComment = { viewModel.toggleLikeComment(it) },
                                        onDeleteComment = { viewModel.deleteComment(it) },
                                        onShareVideo = { viewModel.shareVideo(it, launchSystemChooser = false) },
                                        onReportVideo = { video ->
                                            viewModel.openReportDialog(
                                                targetType = "VIDEO",
                                                targetId = video.id,
                                                summary = "Video by @${video.creatorUsername}: \"${video.caption.take(40)}\""
                                            )
                                        },
                                        onReportComment = { comment ->
                                            viewModel.openReportDialog(
                                                targetType = "COMMENT",
                                                targetId = comment.id,
                                                summary = "Comment by @${comment.authorUsername}: \"${comment.content.take(40)}\""
                                            )
                                        },
                                        onOpenUserProfile = { viewModel.openUserProfile(it) },
                                        onSelectHashtag = { viewModel.setHashtagFilter(it) },
                                        onSelectSound = { viewModel.setSoundFilter(it) },
                                        onNavigateToDiscover = { viewModel.selectTab(MainTab.DISCOVER) },
                                        onOpenAdminDashboard = { viewModel.openAdminDashboard() }
                                    )
                                }

                                MainTab.DISCOVER -> {
                                    BackHandler { viewModel.selectTab(MainTab.HOME) }
                                    DiscoverSearchScreen(
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                        users = allUsers,
                                        videos = allVideos.filter { it.moderationStatus != "REMOVED" },
                                        hashtags = hashtags,
                                        sounds = sounds,
                                        followingUserIds = followingUserIds,
                                        currentUserId = currentUser?.id,
                                        onToggleFollow = { viewModel.toggleFollow(it) },
                                        onOpenUserProfile = { viewModel.openUserProfile(it) },
                                        onSelectHashtag = { viewModel.setHashtagFilter(it) },
                                        onSelectSound = { viewModel.setSoundFilter(it) },
                                        onPlayVideoInFeed = {
                                            viewModel.setFeedMode(FeedMode.FOR_YOU)
                                            viewModel.selectTab(MainTab.HOME)
                                        },
                                        followUiState = followUiState
                                    )
                                }

                                MainTab.CREATE -> {
                                    BackHandler { viewModel.selectTab(MainTab.HOME) }
                                    val myUploaded = allVideos.filter {
                                        it.creatorId == currentUser?.id && it.moderationStatus != "REMOVED"
                                    }
                                    CreateUploadScreen(
                                        sounds = sounds,
                                        trendingHashtags = hashtags,
                                        myVideos = myUploaded,
                                        uploadProgress = uploadProgress,
                                        onUploadVideo = { cap, tags, snd, uri, thumbUri, themeIdx, headline, dur, bitrate, res ->
                                            viewModel.uploadVideoWithCompression(
                                                caption = cap,
                                                hashtagsInput = tags,
                                                sound = snd,
                                                localVideoUri = uri,
                                                thumbnailUri = thumbUri,
                                                thumbnailThemeIndex = themeIdx,
                                                thumbnailHeadline = headline,
                                                durationSec = dur,
                                                targetBitrateKbps = bitrate,
                                                resolution = res
                                            )
                                        },
                                        onDeleteMyVideo = { viewModel.deleteVideo(it) },
                                        onShowMessage = { viewModel.showMessage(it) }
                                    )
                                }

                                MainTab.INBOX -> {
                                    BackHandler { viewModel.selectTab(MainTab.HOME) }
                                    InboxScreen(
                                        notifications = notifications,
                                        messages = messages,
                                        allUsers = allUsers,
                                        blockedUsers = blockedUsers,
                                        currentUser = currentUser,
                                        onMarkAllNotificationsRead = { viewModel.markNotificationsRead() },
                                        onOpenChatThread = { viewModel.openChatThread(it) },
                                        onOpenUserProfile = { viewModel.openUserProfile(it) },
                                        selectedNotificationFilter = selectedNotificationFilter,
                                        onNotificationFilterChange = { viewModel.setNotificationFilter(it) },
                                        onMarkNotificationRead = { viewModel.markSingleNotificationRead(it) },
                                        onDeleteNotification = { viewModel.deleteNotification(it) },
                                        onClearAllNotifications = { viewModel.clearAllNotifications() }
                                    )
                                }

                                MainTab.PROFILE -> {
                                    BackHandler { viewModel.selectTab(MainTab.HOME) }
                                    val user = currentUser
                                    if (user == null) {
                                        AuthScreen(
                                            allUsers = allUsers,
                                            currentUser = null,
                                            isFirebaseConfigured = viewModel.isFirebaseConfigured,
                                            onBack = { viewModel.selectTab(MainTab.HOME) },
                                            onLogin = { u, p -> viewModel.login(u, p) },
                                            onSignUp = { u, d, e, p, admin -> viewModel.signUp(u, d, e, p, admin) },
                                            onGoogleSignIn = { ctx -> viewModel.signInWithGoogle(ctx) },
                                            onResetPassword = { u, p -> viewModel.resetPassword(u, p) },
                                            onLogout = { viewModel.logout() }
                                        )
                                    } else {
                                        val myVideos = allVideos.filter {
                                            it.creatorId == user.id && it.moderationStatus != "REMOVED"
                                        }
                                        val myLikedVideos = allVideos.filter {
                                            it.id in likedVideoIds && it.moderationStatus != "REMOVED"
                                        }
                                        ProfileScreen(
                                            profileUser = user,
                                            isOwnProfile = true,
                                            isFollowing = false,
                                            userVideos = myVideos,
                                            likedVideos = myLikedVideos,
                                            onToggleFollow = {},
                                            onOpenMessage = {},
                                            onUpdateProfile = { d, u, b, c, uri -> viewModel.updateProfile(d, u, b, c, uri) },
                                            onDeleteVideo = { viewModel.deleteVideo(it) },
                                            onOpenSettings = { viewModel.openSettings() },
                                            onOpenAdminDashboard = { viewModel.openAdminDashboard() },
                                            onOpenAuthSwitcher = { viewModel.openAuthScreen() },
                                            onBlockUser = {},
                                            onReportUser = {},
                                            onPlayVideo = { viewModel.selectTab(MainTab.HOME) },
                                            followUiState = followUiState,
                                            onToggleFollowCreator = { viewModel.toggleFollow(it) },
                                            onOpenCreatorProfile = { viewModel.openUserProfile(it) },
                                            firestoreEngine = viewModel.firestoreProfileEngine
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeReportTarget != null) {
                ReportContentDialog(
                    target = activeReportTarget!!,
                    onDismiss = { viewModel.closeReportDialog() },
                    onSubmit = { reason -> viewModel.submitReport(reason) }
                )
            }
        }
    }
}

private data class BottomNavDestination(
    val tab: MainTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun BottomNavigationBar(
    activeTab: MainTab,
    unreadInboxCount: Int,
    onSelectTab: (MainTab) -> Unit
) {
    val destinations = remember {
        listOf(
            BottomNavDestination(
                tab = MainTab.HOME,
                label = "Home",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                testTag = "bottom_nav_home"
            ),
            BottomNavDestination(
                tab = MainTab.DISCOVER,
                label = "Discover",
                selectedIcon = Icons.Filled.Explore,
                unselectedIcon = Icons.Outlined.Explore,
                testTag = "bottom_nav_discover"
            ),
            BottomNavDestination(
                tab = MainTab.CREATE,
                label = "Create",
                selectedIcon = Icons.Filled.AddCircle,
                unselectedIcon = Icons.Outlined.AddCircleOutline,
                testTag = "bottom_nav_create"
            ),
            BottomNavDestination(
                tab = MainTab.INBOX,
                label = "Inbox",
                selectedIcon = Icons.Filled.Inbox,
                unselectedIcon = Icons.Outlined.Inbox,
                testTag = "bottom_nav_inbox"
            ),
            BottomNavDestination(
                tab = MainTab.PROFILE,
                label = "Profile",
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                testTag = "bottom_nav_profile"
            )
        )
    }

    NavigationBar(
        windowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("bottom_navigation_bar")
    ) {
        destinations.forEach { dest ->
            val selected = activeTab == dest.tab
            val isCreate = dest.tab == MainTab.CREATE

            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(dest.tab) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(dest.testTag),
                icon = {
                    when {
                        dest.tab == MainTab.INBOX && unreadInboxCount > 0 -> {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = ElectricCoral) {
                                        Text(
                                            text = unreadInboxCount.toString(),
                                            color = Color.White
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            }
                        }

                        isCreate -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(ElectricCoral, CyberViolet)
                                        )
                                    )
                                    .padding(horizontal = 14.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = dest.selectedIcon,
                                    contentDescription = dest.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        else -> {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = dest.label
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ElectricCoral,
                    selectedTextColor = ElectricCoral,
                    indicatorColor = ElectricCoral.copy(alpha = 0.14f)
                )
            )
        }
    }
}
