package com.example.data.local

import android.content.Context
import com.example.data.engine.SystemNotificationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Supported local notification event categories stored in Room.
 */
enum class NotificationEventType(
    val code: String,
    val displayLabel: String
) {
    ALL("ALL", "All Activity"),
    LIKE("LIKE", "Likes"),
    FOLLOW("FOLLOW", "Follows"),
    COMMENT("COMMENT", "Comments"),
    REPLY("REPLY", "Replies"),
    MENTION("MENTION", "Mentions");

    fun matches(notificationType: String): Boolean {
        return when (this) {
            ALL -> true
            COMMENT -> notificationType.equals("COMMENT", ignoreCase = true) ||
                notificationType.equals("REPLY", ignoreCase = true)
            else -> notificationType.equals(this.code, ignoreCase = true)
        }
    }

    companion object {
        fun fromCode(code: String): NotificationEventType {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ALL
        }
    }
}

/**
 * Immutable UI state model representing the user's notification center and category counts.
 */
data class NotificationCenterUiState(
    val recipientId: Long? = null,
    val allNotifications: List<NotificationEntity> = emptyList(),
    val filteredNotifications: List<NotificationEntity> = emptyList(),
    val selectedFilter: NotificationEventType = NotificationEventType.ALL,
    val unreadCount: Int = 0,
    val likesCount: Int = 0,
    val followsCount: Int = 0,
    val commentsCount: Int = 0,
    val mentionsCount: Int = 0
) {
    val totalCount: Int
        get() = allNotifications.size

    companion object {
        fun fromNotifications(
            recipientId: Long?,
            notifications: List<NotificationEntity>,
            selectedFilter: NotificationEventType = NotificationEventType.ALL,
            blockedActorIds: Set<Long> = emptySet()
        ): NotificationCenterUiState {
            val visible = notifications
                .filter { it.actorId !in blockedActorIds }
                .sortedWith(
                    compareByDescending<NotificationEntity> { it.createdAt }
                        .thenByDescending { it.id }
                )

            val filtered = visible.filter { selectedFilter.matches(it.type) }

            return NotificationCenterUiState(
                recipientId = recipientId,
                allNotifications = visible,
                filteredNotifications = filtered,
                selectedFilter = selectedFilter,
                unreadCount = visible.count { !it.isRead },
                likesCount = visible.count { it.type.equals("LIKE", ignoreCase = true) },
                followsCount = visible.count { it.type.equals("FOLLOW", ignoreCase = true) },
                commentsCount = visible.count {
                    it.type.equals("COMMENT", ignoreCase = true) ||
                        it.type.equals("REPLY", ignoreCase = true)
                },
                mentionsCount = visible.count { it.type.equals("MENTION", ignoreCase = true) }
            )
        }
    }
}

/**
 * Room-backed Notification Management Service for recording, querying, filtering,
 * marking as read, and deleting local notification events (Likes, Follows, Comments, Replies, Mentions).
 */
