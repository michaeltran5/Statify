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
    private lateinit var webView: WebView // Keep WebView in MainActivity as it's shared

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.findNavController()

        webView = findViewById(R.id.webView)
        setupNavigationButtons()
    }

    private fun setupNavigationButtons() {
        findViewById<Button>(R.id.homeButton)?.setOnClickListener {
            navController.navigate(R.id.homeFragment)
        }
        findViewById<Button>(R.id.topTracksButton)?.setOnClickListener {
            navController.navigate(R.id.topTracksFragment)
        }
        findViewById<Button>(R.id.friendsButton)?.setOnClickListener {
            navController.navigate(R.id.friendsFragment)
        }
        findViewById<Button>(R.id.profileButton)?.setOnClickListener {
            navController.navigate(R.id.profileFragment)
        }
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