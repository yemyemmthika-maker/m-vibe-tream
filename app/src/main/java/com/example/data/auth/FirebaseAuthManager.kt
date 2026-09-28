package com.example.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

data class AuthAccountResult(
    val uid: String,
    val email: String,
    val displayName: String,
    val isFirebaseBacked: Boolean
)

/**
 * Handles Firebase Authentication (Email/Password + Google Sign-In via Credential Manager)
 * with graceful fallback check when google-services.json is not yet configured in the environment.
 */
class FirebaseAuthManager(private val appContext: Context) {

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
            if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    val isFirebaseConfigured: Boolean
        get() = firebaseAuth != null

    val currentFirebaseUser: FirebaseUser?
        get() = try {
            firebaseAuth?.currentUser
        } catch (_: Exception) {
            null
        }

    suspend fun signInWithEmail(email: String, password: String): Result<AuthAccountResult> {
        val auth = firebaseAuth
            ?: return Result.failure(IllegalStateException("Firebase not initialized (missing google-services.json)"))

        return suspendCancellableCoroutine { cont ->
            auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener { task ->
                    if (!cont.isActive) return@addOnCompleteListener
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        cont.resume(
                            Result.success(
                                AuthAccountResult(
                                    uid = user?.uid.orEmpty(),
                                    email = user?.email ?: email.trim(),
                                    displayName = user?.displayName ?: email.substringBefore("@"),
                                    isFirebaseBacked = true
                                )
                            )
                        )
                    } else {
                        cont.resume(
                            Result.failure(task.exception ?: IllegalArgumentException("Firebase sign-in failed"))
                        )
                    }
                }
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<AuthAccountResult> {
        val auth = firebaseAuth
            ?: return Result.failure(IllegalStateException("Firebase not initialized (missing google-services.json)"))

        return suspendCancellableCoroutine { cont ->
            auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener { task ->
                    if (!cont.isActive) return@addOnCompleteListener
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        if (user != null && displayName.isNotBlank()) {
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName.trim())
                                .build()
                            user.updateProfile(profileUpdates)
                        }
                        cont.resume(
                            Result.success(
                                AuthAccountResult(
                                    uid = user?.uid.orEmpty(),
                                    email = user?.email ?: email.trim(),
                                    displayName = displayName.ifBlank { email.substringBefore("@") },
                                    isFirebaseBacked = true
                                )
                            )
                        )
                    } else {
                        cont.resume(
                            Result.failure(task.exception ?: IllegalArgumentException("Firebase sign-up failed"))
                        )
                    }
                }
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val auth = firebaseAuth
            ?: return Result.failure(IllegalStateException("Firebase not initialized"))

        return suspendCancellableCoroutine { cont ->
            auth.sendPasswordResetEmail(email.trim())
                .addOnCompleteListener { task ->
                    if (!cont.isActive) return@addOnCompleteListener
                    if (task.isSuccessful) {
                        cont.resume(Result.success(Unit))
                    } else {
                        cont.resume(
                            Result.failure(task.exception ?: IllegalArgumentException("Password reset failed"))
                        )
                    }
                }
        }
    }

    suspend fun signInWithGoogleCredentialManager(
        activityContext: Context,
        serverClientId: String
    ): Result<AuthAccountResult> {
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId.ifBlank { "vibestream-web-client-id.apps.googleusercontent.com" })
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")

                val auth = firebaseAuth
                if (auth != null) {
                    val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
                    suspendCancellableCoroutine { cont ->
                        auth.signInWithCredential(firebaseCred)
                            .addOnCompleteListener { task ->
                                if (!cont.isActive) return@addOnCompleteListener
                                if (task.isSuccessful) {
                                    val user = task.result?.user
                                    cont.resume(
                                        Result.success(
                                            AuthAccountResult(
                                                uid = user?.uid ?: email,
                                                email = user?.email ?: email,
                                                displayName = user?.displayName ?: displayName,
                                                isFirebaseBacked = true
                                            )
                                        )
                                    )
                                } else {
                                    cont.resume(
                                        Result.failure(
                                            task.exception ?: IllegalStateException("Firebase Google Auth failed")
                                        )
                                    )
                                }
                            }
                    }
                } else {
                    Result.success(
                        AuthAccountResult(
                            uid = email,
                            email = email,
                            displayName = displayName,
                            isFirebaseBacked = false
                        )
                    )
                }
            } else {
                Result.failure(IllegalStateException("Unsupported credential type returned"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {
        }
    }
}
