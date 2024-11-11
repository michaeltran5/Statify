package com.cs407.statify

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TopTracksFragment : Fragment() {
    private lateinit var topTracksDetailText: TextView

    private val spotifyApi = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SpotifyApi::class.java)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_top_tracks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        topTracksDetailText = view.findViewById(R.id.topTracksDetailText)

        // Get access token from SharedPreferences
        val sharedPreferences = requireActivity().getSharedPreferences("SPOTIFY", 0)
        val accessToken = sharedPreferences.getString("access_token", null)

        if (accessToken != null) {
            fetchTopTracks(accessToken)
        } else {
            topTracksDetailText.text = "Please login first"
        }
    }

    private fun fetchTopTracks(accessToken: String) {
        lifecycleScope.launch {
            try {
                val auth = "Bearer $accessToken"
                Log.d("TopTracksFragment", "Fetching tracks with token: $accessToken")

                val topTracks = spotifyApi.getTopTracks(auth)
                Log.d("TopTracksFragment", "Retrieved ${topTracks.items.size} tracks")

                val formattedTracks = formatTrackList(topTracks.items.take(10))
                topTracksDetailText.text = formattedTracks

            } catch (e: Exception) {
                Log.e("TopTracksFragment", "Error fetching top tracks", e)
                topTracksDetailText.text = "Error loading tracks: ${e.message}"
                Toast.makeText(requireContext(), "Error fetching top tracks", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun formatTrackList(tracks: List<Track>): String {
        return if (tracks.isEmpty()) {
            "No tracks found"
        } else {
            tracks.mapIndexed { index, track ->
                val position = (index + 1).toString()
                """
                $position. ${track.name}
                Artist: ${track.artists.joinToString(", ") { it.name }}
                Album: ${track.album.name}
                
                """.trimIndent()
            }.joinToString("\n")
        }
    }
}