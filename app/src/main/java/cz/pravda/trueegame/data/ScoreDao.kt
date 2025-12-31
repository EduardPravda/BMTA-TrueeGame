package cz.pravda.trueegame.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: Score)

    @Query("SELECT MIN(moves) FROM score_table")
    fun getBestScore(): Flow<Int?>

    @Query("SELECT * FROM score_table ORDER BY timestamp DESC LIMIT 1")
    fun getLastGame(): Flow<Score?>

    @Query("SELECT * FROM score_table ORDER BY moves ASC LIMIT 50")
    fun getAllScores(): Flow<List<Score>>

    @Query("SELECT * FROM score_table WHERE mode = :mode ORDER BY moves ASC LIMIT 50")
    fun getScoresByMode(mode: String): Flow<List<Score>>

    @Query("SELECT * FROM score_table WHERE `rows` = :rows AND cols = :cols ORDER BY moves ASC LIMIT 50")
    fun getScoresBySize(rows: Int, cols: Int): Flow<List<Score>>

    @Query("SELECT * FROM score_table WHERE mode = :mode AND `rows` = :rows AND cols = :cols ORDER BY moves ASC LIMIT 50")
    fun getScoresByModeAndSize(mode: String, rows: Int, cols: Int): Flow<List<Score>>}