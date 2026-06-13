package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Extended chart components for the FieldMind Research Dashboard.
 * All charts are Canvas-based, fully offline, dependency-free.
 */

// ══════════════════════════════════════════════════════════════════════
//  1. CALENDAR HEATMAP — GitHub-style contribution grid
// ══════════════════════════════════════════════════════════════════════

/**
 * GitHub-style contribution heatmap showing daily observation counts.
 * Each cell = 1 day, columns = weeks (left-to-right), rows = days (top-to-bottom, Mon-Sun).
 * Uses java.time (available via desugaring on API 26+).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarHeatmap(
    dailyCounts: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
    monthsToShow: Int = 12,
    accentColor: Color = FieldMindTheme.colors.observation,
    onTapDay: ((LocalDate, Int) -> Unit)? = null
) {
    val today = LocalDate.now()
    val startDate = today.minusMonths(monthsToShow.toLong())
    val dayCount = ChronoUnit.DAYS.between(startDate, today).toInt().coerceAtLeast(1)
    val maxCount = (dailyCounts.values.maxOrNull() ?: 1).coerceAtLeast(1)
    // Build color scale: 5 shades from surface to accent
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val colors = (0..4).map { i ->
        val fraction = (i + 1) / 5f
        accentColor.copy(alpha = 0.15f + fraction * 0.85f)
    }

    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Month labels row
        val monthLabels = remember(startDate, today) {
            val months = mutableListOf<Pair<Int, String>>() // (weekIndex, label)
            var current = startDate
            while (current <= today) {
                if (current.dayOfMonth <= 7) {
                    val weekOfYear = (ChronoUnit.DAYS.between(startDate, current).toInt() + startDate.dayOfWeek.value - 1) / 7
                    months.add(weekOfYear to current.month.name.take(3))
                }
                current = current.plusDays(1)
            }
            months.distinctBy { it.first }
        }

        Row(Modifier.fillMaxWidth()) {
            // Day labels column (Mon, Wed, Fri)
            Column(
                modifier = Modifier.width(24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf("Mon", "", "Wed", "", "Fri", "").forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp,
                        modifier = Modifier.height(12.dp)
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                // Month labels
                Row(Modifier.fillMaxWidth()) {
                    monthLabels.forEach { (weekIdx, label) ->
                        Spacer(Modifier.width((weekIdx * 14).dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.sp
                        )
                    }
                }

                // Heatmap grid
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .pointerInput(dailyCounts) {
                            detectTapGestures { offset ->
                                val cellSize = size.width / 53f
                                val col = (offset.x / cellSize).toInt().coerceIn(0, 52)
                                val row = (offset.y / (size.height / 7f)).toInt().coerceIn(0, 6)
                                val dayIndex = col * 7 + row
                                if (dayIndex < dayCount) {
                                    val date = startDate.plusDays(dayIndex.toLong())
                                val count = dailyCounts[date] ?: 0
                                selectedDay = date
                                onTapDay?.invoke(date, count)
                                }
                            }
                        }
                ) {
                    val cellSize = this.size.width / 53f
                    val cellPad = 1.5f
                    val cellInner = (cellSize - cellPad * 2).coerceAtLeast(3f)

                    (0 until dayCount).forEach { dayIndex ->
                        val date = startDate.plusDays(dayIndex.toLong())
                        val dow = (date.dayOfWeek.value - 1) // 0=Mon, 1=Tue...
                        val weekOffset = (ChronoUnit.DAYS.between(startDate, date).toInt() + startDate.dayOfWeek.value - 1) / 7

                        val x = weekOffset * cellSize + cellPad
                        val y = dow * (this.size.height / 7f) + cellPad
                        val count = dailyCounts[date] ?: 0
                        val color = when {
                            count == 0 -> emptyColor
                            else -> colors[(count.toFloat() / maxCount * 4).toInt().coerceIn(0, 4)]
                        }
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(cellInner, cellInner),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                        // Highlight selected day
                        if (date == selectedDay) {
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.5f),
                                topLeft = Offset(x - 1f, y - 1f),
                                size = Size(cellInner + 2f, cellInner + 2f),
                                cornerRadius = CornerRadius(3f, 3f),
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }
            }
        }

        // Selected day info
        selectedDay?.let { date ->
            val count = dailyCounts[date] ?: 0
            Text(
                "${date.month.name} ${date.dayOfMonth}, ${date.year}: $count observation${if (count != 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Legend
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
            Box(Modifier.size(10.dp).background(emptyColor, RoundedCornerShape(2.dp)))
            colors.take(4).forEach { c ->
                Box(Modifier.size(10.dp).background(c, RoundedCornerShape(2.dp)))
            }
            Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  2. RADAR / SPIDER CHART — Multi-dimensional comparison
// ══════════════════════════════════════════════════════════════════════

/**
 * Radar (spider) chart for comparing multiple categories across dimensions.
 * Each axis represents a category; the value is plotted as distance from center.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RadarChart(
    categories: List<Pair<String, Float>>, // (label, value 0f..1f)
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant,
    showLabels: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 200.dp
) {
    if (categories.size < 3) {
        Text("Need at least 3 categories for a radar chart", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val n = categories.size
    val angleStep = (2.0 * Math.PI / n).toFloat()
    var selectedAxis by remember { mutableStateOf(-1) }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(height).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .pointerInput(categories) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val radius = minOf(cx, cy) * 0.75f
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                            if (dist <= radius + 20f) {
                                val angle = (kotlin.math.atan2(dy.toDouble(), dx.toDouble()) + Math.PI / 2).toFloat()
                                val normalized = (angle + 2 * Math.PI.toFloat()) % (2 * Math.PI.toFloat())
                                val idx = (normalized / angleStep).toInt().coerceIn(0, n - 1)
                                selectedAxis = if (selectedAxis == idx) -1 else idx
                            }
                        }
                    }
            ) {
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val radius = minOf(cx, cy) * 0.75f

                // Draw concentric grid polygons (25%, 50%, 75%, 100%)
                val gridLevels = listOf(0.25f, 0.50f, 0.75f, 1.0f)
                gridLevels.forEach { level ->
                    val gridPath = Path()
                    (0 until n).forEach { i ->
                        val angle = i * angleStep - Math.PI.toFloat() / 2f
                        val x = cx + radius * level * kotlin.math.cos(angle)
                        val y = cy + radius * level * kotlin.math.sin(angle)
                        if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
                    }
                    gridPath.close()
                    drawPath(gridPath, gridColor.copy(alpha = 0.3f), style = Stroke(width = 1f))
                }

                // Draw axis lines
                (0 until n).forEach { i ->
                    val angle = i * angleStep - Math.PI.toFloat() / 2f
                    val x = cx + radius * kotlin.math.cos(angle)
                    val y = cy + radius * kotlin.math.sin(angle)
                    drawLine(
                        color = if (i == selectedAxis) accentColor else gridColor,
                        start = Offset(cx, cy),
                        end = Offset(x, y),
                        strokeWidth = if (i == selectedAxis) 2f else 1f
                    )
                }

                // Draw data polygon
                val dataPath = Path()
                categories.forEachIndexed { i, (_, value) ->
                    val angle = i * angleStep - Math.PI.toFloat() / 2f
                    val v = value.coerceIn(0f, 1f)
                    val x = cx + radius * v * kotlin.math.cos(angle)
                    val y = cy + radius * v * kotlin.math.sin(angle)
                    if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                }
                dataPath.close()
                drawPath(dataPath, accentColor.copy(alpha = 0.15f))
                drawPath(dataPath, accentColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Draw data points
                categories.forEachIndexed { i, (_, value) ->
                    val angle = i * angleStep - Math.PI.toFloat() / 2f
                    val v = value.coerceIn(0f, 1f)
                    val x = cx + radius * v * kotlin.math.cos(angle)
                    val y = cy + radius * v * kotlin.math.sin(angle)
                    val isSel = i == selectedAxis
                    drawCircle(Color.White, radius = if (isSel) 6f else 4f, center = Offset(x, y))
                    drawCircle(accentColor, radius = if (isSel) 4f else 2.5f, center = Offset(x, y))
                }

                // Draw labels at vertices
                if (showLabels) {
                    categories.forEachIndexed { i, (label, _) ->
                        val angle = i * angleStep - Math.PI.toFloat() / 2f
                        val labelRadius = radius + 24f
                        val x = cx + labelRadius * kotlin.math.cos(angle)
                        val y = cy + labelRadius * kotlin.math.sin(angle)
                        // Draw label using drawContext.canvas.nativeCanvas with Paint
                        // For simplicity, we use a composable-based approach instead
                    }
                }
            }
        }

        // Text labels below the chart
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEachIndexed { i, (label, value) ->
                val isSelected = i == selectedAxis
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .clickable { selectedAxis = if (selectedAxis == i) -1 else i }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(Modifier.size(8.dp).background(accentColor.copy(alpha = 0.4f + value * 0.6f), CircleShape))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  3. TAG CO-OCCURRENCE MATRIX
// ══════════════════════════════════════════════════════════════════════

/**
 * Tag co-occurrence heatmap showing which tags appear together most often.
 * Each cell represents the co-occurrence count of two tags.
 */
