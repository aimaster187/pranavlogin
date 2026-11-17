package com.example.data.remote

import com.google.firebase.firestore.FirebaseFirestoreException

object FirestoreErrorHandler {
    fun toUserMessage(error: Throwable, actionLabel: String = "operation"): String {
        return when (error) {
            is FirebaseFirestoreException -> when (error.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Access denied during $actionLabel. Verify you are signed in as the Root Owner."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Network unavailable during $actionLabel. Offline cache will sync on reconnect."
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "Session expired. Sign in again with Google."
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "Requested record was not found."
                FirebaseFirestoreException.Code.INVALID_ARGUMENT ->
                    "Invalid data submitted for $actionLabel."
                else -> error.localizedMessage ?: "Firestore error (${error.code.name})"
            }
            else -> error.localizedMessage ?: "Unexpected error during $actionLabel"
        }
    }
}
