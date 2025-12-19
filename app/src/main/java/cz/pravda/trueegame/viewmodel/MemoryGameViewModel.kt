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

    private val images = listOf(
        R.drawable.ic_30,
        R.drawable.ic_3g,
        R.drawable.ic_4g,
        R.drawable.ic_5g,
        R.drawable.ic_60,
        R.drawable.ic_air,
        R.drawable.ic_android,
        R.drawable.ic_plus,
        android.R.drawable.ic_menu_camera,
        android.R.drawable.ic_menu_call
    )

    private val _cards = MutableLiveData<List<MemoryCard>>()
    val cards: LiveData<List<MemoryCard>> = _cards

    private val _moves = MutableLiveData<Int>(0)
    val moves: LiveData<Int> = _moves

    private val _timeSeconds = MutableLiveData<Long>(0)
    val timeSeconds: LiveData<Long> = _timeSeconds

    private val _isGameOver = MutableLiveData<Boolean>(false)
    val isGameOver: LiveData<Boolean> = _isGameOver

    private val _lives = MutableLiveData<Int>(3)
    val lives: LiveData<Int> = _lives

    // NOVÉ: Odpočet pro náhled (Memory mód)
    private val _previewTime = MutableLiveData<Int>(0)
    val previewTime: LiveData<Int> = _previewTime

    val pairsMatched = MutableLiveData<Int>(0)
    var totalPairsNeeded = 8

    private var indexOfSingleSelectedCard: Int? = null
    private var isWaiting = false
    private var isPreviewing = false

    private var gameRows = 4
    private var gameCols = 4
    private var gameMode = "CLASSIC"
    private var gameTimeLimit: Long = 60

    private var timerJob: Job? = null
    private var isGameRunning = false

    fun setupGame(rows: Int, cols: Int, mode: String, timeLimit: Long) {
        gameRows = rows
        gameCols = cols
        gameMode = mode
        gameTimeLimit = timeLimit
        resetGame()
    }

    fun resetGame() {
        _isGameOver.value = false
        _moves.value = 0
        pairsMatched.value = 0
        indexOfSingleSelectedCard = null
        isWaiting = false
        isPreviewing = false
        _lives.value = 3
        _previewTime.value = 0 // Reset odpočtu
        stopTimer()

        val totalCards = gameRows * gameCols
        totalPairsNeeded = totalCards / 2

        val availableImages = ArrayList<Int>()
        while (availableImages.size < totalPairsNeeded) {
            availableImages.addAll(images)
        }
        val chosenImages = availableImages.take(totalPairsNeeded)
        val randomizedImages = (chosenImages + chosenImages).shuffled()

        var newCards = randomizedImages.mapIndexed { index, imageId ->
            MemoryCard(id = index, imageId = imageId, isFaceUp = false, isMatched = false)
        }

        // --- MÓD PAMĚŤ ---
        if (gameMode == "MEMORY") {
            _timeSeconds.value = 0

            // Otočíme karty
            newCards = newCards.map { it.copy(isFaceUp = true) }
            _cards.value = newCards

            isPreviewing = true
            isGameRunning = false

            // Cyklus pro odpočet (místo jednoho delay)
            viewModelScope.launch {
                val previewDuration = gameTimeLimit.toInt()

                // Odpočítáváme dolů: 5, 4, 3, 2, 1...
                for (i in previewDuration downTo 1) {
                    _previewTime.postValue(i)
                    delay(1000)
                }
                _previewTime.postValue(0) // Konec odpočtu

                // Skryjeme karty
                val hiddenCards = _cards.value!!.map { it.copy(isFaceUp = false) }
                _cards.value = hiddenCards

                isPreviewing = false
                startTimer() // Spustíme herní čas
            }
        }
        // --- MÓD ČASOVKA ---
        else if (gameMode == "TIME") {
            _cards.value = newCards
            _timeSeconds.value = gameTimeLimit
            startTimer()
        }
        // --- KLASIKA ---
        else {
            _cards.value = newCards
            _timeSeconds.value = 0
            startTimer()
        }
    }

    private fun startTimer() {
        isGameRunning = true
        timerJob = viewModelScope.launch {
            while (isGameRunning) {
                delay(1000)
                val currentTime = _timeSeconds.value ?: 0

                if (gameMode == "TIME") {
                    if (currentTime > 0) {
                        _timeSeconds.value = currentTime - 1
                    } else {
                        stopTimer()
                        _isGameOver.postValue(true)
                    }
                } else {
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

        if (!isGameRunning && !isPreviewing) return
        if (isPreviewing) return
        if (isWaiting) return
        if (position >= currentCards.size) return

        val card = currentCards[position]

        if (card.isFaceUp || card.isMatched) return

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

            val currentPairs = pairsMatched.value ?: 0
            pairsMatched.value = currentPairs + 1

            if ((pairsMatched.value ?: 0) == totalPairsNeeded) {
                stopTimer()
                saveScoreToDb()
                _isGameOver.value = true
            }
        } else {
            // Chyba v módu Paměť
            if (gameMode == "MEMORY") {
                val currentLives = _lives.value ?: 3
                val newLives = currentLives - 1
                _lives.value = newLives

                if (newLives <= 0) {
                    stopTimer()
                    _isGameOver.value = true
                    return
                }
            }

            isWaiting = true
            viewModelScope.launch {
                delay(1000)
                val cardsToFlip = _cards.value?.toMutableList() ?: return@launch
                cardsToFlip[pos1].isFaceUp = false
                cardsToFlip[pos2].isFaceUp = false
                _cards.value = cardsToFlip
                isWaiting = false
            }
        }
    }

    private fun saveScoreToDb() {
        val currentMoves = _moves.value ?: 0
        val currentTime = _timeSeconds.value ?: 0
        val timePlayed = if (gameMode == "TIME") (gameTimeLimit - currentTime) else currentTime

        viewModelScope.launch {
            scoreDao.insert(
                Score(
                    moves = currentMoves,
                    timeSeconds = timePlayed,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}