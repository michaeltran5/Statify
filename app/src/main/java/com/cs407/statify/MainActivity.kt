package com.cs407.statify

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.log

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

    private fun fetchSpotifyData() {
        lifecycleScope.launch {
            try {
                val auth = "Bearer $accessToken"

                // Get user profile
                val userData = spotifyApi.getUserProfile(auth)

                // Get top tracks
                val topTracks = spotifyApi.getTopTracks(auth)

                // Get top artists and calculate genres
                val topArtists = spotifyApi.getTopArtists(auth)

                // Calculate top genres
                val genreCounts = mutableMapOf<String, Int>()
                topArtists.items.forEach { artist ->
                    artist.genres.forEach { genre ->
                        genreCounts[genre] = (genreCounts[genre] ?: 0) + 1
                    }
                }

                val topGenresList = genreCounts.entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .map { (genre, count) -> mapOf(
                        "genre" to genre,
                        "count" to count
                    )}

                // Store in Firebase
                storeDataInFirebase(userData, topTracks, topArtists, topGenresList)

                // Update UI
                updateUI(userData, topTracks.items, topArtists.items, topGenresList)
                calculateListeningTime(auth)

            } catch (e: Exception) {
                Log.e("Statify", "Error fetching data", e)
                Toast.makeText(this@MainActivity, "Error fetching data", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun storeDataInFirebase(
        userData: UserData,
        topTracks: TopTracksResponse,
        topArtists: TopArtistsResponse,
        topGenresList: List<Map<String,Any>>
    ) {
        // test friend list
        val testFriendList: ArrayList<String> = arrayListOf("Friend1", "Friend2")

        val userDataMap = hashMapOf(
            "userId" to userData.id,
            "displayName" to userData.displayName,
            "email" to userData.email,
            "topTracks" to topTracks.items.map { track ->
                hashMapOf(
                    "id" to track.id,
                    "name" to track.name,
                    "artist" to track.artists.firstOrNull()?.name,
                    "album" to track.album.name
                )
            },
            "topArtists" to topArtists.items.take(10).map { artist ->
                hashMapOf(
                    "id" to artist.id,
                    "name" to artist.name,
                    "genres" to artist.genres
                )
            },
            "topGenres" to topGenresList,
            "lastUpdated" to com.google.firebase.Timestamp.now(),
            "username" to userData.displayName,
            "friends" to testFriendList
        )

        val fm = FriendManager(userData.displayName, ArrayList())
        fm.displayFriends(fm.username)

        Log.d("userMap", userDataMap.toString())
        db.collection("users")
            .document(userData.id)
            .set(userDataMap)
            .addOnSuccessListener {
                Log.d("Statify", "Data stored in Firebase")
            }
            .addOnFailureListener { e ->
                Log.e("Statify", "Error storing data", e)
            }
    }

    private suspend fun calculateListeningTime(auth: String) {
        try {
            var totalDurationMs = 0L
            val response = spotifyApi.getRecentlyPlayed(auth)

            response.items.forEach { playHistory ->
                totalDurationMs += playHistory.track.durationMs
            }

            val totalMinutes = totalDurationMs / 60000
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            listeningTimeText.text = getString(R.string.listening_time_format, hours, minutes)

        } catch (e: Exception) {
            Log.e("Statify", "Error calculating listening time", e)
        }
    }

    private fun updateUI(
        userData: UserData,
        topTracks: List<Track>,
        topArtists: List<Artist>,
        topGenres: List<Map<String, Any>>
    ) {
        userNameText.text = getString(R.string.name_format, userData.displayName)
        userEmailText.text = getString(R.string.email_format, userData.email)

        val tracksText = topTracks.mapIndexed { index, track ->
            "${index + 1}. ${track.name} by ${track.artists.firstOrNull()?.name}"
        }.joinToString("\n")
        topTracksText.text = tracksText

        val artistsText = topArtists.take(10).mapIndexed { index, artist ->
            "${index + 1}. ${artist.name}"
        }.joinToString("\n")
        topArtistsText.text = artistsText

        val genresText = topGenres.mapIndexed { index, genreMap ->
            "${index + 1}. ${genreMap["genre"]} (${genreMap["count"]} artists)"
        }.joinToString("\n")
        topGenresText.text = genresText
    }
}