package cz.pravda.trueegame.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.pravda.trueegame.R
import cz.pravda.trueegame.data.MemoryCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MemoryGameViewModel : ViewModel() {

    // 1. Seznam obrázků pro pexeso.
    // Musíš mít v res/drawable vytvořené tyto ikonky (nebo použij jiné, co máš)
    private val images = listOf(
        R.drawable.ic_30, // Zatím tam dej tohle, pokud nemáš jiné
        R.drawable.ic_3g,
        R.drawable.ic_4g,
        R.drawable.ic_5g,
        R.drawable.ic_60,
        R.drawable.ic_air,
        R.drawable.ic_android,
        R.drawable.ic_plus
        // Ideálně si pak přidej Vector Assets: ic_face, ic_star, ic_pets atd.
    )

    // 2. LiveData - to co vidí MainActivity
    private val _cards = MutableLiveData<List<MemoryCard>>()
    val cards: LiveData<List<MemoryCard>> = _cards

    private val _moves = MutableLiveData<Int>(0)
    val moves: LiveData<Int> = _moves

    // Pomocné proměnné
    private var indexOfSingleSelectedCard: Int? = null // Index první otočené karty
    private var isWaiting = false // Aby uživatel nemohl klikat, když se karty otáčí zpět

    init {
        resetGame()
    }

    // 3. Start nové hry
    fun resetGame() {
        _moves.value = 0
        indexOfSingleSelectedCard = null
        isWaiting = false

        // Vezmeme 8 obrázků, zdvojíme je (aby byly páry) a zamícháme
        val chosenImages = images.take(8)
        val randomizedImages = (chosenImages + chosenImages).shuffled()

        // Vytvoříme karty
        val newCards = randomizedImages.mapIndexed { index, imageId ->
            MemoryCard(id = index, imageId = imageId)
        }

        _cards.value = newCards
    }

    // 4. Hlavní logika - co se stane po kliknutí
    fun flipCard(position: Int) {
        // Získáme aktuální seznam karet
        val currentCards = _cards.value?.toMutableList() ?: return
        val card = currentCards[position]

        // Kontroly: Neklikat, když se čeká, nebo když je karta už otočená/hotová
        if (isWaiting || card.isFaceUp || card.isMatched) {
            return
        }

        // Otočíme kartu nahoru
        card.isFaceUp = true
        _cards.value = currentCards // Aktualizujeme UI
        _moves.value = (_moves.value ?: 0) + 1 // Přičteme tah

        // Logika párování
        if (indexOfSingleSelectedCard == null) {
            // Byla to první karta z dvojice
            indexOfSingleSelectedCard = position
        } else {
            // Byla to druhá karta - zkontrolujeme shodu
            checkForMatch(indexOfSingleSelectedCard!!, position, currentCards)
            indexOfSingleSelectedCard = null
        }
    }

    private fun checkForMatch(pos1: Int, pos2: Int, currentCards: MutableList<MemoryCard>) {
        if (currentCards[pos1].imageId == currentCards[pos2].imageId) {
            // SHODA!
            currentCards[pos1].isMatched = true
            currentCards[pos2].isMatched = true
            _cards.value = currentCards

            // Tady by se dalo zkontrolovat vítězství (jsou všechny matched?)
        } else {
            // NESHODA - Musíme počkat a pak otočit zpět
            isWaiting = true // Zablokujeme klikání

            // Spustíme coroutine (vlákno) pro čekání
            viewModelScope.launch {
                delay(1000) // Počkáme 1 sekundu

                // Otočíme zpět
                currentCards[pos1].isFaceUp = false
                currentCards[pos2].isFaceUp = false
                _cards.value = currentCards

                isWaiting = false // Povolíme klikání
            }
        }
    }
}