package com.example.englishword.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishword.ui.theme.EnglishWordTheme
import com.example.englishword.ui.theme.MasteryLevel1
import com.example.englishword.ui.theme.MasteryLevel2
import com.example.englishword.ui.theme.MasteryLevel3
import com.example.englishword.ui.theme.MasteryLevel4
import com.example.englishword.ui.theme.MasteryLevel5
import com.example.englishword.ui.theme.StreakOrange
import java.time.LocalDate

// ============================================================
// Data Classes
// ============================================================

// ============================================================
// 1. WeeklyBarChart
// ============================================================

private val DayLabelsJapanese = listOf("月", "火", "水", "木", "金", "土", "日")

/**
 * A bar chart displaying study counts for each day of the week.
 *
 * Features:
 * - 7 vertical bars with rounded top corners
 * - Japanese day labels on the X-axis
 * - Word count values above each bar
 * - Current day highlighted with a distinct color
 * - Subtle grid lines for readability
 * - Animated bar growth on first composition
 *
 * @param data List of 7 [DailyStudyData] entries, one for each day (Mon-Sun)
 * @param modifier Modifier for the composable
 */
@Composable
fun WeeklyBarChart(
    data: List<DailyStudyData>,
    modifier: Modifier = Modifier
) {
    require(data.size == 7) { "WeeklyBarChart requires exactly 7 data points" }

    val primaryColor = MaterialTheme.colorScheme.primary
    val highlightColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant

    val todayIndex = remember {
        // java.time.DayOfWeek: MONDAY=1, SUNDAY=7. Convert to 0-indexed (Mon=0).
        LocalDate.now().dayOfWeek.value - 1
    }

    val maxCount = remember(data) { data.maxOfOrNull { it.count } ?: 1 }

    // Animate bar heights from 0 to 1
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val density = LocalDensity.current
    val labelTextSizePx = with(density) { 11.sp.toPx() }
    val valueTextSizePx = with(density) { 10.sp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val bottomPadding = 32f // Space for day labels
        val topPadding = 24f   // Space for value labels above bars
        val sidePadding = 16f

        val chartWidth = canvasWidth - sidePadding * 2
        val chartHeight = canvasHeight - bottomPadding - topPadding

        val barCount = 7
        val totalGapRatio = 0.4f // 40% of space is gaps
        val barWidth = chartWidth / (barCount + (barCount - 1) * totalGapRatio / (1f - totalGapRatio))
        val gapWidth = barWidth * totalGapRatio / (1f - totalGapRatio)

        // Draw subtle horizontal grid lines
        val gridLineCount = 4
        for (i in 1..gridLineCount) {
            val y = topPadding + chartHeight * (1f - i.toFloat() / gridLineCount)
            drawLine(
                color = outlineVariantColor.copy(alpha = 0.3f),
                start = Offset(sidePadding, y),
                end = Offset(canvasWidth - sidePadding, y),
                strokeWidth = 1f
            )
        }

        // Draw bars and labels
        for (i in 0 until barCount) {
            val x = sidePadding + i * (barWidth + gapWidth)
            val fraction = if (maxCount > 0) data[i].count.toFloat() / maxCount else 0f
            val animatedFraction = fraction * animationProgress.value
            val barHeight = chartHeight * animatedFraction

            val isToday = i == todayIndex
            val barColor = if (isToday) highlightColor else primaryColor

            // Bar with rounded top corners
            if (barHeight > 0) {
                val cornerRadius = (barWidth / 3f).coerceAtMost(8f)
                val barTop = topPadding + chartHeight - barHeight
                val barBottom = topPadding + chartHeight

                // Draw the bar using a rounded rect, clipped at bottom
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, barTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )

                // Overwrite bottom corners to make them square
                if (barHeight > cornerRadius * 2) {
                    drawRect(
                        color = barColor,
                        topLeft = Offset(x, barBottom - cornerRadius),
                        size = Size(barWidth, cornerRadius)
                    )
                }
            } else {
                // Draw a thin placeholder line for zero values
                drawRoundRect(
                    color = surfaceVariantColor,
                    topLeft = Offset(x, topPadding + chartHeight - 3f),
                    size = Size(barWidth, 3f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
            }

            // Value label above bar
            if (data[i].count > 0) {
                val valueY = topPadding + chartHeight - barHeight - 8f
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = if (isToday) {
                            barColor.copy(alpha = 0.9f).toArgb()
                        } else {
                            onSurfaceVariantColor.copy(alpha = 0.7f).toArgb()
                        }
                        textSize = valueTextSizePx
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(
                            android.graphics.Typeface.DEFAULT,
                            android.graphics.Typeface.BOLD
                        )
                    }
                    drawText(
                        data[i].count.toString(),
                        x + barWidth / 2,
                        valueY,
                        paint
                    )
                }
            }

            // Day label below bar
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = if (isToday) {
                        barColor.toArgb()
                    } else {
                        onSurfaceVariantColor.copy(alpha = 0.6f).toArgb()
                    }
                    textSize = labelTextSizePx
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = if (isToday) {
                        android.graphics.Typeface.create(
                            android.graphics.Typeface.DEFAULT,
                            android.graphics.Typeface.BOLD
                        )
                    } else {
                        android.graphics.Typeface.DEFAULT
                    }
                }
                drawText(
                    DayLabelsJapanese[i],
                    x + barWidth / 2,
                    canvasHeight - 4f,
                    paint
                )
            }
        }

        // Draw baseline
        drawLine(
            color = outlineVariantColor.copy(alpha = 0.5f),
            start = Offset(sidePadding, topPadding + chartHeight),
            end = Offset(canvasWidth - sidePadding, topPadding + chartHeight),
            strokeWidth = 1.5f
        )
    }
}

