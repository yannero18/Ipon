package com.ipon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipon.app.data.model.TransactionCategory
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.model.IncomeCategory
import com.ipon.app.ui.icons.icon
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.KapeBrown

private fun getPastelBackground(category: TransactionCategory): Color =
    when (category) {
        ExpenseCategory.TRANSPO -> Color(0xFFE2EFF1)      // Ice Blue
        ExpenseCategory.FOOD -> Color(0xFFFBECE0)         // Soft Peach
        ExpenseCategory.BILLS -> Color(0xFFF7E6E2)        // Soft Rose
        ExpenseCategory.GROCERIES -> Color(0xFFE5EFE5)     // Sage Green
        ExpenseCategory.GOVERNMENT -> Color(0xFFF1EBF5)    // Soft Orchid
        ExpenseCategory.SHOPPING -> Color(0xFFFDECEE)      // Blossom Pink
        ExpenseCategory.HEALTH -> Color(0xFFE0F2F1)        // Pale Mint
        ExpenseCategory.ENTERTAINMENT -> Color(0xFFEDE7F6)  // Lavender
        ExpenseCategory.EDUCATION -> Color(0xFFE8EAF6)      // Soft Periwinkle
        ExpenseCategory.UTANG -> Color(0xFFEFEBE9)          // Soft Latte
        ExpenseCategory.PADALA -> Color(0xFFFCE4EC)         // Sweet Pink
        IncomeCategory.SALARY, IncomeCategory.FREELANCE, IncomeCategory.BUSINESS -> Color(0xFFE5EFE5) // Income Growth Sage
        IncomeCategory.REMITTANCE, IncomeCategory.GIFT -> Color(0xFFF5EBF0)
        else -> Color(0xFFF4EFEA)                          // Neutral Warm Sand
    }

/**
 * Icon + label pair for a category, replacing the repeated
 * "${category.emoji} ${category.displayName}" string-interpolation pattern
 * that was duplicated across Recurring, Envelopes, and Insights. One place
 * to keep icon sizing/spacing consistent rather than five slightly
 * different copies.
 */
@Composable
fun CategoryLabel(
    category: TransactionCategory,
    text: String = category.displayName,
    iconSize: Dp = 18.dp,
    tint: Color = KapeBrown,
    textColor: Color = KapeBrown,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    customIcon: String? = null,
    hasBackground: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val displayIcon = if (customIcon != null && customIcon.isNotEmpty()) customIcon else category.emoji
        
        if (hasBackground) {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(40.dp)
                    .clip(IponShapes.SquircleSm)
                    .background(getPastelBackground(category)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayIcon,
                    fontSize = 20.sp
                )
            }
        } else {
            Text(
                text = displayIcon,
                fontSize = iconSize.value.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Text(text = text, style = style.copy(fontWeight = FontWeight.Bold), color = textColor)
    }
}
