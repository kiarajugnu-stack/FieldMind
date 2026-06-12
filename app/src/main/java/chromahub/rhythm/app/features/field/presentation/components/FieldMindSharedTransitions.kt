package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Shared element transition helpers for card-to-detail navigation.
 * Provides scale/alpha animations applied to elements during navigation.
 */
object FieldMindTransitions {

    /** Scale factor for an element during shared element transition. */
    @Composable
    fun sharedElementScale(
        isTransitioning: Boolean,
        initialScale: Float = 1f
    ): Float {
        val targetScale = if (isTransitioning) 0.96f else initialScale
        return animateFloatAsState(
            targetValue = targetScale,
            animationSpec = spring(dampingRatio = 0.85f),
            label = "sharedElementScale"
        ).value
    }

    /** Opacity for fading elements during navigation. */
    @Composable
    fun transitionAlpha(isTransitioning: Boolean): Float {
        val target = if (isTransitioning) 0.6f else 1f
        return animateFloatAsState(
            targetValue = target,
            animationSpec = tween(180),
            label = "transitionAlpha"
        ).value
    }
}

/**
 * A modifier extension to apply shared element visual hints.
 * Apply to entity cards to give them a subtle press animation.
 */
fun Modifier.sharedElementHint(isPressed: Boolean): Modifier = this
    .scale(
        scale = if (isPressed) 0.97f else 1f
    )
    .graphicsLayer {
        val targetAlpha = if (isPressed) 0.85f else 1f
        this.alpha = targetAlpha
    }
