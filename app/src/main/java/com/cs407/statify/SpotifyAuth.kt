package com.cs407.statify

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

object SpotifyAuth {
    private const val TAG = "SpotifyAuth"
    const val REQUEST_CODE = 1337
    private const val CLIENT_ID = "14674d2a9e9841f3b7a0b24a5aacd090"
    private const val REDIRECT_URI = "statify://callback"

    fun authenticate(activity: Activity) {
        Log.d(TAG, "Starting authentication")
        try {
            // First attempt: Try using Chrome Custom Tabs or external browser
            val builder = AuthorizationRequest.Builder(
                CLIENT_ID,
                AuthorizationResponse.Type.TOKEN,
                REDIRECT_URI
            )

            builder.setScopes(arrayOf(
                "user-read-private",
                "user-read-email",
                "user-top-read",
                "user-read-recently-played"
            ))
                .setShowDialog(true)

            val request = builder.build()

            // Try opening in browser first
            val authUrl = request.toUri()
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, authUrl)
                activity.startActivity(browserIntent)
                Log.d(TAG, "Opened auth in browser")
            } catch (e: Exception) {
                // Fallback to WebView if browser fails
                Log.d(TAG, "Browser failed, falling back to WebView")
                AuthorizationClient.openLoginActivity(activity, REQUEST_CODE, request)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start auth", e)
            throw e
        }
    }

    // Helper function to convert AuthorizationRequest to Uri
    private fun AuthorizationRequest.toUri(): Uri {
        val uriBuilder = Uri.Builder()
            .scheme("https")
            .authority("accounts.spotify.com")
            .appendPath("authorize")
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "token")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", "user-read-private user-read-email user-top-read user-read-recently-played")
            .appendQueryParameter("show_dialog", "true")

        return uriBuilder.build()
    }
}