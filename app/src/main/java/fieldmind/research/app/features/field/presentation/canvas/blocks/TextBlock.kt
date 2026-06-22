package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import fieldmind.research.app.features.field.presentation.components.rememberFieldMindHaptics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TransformedText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

/**
 * Markdown text block for the infinite canvas.
 *
 * Features:
 * - **Bold**, *Italic*, ~~Strikethrough~~, `Code`, ## Headings, - Lists, [Links](url)
 * - Formatting toolbar: toggles inline Markdown syntax on selected text
 * - Auto-expand height as text grows (measured via onGloballyPositioned)
 * - `/` command menu at line start for quick-insert of other block types
 * - Syntax-highlighted display via [MarkdownVisualTransformation]
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
    // ── Editor state ──
    var textFieldValue by remember(text) {
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }
    var showCommandMenu by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    val haptics = rememberFieldMindHaptics()

    // Track measured height for auto-expand
    var measuredHeightPx by remember { mutableStateOf(0f) }

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Formatting toolbar (visible when selected) ──
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FormattingToolbar(
                onBold = { wrapSelection(textFieldValue, "**", onTextChange) },
                onItalic = { wrapSelection(textFieldValue, "*", onTextChange) },
                onStrikethrough = { wrapSelection(textFieldValue, "~~", onTextChange) },
                onCode = { wrapSelection(textFieldValue, "`", onTextChange) },
                onHeading = { level -> insertAtStartOfLine(textFieldValue, "#".repeat(level) + " ", onTextChange) },
                onBulletList = { insertAtStartOfLine(textFieldValue, "- ", onTextChange) },
                onNumberedList = { insertAtStartOfLine(textFieldValue, "1. ", onTextChange) },
                onLink = { showLinkDialog = true },
                onCommand = {
                    haptics.light()
                    showCommandMenu = !showCommandMenu
                }
            )
        }

        // ── `/` command menu ──
        AnimatedVisibility(
            visible = showCommandMenu,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CommandMenu(
                onSelect = { type ->
                    showCommandMenu = false
                    onInsertBlock?.invoke(type)
                },
                onDismiss = { showCommandMenu = false }
            )
        }

        // ── Link dialog ──
        if (showLinkDialog) {
            LinkInsertDialog(
                onInsert = { linkText, url ->
                    insertLink(textFieldValue, linkText, url, onTextChange)
                    showLinkDialog = false
                },
                onDismiss = { showLinkDialog = false }
            )
        }

        // ── Text editor with auto-height measurement ──
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onTextChange(newValue.text)

                // Show command menu when `/` is typed at the start of a new line
                val text = newValue.text
                if (text.endsWith("/") && text.length >= 1) {
                    val lineStart = text.lastIndexOf('\n', text.length - 2) + 1
                    val lineContent = text.substring(lineStart)
                    // Only trigger if `/` is the first non-whitespace char on the line
                    if (lineContent.trimStart() == "/") {
                        showCommandMenu = true
                    }
                }
            },
            visualTransformation = MarkdownVisualTransformation(),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    val newHeight = coordinates.size.height.toFloat()
                    if (newHeight != measuredHeightPx && newHeight > 0f) {
                        measuredHeightPx = newHeight
                        // Report content height + toolbar padding back to parent
                        onHeightChanged?.invoke(newHeight + 10f) // 10px buffer
                    }
                }
                .heightIn(min = with(LocalDensity.current) { 60.toDp() })
                .padding(12.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Markdown VisualTransformation
// ══════════════════════════════════════════════════════════════════════

/**
 * Applies visual styling to Markdown syntax while keeping the raw text editable.
 *
 * Supported syntax:
 * - `**bold**` → Bold
 * - `*italic*` → Italic
 * - `~~strikethrough~~` → Strikethrough
 * - `` `code` `` → Monospace code
 * - `# H1`, `## H2`, `### H3` → Heading styles
 * - `- item` or `* item` → Bullet list (first line char style)
 * - `[text](url)` → Link style
 */
