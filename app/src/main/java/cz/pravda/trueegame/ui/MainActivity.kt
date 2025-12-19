package cz.pravda.trueegame.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cz.pravda.trueegame.R
import cz.pravda.trueegame.viewmodel.MemoryGameViewModel
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var rvBoard: RecyclerView
    private lateinit var tvScore: TextView
    private lateinit var tvBestScore: TextView
    private lateinit var tvTimer: TextView    // <--- NOVÉ
    private lateinit var tvLastGame: TextView // <--- NOVÉ

    private lateinit var viewModel: MemoryGameViewModel
    private lateinit var adapter: MemoryCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. NEJDŮLEŽITĚJŠÍ: Nejdřív musíme najít všechny prvky na obrazovce!
        // (Přesuň tyto řádky úplně nahoru)
        rvBoard = findViewById(R.id.rv_board)
        tvScore = findViewById(R.id.tv_score)
        tvBestScore = findViewById(R.id.tv_best_score)
        tvTimer = findViewById(R.id.tv_timer)
        tvLastGame = findViewById(R.id.tv_last_game)

        // 2. Načtení parametrů z menu
        val rows = intent.getIntExtra("ROWS", 4)
        val cols = intent.getIntExtra("COLS", 4)
        val mode = intent.getStringExtra("MODE") ?: "CLASSIC"
        val timeLimit = intent.getLongExtra("TIME_LIMIT", 60L)

        // 3. Inicializace ViewModelu a hry
        viewModel = ViewModelProvider(this)[MemoryGameViewModel::class.java]

        // Tady se hra resetuje a nastaví tahy na 0 -> spustí se observer
        // Protože už jsme udělali findViewById (v kroku 1), už to nespadne!
        viewModel.setupGame(rows, cols, mode, timeLimit)

        adapter = MemoryCardAdapter(emptyList()) { position ->
            viewModel.flipCard(position)
        }

        rvBoard.adapter = adapter
        rvBoard.layoutManager = GridLayoutManager(this, cols) // Použijeme počet sloupců z menu

        // 4. Sledování změn (Observers)
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
    }
}