package com.cs407.statify

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomeFragment : Fragment() {
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var listeningTimeText: TextView
    private lateinit var topTracksText: TextView
    private lateinit var topArtistsText: TextView
    private lateinit var topGenresText: TextView
    private lateinit var loginButton: Button
    private lateinit var webView: WebView

    private val db = Firebase.firestore

    private val spotifyApi = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SpotifyApi::class.java)

    private var accessToken: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupWebView()
        setupLoginButton()
    }

    private fun initializeViews(view: View) {
        loginButton = view.findViewById(R.id.loginButton)
        userNameText = view.findViewById(R.id.userNameText)
        userEmailText = view.findViewById(R.id.userEmailText)
        listeningTimeText = view.findViewById(R.id.listeningTimeText)
        topTracksText = view.findViewById(R.id.topTracksText)
        topArtistsText = view.findViewById(R.id.topArtistsText)
        topGenresText = view.findViewById(R.id.topGenresText)
        webView = requireActivity().findViewById(R.id.webView) // WebView stays in MainActivity
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
            Log.d("Statify", "Auth successful, token: $token")

            // Save token to SharedPreferences
            requireActivity().getSharedPreferences("SPOTIFY", 0)
                .edit()
                .putString("access_token", token)
                .apply()

            fetchSpotifyData()
        }

        webView.webViewClient = client
        val authUrl = buildSpotifyAuthUrl()
        webView.visibility = View.VISIBLE
        webView.loadUrl(authUrl)
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
                Toast.makeText(requireContext(), "Error fetching data", Toast.LENGTH_LONG).show()
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