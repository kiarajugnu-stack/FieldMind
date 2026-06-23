package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlin.math.*

/**
 * A floating vertical zoom slider for the canvas.
 *
 * Positioned on the right side of the canvas, this slider allows the user
 * to smoothly zoom from 0.1× to 5.0× using a logarithmic scale.
 * Shows the current zoom percentage and uses pinch-to-zoom from gestures.
 *
 * @param canvasState shared camera state
 * @param viewportSize current viewport size in screen pixels (for focus-centering)
 * @param modifier standard Compose modifier
 * @param show whether the slider is visible (hidden in PAGES mode)
 */
@Composable
fun ZoomSlider(
    canvasState: CanvasState,
    viewportSize: Size = Size(0f, 0f),
    modifier: Modifier = Modifier,
    show: Boolean = true
) {
    if (!show) return

    val density = LocalDensity.current
    val sliderHeightPx = with(density) { 180.dp.toPx() }
    val sliderWidthPx = with(density) { 32.dp.toPx() }

    // Map zoom to slider fraction (logarithmic scale)
    // sliderFraction 0.0 → minZoom, 1.0 → maxZoom
    val sliderFraction = remember(canvasState.zoom) {
        (ln(canvasState.zoom / CanvasState.minZoom) / ln(CanvasState.maxZoom / CanvasState.minZoom))
            .coerceIn(0.0, 1.0)
    }

    // Animate the knob position (inverted: top = max zoom, bottom = min zoom)
    val knobFraction by animateFloatAsState(
        targetValue = 1f - sliderFraction.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "zoomSliderKnob"
    )

    // Animated visibility for the label
    var showLabel by remember { mutableStateOf(false) }
    val labelAlpha by animateFloatAsState(
        targetValue = if (showLabel) 1f else 0f,
        animationSpec = spring(),
        label = "zoomLabelAlpha"
    )

    Column(
        modifier = modifier
            .width(44.dp)
            .heightIn(min = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ── Zoom in button ──
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
                )
                .pointerInput(Unit) {
                    detectTapGestures {
                        val focus = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                        canvasState.applyZoom(1.25f, focus)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                MaterialSymbolIcon("add"),
                "Zoom in",
                size = 18.dp,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // ── Slider track ──
        Box(
            modifier = Modifier
                .width(with(density) { 32.dp })
                .height(with(density) { 180.dp })
                .clip(RoundedCornerShape(16.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                )
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        // Invert: top of slider (y=0) = max zoom, bottom = min zoom
                        val fraction = 1f - (tapOffset.y / sliderHeightPx).coerceIn(0f, 1f)
                        val newZoom = mapFractionToZoom(fraction)
                        canvasState.zoomTo(newZoom)
                        showLabel = true
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val fraction = 1f - (change.position.y / sliderHeightPx).coerceIn(0f, 1f)
                        val newZoom = mapFractionToZoom(fraction)
                        canvasState.zoomTo(newZoom)
                        showLabel = true
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            // ── Track background ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp)
            ) {
                // Vertical line
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .align(Alignment.Center)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            RoundedCornerShape(1.dp)
                        )
                )
            }

            // ── Knob (positioned from top, higher zoom = higher position) ──
            val knobOffset = (knobFraction * sliderHeightPx).coerceIn(0f, sliderHeightPx)
            Box(
                modifier = Modifier
                    .offset(y = with(density) { knobOffset.toDp() })
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f))
                )
            }
        }

        // ── Zoom out button ──
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
                )
                .pointerInput(Unit) {
                    detectTapGestures {
                        val focus = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                        canvasState.applyZoom(0.8f, focus)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                MaterialSymbolIcon("remove"),
                "Zoom out",
                size = 18.dp,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // ── Zoom percentage label (appears on interaction) ──
        AnimatedVisibility(
            visible = labelAlpha > 0f,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                            alpha = 0.9f * labelAlpha
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${(canvasState.zoom * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Auto-hide the label after 2 seconds
        LaunchedEffect(showLabel) {
            if (showLabel) {
                kotlinx.coroutines.delay(2000)
                showLabel = false
            }
        }
    }
}

/**
 * Maps a slider fraction (0.0–1.0) to a zoom value on a logarithmic scale.
 * 0.0 → [CanvasState.minZoom] (0.1x), 1.0 → [CanvasState.maxZoom] (5.0x).
 */
private fun mapFractionToZoom(fraction: Float): Float {
    val logMin = ln(CanvasState.minZoom.toDouble())
    val logMax = ln(CanvasState.maxZoom.toDouble())
    return exp(logMin + fraction.toDouble() * (logMax - logMin)).toFloat()
}
