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
import fieldmind.research.app.shared.presentation.components.icons.Icon
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Material expressive motion specifications for FieldMind.
 *
 * These specs follow the M3 Expressive motion theming guidelines:
 * - **Expressive** springs overshoot for personality (bouncy, vibrant)
 * - **Standard** springs are smooth and predictable (utility, clarity)
 * - All durations are named tokens for consistency
 * - Stagger delays create cascading list animations
 */
object FieldMindMotion {

    // ── Expressive Springs (overshoot / bounce / elastic) ──

    /** Expressive: bold bounce for hero elements, cards entering, state changes. */
    val expressiveSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,   // 0.5 — noticeable overshoot
        stiffness = Spring.StiffnessMedium                 // 1500 — responsive but visible
    )

    /** Expressive: soft bounce for secondary elements, chips, icons. */
    val expressiveSoft = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow                    // 200 — gentle, slow bounce
    )

    /** Expressive: elastic stretch for emphasis (pulls past target then snaps back). */
    val expressiveElastic = spring<Float>(
        dampingRatio = 0.3f,                               // Low damping = more oscillation
        stiffness = Spring.StiffnessMedium
    )

    /** Expressive: gentle float for fading, alpha transitions. */
    val expressiveFloat = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,        // 1.0 — no overshoot
        stiffness = Spring.StiffnessMediumLow
    )

    /** Expressive: rapid snap for micro-interactions (press, tap). */
    val expressiveSnap = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh                   // ~10k — instant with tiny bounce
    )

    /** Expressive: dramatic entrance for onboarding, modals, sheets. */
    val expressiveDramatic = spring<Float>(
        dampingRatio = 0.4f,
        stiffness = 400f                                   // Low stiffness = slow, dramatic bounce
    )

    // ── Standard Springs (no overshoot, utility) ──

    /** Standard: smooth layout transitions. */
    val layoutSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    /** Standard: press feedback. */
    val pressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Standard: save confirmations. */
    val confirmSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    // ── Navigation Springs ──

    /** Navigation: swipe-back gesture feel (low stiffness = slow drag following). */
    val swipeBackSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,   // 0.5 — subtle overshoot on release
        stiffness = 300f                                   // Low stiffness = gentle follow
    )

    /** Navigation: shared element morphing (card content → detail header). */
    val sharedElementSpring = spring<Float>(
        dampingRatio = 0.7f,                                // Slightly overdamped for clean morph
        stiffness = 600f                                    // Medium-fast for responsive feel
    )

    /** Navigation: axis slide between screens (list→detail). */
    val slideSpring = spring<Float>(
        dampingRatio = 0.75f,                               // Slightly bouncy
        stiffness = 700f                                    // Responsive but visible
    )

    /** Navigation: fade-through crossfade (hub→settings page). */
    val fadeThroughSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,         // 1.0 — clean, no overshoot
        stiffness = Spring.StiffnessMediumLow               // ~300 — gentle
    )

    // ── Navigation Spring for IntOffset (used in slide transitions) ──

    /** Spring spec for IntOffset-based slide transitions (pop gestures). */
    val slideOffsetSpring = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // ── Duration Tokens (ms) ──

    /** Micro-interactions: press, hover, tap feedback. */
    const val durationMicro = 120

    /** Subtle transitions: color changes, alpha fades. */
    const val durationSubtle = 200

    /** Standard transitions: card expansion, list item enter. */
    const val durationStandard = 350

    /** Emphasized transitions: screen transitions, hero animations. */
    const val durationEmphasized = 500

    /** Expressive transitions: dramatic entrance, onboarding. */
    const val durationExpressive = 800

    /** Number count-up animation. */
    const val countUpMs = 600

    // ── Stagger & Delay Tokens ──

    /** Delay between each staggered item in a list. */
    const val staggerItemDelayMs = 50

    /** Delay for the first item in a staggered animation. */
    const val staggerInitialDelayMs = 80

    /** Maximum total stagger duration for a list. */
    const val staggerMaxDurationMs = 500

    // ── Shape Morphing ──

    /** Spring for morphing between shapes (e.g., pill → square). */
    val morphSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Spring for corner radius animation. */
    val cornerSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // ── Convenience Tween ──

    /** Fade tween (200ms). */
    val fadeTween = tween<Float>(durationMillis = durationSubtle)

    /** Scale press tween (120ms). */
    val pressScaleTween = tween<Float>(durationMillis = durationMicro)

    // ── Swipe-back Constants ──

    /** Edge zone width for detecting horizontal swipe-back gestures (dp). */
    const val swipeEdgeWidthDp = 30f

    /** Edge zone height for detecting vertical swipe-dismiss gestures (dp). */
    const val swipeEdgeHeightDp = 30f

    /** Fraction of screen width/height required to commit the back gesture. */
    const val swipeThreshold = 0.30f

    /** Max scale-down factor applied during swipe (1.0 = no scale). */
    const val swipeScaleFactor = 0.92f

    /** Max scrim alpha during swipe. */
    const val swipeScrimAlpha = 0.35f

    /** Shadow elevation (dp) at full swipe progress. */
    const val swipeShadowElevationDp = 24f

    // ── Utility ──

    /**
     * Returns the appropriate [AnimationSpec] for an expressive entrance
     * based on the [emphasis] level.
     */
    fun entranceSpec(emphasis: Emphasis = Emphasis.Standard): AnimationSpec<Float> = when (emphasis) {
        Emphasis.Expressive -> expressiveDramatic
        Emphasis.Emphasized -> expressiveSpring
        Emphasis.Standard -> expressiveFloat
        Emphasis.Snap -> expressiveSnap
    }

    /** Emphasis levels for animation weighting. */
    enum class Emphasis { Expressive, Emphasized, Standard, Snap }

    /** Check if reduce-motion is enabled. */
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

    /**
     * Stagger delay for list items. Use with LaunchedEffect(index * staggerItemDelayMs).
     * @param index The item's index in the list
     * @return Delay in milliseconds for this item
     */
    fun staggerDelay(index: Int): Int =
        (staggerInitialDelayMs + index * staggerItemDelayMs).coerceAtMost(staggerMaxDurationMs)
}

