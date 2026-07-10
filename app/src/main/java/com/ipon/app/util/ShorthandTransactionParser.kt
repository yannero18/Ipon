package com.ipon.app.util

import com.ipon.app.data.local.TransactionType

/**
 * Parses a single line of free-form shorthand -- "250 Grab", "Jollibee
 * 150.50", "+500 sweldo" -- into a structured guess at an amount and a
 * merchant/note string.
 *
 * This is deterministic text splitting, not a language model: it finds the
 * first number-looking token in the string and treats everything else as
 * the merchant/note text. A leading "+" marks the whole entry as income;
 * anything else defaults to an expense, since that's the overwhelmingly
 * common case for a quick one-line entry.
 *
 * Deliberately dumb by design: this never guesses a category itself.
 * Category inference stays [MerchantClassifier]/[CategoryMemoryRepository]'s
 * job, kept as a separate step downstream so this parser stays a
 * predictable text splitter with nothing that could be mistaken for
 * "understanding" the sentence -- same honesty boundary the keyword
 * classifier already draws for itself.
 */
data class ParsedShorthand(
    val amountInput: String,
    val type: TransactionType,
    val merchantText: String
)

private val AMOUNT_REGEX = Regex("""\d{1,3}(?:,\d{3})+(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?""")

fun parseShorthandTransaction(raw: String): ParsedShorthand? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null

    val isIncome = trimmed.startsWith("+")
    val withoutSign = (if (isIncome) trimmed.removePrefix("+") else trimmed).trim()
    if (withoutSign.isEmpty()) return null

    val match = AMOUNT_REGEX.find(withoutSign) ?: return null
    val amountInput = match.value.replace(",", "")

    val merchantText = (withoutSign.substring(0, match.range.first) + " " + withoutSign.substring(match.range.last + 1))
        .replace(Regex("\\s+"), " ")
        .trim()

    return ParsedShorthand(
        amountInput = amountInput,
        type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
        merchantText = merchantText
    )
}
