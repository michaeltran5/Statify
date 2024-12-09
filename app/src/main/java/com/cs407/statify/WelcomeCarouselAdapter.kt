package com.cs407.statify

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.text.Html

class WelcomeCarouselAdapter(private val dialog: Dialog) : RecyclerView.Adapter<WelcomeCarouselAdapter.SlideViewHolder>() {

    private val slides = listOf(
        Slide(Html.fromHtml("<font color='#1DB954'><b><big>Statify</big></b></font>",
            Html.FROM_HTML_MODE_LEGACY),"Your personal music stats dashboard\n\n" +
                "Here's what you can do.",false),
        Slide("Track Your Music Journey", "Discover your top artists, songs, and " +
                "genres throughout the year. See how your music taste evolves over time.", false),
        Slide("Connect with Friends", "Add friends to explore their music stats and " +
                "see what they're listening to. Compare tastes and discover new music together.", false),
        Slide("Thank You!", "Thank you for choosing Statify.\n\nWe hope you find " +
                "everything you need.\n\nNow let's see those stats!", true)
    )

    data class Slide(val title: CharSequence, val subtitle: String, val showButton: Boolean)

    inner class SlideViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleText: TextView = view.findViewById(R.id.titleText)
        private val subtitleText: TextView = view.findViewById(R.id.subtitleText)
        private val viewStatsButton: Button = view.findViewById(R.id.viewStatsButton)

        fun bind(slide: Slide) {
            titleText.text = slide.title
            subtitleText.text = slide.subtitle
            viewStatsButton.visibility = if (slide.showButton) View.VISIBLE else View.GONE
            viewStatsButton.setOnClickListener {
                dialog.dismiss()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        return SlideViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_welcome_slide, parent, false)
        )
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount() = slides.size
}