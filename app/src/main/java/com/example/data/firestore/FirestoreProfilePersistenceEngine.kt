package com.example.data.firestore

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.example.data.local.UserEntity
import com.example.data.local.VideoEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents an uploaded video entry stored inside a user's Firestore profile document
 * (`user_profiles/{userId}`) and subcollection (`user_profiles/{userId}/uploaded_videos/{videoId}`).
 */
data class FirestoreUploadedVideoItem(
    val id: Long,
    val creatorId: Long,
    val creatorHandle: String,
    val caption: String,
    val hashtags: String,
    val thumbnailHeadline: String,
    val thumbnailUri: String?,
    val thumbnailThemeIndex: Int,
    val videoUri: String,
    val cdnStreamUrl: String,
    val durationSec: Int,
    val viewsCount: Int,
    val likesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int,
    val createdAt: Long
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "creatorId" to creatorId,
        "creatorHandle" to creatorHandle,
        "caption" to caption,
        "hashtags" to hashtags,
        "thumbnailHeadline" to thumbnailHeadline,
        "thumbnailUri" to (thumbnailUri ?: ""),
        "thumbnailThemeIndex" to thumbnailThemeIndex,
        "videoUri" to videoUri,
        "cdnStreamUrl" to cdnStreamUrl,
        "durationSec" to durationSec,
        "viewsCount" to viewsCount,
        "likesCount" to likesCount,
        "commentsCount" to commentsCount,
        "sharesCount" to sharesCount,
        "createdAt" to createdAt
    )

    fun toVideoEntity(
        creatorDisplayName: String,
        creatorAvatarHex: String,
        creatorVerified: Boolean
    ): VideoEntity {
        val cleanUsername = creatorHandle.trim().removePrefix("@")
        return VideoEntity(
            id = id,
            creatorId = creatorId,
            creatorUsername = cleanUsername,
            creatorDisplayName = creatorDisplayName,
            creatorAvatarHex = creatorAvatarHex,
            creatorVerified = creatorVerified,
            caption = caption,
            hashtags = hashtags,
            soundId = 1L,
            soundTitle = "Original Studio Sound",
            soundArtist = creatorDisplayName,
            videoUri = videoUri,
            cdnStreamUrl = cdnStreamUrl,
            thumbnailUri = thumbnailUri?.takeIf { it.isNotBlank() },
            thumbnailThemeIndex = thumbnailThemeIndex,
            thumbnailHeadline = thumbnailHeadline,
            durationSec = durationSec,
            likesCount = likesCount,
            commentsCount = commentsCount,
            sharesCount = sharesCount,
            viewsCount = viewsCount,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromVideoEntity(video: VideoEntity): FirestoreUploadedVideoItem {
            val normalizedHandle = if (video.creatorUsername.startsWith("@")) {
                video.creatorUsername
            } else {
                "@${video.creatorUsername}"
            }
            return FirestoreUploadedVideoItem(
                id = video.id,
                creatorId = video.creatorId,
                creatorHandle = normalizedHandle,
                caption = video.caption,
                hashtags = video.hashtags,
                thumbnailHeadline = video.thumbnailHeadline,
                thumbnailUri = video.thumbnailUri,
                thumbnailThemeIndex = video.thumbnailThemeIndex,
                videoUri = video.videoUri,
                cdnStreamUrl = video.cdnStreamUrl,
                durationSec = video.durationSec,
                viewsCount = video.viewsCount,
                likesCount = video.likesCount,
                commentsCount = video.commentsCount,
                sharesCount = video.sharesCount,
                createdAt = video.createdAt
            )
        }

        fun fromFirestoreMap(map: Map<String, Any?>): FirestoreUploadedVideoItem {
            return FirestoreUploadedVideoItem(
                id = (map["id"] as? Number)?.toLong() ?: 0L,
                creatorId = (map["creatorId"] as? Number)?.toLong() ?: 0L,
                creatorHandle = (map["creatorHandle"] as? String).orEmpty().let {
                    if (it.startsWith("@")) it else "@$it"
                },
                caption = (map["caption"] as? String).orEmpty(),
                hashtags = (map["hashtags"] as? String).orEmpty(),
                thumbnailHeadline = (map["thumbnailHeadline"] as? String).orEmpty(),
                thumbnailUri = (map["thumbnailUri"] as? String)?.takeIf { it.isNotBlank() },
                thumbnailThemeIndex = (map["thumbnailThemeIndex"] as? Number)?.toInt() ?: 0,
                videoUri = (map["videoUri"] as? String).orEmpty(),
                cdnStreamUrl = (map["cdnStreamUrl"] as? String).orEmpty(),
                durationSec = (map["durationSec"] as? Number)?.toInt() ?: 15,
                viewsCount = (map["viewsCount"] as? Number)?.toInt() ?: 0,
                likesCount = (map["likesCount"] as? Number)?.toInt() ?: 0,
                commentsCount = (map["commentsCount"] as? Number)?.toInt() ?: 0,
                sharesCount = (map["sharesCount"] as? Number)?.toInt() ?: 0,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}

/**
 * Firestore document representation of a User Profile stored in collection `user_profiles/{userId}`.
 * Persists the user's avatar, handle, bio, profile metrics, and their grid of uploaded videos.
 */
data class FirestoreUserProfileDocument(
    val userId: Long,
    val username: String,
    val handle: String, // e.g., "@nova.pulse"
    val displayName: String,
    val email: String = "",
    val bio: String,
    val avatarColorHex: String,
    val avatarUri: String? = null,
    val isVerified: Boolean = false,
    val isAdmin: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val totalLikesReceived: Int = 0,
    val uploadedVideos: List<FirestoreUploadedVideoItem> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
    val documentPath: String = "user_profiles/$userId",
    val persistenceBackend: String = "Firebase Firestore"
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "username" to username,
        "handle" to handle,
        "displayName" to displayName,
        "email" to email,
        "bio" to bio,
        "avatarColorHex" to avatarColorHex,
        "avatarUri" to (avatarUri ?: ""),
        "isVerified" to isVerified,
        "isAdmin" to isAdmin,
        "followersCount" to followersCount,
        "followingCount" to followingCount,
        "totalLikesReceived" to totalLikesReceived,
        "uploadedVideosCount" to uploadedVideos.size,
        "uploadedVideos" to uploadedVideos.map { it.toFirestoreMap() },
        "updatedAt" to updatedAt,
        "documentPath" to documentPath,
        "persistenceBackend" to persistenceBackend
    )

    fun toUserEntity(existingUser: UserEntity? = null): UserEntity {
        val cleanUsername = username.trim().removePrefix("@")
        return existingUser?.copy(
            username = cleanUsername,
            displayName = displayName,
            bio = bio,
            avatarColorHex = avatarColorHex,
            avatarUri = avatarUri?.takeIf { it.isNotBlank() },
            followersCount = followersCount,
            followingCount = followingCount,
            totalLikesReceived = totalLikesReceived,
            isVerified = isVerified,
            isAdmin = isAdmin
        ) ?: UserEntity(
            id = userId,
            username = cleanUsername,
            displayName = displayName,
            email = email.ifBlank { "$cleanUsername@vibestream.social" },
            passwordHash = "firestore_synced",
            bio = bio,
            avatarColorHex = avatarColorHex,
            avatarUri = avatarUri?.takeIf { it.isNotBlank() },
            followersCount = followersCount,
            followingCount = followingCount,
            totalLikesReceived = totalLikesReceived,
            isVerified = isVerified,
            isAdmin = isAdmin
        )
    }

    fun toVideoEntities(): List<VideoEntity> = uploadedVideos.map { item ->
        item.toVideoEntity(
            creatorDisplayName = displayName,
            creatorAvatarHex = avatarColorHex,
            creatorVerified = isVerified
        )
    }

    companion object {
        fun fromDomain(
            user: UserEntity,
            uploadedVideos: List<VideoEntity>,
            updatedAt: Long = System.currentTimeMillis()
        ): FirestoreUserProfileDocument {
            val cleanUsername = user.username.trim().removePrefix("@")
            return FirestoreUserProfileDocument(
                userId = user.id,
                username = cleanUsername,
                handle = "@$cleanUsername",
                displayName = user.displayName,
                email = user.email,
                bio = user.bio,
                avatarColorHex = user.avatarColorHex,
                avatarUri = user.avatarUri,
                isVerified = user.isVerified,
                isAdmin = user.isAdmin,
                followersCount = user.followersCount,
                followingCount = user.followingCount,
                totalLikesReceived = user.totalLikesReceived,
                uploadedVideos = uploadedVideos.map { FirestoreUploadedVideoItem.fromVideoEntity(it) },
                updatedAt = updatedAt,
                documentPath = "user_profiles/${user.id}"
            )
        }

        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreMap(map: Map<String, Any?>): FirestoreUserProfileDocument {
            val uid = (map["userId"] as? Number)?.toLong() ?: 1L
            val rawUsername = (map["username"] as? String).orEmpty().removePrefix("@")
            val rawHandle = (map["handle"] as? String).orEmpty().ifBlank { "@$rawUsername" }
            val rawVideos = (map["uploadedVideos"] as? List<*>)?.mapNotNull { entry ->
                (entry as? Map<String, Any?>)?.let { FirestoreUploadedVideoItem.fromFirestoreMap(it) }
            }.orEmpty()

            return FirestoreUserProfileDocument(
                userId = uid,
                username = rawUsername.ifBlank { rawHandle.removePrefix("@") },
                handle = if (rawHandle.startsWith("@")) rawHandle else "@$rawHandle",
                displayName = (map["displayName"] as? String).orEmpty(),
                email = (map["email"] as? String).orEmpty(),
                bio = (map["bio"] as? String).orEmpty(),
                avatarColorHex = (map["avatarColorHex"] as? String)?.ifBlank { "#FF3366" } ?: "#FF3366",
                avatarUri = (map["avatarUri"] as? String)?.takeIf { it.isNotBlank() },
                isVerified = (map["isVerified"] as? Boolean) ?: false,
                isAdmin = (map["isAdmin"] as? Boolean) ?: false,
                followersCount = (map["followersCount"] as? Number)?.toInt() ?: 0,
                followingCount = (map["followingCount"] as? Number)?.toInt() ?: 0,
                totalLikesReceived = (map["totalLikesReceived"] as? Number)?.toInt() ?: 0,
                uploadedVideos = rawVideos,
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                documentPath = (map["documentPath"] as? String) ?: "user_profiles/$uid",
                persistenceBackend = (map["persistenceBackend"] as? String) ?: "Firebase Firestore"
            )
        }
    }
}

/**
 * Engine that persists and synchronizes User Profile documents (avatar, handle, bio, and uploaded
 * videos grid) using Firebase Cloud Firestore (`user_profiles/{userId}` and subcollection
 * `uploaded_videos`), paired with deterministic local Firestore document serialization so profile
 * persistence works seamlessly both online and offline.
 */
class FirestoreProfilePersistenceEngine(private val appContext: Context? = null) {

    companion object {
        const val COLLECTION_USER_PROFILES = "user_profiles"
        const val SUBCOLLECTION_UPLOADED_VIDEOS = "uploaded_videos"
        private const val PREFS_NAME = "vibestream_firestore_profile_store"
        private const val KEY_DOC_PREFIX = "firestore_doc_user_"
    }

    private val firestore: FirebaseFirestore? by lazy {
        val ctx = appContext ?: return@lazy null
        if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
            return@lazy null
        }
        try {
            if (FirebaseApp.getApps(ctx).isEmpty()) {
                FirebaseApp.initializeApp(ctx)
            }
            if (FirebaseApp.getApps(ctx).isNotEmpty()) {
                val instance = FirebaseFirestore.getInstance()
                try {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build()
                    instance.firestoreSettings = settings
                } catch (_: Throwable) {
                    // Settings may already be locked if Firestore was previously accessed
                }
                instance
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private val prefs: SharedPreferences? by lazy {
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _profilesFlow = MutableStateFlow<Map<Long, FirestoreUserProfileDocument>>(emptyMap())
    val profilesFlow: StateFlow<Map<Long, FirestoreUserProfileDocument>> = _profilesFlow.asStateFlow()

    private val activeListeners = mutableMapOf<Long, ListenerRegistration>()

    val isCloudFirestoreInitialized: Boolean
        get() = firestore != null

    /**
     * Persists a user's profile (avatar, handle, bio, stats, and uploaded videos grid) to
     * Firebase Firestore (`user_profiles/{userId}`) and mirrors the serialized Firestore document.
     */
    fun persistUserProfile(
        user: UserEntity,
        uploadedVideos: List<VideoEntity>
    ): FirestoreUserProfileDocument {
        val doc = FirestoreUserProfileDocument.fromDomain(
            user = user,
            uploadedVideos = uploadedVideos,
            updatedAt = System.currentTimeMillis()
        )
        persistDocumentInternal(doc)
        return doc
    }

    /**
     * Updates a user's avatar, handle, display name, and bio directly in Firebase Firestore
     * while preserving their uploaded videos grid.
     */
    fun updateProfileDetailsInFirestore(
        userId: Long,
        displayName: String,
        handleOrUsername: String,
        bio: String,
        avatarColorHex: String,
        avatarUri: String?,
        existingUser: UserEntity? = null,
        uploadedVideos: List<VideoEntity> = emptyList()
    ): FirestoreUserProfileDocument {
        val cleanUsername = handleOrUsername.trim().lowercase().removePrefix("@").replace(" ", ".")
        val cleanHandle = "@$cleanUsername"
        val currentDoc = getCachedOrPersistedProfile(userId)

        val updatedVideos = if (uploadedVideos.isNotEmpty()) {
            uploadedVideos.map {
                FirestoreUploadedVideoItem.fromVideoEntity(
                    it.copy(
                        creatorUsername = cleanUsername,
                        creatorDisplayName = displayName.trim().ifBlank { it.creatorDisplayName },
                        creatorAvatarHex = avatarColorHex
                    )
                )
            }
        } else {
            currentDoc?.uploadedVideos?.map {
                it.copy(creatorHandle = cleanHandle)
            }.orEmpty()
        }

        val doc = FirestoreUserProfileDocument(
            userId = userId,
            username = cleanUsername,
            handle = cleanHandle,
            displayName = displayName.trim().ifBlank { currentDoc?.displayName ?: cleanUsername },
            email = existingUser?.email ?: currentDoc?.email.orEmpty(),
            bio = bio.trim(),
            avatarColorHex = avatarColorHex,
            avatarUri = avatarUri,
            isVerified = existingUser?.isVerified ?: currentDoc?.isVerified ?: false,
            isAdmin = existingUser?.isAdmin ?: currentDoc?.isAdmin ?: false,
            followersCount = existingUser?.followersCount ?: currentDoc?.followersCount ?: 0,
            followingCount = existingUser?.followingCount ?: currentDoc?.followingCount ?: 0,
            totalLikesReceived = existingUser?.totalLikesReceived ?: currentDoc?.totalLikesReceived ?: 0,
            uploadedVideos = updatedVideos,
            updatedAt = System.currentTimeMillis(),
            documentPath = "$COLLECTION_USER_PROFILES/$userId"
        )

        persistDocumentInternal(doc)
        return doc
    }

    /**
     * Fetches the persisted Firestore profile document (`user_profiles/{userId}`) from Cloud Firestore
     * or the local Firestore document store.
     */
    suspend fun fetchUserProfileDocument(
        userId: Long,
        fallbackUser: UserEntity? = null,
        fallbackVideos: List<VideoEntity> = emptyList()
    ): FirestoreUserProfileDocument? {
        val db = firestore
        if (db != null) {
            val remoteDoc = try {
                withTimeoutOrNull(1200L) {
                    suspendCancellableCoroutine<FirestoreUserProfileDocument?> { cont ->
                        db.collection(COLLECTION_USER_PROFILES)
                            .document(userId.toString())
                            .get()
                            .addOnSuccessListener { snapshot ->
                                if (!cont.isActive) return@addOnSuccessListener
                                val data = snapshot?.data
                                if (data != null && data.isNotEmpty()) {
                                    cont.resume(FirestoreUserProfileDocument.fromFirestoreMap(data))
                                } else {
                                    cont.resume(null)
                                }
                            }
                            .addOnFailureListener {
                                if (cont.isActive) cont.resume(null)
                            }
                    }
                }
            } catch (_: Throwable) {
                null
            }

            if (remoteDoc != null) {
                saveToLocalMirror(remoteDoc)
                _profilesFlow.update { it + (userId to remoteDoc) }
                return remoteDoc
            }
        }

        val localDoc = getCachedOrPersistedProfile(userId)
        if (localDoc != null) {
            return localDoc
        }

        if (fallbackUser != null) {
            return persistUserProfile(fallbackUser, fallbackVideos)
        }
        return null
    }

    /**
     * Attaches a real-time Firestore snapshot listener to `user_profiles/{userId}` when Cloud
     * Firestore is initialized, keeping [profilesFlow] updated automatically.
     */
    fun startRealtimeProfileListener(userId: Long) {
        val db = firestore ?: return
        if (activeListeners.containsKey(userId)) return

        try {
            val registration = db.collection(COLLECTION_USER_PROFILES)
                .document(userId.toString())
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val data = snapshot.data ?: return@addSnapshotListener
                    val parsed = FirestoreUserProfileDocument.fromFirestoreMap(data)
                    saveToLocalMirror(parsed)
                    _profilesFlow.update { it + (userId to parsed) }
                }
            activeListeners[userId] = registration
        } catch (_: Throwable) {
            // Ignore listener registration errors if Firebase project isn't connected
        }
    }

    /**
     * Synchronously reads the latest cached or persisted Firestore profile document for [userId].
     */
    fun getCachedOrPersistedProfile(userId: Long): FirestoreUserProfileDocument? {
        _profilesFlow.value[userId]?.let { return it }
        val rawJson = prefs?.getString("$KEY_DOC_PREFIX$userId", null) ?: return null
        return try {
            val parsed = deserializeFromJson(rawJson)
            _profilesFlow.update { it + (userId to parsed) }
            parsed
        } catch (_: Throwable) {
            null
        }
    }

    private fun persistDocumentInternal(doc: FirestoreUserProfileDocument) {
        // 1. Update in-memory reactive state flow & local Firestore document mirror immediately
        saveToLocalMirror(doc)
        _profilesFlow.update { it + (doc.userId to doc) }

        // 2. Write to Firebase Cloud Firestore collection `user_profiles/{userId}`
        val db = firestore ?: return
        val docRef = db.collection(COLLECTION_USER_PROFILES).document(doc.userId.toString())
        try {
            docRef.set(doc.toFirestoreMap(), SetOptions.merge())
            // Also persist each uploaded video in the user's subcollection `uploaded_videos`
            val videosCollection = docRef.collection(SUBCOLLECTION_UPLOADED_VIDEOS)
            doc.uploadedVideos.forEach { videoItem ->
                videosCollection.document(videoItem.id.toString())
                    .set(videoItem.toFirestoreMap(), SetOptions.merge())
            }
        } catch (_: Throwable) {
            // Firestore automatically queues offline writes when initialized; ignore if unconfigured
        }
    }

    private fun saveToLocalMirror(doc: FirestoreUserProfileDocument) {
        val serialized = serializeToJson(doc)
        prefs?.edit()?.putString("$KEY_DOC_PREFIX${doc.userId}", serialized)?.apply()
    }

    internal fun serializeToJson(doc: FirestoreUserProfileDocument): String {
        val root = JSONObject()
        val map = doc.toFirestoreMap()
        root.put("userId", map["userId"])
        root.put("username", map["username"])
        root.put("handle", map["handle"])
        root.put("displayName", map["displayName"])
        root.put("email", map["email"])
        root.put("bio", map["bio"])
        root.put("avatarColorHex", map["avatarColorHex"])
        root.put("avatarUri", map["avatarUri"] ?: "")
        root.put("isVerified", map["isVerified"])
        root.put("isAdmin", map["isAdmin"])
        root.put("followersCount", map["followersCount"])
        root.put("followingCount", map["followingCount"])
        root.put("totalLikesReceived", map["totalLikesReceived"])
        root.put("updatedAt", map["updatedAt"])
        root.put("documentPath", map["documentPath"])
        root.put("persistenceBackend", map["persistenceBackend"])

        val videosArray = JSONArray()
        doc.uploadedVideos.forEach { video ->
            val vObj = JSONObject()
            vObj.put("id", video.id)
            vObj.put("creatorId", video.creatorId)
            vObj.put("creatorHandle", video.creatorHandle)
            vObj.put("caption", video.caption)
            vObj.put("hashtags", video.hashtags)
            vObj.put("thumbnailHeadline", video.thumbnailHeadline)
            vObj.put("thumbnailUri", video.thumbnailUri ?: "")
            vObj.put("thumbnailThemeIndex", video.thumbnailThemeIndex)
            vObj.put("videoUri", video.videoUri)
            vObj.put("cdnStreamUrl", video.cdnStreamUrl)
            vObj.put("durationSec", video.durationSec)
            vObj.put("viewsCount", video.viewsCount)
            vObj.put("likesCount", video.likesCount)
            vObj.put("commentsCount", video.commentsCount)
            vObj.put("sharesCount", video.sharesCount)
            vObj.put("createdAt", video.createdAt)
            videosArray.put(vObj)
        }
        root.put("uploadedVideos", videosArray)
        return root.toString()
    }

    internal fun deserializeFromJson(jsonString: String): FirestoreUserProfileDocument {
        val root = JSONObject(jsonString)
        val videosArray = root.optJSONArray("uploadedVideos") ?: JSONArray()
        val videoMaps = mutableListOf<Map<String, Any?>>()
        for (i in 0 until videosArray.length()) {
            val vObj = videosArray.optJSONObject(i) ?: continue
            videoMaps.add(
                mapOf(
                    "id" to vObj.optLong("id", 0L),
                    "creatorId" to vObj.optLong("creatorId", 0L),
                    "creatorHandle" to vObj.optString("creatorHandle", ""),
                    "caption" to vObj.optString("caption", ""),
                    "hashtags" to vObj.optString("hashtags", ""),
                    "thumbnailHeadline" to vObj.optString("thumbnailHeadline", ""),
                    "thumbnailUri" to vObj.optString("thumbnailUri", ""),
                    "thumbnailThemeIndex" to vObj.optInt("thumbnailThemeIndex", 0),
                    "videoUri" to vObj.optString("videoUri", ""),
                    "cdnStreamUrl" to vObj.optString("cdnStreamUrl", ""),
                    "durationSec" to vObj.optInt("durationSec", 15),
                    "viewsCount" to vObj.optInt("viewsCount", 0),
                    "likesCount" to vObj.optInt("likesCount", 0),
                    "commentsCount" to vObj.optInt("commentsCount", 0),
                    "sharesCount" to vObj.optInt("sharesCount", 0),
                    "createdAt" to vObj.optLong("createdAt", 0L)
                )
            )
        }

        val map = mapOf<String, Any?>(
            "userId" to root.optLong("userId", 1L),
            "username" to root.optString("username", ""),
            "handle" to root.optString("handle", ""),
            "displayName" to root.optString("displayName", ""),
            "email" to root.optString("email", ""),
            "bio" to root.optString("bio", ""),
            "avatarColorHex" to root.optString("avatarColorHex", "#FF3366"),
            "avatarUri" to root.optString("avatarUri", ""),
            "isVerified" to root.optBoolean("isVerified", false),
            "isAdmin" to root.optBoolean("isAdmin", false),
            "followersCount" to root.optInt("followersCount", 0),
            "followingCount" to root.optInt("followingCount", 0),
            "totalLikesReceived" to root.optInt("totalLikesReceived", 0),
            "uploadedVideos" to videoMaps,
            "updatedAt" to root.optLong("updatedAt", 0L),
            "documentPath" to root.optString("documentPath", "user_profiles/1"),
            "persistenceBackend" to root.optString("persistenceBackend", "Firebase Firestore")
        )
        return FirestoreUserProfileDocument.fromFirestoreMap(map)
    }
}
