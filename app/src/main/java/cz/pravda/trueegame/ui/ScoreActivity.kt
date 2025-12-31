package cz.pravda.trueegame.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cz.pravda.trueegame.R
import cz.pravda.trueegame.data.AppDatabase
import cz.pravda.trueegame.data.Score
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScoreActivity : AppCompatActivity() {

    private lateinit var soundManager: SoundManager
    private lateinit var database: AppDatabase
    private lateinit var rvScores: RecyclerView
    private var scoreJob: Job? = null

    // Filtry
    private var currentMode = "ALL"
    private var currentDiff = "ALL" // "ALL", "EASY", "MEDIUM", "HARD"

    // Tlačítka módů
    private lateinit var btnAll: Button
    private lateinit var btnClassic: Button
    private lateinit var btnTime: Button
    private lateinit var btnMemory: Button

    // Tlačítka obtížnosti
    private lateinit var btnDiffAll: Button
    private lateinit var btnDiffEasy: Button
    private lateinit var btnDiffMedium: Button
    private lateinit var btnDiffHard: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)

        soundManager = SoundManager(this)
        database = AppDatabase.getDatabase(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rvScores = findViewById(R.id.rv_scores)
        rvScores.layoutManager = LinearLayoutManager(this)

        // Inicializace tlačítek
        btnAll = findViewById(R.id.filter_all)
        btnClassic = findViewById(R.id.filter_classic)
        btnTime = findViewById(R.id.filter_time)
        btnMemory = findViewById(R.id.filter_memory)

        btnDiffAll = findViewById(R.id.diff_all)
        btnDiffEasy = findViewById(R.id.diff_easy)
        btnDiffMedium = findViewById(R.id.diff_medium)
        btnDiffHard = findViewById(R.id.diff_hard)

        // Listenery pro MÓD
        btnAll.setOnClickListener { updateMode("ALL") }
        btnClassic.setOnClickListener { updateMode("CLASSIC") }
        btnTime.setOnClickListener { updateMode("TIME") }
        btnMemory.setOnClickListener { updateMode("MEMORY") }

        // Listenery pro OBTÍŽNOST
        btnDiffAll.setOnClickListener { updateDiff("ALL") }
        btnDiffEasy.setOnClickListener { updateDiff("EASY") }     // 4x3
        btnDiffMedium.setOnClickListener { updateDiff("MEDIUM") } // 4x4
        btnDiffHard.setOnClickListener { updateDiff("HARD") }     // 5x4

        findViewById<Button>(R.id.btn_back).setOnClickListener {
            soundManager.playClick()
            finish()
        }

        // Výchozí načtení
        refreshData()
        updateButtonsUI()
    }

    private fun updateMode(mode: String) {
        soundManager.playClick()
        currentMode = mode
        refreshData()
        updateButtonsUI()
    }

    private fun updateDiff(diff: String) {
        soundManager.playClick()
        currentDiff = diff
        refreshData()
        updateButtonsUI()
    }

    private fun refreshData() {
        scoreJob?.cancel()
        scoreJob = lifecycleScope.launch {
            val dao = database.scoreDao()

            val flow = if (currentMode == "ALL" && currentDiff == "ALL") {
                dao.getAllScores()
            } else if (currentMode != "ALL" && currentDiff == "ALL") {
                dao.getScoresByMode(currentMode)
            } else if (currentMode == "ALL" && currentDiff != "ALL") {
                val (rows, cols) = getDimensFromDiff(currentDiff)
                dao.getScoresBySize(rows, cols)
            } else {
                val (rows, cols) = getDimensFromDiff(currentDiff)
                dao.getScoresByModeAndSize(currentMode, rows, cols)
            }

            flow.collect { scores ->
                rvScores.adapter = ScoreAdapter(scores)
            }
        }
    }

    private fun getDimensFromDiff(diff: String): Pair<Int, Int> {
        return when (diff) {
            "EASY" -> Pair(4, 3)
            "MEDIUM" -> Pair(4, 4)
            "HARD" -> Pair(5, 4)
            else -> Pair(0, 0)
        }
    }

    private fun updateButtonsUI() {
        val activeColor = Color.parseColor("#FFFFFF")
        val activeText = Color.parseColor("#4A00E0")
        val inactiveColor = Color.parseColor("#9FA8DA")
        val inactiveText = Color.parseColor("#FFFFFF")

        listOf(btnAll, btnClassic, btnTime, btnMemory, btnDiffAll, btnDiffEasy, btnDiffMedium, btnDiffHard).forEach {
            it.backgroundTintList = ColorStateList.valueOf(inactiveColor)
            it.setTextColor(inactiveText)
        }

        // Aktivace vybraného MÓDU
        val activeModeBtn = when(currentMode) {
            "CLASSIC" -> btnClassic
            "TIME" -> btnTime
            "MEMORY" -> btnMemory
            else -> btnAll
        }
        activeModeBtn.backgroundTintList = ColorStateList.valueOf(activeColor)
        activeModeBtn.setTextColor(activeText)

        // Aktivace vybrané OBTÍŽNOSTI
        val activeDiffBtn = when(currentDiff) {
            "EASY" -> btnDiffEasy
            "MEDIUM" -> btnDiffMedium
            "HARD" -> btnDiffHard
            else -> btnDiffAll
        }
        activeDiffBtn.backgroundTintList = ColorStateList.valueOf(activeColor)
        activeDiffBtn.setTextColor(activeText)
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}

class ScoreAdapter(private val scores: List<Score>) : RecyclerView.Adapter<ScoreAdapter.ScoreViewHolder>() {
    class ScoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tv_rank_number)
        val tvMode: TextView = view.findViewById(R.id.tv_mode_info)
        val tvMoves: TextView = view.findViewById(R.id.tv_moves_count)
        val tvDetails: TextView = view.findViewById(R.id.tv_details_extra)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_score, parent, false)
        return ScoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        val score = scores[position]
        holder.tvRank.text = "#${position + 1}"

        val diffName = when {
            score.rows == 4 && score.cols == 3 -> "Lehká"
            score.rows == 4 && score.cols == 4 -> "Střední"
            score.rows == 5 && score.cols == 4 -> "Těžká"
            else -> "${score.rows}x${score.cols}"
        }
        val modeName = when(score.mode) {
            "TIME" -> "⏱️"
            "MEMORY" -> "🧠"
            else -> "🃏"
        }
        holder.tvMode.text = "$modeName $diffName"
        holder.tvMoves.text = "${score.moves} tahů"

        val dateFormat = SimpleDateFormat("dd.MM.", Locale.getDefault()).format(Date(score.timestamp))
        holder.tvDate.text = dateFormat

        when (score.mode) {
            "TIME" -> {
                val left = if (score.timeLimit - score.timeSeconds < 0) 0 else score.timeLimit - score.timeSeconds
                holder.tvDetails.text = "Zbylý čas: ${left}s"
                holder.tvDetails.setTextColor(Color.parseColor("#E64A19"))
            }
            "MEMORY" -> {
                holder.tvDetails.text = "Životy: ${score.livesLeft} ❤️"
                holder.tvDetails.setTextColor(Color.parseColor("#D32F2F"))
            }
            else -> {
                val min = score.timeSeconds / 60
                val sec = score.timeSeconds % 60
                holder.tvDetails.text = "%02d:%02d".format(min, sec)
                holder.tvDetails.setTextColor(Color.parseColor("#388E3C"))
            }
        }
    }
    override fun getItemCount() = scores.size
}