package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.AppConfig
import com.example.data.engine.CreatorAnalyticsEngine
import com.example.data.firestore.FirestoreProfilePersistenceEngine
import com.example.data.firestore.FirestoreUserProfileDocument
import com.example.data.local.CreatorFollowUiState
import java.util.Locale
import kotlinx.coroutines.launch
import com.example.data.local.FollowRelationshipStatus
import com.example.data.local.UserEntity
import com.example.data.local.VideoEntity
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.FollowDirectoryTab
import com.example.ui.components.FollowedCreatorsBottomSheet
import com.example.ui.components.FollowedCreatorsTrackerSection
import com.example.ui.components.InteractiveVideoCanvas
import com.example.ui.components.VideoDurationCornerOverlay
import com.example.ui.components.VideoPlaybackProgressBar
import com.example.ui.components.VideoPlayerVolumeToggleButton
import com.example.ui.components.formatCompactCount
import com.example.ui.components.launchVideoShareIntent
import com.example.ui.components.parseHexColor
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCyan
import kotlinx.coroutines.delay

@Composable
fun ProfileScreen(
    profileUser: UserEntity,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    userVideos: List<VideoEntity>,
    likedVideos: List<VideoEntity>,
    onBack: (() -> Unit)? = null,
    onToggleFollow: () -> Unit,
    onOpenMessage: () -> Unit,
    onUpdateProfile: (String, String, String, String, String?) -> Unit,
    onDeleteVideo: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onOpenAuthSwitcher: () -> Unit,
    onBlockUser: () -> Unit,
    onReportUser: () -> Unit,
    onPlayVideo: (VideoEntity) -> Unit,
    followUiState: CreatorFollowUiState = CreatorFollowUiState(),
    onToggleFollowCreator: (Long) -> Unit = {},
    onOpenCreatorProfile: (Long) -> Unit = {},
    firestoreEngine: FirestoreProfilePersistenceEngine? = null
) {
    if (onBack != null) {
        BackHandler { onBack() }
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeFirestoreEngine = remember(firestoreEngine, context) {
        firestoreEngine ?: FirestoreProfilePersistenceEngine(context.applicationContext)
    }
    val firestoreProfilesMap by activeFirestoreEngine.profilesFlow.collectAsState()
    val persistedFirestoreDoc = firestoreProfilesMap[profileUser.id]

    // Automatically persist and synchronize profile data + uploaded videos grid with Firebase Firestore
    LaunchedEffect(
        profileUser.id,
        profileUser.username,
        profileUser.displayName,
        profileUser.bio,
        profileUser.avatarColorHex,
        profileUser.avatarUri,
        userVideos.size
    ) {
        activeFirestoreEngine.startRealtimeProfileListener(profileUser.id)
        activeFirestoreEngine.persistUserProfile(
            user = profileUser,
            uploadedVideos = userVideos
        )
    }

    val effectiveHandle = persistedFirestoreDoc?.handle ?: "@${profileUser.username.removePrefix("@")}"
    val effectiveDisplayName = persistedFirestoreDoc?.displayName ?: profileUser.displayName
    val effectiveBio = persistedFirestoreDoc?.bio ?: profileUser.bio
    val effectiveAvatarHex = persistedFirestoreDoc?.avatarColorHex ?: profileUser.avatarColorHex
    val effectiveAvatarUri = persistedFirestoreDoc?.avatarUri ?: profileUser.avatarUri

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Uploaded Videos, 1 = Liked Videos
    var showEditDialog by remember { mutableStateOf(false) }
    var previewModalVideo by remember { mutableStateOf<VideoEntity?>(null) }
    var activeFollowDirectoryTab by remember { mutableStateOf<FollowDirectoryTab?>(null) }

    val displayedVideos = if (selectedTab == 0) userVideos else likedVideos

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Text(
                        text = "@${profileUser.username}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("profile_username_header")
                    )
                }

                if (isOwnProfile) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (profileUser.isAdmin) {
                            IconButton(
                                onClick = onOpenAdminDashboard,
                                modifier = Modifier.testTag("profile_admin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Dashboard",
                                    tint = PulseCyan
                                )
                            }
                        }
                        IconButton(
                            onClick = onOpenAuthSwitcher,
                            modifier = Modifier.testTag("profile_switch_account_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwitchAccount,
                                contentDescription = "Switch Account"
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("profile_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                } else {
                    Row {
                        IconButton(
                            onClick = onReportUser,
                            modifier = Modifier.testTag("profile_report_user_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Report account",
                                tint = NeonAmber
                            )
                        }
                        IconButton(
                            onClick = onBlockUser,
                            modifier = Modifier.testTag("profile_block_user_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Block user",
                                tint = ElectricCoral
                            )
                        }
                    }
                }
            }
        }

        // Profile Picture, Username, Display Name, Bio & Follower/Following Stats
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.testTag("user_profile_avatar")
                ) {
                    CreatorAvatar(
                        username = profileUser.username,
                        colorHex = effectiveAvatarHex,
                        avatarUri = effectiveAvatarUri,
                        size = 96.dp,
                        isVerified = profileUser.isVerified,
                        onAvatarClick = if (isOwnProfile) ({ showEditDialog = true }) else null
                    )
                    if (isOwnProfile) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricCoral,
                            modifier = Modifier
                                .size(30.dp)
                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                .clickable { showEditDialog = true }
                                .testTag("change_profile_photo_badge")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Change profile picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = effectiveDisplayName,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.testTag("profile_display_name")
                )
                Text(
                    text = effectiveHandle,
                    style = MaterialTheme.typography.labelLarge,
                    color = PulseCyan,
                    modifier = Modifier.testTag("user_profile_handle")
                )

                if (profileUser.isAdmin) {
                    Surface(
                        color = PulseCyan.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Platform Administrator",
                            style = MaterialTheme.typography.labelLarge,
                            color = PulseCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = effectiveBio.ifBlank { "No bio added yet." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("profile_bio_text")
                )

                // Firebase Firestore Persistence Status Badge
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firestore_profile_persistence_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Firebase Firestore Synced",
                                tint = MintSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Persisted in Firebase Firestore",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MintSuccess
                                )
                                Text(
                                    text = "Doc: ${persistedFirestoreDoc?.documentPath ?: "user_profiles/${profileUser.id}"} • ${userVideos.size} videos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("firestore_document_path_label")
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    activeFirestoreEngine.persistUserProfile(
                                        user = profileUser.copy(
                                            displayName = effectiveDisplayName,
                                            username = effectiveHandle.removePrefix("@"),
                                            bio = effectiveBio,
                                            avatarColorHex = effectiveAvatarHex,
                                            avatarUri = effectiveAvatarUri
                                        ),
                                        uploadedVideos = userVideos
                                    )
                                }
                            },
                            modifier = Modifier.testTag("firestore_sync_now_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync profile with Firebase Firestore",
                                tint = PulseCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Followers / Following / Likes Stats Row
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStatItem(
                            value = formatCompactCount(profileUser.followingCount),
                            label = "Following",
                            tag = "profile_following_count",
                            onClick = if (isOwnProfile) {
                                { activeFollowDirectoryTab = FollowDirectoryTab.FOLLOWING }
                            } else null
                        )
                        ProfileStatItem(
                            value = formatCompactCount(profileUser.followersCount),
                            label = "Followers",
                            tag = "profile_followers_count",
                            onClick = if (isOwnProfile) {
                                { activeFollowDirectoryTab = FollowDirectoryTab.FOLLOWERS }
                            } else null
                        )
                        ProfileStatItem(
                            value = formatCompactCount(profileUser.totalLikesReceived),
                            label = "Likes",
                            tag = "profile_likes_count"
                        )
                    }
                }

                // Action Buttons
                if (isOwnProfile) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { showEditDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Profile")
                        }
                        if (profileUser.isAdmin) {
                            OutlinedButton(
                                onClick = onOpenAdminDashboard,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Admin Panel")
                            }
                        }
                    }

                    // Active Account Login Details Card on Own Profile
                    val defaultPwd = AppConfig.defaultPasswordFor(profileUser.username)
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_login_details_card")
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = PulseCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Login & Account Details",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Surface(
                                    color = if (profileUser.isAdmin) PulseCyan.copy(alpha = 0.16f) else CyberViolet.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (profileUser.isAdmin) "ADMIN ACCOUNT" else "CREATOR ACCOUNT",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (profileUser.isAdmin) PulseCyan else Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Username: @${profileUser.username}  •  Password: $defaultPwd",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Email: ${profileUser.email}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedButton(
                                onClick = onOpenAuthSwitcher,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_login_details_button")
                            ) {
                                Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Switch Account / View All Login Credentials")
                            }
                        }
                    }

                    // Creator Studio Analytics & Telemetry Engine Card
                    val telemetry = remember(profileUser, userVideos) {
                        CreatorAnalyticsEngine.computeCreatorTelemetry(profileUser, userVideos)
                    }
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_telemetry_card")
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
                                    text = "Studio Telemetry Engine",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PulseCyan
                                )
                                Surface(
                                    color = MintSuccess.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Trust Score: ${telemetry.creatorTrustScore}/100",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MintSuccess,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = String.format(Locale.US, "Engagement: %.1f%%", telemetry.engagementRatePercent),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = String.format(Locale.US, "Virality K: %.2f", telemetry.viralityKFactor),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = String.format(Locale.US, "Watch: %.1fh", telemetry.estimatedWatchTimeHours),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    val relationship = followUiState.relationshipWith(profileUser.id)
                    val isMutual = relationship == FollowRelationshipStatus.MUTUAL_FRIENDS
                    val followsMe = relationship == FollowRelationshipStatus.FOLLOWED_BY
                    val buttonLabel = when {
                        isFollowing && isMutual -> "Friends"
                        isFollowing -> "Following"
                        followsMe -> "Follow Back"
                        else -> "Follow"
                    }

                    if (followsMe || isMutual) {
                        Surface(
                            color = if (isMutual) MintSuccess.copy(alpha = 0.16f) else PulseCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("profile_relationship_badge")
                        ) {
                            Text(
                                text = if (isMutual) "Mutual Friends" else "Follows you",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isMutual) MintSuccess else PulseCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onToggleFollow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else ElectricCoral
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_follow_button")
                        ) {
                            Text(buttonLabel)
                        }
                        OutlinedButton(
                            onClick = onOpenMessage,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_message_button")
                        ) {
                            Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Message")
                        }
                    }
                }
            }
        }

        // Followed Creators Tracker Section on Own Profile
        if (isOwnProfile) {
            item {
                FollowedCreatorsTrackerSection(
                    followUiState = followUiState,
                    onToggleFollow = onToggleFollowCreator,
                    onOpenCreatorProfile = onOpenCreatorProfile,
                    onOpenFullDirectory = { tab -> activeFollowDirectoryTab = tab },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Uploaded Videos / Liked Videos Section Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    leadingIcon = {
                        Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("Uploaded (${userVideos.size})") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_tab_uploaded")
                )
                if (isOwnProfile) {
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        leadingIcon = {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Liked (${likedVideos.size})") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_tab_liked")
                    )
                }
            }
        }

        // 3-Column Vertical Video Grid
        if (displayedVideos.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp)
                        .testTag("user_profile_videos_empty"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 0) "No uploaded videos yet." else "No liked videos yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(displayedVideos.chunked(3).withIndex().toList()) { (rowIndex, rowTriplet) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .then(if (rowIndex == 0) Modifier.testTag("user_profile_videos_grid") else Modifier),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowTriplet.forEach { video ->
                        ProfileVideoTile(
                            video = video,
                            canDelete = isOwnProfile && selectedTab == 0,
                            modifier = Modifier.weight(1f),
                            onClick = { previewModalVideo = video },
                            onDelete = { onDeleteVideo(video.id) }
                        )
                    }
                    repeat(3 - rowTriplet.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            user = profileUser.copy(
                displayName = effectiveDisplayName,
                username = effectiveHandle.removePrefix("@"),
                bio = effectiveBio,
                avatarColorHex = effectiveAvatarHex,
                avatarUri = effectiveAvatarUri
            ),
            onDismiss = { showEditDialog = false },
            onSave = { displayName, username, bio, colorHex, avatarUri ->
                coroutineScope.launch {
                    activeFirestoreEngine.updateProfileDetailsInFirestore(
                        userId = profileUser.id,
                        displayName = displayName,
                        handleOrUsername = username,
                        bio = bio,
                        avatarColorHex = colorHex,
                        avatarUri = avatarUri,
                        existingUser = profileUser,
                        uploadedVideos = userVideos
                    )
                }
                onUpdateProfile(displayName, username, bio, colorHex, avatarUri)
                showEditDialog = false
            }
        )
    }

    if (previewModalVideo != null) {
        val video = previewModalVideo!!
        ProfileVideoPreviewDialog(
            video = video,
            canDelete = isOwnProfile,
            onDismiss = { previewModalVideo = null },
            onOpenInFeed = {
                previewModalVideo = null
                onPlayVideo(video)
            },
            onDelete = {
                onDeleteVideo(video.id)
                previewModalVideo = null
            }
        )
    }

    if (activeFollowDirectoryTab != null) {
        FollowedCreatorsBottomSheet(
            followUiState = followUiState,
            initialTab = activeFollowDirectoryTab!!,
            onDismiss = { activeFollowDirectoryTab = null },
            onToggleFollow = onToggleFollowCreator,
            onOpenCreatorProfile = { uid ->
                activeFollowDirectoryTab = null
                onOpenCreatorProfile(uid)
            }
        )
    }
}

