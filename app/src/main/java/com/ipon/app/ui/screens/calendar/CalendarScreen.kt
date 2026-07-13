package com.ipon.app.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.theme.HairlineBorder
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaperDeep
import com.ipon.app.ui.theme.TabularNumberStyle
import com.ipon.app.ui.theme.Terracotta

private val WEEKDAY_LABELS = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
fun CalendarScreen(viewModelFactory: IponViewModelFactory) {
    val viewModel: CalendarViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    var selectedDay by remember { mutableStateOf<CalendarDay?>(null) }

    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = uiState.monthLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = KapeBrown
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = IponShapes.SquircleLg,
                    colors = CardDefaults.cardColors(containerColor = RicePaperDeep),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Today", style = MaterialTheme.typography.labelSmall, color = KapeBrownSoft)
                            Text(
                                text = uiState.startingBalance.formatPhp(),
                                style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                                color = KapeBrown,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Projected, end of month",
                                style = MaterialTheme.typography.labelSmall,
                                color = KapeBrownSoft
                            )
                            Text(
                                text = uiState.endOfMonthProjectedBalance.formatPhp(),
                                style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                                color = if (uiState.endOfMonthProjectedBalance.minorUnits >= uiState.startingBalance.minorUnits) OceanTeal else Terracotta,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    WEEKDAY_LABELS.forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = KapeBrownSoft
                        )
                    }
                }
            }

            items(uiState.weeks) { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        CalendarDayCell(
                            day = day,
                            isSelected = selectedDay != null && day.isInCurrentMonth && selectedDay?.dayOfMonth == day.dayOfMonth,
                            onClick = {
                                if (day.isInCurrentMonth) {
                                    selectedDay = if (selectedDay?.dayOfMonth == day.dayOfMonth) null else day
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(color = OceanTeal)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Payday / income", style = MaterialTheme.typography.labelSmall, color = KapeBrownSoft)
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendDot(color = Terracotta)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bill due", style = MaterialTheme.typography.labelSmall, color = KapeBrownSoft)
                }
            }

            selectedDay?.let { day ->
                item {
                    SelectedDayDetail(day = day)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val netMinorForDay = remember(day) {
        day.events.sumOf { if (it.isIncome) it.amount.minorUnits else -it.amount.minorUnits }
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(IponShapes.SquircleSm)
            .background(
                when {
                    !day.isInCurrentMonth -> Color.Transparent
                    isSelected -> OceanTeal
                    day.isToday -> OceanTeal.copy(alpha = 0.15f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (day.isToday && !isSelected) {
                    Modifier.border(width = 1.dp, color = OceanTeal, shape = IponShapes.SquircleSm)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = day.isInCurrentMonth, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (day.isInCurrentMonth) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) Color.White else if (day.isPast) KapeBrownSoft else KapeBrown,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (day.isPayday) {
                        LegendDot(color = if (isSelected) Color.White else OceanTeal)
                    }
                    if (day.events.isNotEmpty()) {
                        LegendDot(
                            color = if (isSelected) Color.White else if (netMinorForDay >= 0) OceanTeal else Terracotta
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun SelectedDayDetail(day: CalendarDay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = IponShapes.SquircleLg,
        colors = CardDefaults.cardColors(containerColor = RicePaperDeep),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Day ${day.dayOfMonth}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KapeBrown
            )

            if (day.isPayday) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(color = OceanTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Payday", style = MaterialTheme.typography.bodySmall, color = KapeBrownSoft)
                }
            }

            if (day.events.isEmpty() && !day.isPayday) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Nothing scheduled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KapeBrownSoft
                )
            }

            day.events.forEach { event ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = event.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = KapeBrown
                    )
                    Text(
                        text = (if (event.isIncome) "+" else "-") + event.amount.formatPhp(),
                        style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                        fontWeight = FontWeight.Bold,
                        color = if (event.isIncome) OceanTeal else Terracotta
                    )
                }
            }

            if (day.projectedBalance != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = HairlineBorder)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Projected balance", style = MaterialTheme.typography.bodySmall, color = KapeBrownSoft)
                    Text(
                        text = day.projectedBalance.formatPhp(),
                        style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                        fontWeight = FontWeight.Bold,
                        color = KapeBrown
                    )
                }
            }
        }
    }
}
