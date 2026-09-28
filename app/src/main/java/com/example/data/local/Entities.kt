package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true), Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String,
    val email: String,
    val passwordHash: String,
    val bio: String = "",
    val avatarColorHex: String = "#FF3366",
    val avatarUri: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val totalLikesReceived: Int = 0,
    val isVerified: Boolean = false,
    val isAdmin: Boolean = false,
    val accountStatus: String = "ACTIVE", // ACTIVE, SUSPENDED, BANNED
    val isPrivateAccount: Boolean = false,
    val allowCommentsFromAll: Boolean = true,
    val allowDirectMessages: Boolean = true,
    val notifyLikes: Boolean = true,
    val notifyComments: Boolean = true,
    val notifyFollowers: Boolean = true,
    val notifyMessages: Boolean = true,
    val ageSafetyMode: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "videos",
    indices = [Index(value = ["creatorId"]), Index(value = ["soundId"])]
)
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creatorId: Long,
    val creatorUsername: String,
    val creatorDisplayName: String,
    val creatorAvatarHex: String,
    val creatorVerified: Boolean = false,
    val caption: String,
    val hashtags: String, // e.g. "#synthwave #neon #creator"
    val soundId: Long,
    val soundTitle: String,
    val soundArtist: String,
    val videoUri: String, // Local file/content URI or CDN MP4 stream URL
    val cdnStreamUrl: String,
    val thumbnailUri: String? = null,
    val thumbnailThemeIndex: Int = 0,
    val thumbnailHeadline: String = "",
    val durationSec: Int = 18,
    val resolution: String = "1080x1920",
    val bitrateKbps: Int = 2400,
    val isCompressed: Boolean = true,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val viewsCount: Int = 0,
    val ageRestricted: Boolean = false,
    val moderationStatus: String = "APPROVED", // APPROVED, FLAGGED, REMOVED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "comments",
    indices = [Index(value = ["videoId"]), Index(value = ["parentCommentId"])]
)
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: Long,
    val authorId: Long,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarHex: String,
    val parentCommentId: Long? = null, // non-null for comment replies
    val replyToUsername: String? = null,
    val content: String,
    val likesCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val isReported: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "likes",
    indices = [Index(value = ["videoId", "userId"], unique = true)]
)
data class LikeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: Long,
    val userId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "follows",
    indices = [Index(value = ["followerId", "followingId"], unique = true)]
)
data class FollowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val followerId: Long,
    val followingId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "hashtags")
data class HashtagEntity(
    @PrimaryKey val tag: String, // Normalized lowercase without #, e.g. "neonpulse"
    val displayTag: String, // e.g. "#NeonPulse"
    val category: String,
    val videosCount: Int = 1,
    val viewsCount: Long = 1200L,
    val isTrending: Boolean = false
)

@Entity(tableName = "sounds")
data class SoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val durationSec: Int = 30,
    val bpm: Int = 124,
    val usageCount: Int = 1,
    val isTrending: Boolean = true,
    val coverColorHex: String = "#7C4DFF"
)

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["recipientId"])]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipientId: Long,
    val actorId: Long,
    val actorUsername: String,
    val actorDisplayName: String,
    val actorAvatarHex: String,
    val type: String, // FOLLOW, LIKE, COMMENT, REPLY, MENTION
    val referenceId: Long = 0, // videoId or userId
    val messageText: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "messages",
    indices = [Index(value = ["senderId", "receiverId"]), Index(value = ["peerUserId"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long, // Current local account perspective or global participant
    val peerUserId: Long,
    val peerUsername: String,
    val peerDisplayName: String,
    val peerAvatarHex: String,
    val senderId: Long,
    val receiverId: Long,
    val content: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reporterId: Long,
    val reporterUsername: String,
    val targetType: String, // VIDEO, COMMENT, USER
    val targetId: Long,
    val targetSummary: String,
    val reason: String,
    val status: String = "OPEN", // OPEN, REVIEWED, ACTION_TAKEN, DISMISSED
    val adminNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "blocked_users",
    indices = [Index(value = ["blockerId", "blockedUserId"], unique = true)]
)
data class BlockedUserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blockerId: Long,
    val blockedUserId: Long,
    val blockedUsername: String,
    val blockedDisplayName: String,
    val blockedAvatarHex: String,
    val createdAt: Long = System.currentTimeMillis()
)