/**
 * Reusable, standalone User Profile Component backed by Firebase Firestore (`user_profiles/{userId}`).
 * Displays the user's avatar, handle (`@username`), bio, and a 3-column grid of their uploaded videos,
 * while persisting and hydrating profile updates through [FirestoreProfilePersistenceEngine].
 */
@Composable
fun UserProfileComponent(
    user: UserEntity,
    uploadedVideos: List<VideoEntity>,
    modifier: Modifier = Modifier,
    isOwnProfile: Boolean = true,
    firestoreEngine: FirestoreProfilePersistenceEngine? = null,
    onProfileUpdated: (UserEntity, FirestoreUserProfileDocument) -> Unit = { _, _ -> },
    onVideoClick: (VideoEntity) -> Unit = {},
    onDeleteVideo: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val engine = remember(firestoreEngine, context) {
        firestoreEngine ?: FirestoreProfilePersistenceEngine(context.applicationContext)
    }
    val profilesMap by engine.profilesFlow.collectAsState()
    val persistedDoc = profilesMap[user.id]

    // Hydrate from Firestore on first composition or persist initial user + uploadedVideos
    LaunchedEffect(user.id) {
        engine.startRealtimeProfileListener(user.id)
        val existing = engine.fetchUserProfileDocument(
            userId = user.id,
            fallbackUser = user,
            fallbackVideos = uploadedVideos
        )
        if (existing == null) {
            engine.persistUserProfile(user, uploadedVideos)
        }
    }

    // Keep uploaded videos in sync in Firestore when video list changes
    LaunchedEffect(uploadedVideos) {
        val currentUserState = persistedDoc?.toUserEntity(user) ?: user
        engine.persistUserProfile(currentUserState, uploadedVideos)
    }

    val currentDisplayName = persistedDoc?.displayName ?: user.displayName
    val currentHandle = persistedDoc?.handle ?: "@${user.username.removePrefix("@")}"
    val currentBio = persistedDoc?.bio ?: user.bio
    val currentAvatarHex = persistedDoc?.avatarColorHex ?: user.avatarColorHex
    val currentAvatarUri = persistedDoc?.avatarUri ?: user.avatarUri
    val displayedVideos = remember(persistedDoc, uploadedVideos) {
        if (uploadedVideos.isNotEmpty()) uploadedVideos
        else persistedDoc?.toVideoEntities().orEmpty()
    }

    var showEditModal by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("user_profile_component"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.testTag("user_profile_avatar")
                ) {
                    CreatorAvatar(
                        username = currentHandle.removePrefix("@"),
                        colorHex = currentAvatarHex,
                        avatarUri = currentAvatarUri,
                        size = 92.dp,
                        isVerified = user.isVerified,
                        onAvatarClick = if (isOwnProfile) ({ showEditModal = true }) else null
                    )
                }

                Text(
                    text = currentDisplayName,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.testTag("profile_display_name")
                )

                Text(
                    text = currentHandle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = PulseCyan,
                    modifier = Modifier.testTag("user_profile_handle")
                )

                Text(
                    text = currentBio.ifBlank { "No bio added yet." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .testTag("user_profile_bio")
                )

                Surface(
                    color = MintSuccess.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("firestore_profile_persistence_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MintSuccess,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Firebase Firestore • ${persistedDoc?.documentPath ?: "user_profiles/${user.id}"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MintSuccess
                        )
                    }
                }

                if (isOwnProfile) {
                    Button(
                        onClick = { showEditModal = !showEditModal },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showEditModal) "Close Editor" else "Edit Profile")
                    }
                }

                if (showEditModal) {
                    var editDisplayName by remember(currentDisplayName) { mutableStateOf(currentDisplayName) }
                    var editUsername by remember(currentHandle) { mutableStateOf(currentHandle.removePrefix("@")) }
                    var editBio by remember(currentBio) { mutableStateOf(currentBio) }
                    var editHex by remember(currentAvatarHex) { mutableStateOf(currentAvatarHex) }
                    val avatarPalette = listOf("#FF3366", "#7C4DFF", "#00E5FF", "#FFB300", "#00E676")

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("firestore_profile_editor_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Edit Profile (Persisted to Firebase Firestore)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PulseCyan
                            )
                            OutlinedTextField(
                                value = editDisplayName,
                                onValueChange = { editDisplayName = it },
                                label = { Text("Display Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_display_name_input")
                            )
                            OutlinedTextField(
                                value = editUsername,
                                onValueChange = { editUsername = it },
                                label = { Text("Handle / Username") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_username_input")
                            )
                            OutlinedTextField(
                                value = editBio,
                                onValueChange = { editBio = it },
                                label = { Text("Bio") },
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_bio_input")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                avatarPalette.forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColor(hex))
                                            .border(
                                                width = if (editHex == hex) 2.5.dp else 0.dp,
                                                color = Color.White,
                                                shape = CircleShape
                                            )
                                            .clickable { editHex = hex }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showEditModal = false }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val updatedDoc = engine.updateProfileDetailsInFirestore(
                                            userId = user.id,
                                            displayName = editDisplayName,
                                            handleOrUsername = editUsername,
                                            bio = editBio,
                                            avatarColorHex = editHex,
                                            avatarUri = currentAvatarUri,
                                            existingUser = user,
                                            uploadedVideos = displayedVideos
                                        )
                                        onProfileUpdated(updatedDoc.toUserEntity(user), updatedDoc)
                                        showEditModal = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                                    modifier = Modifier.testTag("save_profile_button")
                                ) {
                                    Text("Save Changes")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Uploaded Videos (${displayedVideos.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.testTag("user_profile_uploaded_header")
                )
            }
        }

        if (displayedVideos.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                        .testTag("user_profile_videos_empty"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No uploaded videos yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(displayedVideos.chunked(3).withIndex().toList()) { (rowIndex, rowTriplet) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (rowIndex == 0) Modifier.testTag("user_profile_videos_grid") else Modifier),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowTriplet.forEach { video ->
                        ProfileVideoTile(
                            video = video,
                            canDelete = isOwnProfile,
                            modifier = Modifier.weight(1f),
                            onClick = { onVideoClick(video) },
                            onDelete = { onDeleteVideo(video.id) }
                        )
                    }
                    repeat(3 - rowTriplet.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatItem(
    value: String,
    label: String,
    tag: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(tag)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileVideoTile(
    video: VideoEntity,
    canDelete: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = parseHexColor(video.creatorAvatarHex)
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .height(172.dp)
            .clickable { onClick() }
            .testTag("profile_video_tile_${video.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1A142E),
                            accent.copy(alpha = 0.65f),
                            Color(0xFF0A0812)
                        )
                    )
                )
        ) {
            if (!video.thumbnailUri.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(video.thumbnailUri),
                    contentDescription = video.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                )
            }

            if (canDelete) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete video",
                        tint = ElectricCoral,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Text(
                text = video.thumbnailHeadline.ifBlank { "REEL" },
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 6.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                        )
                    )
                    .padding(6.dp)
            ) {
                Text(
                    text = video.caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = formatCompactCount(video.viewsCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = ElectricCoral,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = formatCompactCount(video.likesCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileVideoPreviewDialog(
    video: VideoEntity,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onOpenInFeed: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isAudioMuted by remember { mutableStateOf(false) }
    var playbackProgress by remember(video.id) { mutableFloatStateOf(0f) }

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { isPlaying = !isPlaying }
        ) {
            InteractiveVideoCanvas(
                themeIndex = video.thumbnailThemeIndex,
                headline = video.thumbnailHeadline,
                videoUri = video.videoUri,
                isPlaying = isPlaying && !isAudioMuted,
                isActivePage = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close preview", tint = Color.White)
                    }
                    VideoPlayerVolumeToggleButton(
                        isAudioMuted = isAudioMuted,
                        onToggleMute = { isAudioMuted = !isAudioMuted },
                        videoId = video.id,
                        testTagPrefix = "profile_preview_volume_toggle"
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { launchVideoShareIntent(context, video) },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("profile_preview_share_button_${video.id}")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share video", tint = PulseCyan)
                    }
                    Button(
                        onClick = onOpenInFeed,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                    ) {
                        Text("Open in Feed")
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete video", tint = ElectricCoral)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "@${video.creatorUsername}",
                    style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = video.caption,
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                )
                Text(
                    text = video.hashtags,
                    style = MaterialTheme.typography.labelLarge.copy(color = PulseCyan)
                )
            }

            VideoDurationCornerOverlay(
                durationSec = video.durationSec,
                videoId = video.id,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            )

            VideoPlaybackProgressBar(
                playbackProgress = playbackProgress,
                videoId = video.id,
                testTagPrefix = "profile_preview_progress_bar",
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String?) -> Unit
) {
    var displayName by remember { mutableStateOf(user.displayName) }
    var username by remember { mutableStateOf(user.username) }
    var bio by remember { mutableStateOf(user.bio) }
    var selectedHex by remember { mutableStateOf(user.avatarColorHex) }
    var selectedAvatarUri by remember { mutableStateOf(user.avatarUri) }

    val pickAvatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAvatarUri = uri.toString()
        }
    }

    val avatarPalette = listOf("#FF3366", "#7C4DFF", "#00E5FF", "#FFB300", "#00E676")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile Details") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live Avatar Preview + Upload Photo Button
                CreatorAvatar(
                    username = username.ifBlank { user.username },
                    colorHex = selectedHex,
                    avatarUri = selectedAvatarUri,
                    size = 76.dp,
                    isVerified = user.isVerified
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            pickAvatarLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("pick_profile_picture_button")
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Photo")
                    }
                    if (!selectedAvatarUri.isNullOrBlank()) {
                        TextButton(onClick = { selectedAvatarUri = null }) {
                            Text("Use Initials")
                        }
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_display_name_input")
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_username_input")
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_bio_input")
                )

                Text(
                    text = "Profile Ring & Monogram Accent",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    avatarPalette.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .border(
                                    width = if (selectedHex == hex) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(displayName, username, bio, selectedHex, selectedAvatarUri) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AuthScreen(
    allUsers: List<UserEntity>,
    currentUser: UserEntity?,
    isFirebaseConfigured: Boolean = false,
    onBack: (() -> Unit)? = null,
    onLogin: (String, String) -> Unit,
    onSignUp: (String, String, String, String, Boolean) -> Unit,
    onGoogleSignIn: (android.content.Context) -> Unit = {},
    onResetPassword: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    if (onBack != null) {
        BackHandler { onBack() }
    }

    val context = LocalContext.current
    var authMode by remember { mutableIntStateOf(0) } // 0 = Sign In, 1 = Sign Up, 2 = Forgot Password
    var usernameOrEmail by remember { mutableStateOf("nova.pulse") }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("pulse123") }
    var showPassword by remember { mutableStateOf(false) }
    var isAdminCheckbox by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppConfig.APP_NAME,
                        style = MaterialTheme.typography.displayLarge,
                        color = ElectricCoral
                    )
                    Text(
                        text = if (isFirebaseConfigured) {
                            "Firebase Auth & Credential Manager Active"
                        } else {
                            "Sign In / Sign Up (Firebase Auth + Local Sync)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            }
        }

        // Google Sign-In via Credential Manager Button
        item {
            OutlinedButton(
                onClick = { onGoogleSignIn(context) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_credential_signin_button")
            ) {
                Text(
                    text = "G",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = PulseCyan,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continue with Google (Credential Manager)",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        // Mode Selector
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = authMode == 0,
                    onClick = { authMode = 0 },
                    label = { Text("Login") },
                    modifier = Modifier.testTag("auth_mode_login")
                )
                FilterChip(
                    selected = authMode == 1,
                    onClick = { authMode = 1 },
                    label = { Text("Sign Up") },
                    modifier = Modifier.testTag("auth_mode_signup")
                )
                FilterChip(
                    selected = authMode == 2,
                    onClick = { authMode = 2 },
                    label = { Text("Forgot Password") },
                    modifier = Modifier.testTag("auth_mode_forgot")
                )
            }
        }

        // Form Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (authMode) {
                        0 -> {
                            Text("Sign In to Your Account", style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = usernameOrEmail,
                                onValueChange = { usernameOrEmail = it },
                                label = { Text("Username or Email") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_username_input")
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                visualTransformation = if (showPassword) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showPassword = !showPassword },
                                        modifier = Modifier.testTag("toggle_password_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showPassword) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input")
                            )
                            Text(
                                text = "Quick-Fill Login Credentials:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(AppConfig.DEMO_LOGIN_CREDENTIALS, key = { it.username }) { cred ->
                                    FilterChip(
                                        selected = usernameOrEmail.equals(cred.username, ignoreCase = true),
                                        onClick = {
                                            usernameOrEmail = cred.username
                                            password = cred.defaultPassword
                                            showPassword = true
                                        },
                                        label = {
                                            Text("@${cred.username} (${cred.defaultPassword})")
                                        },
                                        modifier = Modifier.testTag("quick_fill_${cred.username}")
                                    )
                                }
                            }
                            Button(
                                onClick = { onLogin(usernameOrEmail, password) },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("login_submit_button")
                            ) {
                                Text("Login")
                            }
                        }

                        1 -> {
                            Text("Create New Creator Account", style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = usernameOrEmail,
                                onValueChange = { usernameOrEmail = it },
                                label = { Text("Username (e.g. alex.vibe)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_username_input")
                            )
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Display Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_displayname_input")
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_email_input")
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_password_input")
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { isAdminCheckbox = !isAdminCheckbox }
                            ) {
                                Checkbox(
                                    checked = isAdminCheckbox,
                                    onCheckedChange = { isAdminCheckbox = it }
                                )
                                Text(
                                    text = "Grant Admin Dashboard privileges",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Button(
                                onClick = {
                                    onSignUp(
                                        usernameOrEmail,
                                        displayName,
                                        email.ifBlank { "$usernameOrEmail@vibestream.social" },
                                        password.ifBlank { "pass123" },
                                        isAdminCheckbox
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("signup_submit_button")
                            ) {
                                Text("Create Account")
                            }
                        }

                        2 -> {
                            Text("Reset Account Password", style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = usernameOrEmail,
                                onValueChange = { usernameOrEmail = it },
                                label = { Text("Username or Registered Email") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_username_input")
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("New Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_password_input")
                            )
                            Button(
                                onClick = {
                                    onResetPassword(usernameOrEmail, password.ifBlank { "pulse123" })
                                    authMode = 0
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("reset_submit_button")
                            ) {
                                Text("Update Password")
                            }
                        }
                    }
                }
            }
        }

        // Detailed Login Credentials & Autofill Reference Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_details_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = PulseCyan
                        )
                        Column {
                            Text(
                                text = "Available Login Details & Credentials",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Tap 'Fill' to populate the login form or 'Sign In' to switch immediately.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AppConfig.DEMO_LOGIN_CREDENTIALS.forEachIndexed { index, cred ->
                        if (index > 0) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        }
                        val isCurrent = currentUser?.username.equals(cred.username, ignoreCase = true)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isCurrent) ElectricCoral.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CreatorAvatar(
                                        username = cred.username,
                                        colorHex = cred.avatarColorHex,
                                        size = 38.dp,
                                        isVerified = true
                                    )
                                    Column {
                                        Text(
                                            text = cred.displayName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = cred.roleLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (cred.isAdmin) PulseCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isCurrent) {
                                    Surface(
                                        color = MintSuccess.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MintSuccess,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Username: ${cred.username}   •   Password: ${cred.defaultPassword}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                            Text(
                                text = "Email: ${cred.email}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        authMode = 0
                                        usernameOrEmail = cred.username
                                        password = cred.defaultPassword
                                        showPassword = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("fill_login_${cred.username}")
                                ) {
                                    Text("Fill Form")
                                }
                                Button(
                                    onClick = {
                                        usernameOrEmail = cred.username
                                        password = cred.defaultPassword
                                        onLogin(cred.username, cred.defaultPassword)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (cred.isAdmin) CyberViolet else ElectricCoral
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("instant_login_${cred.username}")
                                ) {
                                    Text("Sign In")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Instant Demo Account Switcher
        item {
            Text(
                text = "One-Tap Demo Account Switcher",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(allUsers.filter { it.accountStatus != "BANNED" }, key = { it.id }) { user ->
                    val defaultPwd = AppConfig.defaultPasswordFor(user.username)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentUser?.id == user.id) {
                                ElectricCoral.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        modifier = Modifier
                            .width(155.dp)
                            .clickable { onLogin(user.username, defaultPwd) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CreatorAvatar(
                                username = user.username,
                                colorHex = user.avatarColorHex,
                                avatarUri = user.avatarUri,
                                size = 44.dp,
                                isVerified = user.isVerified
                            )
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                            Text(
                                text = if (user.isAdmin) "Admin + Creator" else "Creator",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (user.isAdmin) PulseCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (currentUser != null) {
            item {
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out of @${currentUser.username}")
                }
            }
        }
    }
}
