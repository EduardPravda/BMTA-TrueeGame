package cz.pravda.trueegame.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import cz.pravda.trueegame.R

class SoundManager(private val context: Context) {
    private var soundPool: SoundPool
    private var soundFlip: Int = 0
    private var soundMatch: Int = 0
    private var soundWin: Int = 0
    private var soundLose: Int = 0
    private var soundDamage: Int = 0
    private var soundClick: Int = 0

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundFlip = soundPool.load(context, R.raw.sound_flip, 1)
        soundMatch = soundPool.load(context, R.raw.sound_match, 1)
        soundWin = soundPool.load(context, R.raw.sound_win, 1)
        soundLose = soundPool.load(context, R.raw.sound_lose, 1)
        soundDamage = soundPool.load(context, R.raw.sound_damage, 1)
        soundClick = soundPool.load(context, R.raw.sound_click, 1)
    }

    private fun getVolume(): Float {
        val prefs = context.getSharedPreferences("GAME_PREFS", Context.MODE_PRIVATE)
        return prefs.getFloat("VOLUME", 1.0f)
    }

    fun saveVolume(newVolume: Float) {
        val prefs = context.getSharedPreferences("GAME_PREFS", Context.MODE_PRIVATE)
        prefs.edit().putFloat("VOLUME", newVolume).apply()
    }

    fun playFlip() {
        val vol = getVolume()
        soundPool.play(soundFlip, vol, vol, 0, 0, 1f)
    }

    fun playMatch() {
        val vol = getVolume()
        soundPool.play(soundMatch, vol, vol, 0, 0, 1f)
    }

    fun playWin() {
        val vol = getVolume()
        soundPool.play(soundWin, vol, vol, 0, 0, 1f)
    }

    fun playLose() {
        val vol = getVolume()
        soundPool.play(soundLose, vol, vol, 0, 0, 1f)
    }

    fun playClick() {
        val vol = getVolume()
        soundPool.play(soundClick, vol, vol, 0, 0, 1f)
    }

    fun playLifeLost() {
        val vol = getVolume()
        soundPool.play(soundDamage, vol, vol, 0, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}