package com.cs407.statify

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FriendManager(val username: String, val friendList: ArrayList<String>, private val context: Context) {
    private val db = Firebase.firestore

    /**
     * Search Firestore DB for users to add as friend,
     *
     * @param userToSearch name of user to add as friend
     *
     * @return ArrayList<Strings> of matching users, null if no matching users
     */
    suspend fun searchForFriend(userToSearch: String) : String {
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
     *
     * @param userToAdd name of user to add in database
     *
     */
    suspend fun addFriend(userToAdd: String){
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
     *
     * @param userToRemove name of user to add in database
     *
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
     *
     * @param username name of user in database
     *
     */
    private suspend fun getFriendData(username: String) : ArrayList<String> {
        val topTracks: ArrayList<String> = ArrayList()
        val result = db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .await()

        if (result.isEmpty) {
            Log.d("Error", "No user found with username: $username")
        } else {
            val tracks = result.documents[0].get("topTracks") as? List<HashMap<String, Any>>
            if (tracks != null) {
                for (track in tracks) {
                    topTracks.add(track["name"].toString())
                }
            }
        }
        return topTracks
    }

    /**
     * Queries Firestore DB for names of a user's friends and sets friendList accordingly
     *
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