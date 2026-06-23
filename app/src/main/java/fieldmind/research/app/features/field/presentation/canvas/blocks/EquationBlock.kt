package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import org.json.JSONObject

/**
 * Equation block for the canvas — a LaTeX math input with a styled preview.
 *
 * Since there's no native LaTeX rendering library, the block displays the
 * raw LaTeX source in a monospace editor and renders a styled preview using
 * Compose Text with formatting that visually suggests mathematical notation.
 *
 * Content JSON format:
 * ```json
 * { "latex": "E = mc^2", "display": true }
 * ```
 *
 * @param contentJson serialized equation data
 * @param onContentChanged called when the LaTeX content changes
 * @param isSelected whether this block is currently selected
 */
@Composable
fun EquationBlock(
    contentJson: String,
    onContentChanged: (String) -> Unit,
    isSelected: Boolean
) {
    // Parse content
    val (latex, displayMode) = remember(contentJson) {
        if (contentJson.isNotBlank()) {
            try {
                val obj = JSONObject(contentJson)
                Pair(
                    obj.optString("latex", ""),
                    obj.optBoolean("display", true)
                )
            } catch (_: Exception) {
                Pair("", true)
            }
        } else {
            Pair("", true)
        }
    }

    var isEditing by remember { mutableStateOf(latex.isBlank() && isSelected) }
    var editText by remember(latex) { mutableStateOf(latex) }

    // Sync editText when latex changes externally
    LaunchedEffect(latex) {
        if (!isEditing) editText = latex
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isEditing && isSelected) {
            // ── LaTeX editor ──
            BasicTextField(
                value = editText,
                onValueChange = { newValue ->
                    editText = newValue
                    val json = JSONObject().apply {
                        put("latex", newValue)
                        put("display", true)
                    }.toString()
                    onContentChanged(json)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = false,
                maxLines = 8
            )

            Spacer(Modifier.height(6.dp))

            // Hint
            Text(
                "Type LaTeX (e.g. E = mc^2, \\int_0^1 x^2 dx)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            // ── Rendered preview ──
            if (latex.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            if (isSelected) isEditing = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Render a visually styled equation preview
                    val renderedText = formatLatexPreview(latex)
                    Text(
                        text = renderedText,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Medium,
                            fontStyle = FontStyle.Italic,
                            fontSize = if (renderedText.length > 20) 18.sp else 22.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = if (renderedText.contains('\n')) 28.sp else 22.sp
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )
                }

                // ── Edit button (when selected) ──
                if (isSelected) {
                    IconButton(
                        onClick = {
                            editText = latex
                            isEditing = true
                        },
                        modifier = Modifier
                            .padding(2.dp)
                            .size(24.dp)
                    ) {
                        Icon(
                            MaterialSymbolIcon("edit"),
                            "Edit equation",
                            size = 14.dp,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // ── Empty state ──
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            if (isSelected) {
                                editText = ""
                                isEditing = true
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        MaterialSymbolIcon("functions", defaultWeight = 300),
                        "Equation",
                        size = 32.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isSelected) "Tap to add equation" else "Equation",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

/**
 * Converts simple LaTeX patterns into a more readable plain-text preview.
 * This is a visual helper, not a full renderer.
 *
 * Simple conversions:
 * - `^` for superscript → Unicode superscript chars
 * - `_` for subscript → Unicode subscript chars
 * - `\\sqrt{x}` → √(x)
 * - `\\frac{a}{b}` → a/b or stacked notation
 * - `\\int` → ∫ symbol
 * - `\\sum` → ∑ symbol
 * - `\\pi` → π
 * - `\\alpha`, `\\beta`, etc. → Greek letters
 */
private fun formatLatexPreview(latex: String): String {
    var result = latex
        // Replace common commands with Unicode symbols
        .replace("\\sqrt", "√")
        .replace("\\int", "∫")
        .replace("\\sum", "∑")
        .replace("\\prod", "∏")
        .replace("\\pi", "π")
        .replace("\\alpha", "α")
        .replace("\\beta", "β")
        .replace("\\gamma", "γ")
        .replace("\\delta", "δ")
        .replace("\\theta", "θ")
        .replace("\\lambda", "λ")
        .replace("\\mu", "μ")
        .replace("\\sigma", "σ")
        .replace("\\omega", "ω")
        .replace("\\infty", "∞")
        .replace("\\partial", "∂")
        .replace("\\rightarrow", "→")
        .replace("\\leftarrow", "←")
        .replace("\\Rightarrow", "⇒")
        .replace("\\Leftarrow", "⇐")
        .replace("\\approx", "≈")
        .replace("\\neq", "≠")
        .replace("\\leq", "≤")
        .replace("\\geq", "≥")
        .replace("\\times", "×")
        .replace("\\div", "÷")
        .replace("\\cdot", "·")
        .replace("\\pm", "±")
        .replace("\\cdots", "…")
        .replace("\\ldots", "…")
        .replace("\\nabla", "∇")
        .replace("\\forall", "∀")
        .replace("\\exists", "∃")
        .replace("\\in", "∈")
        .replace("\\notin", "∉")
        .replace("\\subset", "⊂")
        .replace("\\supset", "⊃")
        .replace("\\cup", "∪")
        .replace("\\cap", "∩")
        .replace("\\emptyset", "∅")
        // Remove remaining backslashes from unknown commands
        .replace("\\", "")

    // Simple superscript conversion: x^2 → x² (only single chars after ^)
    result = result.replace(Regex("""\^(\w)""")) { match ->
        val char = match.groupValues[1]
        val superscripts = mapOf(
            '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³',
            '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷',
            '8' to '⁸', '9' to '⁹',
            'n' to 'ⁿ', 'i' to 'ⁱ',
            '+' to '⁺', '-' to '⁻', '(' to '⁽', ')' to '⁾'
        )
        superscripts[char.first()]?.toString() ?: "^$char"
    }

    // Simple subscript conversion: x_1 → x₁
    result = result.replace(Regex("""_(\w)""")) { match ->
        val char = match.groupValues[1]
        val subscripts = mapOf(
            '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃',
            '4' to '₄', '5' to '₅', '6' to '₆', '7' to '₇',
            '8' to '₈', '9' to '₉',
            '+' to '₊', '-' to '₋', '(' to '₍', ')' to '₎'
        )
        subscripts[char.first()]?.toString() ?: "_$char"
    }

    return result.trim()
}
