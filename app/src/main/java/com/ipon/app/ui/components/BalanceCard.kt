package com.ipon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.TabularNumberStyle
import com.ipon.app.util.Money

@Composable
fun BalanceCard(
    availableThisMonth: Money,
    income: Money,
    expense: Money,
    estimatedDaysOfRunway: Int? = null,
    modifier: Modifier = Modifier
) {
    // Ensure all padding values are safe and positive
    val safePaddingH = 24.dp.coerceAtLeast(0.dp)
    val safePaddingV = 24.dp.coerceAtLeast(0.dp)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(IponShapes.SquircleLg)
            .background(OceanTeal)
            .padding(horizontal = safePaddingH, vertical = safePaddingV)
    ) {
<<<<<<< HEAD
        // Decorative organic blob, echoing the squircle motif at large scale.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-40).dp)
                .clip(IponShapes.SquircleLg)
                .background(OceanTealLight.copy(alpha = 0.35f))
=======
        Text(
            text = "AVAILABLE THIS MONTH",
            style = MaterialTheme.typography.labelSmall,
            color = RicePaper.copy(alpha = 0.7f)
>>>>>>> b7c5946b83af4c6274c183da1696202e902eecb5
        )

        Text(
            text = availableThisMonth.formatPhp(),
            style = MaterialTheme.typography.headlineMedium.merge(TabularNumberStyle),
            color = RicePaper,
            modifier = Modifier.padding(top = 6.dp.coerceAtLeast(0.dp))
        )

        Row(
            modifier = Modifier.padding(top = 16.dp.coerceAtLeast(0.dp)),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            BalanceSubItem(label = "In", amount = income, valueColor = Color(0xFFB8E3CC))
            BalanceSubItem(label = "Out", amount = expense, valueColor = Color(0xFFF4C5B8))
        }

        // Only render if days is valid and non-negative
        if (estimatedDaysOfRunway != null && estimatedDaysOfRunway >= 0) {
            Text(
                text = when (estimatedDaysOfRunway) {
                    0 -> "At this pace, your balance is already spent"
                    1 -> "At this pace, about 1 day of balance left"
                    else -> "At this pace, about $estimatedDaysOfRunway days of balance left"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = RicePaper.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 14.dp.coerceAtLeast(0.dp))
            )
        }
    }
}

@Composable
private fun BalanceSubItem(label: String, amount: Money, valueColor: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = RicePaper.copy(alpha = 0.6f)
        )
        Text(
            text = amount.formatPhp(),
            style = MaterialTheme.typography.bodyLarge.merge(TabularNumberStyle),
            color = valueColor
        )
    }
}