private class MarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString):
            androidx.compose.ui.text.TransformedText {

        val raw = text.text
        val styles = mutableListOf<Pair<IntRange, SpanStyle>>()

        // **bold**
        applyPairStyle(raw, "**", "**",
            SpanStyle(fontWeight = FontWeight.Bold)) { styles.add(it) }

        // *italic* (single *, not part of **)
        parseItalic(raw) { styles.add(it) }

        // ~~strikethrough~~
        applyPairStyle(raw, "~~", "~~",
            SpanStyle(textDecoration = TextDecoration.LineThrough)) { styles.add(it) }

        // `code`
        applyPairStyle(raw, "`", "`",
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = Color.Black.copy(alpha = 0.13f)
            )) { styles.add(it) }

        // # Headings — style the content after the prefix
        parseHeadings(raw) { styles.add(it) }

        // Build final annotated string with all styles applied
        val result = buildAnnotatedString {
            append(raw)
            styles.forEach { (range, span) ->
                val start = range.first.coerceIn(0, raw.length)
                val end = range.last.coerceIn(start, raw.length - 1) + 1
                if (start < end) {
                    addStyle(span, start, end)
                }
            }
        }

        return androidx.compose.ui.text.TransformedText(result,
            OffsetMapping.Identity)
    }

    /** Find all `open...close` pairs and style the content between them. */
    private fun applyPairStyle(
        text: String, open: String, close: String,
        style: SpanStyle,
        onMatch: (Pair<IntRange, SpanStyle>) -> Unit
    ) {
        var searchFrom = 0
        while (true) {
            val start = text.indexOf(open, searchFrom)
            if (start < 0) break
            val end = text.indexOf(close, start + open.length)
            if (end < 0) break
            val contentStart = start + open.length
            val contentEnd = end - 1
            if (contentStart <= contentEnd) {
                onMatch(contentStart..contentEnd to style)
            }
            searchFrom = end + close.length
        }
    }

    /** Parse single `*italic*` markers (not `**`). */
    private fun parseItalic(text: String, onMatch: (Pair<IntRange, SpanStyle>) -> Unit) {
        var i = 0
        while (i < text.length) {
            if (text[i] == '*' && i + 1 < text.length && text[i + 1] != '*') {
                val close = text.indexOf('*', i + 1)
                if (close > i && (close + 1 >= text.length || text[close + 1] != '*')) {
                    onMatch(i + 1..<close to SpanStyle(fontStyle = FontStyle.Italic))
                    i = close + 1
                    continue
                }
            }
            // Skip known ** pairs
            if (text[i] == '*' && i + 1 < text.length && text[i + 1] == '*') {
                val close = text.indexOf("**", i + 2)
                if (close > i) { i = close + 2; continue }
            }
            i++
        }
    }

    /** Parse `# `, `## `, `### ` heading prefixes and style the line content. */
    private fun parseHeadings(text: String, onMatch: (Pair<IntRange, SpanStyle>) -> Unit) {
        val lines = text.split("\n")
        var offset = 0
        for (line in lines) {
            val trimmed = line.trimStart()
            val leadingSpaces = line.length - trimmed.length
            when {
                trimmed.startsWith("### ") -> {
                    val contentStart = offset + leadingSpaces + 4 // skip "### "
                    val lineEnd = offset + line.length
                    if (contentStart < lineEnd) {
                        onMatch(contentStart..<lineEnd to
                            SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp))
                    }
                }
                trimmed.startsWith("## ") -> {
                    val contentStart = offset + leadingSpaces + 3 // skip "## "
                    val lineEnd = offset + line.length
                    if (contentStart < lineEnd) {
                        onMatch(contentStart..<lineEnd to
                            SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp))
                    }
                }
                trimmed.startsWith("# ") -> {
                    val contentStart = offset + leadingSpaces + 2 // skip "# "
                    val lineEnd = offset + line.length
                    if (contentStart < lineEnd) {
                        onMatch(contentStart..<lineEnd to
                            SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp))
                    }
                }
            }
            offset += line.length + 1 // +1 for newline
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Formatting Toolbar
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun FormattingToolbar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onStrikethrough: () -> Unit,
    onCode: () -> Unit,
    onHeading: (Int) -> Unit,
    onBulletList: () -> Unit,
    onNumberedList: () -> Unit,
    onLink: () -> Unit,
    onCommand: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarButton(MaterialSymbolIcon("format_bold"), "Bold", onBold)
            ToolbarButton(MaterialSymbolIcon("format_italic"), "Italic", onItalic)
            ToolbarButton(MaterialSymbolIcon("format_strikethrough"), "Strikethrough", onStrikethrough)
            ToolbarDivider()
            ToolbarButton(MaterialSymbolIcon("code"), "Inline code", onCode)
            ToolbarButton(MaterialSymbolIcon("looks_one"), "Heading 1") { onHeading(1) }
            ToolbarButton(MaterialSymbolIcon("looks_two"), "Heading 2") { onHeading(2) }
            ToolbarButton(MaterialSymbolIcon("looks_3"), "Heading 3") { onHeading(3) }
            ToolbarDivider()
            ToolbarButton(MaterialSymbolIcon("format_list_bulleted"), "Bullet list", onBulletList)
            ToolbarButton(MaterialSymbolIcon("format_list_numbered"), "Numbered list", onNumberedList)
            ToolbarButton(MaterialSymbolIcon("link"), "Insert link", onLink)
            ToolbarDivider()
            ToolbarButton(MaterialSymbolIcon("add"), "Insert block (/)", onCommand)
        }
    }
}

