package fieldmind.research.app.features.field.presentation.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.features.field.data.database.entity.DataRecordEntity
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import java.io.File

/**
 * Interactive Data Table for structured data records.
 * Features: column sorting, filtering, search, aggregate functions, pivot, CSV export.
 */
data class DataColumn(
    val key: String,
    val label: String,
    val align: TextAlign = TextAlign.Start,
    val width: Float = 1f, // weight fraction
    val isNumeric: Boolean = false
)

data class DataRow(
    val id: Long,
    val cells: Map<String, String>
)

/**
 * Main interactive data table composable.
 *
 * @param columns Column definitions
 * @param rows Data rows
 * @param onEditRow Called when user taps edit on a row
 * @param onDeleteRow Called when user deletes a row
 * @param onAddRow Called when user taps add
 * @param enableExport Show export button
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FieldDataTable(
    columns: List<DataColumn>,
    rows: List<DataRow>,
    modifier: Modifier = Modifier,
    title: String = "Data Records",
    onEditRow: ((Long) -> Unit)? = null,
    onDeleteRow: ((Long) -> Unit)? = null,
    onAddRow: (() -> Unit)? = null,
    enableExport: Boolean = true,
    enablePivot: Boolean = true,
    emptyMessage: String = "No data records yet. Add measurements, counts, or observations.",
    accentColor: androidx.compose.ui.graphics.Color = FieldMindTheme.colors.data
) {
    val context = LocalContext.current
    var sortColumn by remember { mutableStateOf(-1) }
    var sortAscending by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterColumn by remember { mutableStateOf(-1) }
    var filterValue by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var showAggregates by remember { mutableStateOf(false) }
    var showPivot by remember { mutableStateOf(false) }
    var pivotColumn by remember { mutableStateOf(0) }
    var pivotAggregate by remember { mutableStateOf(0) } // 0=count, 1=sum, 2=average
    var selectedRows by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showSearch by remember { mutableStateOf(false) }

    // Filter rows
    val filteredRows = remember(rows, searchQuery, filterColumn, filterValue) {
        var result = rows
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            result = result.filter { row ->
                row.cells.any { (_, value) -> value.lowercase().contains(q) }
            }
        }
        if (filterColumn >= 0 && filterValue.isNotBlank()) {
            val colKey = columns.getOrNull(filterColumn)?.key ?: return@remember result
            result = result.filter { row ->
                row.cells[colKey]?.equals(filterValue, ignoreCase = true) == true
            }
        }
        result
    }

    // Sort rows
    val sortedRows = remember(filteredRows, sortColumn, sortAscending) {
        if (sortColumn < 0 || sortColumn >= columns.size) return@remember filteredRows
        val col = columns[sortColumn]
        val sorted = if (col.isNumeric) {
            filteredRows.sortedWith { left: DataRow, right: DataRow ->
                val leftValue = left.cells[col.key]?.toDoubleOrNull() ?: Double.MAX_VALUE
                val rightValue = right.cells[col.key]?.toDoubleOrNull() ?: Double.MAX_VALUE
                leftValue.compareTo(rightValue)
            }
        } else {
            filteredRows.sortedWith { left: DataRow, right: DataRow ->
                val leftValue = (left.cells[col.key] ?: "").lowercase()
                val rightValue = (right.cells[col.key] ?: "").lowercase()
                leftValue.compareTo(rightValue)
            }
        }
        if (sortAscending) sorted else sorted.reversed()
    }

    // Unique values for filter dropdown
    val uniqueValues = remember(columns, rows, filterColumn) {
        if (filterColumn < 0) emptyList()
        else columns.getOrNull(filterColumn)?.let { col ->
            rows.map { it.cells[col.key] ?: "" }.filter { it.isNotBlank() }.distinct().sorted()
        } ?: emptyList()
    }

    // Aggregates
    val aggregates = remember(sortedRows, columns) {
        columns.filter { it.isNumeric }.map { col ->
            val values = sortedRows.mapNotNull { it.cells[col.key]?.toDoubleOrNull() }
            col.label to Triple(
                if (values.isEmpty()) 0.0 else values.size.toDouble(),
                values.sum(),
                if (values.isEmpty()) 0.0 else values.average()
            )
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // ── Header ──
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(icon = MaterialSymbolIcon("table_rows"), contentDescription = null, tint = accentColor, size = 18.dp) }
                    Column {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${sortedRows.size} records", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showSearch = !showSearch }, modifier = Modifier.size(32.dp)) {
                        Icon(icon = MaterialSymbolIcon("search"), contentDescription = "Search", size = 18.dp)
                    }
                    if (enableExport) {
                        IconButton(onClick = { exportToCsv(context, columns, sortedRows) }, modifier = Modifier.size(32.dp)) {
                            Icon(icon = MaterialSymbolIcon("file_download"), contentDescription = "Export CSV", size = 18.dp)
                        }
                    }
                    if (onAddRow != null) {
                        IconButton(onClick = onAddRow, modifier = Modifier.size(32.dp)) {
                            Icon(icon = MaterialSymbolIcon("add"), contentDescription = "Add row", size = 18.dp, tint = accentColor)
                        }
                    }
                }
            }

            // ── Search bar ──
            AnimatedVisibility(showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search all columns...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = { if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = "" }) { Icon(icon = MaterialSymbolIcon("close"), contentDescription = "Clear", size = 18.dp) } }
                )
            }

            // ── Toolbar ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = showFilters,
                    onClick = { showFilters = !showFilters },
                    label = { Text("Filter", fontSize = 11.sp) },
                    leadingIcon = { Icon(icon = MaterialSymbolIcon("filter_list"), contentDescription = null, size = 14.dp) }
                )
                FilterChip(
                    selected = showAggregates,
                    onClick = { showAggregates = !showAggregates },
                    label = { Text("Aggregates", fontSize = 11.sp) },
                    leadingIcon = { Icon(icon = MaterialSymbolIcon("functions"), contentDescription = null, size = 14.dp) }
                )
                if (enablePivot) {
                    FilterChip(
                        selected = showPivot,
                        onClick = { showPivot = !showPivot },
                        label = { Text("Pivot", fontSize = 11.sp) },
                        leadingIcon = { Icon(icon = MaterialSymbolIcon("pivot_table_chart"), contentDescription = null, size = 14.dp) }
                    )
                }
                if (selectedRows.isNotEmpty()) {
                    Text("${selectedRows.size} selected", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterVertically))
                }
            }

            // ── Filter panel ──
            AnimatedVisibility(showFilters) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Filter by column", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Column selector
                            columns.forEachIndexed { i, col ->
                                FilterChip(
                                    selected = filterColumn == i,
                                    onClick = { filterColumn = if (filterColumn == i) -1 else i; filterValue = "" },
                                    label = { Text(col.label, fontSize = 9.sp) }
                                )
                            }
                        }
                        if (filterColumn >= 0 && uniqueValues.isNotEmpty()) {
                            Text("Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            // Show first 6 unique values as quick chips
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                uniqueValues.take(8).forEach { value ->
                                    FilterChip(
                                        selected = filterValue == value,
                                        onClick = { filterValue = if (filterValue == value) "" else value },
                                        label = { Text(value, fontSize = 9.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Aggregates panel ──
            AnimatedVisibility(showAggregates) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Numerical Aggregates", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        aggregates.forEach { (label, values) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("Count: ${values.first.toInt()} | Sum: ${"%.1f".format(values.second)} | Avg: ${"%.2f".format(values.third)}",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (aggregates.isEmpty()) {
                            Text("No numeric columns to aggregate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── Pivot panel ──
            AnimatedVisibility(showPivot) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pivot Table", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Text("Group by:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            columns.forEachIndexed { i, col ->
                                FilterChip(
                                    selected = pivotColumn == i,
                                    onClick = { pivotColumn = i },
                                    label = { Text(col.label, fontSize = 9.sp) }
                                )
                            }
                        }
                        Text("Aggregate:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Count", "Sum", "Average").forEachIndexed { i, label ->
                                FilterChip(
                                    selected = pivotAggregate == i,
                                    onClick = { pivotAggregate = i },
                                    label = { Text(label, fontSize = 9.sp) }
                                )
                            }
                        }
                        // Show pivot results
                        val pivotResults = remember(sortedRows, columns, pivotColumn, pivotAggregate) {
                            val groupCol = columns.getOrNull(pivotColumn) ?: return@remember emptyList<Pair<String, String>>()
                            val numericCols = columns.filter { it.isNumeric }
                            if (numericCols.isEmpty()) return@remember emptyList()
                            val grouped = sortedRows.groupBy { row -> row.cells[groupCol.key] ?: "Other" }
                            grouped.map { (group, groupRows) ->
                                val aggResult = when (pivotAggregate) {
                                    1 -> { // sum
                                        numericCols.joinToString(" | ") { nc ->
                                            val sum = groupRows.sumOf { it.cells[nc.key]?.toDoubleOrNull() ?: 0.0 }
                                            "${nc.label}: ${"%.1f".format(sum)}"
                                        }
                                    }
                                    2 -> { // average
                                        numericCols.joinToString(" | ") { nc ->
                                            val vals = groupRows.mapNotNull { it.cells[nc.key]?.toDoubleOrNull() }
                                            val avg = if (vals.isEmpty()) 0.0 else vals.average()
                                            "${nc.label}: ${"%.2f".format(avg)}"
                                        }
                                    }
                                    else -> { // count
                                        "${groupRows.size} records"
                                    }
                                }
                                group to aggResult
                            }.sortedBy { -it.second.length }
                        }
                        if (pivotResults.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                pivotResults.forEach { (group, result) ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(group, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                        Text(result, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }

            // ── Column headers ──
            if (sortedRows.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(icon = MaterialSymbolIcon("table_rows"), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), size = 40.dp)
                        Text(emptyMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (onAddRow != null) {
                            Button(onClick = onAddRow, shape = RoundedCornerShape(14.dp)) { Text("Add record") }
                        }
                    }
                }
            } else {
                // Table header row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Checkbox column
                    Box(Modifier.width(32.dp)) {
                        Checkbox(
                            checked = selectedRows.size == sortedRows.size && sortedRows.isNotEmpty(),
                            onCheckedChange = { all ->
                                selectedRows = if (all) sortedRows.map { it.id }.toSet() else emptySet()
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    columns.forEachIndexed { i, col ->
                        Row(
                            modifier = Modifier
                                .weight(col.width)
                                .clickable {
                                    if (sortColumn == i) sortAscending = !sortAscending
                                    else { sortColumn = i; sortAscending = true }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (col.align == TextAlign.End) Arrangement.End else Arrangement.Start
                        ) {
                            Text(
                                col.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (sortColumn == i) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (sortColumn == i) accentColor else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 10.sp
                            )
                            if (sortColumn == i) {
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    if (sortAscending) "▲" else "▼",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }

                // ── Data rows ──
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    sortedRows.forEach { row ->
                        TableDataRow(
                            row = row,
                            columns = columns,
                            isSelected = row.id in selectedRows,
                            accentColor = accentColor,
                            onToggleSelect = {
                                selectedRows = if (row.id in selectedRows) selectedRows - row.id else selectedRows + row.id
                            },
                            onEdit = onEditRow?.let { { it(row.id) } },
                            onDelete = onDeleteRow?.let { { it(row.id) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TableDataRow(
    row: DataRow,
    columns: List<DataColumn>,
    isSelected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    onToggleSelect: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    var showActions by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.08f) else Color.Transparent)
            .clickable { showActions = !showActions }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        Box(Modifier.width(32.dp)) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.size(18.dp)
            )
        }
        columns.forEach { col ->
            val value = row.cells[col.key] ?: ""
            Text(
                value,
                modifier = Modifier.weight(col.width),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (showActions) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = col.align,
                fontSize = 11.sp
            )
        }

        // Action buttons
        AnimatedVisibility(showActions) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (onEdit != null) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(icon = MaterialSymbolIcon("edit"), contentDescription = "Edit", size = 14.dp, tint = accentColor)
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(icon = MaterialSymbolIcon("delete"), contentDescription = "Delete", size = 14.dp, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
}

// ── Helper: Export to CSV ──

private fun exportToCsv(context: Context, columns: List<DataColumn>, rows: List<DataRow>) {
    val csv = buildString {
        appendLine(columns.joinToString(",") { "\"${it.label}\"" })
        rows.forEach { row ->
            appendLine(columns.joinToString(",") { col ->
                "\"${(row.cells[col.key] ?: "").replace("\"", "\"\"")}\""
            })
        }
    }
    val file = File(context.cacheDir, "fieldmind_data_export_${System.currentTimeMillis()}.csv")
    file.writeText(csv)
    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export Data as CSV"))
}

// ── Helper: Convert DataRecordEntities to table ──

fun dataRecordsToTable(
    records: List<DataRecordEntity>,
    showProject: Boolean = true,
    showObs: Boolean = true
): Triple<List<DataColumn>, List<DataRow>, List<DataRecordEntity>> {
    val cols = mutableListOf<DataColumn>()
    cols.add(DataColumn("id", "ID", isNumeric = true, width = 0.4f))
    cols.add(DataColumn("tool", "Tool", width = 0.7f))
    cols.add(DataColumn("label", "Label", width = 1.2f))
    cols.add(DataColumn("value", "Value", width = 0.7f, align = TextAlign.End, isNumeric = true))
    cols.add(DataColumn("unit", "Unit", width = 0.5f))
    cols.add(DataColumn("date", "Date", width = 0.8f))
    cols.add(DataColumn("notes", "Notes", width = 1.2f))

    val dateFormat = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
    val rows = records.map { record ->
        DataRow(
            id = record.id,
            cells = mapOf(
                "id" to record.id.toString(),
                "tool" to record.toolType,
                "label" to record.label,
                "value" to record.value,
                "unit" to record.unit,
                "date" to dateFormat.format(java.util.Date(record.timestamp)),
                "notes" to record.notes.take(60)
            )
        )
    }
    return Triple(cols, rows, records)
}
