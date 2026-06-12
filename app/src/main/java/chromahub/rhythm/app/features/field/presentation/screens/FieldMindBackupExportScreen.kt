package chromahub.rhythm.app.features.field.presentation.screens

import android.content.Context
import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.features.field.data.database.entity.*
import chromahub.rhythm.app.features.field.data.export.FieldMindExport
import chromahub.rhythm.app.features.field.presentation.components.*
import chromahub.rhythm.app.features.field.presentation.theme.FieldMindTheme
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
// ══════════════════════════════════════════════════════════════════════
//  Backup & export
// ══════════════════════════════════════════════════════════════════════


@Composable
fun ExportStudioContent(
    viewModel: FieldMindViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
    showHeader: Boolean = true,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val data by viewModel.dataRecords.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val settings = viewModel.fieldSettings
    val attachmentMode by settings.attachmentExportMode.collectAsState()
    val autoBackupEnabled by settings.autoBackupEnabled.collectAsState()
    val autoBackupInterval by settings.autoBackupInterval.collectAsState()
    var exportScope by rememberSaveable { mutableStateOf("All") }
    var pendingBytes by remember { mutableStateOf(ByteArray(0)) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var pendingRestorePreview by remember { mutableStateOf<FieldMindExport.ArchivePreview?>(null) }
    var restoring by remember { mutableStateOf(false) }
    var lastBackupRefresh by remember { mutableIntStateOf(0) }
    val lastBackupLabel = remember(lastBackupRefresh) { lastBackupSummary(context) }

    fun scopedProjects() = if (exportScope in listOf("All", "Projects")) projects else emptyList()
    fun scopedObservations() = if (exportScope in listOf("All", "Observations")) observations else emptyList()
    fun scopedSources() = if (exportScope in listOf("All", "Sources")) sources else emptyList()
    fun scopedReports() = if (exportScope in listOf("All", "Reports")) reports else emptyList()
    fun scopedNotes() = if (exportScope in listOf("All", "Observations")) notes else emptyList()
    fun scopedQuestions() = if (exportScope in listOf("All", "Projects")) questions else emptyList()
    fun scopedHypotheses() = if (exportScope in listOf("All", "Projects")) hypotheses else emptyList()
    fun scopedData() = if (exportScope in listOf("All", "Projects")) data else emptyList()
    fun scopeSlug() = exportScope.lowercase(Locale.US).replace(" ", "-")
    fun html() = FieldMindExport.pdfReadyHtml(scopedProjects(), scopedObservations(), scopedSources(), scopedReports())
    fun archiveJson() = FieldMindExport.archiveJson(scopedObservations(), scopedNotes(), scopedQuestions(), scopedHypotheses(), scopedProjects(), scopedSources(), scopedData(), scopedReports(), if (exportScope == "All") flashcards else emptyList())
    fun queueText(text: String) { pendingBytes = text.toByteArray() }

    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri == null) onMessage("Export cancelled.") else runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(pendingBytes) } ?: error("Could not open destination file")
        }.onSuccess { onMessage("Export saved for $exportScope.") }.onFailure { onMessage("Export failed: ${it.localizedMessage}") }
    }
    val importDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) onMessage("Import cancelled.") else runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            FieldMindExport.previewArchiveJson(raw) to raw
        }.onSuccess { (preview, raw) ->
            pendingRestorePreview = preview
            pendingRestoreJson = raw
            onMessage("Backup ready to restore: ${preview.total} records found.")
        }.onFailure { onMessage("Import failed: ${it.localizedMessage}") }
    }

    LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (showHeader) item { FieldScreenHeader("Export Studio", "Choose a scope, backup option, and portable research format.", icon = FieldMindIcons.Export) }
        item {
            ExportStudioHero(
                scope = exportScope,
                onScope = { exportScope = it },
                projects = scopedProjects().size,
                observations = scopedObservations().size,
                sources = scopedSources().size,
                reports = scopedReports().size,
                attachmentMode = attachmentMode,
                lastBackup = lastBackupLabel
            )
        }
        item {
            AutoBackupOptionsCard(
                enabled = autoBackupEnabled,
                interval = autoBackupInterval,
                lastBackup = lastBackupLabel,
                onEnabled = settings::setAutoBackupEnabled,
                onInterval = settings::setAutoBackupInterval,
                onBackupNow = {
                    runCatching { writeManualBackup(context, archiveJson()) }
                        .onSuccess { file -> lastBackupRefresh++; onMessage("Backup saved: ${file.name}") }
                        .onFailure { onMessage("Backup failed: ${it.localizedMessage}") }
                }
            )
        }
        exportSection("Recommended") {
            ExportActionCard("Research PDF", "Clean PDF for sharing a scoped research packet.", "PDF", "Best for teachers, peers, and field reports", FieldMindIcons.Report) { pendingBytes = FieldMindExport.simplePdfBytes("FieldMind $exportScope Export", html().replace(Regex("<[^>]+>"), " ")); createDoc.launch("fieldmind-${scopeSlug()}-export.pdf") }
            ExportActionCard("Archive backup", "Portable JSON bundle for backup and migration.", "JSON", "Best for safe backup copies", FieldMindIcons.Archive) { queueText(archiveJson()); createDoc.launch("fieldmind-${scopeSlug()}-backup.json") }
            ExportActionCard("PDF-ready HTML", "Readable print layout that can be opened in a browser.", "HTML", "Best for print preview and manual PDF export", FieldMindIcons.Article) { queueText(html()); createDoc.launch("fieldmind-${scopeSlug()}-export.html") }
        }
        exportSection("Data tables") {
            ExportActionCard("Observations table", "Rows of scoped observations for spreadsheets.", "CSV", "Best for sorting and analysis", FieldMindIcons.Observation, enabled = exportScope in listOf("All", "Observations")) { queueText(FieldMindExport.observationsCsv(scopedObservations())); createDoc.launch("fieldmind-${scopeSlug()}-observations.csv") }
            ExportActionCard("Data records table", "Measurements, counters, logs, and tool entries.", "CSV", "Best for statistics tools", FieldMindIcons.Data, enabled = exportScope in listOf("All", "Projects")) { queueText(FieldMindExport.dataCsv(scopedData())); createDoc.launch("fieldmind-${scopeSlug()}-data.csv") }
            ExportActionCard("Sources table", "Citation/source metadata for reading workflows.", "CSV", "Best for bibliography cleanup", FieldMindIcons.Source, enabled = exportScope in listOf("All", "Sources")) { queueText(FieldMindExport.sourcesCsv(scopedSources())); createDoc.launch("fieldmind-${scopeSlug()}-sources.csv") }
        }
        exportSection("Visual dashboard") {
            ExportActionCard("Dashboard PNG", "Snapshot image of counts and activity.", "PNG", "Best for quick presentations", FieldMindIcons.Graph, enabled = exportScope == "All") { pendingBytes = FieldMindExport.dashboardPngBytes(observations, sources, projects, notes); createDoc.launch("fieldmind-dashboard.png") }
            ExportActionCard("Dashboard SVG", "Scalable dashboard graphic for documents.", "SVG", "Best for crisp reports", FieldMindIcons.Graph, enabled = exportScope == "All") { queueText(FieldMindExport.dashboardSvg(observations, sources, projects, notes)); createDoc.launch("fieldmind-dashboard.svg") }
        }
        exportSection("Reports & restore") {
            ExportActionCard("Reports Markdown", "Markdown bundle of scoped reports.", "MD", "Best for editing in writing apps", FieldMindIcons.Report, enabled = exportScope in listOf("All", "Reports")) { queueText(scopedReports().joinToString("\n\n---\n\n") { FieldMindExport.buildMarkdownReport(it) }); createDoc.launch("fieldmind-${scopeSlug()}-reports.md") }
            ExportActionCard("Import backup", "Pick a JSON backup and preview it before restore.", "JSON", "Best for checking backup integrity", FieldMindIcons.Archive) { importDoc.launch(arrayOf("application/json", "text/*", "*/*")) }
        }
    }

    pendingRestorePreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { if (!restoring) { pendingRestorePreview = null; pendingRestoreJson = null } },
            icon = { Icon(FieldMindIcons.Archive, contentDescription = null) },
            title = { Text("Restore backup?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FieldMind will merge this archive as new records so existing research is not deleted.")
                    Text(preview.summary(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(enabled = !restoring, onClick = {
                    val raw = pendingRestoreJson ?: return@Button
                    restoring = true
                    viewModel.restoreArchiveJson(raw) { result ->
                        restoring = false
                        pendingRestorePreview = null
                        pendingRestoreJson = null
                        result.onSuccess { onMessage("Restored ${it.total} records from backup.") }
                            .onFailure { onMessage("Restore failed: ${it.localizedMessage}") }
                    }
                }) { Text(if (restoring) "Restoring…" else "Merge restore") }
            },
            dismissButton = { TextButton(enabled = !restoring, onClick = { pendingRestorePreview = null; pendingRestoreJson = null }) { Text("Cancel") } }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.exportSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    item {
        SectionHeader(title, "Grouped export actions with clear formats and use cases.")
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun ExportStudioHero(scope: String, onScope: (String) -> Unit, projects: Int, observations: Int, sources: Int, reports: Int, attachmentMode: String, lastBackup: String) {
    Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(FieldMindIcons.Export, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 28.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Export Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("$scope package • attachments: $attachmentMode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
                }
            }
            ChoiceChips(listOf("All", "Projects", "Observations", "Sources", "Reports"), scope, onSelected = onScope)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExportCount("Projects", projects, Modifier.weight(1f))
                ExportCount("Obs", observations, Modifier.weight(1f))
                ExportCount("Sources", sources, Modifier.weight(1f))
                ExportCount("Reports", reports, Modifier.weight(1f))
            }
            Text("Last backup: $lastBackup", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f))
        }
    }
}