// ============================================================
// 2. MonthlyHeatmap
// ============================================================

// Green palette for heatmap (GitHub-like contribution colors)
private val HeatmapEmptyLight = Color(0xFFEBEDF0)
private val HeatmapEmptyDark = Color(0xFF2D333B)
private val HeatmapGreen1 = Color(0xFF9BE9A8)
private val HeatmapGreen2 = Color(0xFF40C463)
private val HeatmapGreen3 = Color(0xFF30A14E)
private val HeatmapGreen4 = Color(0xFF216E39)

/**
 * A calendar-style heatmap showing daily study activity for the current month.
 *
 * Features:
 * - 7 columns (days of week) x 5 rows (weeks) grid layout
 * - Color intensity reflects study count (empty -> light green -> dark green)
 * - GitHub contribution graph style palette
 * - Month/year header text
 * - Day-of-week column headers in Japanese
 * - Rounded corner cells with subtle spacing
 * - Supports both light and dark themes
 *
 * @param data List of [DailyStudyData] for the month (indexed by day of month)
 * @param modifier Modifier for the composable
 */
@Composable
fun MonthlyHeatmap(
    data: List<DailyStudyData>,
    modifier: Modifier = Modifier
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val emptyColor = if (isDarkTheme) HeatmapEmptyDark else HeatmapEmptyLight

    val now = remember { LocalDate.now() }
    val monthLabel = remember(now) {
        val monthNames = listOf(
            "1月", "2月", "3月", "4月", "5月", "6月",
            "7月", "8月", "9月", "10月", "11月", "12月"
        )
        "${now.year}年 ${monthNames[now.monthValue - 1]}"
    }

    // Calculate grid: which weekday the 1st falls on, total days in month
    val firstDayOfMonth = remember(now) { now.withDayOfMonth(1) }
    val daysInMonth = remember(now) { now.lengthOfMonth() }
    // DayOfWeek.MONDAY = 1 ... SUNDAY = 7, convert to 0-indexed (Mon=0)
    val startOffset = remember(firstDayOfMonth) { firstDayOfMonth.dayOfWeek.value - 1 }

    val maxCount = remember(data) { data.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1 }

    // Animation
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    val density = LocalDensity.current
    val headerTextSizePx = with(density) { 14.sp.toPx() }
    val dayHeaderTextSizePx = with(density) { 10.sp.toPx() }

    Column(modifier = modifier.fillMaxWidth()) {
        // Month/Year header
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = onSurfaceColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val headerHeight = 24f
            val sidePadding = 4f
            val cellSpacing = 4f

            val gridWidth = canvasWidth - sidePadding * 2
            val cols = 7
            val rows = 5
            val cellSize = ((gridWidth - (cols - 1) * cellSpacing) / cols)
                .coerceAtMost((canvasHeight - headerHeight - (rows - 1) * cellSpacing) / rows)
            val cornerRadiusPx = cellSize * 0.2f

            // Center the grid horizontally
            val totalGridWidth = cols * cellSize + (cols - 1) * cellSpacing
            val offsetX = (canvasWidth - totalGridWidth) / 2f

            // Draw day-of-week headers
            val dayHeaders = listOf("月", "火", "水", "木", "金", "土", "日")
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = onSurfaceVariantColor.copy(alpha = 0.5f).toArgb()
                    textSize = dayHeaderTextSizePx
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                for (col in 0 until cols) {
                    val cx = offsetX + col * (cellSize + cellSpacing) + cellSize / 2
                    drawText(dayHeaders[col], cx, headerHeight - 6f, paint)
                }
            }

            // Draw heatmap cells
            var dayOfMonth = 1
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val cellIndex = row * cols + col
                    val x = offsetX + col * (cellSize + cellSpacing)
                    val y = headerHeight + row * (cellSize + cellSpacing)

                    if (cellIndex < startOffset || dayOfMonth > daysInMonth) {
                        // Empty cell outside the month
                        drawRoundRect(
                            color = emptyColor.copy(alpha = 0.3f),
                            topLeft = Offset(x, y),
                            size = Size(cellSize, cellSize),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )
                    } else {
                        // Find study data for this day
                        val dayData = data.getOrNull(dayOfMonth - 1)
                        val count = dayData?.count ?: 0
                        val intensity = if (count == 0) 0f else {
                            (count.toFloat() / maxCount).coerceIn(0.1f, 1f)
                        }

                        val cellColor = if (count == 0) {
                            emptyColor
                        } else {
                            getHeatmapColor(intensity, animationProgress.value)
                        }

                        drawRoundRect(
                            color = cellColor,
                            topLeft = Offset(x, y),
                            size = Size(cellSize, cellSize),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )

                        dayOfMonth++
                    }
                }
            }
        }

        // Legend row
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "少",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariantColor.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            val legendColors = listOf(emptyColor, HeatmapGreen1, HeatmapGreen2, HeatmapGreen3, HeatmapGreen4)
            legendColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "多",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariantColor.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Maps an intensity value (0..1) to a green heatmap color.
 * Uses animation progress to fade in the color.
 */
