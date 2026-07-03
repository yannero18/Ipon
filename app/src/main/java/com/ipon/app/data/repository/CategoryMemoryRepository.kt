package com.ipon.app.data.repository

import com.ipon.app.data.local.MerchantCategoryMemoryDao
import com.ipon.app.data.local.MerchantCategoryMemoryEntity
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.TransactionCategory
import com.ipon.app.data.model.resolveCategory
import com.ipon.app.util.normalizeMerchantKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * One learned merchant -> category memory, in display-friendly form. The
 * Settings "Your learned categories" screen shows this list directly --
 * the whole point of giving the person a screen for this is that "the app
 * learned something" should never be invisible or un-editable.
 */
data class LearnedCategoryMemory(
    val merchantKey: String,
    val type: TransactionType,
    val category: TransactionCategory,
    val confirmCount: Int,
    val lastUsedEpochMillis: Long
)

/**
 * Real, honest personalization: remembers which category the user actually
 * picked for a given merchant string, and recalls that ahead of the
 * generic keyword-rule classifier next time. Not a trained model, not
 * cross-user, never leaves the device -- just a lookup table that grows
 * from the user's own corrections, which is the most defensible form of
 * "the app learns" that doesn't overclaim what's actually happening.
 */
class CategoryMemoryRepository(private val dao: MerchantCategoryMemoryDao) {

    /**
     * Checked BEFORE MerchantClassifier in the suggestion pipeline -- see
     * AddTransactionViewModel.onMerchantChanged(). Returns null on a
     * clean miss (never seen this merchant before for this type), at which point
     * the caller falls back to the keyword rules.
     */
    suspend fun recall(rawMerchantString: String, type: TransactionType): TransactionCategory? {
        val key = normalizeMerchantKey(rawMerchantString)
        if (key.isEmpty()) return null
        val memory = dao.getMemory(key, type) ?: return null
        return resolveCategory(memory.category, type)
    }

    /**
     * Call this whenever a transaction is actually saved with a non-blank
     * merchant string -- whether the category came from this memory, the
     * keyword classifier, or the user typing/picking it manually. Every
     * save is a vote: if it agrees with what's already remembered,
     * confirmCount goes up (the memory gets more confident); if it
     * disagrees, the memory is overwritten and the count resets to 1,
     * since the user has just told us the old memory was wrong for this
     * merchant going forward.
     */
    suspend fun remember(rawMerchantString: String, type: TransactionType, category: TransactionCategory) {
        val key = normalizeMerchantKey(rawMerchantString)
        if (key.isEmpty()) return

        val existing = dao.getMemory(key, type)
        val newConfirmCount = if (existing != null && existing.category == category.displayName) {
            existing.confirmCount + 1
        } else {
            1
        }

        dao.upsert(
            MerchantCategoryMemoryEntity(
                merchantKey = key,
                type = type,
                category = category.displayName,
                confirmCount = newConfirmCount
            )
        )
    }

    /** Powers the Settings "Your learned categories" list. */
    fun observeAll(): Flow<List<LearnedCategoryMemory>> =
        dao.observeAll().map { list ->
            list.map {
                LearnedCategoryMemory(
                    merchantKey = it.merchantKey,
                    type = it.type,
                    category = resolveCategory(it.category, it.type),
                    confirmCount = it.confirmCount,
                    lastUsedEpochMillis = it.lastUsedEpochMillis
                )
            }
        }

    /** Deletes one specific learned memory -- the person disagreeing with what's been remembered, without wiping everything else. */
    suspend fun forget(memory: LearnedCategoryMemory) {
        dao.delete(
            MerchantCategoryMemoryEntity(
                merchantKey = memory.merchantKey,
                type = memory.type,
                category = memory.category.displayName,
                confirmCount = memory.confirmCount,
                lastUsedEpochMillis = memory.lastUsedEpochMillis
            )
        )
    }

    suspend fun clearAll() = dao.clearAll()
}
