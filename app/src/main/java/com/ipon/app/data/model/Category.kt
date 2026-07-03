package com.ipon.app.data.model

import com.ipon.app.data.local.TransactionType

/**
 * Common interface so UI code (TransactionRow, Insights) can render an
 * emoji/label without caring whether a transaction is an expense or income --
 * it just needs *a* category, regardless of which enum it came from.
 */
sealed interface TransactionCategory {
    val displayName: String
    val emoji: String
}

/**
 * Expense categories, tuned to common Philippine spending patterns. Kept
 * deliberately separate from [IncomeCategory] -- a "Jollibee" merchant
 * string should never be a candidate match against income categories, and
 * vice versa for "sweldo." Splitting the enums is what makes
 * [com.ipon.app.util.MerchantClassifier] safe to extend with income-side
 * rules without cross-contaminating matches.
 */
enum class ExpenseCategory(override val displayName: String, override val emoji: String) : TransactionCategory {
    TRANSPO("Transpo", "🚲"),
    FOOD("Food", "🍔"),
    BILLS("Bills", "🧾"),
    GROCERIES("Groceries", "🛒"),
    SHOPPING("Shopping", "🛍️"),
    HEALTH("Health", "💊"),
    ENTERTAINMENT("Entertainment", "🎬"),
    EDUCATION("Education", "📚"),
    GOVERNMENT("Government / Dues", "🏛️"),
    UTANG("Utang payment", "💸"),
    PADALA("Padala / Remittance sent", "📦"),
    OTHER("Other", "📝");

    companion object {
        fun fromDisplayName(name: String): ExpenseCategory =
            entries.find { it.displayName.equals(name, ignoreCase = true) } ?: OTHER
    }
}

/**
 * Income categories reflecting common Philippine income patterns --
 * salaried "sweldo," gig/freelance work, remittances received from family
 * (OFW or otherwise), small sari-sari/online-selling business income, and
 * cash gifts (e.g. "abuloy," birthday/holiday gifts).
 */
enum class IncomeCategory(override val displayName: String, override val emoji: String) : TransactionCategory {
    SALARY("Salary", "💵"),
    FREELANCE("Freelance / Sideline", "💻"),
    REMITTANCE("Remittance received", "📨"),
    BUSINESS("Business / Tindahan", "🏪"),
    GIFT("Gift / Allowance", "🎁"),
    OTHER("Other", "📝");

    companion object {
        fun fromDisplayName(name: String): IncomeCategory =
            entries.find { it.displayName.equals(name, ignoreCase = true) } ?: OTHER
    }
}

/**
 * Unified lookup used by display-only code (row icons, insights breakdown)
 * that has a raw category string and a [TransactionType] but doesn't want
 * two separate code paths just to find an emoji.
 */
fun resolveCategory(displayName: String, type: TransactionType): TransactionCategory =
    when (type) {
        TransactionType.EXPENSE -> ExpenseCategory.fromDisplayName(displayName)
        TransactionType.INCOME -> IncomeCategory.fromDisplayName(displayName)
    }
