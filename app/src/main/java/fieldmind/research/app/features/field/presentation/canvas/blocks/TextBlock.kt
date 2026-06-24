package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Clean text block for the canvas.
 *
 * Design:
 * - No formatting toolbar — just a clean text editor
 * - Outline border when selected (instead of filled background)
 * - Auto-expands height as text grows
 * - Placeholder text when empty
 */
@Composable
fun TextBlock(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onHeightChanged: ((Float) -> Unit)? = null,
    onInsertBlock: ((String) -> Unit)? = null
) {
    var textFieldValue by remember(text) {
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }
    var measuredHeightPx by remember { mutableStateOf(0f) }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue
            onTextChange(newValue.text)
        },
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        val newHeight = coordinates.size.height.toFloat()
                        if (newHeight != measuredHeightPx && newHeight > 0f) {
                            measuredHeightPx = newHeight
                            onHeightChanged?.invoke(newHeight + 10f)
                        }
                    }
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            )
                        } else Modifier
                    )
                    .clip(RoundedCornerShape(6.dp))
                    .padding(12.dp)
                    .heightIn(min = with(LocalDensity.current) { 48.toDp() }),
                contentAlignment = Alignment.TopStart
            ) {
                if (text.isEmpty() && !isSelected) {
                    Text(
                        "Type here…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                innerTextField()
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}


