package cz.pravda.trueegame.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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

        // Fix paddingu
        val mainView = findViewById<android.view.View>(R.id.main)
        val initialPaddingLeft = mainView.paddingLeft
        val initialPaddingTop = mainView.paddingTop
        val initialPaddingRight = mainView.paddingRight
        val initialPaddingBottom = mainView.paddingBottom

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout())
            v.setPadding(
                initialPaddingLeft + systemBars.left,
                initialPaddingTop + systemBars.top,
                initialPaddingRight + systemBars.right,
                initialPaddingBottom + systemBars.bottom
            )
            insets
        }

        val btnStart = findViewById<Button>(R.id.btn_start_game_final)
        val btnBack = findViewById<Button>(R.id.btn_back)
        val rgMode = findViewById<RadioGroup>(R.id.rg_mode)
        val cardTimeSettings = findViewById<CardView>(R.id.card_time_settings)
        val seekBar = findViewById<SeekBar>(R.id.seekbar_time)
        val tvValue = findViewById<TextView>(R.id.tv_time_value)

        var selectedValue: Long = 60

        fun updateSettingsUI(progress: Int, mode: String) {
            when (mode) {
                "TIME" -> {
                    val seconds = progress + 10
                    tvValue.text = "$seconds s"
                    selectedValue = seconds.toLong()
                }
                "MEMORY" -> {
                    val realPreview = progress + 1
                    tvValue.text = "$realPreview s (náhled)"
                    selectedValue = realPreview.toLong()
                }
                else -> {
                    cardTimeSettings.visibility = View.GONE
                }
            }
        }

        rgMode.setOnCheckedChangeListener { _, checkedId ->
            cardTimeSettings.visibility = View.VISIBLE
            when (checkedId) {
                R.id.rb_classic -> {
                    cardTimeSettings.visibility = View.GONE
                }
                R.id.rb_time -> {
                    seekBar.max = 170
                    seekBar.progress = 50
                    updateSettingsUI(50, "TIME")
                }
                R.id.rb_memory -> {
                    seekBar.max = 29
                    seekBar.progress = 4
                    updateSettingsUI(4, "MEMORY")
                }
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val mode = when (rgMode.checkedRadioButtonId) {
                    R.id.rb_time -> "TIME"
                    R.id.rb_memory -> "MEMORY"
                    else -> "CLASSIC"
                }
                updateSettingsUI(progress, mode)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnStart.setOnClickListener {
            val selectedSizeId = findViewById<RadioGroup>(R.id.rg_size).checkedRadioButtonId
            var rows = 4
            var cols = 4
            when (selectedSizeId) {
                R.id.rb_4x3 -> { rows = 4; cols = 3 }
                R.id.rb_4x4 -> { rows = 4; cols = 4 }
                R.id.rb_5x4 -> { rows = 5; cols = 4 }
            }

            var mode = "CLASSIC"
            val checkedId = rgMode.checkedRadioButtonId
            if (checkedId == R.id.rb_time) mode = "TIME"
            if (checkedId == R.id.rb_memory) mode = "MEMORY"

            val finalLimit = if (mode == "CLASSIC") 0L else selectedValue

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("ROWS", rows)
            intent.putExtra("COLS", cols)
            intent.putExtra("MODE", mode)
            // TADY BYLA CHYBA: Musí to být "TIME_LIMIT"
            intent.putExtra("TIME_LIMIT", finalLimit)
            startActivity(intent)
        }

        btnBack.setOnClickListener { finish() }
    }
}