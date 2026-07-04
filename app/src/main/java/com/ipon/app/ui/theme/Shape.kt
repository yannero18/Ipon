package com.ipon.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val OrganicSquircleShape = RoundedCornerShape(24.dp)

/**
 * Modern Editorial Shape System
 * Aligned with the high-fidelity HTML spec using clean, symmetric curves
 * for a premium and cohesive look.
 */
object IponShapes {
    /** Primary container shape: cards, balance card, large dialogues. */
    val SquircleLg = RoundedCornerShape(24.dp)

    val OrganicSquircleShape = SquircleLg

    /** Smaller version for icon chips, badges, emoji container backgrounds. */
    val SquircleSm = RoundedCornerShape(12.dp)

    /** Medium version for transaction rows, list elements, action containers. */
    val SquircleMd = RoundedCornerShape(16.dp)

    /** Standard action and navigation elements. */
    val FabCircle = RoundedCornerShape(50)

    /** Precise asymmetric squircle shape per the design specification. */
    val AsymmetricSquircle = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 8.dp,
        bottomEnd = 24.dp,
        bottomStart = 8.dp
    )
}
