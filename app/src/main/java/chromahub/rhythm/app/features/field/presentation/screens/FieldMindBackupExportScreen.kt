package fieldmind.research.app.features.field.presentation.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.export.FieldMindExport
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextAlign
import android.app.KeyguardManager
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
    var exportScope by rememberSaveable { mutableStateOf("All") }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var pendingRestorePreview by remember { mutableStateOf<FieldMindExport.ArchivePreview?>(null) }
    var restoring by remember { mutableStateOf(false) }
    var lastBackupRefresh by remember { mutableIntStateOf(0) }
    val lastBackupLabel: String = remember(lastBackupRefresh) { lastBackupSummary(context) }

    fun scopedProjects() = if (exportScope in listOf("All", "Projects")) projects else emptyList()
    fun scopedObservations() = if (exportScope in listOf("All", "Observations")) observations else emptyList()
    fun scopedSources() = if (exportScope in listOf("All", "Sources")) sources else emptyList()
    fun scopedReports() = if (exportScope in listOf("All", "Reports")) reports else emptyList()

    LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (showHeader) item {
            StandardScreenHeader(
                title = "Export Studio",
                subtitle = "Choose a scope, backup option, and portable research format.",
                icon = FieldMindIcons.Export
            )
        }
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
            OptionPickerField(label = "Scope", selected = scope, options = listOf("All", "Projects", "Observations", "Sources", "Reports"), onSelected = onScope, icon = FieldMindIcons.Archive)
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
fun FieldMindPrivacyGate(enabled: Boolean, title: String, body: String): Boolean {
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

private fun backupDirectory(context: Context): File = File(context.filesDir, "fieldmind/backups").apply { mkdirs() }

private fun lastBackupSummary(context: Context): String {
    val latest = backupDirectory(context).listFiles { file -> file.isFile && file.extension == "json" }?.maxByOrNull { it.lastModified() }
    return latest?.let { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(it.lastModified())) } ?: "Never"
}

@Composable
fun BackupExportScreen(viewModel: FieldMindViewModel) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.fillMaxSize()) {
            ExportStudioContent(
                viewModel = viewModel,
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
                onMessage = { message -> showFastSnackbar(snackbar, scope, message) }
            )
            FieldMindSnackbarOverlay(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}
