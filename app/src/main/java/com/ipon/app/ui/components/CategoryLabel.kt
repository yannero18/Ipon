package com.ipon.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ipon.app.data.model.TransactionCategory
import com.ipon.app.ui.icons.icon
import com.ipon.app.ui.theme.KapeBrown

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
    iconSize: Dp = 16.dp,
    tint: Color = KapeBrown,
    textColor: Color = KapeBrown,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = category.icon(),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .padding(end = 6.dp)
        )
        Text(text = text, style = style, color = textColor)
    }
}
