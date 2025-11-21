package cz.pravda.trueegame.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "score_table")
data class Score(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val moves: Int,
    val timeSeconds: Long, // <--- NOVÉ: Čas v sekundách
    val timestamp: Long
)