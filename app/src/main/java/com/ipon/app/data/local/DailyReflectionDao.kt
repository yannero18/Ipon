package com.ipon.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyReflectionDao {

    /**
     * REPLACE on conflict, paired with the schema's UNIQUE(dayKey) index --
     * checking in twice on the same day updates that day's entry rather
     * than creating a second one. Lets the UI always just "save" without
     * needing to know whether today already has a reflection.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reflection: DailyReflectionEntity)

    @Query("SELECT * FROM daily_reflections WHERE dayKey = :dayKey LIMIT 1")
    suspend fun getForDay(dayKey: String): DailyReflectionEntity?

    @Query("SELECT * FROM daily_reflections WHERE dayKey = :dayKey LIMIT 1")
    fun observeForDay(dayKey: String): Flow<DailyReflectionEntity?>

    @Query("SELECT * FROM daily_reflections ORDER BY dayKey DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DailyReflectionEntity>>
}
