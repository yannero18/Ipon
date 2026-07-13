package com.ipon.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A debt being tracked for payoff -- a credit card, a 5-6 loan, a personal
 * utang. Distinct from a Goal: a Goal tracks money accumulating toward a
 * target; a Debt tracks a balance being paid DOWN, with an optional
 * interest rate that matters for snowball/avalanche payoff ordering.
 *
 * [originalBalanceMinorUnits] is fixed at creation and never changes --
 * remaining balance is always derived as original minus the sum of real
 * payments (see [DebtPaymentEntity]), the same "derive, don't cache"
 * principle used for Goal progress and Envelope spend.
 */
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "originalBalanceMinorUnits")
    val originalBalanceMinorUnits: Long,

    /**
     * Annual percentage rate as a plain number (e.g. 24.5 for 24.5%), or
     * null if unknown/not applicable. Used only to order avalanche-method
     * payoff suggestions; never used to compute interest charges, since
     * this app doesn't model interest accrual over time.
     */
    @ColumnInfo(name = "interestRatePercent")
    val interestRatePercent: Double?,

    /**
     * A one-time fee deducted before the money reaches your hand -- common
     * with 5-6 lending, pawnshops, and lending apps: you're on the hook for
     * the full [originalBalanceMinorUnits], but what actually lands in your
     * pocket is that minus this fee. Kept as its own field rather than
     * baked into the balance so both numbers stay visible and honest;
     * see [com.ipon.app.data.model.Debt.netProceedsReceived].
     */
    @ColumnInfo(name = "feeMinorUnits")
    val feeMinorUnits: Long = 0L,

    /**
     * Free-text due date, same convention as Goal.deadline -- e.g.
     * "Jul 25, 2026" or "2026-07-25". A single lump-sum deadline, not a
     * recurring monthly date; genuinely recurring obligations (rent,
     * subscriptions) belong in Recurring templates instead.
     */
    @ColumnInfo(name = "dueDate")
    val dueDate: String? = null,

    @ColumnInfo(name = "isArchived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "createdAtEpochMillis")
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

/**
 * A single payment made toward a debt. Append-only, same reasoning as
 * GoalContributionEntity: remaining balance is always SUM(originalBalance)
 * - SUM(payments), so a missed write or a later-deleted payment can never
 * leave the tracked balance silently wrong.
 */
@Entity(
    tableName = "debt_payments",
    indices = [Index(value = ["debtId"])]
)
data class DebtPaymentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "debtId")
    val debtId: String,

    @ColumnInfo(name = "amountMinorUnits")
    val amountMinorUnits: Long,

    @ColumnInfo(name = "paidAtEpochMillis")
    val paidAtEpochMillis: Long = System.currentTimeMillis()
)
