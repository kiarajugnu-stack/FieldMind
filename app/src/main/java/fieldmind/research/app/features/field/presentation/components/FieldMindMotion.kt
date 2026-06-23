package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlin.coroutines.cancellation.CancellationException
import androidx.activity.BackEventCompat
import androidx.activity.ExperimentalActivityApi
import androidx.activity.compose.PredictiveBackHandler
import fieldmind.research.app.shared.presentation.components.icons.Icon
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Material expressive motion specifications for FieldMind.
 */
object FieldMindMotion {

    // -- Expressive Springs (overshoot / bounce / elastic) --

    val expressiveSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val expressiveSoft = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val expressiveElastic = spring<Float>(
        dampingRatio = 0.3f,
        stiffness = Spring.StiffnessMedium
    )

    val expressiveFloat = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val expressiveSnap = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )

    val expressiveDramatic = spring<Float>(
        dampingRatio = 0.4f,
        stiffness = 400f
    )

    // -- Standard Springs (no overshoot) --

    val layoutSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    val pressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val confirmSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    // -- Navigation Springs --

    val swipeBackSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 300f
    )

    val sharedElementSpring = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 600f
    )

    val slideSpring = spring<Float>(
        dampingRatio = 0.75f,
        stiffness = 700f
    )

    val fadeThroughSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val slideOffsetSpring = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // -- Duration Tokens (ms) --

    const val durationMicro = 120
    const val durationSubtle = 200
    const val durationStandard = 350
    const val durationEmphasized = 500
    const val durationExpressive = 800
    const val countUpMs = 600

    // -- Stagger & Delay Tokens --

    const val staggerItemDelayMs = 50
    const val staggerInitialDelayMs = 80
    const val staggerMaxDurationMs = 500

    // -- Shape Morphing --

    val morphSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val cornerSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // -- Convenience Tween --

    val fadeTween = tween<Float>(durationMillis = durationSubtle)
    val pressScaleTween = tween<Float>(durationMillis = durationMicro)

    // -- Swipe-back Constants --

    const val swipeEdgeWidthDp = 30f
    const val swipeEdgeHeightDp = 30f
    const val swipeThreshold = 0.30f
    const val swipeScaleFactor = 0.92f
    const val swipeScrimAlpha = 0.35f
    const val swipeShadowElevationDp = 24f

    // -- Utility --

    fun entranceSpec(emphasis: Emphasis = Emphasis.Standard): AnimationSpec<Float> = when (emphasis) {
        Emphasis.Expressive -> expressiveDramatic
        Emphasis.Emphasized -> expressiveSpring
        Emphasis.Standard -> expressiveFloat
        Emphasis.Snap -> expressiveSnap
    }

    enum class Emphasis { Expressive, Emphasized, Standard, Snap }

    @Composable
    fun isReduceMotion(): Boolean {
        if (LocalInspectionMode.current) return false
        val context = LocalContext.current
        val animatorScale = try {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
            )
        } catch (_: Exception) { 1f }
        return animatorScale == 0f
    }

    fun staggerDelay(index: Int): Int =
        (staggerInitialDelayMs + index * staggerItemDelayMs).coerceAtMost(staggerMaxDurationMs)
}

// -- Expressive Press Modifiers --

fun Modifier.expressivePress(
    scaleDown: Float = 0.95f,
    enabled: Boolean = true
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "expressivePress"
        properties["scaleDown"] = scaleDown
        properties["enabled"] = enabled
    }
) {
    var isPressed by remember { mutableStateOf(false) }
    val reduceMotion = FieldMindMotion.isReduceMotion()
    val target = if (isPressed && enabled && !reduceMotion) scaleDown else 1f

    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = if (isPressed) FieldMindMotion.expressiveSnap else FieldMindMotion.expressiveSpring,
        label = "expressivePress"
    )

    this
        .pointerInput(enabled) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    isPressed = event.changes.any { it.pressed }
                }
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin.Center
        }
}

fun Modifier.expressiveCardPress(
    liftDp: Float = 2f,
    scaleDown: Float = 0.98f,
    enabled: Boolean = true
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "expressiveCardPress"
        properties["liftDp"] = liftDp
        properties["scaleDown"] = scaleDown
    }
) {
    var isPressed by remember { mutableStateOf(false) }
    val reduceMotion = FieldMindMotion.isReduceMotion()

    val animScale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) scaleDown else 1f,
        animationSpec = if (isPressed) FieldMindMotion.expressiveSnap else FieldMindMotion.expressiveSpring,
        label = "cardScale"
    )
    val animLift by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) -liftDp else 0f,
        animationSpec = if (isPressed) FieldMindMotion.expressiveSnap else FieldMindMotion.expressiveSoft,
        label = "cardLift"
    )

    this
        .pointerInput(enabled) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    isPressed = event.changes.any { it.pressed }
                }
            }
        }
        .graphicsLayer {
            scaleX = animScale
            scaleY = animScale
            translationY = animLift
            transformOrigin = TransformOrigin.Center
        }
}

