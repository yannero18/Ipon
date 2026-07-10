package com.ipon.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.Terracotta

/**
 * A hand-picked icon + accent-color pairing for a goal's ring avatar,
 * standing in for a raw emoji -- the GoSave-style "little illustrated
 * badge" look, built entirely from icons already bundled with the app
 * (material-icons-extended) rather than downloaded artwork, so it costs
 * nothing at runtime and needs no network.
 */
data class GoalIconChoice(
    val key: String,
    val icon: ImageVector,
    val accentColor: Color
)

/**
 * [Goal.emoji] is reused as a general "appearance key" rather than only
 * ever holding a literal emoji character -- if it matches an entry here,
 * [GoalRingAvatar] renders the icon+color badge; if it doesn't (including
 * every goal created before this catalog existed, which stored a real
 * emoji like "🎯"), it falls back to rendering the string as literal emoji
 * text exactly like before. No migration needed either way.
 */
object GoalIconCatalog {
    val choices: List<GoalIconChoice> = listOf(
        GoalIconChoice("target", Icons.Filled.Flag, JeepneyOrange),
        GoalIconChoice("wallet", Icons.Filled.AccountBalanceWallet, OceanTeal),
        GoalIconChoice("motorcycle", Icons.Filled.TwoWheeler, Terracotta),
        GoalIconChoice("car", Icons.Filled.DirectionsCar, KapeBrownSoft),
        GoalIconChoice("beach", Icons.Filled.BeachAccess, OceanTeal),
        GoalIconChoice("home", Icons.Filled.Home, Terracotta),
        GoalIconChoice("laptop", Icons.Filled.Laptop, KapeBrownSoft),
        GoalIconChoice("shopping", Icons.Filled.ShoppingBag, JeepneyOrange),
        GoalIconChoice("health", Icons.Filled.LocalHospital, Terracotta),
        GoalIconChoice("school", Icons.Filled.School, OceanTeal),
        GoalIconChoice("gift", Icons.Filled.CardGiftcard, JeepneyOrange),
        GoalIconChoice("food", Icons.Filled.Restaurant, KapeBrownSoft)
    )

    fun find(key: String?): GoalIconChoice? = key?.let { k -> choices.find { it.key == k } }
}
