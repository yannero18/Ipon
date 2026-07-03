package com.ipon.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTemplateDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(template: RecurringTemplateEntity)

    @Update
    suspend fun update(template: RecurringTemplateEntity)

    @Delete
    suspend fun delete(template: RecurringTemplateEntity)

    @Query("SELECT * FROM recurring_templates ORDER BY dayOfPeriod ASC")
    fun observeAll(): Flow<List<RecurringTemplateEntity>>

    @Query("SELECT * FROM recurring_templates WHERE isPaused = 0 ORDER BY dayOfPeriod ASC")
    fun observeActive(): Flow<List<RecurringTemplateEntity>>
}
