package chromahub.rhythm.app.features.field.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Dependency-free charting primitives drawn with Compose [Canvas]. These compute everything
 * locally from in-memory lists so they work fully offline and add no library weight.
 */

/** Circular progress ring with a centered value/caption — used for goals and streaks. */
@Composable
fun ProgressRing(
    progress: Float,
    centerValue: String,
    caption: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    size: androidx.compose.ui.unit.Dp = 96.dp
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val p = progress.coerceIn(0f, 1f)
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val stroke = this.size.minDimension * 0.12f
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * p,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerValue, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Circular progress ring with a multi-color gradient sweep, an animated fill, a large centered
 * value and a caption. Used for the Today daily-goal hero.
 */
@Composable
fun GradientProgressRing(
    progress: Float,
    centerValue: String,
    caption: String,
    modifier: Modifier = Modifier,
    gradient: List<Color>,
    track: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    size: androidx.compose.ui.unit.Dp = 112.dp
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = target, animationSpec = tween(700), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val stroke = this.size.minDimension * 0.13f
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (animated > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(gradient),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerValue, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Vertical bar chart from labelled values. Each bar gets a distinct shade of [barColor], and tapping
 * a bar reveals its label and entry count above the chart.
 */
@Composable
fun BarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    barColors: List<Color>? = null,
    height: androidx.compose.ui.unit.Dp = 140.dp
) {
    if (data.isEmpty()) return
    val max = (data.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val n = data.size
    val shades = remember(data, barColor, barColors) {
        if (barColors != null) data.indices.map { i -> barColors[i % barColors.size] }
        else data.indices.map { i -> barColor.copy(alpha = (1f - 0.5f * (i.toFloat() / (n - 1).coerceAtLeast(1))).coerceIn(0.5f, 1f)) }
    }
    var selected by remember(data) { mutableStateOf(-1) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val sel = data.getOrNull(selected)
        val selColor = if (selected >= 0) shades[selected] else barColor
        Text(
            if (sel != null) "${sel.first}: ${sel.second.toInt()} ${if (sel.second.toInt() == 1) "entry" else "entries"}" else "Tap a bar to see its count",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (sel != null) FontWeight.SemiBold else FontWeight.Normal,
            color = if (sel != null) selColor else labelColor
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val gap = size.width * 0.04f
                        val barWidth = (size.width - gap * (n + 1)) / n
                        val idx = ((offset.x - gap) / (barWidth + gap)).toInt()
                        selected = if (idx in 0 until n) idx else -1
                    }
                }
        ) {
            val gap = this.size.width * 0.04f
            val barWidth = (this.size.width - gap * (n + 1)) / n
            val radius = barWidth * 0.25f
            data.forEachIndexed { i, (_, v) ->
                val h = (v / max) * this.size.height
                val left = gap + i * (barWidth + gap)
                drawRoundRect(
                    color = if (selected >= 0 && i != selected) shades[i].copy(alpha = 0.4f) else shades[i],
                    topLeft = Offset(left, this.size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
                if (i == selected) {
                    drawRoundRect(
                        color = shades[i],
                        topLeft = Offset(left, this.size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                        style = Stroke(width = 3f)
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            data.forEachIndexed { i, (label, _) ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (i == selected) shades[i] else labelColor, fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Smooth line/area chart for a trend over time. */
@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    height: androidx.compose.ui.unit.Dp = 120.dp
) {
    if (values.size < 2) return
    val max = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val min = values.minOrNull() ?: 0f
    val range = (max - min).coerceAtLeast(1f)
    Canvas(modifier.fillMaxWidth().height(height)) {
        val stepX = this.size.width / (values.size - 1)
        fun pointY(v: Float) = this.size.height - ((v - min) / range) * this.size.height * 0.9f - this.size.height * 0.05f
        val line = Path()
        val area = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = pointY(v)
            if (i == 0) { line.moveTo(x, y); area.moveTo(x, this.size.height); area.lineTo(x, y) }
            else { line.lineTo(x, y); area.lineTo(x, y) }
        }
        area.lineTo(this.size.width, this.size.height)
        area.close()
        drawPath(area, fillColor)
        drawPath(line, lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
    }
}

/** Horizontal stacked breakdown legend with colored shares. */
@Composable
fun BreakdownBar(
    parts: List<Triple<String, Float, Color>>,
    modifier: Modifier = Modifier
) {
    val total = parts.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.fillMaxWidth().height(16.dp)) {
            var x = 0f
            parts.forEach { (_, value, color) ->
                val w = (value / total) * this.size.width
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, 0f),
                    size = Size((w - 3f).coerceAtLeast(0f), this.size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
                x += w
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            parts.filter { it.second > 0f }.forEach { (label, value, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(12.dp).padding(0.dp)) {
                        Canvas(Modifier.fillMaxWidth()) {
                            drawRoundRect(color = color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f))
                        }
                    }
                    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(value.toInt().toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Lightweight offline "map": plots GPS points by normalizing lat/long into the canvas box.
 * No tiles, no network, no API key — just spatial distribution of observations.
 */
/** A node in a [ConnectionGraph]: a labeled, colored dot. */
data class GraphNode(val label: String, val color: Color, val emphasis: Boolean = false)

/**
 * Dependency-free knowledge graph drawn as a connection wheel: nodes are placed on a circle and
 * relationships are drawn as chords across it. Emphasized (hub) nodes are larger and labeled.
 */
@Composable
fun ConnectionGraph(
    nodes: List<GraphNode>,
    edges: List<Pair<Int, Int>>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 240.dp,
    edgeColor: Color = MaterialTheme.colorScheme.outline
) {
    Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        if (nodes.size < 2) {
            Text("Link records to projects, questions, and sources to grow your graph.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Box
        }
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(height)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val radius = this.size.minDimension / 2f * 0.78f
            val positions = nodes.indices.map { i ->
                val angle = (2.0 * Math.PI * i / nodes.size) - Math.PI / 2.0
                Offset(cx + (radius * Math.cos(angle)).toFloat(), cy + (radius * Math.sin(angle)).toFloat())
            }
            edges.forEach { (a, b) ->
                if (a in positions.indices && b in positions.indices) {
                    drawLine(edgeColor.copy(alpha = 0.4f), positions[a], positions[b], strokeWidth = 2f)
                }
            }
            nodes.forEachIndexed { i, node ->
                val p = positions[i]
                val r = if (node.emphasis) 13f else 7f
                drawCircle(node.color.copy(alpha = 0.25f), radius = r + 6f, center = p)
                drawCircle(node.color, radius = r, center = p)
            }
        }
    }
}

@Composable
fun MiniMap(
    points: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier,
    pointColor: Color = MaterialTheme.colorScheme.primary,
    height: androidx.compose.ui.unit.Dp = 180.dp
) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        if (points.isEmpty()) {
            Text("No GPS-tagged observations yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Box
        }
        val lats = points.map { it.first }
        val lons = points.map { it.second }
        val minLat = lats.min(); val maxLat = lats.max()
        val minLon = lons.min(); val maxLon = lons.max()
        val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
        val lonRange = (maxLon - minLon).coerceAtLeast(0.0001)
        Canvas(Modifier.fillMaxWidth().height(height)) {
            for (i in 1..3) {
                val gx = this.size.width * i / 4f
                val gy = this.size.height * i / 4f
                drawLine(grid, Offset(gx, 0f), Offset(gx, this.size.height), strokeWidth = 1f)
                drawLine(grid, Offset(0f, gy), Offset(this.size.width, gy), strokeWidth = 1f)
            }
            val pad = this.size.minDimension * 0.1f
            points.forEach { (lat, lon) ->
                val x = pad + ((lon - minLon) / lonRange).toFloat() * (this.size.width - 2 * pad)
                val y = pad + (1f - ((lat - minLat) / latRange).toFloat()) * (this.size.height - 2 * pad)
                drawCircle(pointColor.copy(alpha = 0.25f), radius = 14f, center = Offset(x, y))
                drawCircle(pointColor, radius = 6f, center = Offset(x, y))
            }
        }
    }
}
