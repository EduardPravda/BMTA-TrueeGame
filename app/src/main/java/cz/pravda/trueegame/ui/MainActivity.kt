package cz.pravda.trueegame.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cz.pravda.trueegame.R
import cz.pravda.trueegame.viewmodel.MemoryGameViewModel
import com.google.android.material.snackbar.Snackbar // Pro zobrazení hlášky o výhře

class MainActivity : AppCompatActivity() {

    // Proměnné pro View prvky
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvScore: TextView

    // Proměnné pro logiku
    private lateinit var viewModel: MemoryGameViewModel
    private lateinit var adapter: MemoryCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Najdeme prvky na obrazovce
        rvBoard = findViewById(R.id.rv_board)
        tvScore = findViewById(R.id.tv_score)

        // 2. Inicializujeme ViewModel (mozek hry)
        viewModel = ViewModelProvider(this)[MemoryGameViewModel::class.java]

        // 3. Nastavíme Adapter (prázdný seznam na začátek)
        adapter = MemoryCardAdapter(emptyList()) { position ->
            // Co se stane, když klikneš na kartu:
            viewModel.flipCard(position)
        }

        // 4. Nastavíme RecyclerView (mřížka 4 sloupce)
        rvBoard.adapter = adapter
        rvBoard.layoutManager = GridLayoutManager(this, 4)
        // Zafixujeme velikost pro lepší výkon
        rvBoard.setHasFixedSize(true)

        // 5. Pozorování dat (Observer) - Tady se děje kouzlo MVVM!

        // Sledujeme seznam karet. Kdykoliv se ve ViewModelu změní, překreslíme obrazovku.
        viewModel.cards.observe(this) { newCards ->
            adapter.updateData(newCards)
        }

        // Sledujeme počet tahů a aktualizujeme text
        viewModel.moves.observe(this) { moves ->
            tvScore.text = "Tahů: $moves"
        }
    }
}