package com.cs407.statify

import android.app.ActionBar.LayoutParams
import android.graphics.Color
import android.icu.text.ListFormatter.Width
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
import androidx.core.view.marginTop
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
    lateinit var cardContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_friends, container, false)

        cardContainer = view.findViewById<LinearLayout>(R.id.cardContainer)

        val searchButton = view.findViewById<ImageButton>(R.id.searchButton)

        val context = requireContext()

        searchButton.setOnClickListener {
            handleSearch(view)
        }

        //val cardData = listOf("Friend 1", "Friend 2", "Friend 3", "Friend 4", "Friend 5", "Friend 6", "Friend 7", "Friend 8")
        CoroutineScope(Dispatchers.Main).launch {
            val username = "Collin K"
            friendManager = FriendManager(username, ArrayList<String>(), context)
            friendManager.getFriends()
            populateCards()
        }

        return view
    }

    /**
     * (Re)populates Friends page
     *
     */
    private fun populateCards() {
        for (friend in friendManager.friendList) {
            addCardView(cardContainer, friend, true)
        }
    }

    /**
     * Handles user clicking search button
     *
     * @param view current view
     *
     */
    private fun handleSearch(view: View){
        val inputField = view.findViewById<EditText>(R.id.inputField)
        val text = inputField.text.toString().trim()
        CoroutineScope(Dispatchers.Main).launch {
            val friend = friendManager.searchForFriend(text)
            if (friend != "") {
                clearCards()
                val cardContainer = view.findViewById<LinearLayout>(R.id.cardContainer)
                addCardView(cardContainer, friend, false)
            }
        }
    }

    /**
     * Gets profile image for specified user
     *
     * @param friendName name of user whose profile picture to get
     *
     */
    private fun getImage(friendName: String) {
        TODO()
    }

    /**
     * Clears all current CardViews
     *
     */
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
     * @param parent parent LinearLayout for CardView
     * @param friend name of friend
     * @param remove specifies whether cards are remove or add
     *
     */
    private fun addCardView(parent: LinearLayout, friend: String, remove: Boolean) {

        val action: String
        val color: Int
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

        val cardLayoutVertical = LinearLayout(requireContext())
        cardLayoutVertical.orientation = LinearLayout.VERTICAL
        val vertParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT,
        )
        cardLayoutVertical.gravity = Gravity.CENTER
        cardLayoutVertical.layoutParams = vertParams


        //val imageView = ImageView(requireContext())

        val textView = TextView(requireContext())
        textView.width = LayoutParams.MATCH_PARENT
        textView.text = friend
        textView.textSize = 30f
        textView.setPadding(10, 10, 10, 5)
        textView.gravity = Gravity.CENTER
        val textColor = context?.getColor(R.color.spotify_green) ?: Color.CYAN
        textView.setTextColor(textColor)


        val buttonView = Button(requireContext())
        if (remove){
            action = "Remove Friend"
            color = Color.RED
            buttonView.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    friendManager.removeFriend(friend)
                    clearCards()
                    populateCards()
                }
            }
        } else {
            action = "Add Friend"
            color = Color.WHITE
            buttonView.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    friendManager.addFriend(friend)
                    clearCards()
                    populateCards()
                }
            }
        }
        buttonView.text = action
        buttonView.setBackgroundColor(Color.TRANSPARENT)
        buttonView.setTextColor(color)
        buttonView.gravity = Gravity.CENTER
        buttonView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        cardView.addView(cardLayoutVertical)
        cardLayoutVertical.addView(textView)
        cardLayoutVertical.addView(buttonView)
        //cardView.addView(imageView)
        cardView.setOnClickListener {
            Log.d("CARD CLICKED!", friend)
        }

        parent.addView(cardView)
    }

}