@Composable
fun ExportCount(label: String, count: Int, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AutoBackupOptionsCard(enabled: Boolean, interval: String, lastBackup: String, onEnabled: (Boolean) -> Unit, onInterval: (String) -> Unit, onBackupNow: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(FieldMindIcons.Archive, null, tint = MaterialTheme.colorScheme.primary, size = 24.dp)
                Column(Modifier.weight(1f)) {
                    Text("Backup options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Auto backup: ${if (enabled) interval else "Off"} • Last: $lastBackup", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = onEnabled)
            }
            AnimatedVisibility(enabled) { ChoiceChips(listOf("Daily", "Weekly", "Monthly"), interval, onSelected = onInterval) }
            FilledTonalButton(onClick = onBackupNow, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(FieldMindIcons.Archive, null, size = 18.dp); Spacer(Modifier.size(8.dp)); Text("Save backup now")
            }
        }
    }
}

@Composable
fun ExportActionCard(title: String, subtitle: String, format: String, bestFor: String, icon: MaterialSymbolIcon, enabled: Boolean = true, onClick: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Card(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { haptics.light(); onClick() }.animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = if (enabled) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.12f else 0.06f)), contentAlignment = Alignment.Center) {
                Icon(icon = icon, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, size = 23.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    AssistChip(onClick = {}, enabled = false, label = { Text(format) })
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (enabled) bestFor else "Change scope to enable this export.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(icon = if (enabled) FieldMindIcons.Export else FieldMindIcons.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
        }
    }
}

