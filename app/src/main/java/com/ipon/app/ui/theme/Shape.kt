package com.ipon.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Section 4: "Fluid Asymmetry" -- organic, asymmetrical squircle profiles
 * rather than uniform rounding. The proposal's example is
 * (topStart = 24.dp, topEnd = 8.dp); this extends that same diagonal
 * asymmetry to all four corners so the shape reads as one deliberate
 * gesture rather than two rounded corners and two square ones.
 *
 * Compose's RoundedCornerShape resolves topStart/topEnd to physical
 * left/right based on layout direction, so this also correctly mirrors
 * under RTL locales without any extra handling.
 */
object IponShapes {
    /** Primary container shape: cards, the balance card, transaction rows. */
    val SquircleLg = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 8.dp,
        bottomEnd = 24.dp,
        bottomStart = 8.dp
    )

    /** Smaller version for icon chips, badges, small tappable elements. */
    val SquircleSm = RoundedCornerShape(
        topStart = 14.dp,
        topEnd = 6.dp,
        bottomEnd = 14.dp,
        bottomStart = 6.dp
    )

    /** Medium version for list items, action rows, and smaller cards. */
    val SquircleMd = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 6.dp,
        bottomEnd = 18.dp,
        bottomStart = 6.dp
    )

    /** The transaction coin FAB stays a true circle -- it represents a coin, not a card. */
    val FabCircle = RoundedCornerShape(50)
}
