package com.cs407.statify

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

/**
 * SpotifyAuth object - handles all authentication needed to make calls to Spotify API
 *
 */
object SpotifyAuth {
    private const val TAG = "SpotifyAuth"
    const val REQUEST_CODE = 1337
    private const val CLIENT_ID = "14674d2a9e9841f3b7a0b24a5aacd090"
    private const val REDIRECT_URI = "statify://callback"

    fun authenticate(activity: Activity) {
        Log.d(TAG, "Starting authentication")
        try {
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

            // Try using WebView first
            try {
                AuthorizationClient.openLoginActivity(activity, REQUEST_CODE, request)
                Log.d(TAG, "Opened auth in WebView")
            } catch (e: Exception) {
                // Fallback to browser if WebView fails
                Log.d(TAG, "WebView failed, falling back to browser", e)
                val authUrl = request.toUri()
                val browserIntent = Intent(Intent.ACTION_VIEW, authUrl)
                activity.startActivity(browserIntent)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start auth", e)
            throw e
        }
    }

    private fun AuthorizationRequest.toUri(): Uri {
        val uriBuilder = Uri.Builder()
            .scheme("https")
            .authority("accounts.spotify.com")
            .appendPath("authorize")
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "token")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", "user-read-private user-read-email user-top-read " +
                    "user-read-recently-played")
            .appendQueryParameter("show_dialog", "true")
            .appendQueryParameter("auth_type", "webview")  // Try to force WebView
            .appendQueryParameter("utm_source", "spotify-sdk")
            .appendQueryParameter("utm_medium", "android-sdk")
            .appendQueryParameter("utm_campaign", "android-sdk")

        return uriBuilder.build()
    }
}