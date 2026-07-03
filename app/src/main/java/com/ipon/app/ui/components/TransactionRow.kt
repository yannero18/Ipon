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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.model.IncomeCategory
import com.ipon.app.data.model.Transaction
import com.ipon.app.data.model.resolveCategory
import com.ipon.app.ui.icons.icon
import com.ipon.app.ui.theme.HairlineBorder
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.TabularNumberStyle
import com.ipon.app.ui.theme.Terracotta
import com.ipon.app.ui.theme.JeepneyOrange
import java.text.SimpleDateFormat
import java.util.Locale

private fun categoryColors(category: String, type: TransactionType): Pair<Color, Color> =
    when (resolveCategory(category, type)) {
        ExpenseCategory.TRANSPO -> Color(0xFFE0F2FE) to Color(0xFF0284C7)      // Sky Blue 50 to 600
        ExpenseCategory.FOOD -> Color(0xFFFFEDD5) to Color(0xFFD97706)         // Orange/Amber 50 to 600
        ExpenseCategory.BILLS -> Color(0xFFFEE2E2) to Color(0xFFEF4444)        // Red 50 to 500
        ExpenseCategory.GROCERIES -> Color(0xFFD1FAE5) to Color(0xFF059669)     // Emerald 50 to 600
        ExpenseCategory.GOVERNMENT -> Color(0xFFF3E8FF) to Color(0xFF7C3AED)    // Purple 50 to 600
        IncomeCategory.SALARY, IncomeCategory.FREELANCE, IncomeCategory.BUSINESS ->
            Color(0xFFD1FAE5) to Color(0xFF059669)                             // Positive growth emerald
        IncomeCategory.REMITTANCE, IncomeCategory.GIFT -> Color(0xFFFCE7F3) to Color(0xFFDB2777) // Pink 50 to 600
        else -> Color(0xFFF1F5F9) to Color(0xFF475569)                         // Cool Grey 50 to 600
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
    modifier: Modifier = Modifier
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
    val categoryIcon = resolveCategory(transaction.category, transaction.type).icon()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(IponShapes.SquircleLg)
            .background(Color.White)
            .border(1.dp, HairlineBorder, IponShapes.SquircleLg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(IponShapes.SquircleSm)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = transaction.category,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = transaction.category,
                style = MaterialTheme.typography.bodyLarge,
                color = KapeBrown
            )
            val metaText = listOfNotNull(transaction.merchantRaw, transaction.note)
                .joinToString(" · ")
                .ifEmpty { formattedTime(transaction.occurredAtEpochMillis) }
            Text(
                text = metaText,
                style = MaterialTheme.typography.bodyMedium,
                color = KapeBrownSoft
            )
        }

        val isExpense = transaction.type == TransactionType.EXPENSE
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = (if (isExpense) "\u2212" else "+") + transaction.amount.formatPhp(),
                style = MaterialTheme.typography.bodyLarge.merge(TabularNumberStyle),
                color = if (isExpense) Terracotta else JeepneyOrange
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