private fun getHeatmapColor(intensity: Float, animProgress: Float): Color {
    val adjustedIntensity = intensity * animProgress
    return when {
        adjustedIntensity <= 0f -> HeatmapEmptyLight
        adjustedIntensity < 0.25f -> HeatmapGreen1
        adjustedIntensity < 0.50f -> HeatmapGreen2
        adjustedIntensity < 0.75f -> HeatmapGreen3
        else -> HeatmapGreen4
    }
}

/**
 * Extension to compute relative luminance from a Color for theme detection.
 */
private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}

// ============================================================
// 3. MasteryDonutChart
// ============================================================

// Colors for mastery levels 0-5 (red -> orange -> yellow -> light green -> green -> deep green)
private val MasteryColors = listOf(
    Color(0xFFE53935), // Level 0 - Red (unseen / new)
    MasteryLevel1,     // Level 1 - Red-orange
    MasteryLevel2,     // Level 2 - Orange
    MasteryLevel3,     // Level 3 - Yellow
    MasteryLevel4,     // Level 4 - Light green
    MasteryLevel5      // Level 5 - Green (mastered)
)

/**
 * A donut (ring) chart displaying word distribution across mastery levels.
 *
 * Features:
 * - Ring-shaped chart (not filled pie)
 * - 6 segments for mastery levels 0-5
 * - Colors range from red (level 0) through yellow to green (level 5)
 * - Center text showing total word count and label
 * - Legend below the chart with color swatches and labels
 * - Animated arc drawing on composition
 * - Supports both light and dark themes
 *
 * @param distribution List of [MasteryLevel] entries for levels 0-5
 * @param modifier Modifier for the composable
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MasteryDonutChart(
    distribution: List<MasteryLevel>,
    modifier: Modifier = Modifier
) {
    val totalWords = remember(distribution) { distribution.sumOf { it.count } }
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Animation for sweeping arcs
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(distribution) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    val density = LocalDensity.current
    val centerLargeTextSizePx = with(density) { 22.sp.toPx() }
    val centerSmallTextSizePx = with(density) { 11.sp.toPx() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Donut chart
        Canvas(
            modifier = Modifier.size(160.dp)
        ) {
            val canvasSize = size.minDimension
            val strokeWidth = canvasSize * 0.16f
            val radius = (canvasSize - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Draw background ring
            drawCircle(
                color = surfaceColor.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )

            if (totalWords == 0) {
                // Empty state: draw a full grey ring
                drawCircle(
                    color = onSurfaceVariantColor.copy(alpha = 0.1f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
            } else {
                // Draw arcs for each mastery level
                var startAngle = -90f // Start from top
                val gapAngle = 2f // Small gap between segments

                for (entry in distribution) {
                    if (entry.count == 0) continue

                    val sweepAngle = (entry.count.toFloat() / totalWords * 360f - gapAngle)
                        .coerceAtLeast(0f) * animationProgress.value
                    val colorIndex = entry.level.coerceIn(0, MasteryColors.lastIndex)

                    drawArc(
                        color = MasteryColors[colorIndex],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(
                            center.x - radius,
                            center.y - radius
                        ),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )

                    startAngle += sweepAngle + gapAngle
                }
            }

            // Center text: total count
            drawContext.canvas.nativeCanvas.apply {
                val countPaint = android.graphics.Paint().apply {
                    color = onSurfaceColor.toArgb()
                    textSize = centerLargeTextSizePx
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD
                    )
                }
                drawText(
                    totalWords.toString(),
                    center.x,
                    center.y + centerLargeTextSizePx * 0.1f,
                    countPaint
                )

                val labelPaint = android.graphics.Paint().apply {
                    color = onSurfaceVariantColor.copy(alpha = 0.6f).toArgb()
                    textSize = centerSmallTextSizePx
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText(
                    "語",
                    center.x,
                    center.y + centerLargeTextSizePx * 0.1f + centerSmallTextSizePx * 1.5f,
                    labelPaint
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            distribution.forEach { entry ->
                val colorIndex = entry.level.coerceIn(0, MasteryColors.lastIndex)
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MasteryColors[colorIndex], CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${entry.label} (${entry.count})",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariantColor
                    )
                }
            }
        }
    }
}

// ============================================================
// 4. StreakCard
// ============================================================

/**
 * A prominent card displaying the current and maximum study streak.
 *
 * Features:
 * - Fire icon (Whatshot) for visual emphasis
 * - Large display of current streak days
 * - Secondary display of maximum streak record
 * - Warm orange gradient background
 * - Adapts to light and dark themes
 * - Japanese labels
 *
 * @param currentStreak Number of consecutive study days in the current streak
 * @param maxStreak Maximum streak achieved so far
 * @param modifier Modifier for the composable
 */