class NotificationManagementService(
    private val dao: VibeDao,
    private val appContext: Context? = null
) {

    init {
        if (appContext != null) {
            SystemNotificationEngine.ensureNotificationChannel(appContext)
        }
    }

    /**
     * Observes all local notification events for [recipientId] ordered newest first.
     */
    fun observeNotifications(recipientId: Long): Flow<List<NotificationEntity>> {
        return dao.observeNotificationsForUser(recipientId)
    }

    /**
     * Observes local notifications filtered by a specific [eventType] for [recipientId].
     */
    fun observeNotificationsByType(
        recipientId: Long,
        eventType: NotificationEventType
    ): Flow<List<NotificationEntity>> {
        return when (eventType) {
            NotificationEventType.ALL -> dao.observeNotificationsForUser(recipientId)
            NotificationEventType.COMMENT -> dao.observeNotificationsForUser(recipientId).map { list ->
                list.filter { eventType.matches(it.type) }
            }
            else -> dao.observeNotificationsByType(recipientId, eventType.code)
        }
    }

    /**
     * Observes the unread notification badge count for [recipientId].
     */
    fun observeUnreadCount(recipientId: Long): Flow<Int> {
        return dao.observeUnreadNotificationCount(recipientId)
    }

    /**
     * Observes a combined [NotificationCenterUiState] for [recipientId] with live filter and block-list support.
     */
    fun observeNotificationCenterState(
        recipientId: Long,
        selectedFilterFlow: Flow<NotificationEventType>,
        blockedActorIdsFlow: Flow<Set<Long>>
    ): Flow<NotificationCenterUiState> {
        return combine(
            dao.observeNotificationsForUser(recipientId),
            selectedFilterFlow,
            blockedActorIdsFlow
        ) { notifications, filter, blockedIds ->
            NotificationCenterUiState.fromNotifications(
                recipientId = recipientId,
                notifications = notifications,
                selectedFilter = filter,
                blockedActorIds = blockedIds
            )
        }
    }

    /**
     * Returns an immediate snapshot of stored notifications for [recipientId].
     */
    suspend fun getNotificationsForUser(recipientId: Long): List<NotificationEntity> {
        return dao.getNotificationsForUser(recipientId)
    }

    /**
     * Stores an arbitrary [NotificationEntity] in Room and returns the persisted entity with its generated ID.
     */
    suspend fun storeNotificationEvent(notification: NotificationEntity): NotificationEntity {
        val id = dao.insertNotification(notification)
        val saved = notification.copy(id = id)
        if (appContext != null) {
            SystemNotificationEngine.dispatchSystemNotification(appContext, saved)
        }
        return saved
    }

    /**
     * Records a local LIKE notification event in Room when [actor] likes [video].
     */
    suspend fun recordLikeNotification(
        actor: UserEntity,
        video: VideoEntity,
        recipientId: Long = video.creatorId,
        allowSelfNotification: Boolean = false,
        timestampMs: Long = System.currentTimeMillis()
    ): NotificationEntity? {
        if (!allowSelfNotification && recipientId == actor.id) return null
        val recipient = dao.getUserById(recipientId)
        if (recipient != null && !recipient.notifyLikes) return null

        val headline = video.thumbnailHeadline.ifBlank { video.caption.take(24) }
        val entity = NotificationEntity(
            recipientId = recipientId,
            actorId = actor.id,
            actorUsername = actor.username,
            actorDisplayName = actor.displayName,
            actorAvatarHex = actor.avatarColorHex,
            type = NotificationEventType.LIKE.code,
            referenceId = video.id,
            messageText = "liked your video \"$headline\".",
            isRead = false,
            createdAt = timestampMs
        )
        return storeNotificationEvent(entity)
    }

    /**
     * Records a local FOLLOW notification event in Room when [follower] follows [targetUserId].
     */
    suspend fun recordFollowNotification(
        follower: UserEntity,
        targetUserId: Long,
        allowSelfNotification: Boolean = false,
        timestampMs: Long = System.currentTimeMillis()
    ): NotificationEntity? {
        if (!allowSelfNotification && targetUserId == follower.id) return null
        val recipient = dao.getUserById(targetUserId)
        if (recipient != null && !recipient.notifyFollowers) return null

        val entity = NotificationEntity(
            recipientId = targetUserId,
            actorId = follower.id,
            actorUsername = follower.username,
            actorDisplayName = follower.displayName,
            actorAvatarHex = follower.avatarColorHex,
            type = NotificationEventType.FOLLOW.code,
            referenceId = follower.id,
            messageText = "started following you.",
            isRead = false,
            createdAt = timestampMs
        )
        return storeNotificationEvent(entity)
    }

    /**
     * Records a local COMMENT or REPLY notification event in Room when [author] comments on [video].
     */
    suspend fun recordCommentNotification(
        author: UserEntity,
        video: VideoEntity,
        commentText: String,
        parentComment: CommentEntity? = null,
        allowSelfNotification: Boolean = false,
        timestampMs: Long = System.currentTimeMillis()
    ): NotificationEntity? {
        val isReply = parentComment != null
        val recipientId = parentComment?.authorId ?: video.creatorId
        if (!allowSelfNotification && recipientId == author.id) return null

        val recipient = dao.getUserById(recipientId)
        if (recipient != null && !recipient.notifyComments) return null

        val cleanSnippet = commentText.trim().take(40)
        val typeCode = if (isReply) NotificationEventType.REPLY.code else NotificationEventType.COMMENT.code
        val message = if (isReply) {
            "replied to your comment: \"$cleanSnippet\""
        } else {
            "commented on your video: \"$cleanSnippet\""
        }

        val entity = NotificationEntity(
            recipientId = recipientId,
            actorId = author.id,
            actorUsername = author.username,
            actorDisplayName = author.displayName,
            actorAvatarHex = author.avatarColorHex,
            type = typeCode,
            referenceId = video.id,
            messageText = message,
            isRead = false,
            createdAt = timestampMs
        )
        return storeNotificationEvent(entity)
    }

    /**
     * Records a local MENTION notification event in Room when [actor] mentions [recipientId] or sends a message.
     */
    suspend fun recordMentionNotification(
        actor: UserEntity,
        recipientId: Long,
        referenceId: Long,
        messageText: String,
        allowSelfNotification: Boolean = false,
        timestampMs: Long = System.currentTimeMillis()
    ): NotificationEntity? {
        if (!allowSelfNotification && recipientId == actor.id) return null

        val entity = NotificationEntity(
            recipientId = recipientId,
            actorId = actor.id,
            actorUsername = actor.username,
            actorDisplayName = actor.displayName,
            actorAvatarHex = actor.avatarColorHex,
            type = NotificationEventType.MENTION.code,
            referenceId = referenceId,
            messageText = messageText,
            isRead = false,
            createdAt = timestampMs
        )
        return storeNotificationEvent(entity)
    }

    /**
     * Marks a single notification event as read in Room.
     */
    suspend fun markNotificationRead(notificationId: Long) {
        dao.markNotificationRead(notificationId)
    }

    /**
     * Marks all notification events for [recipientId] as read in Room.
     */
    suspend fun markAllNotificationsRead(recipientId: Long) {
        dao.markAllNotificationsRead(recipientId)
    }

    /**
     * Deletes a single notification event by ID from Room.
     */
    suspend fun deleteNotification(notificationId: Long) {
        dao.deleteNotificationById(notificationId)
    }

    /**
     * Clears all stored notification events for [recipientId] from Room.
     */
    suspend fun clearAllNotifications(recipientId: Long) {
        dao.clearNotificationsForUser(recipientId)
    }
}
