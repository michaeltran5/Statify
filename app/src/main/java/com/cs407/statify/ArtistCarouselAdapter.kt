package com.cs407.statify
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ArtistCarouselAdapter(
    private val artists: List<Artist>,
    private val context: Context,
) : RecyclerView.Adapter<ArtistCarouselAdapter.ArtistViewHolder>() {

    inner class ArtistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val artistImageView: ImageView = itemView.findViewById(R.id.artistImageView)
        private val artistNameText: TextView = itemView.findViewById(R.id.artistNameText)
        private val genresText: TextView = itemView.findViewById(R.id.artistGenresText)

        fun bind(artist: Artist, position: Int) {
            artistNameText.text = "${position + 1}. ${artist.name}"

            genresText.text = artist.genres.take(2).joinToString(" • ")

            artist.images?.firstOrNull()?.let { image ->
                Glide.with(context)
                    .load(image.url)
                    .centerCrop()
                    .into(artistImageView)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_artist_carousel, parent, false)
        return ArtistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        holder.bind(artists[position], position)
    }

    override fun getItemCount() = artists.size
}