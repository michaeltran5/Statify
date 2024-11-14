package com.cs407.statify

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var webView: WebView
    private lateinit var homeButton: Button
    private lateinit var topTracksButton: Button
    private lateinit var friendsButton: Button
    private lateinit var profileButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.findNavController()

        webView = findViewById(R.id.webView)

        // Initialize buttons
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
            navController.navigate(R.id.topTracksFragment)
        }
        friendsButton.setOnClickListener {
            navController.navigate(R.id.friendsFragment)
        }
        profileButton.setOnClickListener {
            navController.navigate(R.id.profileFragment)
        }
    }

    private fun setupNavigationVisibility() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loadingFragment -> {
                    hideNavigationButtons()
                    supportActionBar?.hide()  // Hide the action bar
                }
                else -> {
                    showNavigationButtons()
                    supportActionBar?.show()  // Show the action bar
                }
            }
        }
    }

    private fun hideNavigationButtons() {
        homeButton.visibility = View.GONE
        topTracksButton.visibility = View.GONE
        friendsButton.visibility = View.GONE
        profileButton.visibility = View.GONE
    }

    private fun showNavigationButtons() {
        homeButton.visibility = View.VISIBLE
        topTracksButton.visibility = View.VISIBLE
        friendsButton.visibility = View.VISIBLE
        profileButton.visibility = View.VISIBLE
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.visibility == View.VISIBLE) {
            webView.visibility = View.GONE
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}