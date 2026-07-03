package com.ipon.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun `parse handles the proposal's own example -- 150_50 becomes 15050 minor units`() {
        val money = Money.parse("150.50")
        assertEquals(15050L, money?.minorUnits)
    }

    @Test
    fun `parse strips thousands separators`() {
        val money = Money.parse("12,345.67")
        assertEquals(1234567L, money?.minorUnits)
    }

    @Test
    fun `parse rejects garbage input instead of throwing`() {
        assertNull(Money.parse("not a number"))
        assertNull(Money.parse(""))
        assertNull(Money.parse("   "))
    }

    @Test
    fun `addition is exact across many small transactions, unlike naive Double summation`() {
        // The classic float-drift trap: summing 0.10 ten times in Double
        // arithmetic does not reliably equal 1.00. Money must not reproduce
        // that bug, since it is stored and added as Long minor units.
        var total = Money.ZERO
        val tenCentavos = Money.parse("0.10")!!
        repeat(10) { total += tenCentavos }
        assertEquals(Money.parse("1.00"), total)
        assertEquals(100L, total.minorUnits)
    }

    @Test
    fun `subtraction and negative balances behave correctly`() {
        val income = Money.parse("500.00")!!
        val expense = Money.parse("750.25")!!
        val net = income - expense
        assertTrue(net.isNegative)
        assertEquals(-25025L, net.minorUnits)
    }

    @Test
    fun `formatPhp renders two decimal places with peso sign and grouping`() {
        val money = Money.parse("18420.75")!!
        assertEquals("\u20b118,420.75", money.formatPhp())
    }

    @Test
    fun `formatPhp on zero does not crash and renders zero pesos`() {
        assertEquals("\u20b10.00", Money.ZERO.formatPhp())
    }

    @Test
    fun `scaledBy applies banker's rounding -- HALF_EVEN -- per Section 2`() {
        // 0.5 minor-unit boundary: HALF_EVEN rounds 25 (odd preceding digit
        // effectively at the .5 boundary) down to keep the preceding digit
        // even, distinguishing it from standard HALF_UP rounding.
        val base = Money.ofMinorUnits(125L) // P1.25
        val half = base.scaledBy(BigDecimal("0.5")) // exactly 62.5 minor units

        // HALF_EVEN: 62.5 rounds to 62 (nearest even), not 63 (HALF_UP behavior).
        assertEquals(62L, half.minorUnits)
    }

    @Test
    fun `sum extension folds a list of Money without precision loss`() {
        val amounts = listOf(
            Money.parse("10.10")!!,
            Money.parse("20.20")!!,
            Money.parse("0.01")!!
        )
        assertEquals(Money.parse("30.31"), amounts.sum())
    }

    @Test
    fun `isZero isPositive isNegative are mutually consistent`() {
        assertTrue(Money.ZERO.isZero)
        assertFalse(Money.ZERO.isPositive)
        assertFalse(Money.ZERO.isNegative)

        val positive = Money.parse("0.01")!!
        assertTrue(positive.isPositive)
        assertFalse(positive.isZero)
    }
}
