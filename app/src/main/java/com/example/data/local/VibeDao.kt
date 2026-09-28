package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VibeDao {

    // --- USERS ---
    @Query("SELECT * FROM users ORDER BY followersCount DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUserById(userId: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) OR LOWER(email) = LOWER(:username) LIMIT 1")
    suspend fun findUserByUsernameOrEmail(username: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>): List<Long>

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Long)

    // --- VIDEOS ---
    @Query("SELECT * FROM videos WHERE moderationStatus != 'REMOVED' ORDER BY createdAt DESC")
    fun observeApprovedVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY createdAt DESC")
    fun observeAllVideosForAdmin(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE creatorId = :creatorId AND moderationStatus != 'REMOVED' ORDER BY createdAt DESC")
    fun observeUserVideos(creatorId: Long): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :videoId LIMIT 1")
    suspend fun getVideoById(videoId: Long): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>): List<Long>

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :videoId")
    suspend fun deleteVideoById(videoId: Long)

    @Query("DELETE FROM videos WHERE creatorId = :creatorId")
    suspend fun deleteVideosByCreator(creatorId: Long)

    @Query("UPDATE videos SET creatorUsername = :username, creatorDisplayName = :displayName, creatorAvatarHex = :avatarHex WHERE creatorId = :creatorId")
    suspend fun updateCreatorMetadataOnVideos(
        creatorId: Long,
        username: String,
        displayName: String,
        avatarHex: String
    )

    // --- LIKES ---
    @Query("SELECT * FROM likes WHERE userId = :userId")
    fun observeLikesForUser(userId: Long): Flow<List<LikeEntity>>

    @Query("SELECT * FROM likes WHERE videoId = :videoId AND userId = :userId LIMIT 1")
    suspend fun getLike(videoId: Long, userId: Long): LikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity)

    @Query("DELETE FROM likes WHERE videoId = :videoId AND userId = :userId")
    suspend fun deleteLike(videoId: Long, userId: Long)

    // --- FOLLOWS ---
    @Query("SELECT * FROM follows ORDER BY createdAt DESC")
    fun observeAllFollows(): Flow<List<FollowEntity>>

    @Query("SELECT * FROM follows WHERE followerId = :followerId ORDER BY createdAt DESC")
    fun observeFollowingForUser(followerId: Long): Flow<List<FollowEntity>>

    @Query("SELECT * FROM follows WHERE followingId = :followingId ORDER BY createdAt DESC")
    fun observeFollowersForUser(followingId: Long): Flow<List<FollowEntity>>

    @Query("SELECT * FROM follows WHERE followerId = :followerId AND followingId = :followingId LIMIT 1")
    suspend fun getFollow(followerId: Long, followingId: Long): FollowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun deleteFollow(followerId: Long, followingId: Long)

    // --- COMMENTS ---
    @Query("SELECT * FROM comments WHERE videoId = :videoId ORDER BY createdAt ASC")
    fun observeCommentsForVideo(videoId: Long): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments ORDER BY createdAt DESC")
    fun observeAllComments(): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE id = :commentId LIMIT 1")
    suspend fun getCommentById(commentId: Long): CommentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)

    @Update
    suspend fun updateComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :commentId OR parentCommentId = :commentId")
    suspend fun deleteCommentAndReplies(commentId: Long)

    // --- HASHTAGS ---
    @Query("SELECT * FROM hashtags ORDER BY viewsCount DESC")
    fun observeHashtags(): Flow<List<HashtagEntity>>

    @Query("SELECT * FROM hashtags WHERE tag = :tag LIMIT 1")
    suspend fun getHashtag(tag: String): HashtagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHashtag(hashtag: HashtagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHashtags(hashtags: List<HashtagEntity>)

    // --- SOUNDS ---
    @Query("SELECT * FROM sounds ORDER BY usageCount DESC")
    fun observeSounds(): Flow<List<SoundEntity>>

    @Query("SELECT * FROM sounds WHERE id = :soundId LIMIT 1")
    suspend fun getSoundById(soundId: Long): SoundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSound(sound: SoundEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSounds(sounds: List<SoundEntity>): List<Long>

    @Update
    suspend fun updateSound(sound: SoundEntity)

    // --- NOTIFICATIONS ---
    @Query("SELECT * FROM notifications WHERE recipientId = :userId ORDER BY createdAt DESC, id DESC")
    fun observeNotificationsForUser(userId: Long): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE recipientId = :userId AND type = :type ORDER BY createdAt DESC, id DESC")
    fun observeNotificationsByType(userId: Long, type: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE recipientId = :userId AND isRead = 0")
    fun observeUnreadNotificationCount(userId: Long): Flow<Int>

    @Query("SELECT * FROM notifications WHERE recipientId = :userId ORDER BY createdAt DESC, id DESC")
    suspend fun getNotificationsForUser(userId: Long): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE id = :notificationId LIMIT 1")
    suspend fun getNotificationById(notificationId: Long): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markNotificationRead(notificationId: Long)

    @Query("UPDATE notifications SET isRead = 1 WHERE recipientId = :userId")
    suspend fun markAllNotificationsRead(userId: Long)

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotificationById(notificationId: Long)

    @Query("DELETE FROM notifications WHERE recipientId = :userId")
    suspend fun clearNotificationsForUser(userId: Long)

    // --- MESSAGES ---
    @Query("SELECT * FROM messages WHERE senderId = :userId OR receiverId = :userId ORDER BY createdAt ASC")
    fun observeMessagesForUser(userId: Long): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET isRead = 1 WHERE receiverId = :userId AND peerUserId = :peerId")
    suspend fun markConversationRead(userId: Long, peerId: Long)

    // --- REPORTS ---
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun observeAllReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReports(reports: List<ReportEntity>)

    @Update
    suspend fun updateReport(report: ReportEntity)

    // --- BLOCKED USERS ---
    @Query("SELECT * FROM blocked_users WHERE blockerId = :userId ORDER BY createdAt DESC")
    fun observeBlockedUsers(userId: Long): Flow<List<BlockedUserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedUser(blockedUser: BlockedUserEntity)

    @Query("DELETE FROM blocked_users WHERE blockerId = :blockerId AND blockedUserId = :blockedUserId")
    suspend fun unblockUser(blockerId: Long, blockedUserId: Long)
}
