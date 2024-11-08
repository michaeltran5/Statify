package com.cs407.statify

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SpotifyApi {
    @GET("v1/me")
    suspend fun getUserProfile(
        @Header("Authorization") auth: String
    ): UserData

    @GET("v1/me/top/tracks")
    suspend fun getTopTracks(
        @Header("Authorization") auth: String,
        @Query("limit") limit: Int = 10,
        @Query("time_range") timeRange: String = "medium_term"
    ): TopTracksResponse

    @GET("v1/me/top/artists")
    suspend fun getTopArtists(
        @Header("Authorization") auth: String,
        @Query("limit") limit: Int = 50,
        @Query("time_range") timeRange: String = "medium_term"
    ): TopArtistsResponse

    @GET("v1/me/player/recently-played")
    suspend fun getRecentlyPlayed(
        @Header("Authorization") auth: String,
        @Query("limit") limit: Int = 50,
        @Query("before") before: Long? = null
    ): RecentlyPlayedResponse
}

data class UserData(
    val id: String,
    val display_name: String?,  // Changed to match Spotify's API field name
    val email: String?,
    val images: List<SpotifyImage>?,
) {
    // Add a computed property
    val displayName: String
        get() = display_name ?: "Spotify User"  // Fallback if null
}

data class TopTracksResponse(
    val items: List<Track>
)

data class Track(
    val id: String,
    val name: String,
    val artists: List<Artist>,
    val album: Album,
    val durationMs: Int
)

data class TopArtistsResponse(
    val items: List<Artist>
)

data class Artist(
    val id: String,
    val name: String,
    val genres: List<String>,
    val images: List<SpotifyImage>?
)

data class Album(
    val id: String,
    val name: String,
    val images: List<SpotifyImage>?
)

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
)

data class Cursors(
    val after: String?,
    val before: String?
)