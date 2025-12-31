package cz.pravda.trueegame.ui

import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cz.pravda.trueegame.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var soundManager: SoundManager
    private var lastProgress = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        soundManager = SoundManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val seekBar = findViewById<SeekBar>(R.id.seekbar_volume)
        val btnBack = findViewById<Button>(R.id.btn_back)
        val rgCardSets = findViewById<RadioGroup>(R.id.rg_card_sets)

        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)

        val currentVolume = prefs.getFloat("VOLUME", 1.0f)
        seekBar.progress = (currentVolume * 100).toInt()
        lastProgress = seekBar.progress

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newVolume = progress / 100f
                    soundManager.saveVolume(newVolume)
                    if (progress != lastProgress) {
                        soundManager.playClick()
                        lastProgress = progress
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val savedSet = prefs.getInt("CARD_SET", 1)
        when (savedSet) {
            1 -> findViewById<RadioButton>(R.id.rb_set_1).isChecked = true
            2 -> findViewById<RadioButton>(R.id.rb_set_2).isChecked = true
            3 -> findViewById<RadioButton>(R.id.rb_set_3).isChecked = true
        }

        rgCardSets.setOnCheckedChangeListener { _, checkedId ->
            soundManager.playClick()
            val newSet = when (checkedId) {
                R.id.rb_set_2 -> 2
                R.id.rb_set_3 -> 3
                else -> 1
            }
            prefs.edit().putInt("CARD_SET", newSet).apply()
        }

        btnBack.setOnClickListener {
            soundManager.playClick()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}