fun Modifier.pressScale(
    scaleDown: Float = 0.97f,
    enabled: Boolean = true
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "pressScale"
        properties["scaleDown"] = scaleDown
        properties["enabled"] = enabled
    }
) {
    var isPressed by remember { mutableStateOf(false) }
    val reduceMotion = FieldMindMotion.isReduceMotion()
    val target = if (isPressed && enabled && !reduceMotion) scaleDown else 1f

    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = FieldMindMotion.pressSpring,
        label = "pressScale"
    )

    this
        .pointerInput(enabled) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    isPressed = event.changes.any { it.pressed }
                }
            }
        }
        .scale(scale)
}

fun Modifier.pressCardScale(): Modifier = composed {
    this.pressScale(scaleDown = 0.97f)
}

// -- Tab Swipe Host -- switch between adjacent tabs with horizontal swipe --

/**
 * Wraps content with a horizontal swipe gesture that triggers tab switching.
 * Unlike [SwipeBackHost] which only activates from the left edge, this detects
 * swipes anywhere on the content area (like iOS springboard).
 *
 * Swipe left → calls [onSwipeForward] (next tab)
 * Swipe right → calls [onSwipeBack] (previous tab)
 *
 * Shows visual feedback (offset, scale, gradient scrim) during the swipe.
 */
@Composable
fun TabSwipeHost(
    onSwipeBack: () -> Unit,
    onSwipeForward: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduceMotion = FieldMindMotion.isReduceMotion()
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()

    var tabOffsetX by remember { mutableFloatStateOf(0f) }
    var contentWidth by remember { mutableFloatStateOf(1f) }

    val animatedOffsetX by animateFloatAsState(
        targetValue = tabOffsetX,
        animationSpec = FieldMindMotion.swipeBackSpring,
        label = "tabSwipeX"
    )

    val progress = abs(animatedOffsetX / contentWidth).coerceIn(0f, 1f)
    val scrimAlpha = progress * 0.25f
    val scale = 1f - progress * (1f - FieldMindMotion.swipeScaleFactor)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                contentWidth = coords.size.width.toFloat().coerceAtLeast(1f)
            }
    ) {
        // Gradient scrim on the side opposite the swipe direction
        if (progress > 0.01f) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    if (animatedOffsetX > 0) {
                        Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = scrimAlpha * 0.9f), Color.Black.copy(alpha = scrimAlpha * 0.4f), Color.Transparent),
                            startX = 0f, endX = contentWidth * 0.5f
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = scrimAlpha * 0.4f), Color.Black.copy(alpha = scrimAlpha * 0.9f)),
                            startX = contentWidth * 0.5f, endX = contentWidth
                        )
                    }
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = animatedOffsetX
                    scaleX = scale
                    scaleY = scale
                    clip = true
                }
                .then(
                    if (!reduceMotion) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { /* swipe anywhere */ },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    tabOffsetX = (tabOffsetX + dragAmount.x).coerceIn(-contentWidth * 0.4f, contentWidth * 0.4f)
                                },
                                onDragEnd = {
                                    val threshold = contentWidth * 0.20f
                                    if (tabOffsetX > threshold) {
                                        haptics.confirm()
                                        scope.launch { tabOffsetX = contentWidth; onSwipeBack() }
                                    } else if (tabOffsetX < -threshold) {
                                        haptics.confirm()
                                        scope.launch { tabOffsetX = -contentWidth; onSwipeForward() }
                                    } else {
                                        tabOffsetX = 0f
                                    }
                                },
                                onDragCancel = {
                                    tabOffsetX = 0f
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.TopStart
        ) {
            content()
        }
    }
}

// -- Swipe-back Gesture Host -- iOS-style with predictive peek --

private enum class SwipeDirection { Horizontal, Vertical }

