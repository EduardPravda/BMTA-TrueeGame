package cz.pravda.trueegame.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import cz.pravda.trueegame.R
import cz.pravda.trueegame.data.MemoryCard
import kotlin.math.min

class MemoryCardAdapter(
    private var cards: List<MemoryCard>,
    private val gridCols: Int,
    private val gridRows: Int,
    private val onCardClicked: (Int) -> Unit
) : RecyclerView.Adapter<MemoryCardAdapter.CardViewHolder>() {

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardImage: ImageView = itemView.findViewById(R.id.card_image)
        val cardView: CardView = itemView.findViewById(R.id.card_view)

        fun bind(card: MemoryCard) {
            itemView.setOnClickListener {
                onCardClicked(adapterPosition)
            }

            if (card.isFaceUp || card.isMatched) {
                cardImage.setImageResource(card.imageId)
                cardImage.alpha = if (card.isMatched) 0.4f else 1.0f
            } else {
                cardImage.setImageResource(R.drawable.ic_card_back)
                cardImage.alpha = 1.0f
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)

        val displayMetrics = parent.context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density

        val isLandscape = screenWidth > screenHeight

        // --- PROFESIONÁLNÍ VÝPOČET VELIKOSTI V3 (S rezervou pro výřez) ---

        // Portrait (Výška): Panel DOLE (cca 220dp) + okraje (32dp)
        // Landscape (Šířka): Panel VPRAVO (300dp) + VÍCE místa pro výřez (změna na 380dp) + svislá rezerva

        val marginVerticalDp = if (isLandscape) 40 else 220
        val marginHorizontalDp = if (isLandscape) 380 else 32

        // Poznámka: 380 = 300 (panel) + 80 (bezpečná zóna pro výřez kamery a zaoblení rohů)

        val marginVerticalPx = (marginVerticalDp * density).toInt()
        val marginHorizontalPx = (marginHorizontalDp * density).toInt()

        val safeWidth = screenWidth - marginHorizontalPx
        val safeHeight = screenHeight - marginVerticalPx

        // Spočítáme maximální velikost karty
        val sizeByWidth = safeWidth / gridCols
        val sizeByHeight = safeHeight / gridRows
        val cardMargin = (8 * density).toInt()

        val finalSize = min(sizeByWidth, sizeByHeight) - cardMargin
        val validatedSize = if (finalSize > 0) finalSize else 100

        view.layoutParams.width = validatedSize
        view.layoutParams.height = validatedSize

        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(cards[position])
    }

    override fun getItemCount(): Int = cards.size

    fun updateData(newCards: List<MemoryCard>) {
        cards = newCards
        notifyDataSetChanged()
    }
}