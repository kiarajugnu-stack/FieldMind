package fieldmind.research.app.features.field.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color

/**
 * iOS-style shimmer loading placeholder with an animated gradient sweep.
 *
 * Produces a shimmer effect that sweeps from left to right in a repeating loop,
 * mimicking the iOS skeleton loading pattern with a subtle polished look.
 */
object ShimmerConfig {
    /** Base shimmer color — uses onSurface with low alpha */
    val shimmerBase: Color get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    /** Highlight shimmer color — the sweeping highlight */
    val shimmerHighlight: Color get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    /** Duration of one full shimmer sweep in ms */
    const val shimmerDurationMs = 1400

    /** Corner radius for shimmer cards */
    val shapeCornerRadius = 16.dp
}

/**
 * Applies a shimmer animated gradient overlay to a component.
 * Use on any Box/modifier to give it a pulsing loading appearance.
 */
fun Modifier.shimmerEffect(): Modifier = this.then(
    androidx.compose.ui.draw.drawBehind {
        // Shimmer is drawn behind; works with clip() applied before this modifier
    }
)

/**
 * A shimmer placeholder that mimics a card shape.
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    height: Dp = 100.dp
) {
    val shimmerModifier = rememberShimmerModifier()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(ShimmerConfig.shapeCornerRadius))
            .then(shimmerModifier)
    )
}

/**
 * A shimmer placeholder row with a circular avatar on the left and two text lines on the right.
 */
@Composable
fun ShimmerRow(
    modifier: Modifier = Modifier,
    avatarSize: Dp = 40.dp,
    lineHeight: Dp = 12.dp
) {
    val shimmerMod = rememberShimmerModifier()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                    .then(shimmerMod)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(lineHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .then(shimmerMod)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(lineHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .then(shimmerMod)
            )
        }
    }
}

/**
 * A shimmer placeholder that mimics an EntityCard with icon, title, body, and meta chips.
 */
@Composable
fun ShimmerEntityCard(modifier: Modifier = Modifier) {
    val shimmerMod = rememberShimmerModifier()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon placeholder
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .then(shimmerMod)
            )
            Column(Modifier.weight(1f)) {
                // Title line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.height(8.dp))
                // Body line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .then(shimmerMod)
                )
                Spacer(Modifier.height(12.dp))
                // Meta chips row
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .then(shimmerMod)
                    )
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .then(shimmerMod)
                    )
                }
            }
            // Arrow icon placeholder
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .then(shimmerMod)
            )
        }
    }
}

/**
 * A shimmer feed: shows [count] shimmer cards with staggered appearance.
 * Use while real content is loading.
 */
@Composable
fun ShimmerFeed(
    count: Int = 5,
    modifier: Modifier = Modifier
) {
    val shimmerMod = rememberShimmerModifier()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) {
            ShimmerEntityCard()
        }
    }
}

/**
 * A shimmer header placeholder — icon, title, subtitle.
 */
@Composable
fun ShimmerHeader(modifier: Modifier = Modifier) {
    val shimmerMod = rememberShimmerModifier()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(shimmerMod)
            )
            Column(Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .then(shimmerMod)
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .then(shimmerMod)
                )
            }
        }
    }
}
