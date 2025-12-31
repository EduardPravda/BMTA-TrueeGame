package cz.pravda.trueegame.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "score_table")
data class Score(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val moves: Int,
    val timeSeconds: Long,
    val timestamp: Long,

    val mode: String,
    val rows: Int,
    val cols: Int,
    val livesLeft: Int,
    val timeLimit: Long
)