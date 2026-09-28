package com.example.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Represents the directional or mutual follow relationship between the active user and a creator.
 */
enum class FollowRelationshipStatus {
    SELF,
    NOT_FOLLOWING,
    FOLLOWED_BY,
    FOLLOWING,
    MUTUAL_FRIENDS;

    /**
     * Primary action label to display on a Follow/Unfollow button for this relationship state.
     */
    val actionButtonLabel: String
        get() = when (this) {
            SELF -> "You"
            NOT_FOLLOWING -> "Follow"
            FOLLOWED_BY -> "Follow Back"
            FOLLOWING -> "Following"
            MUTUAL_FRIENDS -> "Friends"
        }

    val isFollowingCreator: Boolean
        get() = this == FOLLOWING || this == MUTUAL_FRIENDS
}

/**
 * Enriched domain model for tracking a creator and the current user's follow relationship with them.
 */
data class FollowedCreatorInfo(
    val creator: UserEntity,
    val followedAt: Long = 0L,
    val relationshipStatus: FollowRelationshipStatus = FollowRelationshipStatus.NOT_FOLLOWING,
    val videosCount: Int = 0,
    val latestVideoHeadline: String? = null
) {
    val creatorId: Long get() = creator.id
    val isFollowing: Boolean get() = relationshipStatus.isFollowingCreator
    val isMutual: Boolean get() = relationshipStatus == FollowRelationshipStatus.MUTUAL_FRIENDS
}

/**
 * Immutable UI state model for the follow/unfollow system, allowing users to track
 * the creators they follow, their followers, mutual friends, and suggested creators.
 */
data class CreatorFollowUiState(
    val currentUserId: Long? = null,
    val followingUserIds: Set<Long> = emptySet(),
    val followerUserIds: Set<Long> = emptySet(),
    val followedCreators: List<FollowedCreatorInfo> = emptyList(),
    val followerCreators: List<FollowedCreatorInfo> = emptyList(),
    val suggestedCreators: List<FollowedCreatorInfo> = emptyList(),
    val lastUpdatedCreatorId: Long? = null
) {
    val mutualFriendIds: Set<Long>
        get() = followingUserIds.intersect(followerUserIds)

    val followingCount: Int
        get() = followingUserIds.size

    val followersCount: Int
        get() = followerUserIds.size

    fun isFollowing(creatorId: Long): Boolean = creatorId in followingUserIds

    fun isFollowedBy(creatorId: Long): Boolean = creatorId in followerUserIds

    fun isMutualFriend(creatorId: Long): Boolean =
        creatorId in followingUserIds && creatorId in followerUserIds

    fun relationshipWith(creatorId: Long): FollowRelationshipStatus {
        if (currentUserId != null && creatorId == currentUserId) {
            return FollowRelationshipStatus.SELF
        }
        val iFollowThem = creatorId in followingUserIds
        val theyFollowMe = creatorId in followerUserIds
        return when {
            iFollowThem && theyFollowMe -> FollowRelationshipStatus.MUTUAL_FRIENDS
            iFollowThem -> FollowRelationshipStatus.FOLLOWING
            theyFollowMe -> FollowRelationshipStatus.FOLLOWED_BY
            else -> FollowRelationshipStatus.NOT_FOLLOWING
        }
    }

    companion object {
        /**
         * Builds a complete [CreatorFollowUiState] from raw users, follow edges, and videos.
         */
        fun fromDomainData(
            currentUserId: Long?,
            allUsers: List<UserEntity>,
            allFollows: List<FollowEntity>,
            allVideos: List<VideoEntity> = emptyList(),
            lastUpdatedCreatorId: Long? = null
        ): CreatorFollowUiState {
            if (currentUserId == null) {
                return CreatorFollowUiState(currentUserId = null)
            }

            val outgoingFollows = allFollows
                .filter { it.followerId == currentUserId }
                .sortedByDescending { it.createdAt }
            val incomingFollows = allFollows
                .filter { it.followingId == currentUserId }
                .sortedByDescending { it.createdAt }

            val followingIds = outgoingFollows.map { it.followingId }.toSet()
            val followerIds = incomingFollows.map { it.followerId }.toSet()

            val usersById = allUsers.associateBy { it.id }
            val videosByCreator = allVideos
                .filter { it.moderationStatus != "REMOVED" }
                .groupBy { it.creatorId }

            fun resolveRelationship(targetId: Long): FollowRelationshipStatus {
                if (targetId == currentUserId) return FollowRelationshipStatus.SELF
                val iFollow = targetId in followingIds
                val theyFollow = targetId in followerIds
                return when {
                    iFollow && theyFollow -> FollowRelationshipStatus.MUTUAL_FRIENDS
                    iFollow -> FollowRelationshipStatus.FOLLOWING
                    theyFollow -> FollowRelationshipStatus.FOLLOWED_BY
                    else -> FollowRelationshipStatus.NOT_FOLLOWING
                }
            }

            fun toCreatorInfo(user: UserEntity, timestamp: Long): FollowedCreatorInfo {
                val creatorVideos = videosByCreator[user.id].orEmpty()
                return FollowedCreatorInfo(
                    creator = user,
                    followedAt = timestamp,
                    relationshipStatus = resolveRelationship(user.id),
                    videosCount = creatorVideos.size,
                    latestVideoHeadline = creatorVideos.firstOrNull()?.thumbnailHeadline?.ifBlank {
                        creatorVideos.firstOrNull()?.caption
                    }
                )
            }

            val followedCreatorsList = outgoingFollows.mapNotNull { edge ->
                val creator = usersById[edge.followingId] ?: return@mapNotNull null
                toCreatorInfo(creator, edge.createdAt)
            }

            val followerCreatorsList = incomingFollows.mapNotNull { edge ->
                val follower = usersById[edge.followerId] ?: return@mapNotNull null
                toCreatorInfo(follower, edge.createdAt)
            }

            val suggestedCreatorsList = allUsers
                .filter {
                    it.id != currentUserId &&
                        it.id !in followingIds &&
                        it.accountStatus == "ACTIVE"
                }
                .sortedByDescending { it.followersCount }
                .map { user -> toCreatorInfo(user, 0L) }

            return CreatorFollowUiState(
                currentUserId = currentUserId,
                followingUserIds = followingIds,
                followerUserIds = followerIds,
                followedCreators = followedCreatorsList,
                followerCreators = followerCreatorsList,
                suggestedCreators = suggestedCreatorsList,
                lastUpdatedCreatorId = lastUpdatedCreatorId
            )
        }
    }
}

