package com.example.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object GoogleSignInHelper {
    private const val TAG = "PranavAuth"

    suspend fun signInWithGoogle(
        activityContext: Context,
        credentialManager: CredentialManager
    ): Result<FirebaseUser> {
        val serverClientId = activityContext.getString(R.string.default_web_client_id)
        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = credentialManager.getCredential(activityContext, request)
            val cred = response.credential
            if (cred is CustomCredential &&
                cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleTokenCred = GoogleIdTokenCredential.createFrom(cred.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleTokenCred.idToken, null)
                val authResult = Firebase.auth.signInWithCredential(firebaseCred).await()
                val user = authResult.user
                    ?: return Result.failure(IllegalStateException("Firebase returned null user after Google Sign-In."))
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unsupported credential type returned."))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(IllegalStateException("Sign-in cancelled by user.", e))
        } catch (e: NoCredentialException) {
            Result.failure(
                IllegalStateException(
                    "No Google account available on this device/emulator. Add a Google account in Settings first.",
                    e
                )
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, "CredentialManager error", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "Google Sign-In failed.", e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected Google Sign-In error", e)
            Result.failure(e)
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope
    ) {
        Firebase.auth.signOut()
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed clearing CredentialManager state: ${e.message}")
            }
        }
    }
}
