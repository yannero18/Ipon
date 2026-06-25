package com.ipon.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.Category
import com.ipon.app.data.model.Transaction
import com.ipon.app.ui.theme.HairlineBorder
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.TabularNumberStyle
import java.text.SimpleDateFormat
import java.util.Locale

private fun categoryColors(category: String): Pair<Color, Color> = when (Category.fromDisplayName(category)) {
    Category.TRANSPO -> Color(0xFFE4EEF0) to OceanTeal
    Category.FOOD -> Color(0xFFFCE9D9) to Color(0xFFF28C38)
    Category.BILLS -> Color(0xFFF3E3DF) to Color(0xFFD1664F)
    Category.GROCERIES -> Color(0xFFE9EFE0) to Color(0xFF5C7A3D)
    else -> Color(0xFFEFEAE2) to KapeBrownSoft
}

/**
 * Section 5: "Physically-Modeled Motion -- transitions use spring-based
 * animation rather than fixed-duration eased curves. A new entry doesn't
 * slide in, it settles, like a coin dropped into a jar, with a touch of
 * overshoot before resting."
 *
 * Implemented with Compose's spring() animation spec using a deliberately
 * low damping ratio (DampingRatioMediumBouncy) so the row visibly overshoots
 * past its resting scale before settling, rather than a critically-damped
 * ease that arrives without any bounce.
 */
@Composable
fun TransactionRow(
    transaction: Transaction,
    isNewlyAdded: Boolean = false,
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

    val (bgColor, _) = categoryColors(transaction.category)
    val emoji = Category.fromDisplayName(transaction.category).emoji

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(IponShapes.SquircleLg)
            .background(Color.White)
            .border(1.dp, HairlineBorder, IponShapes.SquircleLg)
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
            Text(text = emoji, style = MaterialTheme.typography.bodyLarge)
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
        Text(
            text = (if (isExpense) "\u2212" else "+") + transaction.amount.formatPhp(),
            style = MaterialTheme.typography.bodyLarge.merge(TabularNumberStyle),
            color = if (isExpense) KapeBrown else OceanTeal
        )
    }
}

private fun formattedTime(epochMillis: Long): String =
    SimpleDateFormat("h:mm a", Locale.US).format(epochMillis)
