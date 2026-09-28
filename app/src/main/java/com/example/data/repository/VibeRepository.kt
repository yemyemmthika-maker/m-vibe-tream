package com.example.data.repository

import android.content.Context
import com.example.AppConfig
import com.example.data.engine.PasswordSecurityEngine
import com.example.data.engine.TrustAndSafetyEngine
import com.example.data.firestore.FirestoreProfilePersistenceEngine
import com.example.data.firestore.FirestoreUserProfileDocument
import com.example.data.local.BlockedUserEntity
import com.example.data.local.CommentEntity
import com.example.data.local.FollowEntity
import com.example.data.local.HashtagEntity
import com.example.data.local.LikeEntity
import com.example.data.local.MessageEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.NotificationEventType
import com.example.data.local.NotificationManagementService
import com.example.data.local.ReportEntity
import com.example.data.local.SoundEntity
import com.example.data.local.UserEntity
import com.example.data.local.VibeDao
import com.example.data.local.VideoEntity
import kotlinx.coroutines.flow.Flow

class VibeRepository(
    private val dao: VibeDao,
    appContext: Context? = null,
    val notificationService: NotificationManagementService = NotificationManagementService(dao, appContext),
    val firestoreProfileEngine: FirestoreProfilePersistenceEngine = FirestoreProfilePersistenceEngine(appContext)
) {

    val allUsers: Flow<List<UserEntity>> = dao.observeAllUsers()
    val approvedVideos: Flow<List<VideoEntity>> = dao.observeApprovedVideos()
    val allVideosForAdmin: Flow<List<VideoEntity>> = dao.observeAllVideosForAdmin()
    val allComments: Flow<List<CommentEntity>> = dao.observeAllComments()
    val allFollows: Flow<List<FollowEntity>> = dao.observeAllFollows()
    val hashtags: Flow<List<HashtagEntity>> = dao.observeHashtags()
    val sounds: Flow<List<SoundEntity>> = dao.observeSounds()
    val allReports: Flow<List<ReportEntity>> = dao.observeAllReports()

    fun observeUser(userId: Long): Flow<UserEntity?> = dao.observeUserById(userId)
    fun observeLikesForUser(userId: Long): Flow<List<LikeEntity>> = dao.observeLikesForUser(userId)
    fun observeFollowingForUser(userId: Long): Flow<List<FollowEntity>> = dao.observeFollowingForUser(userId)
    fun observeFollowersForUser(userId: Long): Flow<List<FollowEntity>> = dao.observeFollowersForUser(userId)
    fun observeCommentsForVideo(videoId: Long): Flow<List<CommentEntity>> = dao.observeCommentsForVideo(videoId)
    fun observeNotifications(userId: Long): Flow<List<NotificationEntity>> =
        notificationService.observeNotifications(userId)
    fun observeNotificationsByType(userId: Long, eventType: NotificationEventType): Flow<List<NotificationEntity>> =
        notificationService.observeNotificationsByType(userId, eventType)
    fun observeUnreadNotificationCount(userId: Long): Flow<Int> =
        notificationService.observeUnreadCount(userId)
    fun observeMessages(userId: Long): Flow<List<MessageEntity>> = dao.observeMessagesForUser(userId)
    fun observeBlockedUsers(userId: Long): Flow<List<BlockedUserEntity>> = dao.observeBlockedUsers(userId)

    suspend fun ensureSeedData(): Long {
        if (dao.getUserCount() > 0) {
            val defaultUser = dao.findUserByUsernameOrEmail("nova.pulse")
            return defaultUser?.id ?: 1L
        }

        val now = System.currentTimeMillis()
        val users = listOf(
            UserEntity(
                id = 1L,
                username = "nova.pulse",
                displayName = "Nova Pulse",
                email = "nova@vibestream.social",
                passwordHash = "pulse123",
                bio = "Visual synth artist & vertical cinema director ✨ | Platform Creator & Admin",
                avatarColorHex = "#FF3366",
                followersCount = 48200,
                followingCount = 3,
                totalLikesReceived = 319400,
                isVerified = true,
                isAdmin = true,
                createdAt = now - 86_400_000L * 45
            ),
            UserEntity(
                id = 2L,
                username = "kai.motion",
                displayName = "Kai Motion",
                email = "kai@vibestream.social",
                passwordHash = "motion123",
                bio = "Tokyo rooftop choreography & neon transitions ⚡ Daily moves",
                avatarColorHex = "#7C4DFF",
                followersCount = 128500,
                followingCount = 142,
                totalLikesReceived = 940200,
                isVerified = true,
                isAdmin = false,
                createdAt = now - 86_400_000L * 38
            ),
            UserEntity(
                id = 3L,
                username = "aria.synth",
                displayName = "Aria SynthLab",
                email = "aria@vibestream.social",
                passwordHash = "synth123",
                bio = "Modular beats in 30 seconds 🎹 Grab my original sounds for your clips!",
                avatarColorHex = "#00E5FF",
                followersCount = 89300,
                followingCount = 95,
                totalLikesReceived = 612000,
                isVerified = true,
                isAdmin = false,
                createdAt = now - 86_400_000L * 30
            ),
            UserEntity(
                id = 4L,
                username = "milo.frame",
                displayName = "Milo Frame",
                email = "milo@vibestream.social",
                passwordHash = "frame123",
                bio = "Handheld street photography & color grading breakdowns 🎞️",
                avatarColorHex = "#FFB300",
                followersCount = 34900,
                followingCount = 210,
                totalLikesReceived = 184500,
                isVerified = false,
                isAdmin = false,
                createdAt = now - 86_400_000L * 22
            ),
            UserEntity(
                id = 5L,
                username = "zoe.kinetic",
                displayName = "Zoe Kinetic",
                email = "zoe@vibestream.social",
                passwordHash = "kinetic123",
                bio = "3D shader experiments + interactive cyber fashion 🔮",
                avatarColorHex = "#00E676",
                followersCount = 67100,
                followingCount = 88,
                totalLikesReceived = 425000,
                isVerified = true,
                isAdmin = false,
                createdAt = now - 86_400_000L * 15
            ),
            UserEntity(
                id = 6L,
                username = "spam.bot99",
                displayName = "Crypto Prize Bot",
                email = "bot@spam.example",
                passwordHash = "bot123",
                bio = "Click links for instant giveaways",
                avatarColorHex = "#9E9E9E",
                followersCount = 12,
                followingCount = 1900,
                totalLikesReceived = 4,
                isVerified = false,
                isAdmin = false,
                accountStatus = "SUSPENDED",
                createdAt = now - 86_400_000L * 2
            )
        )
        dao.insertUsers(users)

        val sounds = listOf(
            SoundEntity(
                id = 1L,
                title = "Midnight Prism (Original Mix)",
                artist = "Aria SynthLab",
                durationSec = 28,
                bpm = 126,
                usageCount = 14820,
                isTrending = true,
                coverColorHex = "#FF3366"
            ),
            SoundEntity(
                id = 2L,
                title = "Shibuya Rain Drift",
                artist = "Kai Motion x Lofi Grid",
                durationSec = 22,
                bpm = 118,
                usageCount = 9430,
                isTrending = true,
                coverColorHex = "#7C4DFF"
            ),
            SoundEntity(
                id = 3L,
                title = "Cybernetic Horizon",
                artist = "Nova Pulse",
                durationSec = 30,
                bpm = 132,
                usageCount = 7850,
                isTrending = true,
                coverColorHex = "#00E5FF"
            ),
            SoundEntity(
                id = 4L,
                title = "Analog Sunset Tape",
                artist = "Milo Frame",
                durationSec = 19,
                bpm = 96,
                usageCount = 4310,
                isTrending = false,
                coverColorHex = "#FFB300"
            ),
            SoundEntity(
                id = 5L,
                title = "Kinetic Bassline 808",
                artist = "Zoe Kinetic",
                durationSec = 25,
                bpm = 140,
                usageCount = 11200,
                isTrending = true,
                coverColorHex = "#00E676"
            ),
            SoundEntity(
                id = 6L,
                title = "Velvet Echoes (Acoustic Loop)",
                artist = "Studio Collective",
                durationSec = 32,
                bpm = 105,
                usageCount = 2890,
                isTrending = false,
                coverColorHex = "#FF6B8E"
            )
        )
        dao.insertSounds(sounds)

        val hashtags = listOf(
            HashtagEntity("neonpulse", "#NeonPulse", "Visual Art", 4280, 18_400_000L, true),
            HashtagEntity("verticalcinema", "#VerticalCinema", "Filmmaking", 3190, 12_900_000L, true),
            HashtagEntity("synthjam", "#SynthJam", "Music", 2840, 9_600_000L, true),
            HashtagEntity("rooftopflow", "#RooftopFlow", "Dance", 5120, 24_100_000L, true),
            HashtagEntity("shaderart", "#ShaderArt", "Tech & 3D", 1740, 6_300_000L, true),
            HashtagEntity("streetgrade", "#StreetGrade", "Photography", 2290, 8_100_000L, false),
            HashtagEntity("behindthelens", "#BehindTheLens", "Tutorials", 1950, 5_400_000L, false),
            HashtagEntity("creatorchallenge", "#CreatorChallenge", "Community", 6400, 31_000_000L, true)
        )
        dao.insertHashtags(hashtags)

        val sampleMp4Urls = AppConfig.PLACEHOLDER_VIDEO_URLS

        val videos = listOf(
            VideoEntity(
                id = 1L,
                creatorId = 2L,
                creatorUsername = "kai.motion",
                creatorDisplayName = "Kai Motion",
                creatorAvatarHex = "#7C4DFF",
                creatorVerified = true,
                caption = "Late night rooftop footwork synced to 126 BPM! Wait for the neon freeze frame at 0:12 ⚡🔥",
                hashtags = "#RooftopFlow #NeonPulse #CreatorChallenge",
                soundId = 1L,
                soundTitle = "Midnight Prism (Original Mix)",
                soundArtist = "Aria SynthLab",
                videoUri = sampleMp4Urls[0],
                cdnStreamUrl = "${AppConfig.CDN_BASE_URL}/v_1001_1080p_hevc.mp4",
                thumbnailThemeIndex = 0,
                thumbnailHeadline = "ROOFTOP NEON STEP",
                durationSec = 15,
                bitrateKbps = 2450,
                likesCount = 18420,
                commentsCount = 4,
                sharesCount = 1290,
                viewsCount = 94300,
                createdAt = now - 3_600_000L * 2
            ),
            VideoEntity(
                id = 2L,
                creatorId = 3L,
                creatorUsername = "aria.synth",
                creatorDisplayName = "Aria SynthLab",
                creatorAvatarHex = "#00E5FF",
                creatorVerified = true,
                caption = "Layering an analog arpeggiator with granular vocals from scratch. Headphones recommended 🎧🎹",
                hashtags = "#SynthJam #NeonPulse #BehindTheLens",
                soundId = 2L,
                soundTitle = "Shibuya Rain Drift",
                soundArtist = "Kai Motion x Lofi Grid",
                videoUri = sampleMp4Urls[1],
                cdnStreamUrl = "${AppConfig.CDN_BASE_URL}/v_1002_1080p_hevc.mp4",
                thumbnailThemeIndex = 1,
                thumbnailHeadline = "LIVE MODULAR SYNTH",
                durationSec = 22,
                bitrateKbps = 2200,
                likesCount = 12980,
                commentsCount = 2,
                sharesCount = 845,
                viewsCount = 62100,
                createdAt = now - 3_600_000L * 5
            ),
            VideoEntity(
                id = 3L,
                creatorId = 5L,
                creatorUsername = "zoe.kinetic",
                creatorDisplayName = "Zoe Kinetic",
                creatorAvatarHex = "#00E676",
                creatorVerified = true,
                caption = "Real-time reactive cloth simulation driven by bass frequencies. Built in 45 mins! 🔮✨",
                hashtags = "#ShaderArt #VerticalCinema #CreatorChallenge",
                soundId = 5L,
                soundTitle = "Kinetic Bassline 808",
                soundArtist = "Zoe Kinetic",
                videoUri = sampleMp4Urls[2],
                cdnStreamUrl = "${AppConfig.CDN_BASE_URL}/v_1003_1080p_hevc.mp4",
                thumbnailThemeIndex = 2,
                thumbnailHeadline = "AUDIO REACTIVE 3D",
                durationSec = 18,
                bitrateKbps = 2600,
                likesCount = 24150,
                commentsCount = 1,
                sharesCount = 2110,
                viewsCount = 118000,
                createdAt = now - 3_600_000L * 9
            ),
            VideoEntity(
                id = 4L,
                creatorId = 4L,
                creatorUsername = "milo.frame",
                creatorDisplayName = "Milo Frame",
                creatorAvatarHex = "#FFB300",
                creatorVerified = false,
                caption = "35mm anamorphic lens flare test in rainy alleyways — raw vs graded split screen 🎞️🌧️",
                hashtags = "#VerticalCinema #StreetGrade #BehindTheLens",
                soundId = 4L,
                soundTitle = "Analog Sunset Tape",
                soundArtist = "Milo Frame",
                videoUri = sampleMp4Urls[3],
                cdnStreamUrl = "${AppConfig.CDN_BASE_URL}/v_1004_1080p_hevc.mp4",
                thumbnailThemeIndex = 3,
                thumbnailHeadline = "RAW VS GRADED 35MM",
                durationSec = 19,
                bitrateKbps = 2150,
                likesCount = 7640,
                commentsCount = 1,
                sharesCount = 410,
                viewsCount = 39500,
                createdAt = now - 3_600_000L * 14
            ),
            VideoEntity(
                id = 5L,
                creatorId = 1L,
                creatorUsername = "nova.pulse",
                creatorDisplayName = "Nova Pulse",
                creatorAvatarHex = "#FF3366",
                creatorVerified = true,
                caption = "Welcome to VibeStream Studio! Swipe up for For You, tap to pause, double-tap to pulse-like 🚀",
                hashtags = "#NeonPulse #VerticalCinema #CreatorChallenge",
                soundId = 3L,
                soundTitle = "Cybernetic Horizon",
                soundArtist = "Nova Pulse",
                videoUri = sampleMp4Urls[4],
                cdnStreamUrl = "${AppConfig.CDN_BASE_URL}/v_1005_1080p_hevc.mp4",
                thumbnailThemeIndex = 4,
                thumbnailHeadline = "CYBERNETIC HORIZON",
                durationSec = 24,
                bitrateKbps = 2500,
                likesCount = 15800,
                commentsCount = 1,
                sharesCount = 980,
                viewsCount = 78400,
                createdAt = now - 3_600_000L * 20
            ),
            VideoEntity(
                id = 6L,
                creatorId = 6L,
                creatorUsername = "spam.bot99",
                creatorDisplayName = "Crypto Prize Bot",
                creatorAvatarHex = "#9E9E9E",
                creatorVerified = false,
                caption = "Suspicious promotional clip flagged by community safety filter for review.",
                hashtags = "#CreatorChallenge",
                soundId = 6L,
                soundTitle = "Velvet Echoes (Acoustic Loop)",
                soundArtist = "Studio Collective",
                videoUri = sampleMp4Urls[5],
                cdnStreamUrl = "${AppConfig.CDN_BASE_URL}/v_1006_flagged.mp4",
                thumbnailThemeIndex = 5,
                thumbnailHeadline = "FLAGGED PROMO CLIP",
                durationSec = 12,
                bitrateKbps = 1800,
                likesCount = 2,
                commentsCount = 0,
                sharesCount = 0,
                viewsCount = 85,
                moderationStatus = "FLAGGED",
                createdAt = now - 3_600_000L * 26
            )
        )
        dao.insertVideos(videos)

        // Default user (1L) follows Kai (2L), Aria (3L), and Zoe (5L) so "Following" feed has videos immediately
        dao.insertFollow(FollowEntity(followerId = 1L, followingId = 2L))
        dao.insertFollow(FollowEntity(followerId = 1L, followingId = 3L))
        dao.insertFollow(FollowEntity(followerId = 1L, followingId = 5L))

        // Default user liked Video 1 and Video 3
        dao.insertLike(LikeEntity(videoId = 1L, userId = 1L))
        dao.insertLike(LikeEntity(videoId = 3L, userId = 1L))

        // Sample Comments and Nested Replies
        val comments = listOf(
            CommentEntity(
                id = 1L,
                videoId = 1L,
                authorId = 3L,
                authorUsername = "aria.synth",
                authorDisplayName = "Aria SynthLab",
                authorAvatarHex = "#00E5FF",
                parentCommentId = null,
                content = "The way you hit the synth breakdown at 0:12 is unreal!! 🔥🔥",
                likesCount = 342,
                isLikedByCurrentUser = true,
                createdAt = now - 3_200_000L
            ),
            CommentEntity(
                id = 2L,
                videoId = 1L,
                authorId = 2L,
                authorUsername = "kai.motion",
                authorDisplayName = "Kai Motion",
                authorAvatarHex = "#7C4DFF",
                parentCommentId = 1L,
                replyToUsername = "aria.synth",
                content = "@aria.synth Your track made the whole routine come alive! Collab part 2 soon 🙌",
                likesCount = 118,
                createdAt = now - 2_900_000L
            ),
            CommentEntity(
                id = 3L,
                videoId = 1L,
                authorId = 4L,
                authorUsername = "milo.frame",
                authorDisplayName = "Milo Frame",
                authorAvatarHex = "#FFB300",
                parentCommentId = null,
                content = "Lighting setup on that wet concrete reflection is 10/10 cinematography.",
                likesCount = 64,
                createdAt = now - 2_100_000L
            ),
            CommentEntity(
                id = 4L,
                videoId = 1L,
                authorId = 1L,
                authorUsername = "nova.pulse",
                authorDisplayName = "Nova Pulse",
                authorAvatarHex = "#FF3366",
                parentCommentId = 3L,
                replyToUsername = "milo.frame",
                content = "@milo.frame Totally agree, the cyan rim light contrast is super clean!",
                likesCount = 29,
                createdAt = now - 1_500_000L
            ),
            CommentEntity(
                id = 5L,
                videoId = 2L,
                authorId = 5L,
                authorUsername = "zoe.kinetic",
                authorDisplayName = "Zoe Kinetic",
                authorAvatarHex = "#00E676",
                parentCommentId = null,
                content = "Using this sound for my next particle shader clip! 🎹✨",
                likesCount = 91,
                createdAt = now - 4_000_000L
            ),
            CommentEntity(
                id = 6L,
                videoId = 2L,
                authorId = 3L,
                authorUsername = "aria.synth",
                authorDisplayName = "Aria SynthLab",
                authorAvatarHex = "#00E5FF",
                parentCommentId = 5L,
                replyToUsername = "zoe.kinetic",
                content = "@zoe.kinetic Can't wait to see it!! Tag me when it drops!",
                likesCount = 42,
                createdAt = now - 3_700_000L
            ),
            CommentEntity(
                id = 7L,
                videoId = 3L,
                authorId = 1L,
                authorUsername = "nova.pulse",
                authorDisplayName = "Nova Pulse",
                authorAvatarHex = "#FF3366",
                parentCommentId = null,
                content = "Frame rate is buttery smooth even with all those cloth vertices!",
                likesCount = 77,
                createdAt = now - 5_000_000L
            ),
            CommentEntity(
                id = 8L,
                videoId = 4L,
                authorId = 2L,
                authorUsername = "kai.motion",
                authorDisplayName = "Kai Motion",
                authorAvatarHex = "#7C4DFF",
                parentCommentId = null,
                content = "We need to shoot a dance reel on this 35mm setup in Shinjuku!",
                likesCount = 53,
                createdAt = now - 6_000_000L
            ),
            CommentEntity(
                id = 9L,
                videoId = 5L,
                authorId = 3L,
                authorUsername = "aria.synth",
                authorDisplayName = "Aria SynthLab",
                authorAvatarHex = "#00E5FF",
                parentCommentId = null,
                content = "Love the color grading on @nova.pulse's studio reel! 🚀",
                likesCount = 104,
                createdAt = now - 7_000_000L
            )
        )
        dao.insertComments(comments)

        // All 5 Notification Types: FOLLOW, LIKE, COMMENT, REPLY, MENTION
        val notifications = listOf(
            NotificationEntity(
                recipientId = 1L,
                actorId = 2L,
                actorUsername = "kai.motion",
                actorDisplayName = "Kai Motion",
                actorAvatarHex = "#7C4DFF",
                type = "FOLLOW",
                referenceId = 2L,
                messageText = "started following you.",
                isRead = false,
                createdAt = now - 900_000L
            ),
            NotificationEntity(
                recipientId = 1L,
                actorId = 3L,
                actorUsername = "aria.synth",
                actorDisplayName = "Aria SynthLab",
                actorAvatarHex = "#00E5FF",
                type = "MENTION",
                referenceId = 5L,
                messageText = "mentioned you in a comment: \"Love the color grading on @nova.pulse's studio reel! 🚀\"",
                isRead = false,
                createdAt = now - 1_800_000L
            ),
            NotificationEntity(
                recipientId = 1L,
                actorId = 5L,
                actorUsername = "zoe.kinetic",
                actorDisplayName = "Zoe Kinetic",
                actorAvatarHex = "#00E676",
                type = "LIKE",
                referenceId = 5L,
                messageText = "liked your video \"CYBERNETIC HORIZON\".",
                isRead = false,
                createdAt = now - 3_600_000L
            ),
            NotificationEntity(
                recipientId = 1L,
                actorId = 4L,
                actorUsername = "milo.frame",
                actorDisplayName = "Milo Frame",
                actorAvatarHex = "#FFB300",
                type = "REPLY",
                referenceId = 1L,
                messageText = "replied to your comment on \"ROOFTOP NEON STEP\".",
                isRead = true,
                createdAt = now - 7_200_000L
            ),
            NotificationEntity(
                recipientId = 1L,
                actorId = 3L,
                actorUsername = "aria.synth",
                actorDisplayName = "Aria SynthLab",
                actorAvatarHex = "#00E5FF",
                type = "COMMENT",
                referenceId = 5L,
                messageText = "commented on your video: \"Love the color grading!\"",
                isRead = true,
                createdAt = now - 10_800_000L
            )
        )
        dao.insertNotifications(notifications)

        // Direct Messages (Conversations with Kai, Aria, and Milo)
        val messages = listOf(
            MessageEntity(
                ownerUserId = 1L,
                peerUserId = 2L,
                peerUsername = "kai.motion",
                peerDisplayName = "Kai Motion",
                peerAvatarHex = "#7C4DFF",
                senderId = 2L,
                receiverId = 1L,
                content = "Hey Nova! Loved your latest Cybernetic Horizon visual pack 🔥",
                isRead = true,
                createdAt = now - 5_400_000L
            ),
            MessageEntity(
                ownerUserId = 1L,
                peerUserId = 2L,
                peerUsername = "kai.motion",
                peerDisplayName = "Kai Motion",
                peerAvatarHex = "#7C4DFF",
                senderId = 1L,
                receiverId = 2L,
                content = "Thanks Kai! Want me to send over the custom LUT for your next rooftop clip?",
                isRead = true,
                createdAt = now - 5_100_000L
            ),
            MessageEntity(
                ownerUserId = 1L,
                peerUserId = 2L,
                peerUsername = "kai.motion",
                peerDisplayName = "Kai Motion",
                peerAvatarHex = "#7C4DFF",
                senderId = 2L,
                receiverId = 1L,
                content = "100% yes! Shooting at 11pm tonight, let's sync up ⚡",
                isRead = false,
                createdAt = now - 1_200_000L
            ),
            MessageEntity(
                ownerUserId = 1L,
                peerUserId = 3L,
                peerUsername = "aria.synth",
                peerDisplayName = "Aria SynthLab",
                peerAvatarHex = "#00E5FF",
                senderId = 3L,
                receiverId = 1L,
                content = "Just uploaded a new 132 BPM synth stem if you want to test preloading on it 🎹",
                isRead = false,
                createdAt = now - 2_400_000L
            ),
            MessageEntity(
                ownerUserId = 1L,
                peerUserId = 4L,
                peerUsername = "milo.frame",
                peerDisplayName = "Milo Frame",
                peerAvatarHex = "#FFB300",
                senderId = 4L,
                receiverId = 1L,
                content = "That compression pipeline kept the 35mm grain intact at 2.4 Mbps!",
                isRead = true,
                createdAt = now - 14_400_000L
            )
        )
        dao.insertMessages(messages)

        // Initial Moderation Reports for the Admin Dashboard
        val reports = listOf(
            ReportEntity(
                id = 1L,
                reporterId = 2L,
                reporterUsername = "kai.motion",
                targetType = "VIDEO",
                targetId = 6L,
                targetSummary = "Video #6 by @spam.bot99: \"Suspicious promotional clip\"",
                reason = "Spam or misleading engagement",
                status = "OPEN",
                createdAt = now - 3_600_000L * 4
            ),
            ReportEntity(
                id = 2L,
                reporterId = 3L,
                reporterUsername = "aria.synth",
                targetType = "USER",
                targetId = 6L,
                targetSummary = "Account @spam.bot99 (Crypto Prize Bot)",
                reason = "Spam or misleading engagement",
                status = "OPEN",
                createdAt = now - 3_600_000L * 3
            )
        )
        dao.insertReports(reports)

        return 1L
    }

    // --- AUTHENTICATION ---
    suspend fun login(usernameOrEmail: String, passwordPlain: String): Result<UserEntity> {
        val cleanInput = usernameOrEmail.trim().removePrefix("@")
        val user = dao.findUserByUsernameOrEmail(cleanInput)
            ?: return Result.failure(IllegalArgumentException("No account found matching \"$usernameOrEmail\"."))
        if (user.accountStatus == "BANNED") {
            return Result.failure(IllegalStateException("This account has been banned for violating Community Guidelines."))
        }
        if (!PasswordSecurityEngine.verifyPassword(passwordPlain, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Incorrect password. Try 'pulse123' for @nova.pulse or reset password."))
        }
        return Result.success(user)
    }

    suspend fun signUp(
        username: String,
        displayName: String,
        email: String,
        passwordPlain: String,
        isAdminAccount: Boolean = false
    ): Result<UserEntity> {
        val cleanUsername = username.trim().lowercase().removePrefix("@").replace(" ", ".")
        if (cleanUsername.length < 3) {
            return Result.failure(IllegalArgumentException("Username must be at least 3 characters."))
        }
        if (dao.findUserByUsernameOrEmail(cleanUsername) != null) {
            return Result.failure(IllegalArgumentException("Username @$cleanUsername is already taken."))
        }
        if (dao.findUserByUsernameOrEmail(email.trim()) != null) {
            return Result.failure(IllegalArgumentException("An account with email $email already exists."))
        }
        val colors = listOf("#FF3366", "#7C4DFF", "#00E5FF", "#FFB300", "#00E676")
        val newUser = UserEntity(
            username = cleanUsername,
            displayName = displayName.trim().ifBlank { cleanUsername },
            email = email.trim(),
            passwordHash = PasswordSecurityEngine.hashPassword(passwordPlain),
            bio = "New creator on ${AppConfig.APP_NAME} ✨",
            avatarColorHex = colors[cleanUsername.hashCode().ushr(1) % colors.size],
            isAdmin = isAdminAccount
        )
        val newId = dao.insertUser(newUser)
        val saved = dao.getUserById(newId) ?: newUser.copy(id = newId)
        return Result.success(saved)
    }

    suspend fun resetPassword(usernameOrEmail: String, newPassword: String): Result<String> {
        val user = dao.findUserByUsernameOrEmail(usernameOrEmail.trim().removePrefix("@"))
            ?: return Result.failure(IllegalArgumentException("No account found for \"$usernameOrEmail\"."))
        dao.updateUser(user.copy(passwordHash = PasswordSecurityEngine.hashPassword(newPassword)))
        return Result.success("Password updated for @${user.username}. You can now sign in.")
    }

    suspend fun changePassword(userId: Long, newPassword: String): Result<Unit> {
        val user = dao.getUserById(userId) ?: return Result.failure(IllegalStateException("User not found"))
        dao.updateUser(user.copy(passwordHash = PasswordSecurityEngine.hashPassword(newPassword)))
        return Result.success(Unit)
    }

    suspend fun deleteAccount(userId: Long) {
        dao.deleteVideosByCreator(userId)
        dao.deleteUserById(userId)
    }

    // --- FEED & SOCIAL ACTIONS ---
    suspend fun toggleLikeVideo(video: VideoEntity, currentUser: UserEntity): Boolean {
        val existing = dao.getLike(video.id, currentUser.id)
        return if (existing != null) {
            dao.deleteLike(video.id, currentUser.id)
            dao.updateVideo(video.copy(likesCount = (video.likesCount - 1).coerceAtLeast(0)))
            val creator = dao.getUserById(video.creatorId)
            if (creator != null) {
                dao.updateUser(creator.copy(totalLikesReceived = (creator.totalLikesReceived - 1).coerceAtLeast(0)))
            }
            false
        } else {
            dao.insertLike(LikeEntity(videoId = video.id, userId = currentUser.id))
            dao.updateVideo(video.copy(likesCount = video.likesCount + 1))
            val creator = dao.getUserById(video.creatorId)
            if (creator != null) {
                dao.updateUser(creator.copy(totalLikesReceived = creator.totalLikesReceived + 1))
            }
            if (video.creatorId != currentUser.id) {
                notificationService.recordLikeNotification(
                    actor = currentUser,
                    video = video
                )
            }
            true
        }
    }

    suspend fun incrementShareCount(video: VideoEntity) {
        dao.updateVideo(video.copy(sharesCount = video.sharesCount + 1))
    }

    suspend fun followCreator(currentUser: UserEntity, targetUserId: Long): Boolean {
        if (currentUser.id == targetUserId) return false
        val existing = dao.getFollow(currentUser.id, targetUserId)
        if (existing != null) return true
        val targetUser = dao.getUserById(targetUserId)
        dao.insertFollow(FollowEntity(followerId = currentUser.id, followingId = targetUserId))
        dao.updateUser(currentUser.copy(followingCount = currentUser.followingCount + 1))
        if (targetUser != null) {
            dao.updateUser(targetUser.copy(followersCount = targetUser.followersCount + 1))
        }
        notificationService.recordFollowNotification(
            follower = currentUser,
            targetUserId = targetUserId
        )
        return true
    }

    suspend fun unfollowCreator(currentUser: UserEntity, targetUserId: Long): Boolean {
        if (currentUser.id == targetUserId) return false
        val existing = dao.getFollow(currentUser.id, targetUserId) ?: return false
        val targetUser = dao.getUserById(targetUserId)
        dao.deleteFollow(currentUser.id, targetUserId)
        dao.updateUser(currentUser.copy(followingCount = (currentUser.followingCount - 1).coerceAtLeast(0)))
        if (targetUser != null) {
            dao.updateUser(targetUser.copy(followersCount = (targetUser.followersCount - 1).coerceAtLeast(0)))
        }
        return false
    }

    suspend fun toggleFollow(currentUser: UserEntity, targetUserId: Long): Boolean {
        if (currentUser.id == targetUserId) return false
        val existing = dao.getFollow(currentUser.id, targetUserId)
        return if (existing != null) {
            unfollowCreator(currentUser, targetUserId)
        } else {
            followCreator(currentUser, targetUserId)
        }
    }

    // --- COMMENTS & REPLIES ---
    suspend fun addComment(
        video: VideoEntity,
        author: UserEntity,
        rawContent: String,
        parentComment: CommentEntity? = null
    ): Result<CommentEntity> {
        val text = rawContent.trim()
        if (text.isBlank()) {
            return Result.failure(IllegalArgumentException("Comment cannot be empty."))
        }
        // Check NLP Trust & Safety Engine filter
        val safetyAudit = TrustAndSafetyEngine.evaluateTextSafety(text)
        if (safetyAudit.isBlocked) {
            return Result.failure(
                IllegalArgumentException(
                    "Comment blocked by Trust & Safety Engine (${safetyAudit.severityLabel}): ${safetyAudit.flaggedSignals.firstOrNull().orEmpty()}"
                )
            )
        }

        val comment = CommentEntity(
            videoId = video.id,
            authorId = author.id,
            authorUsername = author.username,
            authorDisplayName = author.displayName,
            authorAvatarHex = author.avatarColorHex,
            parentCommentId = parentComment?.id,
            replyToUsername = parentComment?.authorUsername,
            content = text
        )
        val id = dao.insertComment(comment)
        dao.updateVideo(video.copy(commentsCount = video.commentsCount + 1))

        // Create notification for video creator (COMMENT) or parent comment author (REPLY)
        notificationService.recordCommentNotification(
            author = author,
            video = video,
            commentText = text,
            parentComment = parentComment
        )

        // Detect @mentions and notify mentioned users
        val mentionRegex = Regex("@([A-Za-z0-9_.]+)")
        mentionRegex.findAll(text).forEach { matchResult ->
            val mentionedUsername = matchResult.groupValues[1]
            val mentionedUser = dao.findUserByUsernameOrEmail(mentionedUsername)
            if (mentionedUser != null && mentionedUser.id != author.id) {
                notificationService.recordMentionNotification(
                    actor = author,
                    recipientId = mentionedUser.id,
                    referenceId = video.id,
                    messageText = "mentioned you in a comment: \"${text.take(45)}\""
                )
            }
        }

        return Result.success(comment.copy(id = id))
    }

    suspend fun toggleLikeComment(comment: CommentEntity) {
        val liked = !comment.isLikedByCurrentUser
        val newCount = if (liked) comment.likesCount + 1 else (comment.likesCount - 1).coerceAtLeast(0)
        dao.updateComment(comment.copy(isLikedByCurrentUser = liked, likesCount = newCount))
    }

    suspend fun deleteComment(comment: CommentEntity, video: VideoEntity) {
        dao.deleteCommentAndReplies(comment.id)
        dao.updateVideo(video.copy(commentsCount = (video.commentsCount - 1).coerceAtLeast(0)))
    }

    // --- VIDEO UPLOAD & COMPRESSION ---
    suspend fun uploadProcessedVideo(
        creator: UserEntity,
        caption: String,
        rawHashtags: String,
        sound: SoundEntity,
        localVideoUri: String,
        thumbnailUri: String?,
        thumbnailThemeIndex: Int,
        thumbnailHeadline: String,
        durationSec: Int,
        targetBitrateKbps: Int,
        resolution: String = "1080x1920"
    ): Result<VideoEntity> {
        val cleanCaption = caption.trim()
        if (cleanCaption.isBlank()) {
            return Result.failure(IllegalArgumentException("Please add a caption for your video."))
        }
        val captionSafety = TrustAndSafetyEngine.evaluateTextSafety("$cleanCaption $rawHashtags")
        if (captionSafety.isBlocked) {
            return Result.failure(
                IllegalArgumentException(
                    "Upload blocked by Trust & Safety Engine (${captionSafety.severityLabel})."
                )
            )
        }
        val autoModerationStatus = if (captionSafety.riskScore >= 40) "FLAGGED" else "APPROVED"
        val formattedHashtags = rawHashtags
            .split(" ", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ") { if (it.startsWith("#")) it else "#$it" }
            .ifBlank { "#NeonPulse #VibeStream" }

        // Register any new hashtags in Hashtag table
        formattedHashtags.split(" ").forEach { dispTag ->
            val key = dispTag.removePrefix("#").lowercase()
            if (key.isNotBlank()) {
                val existing = dao.getHashtag(key)
                if (existing != null) {
                    dao.insertHashtag(
                        existing.copy(
                            videosCount = existing.videosCount + 1,
                            viewsCount = existing.viewsCount + 500L
                        )
                    )
                } else {
                    dao.insertHashtag(
                        HashtagEntity(
                            tag = key,
                            displayTag = dispTag,
                            category = "Community",
                            videosCount = 1,
                            viewsCount = 1000L,
                            isTrending = true
                        )
                    )
                }
            }
        }

        // Update sound usage count
        dao.updateSound(sound.copy(usageCount = sound.usageCount + 1))

        val streamUri = localVideoUri.ifBlank {
            AppConfig.PLACEHOLDER_VIDEO_URLS.first()
        }
        val video = VideoEntity(
            creatorId = creator.id,
            creatorUsername = creator.username,
            creatorDisplayName = creator.displayName,
            creatorAvatarHex = creator.avatarColorHex,
            creatorVerified = creator.isVerified,
            caption = cleanCaption,
            hashtags = formattedHashtags,
            soundId = sound.id,
            soundTitle = sound.title,
            soundArtist = sound.artist,
            videoUri = streamUri,
            cdnStreamUrl = "${AppConfig.CDN_BASE_URL}/v_${System.currentTimeMillis()}_${targetBitrateKbps}kbps.mp4",
            thumbnailUri = thumbnailUri?.takeIf { it.isNotBlank() },
            thumbnailThemeIndex = thumbnailThemeIndex,
            thumbnailHeadline = thumbnailHeadline.trim().ifBlank { cleanCaption.take(18).uppercase() },
            durationSec = durationSec,
            resolution = resolution,
            bitrateKbps = targetBitrateKbps,
            isCompressed = true,
            moderationStatus = autoModerationStatus
        )
        val id = dao.insertVideo(video)
        val savedVideo = video.copy(id = id)
        val existingDoc = firestoreProfileEngine.getCachedOrPersistedProfile(creator.id)
        val currentCreatorVideos = existingDoc?.toVideoEntities().orEmpty().filter { it.id != id } + savedVideo
        firestoreProfileEngine.persistUserProfile(creator, currentCreatorVideos)
        return Result.success(savedVideo)
    }

    suspend fun deleteVideo(videoId: Long) {
        val target = dao.getVideoById(videoId)
        dao.deleteVideoById(videoId)
        if (target != null) {
            val creator = dao.getUserById(target.creatorId)
            if (creator != null) {
                val existingDoc = firestoreProfileEngine.getCachedOrPersistedProfile(creator.id)
                val remaining = existingDoc?.toVideoEntities().orEmpty().filter { it.id != videoId }
                firestoreProfileEngine.persistUserProfile(creator, remaining)
            }
        }
    }

    // --- PROFILE & SETTINGS (PERSISTED TO FIREBASE FIRESTORE + ROOM) ---
    suspend fun updateProfile(
        user: UserEntity,
        displayName: String,
        username: String,
        bio: String,
        avatarColorHex: String,
        avatarUri: String? = user.avatarUri,
        uploadedVideos: List<VideoEntity> = emptyList()
    ): Result<UserEntity> {
        val cleanUsername = username.trim().lowercase().removePrefix("@").replace(" ", ".")
        if (cleanUsername.length < 3) {
            return Result.failure(IllegalArgumentException("Username must be at least 3 characters."))
        }
        val cleanDisplay = displayName.trim().ifBlank { user.displayName }
        val updated = user.copy(
            displayName = cleanDisplay,
            username = cleanUsername,
            bio = bio.trim(),
            avatarColorHex = avatarColorHex,
            avatarUri = avatarUri
        )
        dao.updateUser(updated)
        dao.updateCreatorMetadataOnVideos(
            creatorId = user.id,
            username = cleanUsername,
            displayName = cleanDisplay,
            avatarHex = avatarColorHex
        )
        firestoreProfileEngine.updateProfileDetailsInFirestore(
            userId = updated.id,
            displayName = updated.displayName,
            handleOrUsername = updated.username,
            bio = updated.bio,
            avatarColorHex = updated.avatarColorHex,
            avatarUri = updated.avatarUri,
            existingUser = updated,
            uploadedVideos = uploadedVideos
        )
        return Result.success(updated)
    }

    suspend fun syncUserProfileToFirestore(
        user: UserEntity,
        uploadedVideos: List<VideoEntity>
    ): FirestoreUserProfileDocument {
        return firestoreProfileEngine.persistUserProfile(user, uploadedVideos)
    }

    suspend fun updateUserSettings(updated: UserEntity) {
        dao.updateUser(updated)
    }

    // --- DIRECT MESSAGES ---
    suspend fun sendDirectMessage(
        sender: UserEntity,
        peer: UserEntity,
        content: String
    ): Result<Unit> {
        val text = content.trim()
        if (text.isBlank()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        dao.insertMessage(
            MessageEntity(
                ownerUserId = sender.id,
                peerUserId = peer.id,
                peerUsername = peer.username,
                peerDisplayName = peer.displayName,
                peerAvatarHex = peer.avatarColorHex,
                senderId = sender.id,
                receiverId = peer.id,
                content = text,
                isRead = true
            )
        )
        // Create notification for recipient
        notificationService.recordMentionNotification(
            actor = sender,
            recipientId = peer.id,
            referenceId = sender.id,
            messageText = "sent you a direct message: \"${text.take(36)}\""
        )
        return Result.success(Unit)
    }

    suspend fun markNotificationRead(notificationId: Long) {
        notificationService.markNotificationRead(notificationId)
    }

    suspend fun markAllNotificationsRead(userId: Long) {
        notificationService.markAllNotificationsRead(userId)
    }

    suspend fun deleteNotification(notificationId: Long) {
        notificationService.deleteNotification(notificationId)
    }

    suspend fun clearAllNotifications(userId: Long) {
        notificationService.clearAllNotifications(userId)
    }

    suspend fun markConversationRead(userId: Long, peerId: Long) {
        dao.markConversationRead(userId, peerId)
    }

    // --- SAFETY, BLOCKING & MODERATION ---
    suspend fun blockUser(blocker: UserEntity, targetUser: UserEntity) {
        dao.insertBlockedUser(
            BlockedUserEntity(
                blockerId = blocker.id,
                blockedUserId = targetUser.id,
                blockedUsername = targetUser.username,
                blockedDisplayName = targetUser.displayName,
                blockedAvatarHex = targetUser.avatarColorHex
            )
        )
        dao.deleteFollow(blocker.id, targetUser.id)
    }

    suspend fun unblockUser(blockerId: Long, blockedUserId: Long) {
        dao.unblockUser(blockerId, blockedUserId)
    }

    suspend fun submitReport(
        reporter: UserEntity,
        targetType: String,
        targetId: Long,
        targetSummary: String,
        reason: String
    ) {
        dao.insertReport(
            ReportEntity(
                reporterId = reporter.id,
                reporterUsername = reporter.username,
                targetType = targetType,
                targetId = targetId,
                targetSummary = targetSummary,
                reason = reason,
                status = "OPEN"
            )
        )
        if (targetType == "COMMENT") {
            val comment = dao.getCommentById(targetId)
            if (comment != null) {
                dao.updateComment(comment.copy(isReported = true))
            }
        } else if (targetType == "VIDEO") {
            val video = dao.getVideoById(targetId)
            if (video != null && video.moderationStatus == "APPROVED") {
                dao.updateVideo(video.copy(moderationStatus = "FLAGGED"))
            }
        }
    }

    // --- ADMIN DASHBOARD ACTIONS ---
    suspend fun adminUpdateReportStatus(report: ReportEntity, newStatus: String, notes: String) {
        dao.updateReport(report.copy(status = newStatus, adminNotes = notes))
    }

    suspend fun adminSetVideoModeration(video: VideoEntity, status: String) {
        dao.updateVideo(video.copy(moderationStatus = status))
    }

    suspend fun adminRemoveVideo(videoId: Long) {
        val video = dao.getVideoById(videoId)
        if (video != null) {
            dao.updateVideo(video.copy(moderationStatus = "REMOVED"))
        }
    }

    suspend fun adminSetUserAccountStatus(user: UserEntity, status: String) {
        dao.updateUser(user.copy(accountStatus = status))
    }
}
