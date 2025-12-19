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
    val lastGame: LiveData<Score?> = scoreDao.getLastGame().asLiveData()

    // Seznam obrázků (8 stačí pro mřížku 4x4)
    private val images = listOf(
        R.drawable.ic_30,
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

    private val _timeSeconds = MutableLiveData<Long>(0)
    val timeSeconds: LiveData<Long> = _timeSeconds

    // Herní stavy
    private var indexOfSingleSelectedCard: Int? = null
    private var isWaiting = false
    private var pairsMatched = 0

    // Konfigurace hry (výchozí hodnoty)
    private var gameRows = 4
    private var gameCols = 4
    private var gameMode = "CLASSIC"
    private var pairsNeeded = 8 // Kolik párů musíme najít k výhře
    private var gameTimeLimit: Long = 60

    // Časovač
    private var timerJob: Job? = null
    private var isGameRunning = false

    init {
        // Spustíme hru s výchozím nastavením (4x4 Classic), aby aplikace nespadla při prvním startu
        resetGame()
    }

    fun setupGame(rows: Int, cols: Int, mode: String, timeLimit: Long) {
        gameRows = rows
        gameCols = cols
        gameMode = mode
        gameTimeLimit = timeLimit

        resetGame()
    }

    fun resetGame() {
        // 1. Reset proměnných
        _moves.value = 0
        pairsMatched = 0
        indexOfSingleSelectedCard = null
        isWaiting = false

        // 2. Nastavení času podle módu
        stopTimer()
        if (gameMode == "TIME") {
            _timeSeconds.value = gameTimeLimit
        } else {
            _timeSeconds.value = 0
        }

        // 3. Příprava karet podle velikosti pole
        val totalCards = gameRows * gameCols
        pairsNeeded = totalCards / 2

        // Vezmeme jen tolik obrázků, kolik potřebujeme (např. pro 4x3 potřebujeme 6 obrázků)
        // Používáme 'take', abychom aplikaci neshodili, pokud by chtěla víc obrázků než máme
        val chosenImages = images.take(pairsNeeded)
        val randomizedImages = (chosenImages + chosenImages).shuffled()

        val newCards = randomizedImages.mapIndexed { index, imageId ->
            MemoryCard(id = index, imageId = imageId)
        }
        _cards.value = newCards

        // 4. Spuštění stopek
        startTimer()
    }

    // <--- UPRAVENO: Časovač počítá nahoru nebo dolů
    private fun startTimer() {
        isGameRunning = true
        timerJob = viewModelScope.launch {
            while (isGameRunning) {
                delay(1000)
                val currentTime = _timeSeconds.value ?: 0

                if (gameMode == "TIME") {
                    // Odpočítávání
                    if (currentTime > 0) {
                        _timeSeconds.value = currentTime - 1
                    } else {
                        // Čas vypršel -> Konec hry (Prohra)
                        stopTimer()
                        // Zde bys mohl přidat logiku pro zobrazení "Game Over"
                    }
                } else {
                    // Klasické přičítání
                    _timeSeconds.value = currentTime + 1
                }
            }
        }
    }

    private fun stopTimer() {
        isGameRunning = false
        timerJob?.cancel()
    }

    fun flipCard(position: Int) {
        val currentCards = _cards.value?.toMutableList() ?: return

        // Ochrana proti kliknutí mimo rozsah nebo když hra neběží (např. vypršel čas)
        if (position >= currentCards.size || (!isGameRunning && gameMode == "TIME" && (_timeSeconds.value ?: 0) == 0L)) {
            return
        }

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

            // <--- UPRAVENO: Kontrola výhry je dynamická (ne natvrdo 8)
            if (pairsMatched == pairsNeeded) {
                stopTimer()
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
        // Ukládáme čas. Pokud to byla časovka, uložíme zbývající čas, nebo dobu trvání?
        // Pro jednoduchost ukládáme aktuální hodnotu na displeji.
        val currentTime = _timeSeconds.value ?: 0

        viewModelScope.launch {
            scoreDao.insert(
                Score(
                    moves = currentMoves,
                    timeSeconds = currentTime,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}