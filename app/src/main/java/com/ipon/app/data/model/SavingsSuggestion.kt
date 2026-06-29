package com.ipon.app.data.model

import com.ipon.app.util.Money

/**
 * A single, honest, rule-based suggestion -- never framed as AI or machine
 * learning, because it isn't. Every suggestion here is computed from real
 * numbers already in the ledger (envelope caps, category trends, recent
 * totals) via plain arithmetic, and every suggestion explains its own
 * reasoning in [reason] so the person can verify it themselves rather than
 * trusting a black box. This is the deliberate alternative to the
 * over-claiming "on-device foundation model" language the original
 * proposal used for merchant classification -- see MerchantClassifier.kt's
 * header comment for that same principle applied earlier in the project.
 */
data class SavingsSuggestion(
    val id: String,
    val title: String,
    val reason: String,
    val severity: SuggestionSeverity,
    val category: ExpenseCategory?
)

enum class SuggestionSeverity { INFO, NOTABLE, IMPORTANT }

/**
 * Pure function, no I/O: given trends and envelope progress the caller
 * already has in hand (from TransactionRepository / EnvelopeRepository),
 * produces a short, ranked list of suggestions. Kept as a standalone
 * function rather than a class so it's trivially unit-testable with plain
 * data -- no mocking a repository required.
 */
fun generateSavingsSuggestions(
    trends: List<CategoryTrend>,
    envelopeProgress: List<EnvelopeProgress>,
    totalIncome: Money,
    totalExpense: Money
): List<SavingsSuggestion> {
    val suggestions = mutableListOf<SavingsSuggestion>()

    // Rule 1: a category trending up sharply (>=25%) with real money behind
    // it (last month wasn't trivially small) is worth a direct callout.
    trends
        .filter { trend ->
            val percent = trend.percentChange
            percent != null && percent >= 25 && trend.lastMonth.minorUnits >= 5000L
        }
        .sortedByDescending { it.percentChange }
        .take(2)
        .forEach { trend ->
            suggestions += SavingsSuggestion(
                id = "trend_up_${trend.category.name}",
                title = "${trend.category.displayName} is up ${trend.percentChange}% from last month",
                reason = "You spent ${trend.thisMonth.formatPhp()} on ${trend.category.displayName} this month, " +
                    "versus ${trend.lastMonth.formatPhp()} last month. " +
                    "Consider setting or lowering an envelope for this category.",
                severity = SuggestionSeverity.NOTABLE,
                category = trend.category
            )
        }

    // Rule 2: any envelope already over budget gets a direct, specific callout.
    envelopeProgress
        .filter { it.isOverBudget }
        .sortedByDescending { (-it.remaining).minorUnits }
        .take(3)
        .forEach { envelope ->
            suggestions += SavingsSuggestion(
                id = "over_budget_${envelope.category.name}",
                title = "${envelope.category.displayName} is over budget",
                reason = "You've spent ${envelope.spent.formatPhp()} against a " +
                    "${envelope.cap.formatPhp()} cap -- ${(-envelope.remaining).formatPhp()} over.",
                severity = SuggestionSeverity.IMPORTANT,
                category = envelope.category
            )
        }

    // Rule 3: spending more than earning this month, in absolute terms.
    if (totalExpense > totalIncome && !totalIncome.isZero) {
        suggestions += SavingsSuggestion(
            id = "spending_exceeds_income",
            title = "You've spent more than you've logged as income this month",
            reason = "${totalExpense.formatPhp()} out versus ${totalIncome.formatPhp()} in so far this month.",
            severity = SuggestionSeverity.IMPORTANT,
            category = null
        )
    }

    // Rule 4: a category with no envelope at all, but real recurring spend
    // (appeared in both this month and last month with non-trivial amounts)
    // -- a nudge toward planning, not just reacting.
    val envelopedCategories = envelopeProgress.map { it.category }.toSet()
    trends
        .filter { trend ->
            trend.category !in envelopedCategories &&
                !trend.thisMonth.isZero &&
                !trend.lastMonth.isZero &&
                trend.thisMonth.minorUnits >= 10000L
        }
        .sortedByDescending { it.thisMonth.minorUnits }
        .take(1)
        .forEach { trend ->
            suggestions += SavingsSuggestion(
                id = "suggest_envelope_${trend.category.name}",
                title = "Consider an envelope for ${trend.category.displayName}",
                reason = "You've spent on this category two months in a row " +
                    "(${trend.thisMonth.formatPhp()} this month) with no budget cap set.",
                severity = SuggestionSeverity.INFO,
                category = trend.category
            )
        }

    return suggestions.sortedByDescending { it.severity.ordinal }
}
