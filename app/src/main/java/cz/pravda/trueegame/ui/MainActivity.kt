package cz.pravda.trueegame.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cz.pravda.trueegame.R
import cz.pravda.trueegame.viewmodel.MemoryGameViewModel
import android.util.Log // Pro logování

class MainActivity : AppCompatActivity() {

    // Proměnné pro View prvky
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvScore: TextView
    private lateinit var tvBestScore: TextView // <--- NOVÉ: Proměnná pro rekord

    // Proměnné pro logiku
    private lateinit var viewModel: MemoryGameViewModel
    private lateinit var adapter: MemoryCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Najdeme prvky na obrazovce
        rvBoard = findViewById(R.id.rv_board)
        tvScore = findViewById(R.id.tv_score)
        tvBestScore = findViewById(R.id.tv_best_score) // <--- NOVÉ: Najdeme textové pole

        // 2. Inicializujeme ViewModel (mozek hry)
        viewModel = ViewModelProvider(this)[MemoryGameViewModel::class.java]

        // 3. Nastavíme Adapter
        adapter = MemoryCardAdapter(emptyList()) { position ->
            viewModel.flipCard(position)
        }

        // 4. Nastavíme RecyclerView
        rvBoard.adapter = adapter
        rvBoard.layoutManager = GridLayoutManager(this, 4)
        rvBoard.setHasFixedSize(true)

        // 5. Pozorování dat (Observer)

        // Sledujeme seznam karet
        viewModel.cards.observe(this) { newCards ->
            adapter.updateData(newCards)
        }

        // Sledujeme aktuální počet tahů
        viewModel.moves.observe(this) { moves ->
            tvScore.text = "Tahů: $moves"
        }

        // <--- NOVÉ: Sledujeme nejlepší skóre z databáze --->
        viewModel.bestScore.observe(this) { best ->
            Log.d("PexesoDebug", "Načteno High Score z DB: $best") // Log pro kontrolu

            if (best != null && best > 0) {
                tvBestScore.text = "Rekord: $best tahů"
            } else {
                tvBestScore.text = "Rekord: -"
            }
        }
    }
}