package com.ipon.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.model.IncomeCategory
import com.ipon.app.data.model.Transaction
import com.ipon.app.data.model.resolveCategory
import com.ipon.app.ui.icons.icon
import com.ipon.app.ui.theme.HairlineBorder
import com.ipon.app.ui.theme.InterFamily
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.TabularNumberStyle
import com.ipon.app.ui.theme.Terracotta
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.WarmCream
import java.text.SimpleDateFormat
import java.util.Locale

private fun categoryColors(category: String, type: TransactionType): Pair<Color, Color> =
    when (resolveCategory(category, type)) {
        ExpenseCategory.TRANSPO -> Color(0xFFE2EFF1) to Color(0xFF1D5D6B)      // Soft pastel ice blue/teal
        ExpenseCategory.FOOD -> Color(0xFFFBECE0) to Color(0xFFD1664F)         // Soft pastel orange/amber
        ExpenseCategory.BILLS -> Color(0xFFF7E6E2) to Color(0xFFD1664F)        // Soft pastel rose/red
        ExpenseCategory.GROCERIES -> Color(0xFFE5EFE5) to Color(0xFF1D5D6B)     // Soft pastel sage green
        ExpenseCategory.GOVERNMENT -> Color(0xFFF1EBF5) to Color(0xFF4A3B32)    // Soft pastel purple
        IncomeCategory.SALARY, IncomeCategory.FREELANCE, IncomeCategory.BUSINESS ->
            Color(0xFFE5EFE5) to Color(0xFF1D5D6B)                             // Positive growth sage
        IncomeCategory.REMITTANCE, IncomeCategory.GIFT -> Color(0xFFF5EBF0) to Color(0xFF4A3B32) // Pink
        else -> Color(0xFFF4EFEA) to Color(0xFF7A685C)                         // Soft warm grey/beige
    }

/**
 * Section 5: "Physically-Modeled Motion -- transitions use spring-based
 * animation rather than fixed-duration eased curves. A new entry doesn't
 * slide in, it settles, like a coin dropped into a jar, with a touch of
 * overshoot before resting."
 *
 * Implemented with Compose's spring() animation spec using a deliberately
 * low damping ratio (DampingRatioMediumBounce) so the row visibly overshoots
 * past its resting scale before settling, rather than a critically-damped
 * ease that arrives without any bounce.
 */
@Composable
fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
    isNewlyAdded: Boolean = false,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    hasCardContainer: Boolean = true
) {
    var hasAppeared by remember(transaction.id) { mutableStateOf(!isNewlyAdded) }

    LaunchedEffect(transaction.id) {
        if (isNewlyAdded) hasAppeared = true
    }

    val scale by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "transactionRowSettle"
    )

    val (bgColor, accentColor) = categoryColors(transaction.category, transaction.type)
    val inkColor = KapeBrown

    val rowModifier = if (hasCardContainer) {
        modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(IponShapes.SquircleMd)
            .background(Color.White)
            .border(1.dp, HairlineBorder, IponShapes.SquircleMd)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    } else {
        modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(IponShapes.SquircleSm)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = resolveCategory(transaction.category, transaction.type).emoji,
                fontSize = 22.sp
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(1f)
        ) {
            Text(
                text = transaction.category,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = inkColor
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            val metaText = listOfNotNull(transaction.merchantRaw, transaction.note)
                .joinToString(" · ")
                .ifEmpty { formattedTime(transaction.occurredAtEpochMillis) }
            Text(
                text = metaText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = inkColor.copy(alpha = 0.65f)
                )
            )
        }

        val isExpense = transaction.type == TransactionType.EXPENSE
        val rawAmountText = transaction.amount.formatPhp()
        val displayAmount = if (isExpense) {
            if (rawAmountText.startsWith("-") || rawAmountText.startsWith("\u2212")) {
                "-" + rawAmountText.substring(1)
            } else {
                "-$rawAmountText"
            }
        } else {
            if (rawAmountText.startsWith("+")) rawAmountText else "+$rawAmountText"
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = displayAmount,
                style = MaterialTheme.typography.bodyLarge.merge(TabularNumberStyle).copy(
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = inkColor
            )
            if (onDeleteClick != null) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Terracotta.copy(alpha = 0.08f))
                        .clickable(onClick = onDeleteClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = Terracotta,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private fun formattedTime(epochMillis: Long): String =
    SimpleDateFormat("h:mm a", Locale.US).format(epochMillis)
