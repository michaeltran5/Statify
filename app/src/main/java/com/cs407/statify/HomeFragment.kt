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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "HomeFragment"

class HomeFragment : Fragment() {
    // UI Components
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var listeningTimeText: TextView
    private lateinit var topTracksText: TextView
    private lateinit var topArtistsText: TextView
    private lateinit var topGenresText: TextView
    private lateinit var loginButton: Button
    private lateinit var webView: WebView

    // Firebase instances
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    // Spotify API setup
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
        checkAuthState()
    }

    private fun initializeViews(view: View) {
        loginButton = view.findViewById(R.id.loginButton)
        userNameText = view.findViewById(R.id.userNameText)
        userEmailText = view.findViewById(R.id.userEmailText)
        listeningTimeText = view.findViewById(R.id.listeningTimeText)
        topTracksText = view.findViewById(R.id.topTracksText)
        topArtistsText = view.findViewById(R.id.topArtistsText)
        topGenresText = view.findViewById(R.id.topGenresText)
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

    private fun checkAuthState() {
        auth.currentUser?.let { user ->
            Log.d(TAG, "Current Firebase user: ${user.uid}")
            loginButton.visibility = View.GONE

            // Check if we have the user's Spotify data
            lifecycleScope.launch {
                try {
                    val docSnapshot = db.collection("users")
                        .document(user.uid)
                        .get()
                        .await()

                    if (docSnapshot.exists()) {
                        // We have data, update UI
                        val spotifyData = docSnapshot.data
                        spotifyData?.let { updateUIFromFirestore(it) }
                    } else {
                        // No data yet, show login button
                        loginButton.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking Firestore data", e)
                    loginButton.visibility = View.VISIBLE
                }
            }
        } ?: run {
            // No Firebase user, show login button
            Log.d(TAG, "No Firebase user found")
            loginButton.visibility = View.VISIBLE
        }
    }

    private fun startSpotifyAuth() {
        val client = SpotifyWebViewClient { token ->
            accessToken = token
            webView.visibility = View.GONE
            Log.d(TAG, "Spotify Auth successful")

            // Save token to SharedPreferences
            requireActivity().getSharedPreferences("SPOTIFY", 0)
                .edit()
                .putString("access_token", token)
                .apply()

            lifecycleScope.launch {
                signInToFirebase()
            }
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

    private suspend fun signInToFirebase() {
        try {
            // Check if we already have a Firebase user
            if (auth.currentUser == null) {
                Log.d(TAG, "Attempting anonymous sign in")
                val result = auth.signInAnonymously().await()
                Log.d(TAG, "Anonymous sign in successful, UID: ${result.user?.uid}")
            } else {
                Log.d(TAG, "Already signed in to Firebase, UID: ${auth.currentUser?.uid}")
            }
            // Now that we're authenticated, proceed with data fetching
            fetchSpotifyData()
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Auth failed", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    "Authentication failed: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun fetchSpotifyData() {
        try {
            val firebaseUser = auth.currentUser ?: throw Exception("No Firebase user")
            val spotifyAuth = "Bearer $accessToken"

            Log.d(TAG, "Fetching Spotify user profile")
            val userData = spotifyApi.getUserProfile(spotifyAuth)

            Log.d(TAG, "Fetching top tracks")
            val topTracks = spotifyApi.getTopTracks(spotifyAuth)

            Log.d(TAG, "Fetching top artists")
            val topArtists = spotifyApi.getTopArtists(spotifyAuth)

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
            storeDataInFirebase(firebaseUser.uid, userData, topTracks, topArtists, topGenresList)

            // Update UI
            withContext(Dispatchers.Main) {
                updateUI(userData, topTracks.items, topArtists.items, topGenresList)
                calculateListeningTime(spotifyAuth)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching data", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun storeDataInFirebase(
        firebaseUid: String,
        userData: UserData,
        topTracks: TopTracksResponse,
        topArtists: TopArtistsResponse,
        topGenresList: List<Map<String, Any>>
    ) {
        val userDataMap = hashMapOf(
            "userId" to firebaseUid,
            "spotifyId" to userData.id,
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
            "friends" to listOf<String>()
        )

        Log.d(TAG, "Storing data in Firebase for user: $firebaseUid")

        db.collection("users")
            .document(firebaseUid)
            .set(userDataMap)
            .addOnSuccessListener {
                Log.d(TAG, "Data stored in Firebase successfully")
                loginButton.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error storing data", e)
                Toast.makeText(requireContext(), "Error saving data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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

            withContext(Dispatchers.Main) {
                listeningTimeText.text = getString(R.string.listening_time_format, hours, minutes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating listening time", e)
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

    private fun updateUIFromFirestore(data: Map<String, Any>) {
        userNameText.text = getString(R.string.name_format, data["displayName"] as? String ?: "Unknown")
        userEmailText.text = getString(R.string.email_format, data["email"] as? String ?: "Unknown")

        @Suppress("UNCHECKED_CAST")
        val tracks = (data["topTracks"] as? List<Map<String, Any>>)?.mapIndexed { index, track ->
            "${index + 1}. ${track["name"]} by ${track["artist"]}"
        }?.joinToString("\n")
        topTracksText.text = tracks ?: "No tracks available"

        @Suppress("UNCHECKED_CAST")
        val artists = (data["topArtists"] as? List<Map<String, Any>>)?.take(10)?.mapIndexed { index, artist ->
            "${index + 1}. ${artist["name"]}"
        }?.joinToString("\n")
        topArtistsText.text = artists ?: "No artists available"

        @Suppress("UNCHECKED_CAST")
        val genres = (data["topGenres"] as? List<Map<String, Any>>)?.mapIndexed { index, genre ->
            "${index + 1}. ${genre["genre"]} (${genre["count"]} artists)"
        }?.joinToString("\n")
        topGenresText.text = genres ?: "No genres available"
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}