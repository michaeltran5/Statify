package com.cs407.statify

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var listeningTimeText: TextView
    private lateinit var topTracksText: TextView
    private lateinit var topArtistsText: TextView
    private lateinit var topGenresText: TextView
    private lateinit var loginButton: Button

    private val db = Firebase.firestore

    private val spotifyApi = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SpotifyApi::class.java)

    private var accessToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeViews()
        setupLoginButton()
    }

    private fun initializeViews() {
        loginButton = findViewById(R.id.loginButton)
        userNameText = findViewById(R.id.userNameText)
        userEmailText = findViewById(R.id.userEmailText)
        listeningTimeText = findViewById(R.id.listeningTimeText)
        topTracksText = findViewById(R.id.topTracksText)
        topArtistsText = findViewById(R.id.topArtistsText)
        topGenresText = findViewById(R.id.topGenresText)
    }

    private fun setupLoginButton() {
        loginButton.setOnClickListener {
            startSpotifyAuth()
        }
    }

    private fun startSpotifyAuth() {
        try {
            Log.d("Statify", "Starting Spotify auth process")
            SpotifyAuth.authenticate(this)
        } catch (e: Exception) {
            Log.e("Statify", "Error launching auth", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("Statify", "onActivityResult called - requestCode: $requestCode, resultCode: $resultCode")

        if (requestCode == SpotifyAuth.REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, data)
            Log.d("Statify", "Auth response type: ${response.type}")

            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    accessToken = response.accessToken
                    Log.d("Statify", "Token received successfully")
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    fetchSpotifyData()
                }
                AuthorizationResponse.Type.ERROR -> {
                    Log.e("Statify", "Auth error: ${response.error}")
                    Toast.makeText(this, "Login failed: ${response.error}", Toast.LENGTH_LONG).show()
                }
                else -> {
                    Log.d("Statify", "Auth cancelled or unknown response")
                    Toast.makeText(this, "Login cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        Log.d("Statify", "onNewIntent called")

        // Handle redirect from browser
        intent?.data?.let { uri ->
            if (uri.scheme == "statify") {
                // Parse the access token from the URI fragment
                val fragment = uri.fragment
                if (fragment != null && fragment.contains("access_token=")) {
                    val accessToken = fragment.substringAfter("access_token=")
                        .substringBefore("&")

                    this.accessToken = accessToken
                    Log.d("Statify", "Token received from browser redirect")
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    fetchSpotifyData()
                }
            }
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
        topGenresList: List<Map<String, Any>>
    ) {
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
            "lastUpdated" to com.google.firebase.Timestamp.now()
        )

        db.collection("users")
            .document(userData.id)
            .set(userDataMap)
            .addOnSuccessListener {
                Log.d("Statify", "Data stored in Firebase")
            }
            .addOnFailureListener { e ->
                Log.e("Statify", "Error storing data", e)
            } //test
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