// ──────────────────────────────────────────────────────────────────────
//  Expressive Press Modifiers
// ──────────────────────────────────────────────────────────────────────

/**
 * Expressive press with Material 3 bounce-back.
 * Scales to [scaleDown] on press, then springs back with expressive overshoot.
 */
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

/**
 * Expressive card press with lift + scale.
 * Cards lift (translateY negative) and scale down slightly, mimicking a literal "lift."
 */
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

/**
 * Standard press scale (backward-compatible with existing usage).
 * Scales down to [scaleDown] when pressed, with a spring bounce.
 */
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

/**
 * Modifier that applies press scale feedback + clips to shape.
 * Use on all interactive cards and buttons for consistent feel.
 */
fun Modifier.pressCardScale(): Modifier = composed {
    this.pressScale(scaleDown = 0.97f)
}

// ──────────────────────────────────────────────────────────────────────
//  Swipe-back Gesture Host — iOS-style with predictive peek
// ──────────────────────────────────────────────────────────────────────

/**
 * Direction of a swipe-back gesture.
 */
private enum class SwipeDirection { Horizontal, Vertical }

/**
 * Wraps content with iOS-style swipe-back and swipe-down gestures.
 *
 * **Horizontal swipe** (from left edge) — the content follows the finger,
 * scales down slightly, and reveals a gradient scrim + shadow edge behind
 * it, mimicking the iOS back-swipe preview effect.
 *
 * **Vertical swipe** (from top edge) — the content slides down with
 * similar effects, useful for dismissing modal-style screens.
 *
 * On release past 30% threshold → calls [onBack] (which triggers the
 * NavHost's popExitTransition/popEnterTransition, rendering both screens).
 * On release before threshold → springs back to position.
 *
 * Place this around any screen composable that should support swipe-back
 * navigation (settings, tools, detail, creation — not bottom-nav tabs).
 */
