package com.ipon.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * A learned merchant -> category mapping, built entirely from the user's
 * own corrections -- not a trained model, not cross-user, not cloud-synced.
 * This is real personalization without pretending to be ML: every time the
 * user logs a transaction with a merchant string and a category, that pair
 * is upserted here. The next time the same (normalized) merchant string
 * appears under the same transaction type, this table is checked BEFORE
 * the keyword-rule classifier, so the user's own choices always outrank
 * the generic PH-pattern rules.
 *
 * [merchantKey] is the normalized form of the raw merchant string
 * (lowercased, trimmed, punctuation-stripped) -- see
 * MerchantMemory.normalize() -- so "Jollibee", "jollibee", and "JOLLIBEE "
 * all hit the same row rather than fragmenting into separate memories.
 *
 * Primary key is the pair (merchantKey, type), not merchantKey alone: the
 * same raw string is looked up scoped by transaction type (an expense
 * classifier lookup should never accidentally surface an income memory or
 * vice versa), so the composite key matches how the data is actually
 * queried rather than relying on merchant strings never colliding across
 * the expense/income boundary.
 *
 * [confirmCount] tracks how many times this exact mapping has been
 * confirmed (by the user picking that category for that merchant, whether
 * auto-suggested or typed fresh). Used to decide whether a memory is
 * "settled" enough to surface confidently versus still provisional after
 * just one or two uses -- see CategoryMemoryRepository.
 */
@Entity(
    tableName = "merchant_category_memory",
    primaryKeys = ["merchantKey", "type"]
)
data class MerchantCategoryMemoryEntity(
    @ColumnInfo(name = "merchantKey")
    val merchantKey: String,

    @ColumnInfo(name = "type")
    val type: TransactionType,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "confirmCount")
    val confirmCount: Int = 1,

    @ColumnInfo(name = "lastUsedEpochMillis")
    val lastUsedEpochMillis: Long = System.currentTimeMillis()
)
