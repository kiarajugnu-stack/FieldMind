package fieldmind.research.app.features.field.presentation.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardElevation
import androidx.compose.ui.unit.dp

/**
 * A [Card] with built-in [expressiveCardPress] animation (lift + scale) and [onClick].
 *
 * This is the primary clickable card wrapper for FieldMind. Use it anywhere a card
 * should respond to tap with the signature iOS-style lift-and-scale feedback.
 *
 * Defaults mirror the project conventions:
 * - RoundedCornerShape(24.dp)
 * - surfaceContainerLow background
 * - zero elevation
 * - 1.5dp lift, 0.985 scale-down on press
 */
@Composable
fun ClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    liftDp: Float = 1.5f,
    scaleDown: Float = 0.985f,
    border: androidx.compose.foundation.BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) = Card(
    onClick = onClick,
    modifier = modifier
        .fillMaxWidth()
        .expressiveCardPress(liftDp = liftDp, scaleDown = scaleDown),
    shape = shape,
    colors = colors,
    elevation = elevation,
    border = border,
    content = content
)

/**
 * A non-clickable information card with the same visual style as [ClickableCard].
 * Use this when you need the same card look but without interactive behavior.
 */
@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border: androidx.compose.foundation.BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) = Card(
    modifier = modifier.fillMaxWidth(),
    shape = shape,
    colors = colors,
    elevation = elevation,
    border = border,
    content = content
)

/**
 * Overload that controls whether [fillMaxWidth] is applied.
 * Pass `false` for inline or weighted layouts where the parent manages width.
 */
@Composable
fun ClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillMaxWidth: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    liftDp: Float = 1.5f,
    scaleDown: Float = 0.985f,
    border: androidx.compose.foundation.BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val effectiveModifier = if (fillMaxWidth) modifier.fillMaxWidth() else modifier
    ClickableCard(
        onClick = onClick,
        modifier = effectiveModifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        liftDp = liftDp,
        scaleDown = scaleDown,
        border = border,
        content = content
    )
}
