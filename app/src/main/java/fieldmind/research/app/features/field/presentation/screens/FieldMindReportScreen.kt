package fieldmind.research.app.features.field.presentation.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.export.FieldMindExport
import fieldmind.research.app.features.field.data.export.FieldMindReportGenerator
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import androidx.compose.foundation.clickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
// ══════════════════════════════════════════════════════════════════════
//  Report Generation Screen
// ══════════════════════════════════════════════════════════════════════

enum class ReportExportFormat { HTML, MARKDOWN }

@Composable
fun FieldMindReportScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val colors = FieldMindTheme.colors

    // Entity collections
    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val dataRecords by viewModel.dataRecords.collectAsState()
    val reports by viewModel.reports.collectAsState()

    // Report state
    var reportTitle by remember { mutableStateOf("FieldMind Research Report") }
    var reportFormat by remember { mutableStateOf(ReportExportFormat.HTML) }
    var isGenerating by remember { mutableStateOf(false) }
    var showRawData by remember { mutableStateOf(false) }
    var generatedHtml by remember { mutableStateOf<String?>(null) }
    var generatedMarkdown by remember { mutableStateOf<String?>(null) }

    // Build report data
    val reportData = remember(observations, notes, projects, sources, dataRecords, reports) {
        FieldMindReportGenerator.buildReportData(
            observations = observations,
            notes = notes,
            projects = projects,
            sources = sources,
            dataRecords = dataRecords,
            reports = reports,
            title = reportTitle
        )
    }

    // ── Generate report content ──
    fun generateReport() {
        scope.launch {
            isGenerating = true
            try {
                withContext(Dispatchers.Default) {
                    val html = FieldMindReportGenerator.generateHtmlReport(reportData, showRawData)
                    val md = FieldMindReportGenerator.generateMarkdownReport(reportData)
                    generatedHtml = html
                    generatedMarkdown = md
                }
            } catch (e: Exception) {
                showFastSnackbar(snackbar, scope, "Report generation failed: ${e.localizedMessage}")
            } finally {
                isGenerating = false
            }
        }
    }

    // ── Export report ──
    fun exportReport() {
        val content = when (reportFormat) {
            ReportExportFormat.HTML -> generatedHtml
            ReportExportFormat.MARKDOWN -> generatedMarkdown
        } ?: return

        scope.launch {
            try {
                val ext = when (reportFormat) {
                    ReportExportFormat.HTML -> "html"
                    ReportExportFormat.MARKDOWN -> "md"
                }
                val dateStamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault()).format(Date())
                val fileName = "fieldmind-report-$dateStamp.$ext"
                val exportDir = File(context.cacheDir, "reports").apply { mkdirs() }
                val exportFile = File(exportDir, fileName)
                exportFile.writeText(content)

                val shareUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    exportFile
                )
                val mimeType = when (reportFormat) {
                    ReportExportFormat.HTML -> "text/html"
                    ReportExportFormat.MARKDOWN -> "text/markdown"
                }
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
                showFastSnackbar(snackbar, scope, "Report saved: $fileName")
            } catch (e: Exception) {
                showFastSnackbar(snackbar, scope, "Export failed: ${e.localizedMessage}")
            }
        }
    }

    // Pulse animation for hero
    val pulseTransition = rememberInfiniteTransition(label = "reportPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "reportGlow"
    )

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ──
            item {
                StandardScreenHeader(
                    title = "Report Generator",
                    subtitle = "Create rich research reports with charts and data tables",
                    icon = FieldMindIcons.Report,
                    trailing = { BackButton(onClick = onBack) }
                )
            }

            // ── Report title ──
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                    .background(colors.report.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(FieldMindIcons.Report, null, tint = colors.report, size = 22.dp)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Report title", fontWeight = FontWeight.SemiBold)
                                Text("Customize the report heading", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedTextField(
                            value = reportTitle,
                            onValueChange = { reportTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true,
                            placeholder = { Text("Enter report title") }
                        )
                    }
                }
            }

            // ── Overview stats ──
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(FieldMindIcons.Insights, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 22.dp)
                            Text("Data overview", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem("${reportData.overview.totalObservations}", "Observations", colors.observation)
                            StatItem("${reportData.overview.totalProjects}", "Projects", colors.hypothesis)
                            StatItem("${reportData.overview.totalSources}", "Sources", colors.data)
                            StatItem("${reportData.overview.totalNotes}", "Notes", colors.info)
                        }
                        Text(
                            "Date range: ${reportData.overview.dateRange}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── Category breakdown (pie chart preview) ──
            if (reportData.overview.topCategories.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Category breakdown", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            reportData.overview.topCategories.take(8).forEach { (cat, count) ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier.size(10.dp).clip(RoundedCornerShape(3.dp))
                                            .background(                                        categoryColor(cat))
                                    )
                                    Text(cat, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    Text("$count", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // ── Options card ──
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(FieldMindIcons.Settings, null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                            Text("Options", fontWeight = FontWeight.SemiBold)
                        }

                        // Format selector
                        Text("Export format", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(ReportExportFormat.HTML to "🌐 HTML", ReportExportFormat.MARKDOWN to "📝 Markdown").forEach { (fmt, label) ->
                                val selected = reportFormat == fmt
                                Surface(
                                    onClick = { reportFormat = fmt },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        label,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Raw data toggle
                        Row(
                            Modifier.fillMaxWidth().clickable(enabled = true, onClick = { showRawData = !showRawData }).padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(FieldMindIcons.Data, null, tint = if (showRawData) colors.data else MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                            Column(Modifier.weight(1f)) {
                                Text("Include raw data appendix", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                Text("CSV data for observations, sources, and data records", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = showRawData, onCheckedChange = { showRawData = it })
                        }
                    }
                }
            }

            // ── Generate button ──
            item {
                Button(
                    onClick = { generateReport() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.report)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Generating report…")
                    } else {
                        Icon(FieldMindIcons.Report, null, size = 18.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(if (generatedHtml != null) "Regenerate report" else "Generate report")
                    }
                }
            }

            // ── Preview and export (after generation) ──
            if (generatedHtml != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(FieldMindIcons.Check, null, tint = colors.positive, size = 22.dp)
                                Text("Report ready", fontWeight = FontWeight.Bold, color = colors.positive)
                                Spacer(Modifier.weight(1f))
                                // Size indicator
                                val size = generatedHtml?.length ?: 0
                                Text(
                                    when {
                                        size < 1024 -> "$size chars"
                                        size < 1024 * 10 -> "${size / 1024} KB"
                                        else -> "%.1f MB".format(size.toFloat() / (1024 * 1024))
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Preview section count
                            val totalEntities = reportData.overview.totalObservations +
                                reportData.overview.totalProjects + reportData.overview.totalSources
                            Text(
                                "Covers $totalEntities records across ${reportData.overview.topCategories.size} categories",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Export button
                            Button(
                                onClick = { exportReport() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(FieldMindIcons.Export, null, size = 18.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Export & share as ${if (reportFormat == ReportExportFormat.HTML) "HTML" else "Markdown"}")
                            }
                        }
                    }
                }
            }

            // ── Information card ──
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.primary, size = 18.dp, modifier = Modifier.padding(top = 2.dp))
                        Column {
                            Text("How reports work", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Reports are generated entirely on-device from your saved observations, notes, projects, and sources. " +
                                "SVG charts (pie, bar) are embedded inline for HTML exports. " +
                                "No data ever leaves your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Snackbar
        FieldMindSnackbarOverlay(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Helper composables
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun categoryColor(category: String): Color = when (category.lowercase()) {
    "bird" -> FieldMindTheme.colors.observation
    "plant" -> FieldMindTheme.colors.data
    "insect" -> FieldMindTheme.colors.categorical[7]
    "mammal" -> FieldMindTheme.colors.categorical[4]
    "weather" -> FieldMindTheme.colors.question
    "geology" -> FieldMindTheme.colors.categorical[9]
    "water" -> FieldMindTheme.colors.categorical[4]
    "reading insight" -> FieldMindTheme.colors.warning
    "fungi" -> FieldMindTheme.colors.categorical[5]
    "reptile" -> FieldMindTheme.colors.observation
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
