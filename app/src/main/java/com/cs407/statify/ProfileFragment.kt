package com.cs407.statify

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ProfileFragment"

class ProfileFragment : Fragment() {
    // UI Components
    private lateinit var profileDetailsText: TextView
    private lateinit var logoutButton: Button
    private lateinit var progressBar: ProgressBar

    // Firebase instances
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    // Coroutine scope for async operations
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        profileDetailsText = view.findViewById(R.id.profileDetailsText)
        logoutButton = view.findViewById(R.id.logoutButton)
        progressBar = view.findViewById(R.id.progressBar)

        setupLogoutButton()
        loadUserProfile()
    }

    private fun setupLogoutButton() {
        logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun logout() {
        coroutineScope.launch {
            try {
                showLoading(true)

                // Clear Spotify token
                requireActivity().getSharedPreferences("SPOTIFY", 0)
                    .edit()
                    .remove("access_token")
                    .apply()

                // Sign out from Firebase
                withContext(Dispatchers.IO) {
                    auth.signOut()
                }

                // Clear UI
                profileDetailsText.text = ""

                // Update UI in MainActivity if needed
                (activity as? MainActivity)?.updateButtonsForLogout()

                showLoading(false)

                // Show success message
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

                // Navigate to home
                findNavController().navigate(R.id.homeFragment)

            } catch (e: Exception) {
                Log.e(TAG, "Error during logout", e)
                showLoading(false)
                Toast.makeText(
                    requireContext(),
                    "Error logging out: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            showLoading(true)

            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    showLoading(false)
                    if (document != null && document.exists()) {
                        val userData = document.data
                        displayUserProfile(userData)
                    } else {
                        Log.d(TAG, "No user data found")
                        profileDetailsText.text = "No profile data available"
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Log.e(TAG, "Error loading profile", e)
                    profileDetailsText.text = "Error loading profile"
                    Toast.makeText(
                        requireContext(),
                        "Error loading profile: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        } else {
            profileDetailsText.text = "Please log in to view profile"
            logoutButton.visibility = View.GONE
        }
    }

    private fun displayUserProfile(userData: Map<String, Any>?) {
        if (userData == null) {
            profileDetailsText.text = "No profile data available"
            return
        }

        val displayName = userData["displayName"] as? String ?: "Unknown"
        val email = userData["email"] as? String ?: "No email"
        val spotifyId = userData["spotifyId"] as? String ?: "No Spotify ID"

        val profileText = buildString {
            appendLine("Profile Information")
            appendLine("-----------------")
            appendLine("Display Name: $displayName")
            appendLine("Email: $email")
            appendLine("Spotify ID: $spotifyId")
            appendLine()
            append(getTopArtistsText(userData))
            append(getTopTracksText(userData))
            append(getTopGenresText(userData))
        }

        profileDetailsText.text = profileText
    }

    @Suppress("UNCHECKED_CAST")
    private fun getTopArtistsText(userData: Map<String, Any>): String {
        val topArtists = userData["topArtists"] as? List<Map<String, Any>> ?: return "No top artists found"
        return buildString {
            appendLine("Top Artists")
            appendLine("----------")
            topArtists.take(5).forEachIndexed { index, artist ->
                appendLine("${index + 1}. ${artist["name"]}")
            }
            appendLine()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getTopTracksText(userData: Map<String, Any>): String {
        val topTracks = userData["topTracks"] as? List<Map<String, Any>> ?: return "No top tracks found"
        return buildString {
            appendLine("Top Tracks")
            appendLine("----------")
            topTracks.take(5).forEachIndexed { index, track ->
                appendLine("${index + 1}. ${track["name"]} by ${track["artist"]}")
            }
            appendLine()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getTopGenresText(userData: Map<String, Any>): String {
        val topGenres = userData["topGenres"] as? List<Map<String, Any>> ?: return "No top genres found"
        return buildString {
            appendLine("Top Genres")
            appendLine("----------")
            topGenres.take(5).forEachIndexed { index, genre ->
                appendLine("${index + 1}. ${genre["genre"]} (${genre["count"]} tracks)")
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        profileDetailsText.visibility = if (show) View.GONE else View.VISIBLE
        logoutButton.isEnabled = !show
    }

    companion object {
        fun newInstance() = ProfileFragment()
    }
}