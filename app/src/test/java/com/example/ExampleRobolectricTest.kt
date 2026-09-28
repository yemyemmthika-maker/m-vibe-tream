package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.example.data.engine.CreatorAnalyticsEngine
import com.example.data.engine.EpicEngineModel
import com.example.data.engine.FeedRecommendationEngine
import com.example.data.engine.PasswordSecurityEngine
import com.example.data.engine.SearchAndDiscoveryEngine
import com.example.data.engine.SynthAudioEngine
import com.example.data.engine.SystemNotificationEngine
import com.example.data.engine.TrustAndSafetyEngine
import com.example.data.engine.VideoProcessingEngine
import com.example.data.firestore.FirestoreProfilePersistenceEngine
import com.example.data.firestore.FirestoreUserProfileDocument
import com.example.data.local.AppDatabase
import com.example.data.local.CommentEntity
import com.example.data.local.FollowEntity
import com.example.data.local.FollowRelationshipStatus
import com.example.data.local.FollowSystemStateHolder
import com.example.data.local.HashtagEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.NotificationEventType
import com.example.data.local.NotificationManagementService
import com.example.data.local.SoundEntity
import com.example.data.local.UserEntity
import com.example.data.local.VideoEntity
import com.example.ui.FeedMode
import com.example.ui.components.FollowDirectoryTab
import com.example.ui.components.FollowedCreatorsSheetContent
import com.example.ui.components.FollowedCreatorsTrackerSection
import com.example.ui.components.NotificationListView
import com.example.ui.components.VerticalFeedOverlay
import com.example.ui.components.formatVideoDurationMss
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DiscoverSearchScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.UserProfileComponent
import com.example.ui.screens.VideoPlaybackStatus
import com.example.ui.theme.VibeStreamTheme
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w420dp-h880dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VibeStream", appName)
    }

    @Test
    fun `vertical feed overlay positions interactive like comment and share buttons vertically on right side`() {
        var likedClicked = 0
        var commentClicked = 0
        var shareClicked = 0

        val testVideo = VideoEntity(
            id = 42L,
            creatorId = 2L,
            creatorUsername = "nova_motion",
            creatorDisplayName = "Nova Motion",
            creatorAvatarHex = "#FF3366",
            creatorVerified = true,
            caption = "Testing vertical feed overlay actions #vibestream",
            hashtags = "#vibestream #motion",
            soundId = 1L,
            soundTitle = "Midnight Pulse",
            soundArtist = "Nova",
            videoUri = "https://cdn.vibestream.social/placeholders/stream_01_rooftop_neon.mp4",
            cdnStreamUrl = "https://cdn.vibestream.social/streams/42/master.m3u8",
            thumbnailThemeIndex = 0,
            thumbnailHeadline = "NEON",
            durationSec = 18,
            likesCount = 1200,
            commentsCount = 85,
            sharesCount = 34
        )

        composeTestRule.setContent {
            VibeStreamTheme(darkTheme = true) {
                Box(modifier = Modifier.size(width = 400.dp, height = 800.dp)) {
                    VerticalFeedOverlay(
                        video = testVideo,
                        playbackStatus = VideoPlaybackStatus.PLAYING,
                        formattedTime = "00:05 / 00:18",
                        playbackProgress = 0.25f,
                        isPlaying = true,
                        isAudioMuted = false,
                        isLiked = false,
                        isFollowingCreator = false,
                        isOwnVideo = false,
                        nextPreloadedUrl = null,
                        onToggleLike = { likedClicked++ },
                        onToggleFollow = {},
                        onOpenComments = { commentClicked++ },
                        onShare = { shareClicked++ },
                        onToggleMute = {},
                        onReport = {},
                        onOpenCreator = {},
                        onClickHashtag = {},
                        onClickSound = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        val overlayBounds = composeTestRule
            .onNodeWithTag("vertical_feed_overlay_42")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot

        val likeNode = composeTestRule.onNodeWithTag("like_button_42").assertIsDisplayed()
        val commentNode = composeTestRule.onNodeWithTag("comment_button_42").assertIsDisplayed()
        val shareNode = composeTestRule.onNodeWithTag("share_button_42").assertIsDisplayed()

        val likeBounds = likeNode.fetchSemanticsNode().boundsInRoot
        val commentBounds = commentNode.fetchSemanticsNode().boundsInRoot
        val shareBounds = shareNode.fetchSemanticsNode().boundsInRoot

        // Verify all three buttons are positioned on the right side of the screen
        val screenMidX = overlayBounds.width / 2f
        assertTrue("Like button should be on the right side", likeBounds.center.x > screenMidX)
        assertTrue("Comment button should be on the right side", commentBounds.center.x > screenMidX)
        assertTrue("Share button should be on the right side", shareBounds.center.x > screenMidX)

        // Verify buttons are stacked vertically (Like above Comment above Share)
        assertTrue("Like should be above Comment", likeBounds.top < commentBounds.top)
        assertTrue("Comment should be above Share", commentBounds.top < shareBounds.top)

        // Verify interactive tap callbacks
        likeNode.performClick()
        commentNode.performClick()
        shareNode.performClick()

        assertEquals(1, likedClicked)
        assertEquals(1, commentClicked)
        assertEquals(1, shareClicked)
    }

    @Test
    fun `clicking comment icon opens bottom sheet modal for specific video and allows viewing and adding comments`() {
        val currentUser = UserEntity(
            id = 1L,
            username = "nova.pulse",
            displayName = "Nova Pulse",
            email = "nova@vibestream.social",
            passwordHash = "pulse123",
            avatarColorHex = "#FF3366",
            isVerified = true
        )

        val video = VideoEntity(
            id = 101L,
            creatorId = 2L,
            creatorUsername = "kai.motion",
            creatorDisplayName = "Kai Motion",
            creatorAvatarHex = "#7C4DFF",
            creatorVerified = true,
            caption = "Rooftop neon step choreography!",
            hashtags = "#RooftopFlow #NeonPulse",
            soundId = 1L,
            soundTitle = "Midnight Prism",
            soundArtist = "Aria SynthLab",
            videoUri = "https://cdn.vibestream.social/placeholders/stream_01_rooftop_neon.mp4",
            cdnStreamUrl = "https://cdn.vibestream.social/streams/101/master.m3u8",
            thumbnailThemeIndex = 0,
            thumbnailHeadline = "ROOFTOP NEON STEP",
            durationSec = 15,
            likesCount = 240,
            commentsCount = 1,
            sharesCount = 12
        )

        val initialComment = CommentEntity(
            id = 501L,
            videoId = 101L,
            authorId = 3L,
            authorUsername = "aria.synth",
            authorDisplayName = "Aria SynthLab",
            authorAvatarHex = "#00E5FF",
            content = "Love the beat sync on this drop!",
            likesCount = 7
        )

        composeTestRule.setContent {
            VibeStreamTheme(darkTheme = true) {
                var activeCommentVideo by remember { mutableStateOf<VideoEntity?>(null) }
                val commentsList = remember { mutableStateListOf(initialComment) }

                Box(modifier = Modifier.size(width = 420.dp, height = 840.dp)) {
                    HomeFeedScreen(
                        videos = listOf(video),
                        currentUser = currentUser,
                        feedMode = FeedMode.FOR_YOU,
                        likedVideoIds = emptySet(),
                        followingUserIds = emptySet(),
                        activeHashtagFilter = null,
                        activeSoundFilter = null,
                        activeCommentVideo = activeCommentVideo,
                        comments = commentsList,
                        sounds = emptyList(),
                        onSelectFeedMode = {},
                        onClearFilter = {},
                        onToggleLike = {},
                        onToggleFollow = {},
                        onOpenComments = { clickedVideo -> activeCommentVideo = clickedVideo },
                        onCloseComments = { activeCommentVideo = null },
                        onSubmitComment = { text, parent ->
                            val targetVideo = activeCommentVideo ?: return@HomeFeedScreen
                            commentsList.add(
                                CommentEntity(
                                    id = (commentsList.size + 600).toLong(),
                                    videoId = targetVideo.id,
                                    authorId = currentUser.id,
                                    authorUsername = currentUser.username,
                                    authorDisplayName = currentUser.displayName,
                                    authorAvatarHex = currentUser.avatarColorHex,
                                    parentCommentId = parent?.id,
                                    replyToUsername = parent?.authorUsername,
                                    content = text
                                )
                            )
                        },
                        onToggleLikeComment = {},
                        onDeleteComment = {},
                        onShareVideo = {},
                        onReportVideo = {},
                        onReportComment = {},
                        onOpenUserProfile = {},
                        onSelectHashtag = {},
                        onSelectSound = {},
                        onNavigateToDiscover = {},
                        onOpenAdminDashboard = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Click the comment icon on video #101
        composeTestRule
            .onNodeWithTag("comment_button_101")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // 2. Verify the bottom sheet modal opens for video #101 and displays its existing comments
        composeTestRule
            .onNodeWithTag("comments_sheet_content_101")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("comments_count_header_101")
            .assertIsDisplayed()
            .assertTextContains("1 Comments")

        composeTestRule
            .onNodeWithText("Love the beat sync on this drop!")
            .assertIsDisplayed()

        // 3. Enter and submit a new comment for video #101
        composeTestRule
            .onNodeWithTag("comment_input_field")
            .assertIsDisplayed()
            .performTextInput("Incredible transitions on the rooftop!")

        composeTestRule
            .onNodeWithTag("send_comment_button")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // 4. Verify the new comment appears in the bottom sheet list and count updates to 2
        composeTestRule
            .onNodeWithTag("comments_count_header_101")
            .assertTextContains("2 Comments")

        composeTestRule
            .onNodeWithText("Incredible transitions on the rooftop!")
            .assertIsDisplayed()
    }

    @Test
    fun `follow and unfollow system state holder tracks followed creators and updates UI state`() {
        val me = UserEntity(
            id = 1L,
            username = "nova.pulse",
            displayName = "Nova Pulse",
            email = "nova@vibestream.social",
            passwordHash = "pulse123",
            followingCount = 1,
            followersCount = 1
        )
        val kai = UserEntity(
            id = 2L,
            username = "kai.motion",
            displayName = "Kai Motion",
            email = "kai@vibestream.social",
            passwordHash = "motion123",
            followersCount = 100
        )
        val aria = UserEntity(
            id = 3L,
            username = "aria.synth",
            displayName = "Aria SynthLab",
            email = "aria@vibestream.social",
            passwordHash = "synth123",
            followersCount = 80
        )

        val initialFollows = listOf(
            // User 1 follows Kai (2), and Aria (3) follows User 1
            FollowEntity(id = 1L, followerId = 1L, followingId = 2L, createdAt = 1000L),
            FollowEntity(id = 2L, followerId = 3L, followingId = 1L, createdAt = 2000L)
        )

        val stateHolder = FollowSystemStateHolder(
            initialCurrentUserId = 1L,
            initialUsers = listOf(me, kai, aria),
            initialFollows = initialFollows
        )

        // Initial state assertions
        val initialState = stateHolder.uiState.value
        assertEquals(setOf(2L), initialState.followingUserIds)
        assertEquals(setOf(3L), initialState.followerUserIds)
        assertEquals(1, initialState.followedCreators.size)
        assertEquals("kai.motion", initialState.followedCreators.first().creator.username)
        assertEquals(FollowRelationshipStatus.FOLLOWING, initialState.relationshipWith(2L))
        assertEquals(FollowRelationshipStatus.FOLLOWED_BY, initialState.relationshipWith(3L))
        assertFalse(stateHolder.followCreator(1L)) // Cannot self-follow

        // Follow Aria (who already follows user 1 -> becomes MUTUAL_FRIENDS)
        val nowFollowingAria = stateHolder.toggleFollow(3L, timestampMs = 3000L)
        assertTrue(nowFollowingAria)

        val afterFollowAria = stateHolder.uiState.value
        assertEquals(setOf(2L, 3L), afterFollowAria.followingUserIds)
        assertEquals(2, afterFollowAria.followingCount)
        assertEquals(FollowRelationshipStatus.MUTUAL_FRIENDS, afterFollowAria.relationshipWith(3L))
        assertTrue(afterFollowAria.isMutualFriend(3L))

        // Unfollow Kai -> removes from followedCreators and moves to suggestedCreators
        val nowFollowingKai = stateHolder.toggleFollow(2L)
        assertFalse(nowFollowingKai)

        val afterUnfollowKai = stateHolder.uiState.value
        assertEquals(setOf(3L), afterUnfollowKai.followingUserIds)
        assertEquals(1, afterUnfollowKai.followingCount)
        assertEquals(FollowRelationshipStatus.NOT_FOLLOWING, afterUnfollowKai.relationshipWith(2L))
        assertTrue(afterUnfollowKai.suggestedCreators.any { it.creatorId == 2L })
    }

    @Test
    fun `followed creators tracker UI displays followed creators and toggles follow state interactively`() {
        val me = UserEntity(
            id = 1L,
            username = "nova.pulse",
            displayName = "Nova Pulse",
            email = "nova@vibestream.social",
            passwordHash = "pulse123",
            followingCount = 1,
            followersCount = 1
        )
        val kai = UserEntity(
            id = 2L,
            username = "kai.motion",
            displayName = "Kai Motion",
            email = "kai@vibestream.social",
            passwordHash = "motion123",
            followersCount = 100,
            isVerified = true
        )
        val aria = UserEntity(
            id = 3L,
            username = "aria.synth",
            displayName = "Aria SynthLab",
            email = "aria@vibestream.social",
            passwordHash = "synth123",
            followersCount = 80,
            isVerified = true
        )

        val stateHolder = FollowSystemStateHolder(
            initialCurrentUserId = 1L,
            initialUsers = listOf(me, kai, aria),
            initialFollows = listOf(
                FollowEntity(id = 1L, followerId = 1L, followingId = 2L, createdAt = 1000L)
            )
        )

        composeTestRule.setContent {
            VibeStreamTheme(darkTheme = true) {
                val followState by stateHolder.uiState.collectAsState()
                Box(modifier = Modifier.size(width = 420.dp, height = 840.dp)) {
                    FollowedCreatorsSheetContent(
                        followUiState = followState,
                        initialTab = FollowDirectoryTab.FOLLOWING,
                        onDismiss = {},
                        onToggleFollow = { creatorId -> stateHolder.toggleFollow(creatorId) },
                        onOpenCreatorProfile = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Verify initial 1 Following count and Kai is displayed in Following directory
        composeTestRule
            .onNodeWithTag("follow_network_summary_text")
            .assertIsDisplayed()
            .assertTextContains("1 Following • 0 Followers")

        composeTestRule
            .onNodeWithTag("follow_directory_row_2")
            .assertIsDisplayed()

        // Switch to Discover (Suggested) tab and follow Aria (#3)
        composeTestRule
            .onNodeWithTag("follow_tab_suggested")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("directory_follow_btn_3")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Verify network summary updates to 2 Following
        composeTestRule
            .onNodeWithTag("follow_network_summary_text")
            .assertTextContains("2 Following • 0 Followers")

        // Switch back to Following tab and verify both Kai (#2) and Aria (#3) are tracked
        composeTestRule
            .onNodeWithTag("follow_tab_following")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("follow_directory_row_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("follow_directory_row_3").assertIsDisplayed()
    }

    @Test
    fun `search screen displays suggested creators and trending hashtags and queries users hashtags and videos`() {
        val users = listOf(
            UserEntity(
                id = 1L,
                username = "nova.pulse",
                displayName = "Nova Pulse",
                email = "nova@vibestream.social",
                passwordHash = "pulse123",
                followersCount = 48200,
                isVerified = true
            ),
            UserEntity(
                id = 2L,
                username = "kai.motion",
                displayName = "Kai Motion",
                email = "kai@vibestream.social",
                passwordHash = "motion123",
                followersCount = 128500,
                isVerified = true
            ),
            UserEntity(
                id = 3L,
                username = "aria.synth",
                displayName = "Aria SynthLab",
                email = "aria@vibestream.social",
                passwordHash = "synth123",
                followersCount = 89300,
                isVerified = true
            )
        )

        val hashtags = listOf(
            HashtagEntity("neonpulse", "#NeonPulse", "Visual Art", 4280, 18_400_000L, true),
            HashtagEntity("synthjam", "#SynthJam", "Music", 2840, 9_600_000L, true)
        )

        val videos = listOf(
            VideoEntity(
                id = 10L,
                creatorId = 2L,
                creatorUsername = "kai.motion",
                creatorDisplayName = "Kai Motion",
                creatorAvatarHex = "#7C4DFF",
                caption = "Late night rooftop footwork #NeonPulse",
                hashtags = "#NeonPulse #RooftopFlow",
                soundId = 1L,
                soundTitle = "Midnight Prism",
                soundArtist = "Aria SynthLab",
                videoUri = "https://cdn.vibestream.social/placeholders/stream_01_rooftop_neon.mp4",
                cdnStreamUrl = "https://cdn.vibestream.social/streams/10/master.m3u8",
                thumbnailHeadline = "ROOFTOP NEON",
                viewsCount = 94300
            ),
            VideoEntity(
                id = 11L,
                creatorId = 3L,
                creatorUsername = "aria.synth",
                creatorDisplayName = "Aria SynthLab",
                creatorAvatarHex = "#00E5FF",
                caption = "Modular synth arpeggiator jam #SynthJam",
                hashtags = "#SynthJam",
                soundId = 2L,
                soundTitle = "Synth Grid",
                soundArtist = "Aria SynthLab",
                videoUri = "https://cdn.vibestream.social/placeholders/stream_02_synth.mp4",
                cdnStreamUrl = "https://cdn.vibestream.social/streams/11/master.m3u8",
                thumbnailHeadline = "MODULAR SYNTH",
                viewsCount = 62000
            )
        )

        composeTestRule.setContent {
            VibeStreamTheme(darkTheme = true) {
                var query by remember { mutableStateOf("") }
                Box(modifier = Modifier.size(width = 420.dp, height = 880.dp)) {
                    DiscoverSearchScreen(
                        searchQuery = query,
                        onSearchQueryChange = { query = it },
                        users = users,
                        videos = videos,
                        hashtags = hashtags,
                        sounds = emptyList(),
                        followingUserIds = emptySet(),
                        currentUserId = 1L,
                        onToggleFollow = {},
                        onOpenUserProfile = {},
                        onSelectHashtag = {},
                        onSelectSound = {},
                        onPlayVideoInFeed = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify top search bar, suggested creators, and trending hashtags are displayed
        val searchBar = composeTestRule
            .onNodeWithTag("discover_search_input")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("suggested_creators_section")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("suggested_creator_card_2")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("trending_hashtags_section")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("trending_hashtag_item_neonpulse")
            .assertIsDisplayed()

        // 2. Query for "synth" via the top search bar to filter users, hashtags, and videos
        searchBar.performTextInput("synth")
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("search_query_summary")
            .assertIsDisplayed()
            .assertTextContains("Results for \"synth\": 1 users • 1 hashtags • 1 videos")

        composeTestRule
            .onNodeWithTag("suggested_creator_card_3")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("trending_hashtag_item_synthjam")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("search_video_card_11")
            .assertIsDisplayed()
    }

    @Test
    fun `notification management service stores like follow and comment events in Room and manages read status`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val dao = db.vibeDao()
            val service = NotificationManagementService(dao)

            val recipient = UserEntity(
                id = 1L,
                username = "nova.pulse",
                displayName = "Nova Pulse",
                email = "nova@vibestream.social",
                passwordHash = "pulse123"
            )
            val actor = UserEntity(
                id = 2L,
                username = "kai.motion",
                displayName = "Kai Motion",
                email = "kai@vibestream.social",
                passwordHash = "motion123",
                avatarColorHex = "#7C4DFF"
            )
            dao.insertUsers(listOf(recipient, actor))

            val recipientVideo = VideoEntity(
                id = 500L,
                creatorId = 1L,
                creatorUsername = "nova.pulse",
                creatorDisplayName = "Nova Pulse",
                creatorAvatarHex = "#FF3366",
                caption = "Cybernetic Horizon Synth Jam",
                hashtags = "#NeonPulse",
                soundId = 1L,
                soundTitle = "Midnight Prism",
                soundArtist = "Aria SynthLab",
                videoUri = "https://cdn.vibestream.social/placeholders/stream_01_rooftop_neon.mp4",
                cdnStreamUrl = "https://cdn.vibestream.social/streams/500/master.m3u8",
                thumbnailHeadline = "CYBERNETIC HORIZON"
            )
            dao.insertVideo(recipientVideo)

            // 1. Store LIKE, FOLLOW, and COMMENT notification events in Room
            val likeNotif = service.recordLikeNotification(
                actor = actor,
                video = recipientVideo,
                timestampMs = 1000L
            )
            val followNotif = service.recordFollowNotification(
                follower = actor,
                targetUserId = recipient.id,
                timestampMs = 2000L
            )
            val commentNotif = service.recordCommentNotification(
                author = actor,
                video = recipientVideo,
                commentText = "Incredible lighting on this take!",
                timestampMs = 3000L
            )

            assertNotNull(likeNotif)
            assertNotNull(followNotif)
            assertNotNull(commentNotif)

            // 2. Verify all 3 notifications are persisted in Room ordered newest first
            val stored = service.observeNotifications(recipient.id).first()
            assertEquals(3, stored.size)
            assertEquals("COMMENT", stored[0].type)
            assertEquals("FOLLOW", stored[1].type)
            assertEquals("LIKE", stored[2].type)
            assertEquals(3, service.observeUnreadCount(recipient.id).first())

            // 3. Verify filtering by NotificationEventType in Room
            val likeEvents = service.observeNotificationsByType(recipient.id, NotificationEventType.LIKE).first()
            assertEquals(1, likeEvents.size)
            assertTrue(likeEvents.first().messageText.contains("CYBERNETIC HORIZON"))

            // 4. Mark single notification read and then mark all read
            service.markNotificationRead(stored[0].id)
            assertEquals(2, service.observeUnreadCount(recipient.id).first())

            service.markAllNotificationsRead(recipient.id)
            assertEquals(0, service.observeUnreadCount(recipient.id).first())
        } finally {
            db.close()
        }
    }

    @Test
    fun `notification list view composable displays like follow and comment notifications and filters by type`() {
        val sampleNotifications = listOf(
            NotificationEntity(
                id = 10L,
                recipientId = 1L,
                actorId = 2L,
                actorUsername = "kai.motion",
                actorDisplayName = "Kai Motion",
                actorAvatarHex = "#7C4DFF",
                type = "LIKE",
                referenceId = 500L,
                messageText = "liked your video \"CYBERNETIC HORIZON\".",
                isRead = false,
                createdAt = 3000L
            ),
            NotificationEntity(
                id = 11L,
                recipientId = 1L,
                actorId = 3L,
                actorUsername = "aria.synth",
                actorDisplayName = "Aria SynthLab",
                actorAvatarHex = "#00E5FF",
                type = "FOLLOW",
                referenceId = 3L,
                messageText = "started following you.",
                isRead = false,
                createdAt = 2000L
            ),
            NotificationEntity(
                id = 12L,
                recipientId = 1L,
                actorId = 4L,
                actorUsername = "milo.frame",
                actorDisplayName = "Milo Frame",
                actorAvatarHex = "#FFB300",
                type = "COMMENT",
                referenceId = 500L,
                messageText = "commented on your video: \"Awesome transitions!\"",
                isRead = false,
                createdAt = 1000L
            )
        )

        composeTestRule.setContent {
            VibeStreamTheme(darkTheme = true) {
                val notifList = remember { mutableStateListOf(*sampleNotifications.toTypedArray()) }
                Box(modifier = Modifier.size(width = 420.dp, height = 880.dp)) {
                    NotificationListView(
                        notifications = notifList,
                        onMarkNotificationRead = { id ->
                            val idx = notifList.indexOfFirst { it.id == id }
                            if (idx >= 0) {
                                notifList[idx] = notifList[idx].copy(isRead = true)
                            }
                        },
                        onMarkAllRead = {
                            for (i in notifList.indices) {
                                notifList[i] = notifList[i].copy(isRead = true)
                            }
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify the notification list view and summary metrics are displayed
        composeTestRule.onNodeWithTag("notification_list_view").assertIsDisplayed()
        composeTestRule.onNodeWithTag("notif_metric_likes").assertTextContains("1 Likes")
        composeTestRule.onNodeWithTag("notif_metric_follows").assertTextContains("1 Follows")
        composeTestRule.onNodeWithTag("notif_metric_comments").assertTextContains("1 Comments")

        // 2. Verify all 3 notification cards (Like, Follow, Comment) are rendered in the list view
        composeTestRule.onNodeWithTag("notification_item_10").assertIsDisplayed()
        composeTestRule.onNodeWithTag("notification_item_11").assertIsDisplayed()
        composeTestRule.onNodeWithTag("notification_item_12").assertIsDisplayed()

        // 3. Click the "Likes" filter chip and verify only the LIKE notification is shown
        composeTestRule.onNodeWithTag("notification_filter_LIKE").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("notification_item_10").assertIsDisplayed()
        composeTestRule.onNodeWithText("liked your video \"CYBERNETIC HORIZON\".").assertIsDisplayed()
    }

    @Test
    fun `video player displays small overlay label in corner with video duration in M_SS format`() {
        assertEquals("0:18", formatVideoDurationMss(18))
        assertEquals("1:05", formatVideoDurationMss(65))
        assertEquals("2:30", formatVideoDurationMss(150))

        val testVideo = VideoEntity(
            id = 88L,
            creatorId = 2L,
            creatorUsername = "kai.motion",
            creatorDisplayName = "Kai Motion",
            creatorAvatarHex = "#7C4DFF",
            creatorVerified = true,
            caption = "Testing corner duration overlay label in M:SS format",
            hashtags = "#NeonPulse",
            soundId = 1L,
            soundTitle = "Midnight Prism",
            soundArtist = "Aria SynthLab",
            videoUri = "https://cdn.vibestream.social/placeholders/stream_01_rooftop_neon.mp4",
            cdnStreamUrl = "https://cdn.vibestream.social/streams/88/master.m3u8",
            thumbnailThemeIndex = 0,
            thumbnailHeadline = "DURATION TEST",
            durationSec = 65
        )

        composeTestRule.setContent {
            VibeStreamTheme(darkTheme = true) {
                Box(modifier = Modifier.size(width = 400.dp, height = 800.dp)) {
                    VerticalFeedOverlay(
                        video = testVideo,
                        playbackStatus = VideoPlaybackStatus.PLAYING,
                        formattedTime = "0:12 / 1:05",
                        playbackProgress = 0.2f,
                        isPlaying = true,
                        isAudioMuted = false,
                        isLiked = false,
                        isFollowingCreator = false,
                        isOwnVideo = false,
                        nextPreloadedUrl = null,
                        onToggleLike = {},
                        onToggleFollow = {},
                        onOpenComments = {},
                        onShare = {},
                        onToggleMute = {},
                        onReport = {},
                        onOpenCreator = {},
                        onClickHashtag = {},
                        onClickSound = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        val playerBounds = composeTestRule
            .onNodeWithTag("vertical_feed_overlay_88")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot

        val durationNode = composeTestRule
            .onNodeWithTag("video_duration_overlay_88")
            .assertIsDisplayed()
            .assertTextContains("1:05")

        val durationBounds = durationNode.fetchSemanticsNode().boundsInRoot

        // Verify the duration overlay is small and positioned in the top-right corner of the video player
        assertTrue("Duration label should be in the right half of the player", durationBounds.center.x > playerBounds.width * 0.7f)
        assertTrue("Duration label should be in the top corner of the player", durationBounds.center.y < playerBounds.height * 0.15f)
    }

    @Test
    fun `clicking share button on video overlay triggers Android share intent with video link`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = Shadows.shadowOf(app)
        // Clear any previously queued intents
        while (shadowApp.nextStartedActivity != null) {
            // drain
        }

        var shareCallbackInvoked = 0
        val testVideo = VideoEntity(
            id = 99L,
            creatorId = 3L,
            creatorUsername = "aria.synth",
            creatorDisplayName = "Aria SynthLab",
            creatorAvatarHex = "#00E5FF",
            creatorVerified = true,
            caption = "Modular arpeggiator live jam #SynthJam",
            hashtags = "#SynthJam",
            soundId = 1L,
            soundTitle = "Midnight Prism",
            soundArtist = "Aria SynthLab",
            videoUri = "https://cdn.vibestream.social/placeholders/stream_02_synth_lab.mp4",
            cdnStreamUrl = "https://cdn.vibestream.social/streams/99/master.m3u8",
            thumbnailThemeIndex = 1,
            thumbnailHeadline = "SYNTH JAM",
            durationSec = 22,
            sharesCount = 14
        )

        composeTestRule.setContent {
            VibeStreamTheme(darkTheme = true) {
                Box(modifier = Modifier.size(width = 400.dp, height = 800.dp)) {
                    VerticalFeedOverlay(
                        video = testVideo,
                        playbackStatus = VideoPlaybackStatus.PLAYING,
                        formattedTime = "0:04 / 0:22",
                        playbackProgress = 0.2f,
                        isPlaying = true,
                        isAudioMuted = false,
                        isLiked = false,
                        isFollowingCreator = false,
                        isOwnVideo = false,
                        nextPreloadedUrl = null,
                        onToggleLike = {},
                        onToggleFollow = {},
                        onOpenComments = {},
                        onShare = { shareCallbackInvoked++ },
                        onToggleMute = {},
                        onReport = {},
                        onOpenCreator = {},
                        onClickHashtag = {},
                        onClickSound = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("share_button_99")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        assertEquals(1, shareCallbackInvoked)

        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull("Clicking share button should trigger an Android activity intent", startedIntent)
        assertEquals(Intent.ACTION_CHOOSER, startedIntent.action)

        @Suppress("DEPRECATION")
        val targetSendIntent = startedIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull("Chooser intent should wrap an ACTION_SEND intent", targetSendIntent)
        assertEquals(Intent.ACTION_SEND, targetSendIntent!!.action)
        assertEquals("text/plain", targetSendIntent.type)

        val sharedText = targetSendIntent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        assertTrue(
            "Shared text should contain the video CDN link",
            sharedText.contains("https://cdn.vibestream.social/streams/99/master.m3u8")
        )
        assertTrue(
            "Shared text should contain the creator username",
            sharedText.contains("@aria.synth")
        )
    }

    @Test
    fun `volume toggle button on video player toggles mute and unmute states`() {
        var muteToggleCount = 0
        val testVideo = VideoEntity(
            id = 120L,
            creatorId = 2L,
            creatorUsername = "kai.motion",
            creatorDisplayName = "Kai Motion",
            creatorAvatarHex = "#7C4DFF",
            creatorVerified = true,
            caption = "Testing video player volume toggle mute and unmute",
            hashtags = "#RooftopFlow",
            soundId = 1L,
            soundTitle = "Midnight Prism",
            soundArtist = "Aria SynthLab",
            videoUri = "https://cdn.vibestream.social/placeholders/stream_01_rooftop_neon.mp4",
            cdnStreamUrl = "https://cdn.vibestream.social/streams/120/master.m3u8",
            thumbnailThemeIndex = 0,
            thumbnailHeadline = "VOLUME TEST",
            durationSec = 15
        )

        composeTestRule.setContent {
            VibeStreamTheme(darkTheme = true) {
                var isMuted by remember { mutableStateOf(false) }
                Box(modifier = Modifier.size(width = 400.dp, height = 800.dp)) {
                    VerticalFeedOverlay(
                        video = testVideo,
                        playbackStatus = VideoPlaybackStatus.PLAYING,
                        formattedTime = "0:03 / 0:15",
                        playbackProgress = 0.2f,
                        isPlaying = true,
                        isAudioMuted = isMuted,
                        isLiked = false,
                        isFollowingCreator = false,
                        isOwnVideo = false,
                        nextPreloadedUrl = null,
                        onToggleLike = {},
                        onToggleFollow = {},
                        onOpenComments = {},
                        onShare = {},
                        onToggleMute = {
                            muteToggleCount++
                            isMuted = !isMuted
                        },
                        onReport = {},
                        onOpenCreator = {},
                        onClickHashtag = {},
                        onClickSound = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Initially unmuted ("Sound On")
        val volumeButton = composeTestRule
            .onNodeWithTag("volume_toggle_button_120")
            .assertIsDisplayed()
            .assertTextContains("Sound On")

        // Tap once to mute
        volumeButton.performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, muteToggleCount)
        composeTestRule
            .onNodeWithTag("volume_toggle_button_120")
            .assertTextContains("Muted")
        composeTestRule
            .onNodeWithTag("mute_button_120")
            .assertTextContains("Muted")

        // Tap again to unmute
        volumeButton.performClick()
        composeTestRule.waitForIdle()

        assertEquals(2, muteToggleCount)
        composeTestRule
            .onNodeWithTag("volume_toggle_button_120")
            .assertTextContains("Sound On")
        composeTestRule
            .onNodeWithTag("mute_button_120")
            .assertTextContains("Audio")
    }

    @Test
    fun `comment icon button on video overlay displays existing comment count and opens bottom sheet with list of user comments`() {
        val testVideo = VideoEntity(
            id = 150L,
            creatorId = 2L,
            creatorUsername = "kai.motion",
            creatorDisplayName = "Kai Motion",
            creatorAvatarHex = "#7C4DFF",
            creatorVerified = true,
            caption = "Neon street dance session #RooftopFlow",
            hashtags = "#RooftopFlow",
            soundId = 1L,
            soundTitle = "Midnight Prism",
            soundArtist = "Aria SynthLab",
            videoUri = "https://cdn.vibestream.social/placeholders/stream_01_rooftop_neon.mp4",
            cdnStreamUrl = "https://cdn.vibestream.social/streams/150/master.m3u8",
            thumbnailThemeIndex = 0,
            thumbnailHeadline = "COMMENTS TEST",
            durationSec = 18,
            commentsCount = 2
        )

        val existingComments = listOf(
            CommentEntity(
                id = 901L,
                videoId = 150L,
                authorId = 3L,
                authorUsername = "aria.synth",
                authorDisplayName = "Aria SynthLab",
                authorAvatarHex = "#00E5FF",
                content = "First comment: incredible synth timing!",
                likesCount = 12,
                createdAt = 2000L
            ),
            CommentEntity(
                id = 902L,
                videoId = 150L,
                authorId = 4L,
                authorUsername = "milo.frame",
                authorDisplayName = "Milo Frame",
                authorAvatarHex = "#FFB300",
                content = "Second comment: the color grading is top tier.",
                likesCount = 5,
                createdAt = 1000L
            )
        )

        composeTestRule.setContent {
            VibeStreamTheme(darkTheme = true) {
                Box(modifier = Modifier.size(width = 420.dp, height = 860.dp)) {
                    VerticalFeedOverlay(
                        video = testVideo,
                        playbackStatus = VideoPlaybackStatus.PLAYING,
                        formattedTime = "0:05 / 0:18",
                        playbackProgress = 0.25f,
                        isPlaying = true,
                        isAudioMuted = false,
                        isLiked = false,
                        isFollowingCreator = false,
                        isOwnVideo = false,
                        nextPreloadedUrl = null,
                        onToggleLike = {},
                        onToggleFollow = {},
                        onOpenComments = {},
                        onShare = {},
                        onToggleMute = {},
                        onReport = {},
                        onOpenCreator = {},
                        onClickHashtag = {},
                        onClickSound = {},
                        comments = existingComments
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify the comment icon button on the video overlay displays the count of existing comments ("2")
        val commentButton = composeTestRule
            .onNodeWithTag("comment_button_150")
            .assertIsDisplayed()
            .assertTextContains("2")

        // 2. Tap the comment icon button to open the bottom sheet
        commentButton.performClick()
        composeTestRule.waitForIdle()

        // 3. Verify the bottom sheet opens and displays the list of user comments
        composeTestRule
            .onNodeWithTag("comments_sheet_content_150")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("comments_count_header_150")
            .assertTextContains("2 Comments")
        composeTestRule
            .onNodeWithTag("comments_list_150")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("comment_item_901")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("First comment: incredible synth timing!")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("comment_item_902")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Second comment: the color grading is top tier.")
            .assertIsDisplayed()
    }

    @Test
    fun `slim progress bar at bottom of video player visually indicates playback progress`() {
        val testVideo = VideoEntity(
            id = 175L,
            creatorId = 2L,
            creatorUsername = "kai.motion",
            creatorDisplayName = "Kai Motion",
            creatorAvatarHex = "#7C4DFF",
            creatorVerified = true,
            caption = "Testing slim bottom playback progress bar",
            hashtags = "#NeonPulse",
            soundId = 1L,
            soundTitle = "Midnight Prism",
            soundArtist = "Aria SynthLab",
            videoUri = "https://cdn.vibestream.social/placeholders/stream_01_rooftop_neon.mp4",
            cdnStreamUrl = "https://cdn.vibestream.social/streams/175/master.m3u8",
            thumbnailThemeIndex = 0,
            thumbnailHeadline = "PROGRESS TEST",
            durationSec = 20
        )

        var progressState by mutableStateOf(0.35f)

        composeTestRule.setContent {
            VibeStreamTheme(darkTheme = true) {
                Box(modifier = Modifier.size(width = 400.dp, height = 800.dp)) {
                    VerticalFeedOverlay(
                        video = testVideo,
                        playbackStatus = VideoPlaybackStatus.PLAYING,
                        formattedTime = "0:07 / 0:20",
                        playbackProgress = progressState,
                        isPlaying = true,
                        isAudioMuted = false,
                        isLiked = false,
                        isFollowingCreator = false,
                        isOwnVideo = false,
                        nextPreloadedUrl = null,
                        onToggleLike = {},
                        onToggleFollow = {},
                        onOpenComments = {},
                        onShare = {},
                        onToggleMute = {},
                        onReport = {},
                        onOpenCreator = {},
                        onClickHashtag = {},
                        onClickSound = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        val playerBounds = composeTestRule
            .onNodeWithTag("vertical_feed_overlay_175")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot

        val progressBarNode = composeTestRule
            .onNodeWithTag("video_progress_bar_175")
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.35f, 0f..1f))

        val barSemantics = progressBarNode.fetchSemanticsNode()
        val barBounds = barSemantics.boundsInRoot

        // Verify the progress bar is slim and positioned at the very bottom of the video player
        assertTrue("Progress bar should be at the bottom of the video player", barBounds.bottom >= playerBounds.bottom - 4f)
        assertTrue("Progress bar should be slim", barBounds.height <= 24f)
        assertEquals("35%", barSemantics.config[SemanticsProperties.StateDescription])

        // Advance playback progress to 80% and verify the progress bar updates visually
        progressState = 0.80f
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("video_progress_bar_175")
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.80f, 0f..1f))
        assertEquals(
            "80%",
            composeTestRule.onNodeWithTag("video_progress_bar_175").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        )
    }

    @Test
    fun `video player overlay integrates share intent volume toggle comment bottom sheet and slim progress bar together`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowApp = Shadows.shadowOf(context as Application)
        while (shadowApp.nextStartedActivity != null) {
            // Clear any prior started intents
        }

        val testVideo = VideoEntity(
            id = 200L,
            creatorId = 2L,
            creatorUsername = "kai.motion",
            creatorDisplayName = "Kai Motion",
            creatorAvatarHex = "#7C4DFF",
            creatorVerified = true,
            caption = "All four video overlay features integrated test",
            hashtags = "#VibeStream #AllFeatures",
            soundId = 1L,
            soundTitle = "Midnight Prism",
            soundArtist = "Aria SynthLab",
            videoUri = "https://cdn.vibestream.social/placeholders/stream_01_rooftop_neon.mp4",
            cdnStreamUrl = "https://cdn.vibestream.social/streams/200/master.m3u8",
            thumbnailThemeIndex = 0,
            thumbnailHeadline = "ALL FEATURES",
            durationSec = 25,
            likesCount = 120,
            commentsCount = 2,
            sharesCount = 18
        )

        val existingComments = listOf(
            CommentEntity(
                id = 951L,
                videoId = 200L,
                authorId = 3L,
                authorUsername = "aria.synth",
                authorDisplayName = "Aria SynthLab",
                authorAvatarHex = "#00E5FF",
                content = "Loving the new slim progress bar and volume controls!",
                likesCount = 9,
                createdAt = 2000L
            ),
            CommentEntity(
                id = 952L,
                videoId = 200L,
                authorId = 4L,
                authorUsername = "milo.frame",
                authorDisplayName = "Milo Frame",
                authorAvatarHex = "#FFB300",
                content = "Sharing this clip right away!",
                likesCount = 4,
                createdAt = 1000L
            )
        )

        var progressState by mutableStateOf(0.45f)
        var shareCount by mutableStateOf(0)
        var muteToggleCount by mutableStateOf(0)

        composeTestRule.setContent {
            VibeStreamTheme(darkTheme = true) {
                var isMuted by remember { mutableStateOf(false) }
                Box(modifier = Modifier.size(width = 420.dp, height = 860.dp)) {
                    VerticalFeedOverlay(
                        video = testVideo,
                        playbackStatus = VideoPlaybackStatus.PLAYING,
                        formattedTime = "0:11 / 0:25",
                        playbackProgress = progressState,
                        isPlaying = true,
                        isAudioMuted = isMuted,
                        isLiked = false,
                        isFollowingCreator = false,
                        isOwnVideo = false,
                        nextPreloadedUrl = null,
                        onToggleLike = {},
                        onToggleFollow = {},
                        onOpenComments = {},
                        onShare = { shareCount++ },
                        onToggleMute = {
                            muteToggleCount++
                            isMuted = !isMuted
                        },
                        onReport = {},
                        onOpenCreator = {},
                        onClickHashtag = {},
                        onClickSound = {},
                        comments = existingComments
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify slim progress bar at bottom of video player visually indicates playback progress
        composeTestRule
            .onNodeWithTag("video_progress_bar_200")
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.45f, 0f..1f))

        progressState = 0.75f
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("video_progress_bar_200")
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.75f, 0f..1f))

        // 2. Verify volume toggle button mutes and unmutes video playback
        val volumeButton = composeTestRule
            .onNodeWithTag("volume_toggle_button_200")
            .assertIsDisplayed()
            .assertTextContains("Sound On")

        volumeButton.performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, muteToggleCount)
        composeTestRule.onNodeWithTag("volume_toggle_button_200").assertTextContains("Muted")

        volumeButton.performClick()
        composeTestRule.waitForIdle()
        assertEquals(2, muteToggleCount)
        composeTestRule.onNodeWithTag("volume_toggle_button_200").assertTextContains("Sound On")

        // 3. Verify share button triggers Android share intent with video link
        composeTestRule
            .onNodeWithTag("share_button_200")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, shareCount)
        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull("Share button should launch Android chooser intent", startedIntent)
        assertEquals(Intent.ACTION_CHOOSER, startedIntent.action)
        @Suppress("DEPRECATION")
        val sendIntent = startedIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(sendIntent)
        assertEquals(Intent.ACTION_SEND, sendIntent!!.action)
        assertTrue(
            sendIntent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                .contains("https://cdn.vibestream.social/streams/200/master.m3u8")
        )

        // 4. Verify comment icon button displays existing comment count and opens bottom sheet with user comments
        val commentButton = composeTestRule
            .onNodeWithTag("comment_button_200")
            .assertIsDisplayed()
            .assertTextContains("2")

        commentButton.performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("comments_sheet_content_200").assertIsDisplayed()
        composeTestRule.onNodeWithTag("comments_list_200").assertIsDisplayed()
        composeTestRule.onNodeWithText("Loving the new slim progress bar and volume controls!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sharing this clip right away!").assertIsDisplayed()
    }

    @Test
    fun realFeatureEngines_audioVideoCryptoNotificationAndRanking_executeWithRealOutputs() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Real PCM Audio Synthesis Engine
        val pcmSamples = SynthAudioEngine.synthesizePcmBuffer(
            bpm = 128,
            themeIndex = 1,
            durationMs = 500
        )
        assertTrue("Synthesized 16-bit PCM audio buffer must be non-empty", pcmSamples.size > 5000)
        assertTrue("PCM samples must contain non-zero audio waveform signal", pcmSamples.any { it.toInt() != 0 })

        // 2. Real Video Processing & Packaging Engine (generates real JPEG thumbnail & container artifact)
        val stagesObserved = mutableListOf<String>()
        val artifact = VideoProcessingEngine.processAndPackageVideo(
            context = context,
            sourceVideoUri = "",
            customThumbnailUri = null,
            themeIndex = 2,
            headline = "ENGINE TEST",
            durationSec = 15,
            targetBitrateKbps = 2400,
            resolution = "1080x1920",
            soundBpm = 128
        ) { _, _, stageLabel, _ ->
            stagesObserved.add(stageLabel)
        }
        assertEquals(4, stagesObserved.size)
        assertNotNull("Generated cover thumbnail URI must exist", artifact.thumbnailUri)
        assertTrue("Output video URI must be generated", artifact.outputVideoUri.startsWith("file://"))
        assertTrue("Processed byte size must be positive", artifact.processedBytes > 0L)
        assertTrue("Compression summary must report MB metrics", artifact.compressionSummary.contains("MB"))

        // 3. Real Cryptographic SHA-256 Password Hashing Engine
        val hashed = PasswordSecurityEngine.hashPassword("vibe123")
        assertTrue("Password hash must use sha256 prefix", hashed.startsWith("sha256:"))
        assertTrue(PasswordSecurityEngine.verifyPassword("vibe123", hashed))
        assertFalse(PasswordSecurityEngine.verifyPassword("wrong_pass", hashed))

        // 4. Real Multi-Signal Feed Recommendation & Ranking Engine
        val lowEngagementVideo = VideoEntity(
            id = 301L,
            creatorId = 10L,
            creatorUsername = "creator_low",
            creatorDisplayName = "Creator Low",
            creatorAvatarHex = "#00E5FF",
            caption = "Low engagement clip",
            hashtags = "#test",
            soundId = 1L,
            soundTitle = "Sound 1",
            soundArtist = "Artist 1",
            videoUri = "https://cdn.vibestream.social/streams/301/clip.mp4",
            cdnStreamUrl = "https://cdn.vibestream.social/streams/301/master.m3u8",
            likesCount = 5,
            commentsCount = 0,
            sharesCount = 0,
            viewsCount = 5000,
            createdAt = System.currentTimeMillis() - 86_400_000L * 5
        )
        val highEngagementVideo = VideoEntity(
            id = 302L,
            creatorId = 11L,
            creatorUsername = "creator_high",
            creatorDisplayName = "Creator High",
            creatorAvatarHex = "#FF3366",
            caption = "High velocity trending clip",
            hashtags = "#neonpulse #synth",
            soundId = 2L,
            soundTitle = "Sound 2",
            soundArtist = "Artist 2",
            videoUri = "https://cdn.vibestream.social/streams/302/clip.mp4",
            cdnStreamUrl = "https://cdn.vibestream.social/streams/302/master.m3u8",
            likesCount = 1800,
            commentsCount = 240,
            sharesCount = 95,
            viewsCount = 2500,
            createdAt = System.currentTimeMillis()
        )
        val ranked = FeedRecommendationEngine.rankVideosForUser(
            videos = listOf(lowEngagementVideo, highEngagementVideo),
            likedVideoIds = emptySet(),
            followingUserIds = setOf(11L)
        )
        assertEquals("High velocity followed creator video must rank first", 302L, ranked.first().id)

        // 5. Real Android System Notification Channel Registration
        SystemNotificationEngine.ensureNotificationChannel(context)
        assertTrue(SystemNotificationEngine.CHANNEL_ID.isNotBlank())
    }

    @Test
    fun loginDetails_authScreenAndProfileScreen_displayCredentialsAndSupportAutofill() {
        val sampleUser = UserEntity(
            id = 1L,
            username = "nova.pulse",
            displayName = "Nova Pulse",
            email = "nova@vibestream.social",
            passwordHash = PasswordSecurityEngine.hashPassword("pulse123"),
            bio = "Platform Creator & Admin",
            avatarColorHex = "#FF3366",
            isVerified = true,
            isAdmin = true
        )
        var submittedLoginPair: Pair<String, String>? = null

        composeTestRule.setContent {
            VibeStreamTheme(darkTheme = true) {
                AuthScreen(
                    allUsers = listOf(sampleUser),
                    currentUser = sampleUser,
                    isFirebaseConfigured = false,
                    onBack = {},
                    onLogin = { u, p -> submittedLoginPair = u to p },
                    onSignUp = { _, _, _, _, _ -> },
                    onGoogleSignIn = {},
                    onResetPassword = { _, _ -> },
                    onLogout = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("login_details_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quick_fill_kai.motion").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("login_username_input").assertTextContains("kai.motion")
        composeTestRule.onNodeWithTag("login_password_input").assertTextContains("motion123")

        composeTestRule.onNodeWithTag("login_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("kai.motion" to "motion123", submittedLoginPair)
    }

    @Test
    fun epicEngineArchitecture_searchSafetyAnalyticsAndClusterBenchmark_verifyAllSevenEngines() {
        val sampleUser = UserEntity(
            id = 1L,
            username = "nova.pulse",
            displayName = "Nova Pulse",
            email = "nova@vibestream.social",
            passwordHash = PasswordSecurityEngine.hashPassword("pulse123"),
            bio = "Platform Creator & Admin",
            avatarColorHex = "#FF3366",
            followersCount = 28500,
            followingCount = 142,
            totalLikesReceived = 412000,
            isVerified = true,
            isAdmin = true
        )
        val secondUser = UserEntity(
            id = 2L,
            username = "kai.motion",
            displayName = "Kai Motion",
            email = "kai@vibestream.social",
            passwordHash = PasswordSecurityEngine.hashPassword("motion123"),
            bio = "Kinetic dance & cyberpunk visuals",
            avatarColorHex = "#00E5FF",
            followersCount = 19200,
            followingCount = 98,
            totalLikesReceived = 210000,
            isVerified = true,
            isAdmin = false
        )
        val sampleVideo = VideoEntity(
            id = 501L,
            creatorId = 1L,
            creatorUsername = "nova.pulse",
            creatorDisplayName = "Nova Pulse",
            creatorAvatarHex = "#FF3366",
            caption = "Neon cyberpunk rooftop session at 128 BPM",
            hashtags = "#neonpulse #cyberpunk #synthwave",
            soundId = 1L,
            soundTitle = "Midnight Cyber Bass",
            soundArtist = "Nova Pulse",
            videoUri = "https://cdn.vibestream.social/streams/501/clip.mp4",
            cdnStreamUrl = "https://cdn.vibestream.social/streams/501/master.m3u8",
            durationSec = 24,
            likesCount = 4200,
            commentsCount = 310,
            sharesCount = 185,
            viewsCount = 19500
        )
        val sampleHashtag = HashtagEntity(
            tag = "neonpulse",
            displayTag = "#NeonPulse",
            category = "Visual Art",
            videosCount = 1420,
            viewsCount = 2400000L,
            isTrending = true
        )
        val sampleSound = SoundEntity(
            id = 1L,
            title = "Midnight Cyber Bass",
            artist = "Nova Pulse",
            durationSec = 30,
            bpm = 128,
            usageCount = 3420,
            isTrending = true
        )

        // 1. BM25 + Fuzzy Levenshtein Search & Discovery Engine (including typo tolerance)
        val typoUsers = SearchAndDiscoveryEngine.searchAndRankUsers(
            users = listOf(sampleUser, secondUser),
            rawQuery = "novapuls", // fuzzy match for nova.pulse
            currentUserId = null
        )
        assertTrue("BM25 + Levenshtein search should match nova.pulse on typo query", typoUsers.any { it.username == "nova.pulse" })

        val matchedVideos = SearchAndDiscoveryEngine.searchAndRankVideos(
            videos = listOf(sampleVideo),
            rawQuery = "cyberpunk 128"
        )
        assertEquals(1, matchedVideos.size)

        // 2. Automated Trust, Safety & NLP Risk Scoring Engine
        val cleanResult = TrustAndSafetyEngine.evaluateTextSafety("Love the synth bassline and camera transitions!")
        assertFalse(cleanResult.isBlocked)
        assertEquals("SAFE", cleanResult.severityLabel)

        val spamResult = TrustAndSafetyEngine.evaluateTextSafety("Click this malware link for free crypto scam now http://bad.link")
        assertTrue("Critical spam/malware text must be blocked by TrustAndSafetyEngine", spamResult.isBlocked)
        assertTrue(spamResult.riskScore >= 80)

        // 3. Creator Analytics & Telemetry Engine
        val telemetry = CreatorAnalyticsEngine.computeCreatorTelemetry(
            user = sampleUser,
            userVideos = listOf(sampleVideo)
        )
        assertTrue(telemetry.engagementRatePercent > 10.0)
        assertTrue(telemetry.viralityKFactor > 0.5)
        assertTrue(telemetry.creatorTrustScore >= 85)

        // 4. Unified 7-Engine Cluster Benchmark (EpicEngineModel)
        val clusterBenchmark = EpicEngineModel.runLiveDiagnosticBenchmark(
            videos = listOf(sampleVideo),
            users = listOf(sampleUser, secondUser),
            hashtags = listOf(sampleHashtag),
            sounds = listOf(sampleSound)
        )
        assertEquals(7, clusterBenchmark.engineNodes.size)
        assertTrue(clusterBenchmark.synthesizedPcmSamples > 0)
        assertEquals(1, clusterBenchmark.rankedFeedVideosCount)
    }

    @Test
    fun userProfileComponent_displaysAvatarHandleBioAndVideoGrid_andPersistsProfileViaFirebaseFirestore() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val firestoreEngine = FirestoreProfilePersistenceEngine(context)

        val initialUser = UserEntity(
            id = 77L,
            username = "nova.pulse",
            displayName = "Nova Pulse",
            email = "nova@vibestream.social",
            passwordHash = PasswordSecurityEngine.hashPassword("pulse123"),
            bio = "Visual synth artist & vertical cinema director",
            avatarColorHex = "#FF3366",
            followersCount = 48200,
            followingCount = 140,
            totalLikesReceived = 319400,
            isVerified = true,
            isAdmin = true
        )

        val uploadedVideos = listOf(
            VideoEntity(
                id = 701L,
                creatorId = 77L,
                creatorUsername = "nova.pulse",
                creatorDisplayName = "Nova Pulse",
                creatorAvatarHex = "#FF3366",
                caption = "Cybernetic Horizon Reel #1",
                hashtags = "#NeonPulse #Synthwave",
                soundId = 1L,
                soundTitle = "Midnight Prism",
                soundArtist = "Nova Pulse",
                videoUri = "https://cdn.vibestream.social/streams/701/clip.mp4",
                cdnStreamUrl = "https://cdn.vibestream.social/streams/701/master.m3u8",
                thumbnailHeadline = "CYBER HORIZON",
                durationSec = 18,
                viewsCount = 12400,
                likesCount = 1890
            ),
            VideoEntity(
                id = 702L,
                creatorId = 77L,
                creatorUsername = "nova.pulse",
                creatorDisplayName = "Nova Pulse",
                creatorAvatarHex = "#FF3366",
                caption = "Analog Modular Arpeggio Session",
                hashtags = "#ModularSynth #Studio",
                soundId = 1L,
                soundTitle = "Midnight Prism",
                soundArtist = "Nova Pulse",
                videoUri = "https://cdn.vibestream.social/streams/702/clip.mp4",
                cdnStreamUrl = "https://cdn.vibestream.social/streams/702/master.m3u8",
                thumbnailHeadline = "MODULAR JAM",
                durationSec = 24,
                viewsCount = 8900,
                likesCount = 1120
            )
        )

        // Pre-seed initial profile document into Firestore engine
        val seededDoc = firestoreEngine.persistUserProfile(initialUser, uploadedVideos)
        assertEquals("user_profiles/77", seededDoc.documentPath)
        assertEquals("@nova.pulse", seededDoc.handle)
        assertEquals(2, seededDoc.uploadedVideos.size)

        // Verify lossless Firestore Map serialization / deserialization
        val firestoreMap = seededDoc.toFirestoreMap()
        val roundTripDoc = FirestoreUserProfileDocument.fromFirestoreMap(firestoreMap)
        assertEquals("@nova.pulse", roundTripDoc.handle)
        assertEquals("Visual synth artist & vertical cinema director", roundTripDoc.bio)
        assertEquals(2, roundTripDoc.uploadedVideos.size)

        var clickedVideoId: Long? = null

        composeTestRule.setContent {
            VibeStreamTheme(darkTheme = true) {
                Box(modifier = Modifier.size(width = 420.dp, height = 900.dp)) {
                    UserProfileComponent(
                        user = initialUser,
                        uploadedVideos = uploadedVideos,
                        isOwnProfile = true,
                        firestoreEngine = firestoreEngine,
                        onVideoClick = { clickedVideoId = it.id }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify User Avatar, Handle, Bio, and Firebase Firestore Badge are displayed
        composeTestRule.onNodeWithTag("user_profile_component").assertIsDisplayed()
        composeTestRule.onNodeWithTag("user_profile_avatar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_display_name").assertTextContains("Nova Pulse")
        composeTestRule.onNodeWithTag("user_profile_handle").assertTextContains("@nova.pulse")
        composeTestRule.onNodeWithTag("user_profile_bio").assertTextContains("Visual synth artist & vertical cinema director")
        composeTestRule.onNodeWithTag("firestore_profile_persistence_badge").assertIsDisplayed()

        // 2. Verify Grid of Uploaded Videos is displayed and interactive
        composeTestRule.onNodeWithTag("user_profile_videos_grid").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_video_tile_701").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(701L, clickedVideoId)
        composeTestRule.onNodeWithTag("profile_video_tile_702").assertIsDisplayed()

        // 3. Edit Handle and Bio via Edit Profile Dialog and verify Firebase Firestore persistence
        composeTestRule.onNodeWithTag("edit_profile_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("edit_username_input").performTextClearance()
        composeTestRule.onNodeWithTag("edit_username_input").performTextInput("nova.firestore")
        composeTestRule.onNodeWithTag("edit_bio_input").performTextClearance()
        composeTestRule.onNodeWithTag("edit_bio_input").performTextInput("Updated bio persisted in Firebase Firestore")
        composeTestRule.onNodeWithTag("save_profile_button").performClick()
        composeTestRule.waitForIdle()

        // Verify UI immediately reflects the updated Firestore document
        composeTestRule.onNodeWithTag("user_profile_handle").assertTextContains("@nova.firestore")
        composeTestRule.onNodeWithTag("user_profile_bio").assertTextContains("Updated bio persisted in Firebase Firestore")

        // Verify a brand-new FirestoreProfilePersistenceEngine instance reads the persisted document
        val reloadedEngine = FirestoreProfilePersistenceEngine(context)
        val persistedDoc = runBlocking { reloadedEngine.fetchUserProfileDocument(77L) }
        assertNotNull("Persisted Firestore document must be retrievable across sessions", persistedDoc)
        assertEquals("@nova.firestore", persistedDoc!!.handle)
        assertEquals("Updated bio persisted in Firebase Firestore", persistedDoc.bio)
        assertEquals(2, persistedDoc.uploadedVideos.size)
    }
}