@Composable
fun SwipeBackHost(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduceMotion = FieldMindMotion.isReduceMotion()
    val scope = rememberCoroutineScope()

    // Track which swipe direction is active
    var activeDirection by remember { mutableStateOf<SwipeDirection?>(null) }

    // Raw offsets driven by the gesture (immediate, no animation)
    var rawOffsetX by remember { mutableFloatStateOf(0f) }
    var rawOffsetY by remember { mutableFloatStateOf(0f) }

    // Target offsets that animate with spring physics
    var targetOffsetX by remember { mutableFloatStateOf(0f) }
    var targetOffsetY by remember { mutableFloatStateOf(0f) }

    // Layout size
    var contentWidth by remember { mutableFloatStateOf(1f) }
    var contentHeight by remember { mutableFloatStateOf(1f) }

    // Animate toward target with spring
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

    // Progress (0..1) — used for scrim, scale, shadow
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

    // Derived visual values
    val scrimAlpha = progress * FieldMindMotion.swipeScrimAlpha
    val contentScale = 1f - progress * (1f - FieldMindMotion.swipeScaleFactor)
    val shadowElevation = progress * FieldMindMotion.swipeShadowElevationDp

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                contentWidth = coords.size.width.toFloat().coerceAtLeast(1f)
                contentHeight = coords.size.height.toFloat().coerceAtLeast(1f)
            }
    ) {
        // ── Previous screen preview (depth scrim + shadow gradient) ──
        if (progress > 0.01f) {
            // Gradient scrim — reveals a peek of the screen behind
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

        // ── Swipeable content ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val offsetX = animatedOffsetX.roundToInt()
                    val offsetY = animatedOffsetY.roundToInt()

                    translationX = offsetX.toFloat()
                    translationY = offsetY.toFloat()
                    scaleX = contentScale
                    scaleY = contentScale
                    shadowElevation = shadowElevation
                    transformOrigin = TransformOrigin(if (offsetX > 0) 0f else 0.5f, if (offsetY > 0) 0f else 0.5f)

                    // Clip to prevent content bleeding outside during scale
                    clip = true
                    shape = RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomEnd = if (offsetX > 0) (12f * progress).dp else 0.dp,
                        bottomStart = if (offsetX > 0) (12f * progress).dp else 0.dp
                    )
                }
                .then(
                    if (!reduceMotion) {
                        Modifier.pointerInput(Unit) {
                            // Use detectDragGestures for both horizontal and vertical
                            detectDragGestures(
                                onDragStart = { startPos ->
                                    // Determine direction based on where the drag starts
                                    val startXDp = startPos.x
                                    val startYDp = startPos.y
                                    if (startXDp <= FieldMindMotion.swipeEdgeWidthDp) {
                                        activeDirection = SwipeDirection.Horizontal
                                    } else if (startYDp <= FieldMindMotion.swipeEdgeHeightDp) {
                                        activeDirection = SwipeDirection.Vertical
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()

                                    when (activeDirection) {
                                        SwipeDirection.Horizontal -> {
                                            // Only allow rightward drag
                                            targetOffsetX = (targetOffsetX + dragAmount.x).coerceAtLeast(0f)
                                            targetOffsetY = 0f
                                        }
                                        SwipeDirection.Vertical -> {
                                            // Only allow downward drag
                                            targetOffsetY = (targetOffsetY + dragAmount.y).coerceAtLeast(0f)
                                            targetOffsetX = 0f
                                        }
                                        null -> {
                                            // Try to determine direction from the drag delta itself
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
                                        // Commit the back gesture — animate to full extent
                                        scope.launch {
                                            when (activeDirection) {
                                                SwipeDirection.Horizontal -> targetOffsetX = contentWidth
                                                SwipeDirection.Vertical -> targetOffsetY = contentHeight
                                                null -> {}
                                            }
                                            onBack()
                                        }
                                    } else {
                                        // Snap back to origin
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

            // ── Back arrow indicator — appears on left edge during horizontal swipe ──
            if (activeDirection == SwipeDirection.Horizontal && currentOffset > contentWidth * 0.05f) {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon = FieldMindIcons.Back,
                        contentDescription = "Swipe back",
                        size = 22.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Down arrow indicator — appears at top edge during vertical swipe ──
            if (activeDirection == SwipeDirection.Vertical && currentOffset > contentHeight * 0.05f) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .align(Alignment.TopCenter)
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon = FieldMindIcons.Back, // reuse back arrow for now (rotate later if desired)
                        contentDescription = "Swipe down to dismiss",
                        size = 22.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
