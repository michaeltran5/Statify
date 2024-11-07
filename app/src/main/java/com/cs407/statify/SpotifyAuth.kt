package com.cs407.statify

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

object SpotifyAuth {
    private const val TAG = "SpotifyAuth"
    const val REQUEST_CODE = 1337
    private const val CLIENT_ID = "14674d2a9e9841f3b7a0b24a5aacd090" // Replace with your actual client ID
    private const val REDIRECT_URI = "statify://callback"

    fun getAuthIntent(activity: Activity): Intent {
        Log.d(TAG, "Building auth request with CLIENT_ID length: ${CLIENT_ID.length}")
        try {
            val builder = AuthorizationRequest.Builder(
                CLIENT_ID,
                AuthorizationResponse.Type.TOKEN,
                REDIRECT_URI
            ).setShowDialog(true) // Force showing the auth dialog
                .setScopes(arrayOf(
                    "user-read-private",
                    "user-read-email",
                    "user-top-read",
                    "user-read-recently-played"
                ))

            val request = builder.build()
            Log.d(TAG, "Auth request built successfully")
            return AuthorizationClient.createLoginActivityIntent(activity, request)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating auth intent", e)
            throw e
        }
    }
}