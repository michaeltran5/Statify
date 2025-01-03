package com.cs407.statify

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "ProfileFragment"

/**
 * Class that initializes/populates the Profile Page for Statify
 *
 */
class ProfileFragment : Fragment() {
    private lateinit var usernameText: TextView
    private lateinit var friendCountText: TextView
    private lateinit var emailText: TextView
    private lateinit var logoutButton: Button
    private lateinit var profileImage: ImageView
    private lateinit var recentlyPlayedRecyclerView: RecyclerView


    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val spotifyApi = retrofit.create(SpotifyApi::class.java)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    /**
     * Initialize Views
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        usernameText = view.findViewById(R.id.usernameText)
        emailText = view.findViewById(R.id.emailText)
        logoutButton = view.findViewById(R.id.logoutButton)
        profileImage = view.findViewById(R.id.profileImage)
        friendCountText = view.findViewById(R.id.friendCount)
        recentlyPlayedRecyclerView = view.findViewById(R.id.recentlyPlayedRecyclerView)

        setupLogoutButton()
        loadUserProfile()
        fetchRecentlyPlayedTracks()
    }

    /**
     * Logout Button Initialization/Event Handler
     *
     */
    private fun setupLogoutButton() {
        logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }
    }


    /**
     * Alerts user that they have been successfully logged out
     *
     */
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

    /**
     * Logout function
     *
     */
    private fun logout() {
        coroutineScope.launch {
            try {
                showLoading(true)

                requireActivity().getSharedPreferences("SPOTIFY", 0)
                    .edit()
                    .remove("access_token")
                    .apply()

                withContext(Dispatchers.IO) {
                    auth.signOut()
                }

                usernameText.text = ""
                emailText.text = ""

                (activity as? MainActivity)?.updateButtonsForLogout()

                showLoading(false)

                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

                findNavController().navigate(R.id.loginFragment)

            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(
                    requireContext(),
                    "Error logging out: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Fetches the user's Spotify username/profile picture using Spotify API
     *
     */
    private fun fetchSpotifyProfile() {
        coroutineScope.launch {
            try {
                val sharedPreferences = requireActivity().getSharedPreferences("SPOTIFY", 0)
                val accessToken = sharedPreferences.getString("access_token", null)

                if (accessToken != null) {
                    withContext(Dispatchers.IO) {
                        val userData = spotifyApi.getUserProfile("Bearer $accessToken")
                        withContext(Dispatchers.Main) {
                            userData.profileImageUrl?.let { imageUrl ->
                                Glide.with(requireContext())
                                    .load(imageUrl)
                                    .circleCrop()
                                    .into(profileImage)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Fetches the user's 5 most recently played songs from Spotify API
     *
     */
    private fun fetchRecentlyPlayedTracks() {
        coroutineScope.launch {
            try {
                val sharedPreferences = requireActivity().getSharedPreferences("SPOTIFY", 0)
                val accessToken = sharedPreferences.getString("access_token", null)

                if (accessToken != null) {
                    val recentlyPlayed = withContext(Dispatchers.IO) {
                        spotifyApi.getRecentlyPlayed("Bearer $accessToken")
                    }

                    withContext(Dispatchers.Main) {
                        val trackAdapter = TopTracksAdapter()
                        recentlyPlayedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                        recentlyPlayedRecyclerView.adapter = trackAdapter
                        val tracks = recentlyPlayed.items.map { it.track }
                        trackAdapter.submitList(tracks.take(5))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Sets the views to the data fetched from Firebase DB
     *
     */
    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            showLoading(true)

            fetchSpotifyProfile()

            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    showLoading(false)
                    if (document != null && document.exists()) {
                        val userData = document.data
                        displayUserProfile(userData)
                    } else {
                        usernameText.text = "No profile data available"
                        emailText.text = ""
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Log.e(TAG, "Error loading profile", e)
                    usernameText.text = "Error loading profile"
                    emailText.text = ""
                    Toast.makeText(
                        requireContext(),
                        "Error loading profile: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        } else {
            usernameText.text = "Please log in to view profile"
            emailText.text = ""
            logoutButton.visibility = View.GONE
        }
    }

    /**
     * Displays full user profile based on all the previously fetched data
     *
     */
    private fun displayUserProfile(userData: Map<String, Any>?) {
        if (userData == null) {
            usernameText.text = "Unknown User"
            emailText.text = ""
            return
        }

        val displayName = userData["displayName"] as? String ?: "Unknown"
        val email = userData["email"] as? String ?: "No email"

        usernameText.text = displayName
        emailText.text = email
    }

    private fun showLoading(show: Boolean) {
        usernameText.visibility = if (show) View.GONE else View.VISIBLE
        emailText.visibility = if (show) View.GONE else View.VISIBLE
        logoutButton.isEnabled = !show
    }

    companion object {
        fun newInstance() = ProfileFragment()
    }
}