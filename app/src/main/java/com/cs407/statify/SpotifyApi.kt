package com.cs407.statify

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import kotlin.math.abs

/**
 * Interface for fetching all data needed from Spotify API
 *
 */
interface SpotifyApi {
    @GET("v1/me") //Gets User Profile
    suspend fun getUserProfile(
        @Header("Authorization") auth: String
    ): UserData

    @GET("v1/me/top/tracks") //Gets Top Tracks
    suspend fun getTopTracks(
        @Header("Authorization") auth: String,
        @Query("limit") limit: Int = 20,
        @Query("time_range") timeRange: String = "medium_term"
    ): TopTracksResponse

    @GET("v1/me/top/artists") //Gets Top Artists
    suspend fun getTopArtists(
        @Header("Authorization") auth: String,
        @Query("limit") limit: Int = 20,
        @Query("time_range") timeRange: String = "medium_term"
    ): TopArtistsResponse

    @GET("v1/me/player/recently-played") //Gets recently played
    suspend fun getRecentlyPlayed(
        @Header("Authorization") auth: String,
        @Query("limit") limit: Int = 50,
        @Query("before") before: Long? = null
    ): RecentlyPlayedResponse
}

object SpotifyImageSizes { // Image size constants

    const val SMALL = 64
    const val MEDIUM = 300
    const val LARGE = 640
}

// Extension function for getting best matching image URL
fun List<SpotifyImage>?.getBestImageUrl(preferredSize: Int = SpotifyImageSizes.MEDIUM): String? {
    if (this.isNullOrEmpty()) return null
    return minByOrNull { image ->
        abs((image.width ?: 0) - preferredSize)
    }?.url ?: firstOrNull()?.url
}

/**
 * User Data class - stores all information tied to a Spotify User's account
 *
 */
data class UserData(
    val id: String,
    val display_name: String?,
    val email: String?,
    val images: List<SpotifyImage>?
) {
    val displayName: String
        get() = display_name ?: "Spotify User"

    val profileImageUrl: String?
        get() = images?.getBestImageUrl()

    fun getProfileImage(size: Int = SpotifyImageSizes.MEDIUM): String? =
        images?.getBestImageUrl(size)
}

data class TopTracksResponse(
    val items: List<Track>
)

data class Track(
    val id: String,
    val name: String,
    val artists: List<Artist>,
    val album: Album,
    val durationMs: Int,
) {
    val imageUrl: String?
        get() = album.images?.getBestImageUrl()

    fun getImage(size: Int = SpotifyImageSizes.MEDIUM): String? =
        album.images?.getBestImageUrl(size)

    // Helper property to get primary artist name
    val artistName: String
        get() = artists.firstOrNull()?.name ?: ""

    // Helper property to get all artist names
    val allArtistNames: String
        get() = artists.joinToString(", ") { it.name }
}

data class TopArtistsResponse(
    val items: List<Artist>
)

/**
 * Artist data class - stores all data related to a Spotify Artist
 *
 */
data class Artist(
    val id: String,
    val name: String,
    val genres: List<String>,
    val images: List<SpotifyImage>?
) {
    val imageUrl: String?
        get() = images?.getBestImageUrl()

    fun getImage(size: Int = SpotifyImageSizes.MEDIUM): String? =
        images?.getBestImageUrl(size)
}

/**
 * Album data class - stores all data related to an Album
 *
 */
data class Album(
    val id: String,
    val name: String,
    val images: List<SpotifyImage>?
) {
    val imageUrl: String?
        get() = images?.getBestImageUrl()

    fun getImage(size: Int = SpotifyImageSizes.MEDIUM): String? =
        images?.getBestImageUrl(size)
}

data class SpotifyImage(
    val url: String,
    val height: Int?,
    val width: Int?
)

data class RecentlyPlayedResponse(
    val items: List<PlayHistoryObject>,
    val cursors: Cursors?
)

data class PlayHistoryObject(
    val track: Track,
    val playedAt: String
) {
    // Helper property to easily get the track's image
    val imageUrl: String?
        get() = track.imageUrl

    fun getImage(size: Int = SpotifyImageSizes.MEDIUM): String? =
        track.getImage(size)
}

data class Cursors(
    val after: String?,
    val before: String?
)