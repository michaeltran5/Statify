package com.cs407.statify

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.graphics.Paint
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.TypefaceSpan
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

private const val TAG = "HomeFragment"

class HomeFragment : Fragment() {
    private lateinit var topTracksContainer: FrameLayout
    private lateinit var topTracksRecyclerView: RecyclerView
    private lateinit var viewPager: ViewPager2
    private lateinit var topGenresText: TextView
    private lateinit var webView: WebView

    private val auth = Firebase.auth
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
        checkAuthState()
    }

    private fun initializeViews(view: View) {
        topTracksContainer = view.findViewById(R.id.topTracksContainer)
        viewPager = view.findViewById(R.id.artistCarouselViewPager)
        topGenresText = view.findViewById(R.id.topGenresText)
        webView = requireActivity().findViewById(R.id.webView)
    }

    private fun checkAuthState() {
        auth.currentUser?.let { user ->
            Log.d(TAG, "Current Firebase user: ${user.uid}")
            accessToken = requireActivity().getSharedPreferences("SPOTIFY", 0)
                .getString("access_token", null)

            if (accessToken != null) {
                lifecycleScope.launch {
                    fetchSpotifyData()
                }
            }
        }
    }

    private suspend fun fetchSpotifyData() {
        try {
            val firebaseUser = auth.currentUser ?: throw Exception("No Firebase user")
            val spotifyAuth = "Bearer $accessToken"

            val userData = spotifyApi.getUserProfile(spotifyAuth)
            val topTracks = spotifyApi.getTopTracks(spotifyAuth)
            val topArtists = spotifyApi.getTopArtists(spotifyAuth)

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

            storeDataInFirebase(firebaseUser.uid, userData, topTracks, topArtists, topGenresList)

            withContext(Dispatchers.Main) {
                updateUI(userData, topTracks.items, topArtists.items, topGenresList)
                calculateListeningTime(spotifyAuth)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching data", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG).show()
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
                    "genres" to artist.genres,
                    "images" to (artist.images?.map { image ->
                        mapOf(
                            "url" to image.url,
                            "height" to image.height,
                            "width" to image.width
                        )
                    } ?: emptyList<Map<String, Any>>())
                )
            },
            "topGenres" to topGenresList,
            "lastUpdated" to com.google.firebase.Timestamp.now(),
            "username" to userData.displayName,
            "friends" to listOf<String>()
        )

        db.collection("users")
            .document(firebaseUid)
            .set(userDataMap)
            .addOnFailureListener { e ->
                Log.e(TAG, "Error storing data", e)
                Toast.makeText(requireContext(), "Error saving data: ${e.localizedMessage}",
                    Toast.LENGTH_LONG).show()
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
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating listening time", e)
        }
    }

    private fun setupArtistCarousel(topArtists: List<Artist>) {
        val carouselAdapter = ArtistCarouselAdapter(
            artists = topArtists,
            context = requireContext()
        )

        viewPager.apply {
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

    private fun updateUI(
        userData: UserData,
        topTracks: List<Track>,
        topArtists: List<Artist>,
        topGenres: List<Map<String, Any>>
    ) {
        setupArtistCarousel(topArtists)

        topTracksRecyclerView = RecyclerView(requireContext())
        topTracksRecyclerView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        topTracksRecyclerView.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )

        topTracksRecyclerView.viewTreeObserver.addOnGlobalLayoutListener {
            val itemView = (topTracksRecyclerView.layoutManager as LinearLayoutManager).findViewByPosition(0)
            itemView?.let {
                val itemHeight = it.measuredHeight
                val totalHeight = itemHeight * 6 // Adjust for 5 items
                topTracksRecyclerView.layoutParams.height = totalHeight
                topTracksRecyclerView.requestLayout()
            }
        }

        val topTracksAdapter = TopTracksAdapter()
        topTracksRecyclerView.adapter = topTracksAdapter
        topTracksAdapter.submitList(topTracks.take(5))

        topTracksContainer.addView(topTracksRecyclerView)

        val genresText = topGenres.mapIndexed { index, genreMap ->
            val genreInfo = "${index + 1}  ${genreMap["genre"]} (${genreMap["count"]} artists)"
            getStyledText(index + 1, genreInfo)
        }.joinToSpanned("\n")
        topGenresText.text = genresText
    }

    private fun updateUIFromFirestore(data: Map<String, Any>) {
        val topArtists = (data["topArtists"] as? List<Map<String, Any>>)
            ?.take(10)
            ?.map { artistData ->
                Artist(
                    id = artistData["id"] as? String ?: "",
                    name = artistData["name"] as? String ?: "",
                    genres = (artistData["genres"] as? List<String>) ?: listOf(),
                    images = (artistData["images"] as? List<Map<String, Any>>)?.map { imageData ->
                        SpotifyImage(
                            url = imageData["url"] as? String ?: "",
                            height = (imageData["height"] as? Number)?.toInt() ?: 0,
                            width = (imageData["width"] as? Number)?.toInt() ?: 0
                        )
                    }
                )
            } ?: listOf()

        setupArtistCarousel(topArtists)

        @Suppress("UNCHECKED_CAST")
        val tracks = (data["topTracks"] as? List<Map<String, Any>>)
            ?.take(10)
            ?.mapIndexed { index, track ->
                val trackInfo = "${index + 1}  ${track["name"]} by ${track["artist"]}"
                getStyledText(index + 1, trackInfo)
            }?.joinToSpanned("\n")

        val genres = (data["topGenres"] as? List<Map<String, Any>>)
            ?.mapIndexed { index, genre ->
                val genreInfo = "${index + 1}  ${genre["genre"]} (${genre["count"]} artists)"
                getStyledText(index + 1, genreInfo)
            }?.joinToSpanned("\n")
        topGenresText.text = genres ?: "No genres available"
    }

    private fun getStyledText(number: Int, fullText: String): SpannableString {
        val spannable = SpannableString(fullText)
        val numberString = "$number"
        val startIndex = fullText.indexOf(numberString)
        val endIndex = startIndex + numberString.length
        val restOfLineStartIndex = endIndex

        if (startIndex >= 0) {
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.spotify_green)),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannable.setSpan(
                AbsoluteSizeSpan(25, true),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val numberTypeface = Typeface.create("monospace", Typeface.BOLD)
            spannable.setSpan(
                CustomTypefaceSpan(numberTypeface),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannable.setSpan(
                AbsoluteSizeSpan(18, true),
                restOfLineStartIndex,
                spannable.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    class CustomTypefaceSpan(private val typeface: Typeface) : TypefaceSpan(null) {
        override fun updateDrawState(ds: TextPaint) {
            applyCustomTypeface(ds, typeface)
        }

        override fun updateMeasureState(paint: TextPaint) {
            applyCustomTypeface(paint, typeface)
        }

        private fun applyCustomTypeface(paint: Paint, tf: Typeface) {
            val oldTypeface = paint.typeface
            val oldStyle = oldTypeface?.style ?: 0

            val fake = oldStyle and tf.style.inv()
            if (fake and Typeface.BOLD != 0) {
                paint.isFakeBoldText = true
            }

            if (fake and Typeface.ITALIC != 0) {
                paint.textSkewX = -0.25f
            }

            paint.typeface = tf
        }
    }

    private fun List<Spanned>.joinToSpanned(separator: String): Spanned {
        return this.reduce { acc, spanned ->
            SpannableStringBuilder(acc).append(separator).append(spanned)
        }
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}