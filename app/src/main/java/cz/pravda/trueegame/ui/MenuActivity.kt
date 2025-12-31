package cz.pravda.trueegame.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cz.pravda.trueegame.R
import kotlin.system.exitProcess

class MenuActivity : AppCompatActivity() {

    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        soundManager = SoundManager(this)

        val mainView = findViewById<ConstraintLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnStart = findViewById<Button>(R.id.btn_start_game)
        val btnScore = findViewById<Button>(R.id.btn_score)
        val btnSettings = findViewById<Button>(R.id.btn_settings)
        val btnExit = findViewById<Button>(R.id.btn_exit)

        btnStart.setOnClickListener {
            soundManager.playClick()
            startActivity(Intent(this, GameSetupActivity::class.java))
        }

        btnScore.setOnClickListener {
            soundManager.playClick()
            startActivity(Intent(this, ScoreActivity::class.java))
        }

        btnSettings.setOnClickListener {
            soundManager.playClick()
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnExit.setOnClickListener {
            soundManager.playClick()
            finishAffinity()
            exitProcess(0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }

    override fun onResume() {
        super.onResume()
    }
}