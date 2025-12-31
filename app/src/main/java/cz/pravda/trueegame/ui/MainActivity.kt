package cz.pravda.trueegame.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cz.pravda.trueegame.R
import cz.pravda.trueegame.viewmodel.MemoryGameViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var rvBoard: RecyclerView
    private lateinit var tvScore: TextView
    private lateinit var tvBestScore: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvLastGame: TextView
    private lateinit var tvLives: TextView
    private lateinit var tvPreviewInfo: TextView

    private lateinit var viewModel: MemoryGameViewModel
    private lateinit var adapter: MemoryCardAdapter
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        soundManager = SoundManager(this)

        val mainView = findViewById<ConstraintLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rvBoard = findViewById(R.id.rv_board)
        tvScore = findViewById(R.id.tv_score)
        tvBestScore = findViewById(R.id.tv_best_score)
        tvTimer = findViewById(R.id.tv_timer)
        tvLastGame = findViewById(R.id.tv_last_game)
        tvLives = findViewById(R.id.tv_lives)
        tvPreviewInfo = findViewById(R.id.tv_preview_info)

        val btnBackIngame = findViewById<Button>(R.id.btn_back_ingame)
        val layoutGameOver = findViewById<ConstraintLayout>(R.id.layout_game_over)
        val tvGameOverTitle = findViewById<TextView>(R.id.tv_game_over_title)
        val tvStatPairs = findViewById<TextView>(R.id.tv_stat_pairs)
        val tvStatTime = findViewById<TextView>(R.id.tv_stat_time)
        val tvStatSpeed = findViewById<TextView>(R.id.tv_stat_speed)
        val btnPlayAgain = findViewById<Button>(R.id.btn_play_again)
        val btnBackToMenu = findViewById<Button>(R.id.btn_back_to_menu)

        val rows = intent.getIntExtra("ROWS", 4)
        val cols = intent.getIntExtra("COLS", 4)
        val mode = intent.getStringExtra("MODE") ?: "CLASSIC"
        val limit = intent.getLongExtra("TIME_LIMIT", 0L)

        viewModel = ViewModelProvider(this)[MemoryGameViewModel::class.java]

        if (savedInstanceState == null) {
            viewModel.setupGame(rows, cols, mode, limit)
        }

        if (mode == "MEMORY") {
            tvLives.visibility = View.VISIBLE
        } else {
            tvLives.visibility = View.GONE
        }

        adapter = MemoryCardAdapter(emptyList(), cols, rows) { position ->
            val wasFlipped = viewModel.flipCard(position)
            if (wasFlipped) {
                soundManager.playFlip()
            }
        }

        rvBoard.adapter = adapter

        rvBoard.layoutManager = object : GridLayoutManager(this, cols) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
        rvBoard.overScrollMode = View.OVER_SCROLL_NEVER

        viewModel.cards.observe(this) { newCards ->
            adapter.updateData(newCards)
        }

        viewModel.moves.observe(this) { moves ->
            tvScore.text = "Tahů: $moves"
        }

        viewModel.timeSeconds.observe(this) { seconds ->
            val minutes = seconds / 60
            val secs = seconds % 60
            tvTimer.text = String.format("Čas: %02d:%02d", minutes, secs)
        }

        var previousPairs = 0
        viewModel.pairsMatched.observe(this) { currentPairs ->
            if (currentPairs > previousPairs) {
                soundManager.playMatch()
            }
            previousPairs = currentPairs
        }

        viewModel.previewTime.observe(this) { timeLeft ->
            if (timeLeft > 0) {
                tvPreviewInfo.visibility = View.VISIBLE
                tvPreviewInfo.text = "$timeLeft"
            } else {
                tvPreviewInfo.visibility = View.GONE
            }
        }

        var previousLives = -1
        viewModel.lives.observe(this) { livesLeft ->
            if (previousLives != -1) {
                if (livesLeft < previousLives) {
                    soundManager.playLifeLost()
                }
            }
            previousLives = livesLeft

            if (mode == "MEMORY") {
                val hearts = StringBuilder()
                repeat(livesLeft) { hearts.append("❤️") }
                if (livesLeft == 0) {
                    tvLives.text = "☠️"
                } else {
                    tvLives.text = hearts.toString()
                }
            }
        }

        viewModel.bestScore.observe(this) { best ->
            tvBestScore.text = if (best != null) "Rekord: $best tahů" else "Rekord: -"
        }

        viewModel.lastGame.observe(this) { score ->
            if (score != null) {
                val minutes = score.timeSeconds / 60
                val secs = score.timeSeconds % 60
                tvLastGame.text = String.format("Minule: %d tahů (%02d:%02d)", score.moves, minutes, secs)
            } else {
                tvLastGame.text = "Minule: -"
            }
        }

        viewModel.isGameOver.observe(this) { isOver ->
            if (isOver) {
                layoutGameOver.visibility = View.VISIBLE
                tvPreviewInfo.visibility = View.GONE

                val pairsFound = viewModel.pairsMatched.value ?: 0
                val totalPairs = viewModel.totalPairsNeeded
                val finalTimeDisplay = viewModel.timeSeconds.value ?: 0

                var realTimeTaken = finalTimeDisplay
                if (mode == "TIME") {
                    realTimeTaken = limit - finalTimeDisplay
                    if (realTimeTaken < 0) realTimeTaken = 0
                }

                val lives = viewModel.lives.value ?: 0

                if (pairsFound == totalPairs) {
                    tvGameOverTitle.text = "VÍTĚZSTVÍ!"
                    tvGameOverTitle.setTextColor(Color.parseColor("#4CAF50"))
                    soundManager.playWin()
                } else {
                    tvGameOverTitle.setTextColor(Color.RED)
                    soundManager.playLose()

                    if (mode == "TIME" && finalTimeDisplay == 0L) {
                        tvGameOverTitle.text = "ČAS VYPRŠEL!"
                    } else if (mode == "MEMORY" && lives == 0) {
                        tvGameOverTitle.text = "DOŠLY ŽIVOTY!"
                    } else {
                        tvGameOverTitle.text = "KONEC HRY"
                    }
                }

                tvStatPairs.text = "$pairsFound / $totalPairs"
                tvStatTime.text = "${realTimeTaken}s"

                if (pairsFound > 0) {
                    val speed = realTimeTaken.toFloat() / pairsFound
                    tvStatSpeed.text = String.format("%.1f s/pár", speed)
                } else {
                    tvStatSpeed.text = "- s/pár"
                }

            } else {
                layoutGameOver.visibility = View.GONE
            }
        }

        btnPlayAgain.setOnClickListener {
            soundManager.playClick()
            previousPairs = 0
            previousLives = -1
            viewModel.resetGame()
        }

        btnBackToMenu.setOnClickListener {
            soundManager.playClick()
            finish()
        }

        btnBackIngame.setOnClickListener {
            soundManager.playClick()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}