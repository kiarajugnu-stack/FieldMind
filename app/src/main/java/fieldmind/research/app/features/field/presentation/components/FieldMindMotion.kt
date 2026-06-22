package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import fieldmind.research.app.shared.presentation.components.icons.Icon
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
//  Swipe-back Gesture Host
// ──────────────────────────────────────────────────────────────────────

/**
 * Wraps content with a horizontal swipe-back gesture. Users can swipe
 * from the left edge to navigate back. The content follows the finger
 * with spring physics, snapping back if released before the threshold or
 * calling [onBack] if released past the threshold.
 *
 * Uses [detectHorizontalDragGestures] for gesture detection with
 * [animateFloatAsState] for smooth spring-back animation.
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

    var offsetX by remember { mutableFloatStateOf(0f) }
    var contentWidth by remember { mutableFloatStateOf(0f) }

    // Animate offset with spring when released
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = FieldMindMotion.swipeBackSpring,
        label = "swipeOffset",
        finishedListener = {
            // Reset to 0 after completion (happens when snapped back or released)
        }
    )

    // Scrim alpha — darkens the content behind as swipe progresses
    val scrimAlpha = if (contentWidth > 0f && offsetX > 0f) {
        (offsetX / contentWidth).coerceIn(0f, 0.35f)
    } else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                contentWidth = coordinates.size.width.toFloat()
            }
    ) {
        // Scrim overlay — shown behind the content as it slides
        if (scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
            )
        }

        // Swipeable content with drag gesture
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(x = animatedOffsetX.roundToInt(), y = 0) }
                .then(
                    if (!reduceMotion) {
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    // If past 30% threshold, trigger back navigation
                                    if (offsetX > contentWidth * 0.30f) {
                                        scope.launch {
                                            // Animate to full width then call back
                                            offsetX = contentWidth
                                            onBack()
                                        }
                                    } else {
                                        // Snap back
                                        offsetX = 0f
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    offsetX = (offsetX + dragAmount).coerceAtLeast(0f)
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

            // Back indicator — appears on left edge during drag
            if (offsetX > contentWidth * 0.05f) {
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
        }
    }
}
