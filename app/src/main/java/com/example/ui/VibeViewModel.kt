package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.FirebaseAuthManager
import com.example.data.engine.FeedRecommendationEngine
import com.example.data.engine.VideoProcessingEngine
import com.example.data.firestore.FirestoreProfilePersistenceEngine
import com.example.data.local.AppDatabase
import com.example.data.local.BlockedUserEntity
import com.example.data.local.CommentEntity
import com.example.data.local.CreatorFollowUiState
import com.example.data.local.HashtagEntity
import com.example.data.local.MessageEntity
import com.example.data.local.NotificationCenterUiState
import com.example.data.local.NotificationEntity
import com.example.data.local.NotificationEventType
import com.example.data.local.NotificationManagementService
import com.example.data.local.ReportEntity
import com.example.data.local.SoundEntity
import com.example.data.local.UserEntity
import com.example.data.local.VideoEntity
import com.example.data.repository.VibeRepository
import com.example.ui.components.buildVideoShareLink
import com.example.ui.components.launchVideoShareIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    HOME, DISCOVER, CREATE, INBOX, PROFILE
}

enum class FeedMode {
    FOR_YOU, FOLLOWING
}

enum class OverlayScreen {
    NONE,
    SETTINGS,
    ADMIN_DASHBOARD,
    VIEW_USER_PROFILE,
    CHAT_THREAD,
    AUTH_SCREEN
}

data class ReportTarget(
    val targetType: String, // VIDEO, COMMENT, USER
    val targetId: Long,
    val summary: String
)

