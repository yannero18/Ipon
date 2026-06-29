package com.ipon.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantCategoryMemoryDao {

    @Query("SELECT * FROM merchant_category_memory WHERE merchantKey = :merchantKey AND type = :type LIMIT 1")
    suspend fun getMemory(merchantKey: String, type: TransactionType): MerchantCategoryMemoryEntity?

    /**
     * Every learned mapping, most recently used first -- powers the
     * Settings "Your learned categories" screen, where the person can see
     * and individually delete what's been remembered. Sorting by
     * lastUsedEpochMillis rather than confirmCount surfaces what's
     * currently relevant (merchants you're actively logging) ahead of
     * stale memories from a merchant you haven't used in months.
     */
    @Query("SELECT * FROM merchant_category_memory ORDER BY lastUsedEpochMillis DESC")
    fun observeAll(): Flow<List<MerchantCategoryMemoryEntity>>

    /**
     * Plain upsert -- whatever confirmCount the caller computed is written
     * as-is. The actual "increment if same category, reset to 1 if
     * different" decision is made in CategoryMemoryRepository.remember(),
     * in Kotlin, after reading the existing row -- kept out of raw SQL so
     * that logic is easy to read and unit-test rather than living in a
     * CASE expression.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: MerchantCategoryMemoryEntity)

    @Delete
    suspend fun delete(memory: MerchantCategoryMemoryEntity)

    @Query("DELETE FROM merchant_category_memory")
    suspend fun clearAll()
}