/**
 * Stateful Follow/Unfollow UI state controller that manages follow relationships,
 * creator follower counts, and tracked creators in memory or in coordination with a repository.
 */
class FollowSystemStateHolder(
    initialCurrentUserId: Long = 1L,
    initialUsers: List<UserEntity> = emptyList(),
    initialFollows: List<FollowEntity> = emptyList(),
    initialVideos: List<VideoEntity> = emptyList()
) {
    private val _users = MutableStateFlow(initialUsers)
    val users: StateFlow<List<UserEntity>> = _users.asStateFlow()

    private val _follows = MutableStateFlow(initialFollows)
    val follows: StateFlow<List<FollowEntity>> = _follows.asStateFlow()

    private val _uiState = MutableStateFlow(
        CreatorFollowUiState.fromDomainData(
            currentUserId = initialCurrentUserId,
            allUsers = initialUsers,
            allFollows = initialFollows,
            allVideos = initialVideos
        )
    )
    val uiState: StateFlow<CreatorFollowUiState> = _uiState.asStateFlow()

    private var currentUserId: Long = initialCurrentUserId
    private var videos: List<VideoEntity> = initialVideos

    /**
     * Follows a creator if not already following, updating follower/following counts and UI state.
     * @return true if the creator is now followed, false if invalid (e.g. self-follow).
     */
    fun followCreator(targetCreatorId: Long, timestampMs: Long = System.currentTimeMillis()): Boolean {
        if (targetCreatorId == currentUserId) return false
        if (_uiState.value.isFollowing(targetCreatorId)) return true

        val newEdge = FollowEntity(
            id = (_follows.value.maxOfOrNull { it.id } ?: 0L) + 1L,
            followerId = currentUserId,
            followingId = targetCreatorId,
            createdAt = timestampMs
        )
        _follows.update { it + newEdge }
        _users.update { list ->
            list.map { user ->
                when (user.id) {
                    currentUserId -> user.copy(followingCount = user.followingCount + 1)
                    targetCreatorId -> user.copy(followersCount = user.followersCount + 1)
                    else -> user
                }
            }
        }
        rebuildState(lastUpdatedCreatorId = targetCreatorId)
        return true
    }

    /**
     * Unfollows a creator if currently following, updating follower/following counts and UI state.
     * @return false to indicate the creator is no longer followed.
     */
    fun unfollowCreator(targetCreatorId: Long): Boolean {
        if (!_uiState.value.isFollowing(targetCreatorId)) return false

        _follows.update { list ->
            list.filterNot { it.followerId == currentUserId && it.followingId == targetCreatorId }
        }
        _users.update { list ->
            list.map { user ->
                when (user.id) {
                    currentUserId -> user.copy(followingCount = (user.followingCount - 1).coerceAtLeast(0))
                    targetCreatorId -> user.copy(followersCount = (user.followersCount - 1).coerceAtLeast(0))
                    else -> user
                }
            }
        }
        rebuildState(lastUpdatedCreatorId = targetCreatorId)
        return false
    }

    /**
     * Toggles the follow state for [targetCreatorId].
     * @return true if now following [targetCreatorId], false if unfollowed.
     */
    fun toggleFollow(targetCreatorId: Long, timestampMs: Long = System.currentTimeMillis()): Boolean {
        if (targetCreatorId == currentUserId) return false
        return if (_uiState.value.isFollowing(targetCreatorId)) {
            unfollowCreator(targetCreatorId)
        } else {
            followCreator(targetCreatorId, timestampMs)
        }
    }

    fun setActiveUser(userId: Long) {
        currentUserId = userId
        rebuildState(lastUpdatedCreatorId = null)
    }

    private fun rebuildState(lastUpdatedCreatorId: Long?) {
        _uiState.value = CreatorFollowUiState.fromDomainData(
            currentUserId = currentUserId,
            allUsers = _users.value,
            allFollows = _follows.value,
            allVideos = videos,
            lastUpdatedCreatorId = lastUpdatedCreatorId
        )
    }
}