@Composable
fun TagCoOccurrenceMatrix(
    tagPairs: List<Pair<String, String>>, // List of tag pairs that co-occur
    modifier: Modifier = Modifier,
    accentColor: Color = FieldMindTheme.colors.info
) {
    if (tagPairs.isEmpty()) {
        Text("Add tags to observations to see co-occurrence patterns.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    // Count co-occurrences
    val coOccurrenceMap = remember(tagPairs) {
        val map = mutableMapOf<Set<String>, Int>()
        tagPairs.forEach { (a, b) ->
            val key = setOf(a.lowercase(), b.lowercase())
            if (key.size == 2) map[key] = (map[key] ?: 0) + 1
        }
        map
    }

    // Get top tags by co-occurrence frequency
    val topTags = remember(coOccurrenceMap) {
        coOccurrenceMap.entries
            .flatMap { (tags, _) -> tags.toList() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(8)
            .map { it.key }
    }

    if (topTags.size < 2) {
        Text("Need at least 2 tags that appear together.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    val maxCount = coOccurrenceMap.values.maxOrNull() ?: 1
    val n = topTags.size
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Selected cell info
        selectedCell?.let { (row, col) ->
            val tag1 = topTags[row]
            val tag2 = topTags[col]
            val count = coOccurrenceMap[setOf(tag1, tag2)] ?: 0
            Text(
                "\"$tag1\" + \"$tag2\": $count co-occurrence${if (count != 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }

        // Matrix grid
        Canvas(
            Modifier
                .fillMaxWidth()
                .height((n * 32 + 24).dp)
                .pointerInput(topTags) {
                    detectTapGestures { offset ->
                        val cellSize = size.width / (n + 1)
                        val row = ((offset.y - cellSize) / cellSize).toInt()
                        val col = ((offset.x - cellSize) / cellSize).toInt()
                        if (row in 0 until n && col in 0 until n && row != col) {
                            selectedCell = if (selectedCell == Pair(row, col)) null else Pair(row, col)
                        }
                    }
                }
        ) {
            val cellSize = this.size.width / (n + 1)
            val cellPad = 2f
            val cellInner = cellSize - cellPad * 2

            // Row/column label offset
            (0 until n).forEach { i ->
                val x = (i + 1) * cellSize + cellSize / 2
                val y = cellSize / 2
                // Labels are drawn via the text row/column below
            }

            // Draw matrix cells
            (0 until n).forEach { row ->
                (0 until n).forEach { col ->
                    if (row != col) {
                        val tag1 = topTags[row]
                        val tag2 = topTags[col]
                        val count = coOccurrenceMap[setOf(tag1, tag2)] ?: 0
                        val intensity = (count.toFloat() / maxCount).coerceIn(0f, 1f)

                        val left = (col + 1) * cellSize + cellPad
                        val top = (row + 1) * cellSize + cellPad
                        val isSelected = selectedCell == Pair(row, col) || selectedCell == Pair(col, row)

                        drawRoundRect(
                            color = accentColor.copy(alpha = 0.08f + intensity * 0.7f),
                            topLeft = Offset(left, top),
                            size = Size(cellInner, cellInner),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        if (isSelected) {
                            drawRoundRect(
                                color = accentColor,
                                topLeft = Offset(left - 1f, top - 1f),
                                size = Size(cellInner + 2f, cellInner + 2f),
                                cornerRadius = CornerRadius(5f, 5f),
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }
            }
        }

        // Tag labels row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(Modifier.width(24.dp)) // offset for row labels
            topTags.forEach { tag ->
                Text(
                    tag,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontSize = 8.sp
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  4. ACTIVITY BY HOUR — 24-hour bar chart
// ══════════════════════════════════════════════════════════════════════

/**
 * 24-hour activity distribution bar chart showing which hours of the day
 * the user makes the most observations.
 */
@Composable
fun ActivityByHourChart(
    hourlyCounts: Map<Int, Int>, // 0-23 -> count
    modifier: Modifier = Modifier,
    accentColor: Color = FieldMindTheme.colors.warning,
    height: androidx.compose.ui.unit.Dp = 120.dp
) {
    val maxCount = (hourlyCounts.values.maxOrNull() ?: 1).coerceAtLeast(1)
    var selectedHour by remember { mutableStateOf(-1) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Selected hour info
        if (selectedHour >= 0) {
            val count = hourlyCounts[selectedHour] ?: 0
            val amPm = if (selectedHour < 12) "AM" else "PM"
            val hour12 = if (selectedHour == 0) 12 else if (selectedHour > 12) selectedHour - 12 else selectedHour
            Text(
                "$hour12:00 $amPm: $count observation${if (count != 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(hourlyCounts) {
                    detectTapGestures { offset ->
                        val barWidth = size.width / 24f
                        val hour = (offset.x / barWidth).toInt().coerceIn(0, 23)
                        selectedHour = if (selectedHour == hour) -1 else hour
                    }
                }
        ) {
            val barWidth = this.size.width / 24f
            val gap = 1.5f

            (0..23).forEach { hour ->
                val count = hourlyCounts[hour] ?: 0
                val height = if (maxCount > 0) (count.toFloat() / maxCount) * this.size.height else 0f
                val x = hour * barWidth + gap
                val isSelected = hour == selectedHour

                drawRoundRect(
                    color = if (isSelected) accentColor else accentColor.copy(alpha = 0.35f + (height / this.size.height) * 0.5f),
                    topLeft = Offset(x, this.size.height - height),
                    size = Size(barWidth - gap * 2, height.coerceAtLeast(1f)),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                if (isSelected) {
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(x - 1f, this.size.height - height - 1f),
                        size = Size(barWidth - gap * 2 + 2f, height.coerceAtLeast(1f) + 2f),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(width = 2f)
                    )
                }
            }

            // Draw midnight/noon markers
            drawLine(gridColor, start = Offset(0f, this.size.height / 2f), end = Offset(this.size.width, this.size.height / 2f), strokeWidth = 0.5f)
        }

        // Hour labels (every 3 hours)
        Row(Modifier.fillMaxWidth()) {
            (0..23 step 3).forEach { hour ->
                val amPm = if (hour < 12) "AM" else "PM"
                val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                Text(
                    "$hour12$amPm",
                    modifier = Modifier.weight(if (hour == 0) 1f else 1f),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 7.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
                if (hour < 21) Spacer(Modifier.weight(2f))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  5. DAY-OF-WEEK CHART — 7-bar chart
// ══════════════════════════════════════════════════════════════════════

private val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/**
 * Day-of-week breakdown chart showing which days are most productive.
 */
@Composable
fun DayOfWeekChart(
    dayCounts: Map<Int, Int>, // 1=Mon..7=Sun -> count
    modifier: Modifier = Modifier,
    accentColor: Color = FieldMindTheme.colors.info,
    height: androidx.compose.ui.unit.Dp = 100.dp
) {
    val maxCount = (dayCounts.values.maxOrNull() ?: 1).coerceAtLeast(1)
    var selectedDay by remember { mutableStateOf(-1) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (selectedDay >= 0) {
            val count = dayCounts[selectedDay + 1] ?: 0
            Text(
                "${dayNames[selectedDay]}: $count observation${if (count != 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(dayCounts) {
                    detectTapGestures { offset ->
                        val barWidth = size.width / 7f
                        val idx = (offset.x / barWidth).toInt().coerceIn(0, 6)
                        selectedDay = if (selectedDay == idx) -1 else idx
                    }
                }
        ) {
            val barWidth = this.size.width / 7f
            val gap = 4f

            (0..6).forEach { i ->
                val count = dayCounts[i + 1] ?: 0
                val h = if (maxCount > 0) (count.toFloat() / maxCount) * this.size.height else 0f
                val x = i * barWidth + gap
                val isSelected = i == selectedDay

                drawRoundRect(
                    color = if (isSelected) accentColor else accentColor.copy(alpha = 0.3f + (h / this.size.height) * 0.6f),
                    topLeft = Offset(x, this.size.height - h),
                    size = Size(barWidth - gap * 2, h.coerceAtLeast(1f)),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                if (isSelected) {
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(x - 1f, this.size.height - h - 1f),
                        size = Size(barWidth - gap * 2 + 2f, h.coerceAtLeast(1f) + 2f),
                        cornerRadius = CornerRadius(7f, 7f),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        // Day labels
        Row(Modifier.fillMaxWidth()) {
            (0..6).forEach { i ->
                Text(
                    dayNames[i],
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (i == selectedDay) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (i == selectedDay) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  6. MOVING AVERAGE OVERLAY — LineChart with 7-day rolling avg
// ══════════════════════════════════════════════════════════════════════

/**
 * Line chart with a moving average overlay.
 * [dailyValues] = map of date string (yyyy-MM-dd) to count.
 * Shows daily bars + 7-day rolling average line.
 */
@Composable
fun MovingAverageChart(
    dailyValues: Map<String, Int>,
    modifier: Modifier = Modifier,
    barColor: Color = FieldMindTheme.colors.observation.copy(alpha = 0.4f),
    lineColor: Color = FieldMindTheme.colors.positive,
    height: androidx.compose.ui.unit.Dp = 130.dp,
    windowSize: Int = 7
) {
    if (dailyValues.size < 2) {
        Text("Need at least 2 days of data",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    val sortedDates = dailyValues.keys.sorted()
    val values = sortedDates.map { dailyValues[it] ?: 0 }
    val maxVal = (values.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()

    // Calculate moving average
    val movingAvg = remember(values, windowSize) {
        values.mapIndexed { index, _ ->
            val start = (index - windowSize + 1).coerceAtLeast(0)
            val window = values.subList(start, index + 1)
            window.average().toFloat()
        }
    }

    val n = values.size
    var selectedIndex by remember { mutableStateOf(-1) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (selectedIndex >= 0) {
            Text(
                "${sortedDates[selectedIndex]}: ${values[selectedIndex]} obs (avg: ${"%.1f".format(movingAvg[selectedIndex])})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = lineColor
            )
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(dailyValues) {
                    detectTapGestures { offset ->
                        val barWidth = size.width / n
                        val idx = (offset.x / barWidth).toInt().coerceIn(0, n - 1)
                        selectedIndex = if (selectedIndex == idx) -1 else idx
                    }
                }
        ) {
            val barWidth = this.size.width / n
            val gap = 2f

            // Draw daily bars
            values.forEachIndexed { i, v ->
                val h = (v.toFloat() / maxVal) * this.size.height
                val x = i * barWidth + gap
                val isSelected = i == selectedIndex
                drawRoundRect(
                    color = if (isSelected) barColor.copy(alpha = 0.8f) else barColor,
                    topLeft = Offset(x, this.size.height - h),
                    size = Size(barWidth - gap * 2, h.coerceAtLeast(1f)),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }

            // Draw moving average line
            if (movingAvg.size >= 2) {
                val linePath = Path()
                movingAvg.forEachIndexed { i, avg ->
                    val x = i * barWidth + barWidth / 2f
                    val y = this.size.height - (avg / maxVal) * this.size.height
                    if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                }
                drawPath(linePath, lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Draw dots on moving average
                movingAvg.forEachIndexed { i, avg ->
                    val x = i * barWidth + barWidth / 2f
                    val y = this.size.height - (avg / maxVal) * this.size.height
                    val isSelected = i == selectedIndex
                    drawCircle(lineColor.copy(alpha = 0.5f), radius = if (isSelected) 6f else 3f, center = Offset(x, y))
                    drawCircle(if (isSelected) lineColor else Color.White, radius = if (isSelected) 3f else 1.5f, center = Offset(x, y))
                }
            }
        }

        // Date range label
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(sortedDates.firstOrNull()?.takeLast(5) ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
            Text("${windowSize}-day moving avg", style = MaterialTheme.typography.labelSmall, color = lineColor, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
            Text(sortedDates.lastOrNull()?.takeLast(5) ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  7. WEATHER CORRELATION — Scatter plot + trend line
// ══════════════════════════════════════════════════════════════════════

/**
 * Scatter plot showing correlation between weather metric (e.g., temperature)
 * and observation counts. With optional trend line.
 */
@Composable
fun WeatherCorrelationChart(
    dataPoints: List<Pair<Float, Float>>, // (weatherValue, observationCount)
    modifier: Modifier = Modifier,
    xLabel: String = "Temperature (°C)",
    yLabel: String = "Observations",
    pointColor: Color = FieldMindTheme.colors.observation,
    trendColor: Color = FieldMindTheme.colors.positive,
    height: androidx.compose.ui.unit.Dp = 160.dp
) {
    if (dataPoints.size < 3) {
        Text("Need at least 3 data points for correlation (enable weather + GPS on capture).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    val xValues = dataPoints.map { it.first }
    val yValues = dataPoints.map { it.second }
    val xMin = xValues.min(); val xMax = xValues.max()
    val yMin = yValues.min(); val yMax = yValues.max()
    val xRange = (xMax - xMin).coerceAtLeast(1f)
    val yRange = (yMax - yMin).coerceAtLeast(1f)

    // Linear regression for trend line
    val trendLine = remember(dataPoints) {
        val n = dataPoints.size
        val sumX = xValues.sum()
        val sumY = yValues.sum()
        val sumXY = dataPoints.sumOf { it.first.toDouble() * it.second.toDouble() }.toFloat()
        val sumX2 = xValues.sumOf { (it.toDouble() * it.toDouble()) }.toFloat()
        val slope = if (n * sumX2 - sumX * sumX != 0f) (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX) else 0f
        val intercept = (sumY - slope * sumX) / n
        slope to intercept
    }

    var selectedPoint by remember { mutableStateOf(-1) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Trend info
        Text(
            "Trend: ${if (trendLine.first > 0) "↑ Positive" else if (trendLine.first < 0) "↓ Negative" else "→ Flat"} correlation (slope: ${"%.2f".format(trendLine.first)})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = trendColor
        )

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(dataPoints) {
                    detectTapGestures { offset ->
                        val padX = size.width * 0.08f
                        val padY = size.height * 0.08f
                        val plotW = size.width - 2 * padX
                        val plotH = size.height - 2 * padY
                        var closest = -1
                        var closestDist = Float.MAX_VALUE
                        dataPoints.forEachIndexed { i, (x, y) ->
                            val px = padX + ((x - xMin) / xRange) * plotW
                            val py = padY + (1f - ((y - yMin) / yRange)) * plotH
                            val dist = kotlin.math.sqrt((offset.x - px).let { it * it } + (offset.y - py).let { it * it })
                            if (dist < closestDist && dist < 30f) { closestDist = dist; closest = i }
                        }
                        selectedPoint = if (selectedPoint == closest) -1 else closest
                    }
                }
        ) {
            val padX = this.size.width * 0.08f
            val padY = this.size.height * 0.08f
            val plotW = this.size.width - 2 * padX
            val plotH = this.size.height - 2 * padY

            // Draw grid lines
            (0..4).forEach { i ->
                val y = padY + (i / 4f) * plotH
                drawLine(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    start = Offset(padX, y),
                    end = Offset(padX + plotW, y),
                    strokeWidth = 0.5f
                )
            }

            // Draw trend line
            val trendStartY = padY + (1f - ((trendLine.first * xMin + trendLine.second - yMin) / yRange)) * plotH
            val trendEndY = padY + (1f - ((trendLine.first * xMax + trendLine.second - yMin) / yRange)) * plotH
            drawLine(trendColor.copy(alpha = 0.6f), start = Offset(padX, trendStartY), end = Offset(padX + plotW, trendEndY), strokeWidth = 2.5f)

            // Draw data points
            dataPoints.forEachIndexed { i, (x, y) ->
                val px = padX + ((x - xMin) / xRange) * plotW
                val py = padY + (1f - ((y - yMin) / yRange)) * plotH
                val isSelected = i == selectedPoint
                drawCircle(pointColor.copy(alpha = 0.3f), radius = if (isSelected) 10f else 7f, center = Offset(px, py))
                drawCircle(if (isSelected) pointColor else pointColor.copy(alpha = 0.7f), radius = if (isSelected) 6f else 4f, center = Offset(px, py))
                if (isSelected) {
                    drawCircle(Color.White, radius = 2.5f, center = Offset(px, py))
                }
            }
        }

        // Selected point info
        selectedPoint?.let { idx ->
            val (x, y) = dataPoints[idx]
            Text(
                "$xLabel: ${"%.1f".format(x)} → $yLabel: ${y.toInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = pointColor,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Axis labels
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$xLabel →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
            Text("$yLabel ↑", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  8. NETWORK GRAPH TIMELINE — Animated knowledge graph evolution
// ══════════════════════════════════════════════════════════════════════

/**
 * Animated network graph showing how the knowledge graph evolves over time.
 * New nodes appear with a scale animation. Time slider lets user scrub through
 * the evolution timeline (Months).
 */
@Composable
fun NetworkGraphTimeline(
    allNodes: List<GraphNode>,
    allEdges: List<Pair<Int, Int>>,
    nodeTimestamps: List<Long>, // Creation timestamp for each node (same index as allNodes)
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 220.dp,
    edgeColor: Color = MaterialTheme.colorScheme.outline
) {
    if (allNodes.size < 2) {
        Text("Add linked projects, questions, and observations to see your network evolve.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    val minTime = nodeTimestamps.minOrNull() ?: 0L
    val maxTime = nodeTimestamps.maxOrNull() ?: 1L
    val timeRange = (maxTime - minTime).coerceAtLeast(1L)

    // Timeline slider position (0f .. 1f)
    var sliderPosition by remember { mutableStateOf(1f) }
    val animatedPosition by animateFloatAsState(
        targetValue = sliderPosition,
        animationSpec = tween(300),
        label = "timeline"
    )

    // Filter nodes and edges up to current time position
    val cutoffTime = (minTime + (timeRange * animatedPosition).toLong()).coerceAtMost(maxTime)
    val visibleIndices = remember(nodeTimestamps, cutoffTime) {
        nodeTimestamps.indices.filter { nodeTimestamps[it] <= cutoffTime }.toSet()
    }
    val visibleNodes = remember(allNodes, visibleIndices) {
        allNodes.filterIndexed { i, _ -> i in visibleIndices }
    }
    val visibleEdges = remember(allEdges, visibleIndices) {
        allEdges.filter { (a, b) -> a in visibleIndices && b in visibleIndices }
    }
    val nodeIndexMap = remember(visibleIndices) {
        visibleIndices.sorted().mapIndexed { idx, originalIdx -> originalIdx to idx }.toMap()
    }
    val remappedEdges = remember(visibleEdges, nodeIndexMap) {
        visibleEdges.mapNotNull { (a, b) ->
            val na = nodeIndexMap[a]; val nb = nodeIndexMap[b]
            if (na != null && nb != null) na to nb else null
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Time info
        val cutoffDate = Instant.ofEpochMilli(cutoffTime).atZone(ZoneId.systemDefault()).toLocalDate()
        Text(
            "Showing: ${visibleNodes.size}/${allNodes.size} nodes • ${cutoffDate.month.name} ${cutoffDate.year}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        // Network graph
        Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            if (visibleNodes.size < 2) {
                Text("Not enough connected entities yet",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Canvas(Modifier.fillMaxWidth().height(height)) {
                    val cx = this.size.width / 2f
                    val cy = this.size.height / 2f
                    val radius = this.size.minDimension / 2f * 0.78f
                    val positions = visibleNodes.indices.map { i ->
                        val angle = (2.0 * Math.PI * i / visibleNodes.size) - Math.PI / 2.0
                        Offset(cx + (radius * Math.cos(angle)).toFloat(), cy + (radius * Math.sin(angle)).toFloat())
                    }
                    remappedEdges.forEach { (a, b) ->
                        if (a in positions.indices && b in positions.indices) {
                            drawLine(edgeColor.copy(alpha = 0.35f), positions[a], positions[b], strokeWidth = 1.5f)
                        }
                    }
                    visibleNodes.forEachIndexed { i, node ->
                        val p = positions[i]
                        val r = if (node.emphasis) 12f else 6f
                        drawCircle(node.color.copy(alpha = 0.2f), radius = r + 5f, center = p)
                        drawCircle(node.color, radius = r, center = p)
                    }
                }
            }
        }

        // Timeline slider
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Past", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            Box(Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Box(
                    Modifier
                        .fillMaxWidth(sliderPosition)
                        .fillMaxHeight()
                        .background(FieldMindTheme.colors.observation.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { /* handled by pointer input */ },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .background(FieldMindTheme.colors.observation, CircleShape)
                            .clickable(enabled = false) { }
                    )
                }
            }
            Text("Present", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  9. DATA QUALITY METER — Research health score gauge
// ══════════════════════════════════════════════════════════════════════

/**
 * Data quality score gauge showing research health across multiple dimensions.
 * Displays a score (0-100) with sub-metrics.
 */
@Composable
fun DataQualityMeter(
    score: Int, // 0-100
    metrics: List<Pair<String, Float>>, // Each: (label, fraction 0f-1f)
    modifier: Modifier = Modifier,
    accentColor: Color = FieldMindTheme.colors.positive
) {
    val scoreColor = when {
        score >= 80 -> FieldMindTheme.colors.positive
        score >= 50 -> FieldMindTheme.colors.warning
        else -> FieldMindTheme.colors.error
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Score gauge
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$score",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = scoreColor
                )
                Text(
                    when {
                        score >= 80 -> "Excellent"
                        score >= 60 -> "Good"
                        score >= 40 -> "Fair"
                        else -> "Needs attention"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scoreColor
                )
            }
        }

        // Sub-metrics
        metrics.forEach { (label, fraction) ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight()
                        .background(accentColor.copy(alpha = 0.3f + fraction * 0.7f), RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Shared grid color for chart axes
// ══════════════════════════════════════════════════════════════════════

private val gridColor: Color get() = Color.Gray.copy(alpha = 0.3f)
