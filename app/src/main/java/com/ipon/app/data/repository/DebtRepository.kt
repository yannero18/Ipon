package com.ipon.app.data.repository

import com.ipon.app.data.local.DebtDao
import com.ipon.app.data.local.DebtEntity
import com.ipon.app.data.local.DebtPaymentEntity
import com.ipon.app.data.model.Debt
import com.ipon.app.data.model.DebtProgress
import com.ipon.app.data.model.toDomain
import com.ipon.app.data.model.toEntity
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class DebtRepository(private val dao: DebtDao) {

    fun observeProgress(): Flow<List<DebtProgress>> =
        combine(dao.observeActive(), dao.observePaidTotals()) { debts, paidTotals ->
            val paidByDebtId = paidTotals.associate { it.debtId to it.paidMinorUnits }
            debts.map { debt ->
                DebtProgress(
                    debt = debt.toDomain(),
                    paid = Money.ofMinorUnits(paidByDebtId[debt.id] ?: 0L)
                )
            }
        }

    suspend fun createDebt(
        label: String,
        originalBalance: Money,
        interestRatePercent: Double?,
        fee: Money = Money.ZERO,
        dueDate: String? = null
    ) {
        dao.insert(
            DebtEntity(
                id = UUID.randomUUID().toString(),
                label = label,
                originalBalanceMinorUnits = originalBalance.minorUnits,
                interestRatePercent = interestRatePercent,
                feeMinorUnits = fee.minorUnits,
                dueDate = dueDate
            )
        )
    }

    /** General-purpose edit -- label, balance, rate, fee, due date. Archive/delete stay their own dedicated calls below since they're distinct, less-reversible actions. */
    suspend fun updateDebt(debt: Debt) {
        dao.update(debt.toEntity())
    }

    suspend fun archiveDebt(debt: Debt) {
        dao.update(debt.copy(isArchived = true).toEntity())
    }

    suspend fun deleteDebt(debt: Debt) {
        dao.deleteDebtAndPayments(debt.toEntity())
    }

    suspend fun recordPayment(debtId: String, amount: Money) {
        dao.insertPayment(
            DebtPaymentEntity(
                debtId = debtId,
                amountMinorUnits = amount.minorUnits
            )
        )
    }
}