@OptIn(ExperimentalActivityApi::class)
@Composable
fun SwipeBackHost(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduceMotion = FieldMindMotion.isReduceMotion()
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()

    var activeDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    var targetOffsetX by remember { mutableFloatStateOf(0f) }
    var targetOffsetY by remember { mutableFloatStateOf(0f) }
    var contentWidth by remember { mutableFloatStateOf(1f) }
    var contentHeight by remember { mutableFloatStateOf(1f) }

    // Predictive back gesture (Android 14+) — drives peek animation from system back gesture
    PredictiveBackHandler(enabled = !reduceMotion) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                targetOffsetX = (contentWidth * backEvent.progress).coerceAtLeast(0f)
            }
            // Flow completed → gesture committed; system handles back navigation
        } catch (_: CancellationException) {
            // Gesture cancelled — snap back via spring animation
            targetOffsetX = 0f
        }
    }

    val animatedOffsetX by animateFloatAsState(
        targetValue = targetOffsetX,
        animationSpec = FieldMindMotion.swipeBackSpring,
        label = "swipeX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = targetOffsetY,
        animationSpec = FieldMindMotion.swipeBackSpring,
        label = "swipeY"
    )

    val maxExtent = when (activeDirection) {
        SwipeDirection.Horizontal -> contentWidth
        SwipeDirection.Vertical -> contentHeight
        null -> 1f
    }
    val currentOffset = when (activeDirection) {
        SwipeDirection.Horizontal -> animatedOffsetX
        SwipeDirection.Vertical -> animatedOffsetY
        null -> 0f
    }
    val progress = (currentOffset / maxExtent).coerceIn(0f, 1f)
    val scrimAlpha = progress * FieldMindMotion.swipeScrimAlpha
    val contentScale = 1f - progress * (1f - FieldMindMotion.swipeScaleFactor)
    val swipeElevation = progress * FieldMindMotion.swipeShadowElevationDp

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                contentWidth = coords.size.width.toFloat().coerceAtLeast(1f)
                contentHeight = coords.size.height.toFloat().coerceAtLeast(1f)
            }
    ) {
        if (progress > 0.01f) {
            when (activeDirection) {
                SwipeDirection.Horizontal -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = scrimAlpha * 0.9f),
                                        Color.Black.copy(alpha = scrimAlpha * 0.4f),
                                        Color.Transparent
                                    ),
                                    startX = 0f,
                                    endX = contentWidth * 0.5f
                                )
                            )
                    )
                }
                SwipeDirection.Vertical -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = scrimAlpha * 0.9f),
                                        Color.Black.copy(alpha = scrimAlpha * 0.4f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = contentHeight * 0.5f
                                )
                            )
                    )
                }
                null -> {}
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val ox = animatedOffsetX.roundToInt()
                    val oy = animatedOffsetY.roundToInt()
                    translationX = ox.toFloat()
                    translationY = oy.toFloat()
                    scaleX = contentScale
                    scaleY = contentScale
                    this.shadowElevation = swipeElevation
                    transformOrigin = TransformOrigin(if (ox > 0) 0f else 0.5f, if (oy > 0) 0f else 0.5f)
                    clip = true
                }
                .then(
                    if (!reduceMotion) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { startPos ->
                                    if (startPos.x <= FieldMindMotion.swipeEdgeWidthDp) {
                                        activeDirection = SwipeDirection.Horizontal
                                    } else if (startPos.y <= FieldMindMotion.swipeEdgeHeightDp) {
                                        activeDirection = SwipeDirection.Vertical
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    when (activeDirection) {
                                        SwipeDirection.Horizontal -> {
                                            targetOffsetX = (targetOffsetX + dragAmount.x).coerceAtLeast(0f)
                                            targetOffsetY = 0f
                                        }
                                        SwipeDirection.Vertical -> {
                                            targetOffsetY = (targetOffsetY + dragAmount.y).coerceAtLeast(0f)
                                            targetOffsetX = 0f
                                        }
                                        null -> {
                                            val dx = dragAmount.x
                                            val dy = dragAmount.y
                                            if (abs(dx) > abs(dy) && dx > 0) {
                                                activeDirection = SwipeDirection.Horizontal
                                                targetOffsetX = (targetOffsetX + dx).coerceAtLeast(0f)
                                            } else if (abs(dy) > abs(dx) && dy > 0) {
                                                activeDirection = SwipeDirection.Vertical
                                                targetOffsetY = (targetOffsetY + dy).coerceAtLeast(0f)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    val maxVal = when (activeDirection) {
                                        SwipeDirection.Horizontal -> contentWidth
                                        SwipeDirection.Vertical -> contentHeight
                                        null -> Float.MAX_VALUE
                                    }
                                    val currentVal = when (activeDirection) {
                                        SwipeDirection.Horizontal -> targetOffsetX
                                        SwipeDirection.Vertical -> targetOffsetY
                                        null -> 0f
                                    }
                                    if (currentVal > maxVal * FieldMindMotion.swipeThreshold) {
                                        // If the system back gesture already animated to full extent,
                                        // don't call onBack() again — the system handled it.
                                        if (currentVal < maxVal * 0.99f) {
                                            haptics.confirm()
                                            scope.launch {
                                                when (activeDirection) {
                                                    SwipeDirection.Horizontal -> targetOffsetX = contentWidth
                                                    SwipeDirection.Vertical -> targetOffsetY = contentHeight
                                                    null -> {}
                                                }
                                                onBack()
                                            }
                                        } else {
                                            // System already handled back; just clean up local state
                                            activeDirection = null
                                            targetOffsetX = 0f
                                            targetOffsetY = 0f
                                        }
                                    } else {
                                        activeDirection = null
                                        targetOffsetX = 0f
                                        targetOffsetY = 0f
                                    }
                                },
                                onDragCancel = {
                                    activeDirection = null
                                    targetOffsetX = 0f
                                    targetOffsetY = 0f
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.TopStart
        ) {
            content()

            if (activeDirection == SwipeDirection.Horizontal && currentOffset > contentWidth * 0.05f) {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FieldMindIcons.ChevronLeft, "Swipe back", size = 22.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (activeDirection == SwipeDirection.Vertical && currentOffset > contentHeight * 0.05f) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .align(Alignment.TopCenter)
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(FieldMindIcons.ChevronDown, "Swipe down to dismiss", size = 22.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
