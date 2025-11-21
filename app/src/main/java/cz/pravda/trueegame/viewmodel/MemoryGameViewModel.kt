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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MemoryGameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val scoreDao = database.scoreDao()

    val bestScore: LiveData<Int?> = scoreDao.getBestScore().asLiveData()
    // <--- NOVÉ: Sledujeme poslední hru
    val lastGame: LiveData<Score?> = scoreDao.getLastGame().asLiveData()

    private val images = listOf(
        R.drawable.ic_30, // Zde nech své ikonky
        R.drawable.ic_3g,
        R.drawable.ic_4g,
        R.drawable.ic_5g,
        R.drawable.ic_60,
        R.drawable.ic_air,
        R.drawable.ic_android,
        R.drawable.ic_plus
    )

    private val _cards = MutableLiveData<List<MemoryCard>>()
    val cards: LiveData<List<MemoryCard>> = _cards

    private val _moves = MutableLiveData<Int>(0)
    val moves: LiveData<Int> = _moves

    // <--- NOVÉ: LiveData pro čas
    private val _timeSeconds = MutableLiveData<Long>(0)
    val timeSeconds: LiveData<Long> = _timeSeconds

    private var indexOfSingleSelectedCard: Int? = null
    private var isWaiting = false
    private var pairsMatched = 0

    // <--- NOVÉ: Proměnná pro stopky
    private var timerJob: Job? = null
    private var isGameRunning = false

    init {
        resetGame()
    }

    fun resetGame() {
        // Reset hodnot
        _moves.value = 0
        _timeSeconds.value = 0
        pairsMatched = 0
        indexOfSingleSelectedCard = null
        isWaiting = false

        // Zastavení starého časovače a spuštění nového
        stopTimer()
        startTimer()

        val chosenImages = images.take(8)
        val randomizedImages = (chosenImages + chosenImages).shuffled()

        val newCards = randomizedImages.mapIndexed { index, imageId ->
            MemoryCard(id = index, imageId = imageId)
        }
        _cards.value = newCards
    }

    // <--- NOVÉ: Funkce pro stopky
    private fun startTimer() {
        isGameRunning = true
        timerJob = viewModelScope.launch {
            while (isGameRunning) {
                delay(1000) // Počkáme 1 sekundu
                _timeSeconds.value = (_timeSeconds.value ?: 0) + 1
            }
        }
    }

    private fun stopTimer() {
        isGameRunning = false
        timerJob?.cancel()
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
            pairsMatched++

            if (pairsMatched == 8) {
                stopTimer() // <--- Zastavíme čas
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
        val currentTime = _timeSeconds.value ?: 0

        viewModelScope.launch {
            scoreDao.insert(
                Score(
                    moves = currentMoves,
                    timeSeconds = currentTime, // Ukládáme i čas
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}