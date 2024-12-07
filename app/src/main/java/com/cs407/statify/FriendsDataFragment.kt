package com.cs407.statify

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Serializable

class FriendsDataFragment : Fragment() {

    private lateinit var friendName: String
    private lateinit var friendManager: FriendManager
    private lateinit var topTracks: ArrayList<String>

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

        loadFriendData(view)
    }

    private fun loadFriendData(view: View) {

        friendName = arguments?.getString("friend") ?: ""
        friendManager = arguments?.getSerializable("friendManager") as? FriendManager ?: FriendManager("", ArrayList<String>(), requireContext())

        val textView = view.findViewById<TextView>(R.id.friendName)
        textView.text = friendName
        val button = view.findViewById<ImageButton>(R.id.backButton)
        button.setOnClickListener {
            findNavController().navigate(R.id.action_friendsDataFragment_to_friendsFragment)
        }

        TopTracksAdapter.TrackViewHolder(view)

        CoroutineScope(Dispatchers.Main).launch {
            topTracks = friendManager.getFriendData(friendName)
            Log.d("Logged!", topTracks.toString())
            val dataView = view.findViewById<TextView>(R.id.listeningData)
            dataView.text = topTracks.toString()
        }
    }
}