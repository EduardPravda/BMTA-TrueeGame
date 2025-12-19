package cz.pravda.trueegame.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import cz.pravda.trueegame.R
import kotlin.system.exitProcess

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // Najdeme tlačítka podle ID z XML
        val btnStart = findViewById<Button>(R.id.btn_start_game)
        val btnExit = findViewById<Button>(R.id.btn_exit)

        // Kliknutí na "Nová hra" -> Spustí MainActivity (tvou hru)
        btnStart.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Kliknutí na "Ukončit" -> Zavře aplikaci
        btnExit.setOnClickListener {
            finishAffinity() // Zavře všechny aktivity
            exitProcess(0)   // Ukončí proces
        }
    }
}