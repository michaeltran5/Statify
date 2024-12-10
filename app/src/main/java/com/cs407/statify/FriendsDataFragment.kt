package com.cs407.statify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FriendsDataFragment : Fragment() {

    data class TrackData(
        val name: String = "",
        val artists: String = "",
        val album: Album = Album("", "", null),
        val id: String = "",
    ) {
        val imageUrl: String?
            get() = album.images?.getBestImageUrl()

        fun getImage(size: Int = SpotifyImageSizes.MEDIUM): String? =
            album.images?.getBestImageUrl(size)
    }

    private lateinit var friendName: String
    private lateinit var friendManager: FriendManager
    private lateinit var topTracks: ArrayList<TrackData>
    private lateinit var topArtists: ArrayList<Artist>
    private lateinit var recyclerView: RecyclerView
    private lateinit var trackAdapter: FriendsTrackAdapter
    private lateinit var artistViewPager: ViewPager2
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_friend_data, container, false)


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trackAdapter = FriendsTrackAdapter()
        recyclerView = view.findViewById<RecyclerView>(R.id.tracksRecyclerView).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@FriendsDataFragment.trackAdapter
        }
        artistViewPager = view.findViewById(R.id.friendArtistCarousel)
        loadFriendData(view)
    }

    private fun loadFriendData(view: View) {

        friendName = arguments?.getString("friend") ?: ""
        friendManager = arguments?.getSerializable("friendManager") as? FriendManager ?:
        FriendManager("", ArrayList<String>(), requireContext())

        val textView = view.findViewById<TextView>(R.id.friendName)
        textView.text = friendName
        val button = view.findViewById<ImageButton>(R.id.backButton)
        button.setOnClickListener {
            findNavController().navigate(R.id.action_friendsDataFragment_to_friendsFragment)
        }

        CoroutineScope(Dispatchers.Main).launch {
            topTracks = friendManager.getFriendTrackData(friendName)
            topArtists = friendManager.getFriendArtistData(friendName)
            setupArtistCarousel(topArtists)
            trackAdapter.submitList(topTracks)
        }

    }

    private fun setupArtistCarousel(topArtists: List<Artist>) {
        val carouselAdapter = ArtistCarouselAdapter(
            artists = topArtists,
            context = requireContext()
        )

        artistViewPager.apply {
            adapter = carouselAdapter
            offscreenPageLimit = 3
            setPageTransformer { page, position ->
                val scaleFactor = 0.85f
                val minScale = 0.8f
                val absPosition = kotlin.math.abs(position)

                page.scaleX = minScale + (1 - absPosition) * (scaleFactor - minScale)
                page.scaleY = minScale + (1 - absPosition) * (scaleFactor - minScale)
                page.alpha = 1 - absPosition
            }
        }
    }

    class FriendsTrackAdapter : RecyclerView.Adapter<FriendsTrackAdapter.TrackViewHolder>() {
        private var tracks: List<TrackData> = emptyList()

        fun submitList(newTracks: List<TrackData>) {
            tracks = newTracks
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

            fun bind(track: TrackData, position: Int) {
                positionText.text = position.toString()
                trackName.text = track.name
                artistName.text = track.artists

                track.getImage(SpotifyImageSizes.MEDIUM)?.let { imageUrl ->
                    Glide.with(itemView.context)
                        .load(imageUrl)
                        .apply(RequestOptions()
                            .centerCrop()
                            .placeholder(R.drawable.placeholder_album)
                            .error(R.drawable.placeholder_album))
                        .into(trackImage)
                } ?: run {
                    trackImage.setImageResource(R.drawable.placeholder_album)
                }
            }
        }
    }

}