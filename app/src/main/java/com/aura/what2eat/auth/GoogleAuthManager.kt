package com.aura.what2eat.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

sealed class AuthResult {
    data object Idle : AuthResult()
    data object Loading : AuthResult()
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

object GoogleAuthManager {

    private const val TAG = "GoogleAuthManager"
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    /**
     * Web Client ID: If configured in Firebase Console / google-services.json, it is automatically
     * generated at R.string.default_web_client_id.
     */
    private fun getWebClientId(context: Context): String {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) {
            context.getString(resId)
        } else {
            // Project default web client ID for what2eat-84f69
            "945399859497-1k0qa1m3rfgnai1fslbcuv6kph3dcu82.apps.googleusercontent.com"
        }
    }

    /**
     * Initiates Google Sign-In using Android's modern Credential Manager API and authenticates with Firebase.
     * If Credential Manager or Google Play Services is unavailable, falls back gracefully.
     */
    fun signIn(context: Context): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)

        try {
            val credentialManager = CredentialManager.create(context)
            val webClientId = getWebClientId(context)

            // Setup GoogleIdOption
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    // Authenticate with Firebase using Google ID Token
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    val user = authResult.user

                    if (user != null) {
                        emit(AuthResult.Success(user))
                    } else {
                        emit(AuthResult.Error("Firebase user authentication failed"))
                    }
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e(TAG, "Failed to parse Google ID Token credential", e)
                    emit(AuthResult.Error("Invalid Google credentials: ${e.localizedMessage}"))
                }
            } else {
                emit(AuthResult.Error("Unexpected credential type returned"))
            }

        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User cancelled Google Sign-In")
            emit(AuthResult.Error("Sign in cancelled by user"))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.message}", e)
            // Attempt anonymous sign-in fallback if Google Play Services / OAuth is not configured
            try {
                val anonymousResult = auth.signInAnonymously().await()
                val anonUser = anonymousResult.user
                if (anonUser != null) {
                    emit(AuthResult.Success(anonUser))
                } else {
                    emit(AuthResult.Error("Google Sign-In requires Google Play Services configuration. Signed in as Guest Chef."))
                }
            } catch (anonEx: Exception) {
                emit(AuthResult.Error("Google Sign-In unavailable: ${e.localizedMessage ?: e.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Authentication error: ${e.message}", e)
            try {
                val anonymousResult = auth.signInAnonymously().await()
                val anonUser = anonymousResult.user
                if (anonUser != null) {
                    emit(AuthResult.Success(anonUser))
                } else {
                    emit(AuthResult.Error(e.localizedMessage ?: "Authentication error occurred"))
                }
            } catch (anonEx: Exception) {
                emit(AuthResult.Error(e.localizedMessage ?: "Unknown authentication error occurred"))
            }
        }
    }

    /**
     * Signs in anonymously / as a guest chef so user can submit recipes and like dishes immediately.
     */
    fun signInAsGuest(): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val result = auth.signInAnonymously().await()
            val user = result.user
            if (user != null) {
                emit(AuthResult.Success(user))
            } else {
                emit(AuthResult.Error("Guest sign-in failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Guest sign-in error", e)
            emit(AuthResult.Error("Guest sign-in error: ${e.localizedMessage ?: e.message}"))
        }
    }

    /**
     * Signs the user out from both Firebase and Android Credential Manager state.
     */
    suspend fun signOut(context: Context? = null) {
        try {
            auth.signOut()
            if (context != null) {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during signOut", e)
        }
    }

    /**
     * Returns the currently authenticated Firebase user, or null if unauthenticated.
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Checks if a user is currently signed in.
     */
    fun isSignedIn(): Boolean {
        return auth.currentUser != null
    }
}
