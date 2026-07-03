package com.ipon.app.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.model.IncomeCategory
import com.ipon.app.data.model.TransactionCategory

/**
 * Single source of truth mapping a [TransactionCategory] to its custom
 * vector icon. Used everywhere a category icon needs to render (transaction
 * rows, category chips, envelope/goal cards) instead of reading
 * `category.emoji` directly -- this is the seam that replaced emoji
 * app-wide without touching every call site's logic, only its icon
 * rendering.
 *
 * No `else` branch: TransactionCategory is a sealed interface implemented
 * by exactly ExpenseCategory and IncomeCategory, so this `when` is
 * exhaustive. Adding a new category to either enum without adding a case
 * here is a compile error, not a silent fallback to a generic icon.
 */
fun TransactionCategory.icon(): ImageVector = when (this) {
    ExpenseCategory.TRANSPO -> IponIcons.Transpo
    ExpenseCategory.FOOD -> IponIcons.Food
    ExpenseCategory.BILLS -> IponIcons.Bills
    ExpenseCategory.GROCERIES -> IponIcons.Groceries
    ExpenseCategory.SHOPPING -> IponIcons.Shopping
    ExpenseCategory.HEALTH -> IponIcons.Health
    ExpenseCategory.ENTERTAINMENT -> IponIcons.Entertainment
    ExpenseCategory.EDUCATION -> IponIcons.Education
    ExpenseCategory.GOVERNMENT -> IponIcons.Government
    ExpenseCategory.UTANG -> IponIcons.Utang
    ExpenseCategory.PADALA -> IponIcons.Padala
    ExpenseCategory.OTHER -> IponIcons.Other
    IncomeCategory.SALARY -> IponIcons.Salary
    IncomeCategory.FREELANCE -> IponIcons.Salary
    IncomeCategory.REMITTANCE -> IponIcons.Padala
    IncomeCategory.BUSINESS -> IponIcons.Shopping
    IncomeCategory.GIFT -> IponIcons.Padala
    IncomeCategory.OTHER -> IponIcons.Other
}
