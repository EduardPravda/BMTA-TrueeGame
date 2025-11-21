package cz.pravda.trueegame.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Insert
    suspend fun insert(score: Score)

    // Nejlepší skóre (podle počtu tahů)
    @Query("SELECT MIN(moves) FROM score_table")
    fun getBestScore(): Flow<Int?>

    // <--- NOVÉ: Poslední odehraná hra (řazeno podle času vložení)
    @Query("SELECT * FROM score_table ORDER BY timestamp DESC LIMIT 1")
    fun getLastGame(): Flow<Score?>
}