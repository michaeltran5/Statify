package com.cs407.statify

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.cs407.statify.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

class FriendsFragment : Fragment() {

    lateinit var friendManager: FriendManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_friends, container, false)

        val cardContainer = view.findViewById<LinearLayout>(R.id.cardContainer)

        val searchButton = view.findViewById<ImageButton>(R.id.searchButton)

        val context = requireContext()

        searchButton.setOnClickListener {
            handleSearch(view)
            Toast.makeText(context, "Search button clicked!", Toast.LENGTH_SHORT).show()
        }

        //val cardData = listOf("Friend 1", "Friend 2", "Friend 3", "Friend 4", "Friend 5", "Friend 6", "Friend 7", "Friend 8")
        CoroutineScope(Dispatchers.Main).launch {
            val username = "Collin K"
            Log.d("Username", username)
            friendManager = FriendManager(username, ArrayList<String>(), context)
            friendManager.displayFriends(username)
            Log.d("MADE IT TO HERE!!!!!!!!!", friendManager.friendList.toString())
            for (friend in friendManager.friendList) {
                addCardView(cardContainer, friend)
            }
        }

        return view
    }

    private fun handleSearch(view: View){
        val inputField = view.findViewById<EditText>(R.id.inputField)
        val text = inputField.text.toString().trim()
        CoroutineScope(Dispatchers.Main).launch {
            val friend = friendManager.searchForFriend(text)
            if (friend != "") {
                clearCards()
                val cardContainer = view.findViewById<LinearLayout>(R.id.cardContainer)
                addCardView(cardContainer, friend)
            }
        }
    }

    private fun getImage() {

    }

    private fun clearCards() {
        val cardContainer = view?.findViewById<LinearLayout>(R.id.cardContainer)
        var i = 0
        val count = cardContainer?.childCount ?: 0
        while (i < count) {
            val child = cardContainer?.getChildAt(i)
            if (child is CardView) {
                cardContainer.removeViewAt(i)
            } else {
                i++
            }
        }
    }

    /**
     * Creates CardView for friend
     *
     * @param parent Parent LinearLayout for CardView
     * @param friend name of friend
     *
     */
    private fun addCardView(parent: LinearLayout, friend: String) {

        val cardView = CardView(requireContext())

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        layoutParams.setMargins(0, 10, 0, 10)
        cardView.layoutParams = layoutParams

        cardView.cardElevation = 8f
        val cardColor = context?.getColor(R.color.dark_grey) ?: Color.CYAN
        cardView.setCardBackgroundColor(cardColor)
        cardView.radius = 12f

        //val imageView = ImageView(requireContext())

        val textView = TextView(requireContext())
        textView.text = friend
        textView.textSize = 30f
        textView.setPadding(10, 10, 10, 10)
        textView.gravity = Gravity.CENTER
        val textColor = context?.getColor(R.color.spotify_green) ?: Color.CYAN
        textView.setTextColor(textColor)

        cardView.addView(textView)
        //cardView.addView(imageView)
        cardView.setOnClickListener {
            Log.d("CARD CLICKED!", friend)
        }

        parent.addView(cardView)
    }

}
