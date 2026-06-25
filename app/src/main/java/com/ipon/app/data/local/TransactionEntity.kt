package com.ipon.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType { EXPENSE, INCOME }

/**
 * Core ledger row.
 *
 * WITHOUT ROWID per Section 2: Room/SQLite normally maintains a hidden
 * monotonic rowid alongside any declared primary key, which costs an extra
 * B-tree indirection per lookup. Since [id] is already a globally unique,
 * evenly distributed UUID (not a monotonic int), WITHOUT ROWID lets SQLite
 * use the primary key itself as the clustered index -- better cache
 * locality on primary-key reads, at the cost of slightly worse insert
 * locality, since UUIDs land at random points in the B-tree rather than
 * always appending at the end. For a personal-finance ledger that is
 * read-heavy and never approaches web-scale row counts, that tradeoff
 * favors WITHOUT ROWID.
 *
 * Room's @Entity annotation has no first-class flag for WITHOUT ROWID, so
 * it is applied via a raw CREATE TABLE in the bundled schema
 * (see assets/database/ipon_schema.sql and IponDatabase's
 * createFromAsset wiring) rather than letting Room auto-generate DDL.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["occurredAtEpochMillis"]),
        Index(value = ["category"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    /** Stored in minor units (centavos). Never a Double. See [com.ipon.app.util.Money]. */
    @ColumnInfo(name = "amountMinorUnits")
    val amountMinorUnits: Long,

    @ColumnInfo(name = "type")
    val type: TransactionType,

    @ColumnInfo(name = "category")
    val category: String,

    /** Raw text as entered or ingested (e.g. "STAMARIA-TODA-04"), kept for audit/re-classification. */
    @ColumnInfo(name = "merchantRaw")
    val merchantRaw: String?,

    @ColumnInfo(name = "note")
    val note: String?,

    @ColumnInfo(name = "occurredAtEpochMillis")
    val occurredAtEpochMillis: Long,

    @ColumnInfo(name = "createdAtEpochMillis")
    val createdAtEpochMillis: Long = System.currentTimeMillis(),

    /** True if [category] was set by the on-device classifier rather than the user directly. */
    @ColumnInfo(name = "isAutoCategorized")
    val isAutoCategorized: Boolean = false
)
