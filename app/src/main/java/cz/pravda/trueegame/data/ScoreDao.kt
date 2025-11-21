package cz.pravda.trueegame.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    // Uloží nové skóre
    @Insert
    suspend fun insert(score: Score)

    // Získá nejlepší skóre (nejméně tahů)
    // Flow znamená, že když se změní data, aplikace to hned uvidí
    @Query("SELECT MIN(moves) FROM score_table")
    fun getBestScore(): Flow<Int?>
}