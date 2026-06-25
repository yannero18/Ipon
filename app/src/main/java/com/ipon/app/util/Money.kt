package com.ipon.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Scaled-integer money type, per Section 2 of the Ipon proposal.
 *
 * Every amount is stored as a [Long] representing the minor unit (centavos for PHP).
 * This avoids the progressive rounding error that accumulates with Double/Float
 * arithmetic over thousands of transactions. A purchase of P150.50 is stored as 15050L.
 *
 * Design notes:
 * - This is an inline value class: at runtime it is just a boxed/unboxed Long, so it
 *   carries zero allocation overhead over using a raw Long directly, while still
 *   preventing you from accidentally adding a raw "amount in pesos" Long to a
 *   "minor units" Long at the type level.
 * - Arithmetic (plus/minus) is exact, since Long addition has no rounding behavior.
 * - Only display formatting touches BigDecimal/RoundingMode, and only at the boundary
 *   where a Long must become a human-readable string.
 */
@JvmInline
value class Money private constructor(val minorUnits: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(minorUnits + other.minorUnits)
    operator fun minus(other: Money): Money = Money(minorUnits - other.minorUnits)
    operator fun unaryMinus(): Money = Money(-minorUnits)

    /**
     * Scales by a rational multiplier (e.g. splitting a transaction, applying a
     * percentage). This is the one place fractional math can creep in, so it is
     * routed through BigDecimal with explicit HALF_EVEN (banker's rounding) per
     * the proposal's stated rounding policy, then rescaled back to a Long.
     */
    fun scaledBy(multiplier: BigDecimal): Money {
        val result = BigDecimal(minorUnits)
            .multiply(multiplier)
            .setScale(0, RoundingMode.HALF_EVEN)
        return Money(result.toLong())
    }

    val isNegative: Boolean get() = minorUnits < 0
    val isZero: Boolean get() = minorUnits == 0L
    val isPositive: Boolean get() = minorUnits > 0

    override fun compareTo(other: Money): Int = minorUnits.compareTo(other.minorUnits)

    /** Renders using PHP grouping/decimal conventions with tabular-friendly fixed decimals. */
    fun formatPhp(showSign: Boolean = false): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val pattern = if (showSign && minorUnits > 0) "+#,##0.00" else "#,##0.00"
        val formatter = DecimalFormat(pattern, symbols)
        val majorUnitsValue = BigDecimal(minorUnits).movePointLeft(2)
        return "₱" + formatter.format(majorUnitsValue)
    }

    companion object {
        val ZERO = Money(0L)

        /** Constructs from already-known minor units (e.g. reading from the database). */
        fun ofMinorUnits(minorUnits: Long): Money = Money(minorUnits)

        /**
         * Parses a user-facing decimal string ("150.50", "150", "150,000.75") into Money.
         * This is the single ingestion point where a string becomes a Long, so any
         * future currency-aware parsing (e.g. locale-specific separators) lives here only.
         */
        fun parse(input: String): Money? {
            val cleaned = input.trim().replace(",", "")
            if (cleaned.isEmpty()) return null
            val decimal = cleaned.toBigDecimalOrNull() ?: return null
            val minorUnits = decimal
                .setScale(2, RoundingMode.HALF_EVEN)
                .movePointRight(2)
                .toLong()
            return Money(minorUnits)
        }

        private fun String.toBigDecimalOrNull(): BigDecimal? =
            try { BigDecimal(this) } catch (e: NumberFormatException) { null }
    }
}

fun List<Money>.sum(): Money = fold(Money.ZERO) { acc, m -> acc + m }
