package com.ipon.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipon.app.data.model.EnvelopeProgress
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.TabularNumberStyle
import com.ipon.app.util.Money
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Vibrant Fintech-ready Palette corresponding to category colors
fun getEnvelopeColor(category: ExpenseCategory): Color = when (category) {
    ExpenseCategory.TRANSPO -> Color(0xFF0284C7)      // Sky Blue 600
    ExpenseCategory.FOOD -> Color(0xFFD97706)         // Orange/Amber 600
    ExpenseCategory.BILLS -> Color(0xFFEF4444)        // Red 500
    ExpenseCategory.GROCERIES -> Color(0xFF059669)     // Emerald 600
    ExpenseCategory.GOVERNMENT -> Color(0xFF7C3AED)    // Purple 600
    ExpenseCategory.SHOPPING -> Color(0xFFDB2777)      // Pink 600
    ExpenseCategory.HEALTH -> Color(0xFF0D9488)        // Teal 600
    ExpenseCategory.ENTERTAINMENT -> Color(0xFF8B5CF6)  // Violet 500
    ExpenseCategory.EDUCATION -> Color(0xFF6366F1)      // Indigo 500
    ExpenseCategory.UTANG -> Color(0xFFB45309)          // Amber/Brown 700
    ExpenseCategory.PADALA -> Color(0xFFF43F5E)         // Rose 500
    ExpenseCategory.OTHER -> Color(0xFF64748B)          // Slate 500
}

@Composable
fun InteractiveEnvelopeDonutChart(
    envelopes: List<EnvelopeProgress>,
    modifier: Modifier = Modifier
) {
    // Filter out envelopes with 0 cap to avoid division issues, unless all are zero
    val validEnvelopes = remember(envelopes) {
        val nonZero = envelopes.filter { it.cap.minorUnits > 0 }
        if (nonZero.isEmpty() && envelopes.isNotEmpty()) envelopes else nonZero
    }

    if (validEnvelopes.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = com.ipon.app.ui.icons.IponIcons.Alkansya,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = KapeBrown.copy(alpha = 0.12f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No active envelopes allocated yet.\nAdd budgets in the Plan tab!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KapeBrownSoft,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val totalCapMinor = remember(validEnvelopes) {
        validEnvelopes.sumOf { it.cap.minorUnits }
    }
    val totalCapMoney = remember(totalCapMinor) { Money.ofMinorUnits(totalCapMinor) }

    var selectedIndex by remember { mutableIntStateOf(-1) }
    
    // Reset selected index if envelopes data changes
    LaunchedEffect(validEnvelopes) {
        selectedIndex = -1
    }

    // Animation progress
    var animatedProgress by remember { mutableStateOf(0f) }
    val animationFraction by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "donut_sweep_anim"
    )

    LaunchedEffect(Unit) {
        animatedProgress = 1f
    }

    // Compute start and sweep angles
    val slices = remember(validEnvelopes, totalCapMinor) {
        var accumulatedAngle = 0f
        validEnvelopes.map { progress ->
            val fraction = if (totalCapMinor == 0L) 1f / validEnvelopes.size else progress.cap.minorUnits.toFloat() / totalCapMinor
            val sweep = fraction * 360f
            val sliceStart = accumulatedAngle
            accumulatedAngle += sweep
            SliceData(
                progress = progress,
                startAngle = sliceStart,
                sweepAngle = sweep,
                color = getEnvelopeColor(progress.category),
                percentage = fraction * 100f
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, shape = MaterialTheme.shapes.medium)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val strokeWidthDp = 24.dp
            val strokeWidthPx = with(LocalDensity.current) { strokeWidthDp.toPx() }
            
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(slices) {
                        detectTapGestures { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            val distance = sqrt(dx * dx + dy * dy)
                            
                            val outerRadius = size.width / 2f
                            val innerRadius = outerRadius - strokeWidthPx
                            
                            // If user clicked inside the ring bounds
                            if (distance in innerRadius..outerRadius) {
                                var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angleDeg < 0) angleDeg += 360f
                                
                                val clickedIdx = slices.indexOfFirst { slice ->
                                    val start = slice.startAngle
                                    val end = slice.startAngle + slice.sweepAngle
                                    if (end > 360f && angleDeg < (end - 360f)) {
                                        true
                                    } else {
                                        angleDeg >= start && angleDeg <= end
                                    }
                                }
                                if (clickedIdx != -1) {
                                    selectedIndex = if (selectedIndex == clickedIdx) -1 else clickedIdx
                                }
                            } else {
                                selectedIndex = -1
                            }
                        }
                    }
            ) {
                val canvasSize = size.width
                val arcRadius = canvasSize - strokeWidthPx
                val rectSize = Size(arcRadius, arcRadius)
                val topLeftOffset = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)

                slices.forEachIndexed { index, slice ->
                    val isSelected = selectedIndex == index
                    val activeStrokeWidth = if (isSelected) strokeWidthPx * 1.25f else strokeWidthPx
                    val start = slice.startAngle
                    val sweep = slice.sweepAngle * animationFraction

                    drawArc(
                        color = slice.color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = rectSize,
                        style = Stroke(width = activeStrokeWidth, cap = StrokeCap.Butt)
                    )
                }
            }

            // Center Readout Details - Premium dial inset
            Box(
                modifier = Modifier
                    .size(155.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, KapeBrownSoft.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (selectedIndex == -1) {
                        Text(
                            text = "TOTAL BUDGET",
                            style = MaterialTheme.typography.labelSmall,
                            color = KapeBrownSoft,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = totalCapMoney.formatPhp(),
                            style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                            color = KapeBrown,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${validEnvelopes.size} Envelopes",
                            style = MaterialTheme.typography.bodySmall,
                            color = KapeBrownSoft
                        )
                    } else {
                        val activeSlice = slices[selectedIndex]
                        val displayLabel = buildString {
                            if (!activeSlice.progress.customIcon.isNullOrEmpty()) {
                                append(activeSlice.progress.customIcon)
                                append(" ")
                            }
                            append(activeSlice.progress.category.displayName.uppercase())
                        }
                        Text(
                            text = displayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = activeSlice.color,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeSlice.progress.cap.formatPhp(),
                            style = MaterialTheme.typography.titleLarge.merge(TabularNumberStyle),
                            color = KapeBrown,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("%.1f%% of total", activeSlice.percentage),
                            style = MaterialTheme.typography.bodySmall,
                            color = KapeBrownSoft
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Premium Flow Legend (Column of Rows) - avoiding nested scroll issues completely
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.chunked(2).forEach { rowSlices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowSlices.forEach { slice ->
                        val index = slices.indexOf(slice)
                        val isSelected = selectedIndex == index
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) slice.color.copy(alpha = 0.12f)
                                    else Color(0xFFF8FAFC)
                                )
                                .clickable {
                                    selectedIndex = if (selectedIndex == index) -1 else index
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(slice.color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val displayLabel = buildString {
                                    if (!slice.progress.customIcon.isNullOrEmpty()) {
                                        append(slice.progress.customIcon)
                                        append(" ")
                                    }
                                    append(slice.progress.category.displayName)
                                }
                                Text(
                                    text = displayLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = KapeBrown,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = slice.progress.cap.formatPhp(),
                                    style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                                    color = KapeBrownSoft
                                )
                            }
                        }
                    }
                    if (rowSlices.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

data class SliceData(
    val progress: EnvelopeProgress,
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Color,
    val percentage: Float
)
