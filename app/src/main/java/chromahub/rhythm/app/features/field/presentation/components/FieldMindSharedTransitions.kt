package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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

    // ── Shared Element Animations ──

    /**
     * Animated scale for a shared element during navigation.
     * Apply this to the source element (card) and target element (detail header).
     */
    @Composable
    fun sharedElementScale(
        isTransitioning: Boolean,
        initialScale: Float = 1f
    ): Float {
        val targetScale = if (isTransitioning) 0.96f else initialScale
        return animateFloatAsState(
            targetValue = targetScale,
            animationSpec = sharedElementSpring,
            label = "sharedElementScale"
        ).value
    }

    /** Opacity for fading elements during navigation. */
    @Composable
    fun transitionAlpha(isTransitioning: Boolean): Float {
        return animateFloatAsState(
            targetValue = if (isTransitioning) 0.6f else 1f,
            animationSpec = tween(180),
            label = "transitionAlpha"
        ).value
    }

    /**
     * Container transform: animates bounds (size) between two states.
     * Use with Modifier.clip() for morphing shape effect.
     */
    @Composable
    fun containerBounds(
        isExpanded: Boolean,
        expandedSize: Dp,
        collapsedSize: Dp
    ): Dp {
        return animateDpAsState(
            targetValue = if (isExpanded) expandedSize else collapsedSize,
            animationSpec = spring(
                dampingRatio = if (isExpanded) 0.65f else 0.8f,
                stiffness = 500f
            ),
            label = "containerBounds"
        ).value
    }

    // ── Standardized Content Transitions ──

    /**
     * Fade-through transition: brief cross-fade between content states.
     * Use for non-sequential state changes (tabs, filters, views).
     */
    val fadeThrough: ContentTransform = fadeIn(
        animationSpec = tween(250)
    ) togetherWith fadeOut(
        animationSpec = tween(200)
    )

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
     * Shared axis (vertical) for entering items — slides up with fade.
     */
    val itemEnter: ContentTransform = slideInVertically(
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)
    ) { it / 4 } + fadeIn(
        animationSpec = tween(250)
    ) togetherWith fadeOut(
        animationSpec = tween(200)
    )

    /**
     * Animated content wrapper that animates transitions between states.
     * Use with key(state) to animate content changes.
     *
     * Example:
     * ```kotlin
     * FieldMindTransitions.AnimatedContent(targetState = tab) { currentTab ->
     *     when (currentTab) { ... }
     * }
     * ```
     */
    @Composable
    fun AnimatedContent(
        targetState: Any?,
        transition: ContentTransform = fadeThrough,
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
