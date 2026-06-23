package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * iOS-style pull-to-refresh container with spring bounce physics.
 *
 * Wraps content and detects overscroll drag past the top edge. Shows a
 * pulsing shimmer indicator that scales with drag distance. When released
 * past the threshold, triggers [onRefresh] with a spring bounce and haptic.
 *
 * Features:
 * - Spring-based drag physics with iOS-style rubber-banding resistance
 * - Shimmer/glow indicator ring that scales and fades with pull distance
 * - Haptic feedback at threshold crossing
 * - Smooth spring-back on cancel
 * - Reduce-motion support
 * - Customizable threshold and indicator size
 */
@Composable
fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indicatorSize: Float = 40f,
    thresholdFraction: Float = 0.12f,
    maxPullDp: Float = 120f,
    content: @Composable () -> Unit
) {
    val reduceMotion = FieldMindMotion.isReduceMotion()
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()
    val density = LocalDensity.current

    var pullOffset by remember { mutableFloatStateOf(0f) }
    var contentHeight by remember { mutableFloatStateOf(1f) }
    var hasTriggered by remember { mutableStateOf(false) }
    var showRefreshIndicator by remember { mutableStateOf(false) }

    val maxPullPx = with(density) { maxPullDp.dp.toPx() }
    val thresholdPx = contentHeight * thresholdFraction

    // Animate the pull offset with spring for iOS-style bounce-back
    val animatedPull by animateFloatAsState(
        targetValue = pullOffset,
        animationSpec = if (pullOffset > 0f)
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 200f)
        else
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 400f),
        label = "pullToRefresh"
    )

    // Normalized progress (0..1) for indicator
    val pullProgress = (animatedPull / maxPullPx).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                contentHeight = coords.size.height.toFloat().coerceAtLeast(1f)
            }
    ) {
        // Content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, animatedPull.roundToInt()) }
                .then(
                    if (enabled && !reduceMotion) {
                        Modifier.pointerInput(Unit) {                                detectVerticalDragGestures(
                                onDragStart = {
                                    hasTriggered = false
                                },
                                onVerticalDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Float ->
                                    change.consume()
                                    // Apply rubber-band resistance: the further you pull,
                                    // the harder it gets (iOS-style diminishing returns)
                                    val currentPull = pullOffset
                                    val newPull = currentPull + dragAmount
                                    pullOffset = if (newPull > 0) {
                                        // Rubber band: resistance increases with pull distance
                                        maxPullPx * (1f - 1f / (newPull / maxPullPx + 1f))
                                    } else {
                                        0f.coerceAtLeast(newPull)
                                    }

                                    // Trigger haptic when crossing threshold
                                    if (!hasTriggered && pullOffset >= thresholdPx) {
                                        hasTriggered = true
                                        haptics.confirm()
                                    }
                                },
                                onDragEnd = {
                                    if (pullOffset >= thresholdPx && !isRefreshing) {
                                        // Trigger refresh — snap to indicator position
                                        showRefreshIndicator = true
                                        scope.launch {
                                            pullOffset = maxPullPx * 0.3f
                                            onRefresh()
                                            // Wait for refresh to complete
                                            while (isRefreshing) {
                                                delay(100)
                                            }
                                            showRefreshIndicator = false
                                            pullOffset = 0f
                                        }
                                    } else {
                                        // Snap back
                                        pullOffset = 0f
                                    }
                                },
                                onDragCancel = {
                                    pullOffset = 0f
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }

        // Pull-to-refresh indicator at the top
        if (pullProgress > 0.01f || showRefreshIndicator) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (showRefreshIndicator) with(density) { (maxPullPx * 0.3f).toDp() } else animatedPull.dp)
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing || showRefreshIndicator) {
                    // Spinning loader when refreshing
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // Shimmer ring that grows with pull distance
                    val ringScale = (pullProgress * 0.6f + 0.4f).coerceIn(0.4f, 1f)
                    val ringAlpha = pullProgress.coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer {
                                scaleX = ringScale
                                scaleY = ringScale
                                this.alpha = ringAlpha
                            }
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = ringAlpha * 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            progress = pullProgress * 2f,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = ringAlpha)
                        )
                    }
                }
            }
        }
    }
}
