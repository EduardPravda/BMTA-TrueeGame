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
    private var cards: List<MemoryCard>,        // Seznam karet
    private val onCardClicked: (Int) -> Unit    // Co se má stát po kliknutí (funkce)
) : RecyclerView.Adapter<MemoryCardAdapter.CardViewHolder>() {

    // 1. Tato třída drží odkazy na prvky v item_card.xml
    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardImage: ImageView = itemView.findViewById(R.id.card_image)
        val cardView: CardView = itemView.findViewById(R.id.card_view)

        fun bind(card: MemoryCard) {
            // Kliknutí na kartu
            itemView.setOnClickListener {
                onCardClicked(adapterPosition)
            }

            // LOGIKA ZOBRAZENÍ:
            // Pokud je karta otočená (faceUp) NEBO už uhodnutá (matched), ukaž obrázek.
            // Jinak ukaž rub (ic_card_back).
            if (card.isFaceUp || card.isMatched) {
                cardImage.setImageResource(card.imageId)

                // Pokud je už spárovaná, trochu ji zprůhledníme, ať je to vidět
                cardImage.alpha = if (card.isMatched) 0.4f else 1.0f
            } else {
                // Rub karty
                cardImage.setImageResource(R.drawable.ic_card_back)
                cardImage.alpha = 1.0f
            }
        }
    }

    // 2. Vytvoření "okýnka" pro kartu
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)

        // Trik: Nastavíme výšku karty dynamicky, aby byla čtvercová
        // (Vydělíme šířku displeje počtem sloupců - zde 4)
        val cardSideLength = parent.width / 4 - (2 * 8) // Odečteme marginy
        view.layoutParams.height = cardSideLength
        view.layoutParams.width = cardSideLength

        return CardViewHolder(view)
    }

    // 3. Naplnění daty
    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(cards[position])
    }

    // 4. Kolik je karet?
    override fun getItemCount(): Int = cards.size

    // Pomocná funkce pro aktualizaci seznamu (až se změní ve ViewModelu)
    fun updateData(newCards: List<MemoryCard>) {
        cards = newCards
        notifyDataSetChanged()
    }
}