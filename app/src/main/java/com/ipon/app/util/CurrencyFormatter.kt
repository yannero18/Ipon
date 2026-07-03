package com.ipon.app.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * A unified CurrencyFormatter utility that handles the Philippine Peso (₱) symbol placement,
 * grouping separators, and sign representation correctly for all UI components.
 */
object CurrencyFormatter {
    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        currencySymbol = "₱"
    }

    /**
     * Formats a [Money] amount into a standard Philippine Peso string (e.g., "₱1,234.56" or "-₱1,234.56").
     */
    fun format(amount: Money, showSign: Boolean = false): String {
        return format(amount.minorUnits, showSign)
    }

    /**
     * Formats raw [minorUnits] into a standard Philippine Peso string.
     */
    fun format(minorUnits: Long, showSign: Boolean = false): String {
        val majorUnits = BigDecimal(minorUnits).movePointLeft(2)
        val isNegative = minorUnits < 0
        val absoluteVal = majorUnits.abs()
        
        val pattern = if (showSign && minorUnits > 0) "+#,##0.00" else "#,##0.00"
        val formatter = DecimalFormat(pattern, symbols)
        val formattedNum = formatter.format(absoluteVal)
        
        return when {
            isNegative -> "-₱$formattedNum"
            showSign && minorUnits > 0 -> "+₱$formattedNum"
            else -> "₱$formattedNum"
        }
    }

    /**
     * Formats raw [minorUnits] into a standard Philippine Peso string with no decimal places.
     */
    fun formatNoDecimals(minorUnits: Long): String {
        val majorUnits = BigDecimal(minorUnits).movePointLeft(2)
        val isNegative = minorUnits < 0
        val absoluteVal = majorUnits.abs()
        
        val formatter = DecimalFormat("#,##0", symbols)
        val formattedNum = formatter.format(absoluteVal)
        
        return if (isNegative) "-₱$formattedNum" else "₱$formattedNum"
    }
}
