package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset

/**
 * Shared element transition helpers for card-to-detail navigation,
 * container transforms, and animated content transitions.
 *
 * Follows M3 expressive motion patterns:
 * - Container transforms: elements morph from card → detail
 * - Fade-through: content fades between states with a brief cross-fade
 * - Shared axis: content slides along a shared horizontal/vertical axis
 */
object FieldMindTransitions {

    // ── Spring Specs ──

    /** Spring for shared element transitions (card → detail). */
    val sharedElementSpring = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 600f
    )

    /** Spring for container transform bounds animation. */
    val containerTransformSpring = spring<Float>(
        dampingRatio = 0.65f,
        stiffness = 500f
    )

    /** Spring for axis slide transitions. */
    val axisSlideSpring = spring<Float>(
        dampingRatio = 0.75f,
        stiffness = 700f
    )

    // ── Screen Transition Specs ──

    /**
     * Fade-through transition: brief cross-fade between content states.
     * Use for non-sequential state changes (settings hub → settings page).
     * @param durationMs Fade duration; default 250ms
     */
    fun fadeThrough(durationMs: Int = 250): ContentTransform =
        fadeIn(animationSpec = tween(durationMs, easing = FastOutSlowInEasing)) togetherWith
        fadeOut(animationSpec = tween(durationMs, easing = FastOutSlowInEasing))

    /**
     * Scale-in + fade for modal-style screens (dialogs, overlays).
     * Content enters from slightly smaller and fades up.
     */
    val scaleFade: ContentTransform =
        scaleIn(initialScale = 0.92f, animationSpec = tween(250, easing = FastOutSlowInEasing)) +
        fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) togetherWith
        scaleOut(targetScale = 0.95f, animationSpec = tween(180)) +
        fadeOut(animationSpec = tween(150))

    /**
     * Shared axis (horizontal): slides from the right on forward nav,
     * from the left on back nav. Use for list→detail navigation.
     * @param slideFraction How much of the screen width to slide (default 1/4)
     */
    fun sharedAxisHorizontal(slideFraction: Float = 0.25f): ContentTransform {
        val slideSpec = tween<IntOffset>(350, easing = FastOutSlowInEasing)
        val fadeSpec = tween<Float>(200, easing = FastOutSlowInEasing)
        return slideInHorizontally(animationSpec = slideSpec) { (it * slideFraction).toInt() } +
            fadeIn(animationSpec = fadeSpec) togetherWith
            slideOutHorizontally(animationSpec = slideSpec) { -(it * slideFraction * 0.8f).toInt() } +
            fadeOut(animationSpec = fadeSpec)
    }

    /**
     * Shared axis (vertical): content slides up/down along shared vertical axis.
     * Use for sequential content changes (expand/collapse, steps).
     */
    val sharedAxisVertical: ContentTransform = slideInVertically(
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)
    ) { it / 6 } + fadeIn(
        animationSpec = tween(200)
    ) togetherWith slideOutVertically(
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 800f)
    ) { -it / 8 } + fadeOut(
        animationSpec = tween(150)
    )

    /**
     * Fade-through + slight scale for hub→page transitions.
     * New content fades in while old fades out, with a quick cross-fade moment.
     */
    val fadeThroughScale: ContentTransform =
        scaleIn(initialScale = 0.97f, animationSpec = tween(250, easing = FastOutSlowInEasing)) +
        fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)) togetherWith
        scaleOut(targetScale = 1.03f, animationSpec = tween(180)) +
        fadeOut(animationSpec = tween(180))

    /**
     * List item entrance: slide up + fade in with configurable offset.
     * @param offsetFraction vertical slide distance as fraction of height
     */
    fun itemEnter(offsetFraction: Int = 4): ContentTransform = slideInVertically(
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)
    ) { it / offsetFraction } + fadeIn(
        animationSpec = tween(250)
    ) togetherWith fadeOut(
        animationSpec = tween(200)
    )

    /**
     * Default crossfade for inline AnimatedContent.
     */
    val fadeThroughInline: ContentTransform =
        fadeIn(animationSpec = tween(250)) togetherWith
        fadeOut(animationSpec = tween(200))

    /**
     * Animated content wrapper that animates transitions between states.
     * Use with key(state) to animate content changes.
     */
    @Composable
    fun AnimatedContent(
        targetState: Any?,
        transition: ContentTransform = fadeThroughInline,
        content: @Composable () -> Unit
    ) {
        key(targetState) {
            androidx.compose.animation.AnimatedContent(
                targetState = targetState,
                transitionSpec = { transition },
                label = "fieldmindTransition"
            ) { content() }
        }
    }
}

/**
 * A modifier extension to apply shared element visual hints.
 * Apply to entity cards to give them a subtle press animation
 * with expressive spring bounce.
 */
fun Modifier.sharedElementHint(isPressed: Boolean): Modifier = this
    .scale(
        scale = if (isPressed) 0.97f else 1f
    )
    .graphicsLayer {
        val targetAlpha = if (isPressed) 0.85f else 1f
        this.alpha = targetAlpha
    }