private fun backupDirectory(context: Context): File = File(context.filesDir, "fieldmind/backups").apply { mkdirs() }

private fun lastBackupSummary(context: Context): String {
    val latest = backupDirectory(context).listFiles { file -> file.isFile && file.extension == "json" }?.maxByOrNull { it.lastModified() }
    return latest?.let { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(it.lastModified())) } ?: "Never"
}

private fun writeManualBackup(context: Context, archiveJson: String): File {
    val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    return File(backupDirectory(context), "fieldmind-manual-$stamp.json").apply { writeText(archiveJson) }
}


@Composable
private fun FieldMindPrivacyGate(enabled: Boolean, title: String, body: String): Boolean {
    if (!enabled) return true
    val context = LocalContext.current
    var unlocked by rememberSaveable(enabled) { mutableStateOf(false) }
    val keyguard = remember(context) { context.getSystemService(KeyguardManager::class.java) }
    val unlockLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        unlocked = result.resultCode == android.app.Activity.RESULT_OK
    }
    if (unlocked) return true

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(FieldMindIcons.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 28.dp)
                }
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Button(
                    onClick = {
                        val intent = keyguard?.createConfirmDeviceCredentialIntent("Unlock FieldMind", "Confirm your device lock to view sensitive research data.")
                        if (intent != null) unlockLauncher.launch(intent) else unlocked = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (keyguard?.isDeviceSecure == true) "Unlock" else "Continue") }
                if (keyguard?.isDeviceSecure != true) Text("Set a device PIN/password for stronger protection. FieldMind will use it automatically.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
    return false
}

@Composable
fun BackupExportScreen(viewModel: FieldMindViewModel) {
    val privacyEnabled by viewModel.fieldSettings.privacyLockEnabled.collectAsState()
    if (!FieldMindPrivacyGate(privacyEnabled, "Export is locked", "Backups and exports can contain sensitive field notes, sources, and locations.")) return
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        ExportStudioContent(
            viewModel = viewModel,
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
            onMessage = { message -> scope.launch { snackbar.showSnackbar(message) } }
        )
    }
}

