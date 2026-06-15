package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * Standardized motion specifications for FieldMind.
 * Use these instead of ad-hoc animation specs for consistency.
 */
object FieldMindMotion {
    /** Bouncy spring for selection/press feedback. */
    val pressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Gentle spring for layout changes. */
    val layoutSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    /** Standard fade duration. */
    const val fadeMs = 200

    /** Standard scale-press duration. */
    const val pressScaleMs = 120

    /** Number roll-up duration. */
    const val countUpMs = 600

    /** Spring for save confirmations. */
    val confirmSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    /** Tween for fades. */
    val fadeTween = tween<Float>(durationMillis = fadeMs)

    /** Tween for scale presses. */
    val pressScaleTween = tween<Float>(durationMillis = pressScaleMs)

    /** Check if reduce-motion is enabled. */
    @Composable
    fun isReduceMotion(): Boolean {
        // In inspection mode (preview), reduce motion is always off
        if (LocalInspectionMode.current) return false
        // Check system-level animation scale
        val animatorScale = try {
            android.provider.Settings.Global.getFloat(
                androidx.compose.ui.platform.LocalContext.current.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
            )
        } catch (_: Exception) { 1f }
        return animatorScale == 0f
    }
}

/**
 * A modifier that adds press scale animation feedback.
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

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = target,
        animationSpec = FieldMindMotion.pressSpring,
        label = "pressScale"
    )

    // Pointer input for press detection, chained with scale animation
    this.pointerInput(enabled) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                isPressed = event.changes.any { it.pressed }
            }
        }
    }.scale(scale)
}

/**
 * Modifier that applies press scale feedback + clips to shape.
 * Use on all interactive cards and buttons for consistent feel.
 */
fun Modifier.pressCardScale(): Modifier = composed {
    this.pressScale(scaleDown = 0.97f)
}
