package com.cs407.statify

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.request.RequestOptions

class TopTracksFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TopTracksAdapter

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

        adapter = TopTracksAdapter()
        recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@TopTracksFragment.adapter
        }

        val sharedPreferences = requireActivity().getSharedPreferences("SPOTIFY", 0)
        val accessToken = sharedPreferences.getString("access_token", null)

        if (accessToken != null) {
            fetchTopTracks(accessToken)
        } else {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchTopTracks(accessToken: String) {
        lifecycleScope.launch {
            try {
                val auth = "Bearer $accessToken"
                Log.d("TopTracksFragment", "Fetching tracks with token: $accessToken")

                // Set limit to 10 in the API call
                val topTracks = spotifyApi.getTopTracks(auth, limit = 50)
                Log.d("TopTracksFragment", "Retrieved ${topTracks.items.size} tracks")

                adapter.submitList(topTracks.items)

            } catch (e: Exception) {
                Log.e("TopTracksFragment", "Error fetching top tracks", e)
                Toast.makeText(requireContext(), "Error fetching top tracks", Toast.LENGTH_LONG).show()
            }
        }
    }
}

class TopTracksAdapter : RecyclerView.Adapter<TopTracksAdapter.TrackViewHolder>() {
    private var tracks: List<Track> = emptyList()

    fun submitList(newTracks: List<Track>) {
        tracks = newTracks.take(50) // Ensure we only take top 50 even if more are returned
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(tracks[position], position + 1)
    }

    override fun getItemCount(): Int = tracks.size

    class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val positionText: TextView = itemView.findViewById(R.id.positionText)
        private val trackImage: ImageView = itemView.findViewById(R.id.trackImage)
        private val trackName: TextView = itemView.findViewById(R.id.trackName)
        private val artistName: TextView = itemView.findViewById(R.id.artistName)
        private val albumName: TextView = itemView.findViewById(R.id.albumName)

        fun bind(track: Track, position: Int) {
            positionText.text = position.toString()
            trackName.text = track.name
            artistName.text = track.artists.joinToString(", ") { it.name }
            albumName.text = track.album.name

            // Load image using Glide
            track.getImage(SpotifyImageSizes.MEDIUM)?.let { imageUrl ->
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .apply(RequestOptions()
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_album)
                        .error(R.drawable.placeholder_album))
                    .into(trackImage)
            } ?: run {
                // If no image URL is available, load placeholder
                trackImage.setImageResource(R.drawable.placeholder_album)
            }
        }
    }
}