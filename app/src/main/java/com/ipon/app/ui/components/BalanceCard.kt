package com.ipon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.TabularSerifNumberStyle
import com.ipon.app.util.Money

// Localized Color Palette matching CSS design tokens
object KapePalette {
    val BackgroundWarm = Color(0xFFF6EDE0)    // Warm, slightly saturated amber textured rice-paper off-white
    val HeroCardSolid = Color(0xFF4E3729)     // Rich roasted clay espresso brown ink for solid ink/cards
    val AccentTerracotta = Color(0xFFD1664F)  // Warm Earth Accent
    val TextPrimary = Color(0xFF4E3729)       // Rich roasted clay espresso brown ink for text
    val TextLight = Color(0xFFFDF9F0)         // Soft warm organic cream text
    
    // Deep slate-teal palette for the premium contrast anchor card
    val SlateTealStart = Color(0xFF1D5D6B)    // Premium Deep Slate Teal (OceanTeal)
    val SlateTealMedium = Color(0xFF164954)   // Muted Marine
    val SlateTealEnd = Color(0xFF0C2B32)      // Midnight Teal
    val MockupTealHero = Color(0xFF1D5D6B)    // Premium Teal Anchor
    val GreenIncome = Color(0xFF8CE1C2)       // Soft Muted incoming green
    val AccentCoral = Color(0xFFF6A998)       // Soft Outgoing text accent
}

@Composable
fun BalanceCard(
    availableThisMonth: Money,
    income: Money,
    expense: Money,
    totalSavings: Money,
    modifier: Modifier = Modifier,
    accountName: String = "Yannero",
    estimatedDaysOfRunway: Int? = null,
    // NEW PAYDAY VARIABLES
    daysUntilPayday: Int? = null,
    safeDailySpend: Money = Money.ZERO
) {
    // Exact uniform squircle shape (20dp) from your mockup design
    val cardShape = RoundedCornerShape(20.dp)

    // Split balance into whole number and decimal cents parts for high-contrast sizing
    val rawAmountInDouble = availableThisMonth.minorUnits.toDouble() / 100.0
    val formattedBalance = String.format("%,.2f", rawAmountInDouble)
    val parts = formattedBalance.split(".")
    val wholeAmount = parts[0]
    val centsAmount = parts.getOrNull(1) ?: "00"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = OceanTeal,
                shape = cardShape
            )
            .drawBehind {
                val h = this.size.height
                val w = this.size.width

                // Draw a beautiful, elegant semi-transparent overlapping backdrop curve on the right edge
                drawCircle(
                    color = Color(0x10FFFFFF), // ~6% opacity white
                    radius = h * 0.9f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.98f, h * 0.35f)
                )
                drawCircle(
                    color = Color(0x06FFFFFF), // ~2% opacity white
                    radius = h * 0.6f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.98f, h * 0.35f)
                )
            }
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Label section
            Text(
                text = "AVAILABLE THIS MONTH",
                color = RicePaper.copy(alpha = 0.65f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Massive balance with stepped decimal numbers
            Text(
                text = buildAnnotatedString {
                    // Currency sign matched in font size and style beautifully
                    withStyle(style = SpanStyle(
                        fontSize = 32.sp, 
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    )) {
                        append("₱")
                    }
                    // Bold main display numbers
                    withStyle(style = SpanStyle(
                        fontSize = 44.sp, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        fontFeatureSettings = "tnum"
                    )) {
                        append(wholeAmount)
                    }
                    // Decimal parts softly stepped down
                    withStyle(style = SpanStyle(
                        fontSize = 28.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = RicePaper.copy(alpha = 0.9f),
                        fontFeatureSettings = "tnum"
                    )) {
                        append(".$centsAmount")
                    }
                },
                color = RicePaper,
                style = TabularSerifNumberStyle
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Metrics section: In, Out, Saved distributed as columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    label = "In", 
                    amount = income, 
                    amountColor = KapePalette.GreenIncome,./
                    modifier = Modifier.weight(1f)
                )
                MetricColumn(
                    label = "Out", 
                    amount = expense, 
                    amountColor = KapePalette.AccentCoral,
                    modifier = Modifier.weight(1f)
                )
                MetricColumn(
                    label = "Saved", 
                    amount = totalSavings, 
                    amountColor = RicePaper,
                    modifier = Modifier.weight(1f)
                )
            }

            // Payday / Runway Indicator
            if (daysUntilPayday != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = RicePaper.copy(alpha = 0.15f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (daysUntilPayday == 0) {
                        Text(
                            text = "It's Payday! 🎉", 
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
                            color = RicePaper
                        )
                    } else {
                        Text(
                            text = "Safe to spend: ${safeDailySpend.formatPhp()} / day",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFAEE2C9) // Soft mint green
                        )
                        Text(
                            text = "$daysUntilPayday days left",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RicePaper.copy(alpha = 0.7f)
                        )
                    }
                }
            } else if (estimatedDaysOfRunway != null && estimatedDaysOfRunway >= 0) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = RicePaper.copy(alpha = 0.15f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = when (estimatedDaysOfRunway) {
                        0 -> "At this pace, your balance is spent"
                        1 -> "At this pace, about 1 day left"
                        else -> "At this pace, about $estimatedDaysOfRunway days of balance left"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = RicePaper.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    amount: Money,
    amountColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = RicePaper.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = "tnum")) {
                    append("₱")
                }
                withStyle(style = SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")) {
                    append(String.format("%,.2f", amount.minorUnits.toDouble() / 100.0))
                }
            },
            color = amountColor,
            style = TabularSerifNumberStyle
        )
    }
}