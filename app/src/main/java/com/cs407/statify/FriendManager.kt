package com.cs407.statify

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.io.Serializable

class FriendManager(
    val username: String,
    val friendList: ArrayList<String>,
    @Transient private val context: Context
) : Serializable {
    private val db = Firebase.firestore

    /**
     * Search Firestore DB for users to add as friend
     * @param userToSearch name of user to add as friend
     * @return String username if found, empty string if not found
     */
    suspend fun searchForFriend(userToSearch: String): String {
        var searchResult: String = ""
        val result = db.collection("users")
            .whereEqualTo("username", userToSearch)
            .get()
            .await()

        if (result.isEmpty) {
            Toast.makeText(context,"Could not find user with username $userToSearch", Toast.LENGTH_SHORT).show()
        } else {
            searchResult = result.documents[0].get("username") as String
            Log.d("Found Friend", searchResult)
        }
        return searchResult
    }

    /**
     * Adds a friend to user's friend list
     * @param userToAdd name of user to add in database
     */
    suspend fun addFriend(userToAdd: String) {
        val result = db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .await()
        if (!this.friendList.contains(userToAdd)) {
            this.friendList.add(userToAdd)
            result.documents[0].reference.update("friends", this.friendList)
            Toast.makeText(context,"Added $userToAdd", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "$userToAdd is already your friend!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Removes a friend from user's friend list
     * @param userToRemove name of user to remove from database
     */
    suspend fun removeFriend(userToRemove: String) {
        val result = db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .await()
        if (this.friendList.contains(userToRemove)) {
            this.friendList.remove(userToRemove)
            result.documents[0].reference.update("friends", this.friendList)
            Toast.makeText(context,"Removed $userToRemove", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context,"$userToRemove is not your friend!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Queries Firestore DB for a specific user's top tracks
     * @param username name of user in database
     * @return ArrayList of TrackData objects
     */
    suspend fun getFriendData(username: String): ArrayList<FriendsDataFragment.TrackData> {
        Log.d("FriendManager", "Getting data for username: $username")
        val topTracks: ArrayList<FriendsDataFragment.TrackData> = ArrayList()

        try {
            val result = db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()

            if (result.isEmpty) {
                Log.d("FriendManager", "No user found with username: $username")
                return topTracks
            }

            val tracks = result.documents[0].get("topTracks") as? List<Map<String, Any>>
            Log.d("FriendManager", "Raw tracks data: $tracks")

            tracks?.forEach { track ->
                try {
                    val albumValue = track["album"]
                    Log.d("FriendManager", """
                    Album value debug:
                    Type: ${albumValue?.javaClass}
                    Value: $albumValue
                """.trimIndent())

                    val albumData = when (albumValue) {
                        is String -> {
                            mapOf(
                                "id" to "",
                                "name" to albumValue,
                                "images" to emptyList<Map<String, Any>>()
                            )
                        }
                        is Map<*, *> -> {
                            @Suppress("UNCHECKED_CAST")
                            albumValue as Map<String, Any>
                        }
                        else -> {
                            Log.e("FriendManager", "Unexpected album type: ${albumValue?.javaClass}")
                            mapOf(
                                "id" to "",
                                "name" to "",
                                "images" to emptyList<Map<String, Any>>()
                            )
                        }
                    }

                    val images = (albumData["images"] as? List<Map<String, Any>>)?.map { imageData ->
                        SpotifyImage(
                            url = imageData["url"] as? String ?: "",
                            height = (imageData["height"] as? Number)?.toInt(),
                            width = (imageData["width"] as? Number)?.toInt()
                        )
                    }

                    Log.d("FriendManager", "Parsed images: $images")

                    val album = Album(
                        id = albumData["id"] as? String ?: "",
                        name = albumData["name"] as? String ?: "",
                        images = images
                    )

                    val trackData = FriendsDataFragment.TrackData(
                        name = track["name"] as? String ?: "",
                        artists = track["artist"] as? String ?: "",
                        album = album,
                        id = track["id"] as? String ?: ""
                    )

                    Log.d("FriendManager", """
                    Created track:
                    Name: ${trackData.name}
                    Artist: ${trackData.artists}
                    Album: ${trackData.album.name}
                    Has images: ${trackData.album.images?.isNotEmpty()}
                    Image URL: ${trackData.getImage()}
                """.trimIndent())

                    topTracks.add(trackData)

                } catch (e: Exception) {
                    Log.e("FriendManager", "Error parsing track: ${e.message}")
                    e.printStackTrace()
                }
            }

        } catch (e: Exception) {
            Log.e("FriendManager", "Error getting friend data: ${e.message}")
            e.printStackTrace()
        }

        Log.d("FriendManager", "Returning ${topTracks.size} tracks")
        return topTracks.take(10) as ArrayList<FriendsDataFragment.TrackData>
    }

    /**
     * Queries Firestore DB for names of a user's friends and updates friendList
     */
    suspend fun getFriends() {
        val result = db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .await()

        if (result.isEmpty) {
            Log.d("Error", "No user found with username: $username")
        } else {
            val list = result.documents[0].get("friends") as List<*>
            for (friend in list) {
                friendList.add(friend.toString())
            }
        }
    }
}