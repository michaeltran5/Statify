package com.cs407.statify

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "LoginFragment"

class LoginFragment : Fragment() {
    private lateinit var loginButton: Button
    private lateinit var webView: WebView
    private val auth = Firebase.auth
    private var accessToken: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupWebView()
        setupLoginButton()
    }

    private fun initializeViews(view: View) {
        loginButton = view.findViewById(R.id.loginButton)
        webView = requireActivity().findViewById(R.id.webView)
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
        }
        WebView.setWebContentsDebuggingEnabled(true)
        webView.visibility = View.GONE
    }

    private fun setupLoginButton() {
        loginButton.setOnClickListener {
            startSpotifyAuth()
        }
    }

    private fun startSpotifyAuth() {
        val client = SpotifyWebViewClient { token ->
            accessToken = token
            webView.visibility = View.GONE
            Log.d(TAG, "Spotify Auth successful")

            requireActivity().getSharedPreferences("SPOTIFY", 0)
                .edit()
                .putString("access_token", token)
                .apply()

            lifecycleScope.launch {
                if (auth.currentUser == null) {
                    auth.signInAnonymously().await()
                }
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }
        }

        webView.webViewClient = client
        webView.visibility = View.VISIBLE
        webView.loadUrl(buildSpotifyAuthUrl())
    }

    private fun buildSpotifyAuthUrl(): String {
        return Uri.Builder()
            .scheme("https")
            .authority("accounts.spotify.com")
            .appendPath("authorize")
            .appendQueryParameter("client_id", "14674d2a9e9841f3b7a0b24a5aacd090")
            .appendQueryParameter("response_type", "token")
            .appendQueryParameter("redirect_uri", "statify://callback")
            .appendQueryParameter("scope", "user-read-private user-read-email user-top-read user-read-recently-played")
            .appendQueryParameter("show_dialog", "true")
            .build()
            .toString()
    }

    companion object {
        fun newInstance() = LoginFragment()
    }
}