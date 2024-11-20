package com.cs407.statify

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var webView: WebView
    private lateinit var bottomButtonLayout: LinearLayout
    private lateinit var homeButton: Button
    private lateinit var topTracksButton: Button
    private lateinit var friendsButton: Button
    private lateinit var profileButton: Button

    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.findNavController()

        // Initialize views
        webView = findViewById(R.id.webView)
        bottomButtonLayout = findViewById(R.id.bottomButtonLayout)
        homeButton = findViewById(R.id.homeButton)
        topTracksButton = findViewById(R.id.topTracksButton)
        friendsButton = findViewById(R.id.friendsButton)
        profileButton = findViewById(R.id.profileButton)

        setupNavigationButtons()
        setupNavigationVisibility()
    }

    private fun setupNavigationButtons() {
        homeButton.setOnClickListener {
            navController.navigate(R.id.homeFragment)
        }
        topTracksButton.setOnClickListener {
            if (auth.currentUser != null) {
                navController.navigate(R.id.topTracksFragment)
            } else {
                showLoginPrompt()
            }
        }
        friendsButton.setOnClickListener {
            if (auth.currentUser != null) {
                navController.navigate(R.id.friendsFragment)
            } else {
                showLoginPrompt()
            }
        }
        profileButton.setOnClickListener {
            if (auth.currentUser != null) {
                navController.navigate(R.id.profileFragment)
            } else {
                showLoginPrompt()
            }
        }
    }

    private fun showLoginPrompt() {
        navController.navigate(R.id.homeFragment)
        Toast.makeText(this, "Please log in to access this feature", Toast.LENGTH_SHORT).show()
    }

    private fun setupNavigationVisibility() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loadingFragment -> {
                    // Hide everything during loading animation
                    bottomButtonLayout.visibility = View.GONE
                    supportActionBar?.hide()
                }
                else -> {
                    // Show navigation for other fragments
                    bottomButtonLayout.visibility = View.VISIBLE
                    supportActionBar?.show()
                }
            }
        }
    }

    fun updateButtonsForLogout() {
        // Clear Spotify token and any cached data
        getSharedPreferences("SPOTIFY", 0).edit().clear().apply()

        // Ensure webView is hidden
        webView.visibility = View.GONE

        // Navigate to home
        navController.navigate(R.id.homeFragment)
    }

    fun showWebView() {
        webView.visibility = View.VISIBLE
    }

    fun hideWebView() {
        webView.visibility = View.GONE
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            webView.visibility == View.VISIBLE -> {
                webView.visibility = View.GONE
            }
            else -> {
                @Suppress("DEPRECATION")
                super.onBackPressed()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear sensitive data
        webView.clearCache(true)
        webView.clearHistory()
        getSharedPreferences("SPOTIFY", 0).edit().clear().apply()
    }
}