@Composable
fun StreakCard(
    currentStreak: Int,
    maxStreak: Int,
    modifier: Modifier = Modifier
) {
    val isActive = currentStreak > 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (isActive) {
                        Brush.linearGradient(
                            colors = listOf(
                                StreakOrange.copy(alpha = 0.18f),
                                StreakOrange.copy(alpha = 0.06f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fire icon with background circle
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = if (isActive) {
                        StreakOrange.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Whatshot,
                            contentDescription = "Streak",
                            tint = if (isActive) StreakOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Current streak
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "現在の連続記録",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isActive) {
                            StreakOrange.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = currentStreak.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) {
                                StreakOrange
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "日",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isActive) {
                                StreakOrange.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                // Max streak (right side)
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "最高記録",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = maxStreak.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "日",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// Private helpers
// ============================================================

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}

// ============================================================
// Previews
// ============================================================

@Preview(showBackground = true, name = "WeeklyBarChart - Light")
@Composable
private fun WeeklyBarChartPreview() {
    EnglishWordTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            WeeklyBarChart(
                data = listOf(
                    DailyStudyData("月", 12),
                    DailyStudyData("火", 25),
                    DailyStudyData("水", 8),
                    DailyStudyData("木", 30),
                    DailyStudyData("金", 0),
                    DailyStudyData("土", 18),
                    DailyStudyData("日", 22)
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, name = "WeeklyBarChart - Dark")
@Composable
private fun WeeklyBarChartDarkPreview() {
    EnglishWordTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            WeeklyBarChart(
                data = listOf(
                    DailyStudyData("月", 12),
                    DailyStudyData("火", 25),
                    DailyStudyData("水", 8),
                    DailyStudyData("木", 30),
                    DailyStudyData("金", 0),
                    DailyStudyData("土", 18),
                    DailyStudyData("日", 22)
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "MonthlyHeatmap - Light")
@Composable
private fun MonthlyHeatmapPreview() {
    EnglishWordTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            val sampleData = (1..28).map { day ->
                DailyStudyData(
                    date = "2026-02-%02d".format(day),
                    count = listOf(0, 0, 5, 12, 3, 20, 8, 0, 15, 25, 2, 0, 18, 7,
                        0, 30, 10, 0, 22, 14, 6, 0, 0, 28, 11, 9, 16, 4)[day - 1]
                )
            }
            MonthlyHeatmap(
                data = sampleData,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, name = "MonthlyHeatmap - Dark")
@Composable
private fun MonthlyHeatmapDarkPreview() {
    EnglishWordTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            val sampleData = (1..28).map { day ->
                DailyStudyData(
                    date = "2026-02-%02d".format(day),
                    count = listOf(0, 0, 5, 12, 3, 20, 8, 0, 15, 25, 2, 0, 18, 7,
                        0, 30, 10, 0, 22, 14, 6, 0, 0, 28, 11, 9, 16, 4)[day - 1]
                )
            }
            MonthlyHeatmap(
                data = sampleData,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "MasteryDonutChart - Light")
@Composable
private fun MasteryDonutChartPreview() {
    EnglishWordTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MasteryDonutChart(
                distribution = listOf(
                    MasteryLevel(0, 45, "未学習"),
                    MasteryLevel(1, 30, "学習開始"),
                    MasteryLevel(2, 25, "学習中"),
                    MasteryLevel(3, 40, "定着中"),
                    MasteryLevel(4, 35, "ほぼ習得"),
                    MasteryLevel(5, 75, "習得済み")
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, name = "MasteryDonutChart - Dark")
@Composable
private fun MasteryDonutChartDarkPreview() {
    EnglishWordTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MasteryDonutChart(
                distribution = listOf(
                    MasteryLevel(0, 45, "未学習"),
                    MasteryLevel(1, 30, "学習開始"),
                    MasteryLevel(2, 25, "学習中"),
                    MasteryLevel(3, 40, "定着中"),
                    MasteryLevel(4, 35, "ほぼ習得"),
                    MasteryLevel(5, 75, "習得済み")
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "StreakCard - Active (Light)")
@Composable
private fun StreakCardActivePreview() {
    EnglishWordTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StreakCard(
                currentStreak = 14,
                maxStreak = 30,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, name = "StreakCard - Active (Dark)")
@Composable
private fun StreakCardActiveDarkPreview() {
    EnglishWordTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StreakCard(
                currentStreak = 14,
                maxStreak = 30,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "StreakCard - Inactive")
@Composable
private fun StreakCardInactivePreview() {
    EnglishWordTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StreakCard(
                currentStreak = 0,
                maxStreak = 12,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "All Charts Combined")
@Composable
private fun AllChartsPreview() {
    EnglishWordTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                StreakCard(currentStreak = 7, maxStreak = 21)

                WeeklyBarChart(
                    data = listOf(
                        DailyStudyData("月", 12),
                        DailyStudyData("火", 25),
                        DailyStudyData("水", 8),
                        DailyStudyData("木", 30),
                        DailyStudyData("金", 0),
                        DailyStudyData("土", 18),
                        DailyStudyData("日", 22)
                    )
                )

                MasteryDonutChart(
                    distribution = listOf(
                        MasteryLevel(0, 45, "未学習"),
                        MasteryLevel(1, 30, "学習開始"),
                        MasteryLevel(2, 25, "学習中"),
                        MasteryLevel(3, 40, "定着中"),
                        MasteryLevel(4, 35, "ほぼ習得"),
                        MasteryLevel(5, 75, "習得済み")
                    )
                )
            }
        }
    }
}
