package cz.pravda.trueegame.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cz.pravda.trueegame.R
import cz.pravda.trueegame.data.AppDatabase
import cz.pravda.trueegame.data.MemoryCard
import cz.pravda.trueegame.data.Score
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Změna: Dědíme z AndroidViewModel, abychom měli přístup k databázi
class MemoryGameViewModel(application: Application) : AndroidViewModel(application) {

    // --- DATABÁZE ---
    private val database = AppDatabase.getDatabase(application)
    private val scoreDao = database.scoreDao()

    // LiveData s nejlepším skóre (čte se samo z databáze)
    val bestScore: LiveData<Int?> = scoreDao.getBestScore().asLiveData()
    // ----------------

    private val images = listOf(
        R.drawable.ic_launcher_foreground, // Tady si nech své ikonky!
        R.drawable.ic_launcher_foreground,
        R.drawable.ic_launcher_foreground,
        R.drawable.ic_launcher_foreground,
        R.drawable.ic_launcher_foreground,
        R.drawable.ic_launcher_foreground,
        R.drawable.ic_launcher_foreground,
        R.drawable.ic_launcher_foreground
    )

    private val _cards = MutableLiveData<List<MemoryCard>>()
    val cards: LiveData<List<MemoryCard>> = _cards

    private val _moves = MutableLiveData<Int>(0)
    val moves: LiveData<Int> = _moves

    private var indexOfSingleSelectedCard: Int? = null
    private var isWaiting = false
    private var pairsMatched = 0 // Počítadlo hotových párů

    init {
        resetGame()
    }

    fun resetGame() {
        _moves.value = 0
        pairsMatched = 0
        indexOfSingleSelectedCard = null
        isWaiting = false

        val chosenImages = images.take(8)
        val randomizedImages = (chosenImages + chosenImages).shuffled()

        val newCards = randomizedImages.mapIndexed { index, imageId ->
            MemoryCard(id = index, imageId = imageId)
        }

        _cards.value = newCards
    }

    fun flipCard(position: Int) {
        val currentCards = _cards.value?.toMutableList() ?: return
        val card = currentCards[position]

        if (isWaiting || card.isFaceUp || card.isMatched) {
            return
        }

        card.isFaceUp = true
        _cards.value = currentCards
        _moves.value = (_moves.value ?: 0) + 1

        if (indexOfSingleSelectedCard == null) {
            indexOfSingleSelectedCard = position
        } else {
            checkForMatch(indexOfSingleSelectedCard!!, position, currentCards)
            indexOfSingleSelectedCard = null
        }
    }

    private fun checkForMatch(pos1: Int, pos2: Int, currentCards: MutableList<MemoryCard>) {
        if (currentCards[pos1].imageId == currentCards[pos2].imageId) {
            currentCards[pos1].isMatched = true
            currentCards[pos2].isMatched = true
            _cards.value = currentCards

            // Zvýšíme počet hotových párů
            pairsMatched++

            // Pokud máme 8 párů, je konec hry -> ULOŽIT DO DB
            if (pairsMatched == 8) {
                saveScoreToDb()
            }

        } else {
            isWaiting = true
            viewModelScope.launch {
                delay(1000)
                currentCards[pos1].isFaceUp = false
                currentCards[pos2].isFaceUp = false
                _cards.value = currentCards
                isWaiting = false
            }
        }
    }

    private fun saveScoreToDb() {
        val currentMoves = _moves.value ?: 0
        viewModelScope.launch {
            scoreDao.insert(Score(moves = currentMoves, timestamp = System.currentTimeMillis()))
        }
    }
}