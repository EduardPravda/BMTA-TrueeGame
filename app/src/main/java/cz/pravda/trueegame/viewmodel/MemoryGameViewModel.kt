package cz.pravda.trueegame.viewmodel

import android.app.Application
import android.content.Context
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

    // --- SADA 1: IT IKONY ---
    private val imagesSet1 = listOf(
        R.drawable.iticonsand,
        R.drawable.iticonsdolar,
        R.drawable.iticonshash,
        R.drawable.iticonslevazavorka,
        R.drawable.iticonsminus,
        R.drawable.iticonsplus,
        R.drawable.iticonspodtrzitko,
        R.drawable.iticonspravazavorka,
        R.drawable.iticonsprocento,
        R.drawable.iticonszavinac
    )

    // --- SADA 2: ČÍSLA ---
    private val imagesSet2 = listOf(
        R.drawable.numbericonsone,
        R.drawable.numbericonstwo,
        R.drawable.numbericonsthree,
        R.drawable.numbericonsfour,
        R.drawable.numbericonsfive,
        R.drawable.numbericonssix,
        R.drawable.numbericonsseven,
        R.drawable.numbericonseight,
        R.drawable.numbericonsnein,
        R.drawable.numbericonsten
    )

    // --- SADA 3: BARVY ---
    private val imagesSet3 = listOf(
        R.drawable.colouriconsblack,
        R.drawable.colouriconsblue,
        R.drawable.colouriconsgreen,
        R.drawable.colouriconsgrey,
        R.drawable.colouriconsorange,
        R.drawable.colouriconspink,
        R.drawable.colouriconspurple,
        R.drawable.colouriconsred,
        R.drawable.colouriconswhite,
        R.drawable.colouriconsyellow
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
        _lives.value = if (gameMode == "MEMORY") 3 else 0
        _previewTime.value = 0
        stopTimer()

        val totalCards = gameRows * gameCols
        totalPairsNeeded = totalCards / 2

        val prefs = getApplication<Application>().getSharedPreferences("GAME_PREFS", Context.MODE_PRIVATE)
        val selectedSet = prefs.getInt("CARD_SET", 1)

        val sourceImages = when (selectedSet) {
            2 -> imagesSet2
            3 -> imagesSet3
            else -> imagesSet1
        }

        val availableImages = ArrayList<Int>()
        while (availableImages.size < totalPairsNeeded) {
            availableImages.addAll(sourceImages)
        }
        val chosenImages = availableImages.take(totalPairsNeeded)
        val randomizedImages = (chosenImages + chosenImages).shuffled()

        var newCards = randomizedImages.mapIndexed { index, imageId ->
            MemoryCard(id = index, imageId = imageId, isFaceUp = false, isMatched = false)
        }

        if (gameMode == "MEMORY") {
            _timeSeconds.value = 0
            newCards = newCards.map { it.copy(isFaceUp = true) }
            _cards.value = newCards

            isPreviewing = true
            isGameRunning = false

            viewModelScope.launch {
                val previewDuration = gameTimeLimit.toInt()
                for (i in previewDuration downTo 1) {
                    _previewTime.postValue(i)
                    delay(1000)
                }
                _previewTime.postValue(0)

                val hiddenCards = _cards.value!!.map { it.copy(isFaceUp = false) }
                _cards.value = hiddenCards

                isPreviewing = false
                startTimer()
            }
        }
        else if (gameMode == "TIME") {
            _cards.value = newCards
            _timeSeconds.value = gameTimeLimit
            startTimer()
        }
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

    fun flipCard(position: Int): Boolean {
        val currentCards = _cards.value?.toMutableList() ?: return false

        if (!isGameRunning && !isPreviewing) return false
        if (isPreviewing) return false
        if (isWaiting) return false
        if (position >= currentCards.size) return false

        val card = currentCards[position]

        if (card.isFaceUp || card.isMatched) return false

        card.isFaceUp = true
        _cards.value = currentCards

        _moves.value = (_moves.value ?: 0) + 1

        if (indexOfSingleSelectedCard == null) {
            indexOfSingleSelectedCard = position
        } else {
            checkForMatch(indexOfSingleSelectedCard!!, position, currentCards)
            indexOfSingleSelectedCard = null
        }
        return true
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
        val lives = if (gameMode == "MEMORY") (_lives.value ?: 0) else 0

        viewModelScope.launch {
            scoreDao.insert(
                Score(
                    moves = currentMoves,
                    timeSeconds = timePlayed,
                    timestamp = System.currentTimeMillis(),
                    mode = gameMode,
                    rows = gameRows,
                    cols = gameCols,
                    livesLeft = lives,
                    timeLimit = gameTimeLimit
                )
            )
        }
    }
}