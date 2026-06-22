package fieldmind.research.app.features.field.presentation.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data model for the table block content.
 * Stored as JSON in [CanvasBlockEntity.contentJson].
 *
 * @param headers the column header labels
 * @param rows the data rows, each a list of cell values matching columns by index
 */
data class TableData(
    val headers: List<String> = listOf("Column 1", "Column 2", "Column 3"),
    val rows: List<List<String>> = listOf(
        listOf("", "", ""),
        listOf("", "", "")
    )
) {
    val columnCount: Int get() = headers.size

    /** Get the value at a specific cell. */
    fun getCell(rowIndex: Int, colIndex: Int): String {
        if (rowIndex < 0 || rowIndex >= rows.size) return ""
        val row = rows[rowIndex]
        return if (colIndex in row.indices) row[colIndex] else ""
    }

    /** Set the value at a specific cell. Returns a new [TableData] with the updated cell (immutable copy). */
    fun setCell(rowIndex: Int, colIndex: Int, value: String): TableData {
        val newRows = rows.toMutableList()
        if (rowIndex in newRows.indices && colIndex in newRows[rowIndex].indices) {
            val newRow = newRows[rowIndex].toMutableList()
            newRow[colIndex] = value
            newRows[rowIndex] = newRow
        }
        return copy(rows = newRows)
    }

    /** Add a new empty column at the end. */
    fun addColumn(): TableData = copy(
        headers = headers + "Column ${headers.size + 1}",
        rows = rows.map { it + "" }
    )

    /** Remove the last column if there are > 1 columns. */
    fun removeColumn(): TableData {
        if (columnCount <= 1) return this
        return copy(
            headers = headers.dropLast(1),
            rows = rows.map { it.dropLast(1) }
        )
    }

    /** Add a new empty row at the end. */
    fun addRow(): TableData = copy(
        rows = rows + List(columnCount) { "" }
    )

    /** Remove the last row if there are > 0 rows. */
    fun removeRow(): TableData {
        if (rows.size <= 1) return this
        return copy(rows = rows.dropLast(1))
    }

    /** Export as a Markdown table string. */
    fun toMarkdown(): String {
        if (headers.isEmpty()) return ""
        val sb = StringBuilder()

        // Header row
        sb.append("| ")
        sb.append(headers.joinToString(" | "))
        sb.append(" |\n")

        // Separator row
        sb.append("| ")
        sb.append(headers.joinToString(" | ") { "---" })
        sb.append(" |\n")

        // Data rows
        for (row in rows) {
            sb.append("| ")
            sb.append(row.joinToString(" | ") { it })
            sb.append(" |\n")
        }

        return sb.toString()
    }

    companion object {
        /** Parse from JSON string. Returns null on failure. */
        fun fromJson(jsonString: String): TableData? = try {
            val obj = JSONObject(jsonString)
            val headers: List<String> = obj.getJSONArray("headers").let { arr ->
                (0 until arr.length()).map { arr.optString(it, "") }
            }
            val rows: List<List<String>> = obj.getJSONArray("rows").let { arr ->
                (0 until arr.length()).map { rowIdx ->
                    val row = arr.getJSONArray(rowIdx)
                    (0 until row.length()).map { colIdx -> row.optString(colIdx, "") }
                }
            }
            TableData(headers = headers, rows = rows)
        } catch (_: Exception) { null }

        /** Serialize to JSON string. */
        fun toJson(data: TableData): String = try {
            val obj = JSONObject()
            obj.put("headers", JSONArray(data.headers))
            val rowsArray = JSONArray()
            for (row in data.rows) {
                rowsArray.put(JSONArray(row))
            }
            obj.put("rows", rowsArray)
            obj.toString()
        } catch (_: Exception) { "" }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  TableBlock Composable
// ══════════════════════════════════════════════════════════════════════

/**
 * Editable table block for the infinite canvas.
 *
 * Features:
 * - Grid of editable text cells
 * - Header row with bold styling and subtle background
 * - Add / remove rows and columns via toolbar
 * - Export as Markdown table (copied to clipboard)
 * - Horizontal scroll for wide tables
 * - Vertical scroll for long tables
 *
 * Table data is serialized as JSON in [CanvasBlockEntity.contentJson]
 * using the [TableData] data class.
 *
 * @param tableData the current table state
 * @param onTableChange called when the table data changes
 * @param isSelected whether the containing block is selected (shows toolbar)
 * @param modifier standard Compose modifier
 */
@Composable
fun TableBlock(
    tableData: TableData,
    onTableChange: (TableData) -> Unit = {},
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var showToolbar by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextRowIndex by remember { mutableIntStateOf(-1) }

    // ── Default minimum column width ──
    val minColWidth = 80.dp
    val cellVerticalPadding = 8.dp
    val cellHorizontalPadding = 10.dp

    Column(modifier = modifier.fillMaxSize()) {
        // ── Toolbar (visible when selected) ──
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TableToolbar(
                onAddRow = { onTableChange(tableData.addRow()) },
                onAddColumn = { onTableChange(tableData.addColumn()) },
                onDeleteRow = {
                    if (tableData.rows.size > 1) {
                        onTableChange(tableData.removeRow())
                    }
                },
                onDeleteColumn = {
                    if (tableData.columnCount > 1) {
                        onTableChange(tableData.removeColumn())
                    }
                },
                onExportMarkdown = {
                    clipboard.setText(AnnotatedString(tableData.toMarkdown()))
                }
            )
        }

        // ── Table grid ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
        ) {
            val verticalScroll = rememberScrollState()
            val horizontalScroll = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
                    .horizontalScroll(horizontalScroll)
            ) {
                // ── Header row ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
                ) {
                    tableData.headers.forEachIndexed { colIndex, header ->
                        TableHeaderCell(
                            value = header,
                            onValueChange = { newValue ->
                                val newHeaders = tableData.headers.toMutableList()
                                if (colIndex in newHeaders.indices) {
                                    newHeaders[colIndex] = newValue
                                    onTableChange(tableData.copy(headers = newHeaders))
                                }
                            },
                            minWidth = minColWidth,
                            isSelected = isSelected
                        )
                    }
                }

                // ── Data rows ──
                tableData.rows.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (rowIndex % 2 == 1) {
                                    Modifier.background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    )
                                } else Modifier
                            )
                            .clickable(enabled = isSelected) {
                                contextRowIndex = rowIndex
                                showContextMenu = true
                            }
                    ) {
                        row.forEachIndexed { colIndex, cellValue ->
                            TableCell(
                                value = cellValue,
                                onValueChange = { newValue ->
                                    onTableChange(tableData.setCell(rowIndex, colIndex, newValue))
                                },
                                minWidth = minColWidth
                            )
                        }
                    }
                }
            }

            // ── Empty state hint ──
            if (tableData.rows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Empty table",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // ── Context menu dialog ──
        if (showContextMenu && contextRowIndex >= 0) {
            val rowIdx = contextRowIndex
            AlertDialog(
                onDismissRequest = { showContextMenu = false },
                icon = { Icon(MaterialSymbolIcon("table_rows"), "Row options", size = 24.dp) },
                title = { Text("Row ${rowIdx + 1}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                onTableChange(tableData.addRow())
                                showContextMenu = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(MaterialSymbolIcon("playlist_add"), null, size = 18.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("Insert row below")
                        }
                        if (tableData.rows.size > 1) {
                            TextButton(
                                onClick = {
                                    val newRows = tableData.rows.toMutableList()
                                    if (rowIdx in newRows.indices) {
                                        newRows.removeAt(rowIdx)
                                        onTableChange(tableData.copy(rows = newRows))
                                    }
                                    showContextMenu = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(MaterialSymbolIcon("remove_circle"), null, size = 18.dp)
                                Spacer(Modifier.size(8.dp))
                                Text("Delete this row")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showContextMenu = false }) { Text("Done") }
                }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Table Toolbar
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun TableToolbar(
    onAddRow: () -> Unit,
    onAddColumn: () -> Unit,
    onDeleteRow: () -> Unit,
    onDeleteColumn: () -> Unit,
    onExportMarkdown: () -> Unit
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
            ToolbarButton(MaterialSymbolIcon("playlist_add"), "Add row", onAddRow)
            ToolbarButton(MaterialSymbolIcon("add_column"), "Add column", onAddColumn)
            TableToolbarDivider()
            ToolbarButton(MaterialSymbolIcon("remove_circle"), "Remove last row", onDeleteRow)
            ToolbarButton(MaterialSymbolIcon("remove_circle_outline"), "Remove last column", onDeleteColumn)
            TableToolbarDivider()
            ToolbarButton(MaterialSymbolIcon("content_copy"), "Copy as Markdown table", onExportMarkdown)
        }
    }
}

@Composable
private fun ToolbarButton(icon: MaterialSymbolIcon, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, label, size = 18.dp)
    }
}

@Composable
private fun TableToolbarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    )
}

// ══════════════════════════════════════════════════════════════════════
//  Header Cell
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun TableHeaderCell(
    value: String,
    onValueChange: (String) -> Unit,
    minWidth: Dp,
    isSelected: Boolean
) {
    val density = androidx.compose.ui.platform.LocalDensity.current

    Box(
        modifier = Modifier
            .widthIn(min = minWidth)
            .defaultMinSize(minHeight = 36.dp)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = cellHorizontalPadding, vertical = cellVerticalPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        if (isSelected) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                ),
                singleLine = true
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Data Cell
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun TableCell(
    value: String,
    onValueChange: (String) -> Unit,
    minWidth: Dp
) {
    Box(
        modifier = Modifier
            .widthIn(min = minWidth)
            .defaultMinSize(minHeight = 32.dp)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            .padding(horizontal = cellHorizontalPadding, vertical = cellVerticalPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            ),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                fontSize = 12.sp
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Defaults
// ══════════════════════════════════════════════════════════════════════

private val cellHorizontalPadding = 10.dp
private val cellVerticalPadding = 8.dp
