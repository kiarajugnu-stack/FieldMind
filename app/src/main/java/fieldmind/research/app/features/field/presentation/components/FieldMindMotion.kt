package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        stiffness = 800f
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
    const val swipeCornerRadiusDp = 22f
    const val swipeBaseCornerRadiusDp = 4f

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
 * System back gesture (edge) → drives predictive peek animation via [PredictiveBackHandler];
 * on commit, navigates back via [onBack].
 *
 * Shows visual feedback (offset, scale, gradient scrim) during the swipe.
 */
@OptIn(ExperimentalActivityApi::class)
@Composable
fun TabSwipeHost(
    onSwipeBack: (() -> Unit)?,
    onSwipeForward: (() -> Unit)?,
    onBack: (() -> Unit)? = null,
    previousScreen: PreviousScreenInfo? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduceMotion = FieldMindMotion.isReduceMotion()
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()

    val animX = remember { Animatable(0f) }
    var contentWidth by remember { mutableFloatStateOf(1f) }

    // Detect keyboard visibility reactively via InputMethodManager
    val context = LocalContext.current
    var isImeVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            isImeVisible = imm?.isAcceptingText ?: false
            delay(100)
        }
    }

    // Check if swipe directions are available
    val canSwipeBack = onSwipeBack != null
    val canSwipeForward = onSwipeForward != null

    // ── Track whether the current gesture is a system back or tab swipe ──
    var isSystemBack by remember { mutableStateOf(false) }

    // Predictive back gesture (Android 14+) — drives peek animation from system back gesture
    PredictiveBackHandler(enabled = !reduceMotion && onBack != null && !isImeVisible) { progressFlow ->
        isSystemBack = true
        try {
            progressFlow.collect { backEvent ->
                val offset = (contentWidth * backEvent.progress).coerceAtLeast(0f)
                animX.snapTo(offset)
            }
            // Flow completed → gesture committed
            isSystemBack = false
            animX.snapTo(0f)
            haptics.confirm()
            onBack?.invoke()
        } catch (_: CancellationException) {
            isSystemBack = false
            animX.snapTo(0f)
        }
    }

    val progress = abs(animX.value / contentWidth).coerceIn(0f, 1f)
    val scrimAlpha = progress * 0.25f
    val scale = 1f - progress * (1f - FieldMindMotion.swipeScaleFactor)
    val swipeCornerRadius = (FieldMindMotion.swipeBaseCornerRadiusDp + progress * (FieldMindMotion.swipeCornerRadiusDp - FieldMindMotion.swipeBaseCornerRadiusDp)).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                contentWidth = coords.size.width.toFloat().coerceAtLeast(1f)
            }
    ) {
        // ── Previous screen peek preview (system back gesture only) ──
        if (progress > 0.01f && previousScreen != null && isSystemBack) {
            val previewWidth = contentWidth * 0.85f
            val previewOffset = animX.value - previewWidth
            val previewScale = 0.94f + (1f - 0.94f) * (1f - progress)
            val screenColor = MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(previewOffset.roundToInt(), 0) }
                    .width(Dp(previewWidth))
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = previewScale
                        scaleY = previewScale
                        transformOrigin = TransformOrigin(1f, 0.5f)
                    }
            ) {
                // Main preview surface — full-height rounded card like a real screen
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    tonalElevation = 3.dp,
                    shadowElevation = 16.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // ── Mock status bar area ──
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(screenColor.copy(alpha = 0.08f))
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        FieldMindIcons.ChevronLeft,
                                        "Back",
                                        size = 22.dp,
                                        tint = screenColor.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        previousScreen.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // ── Mock screen content cards ──
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            repeat(3) { idx ->
                                val cardAlpha = 1f - idx * 0.12f
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (idx == 0) 80.dp else 60.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = cardAlpha * 0.35f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(if (idx == 0) 40.dp else 32.dp)
                                                .background(
                                                    screenColor.copy(alpha = cardAlpha * 0.15f),
                                                    RoundedCornerShape(50)
                                                )
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .width((80 + idx * 30).dp)
                                                    .height(10.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha * 0.15f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width((120 + idx * 20).dp)
                                                    .height(8.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha * 0.09f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            Text(
                                "Swipe to go back",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }

        // ── Gradient scrim on the side opposite the swipe direction ──
        if (progress > 0.01f) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    if (animX.value > 0) {
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

        // ── Current screen content (transformed) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(swipeCornerRadius))
                .graphicsLayer {
                    translationX = animX.value
                    scaleX = scale
                    scaleY = scale
                    clip = true
                }
                .then(
                    if (!reduceMotion && !isImeVisible) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { /* swipe anywhere */ },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    // Only allow drag in directions that have valid callbacks
                                    if ((dragAmount.x > 0 && canSwipeBack) || (dragAmount.x < 0 && canSwipeForward)) {
                                        val newX = (animX.value + dragAmount.x).coerceIn(-contentWidth * 0.4f, contentWidth * 0.4f)
                                        scope.launch { animX.snapTo(newX) }
                                    }
                                },
                                onDragEnd = {
                                    val threshold = contentWidth * 0.20f
                                    if (animX.value > threshold && canSwipeBack) {
                                        haptics.confirm()
                                        onSwipeBack?.invoke()
                                    } else if (animX.value < -threshold && canSwipeForward) {
                                        haptics.confirm()
                                        onSwipeForward?.invoke()
                                    } else {
                                        scope.launch { animX.snapTo(0f) }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { animX.snapTo(0f) }
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

/**
 * Previous screen peek state for the navigation peek animation.
 * Passed from the navigation layer (e.g., NavHost) to show which
 * destination is behind the current screen during the back gesture.
 *
 * @param label Human-readable name of the previous destination
 * @param route Route string of the previous destination (for matching icons)
 */
data class PreviousScreenInfo(
    val label: String,
    val route: String = ""
)

@OptIn(ExperimentalActivityApi::class)
@Composable
fun SwipeBackHost(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    previousScreen: PreviousScreenInfo? = null,
    content: @Composable () -> Unit
) {
    val reduceMotion = FieldMindMotion.isReduceMotion()
    val scope = rememberCoroutineScope()
    val haptics = rememberFieldMindHaptics()

    var activeDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    val animX = remember { Animatable(0f) }
    val animY = remember { Animatable(0f) }
    var contentWidth by remember { mutableFloatStateOf(1f) }
    var contentHeight by remember { mutableFloatStateOf(1f) }

    // Detect keyboard visibility reactively via InputMethodManager
    val context = LocalContext.current
    var isImeVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            isImeVisible = imm?.isAcceptingText ?: false
            delay(100)
        }
    }

    // Predictive back gesture (Android 14+) — drives peek animation from system back gesture
    PredictiveBackHandler(enabled = !reduceMotion && !isImeVisible) { progressFlow ->
        try {
            var hadProgress = false
            // Inside PredictiveBackHandler coroutine — snapTo is suspend, call directly
            progressFlow.collect { backEvent ->
                hadProgress = true
                animX.snapTo((contentWidth * backEvent.progress).coerceAtLeast(0f))
            }
            // Flow completed → gesture committed
            // Reset offset before navigating to prevent blank/offset screen
            // during the pop exit transition.
            animX.snapTo(0f)
            animY.snapTo(0f)
            // Navigate immediately for both gesture swipes AND hardware button presses.
            // Note: detectDragGestures.onDragEnd does NOT fire for system back gestures,
            // so we must navigate here directly rather than deferring to onDragEnd.
            haptics.confirm()
            onBack()
        } catch (_: CancellationException) {
            // Gesture cancelled — snap back via spring animation
            animX.snapTo(0f)
        }
    }

    // ── Unified progress computation ──
    // PredictiveBackHandler drives animX but leaves activeDirection=null.
    // Manual drag sets activeDirection. We compute progress from whichever
    // source has positive offset.
    val horizontalProgress = (abs(animX.value) / contentWidth).coerceIn(0f, 1f)
    val verticalProgress = (abs(animY.value) / contentHeight).coerceIn(0f, 1f)
    val (progress, isHorizontalPeek) = when (activeDirection) {
        SwipeDirection.Horizontal -> Pair(horizontalProgress, true)
        SwipeDirection.Vertical -> Pair(verticalProgress, false)
        null -> Pair(horizontalProgress.coerceAtLeast(verticalProgress), horizontalProgress >= verticalProgress)
    }
    val scrimAlpha = progress * FieldMindMotion.swipeScrimAlpha
    val contentScale = 1f - progress * (1f - FieldMindMotion.swipeScaleFactor)
    val swipeElevation = progress * FieldMindMotion.swipeShadowElevationDp
    val swipeCornerRadius = (FieldMindMotion.swipeBaseCornerRadiusDp + progress * (FieldMindMotion.swipeCornerRadiusDp - FieldMindMotion.swipeBaseCornerRadiusDp)).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                contentWidth = coords.size.width.toFloat().coerceAtLeast(1f)
                contentHeight = coords.size.height.toFloat().coerceAtLeast(1f)
            }
            .background(Color.Transparent) // ensure transparent background so previous screen preview is visible
    ) {
        // ── Layer 1: Previous screen peek preview ──
        // Slides in from the left behind the current content.
        // Uses parallax (70% speed) for depth layering effect.
        if (progress > 0.01f && previousScreen != null && isHorizontalPeek) {
            val previewWidth = contentWidth * 0.85f
            val previewOffset = animX.value - previewWidth
            val previewScale = 0.94f + (1f - 0.94f) * (1f - progress)
            val screenColor = MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(previewOffset.roundToInt(), 0) }
                    .width(Dp(previewWidth))
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = previewScale
                        scaleY = previewScale
                        transformOrigin = TransformOrigin(1f, 0.5f)
                    }
            ) {
                // Main preview surface — full-height rounded card like a real screen
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    tonalElevation = 3.dp,
                    shadowElevation = 16.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // ── Mock status bar area ──
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(screenColor.copy(alpha = 0.08f))
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Back arrow + screen name
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        FieldMindIcons.ChevronLeft,
                                        "Back",
                                        size = 22.dp,
                                        tint = screenColor.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        previousScreen.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // ── Mock screen content area ──
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Placeholder content cards
                            repeat(3) { idx ->
                                val cardAlpha = 1f - idx * 0.12f
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (idx == 0) 80.dp else 60.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = cardAlpha * 0.35f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Mock avatar circle
                                        Box(
                                            modifier = Modifier
                                                .size(if (idx == 0) 40.dp else 32.dp)
                                                .background(
                                                    screenColor.copy(alpha = cardAlpha * 0.15f),
                                                    RoundedCornerShape(50)
                                                )
                                        )
                                        // Mock text lines
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .width((80 + idx * 30).dp)
                                                    .height(10.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha * 0.15f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width((120 + idx * 20).dp)
                                                    .height(8.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha * 0.09f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            // Bottom hint
                            Text(
                                "Swipe to go back",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }

        // ── Layer 2: Scrim (dark gradient on reveal side) ──
        if (progress > 0.01f && isHorizontalPeek) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = scrimAlpha * 0.85f),
                                Color.Black.copy(alpha = scrimAlpha * 0.35f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = contentWidth * 0.5f
                        )
                    )
            )
        } else if (progress > 0.01f) {
            // Vertical scrim for downward swipe
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = scrimAlpha * 0.85f),
                                Color.Black.copy(alpha = scrimAlpha * 0.35f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = contentHeight * 0.5f
                        )
                    )
            )
        }

        // ── Layer 3: Current screen content (transformed) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(swipeCornerRadius))
                .graphicsLayer {
                    val ox = animX.value.roundToInt()
                    val oy = animY.value.roundToInt()
                    translationX = ox.toFloat()
                    translationY = oy.toFloat()
                    scaleX = contentScale
                    scaleY = contentScale
                    this.shadowElevation = swipeElevation
                    transformOrigin = TransformOrigin(
                        if (ox > 0) 0f else 0.5f,
                        if (oy > 0) 0f else 0.5f
                    )
                    clip = true
                }
                .then(
                    if (!reduceMotion && !isImeVisible) {
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
                                            val newX = (animX.value + dragAmount.x).coerceAtLeast(0f)
                                            scope.launch { animX.snapTo(newX) }
                                        }
                                        SwipeDirection.Vertical -> {
                                            val newY = (animY.value + dragAmount.y).coerceAtLeast(0f)
                                            scope.launch { animY.snapTo(newY) }
                                        }
                                        null -> {
                                            val dx = dragAmount.x
                                            val dy = dragAmount.y
                                            if (abs(dx) > abs(dy) && dx > 0) {
                                                activeDirection = SwipeDirection.Horizontal
                                                scope.launch { animX.snapTo(dx.coerceAtLeast(0f)) }
                                            } else if (abs(dy) > abs(dx) && dy > 0) {
                                                activeDirection = SwipeDirection.Vertical
                                                scope.launch { animY.snapTo(dy.coerceAtLeast(0f)) }
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
                                        SwipeDirection.Horizontal -> animX.value
                                        SwipeDirection.Vertical -> animY.value
                                        null -> 0f
                                    }
                                    if (currentVal > maxVal * FieldMindMotion.swipeThreshold) {
                                        haptics.confirm()
                                        activeDirection = null
                                        // Snap offset to 0 immediately before navigating.
                                        // snapTo is a suspend function — must be called inside scope.launch.
                                        scope.launch {
                                            animX.snapTo(0f)
                                            animY.snapTo(0f)
                                            onBack()
                                        }
                                    } else {
                                        // Snap back to 0
                                        activeDirection = null
                                        scope.launch {
                                            animX.snapTo(0f)
                                            animY.snapTo(0f)
                                        }
                                    }
                                },
                                onDragCancel = {
                                    activeDirection = null
                                    scope.launch {
                                        animX.snapTo(0f)
                                        animY.snapTo(0f)
                                    }
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

            // ── Back arrow indicator ──
            if (isHorizontalPeek && animX.value > contentWidth * 0.05f) {
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

            // ── Downward swipe indicator ──
            if (!isHorizontalPeek && animY.value > contentHeight * 0.05f) {
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