data class UploadProgressState(
    val isUploading: Boolean = false,
    val stageLabel: String = "",
    val progressFraction: Float = 0f,
    val currentStepIndex: Int = 0,
    val compressionSummary: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class VibeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VibeRepository =
        VibeRepository(AppDatabase.getInstance(application).vibeDao(), application)

    val notificationService: NotificationManagementService
        get() = repository.notificationService

    val firestoreProfileEngine: FirestoreProfilePersistenceEngine
        get() = repository.firestoreProfileEngine

    private val authManager: FirebaseAuthManager = FirebaseAuthManager(application)

    val isFirebaseConfigured: Boolean
        get() = authManager.isFirebaseConfigured

    private val _currentUserId = MutableStateFlow<Long?>(1L)
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _activeTab = MutableStateFlow(MainTab.HOME)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val tabHistory = ArrayDeque<MainTab>()

    private val _feedMode = MutableStateFlow(FeedMode.FOR_YOU)
    val feedMode: StateFlow<FeedMode> = _feedMode.asStateFlow()

    private val _overlayScreen = MutableStateFlow(OverlayScreen.NONE)
    val overlayScreen: StateFlow<OverlayScreen> = _overlayScreen.asStateFlow()

    private val _selectedProfileUserId = MutableStateFlow<Long?>(null)
    val selectedProfileUserId: StateFlow<Long?> = _selectedProfileUserId.asStateFlow()

    private val _selectedChatPeerId = MutableStateFlow<Long?>(null)
    val selectedChatPeerId: StateFlow<Long?> = _selectedChatPeerId.asStateFlow()

    private val _activeCommentVideo = MutableStateFlow<VideoEntity?>(null)
    val activeCommentVideo: StateFlow<VideoEntity?> = _activeCommentVideo.asStateFlow()

    private val _activeReportTarget = MutableStateFlow<ReportTarget?>(null)
    val activeReportTarget: StateFlow<ReportTarget?> = _activeReportTarget.asStateFlow()

    private val _selectedHashtagFilter = MutableStateFlow<String?>(null)
    val selectedHashtagFilter: StateFlow<String?> = _selectedHashtagFilter.asStateFlow()

    private val _selectedSoundFilter = MutableStateFlow<SoundEntity?>(null)
    val selectedSoundFilter: StateFlow<SoundEntity?> = _selectedSoundFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedNotificationFilter = MutableStateFlow(NotificationEventType.ALL)
    val selectedNotificationFilter: StateFlow<NotificationEventType> = _selectedNotificationFilter.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _uploadProgress = MutableStateFlow(UploadProgressState())
    val uploadProgress: StateFlow<UploadProgressState> = _uploadProgress.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = _currentUserId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.observeUser(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVideosForAdmin: StateFlow<List<VideoEntity>> = repository.allVideosForAdmin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allComments: StateFlow<List<CommentEntity>> = repository.allComments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hashtags: StateFlow<List<HashtagEntity>> = repository.hashtags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sounds: StateFlow<List<SoundEntity>> = repository.sounds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<ReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedVideoIds: StateFlow<Set<Long>> = _currentUserId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observeLikesForUser(id)
        }
        .combine(flowOf(Unit)) { likes, _ -> likes.map { it.videoId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val followingUserIds: StateFlow<Set<Long>> = _currentUserId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observeFollowingForUser(id)
        }
        .combine(flowOf(Unit)) { follows, _ -> follows.map { it.followingId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val followUiState: StateFlow<CreatorFollowUiState> = combine(
        _currentUserId,
        allUsers,
        repository.allFollows,
        repository.approvedVideos
    ) { currentId, users, follows, videos ->
        CreatorFollowUiState.fromDomainData(
            currentUserId = currentId,
            allUsers = users,
            allFollows = follows,
            allVideos = videos
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CreatorFollowUiState())

    val blockedUsers: StateFlow<List<BlockedUserEntity>> = _currentUserId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observeBlockedUsers(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = _currentUserId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observeNotifications(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationCenterState: StateFlow<NotificationCenterUiState> = combine(
        _currentUserId,
        notifications,
        _selectedNotificationFilter,
        blockedUsers
    ) { uid, notifs, filter, blocked ->
        val blockedSet = blocked.map { it.blockedUserId }.toSet()
        NotificationCenterUiState.fromNotifications(
            recipientId = uid,
            notifications = notifs,
            selectedFilter = filter,
            blockedActorIds = blockedSet
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationCenterUiState())

    val messages: StateFlow<List<MessageEntity>> = _currentUserId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observeMessages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeVideoComments: StateFlow<List<CommentEntity>> = _activeCommentVideo
        .flatMapLatest { video ->
            if (video == null) flowOf(emptyList()) else repository.observeCommentsForVideo(video.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feedVideos: StateFlow<List<VideoEntity>> = combine(
        repository.approvedVideos,
        _feedMode,
        followingUserIds,
        blockedUsers,
        currentUser
    ) { videos, mode, followingIds, blocked, user ->
        val blockedSet = blocked.map { it.blockedUserId }.toSet()
        val filtered = videos.filter { video ->
            val notBlocked = video.creatorId !in blockedSet
            val ageOk = !(user?.ageSafetyMode == true && video.ageRestricted)
            val matchesMode = when (mode) {
                FeedMode.FOR_YOU -> true
                FeedMode.FOLLOWING -> video.creatorId in followingIds || video.creatorId == user?.id
            }
            notBlocked && ageOk && matchesMode
        }
        if (mode == FeedMode.FOR_YOU) {
            FeedRecommendationEngine.rankVideosForUser(
                videos = filtered,
                likedVideoIds = likedVideoIds.value,
                followingUserIds = followingIds
            )
        } else {
            filtered
        }
    }.combine(_selectedHashtagFilter) { videos, tagFilter ->
        if (tagFilter.isNullOrBlank()) videos
        else videos.filter { it.hashtags.contains(tagFilter, ignoreCase = true) }
    }.combine(_selectedSoundFilter) { videos, soundFilter ->
        if (soundFilter == null) videos
        else videos.filter { it.soundId == soundFilter.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val defaultId = repository.ensureSeedData()
            _currentUserId.value = defaultId
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun selectTab(tab: MainTab) {
        if ((tab == MainTab.CREATE || tab == MainTab.INBOX || tab == MainTab.PROFILE) && _currentUserId.value == null) {
            _overlayScreen.value = OverlayScreen.AUTH_SCREEN
            return
        }
        _overlayScreen.value = OverlayScreen.NONE
        val current = _activeTab.value
        if (current != tab) {
            tabHistory.addLast(current)
            if (tabHistory.size > 10) {
                tabHistory.removeFirst()
            }
            _activeTab.value = tab
        }
    }

    fun navigateBack(): Boolean {
        if (_activeCommentVideo.value != null) {
            _activeCommentVideo.value = null
            return true
        }
        if (_activeReportTarget.value != null) {
            _activeReportTarget.value = null
            return true
        }
        if (_overlayScreen.value != OverlayScreen.NONE) {
            _overlayScreen.value = OverlayScreen.NONE
            return true
        }
        if (tabHistory.isNotEmpty()) {
            _activeTab.value = tabHistory.removeLast()
            return true
        }
        if (_activeTab.value != MainTab.HOME) {
            _activeTab.value = MainTab.HOME
            return true
        }
        return false
    }

    fun setFeedMode(mode: FeedMode) {
        _feedMode.value = mode
    }

    fun setHashtagFilter(tag: String?) {
        _selectedHashtagFilter.value = tag
        if (tag != null) {
            _selectedSoundFilter.value = null
            _feedMode.value = FeedMode.FOR_YOU
            _overlayScreen.value = OverlayScreen.NONE
            _activeTab.value = MainTab.HOME
        }
    }

    fun setSoundFilter(sound: SoundEntity?) {
        _selectedSoundFilter.value = sound
        if (sound != null) {
            _selectedHashtagFilter.value = null
            _feedMode.value = FeedMode.FOR_YOU
            _overlayScreen.value = OverlayScreen.NONE
            _activeTab.value = MainTab.HOME
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openUserProfile(userId: Long) {
        if (userId == _currentUserId.value) {
            _overlayScreen.value = OverlayScreen.NONE
            _activeTab.value = MainTab.PROFILE
        } else {
            _selectedProfileUserId.value = userId
            _overlayScreen.value = OverlayScreen.VIEW_USER_PROFILE
        }
    }

    fun openChatThread(peerUserId: Long) {
        val myId = _currentUserId.value
        if (myId == null) {
            _overlayScreen.value = OverlayScreen.AUTH_SCREEN
            return
        }
        _selectedChatPeerId.value = peerUserId
        _overlayScreen.value = OverlayScreen.CHAT_THREAD
        viewModelScope.launch {
            repository.markConversationRead(myId, peerUserId)
        }
    }

    fun openSettings() {
        _overlayScreen.value = OverlayScreen.SETTINGS
    }

    fun openAdminDashboard() {
        _overlayScreen.value = OverlayScreen.ADMIN_DASHBOARD
    }

    fun openAuthScreen() {
        _overlayScreen.value = OverlayScreen.AUTH_SCREEN
    }

    fun closeOverlay() {
        _overlayScreen.value = OverlayScreen.NONE
    }

    fun openComments(video: VideoEntity) {
        _activeCommentVideo.value = video
    }

    fun closeComments() {
        _activeCommentVideo.value = null
    }

    fun openReportDialog(targetType: String, targetId: Long, summary: String) {
        _activeReportTarget.value = ReportTarget(targetType, targetId, summary)
    }

    fun closeReportDialog() {
        _activeReportTarget.value = null
    }

    // --- AUTH ACTIONS ---
    fun login(usernameOrEmail: String, passwordPlain: String) {
        viewModelScope.launch {
            val trimmed = usernameOrEmail.trim()
            if (trimmed.contains("@") && authManager.isFirebaseConfigured) {
                val fbResult = authManager.signInWithEmail(trimmed, passwordPlain)
                if (fbResult.isSuccess) {
                    val fbAccount = fbResult.getOrNull()!!
                    val localRes = repository.login(fbAccount.email, passwordPlain)
                    if (localRes.isSuccess) {
                        val user = localRes.getOrNull()!!
                        _currentUserId.value = user.id
                        _overlayScreen.value = OverlayScreen.NONE
                        showMessage("Signed in via Firebase Auth as @${user.username}!")
                        return@launch
                    } else {
                        val created = repository.signUp(
                            username = fbAccount.email.substringBefore("@"),
                            displayName = fbAccount.displayName,
                            email = fbAccount.email,
                            passwordPlain = passwordPlain,
                            isAdminAccount = false
                        )
                        created.onSuccess { user ->
                            _currentUserId.value = user.id
                            _overlayScreen.value = OverlayScreen.NONE
                            showMessage("Signed in via Firebase Auth as @${user.username}!")
                            return@launch
                        }
                    }
                }
            }

            val res = repository.login(usernameOrEmail, passwordPlain)
            res.onSuccess { user ->
                _currentUserId.value = user.id
                _overlayScreen.value = OverlayScreen.NONE
                showMessage("Welcome back, @${user.username}!")
            }.onFailure { err ->
                showMessage(err.message ?: "Login failed")
            }
        }
    }

    fun signUp(
        username: String,
        displayName: String,
        email: String,
        passwordPlain: String,
        isAdminAccount: Boolean
    ) {
        viewModelScope.launch {
            if (email.contains("@") && authManager.isFirebaseConfigured) {
                authManager.signUpWithEmail(email, passwordPlain, displayName)
            }
            val res = repository.signUp(username, displayName, email, passwordPlain, isAdminAccount)
            res.onSuccess { user ->
                _currentUserId.value = user.id
                _overlayScreen.value = OverlayScreen.NONE
                showMessage("Account @${user.username} created!")
            }.onFailure { err ->
                showMessage(err.message ?: "Sign up failed")
            }
        }
    }

    fun signInWithGoogle(activityContext: Context, serverClientId: String = "") {
        viewModelScope.launch {
            val credResult = authManager.signInWithGoogleCredentialManager(activityContext, serverClientId)
            credResult.onSuccess { account ->
                val existing = repository.login(account.email, "")
                if (existing.isSuccess) {
                    val user = existing.getOrNull()!!
                    _currentUserId.value = user.id
                    _overlayScreen.value = OverlayScreen.NONE
                    showMessage("Signed in with Google as @${user.username}!")
                } else {
                    val created = repository.signUp(
                        username = account.email.substringBefore("@"),
                        displayName = account.displayName,
                        email = account.email,
                        passwordPlain = "google_oauth",
                        isAdminAccount = false
                    )
                    created.onSuccess { user ->
                        _currentUserId.value = user.id
                        _overlayScreen.value = OverlayScreen.NONE
                        showMessage("Google account @${user.username} connected!")
                    }.onFailure { err ->
                        showMessage(err.message ?: "Could not link Google account")
                    }
                }
            }.onFailure { err ->
                showMessage(
                    "Google Sign-In via Credential Manager: ${err.localizedMessage ?: "Configure OAuth Web Client ID in google-services.json"}"
                )
            }
        }
    }

    fun resetPassword(usernameOrEmail: String, newPassword: String) {
        viewModelScope.launch {
            if (usernameOrEmail.contains("@") && authManager.isFirebaseConfigured) {
                authManager.sendPasswordReset(usernameOrEmail)
            }
            val res = repository.resetPassword(usernameOrEmail, newPassword)
            res.onSuccess { msg -> showMessage(msg) }
                .onFailure { err -> showMessage(err.message ?: "Reset failed") }
        }
    }

    fun changePassword(newPassword: String) {
        val uid = _currentUserId.value ?: return
        if (newPassword.length < 4) {
            showMessage("Password must be at least 4 characters.")
            return
        }
        viewModelScope.launch {
            repository.changePassword(uid, newPassword)
            showMessage("Password updated successfully.")
        }
    }

    fun logout() {
        authManager.signOut()
        _currentUserId.value = null
        _overlayScreen.value = OverlayScreen.AUTH_SCREEN
        showMessage("Logged out.")
    }

    fun deleteCurrentAccount() {
        val uid = _currentUserId.value ?: return
        viewModelScope.launch {
            repository.deleteAccount(uid)
            _currentUserId.value = null
            _overlayScreen.value = OverlayScreen.AUTH_SCREEN
            showMessage("Your account and videos have been deleted.")
        }
    }

    // --- SOCIAL & VIDEO ACTIONS ---
    fun toggleLikeVideo(video: VideoEntity) {
        val user = currentUser.value
        if (user == null) {
            _overlayScreen.value = OverlayScreen.AUTH_SCREEN
            return
        }
        viewModelScope.launch {
            repository.toggleLikeVideo(video, user)
        }
    }

    fun shareVideo(video: VideoEntity, launchSystemChooser: Boolean = true) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val shareLink = buildVideoShareLink(video)
            if (launchSystemChooser) {
                launchVideoShareIntent(app, video)
            }
            try {
                val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                clipboard?.setPrimaryClip(
                    android.content.ClipData.newPlainText("VibeStream Video Link", shareLink)
                )
            } catch (_: Exception) {
            }
            repository.incrementShareCount(video)
            showMessage("Sharing video link: $shareLink")
        }
    }

    fun followCreator(targetUserId: Long) {
        val user = currentUser.value
        if (user == null) {
            _overlayScreen.value = OverlayScreen.AUTH_SCREEN
            return
        }
        viewModelScope.launch {
            val isNowFollowing = repository.followCreator(user, targetUserId)
            if (isNowFollowing) {
                showMessage("Following creator")
            }
        }
    }

    fun unfollowCreator(targetUserId: Long) {
        val user = currentUser.value
        if (user == null) {
            _overlayScreen.value = OverlayScreen.AUTH_SCREEN
            return
        }
        viewModelScope.launch {
            repository.unfollowCreator(user, targetUserId)
            showMessage("Unfollowed creator")
        }
    }

    fun toggleFollow(targetUserId: Long) {
        val user = currentUser.value
        if (user == null) {
            _overlayScreen.value = OverlayScreen.AUTH_SCREEN
            return
        }
        viewModelScope.launch {
            val isNowFollowing = repository.toggleFollow(user, targetUserId)
            showMessage(if (isNowFollowing) "Following creator" else "Unfollowed creator")
        }
    }

    fun submitComment(content: String, parentComment: CommentEntity? = null) {
        val user = currentUser.value
        val video = _activeCommentVideo.value
        if (user == null || video == null) {
            _overlayScreen.value = OverlayScreen.AUTH_SCREEN
            return
        }
        viewModelScope.launch {
            val res = repository.addComment(video, user, content, parentComment)
            res.onSuccess {
                _activeCommentVideo.value = video.copy(commentsCount = video.commentsCount + 1)
            }.onFailure { err ->
                showMessage(err.message ?: "Could not post comment")
            }
        }
    }

    fun toggleLikeComment(comment: CommentEntity) {
        viewModelScope.launch {
            repository.toggleLikeComment(comment)
        }
    }

    fun deleteComment(comment: CommentEntity) {
        val video = _activeCommentVideo.value ?: return
        viewModelScope.launch {
            repository.deleteComment(comment, video)
            _activeCommentVideo.value = video.copy(commentsCount = (video.commentsCount - 1).coerceAtLeast(0))
            showMessage("Comment deleted.")
        }
    }

    fun uploadVideoWithCompression(
        caption: String,
        hashtagsInput: String,
        sound: SoundEntity,
        localVideoUri: String,
        thumbnailUri: String?,
        thumbnailThemeIndex: Int,
        thumbnailHeadline: String,
        durationSec: Int,
        targetBitrateKbps: Int,
        resolution: String = "1080x1920"
    ) {
        val user = currentUser.value
        if (user == null) {
            _overlayScreen.value = OverlayScreen.AUTH_SCREEN
            return
        }
        viewModelScope.launch {
            val app = getApplication<Application>()
            val artifact = VideoProcessingEngine.processAndPackageVideo(
                context = app,
                sourceVideoUri = localVideoUri,
                customThumbnailUri = thumbnailUri,
                themeIndex = thumbnailThemeIndex,
                headline = thumbnailHeadline.ifBlank { caption.take(18).uppercase() },
                durationSec = durationSec,
                targetBitrateKbps = targetBitrateKbps,
                resolution = resolution,
                soundBpm = sound.bpm
            ) { stepIndex, progress, stageLabel, summary ->
                _uploadProgress.value = UploadProgressState(
                    isUploading = true,
                    stageLabel = stageLabel,
                    progressFraction = progress,
                    currentStepIndex = stepIndex,
                    compressionSummary = summary
                )
                delay(160)
            }

            val result = repository.uploadProcessedVideo(
                creator = user,
                caption = caption,
                rawHashtags = hashtagsInput,
                sound = sound,
                localVideoUri = artifact.outputVideoUri,
                thumbnailUri = artifact.thumbnailUri,
                thumbnailThemeIndex = thumbnailThemeIndex,
                thumbnailHeadline = thumbnailHeadline,
                durationSec = artifact.probedMetadata.durationSec,
                targetBitrateKbps = targetBitrateKbps,
                resolution = resolution
            )
            _uploadProgress.value = UploadProgressState(isUploading = false)
            result.onSuccess {
                _feedMode.value = FeedMode.FOR_YOU
                _selectedHashtagFilter.value = null
                _selectedSoundFilter.value = null
                _activeTab.value = MainTab.HOME
                showMessage("Video processed (${artifact.compressionSummary}) & published!")
            }.onFailure { err ->
                showMessage(err.message ?: "Upload failed")
            }
        }
    }

    fun deleteVideo(videoId: Long) {
        viewModelScope.launch {
            repository.deleteVideo(videoId)
            showMessage("Video deleted.")
        }
    }

    fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        avatarColorHex: String,
        avatarUri: String? = null
    ) {
        val user = currentUser.value ?: return
        val uploadedVideos = allVideosForAdmin.value.filter { it.creatorId == user.id }
        viewModelScope.launch {
            val res = repository.updateProfile(
                user = user,
                displayName = displayName,
                username = username,
                bio = bio,
                avatarColorHex = avatarColorHex,
                avatarUri = avatarUri,
                uploadedVideos = uploadedVideos
            )
            res.onSuccess {
                showMessage("Profile saved & synced to Firebase Firestore!")
            }.onFailure { err ->
                showMessage(err.message ?: "Update failed")
            }
        }
    }

    fun updateUserSettings(updated: UserEntity) {
        viewModelScope.launch {
            repository.updateUserSettings(updated)
            showMessage("Settings saved.")
        }
    }

    fun setNotificationFilter(filter: NotificationEventType) {
        _selectedNotificationFilter.value = filter
    }

    fun markSingleNotificationRead(notificationId: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun markNotificationsRead() {
        val uid = _currentUserId.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsRead(uid)
            showMessage("All notifications marked as read.")
        }
    }

    fun deleteNotification(notificationId: Long) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId)
            showMessage("Notification removed.")
        }
    }

    fun clearAllNotifications() {
        val uid = _currentUserId.value ?: return
        viewModelScope.launch {
            repository.clearAllNotifications(uid)
            showMessage("All notifications cleared.")
        }
    }

    fun sendDirectMessage(peer: UserEntity, text: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.sendDirectMessage(user, peer, text)
            res.onFailure { err -> showMessage(err.message ?: "Message failed") }
        }
    }

    fun blockUser(targetUser: UserEntity) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.blockUser(user, targetUser)
            if (_overlayScreen.value == OverlayScreen.VIEW_USER_PROFILE || _overlayScreen.value == OverlayScreen.CHAT_THREAD) {
                _overlayScreen.value = OverlayScreen.NONE
            }
            showMessage("Blocked @${targetUser.username}. Their content is now hidden.")
        }
    }

    fun unblockUser(blockedUserId: Long) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.unblockUser(user.id, blockedUserId)
            showMessage("User unblocked.")
        }
    }

    fun submitReport(reason: String) {
        val user = currentUser.value ?: return
        val target = _activeReportTarget.value ?: return
        viewModelScope.launch {
            repository.submitReport(
                reporter = user,
                targetType = target.targetType,
                targetId = target.targetId,
                targetSummary = target.summary,
                reason = reason
            )
            _activeReportTarget.value = null
            showMessage("Report submitted to Safety & Moderation team.")
        }
    }

    // --- ADMIN ACTIONS ---
    fun adminUpdateReport(report: ReportEntity, newStatus: String, notes: String) {
        viewModelScope.launch {
            repository.adminUpdateReportStatus(report, newStatus, notes)
            showMessage("Report #${report.id} marked as $newStatus.")
        }
    }

    fun adminSetVideoModeration(video: VideoEntity, status: String) {
        viewModelScope.launch {
            repository.adminSetVideoModeration(video, status)
            showMessage("Video #${video.id} status set to $status.")
        }
    }

    fun adminSetUserStatus(user: UserEntity, status: String) {
        viewModelScope.launch {
            repository.adminSetUserAccountStatus(user, status)
            showMessage("Account @${user.username} status set to $status.")
        }
    }
}