@Composable
private fun ToolbarButton(icon: MaterialSymbolIcon, contentDescription: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(icon, contentDescription, size = 18.dp)
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    )
}

// ══════════════════════════════════════════════════════════════════════
//  / Command Menu
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun CommandMenu(
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val commands = listOf(
        "image" to "Image",
        "table" to "Table",
        "figure" to "Figure",
        "equation" to "Equation",
        "drawing" to "Drawing",
        "sticky" to "Sticky Note",
        "voice" to "Voice Note",
        "pdf" to "PDF"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Text(
                "Insert block",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            commands.forEach { (type, label) ->
                TextButton(
                    onClick = { onSelect(type) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Link Insert Dialog
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun LinkInsertDialog(
    onInsert: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var linkText by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(MaterialSymbolIcon("link"), "Link", size = 24.dp) },
        title = { Text("Insert link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = linkText,
                    onValueChange = { linkText = it },
                    label = { Text("Display text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onInsert(linkText, url) },
                shape = RoundedCornerShape(12.dp),
                enabled = linkText.isNotBlank() && url.isNotBlank()
            ) {
                Text("Insert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ══════════════════════════════════════════════════════════════════════
//  Markdown helper functions
// ══════════════════════════════════════════════════════════════════════

/**
 * Wraps selected text with [marker] on both sides (e.g., "**" around selection).
 * If no text is selected, inserts two markers and places cursor between them.
 */
private fun wrapSelection(field: TextFieldValue, marker: String, onTextChange: (String) -> Unit) {
    val text = field.text
    val start = field.selection.start
    val end = field.selection.end

    if (start == end) {
        val newText = text.substring(0, start) + marker + marker + text.substring(start)
        onTextChange(newText)
    } else {
        val selected = text.substring(start, end)
        val newText = text.substring(0, start) + marker + selected + marker + text.substring(end)
        onTextChange(newText)
    }
}

/**
 * Inserts [prefix] at the start of the current line (for headings, lists).
 */
private fun insertAtStartOfLine(field: TextFieldValue, prefix: String, onTextChange: (String) -> Unit) {
    val text = field.text
    val cursor = field.selection.start
    val lineStart = if (cursor <= 0) 0 else text.lastIndexOf('\n', cursor - 1) + 1
    val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
    onTextChange(newText)
}

/**
 * Inserts a Markdown link `[text](url)` at cursor position.
 */
private fun insertLink(field: TextFieldValue, linkText: String, url: String, onTextChange: (String) -> Unit) {
    val text = field.text
    val cursor = field.selection.start
    val markdown = "[$linkText]($url)"
    val newText = text.substring(0, cursor) + markdown + text.substring(cursor)
    onTextChange(newText)
}
