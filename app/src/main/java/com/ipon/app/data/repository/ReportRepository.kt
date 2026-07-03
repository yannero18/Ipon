package com.ipon.app.data.repository

import com.ipon.app.data.local.ReportDao
import com.ipon.app.data.local.ReportEntity
import com.ipon.app.data.model.EnvelopeProgress
import com.ipon.app.data.model.Report
import com.ipon.app.data.model.Transaction
import com.ipon.app.data.model.toDomain
import com.ipon.app.data.model.toEntity
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ReportRepository(private val dao: ReportDao) {

    fun observeAllReports(): Flow<List<Report>> =
        dao.getAllReports().map { list -> list.map { it.toDomain() } }

    suspend fun getReportForPeriod(periodYearMonth: String): Report? =
        dao.getReportForPeriod(periodYearMonth)?.toDomain()

    suspend fun saveReport(report: Report) {
        dao.insertReport(report.toEntity())
    }

    suspend fun deleteReportById(id: String) {
        dao.deleteReportById(id)
    }

    suspend fun generateAndSaveMonthlyReport(
        periodYearMonth: String,
        income: Money,
        expense: Money,
        transactions: List<Transaction>,
        envelopes: List<EnvelopeProgress>
    ): Report {
        val title = "Ipon Report for $periodYearMonth"
        val content = generateReportContent(periodYearMonth, income, expense, transactions, envelopes)
        
        val report = Report(
            id = UUID.randomUUID().toString(),
            periodYearMonth = periodYearMonth,
            title = title,
            content = content,
            createdAtEpochMillis = System.currentTimeMillis()
        )
        saveReport(report)
        return report
    }

    private fun generateReportContent(
        periodYearMonth: String,
        income: Money,
        expense: Money,
        transactions: List<Transaction>,
        envelopes: List<EnvelopeProgress>
    ): String {
        val net = income - expense
        val topSpendingCategory = envelopes.maxByOrNull { it.spent }
        val overBudgets = envelopes.filter { it.isOverBudget }
        val goodSavers = envelopes.filter { !it.isOverBudget && it.fractionUsed < 0.8f && !it.cap.isZero }

        val sb = java.lang.StringBuilder()
        sb.append("Financial Status: ${if (net.minorUnits >= 0) "SURPLUS" else "DEFICIT"}\n\n")
        sb.append("--- OVERVIEW ---\n")
        sb.append("Total Income: Php $income\n")
        sb.append("Total Expenses: Php $expense\n")
        sb.append("Net Savings: Php $net\n")
        if (!income.isZero) {
            val percentageUsed = (expense.minorUnits.toDouble() / income.minorUnits.toDouble() * 100).toInt()
            sb.append("Spending Ratio: $percentageUsed% of income\n")
        }
        sb.append("\n--- CATEGORY BREAKDOWN ---\n")
        if (topSpendingCategory != null && !topSpendingCategory.spent.isZero) {
            sb.append("Your highest spending category this month is ${topSpendingCategory.category.displayName} at Php ${topSpendingCategory.spent} (budget Php ${topSpendingCategory.cap}).\n")
        } else {
            sb.append("No recorded expenses this month.\n")
        }
        
        if (overBudgets.isNotEmpty()) {
            sb.append("\n⚠️ BUDGET OVERRUNS:\n")
            overBudgets.forEach { env ->
                val overBy = env.spent - env.cap
                sb.append("- ${env.category.displayName}: Spent Php ${env.spent} against Php ${env.cap} (Over by Php $overBy)\n")
            }
        } else if (envelopes.isNotEmpty()) {
            sb.append("\n✨ ALL BUDGETS SECURED: Great job! No envelope limits were breached this month.\n")
        }

        if (goodSavers.isNotEmpty()) {
            sb.append("\n💡 SAVINGS SHIELD:\n")
            goodSavers.forEach { env ->
                val usedPercent = (env.fractionUsed * 100).toInt()
                sb.append("- ${env.category.displayName}: Only used $usedPercent% of your Php ${env.cap} budget (Php ${env.remaining} left over).\n")
            }
        }
        
        sb.append("\n--- LEDGER SUMMARY ---\n")
        sb.append("A total of ${transactions.size} records were logged during this cycle.\n")
        
        return sb.toString()
    }
}
