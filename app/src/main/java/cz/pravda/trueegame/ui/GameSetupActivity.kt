package cz.pravda.trueegame.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import cz.pravda.trueegame.R

class GameSetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_setup)

        val btnStart = findViewById<Button>(R.id.btn_start_game_final)
        val rgSize = findViewById<RadioGroup>(R.id.rg_size)
        val rgMode = findViewById<RadioGroup>(R.id.rg_mode)

        // Prvky pro čas
        val cardTimeSettings = findViewById<CardView>(R.id.card_time_settings)
        val seekBarTime = findViewById<SeekBar>(R.id.seekbar_time)
        val tvTimeValue = findViewById<TextView>(R.id.tv_time_value)

        // --- ZMĚNA: EXTRÉMNÍ ROZSAH (1s - 180s) ---
        // SeekBar začíná na 0.
        // Min = 0 (+1) = 1 sekunda
        // Max = 179 (+1) = 180 sekund
        seekBarTime.max = 179

        // Výchozí hodnota: Chceme 60s.
        // Takže 60 - 1 = 59.
        seekBarTime.progress = 59

        // Listener pro posuvník
        seekBarTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // --- ZMĚNA: Matematika +1 (každý dílek je vteřina) ---
                val realTime = progress + 1
                tvTimeValue.text = "$realTime s"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Přepínání módu
        rgMode.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_time) {
                cardTimeSettings.visibility = View.VISIBLE
            } else {
                cardTimeSettings.visibility = View.GONE
            }
        }

        btnStart.setOnClickListener {
            val is4x4 = findViewById<RadioButton>(R.id.rb_4x4).isChecked
            val rows = 4
            val cols = if (is4x4) 4 else 3

            val isTimeMode = findViewById<RadioButton>(R.id.rb_time).isChecked
            val mode = if (isTimeMode) "TIME" else "CLASSIC"

            // --- ZMĚNA: Odesílání času (+1) ---
            val selectedTime = if (isTimeMode) (seekBarTime.progress + 1).toLong() else 0L

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("ROWS", rows)
            intent.putExtra("COLS", cols)
            intent.putExtra("MODE", mode)
            intent.putExtra("TIME_LIMIT", selectedTime)
            startActivity(intent)
        }
    }
}