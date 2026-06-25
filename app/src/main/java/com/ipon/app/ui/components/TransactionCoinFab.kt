package com.ipon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.RicePaper

/**
 * Section 4: "the transaction coin FAB" -- the one place Jeepney Orange is
 * used as a functional trigger rather than decoration. Kept as a true
 * circle (not a squircle) since it represents a coin being dropped in,
 * per Section 5's "settles, like a coin dropped into a jar" framing.
 */
@Composable
fun TransactionCoinFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(60.dp)
            .shadow(elevation = 14.dp, shape = IponShapes.FabCircle, ambientColor = JeepneyOrange)
            .clip(IponShapes.FabCircle)
            .background(JeepneyOrange)
            .clickable(
                interactionSource = MutableInteractionSource(),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+",
            color = RicePaper,
            fontSize = 30.sp
        )
    }
}
