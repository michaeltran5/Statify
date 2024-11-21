package com.cs407.statify

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.cs407.statify.R

class FriendsFragment : Fragment() {
    private lateinit var friendsListText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_friends, container, false)

        val cardContainer = view.findViewById<LinearLayout>(R.id.cardContainer)

        val cardData = listOf("Friend 1", "Friend 2", "Friend 3", "Friend 4", "Friend 5")

        Log.d("MADE IT TO HERE!!!!!!!!!", cardData.toString())
        for (card in cardData) {
            addCardView(cardContainer, card.toString())
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        friendsListText = view.findViewById(R.id.friendsListText)
        // Add your logic to populate friends data
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

        val textView = TextView(requireContext())
        textView.text = friend
        textView.textSize = 30f
        textView.setPadding(10, 10, 10, 10)
        textView.gravity = Gravity.CENTER
        val textColor = context?.getColor(R.color.spotify_green) ?: Color.CYAN
        textView.setTextColor(textColor)

        cardView.addView(textView)
        cardView.setOnClickListener {
            Log.d("CARD CLICKED!", friend)
        }

        parent.addView(cardView)
    }
}
