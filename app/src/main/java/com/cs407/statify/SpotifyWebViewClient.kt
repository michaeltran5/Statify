package com.cs407.statify

import android.net.Uri
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest

/**
 * Sets up the Spotify Web View that users see during login process
 *
 */
class SpotifyWebViewClient(private val onAuthComplete: (String) -> Unit) : WebViewClient() {

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        url?.let { urlString ->
            handleUrl(urlString)
        }
        return false
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        request?.url?.toString()?.let { urlString ->
            handleUrl(urlString)
        }
        return false
    }

    private fun handleUrl(urlString: String): Boolean {
        Log.d("SpotifyWebView", "URL loading: $urlString")
        if (urlString.startsWith("statify://callback")) {
            // Extract access token from URL fragment
            val uri = Uri.parse(urlString)
            val fragment = uri.fragment
            fragment?.let {
                val accessToken = extractAccessToken(it)
                if (accessToken != null) {
                    onAuthComplete(accessToken)
                    return true
                }
            }
        }
        return false
    }

    private fun extractAccessToken(fragment: String): String? {
        val params = fragment.split("&")
        for (param in params) {
            val parts = param.split("=")
            if (parts.size == 2 && parts[0] == "access_token") {
                return parts[1]
            }
        }
        return null
    }
}