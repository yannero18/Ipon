package com.ipon.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipon.app.ui.theme.HairlineBorder
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.RicePaperDeep
import com.ipon.app.ui.theme.TabularNumberStyle
import com.ipon.app.ui.theme.Terracotta
import com.ipon.app.util.Money

@Composable
fun DashboardSummary(
    piggyBankBalance: Money, // Liquid unallocated cash
    totalSavingsBalance: Money, // Goal saved balance
    totalIncome: Money,
    totalExpense: Money,
    modifier: Modifier = Modifier
) {
    // Calculate progress ratio (expenses over income)
    val progressRatio = remember(totalIncome, totalExpense) {
        if (totalIncome.isZero) 0f else (totalExpense.minorUnits.toFloat() / totalIncome.minorUnits.toFloat()).coerceIn(0f, 1f)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(durationMillis = 1000),
        label = "budgetProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_summary_card")
            .border(1.dp, HairlineBorder, IponShapes.SquircleLg),
        shape = IponShapes.SquircleLg,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Main title & header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MY ALKANSYA SUMMARY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = KapeBrownSoft
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(OceanTeal.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Offline Mode",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = OceanTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Grid Layout with uniform columns and proper weights
            // Row 1: Piggy Bank and Goal Savings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Piggy Bank (Alkansya) Balance
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Savings,
                            contentDescription = null,
                            tint = OceanTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Piggy Bank",
                            style = MaterialTheme.typography.labelMedium,
                            color = KapeBrownSoft,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = piggyBankBalance.formatPhp(),
                        style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                        color = KapeBrown,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Available to spend",
                        style = MaterialTheme.typography.bodySmall,
                        color = KapeBrownSoft,
                        fontSize = 10.sp
                    )
                }

                // Vertical Separator
                Box(
                    modifier = Modifier
                        .height(45.dp)
                        .width(1.dp)
                        .background(HairlineBorder)
                )

                // Column 2: Total Savings Balance
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = null,
                            tint = OceanTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Goal Savings",
                            style = MaterialTheme.typography.labelMedium,
                            color = KapeBrownSoft,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = totalSavingsBalance.formatPhp(),
                        style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                        color = OceanTeal,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Allocated to goals",
                        style = MaterialTheme.typography.bodySmall,
                        color = KapeBrownSoft,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.HorizontalDivider(color = HairlineBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Row 2: Budget Spent Progress and Inflow/Outflow Overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 3: Monthly Budget Progress Dial
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(48.dp)
                    ) {
                        // Background circle arc
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = Color(0xFFF1F5F9),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        
                        // Active progress circle arc with gradient colors
                        val progressColor = if (progressRatio > 0.85f) Terracotta else OceanTeal
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = progressColor,
                                startAngle = -90f,
                                sweepAngle = animatedProgress * 360f,
                                useCenter = false,
                                style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Percentage overlay
                        Text(
                            text = String.format("%.0f%%", progressRatio * 100),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = KapeBrown,
                            fontSize = 10.sp
                        )
                    }
                    Column {
                        Text(
                            text = "Budget Spent",
                            style = MaterialTheme.typography.labelMedium,
                            color = KapeBrownSoft,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Limit tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = KapeBrownSoft,
                            fontSize = 10.sp
                        )
                    }
                }

                // Vertical Separator
                Box(
                    modifier = Modifier
                        .height(45.dp)
                        .width(1.dp)
                        .background(HairlineBorder)
                )

                // Column 4: Inflow / Outflow summary details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (progressRatio > 0.85f) Terracotta else JeepneyOrange)
                        )
                        Text(
                            text = "Outflow",
                            style = MaterialTheme.typography.labelMedium,
                            color = KapeBrownSoft,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = totalExpense.formatPhp(),
                        style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                        color = KapeBrown,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Inflow: ${totalIncome.formatPhp()}",
                        style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                        color = KapeBrownSoft,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
