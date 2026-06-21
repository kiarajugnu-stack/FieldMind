package fieldmind.research.app.features.field.presentation.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
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
import fieldmind.research.app.features.field.data.export.FieldMindExportMediaPacker
import fieldmind.research.app.features.field.data.export.FieldMindExportEncryption
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════
//  Data models
// ══════════════════════════════════════════════════════════════════════

enum class BackupTab { EXPORT, IMPORT, BACKUP }

enum class ExportScopeType { ALL, PROJECTS, OBSERVATIONS, SOURCES, REPORTS, CUSTOM }

data class FormatOption(
    val name: String,
    val desc: String,
    val icon: MaterialSymbolIcon,
    val color: Color,
    val group: String = "document" // document, data, image, package
)

private val exportFormats = listOf(
    FormatOption("JSON", "Structured archive for migration", FieldMindIcons.Archive, androidx.compose.ui.graphics.Color(0xFF7B1FA2), "data"),
    FormatOption("CSV", "Tabular data for spreadsheets", FieldMindIcons.Data, androidx.compose.ui.graphics.Color(0xFF1565C0), "data"),
    FormatOption("Markdown", "Readable text for docs & notes", FieldMindIcons.Article, androidx.compose.ui.graphics.Color(0xFF558B2F), "document"),
    FormatOption("HTML", "Print-ready web layout", FieldMindIcons.Article, androidx.compose.ui.graphics.Color(0xFFE65100), "document"),
    FormatOption("PDF", "Portable document format", FieldMindIcons.Report, androidx.compose.ui.graphics.Color(0xFF6A1B9A), "document"),
    FormatOption("PNG", "Dashboard snapshot image", FieldMindIcons.Graph, androidx.compose.ui.graphics.Color(0xFF2E7D32), "image"),
    FormatOption("SVG", "Scalable vector graphic", FieldMindIcons.Graph, androidx.compose.ui.graphics.Color(0xFF00838F), "image"),
    FormatOption(".fieldmind", "Package with images & encryption", FieldMindIcons.Archive, androidx.compose.ui.graphics.Color(0xFF1B5E20), "package")
)

data class ExportRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val format: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val entityCounts: Map<String, Int> = emptyMap(),
    val exportedAt: Long = System.currentTimeMillis(),
    val destination: String = "",
    val success: Boolean = true
)

// ══════════════════════════════════════════════════════════════════════
//  Backup & Restore Screen
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndRestoreScreen(
    viewModel: FieldMindViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val colors = FieldMindTheme.colors

    // Tab state
    var activeTab by remember { mutableStateOf(BackupTab.EXPORT) }

    // Hero card state
    var lastBackupRefresh by remember { mutableIntStateOf(0) }
    val lastBackupLabel: String = remember(lastBackupRefresh) { lastBackupSummary(context) }

    // Entity collections
    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val dataRecords by viewModel.dataRecords.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()

    // Settings
    val settings = viewModel.fieldSettings
    val autoBackupEnabled by settings.autoBackupEnabled.collectAsState()
    val autoBackupInterval by settings.autoBackupInterval.collectAsState()
    val defaultExportFormat by settings.defaultExportFormat.collectAsState()

    // Export privacy settings
    val clearClipboardAfterExport by settings.clearClipboardAfterExport.collectAsState()
    val exportGpsPrivacy by settings.exportGpsPrivacy.collectAsState()
    val exportExcludeMedia by settings.exportExcludeMedia.collectAsState()

    // Backup folder URI from settings (reactive) — must be declared before exportDestinationUri
    val backupFolderUri by settings.backupFolderUri.collectAsState()

    // Export state
    var exportDestinationUri by remember(backupFolderUri) { mutableStateOf<Uri?>(backupFolderUri.takeIf { it.isNotBlank() }?.let(Uri::parse)) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportStepText by remember { mutableStateOf("") }

    // Import state
    var importFileUri by remember { mutableStateOf<Uri?>(null) }
    var importFileName by remember { mutableStateOf("") }
    var importFileSize by remember { mutableStateOf("") }
    var importPreview by remember { mutableStateOf<FieldMindExport.ArchivePreview?>(null) }
    var importMode by remember { mutableStateOf("Merge") }
    var isImporting by remember { mutableStateOf(false) }
    var importedPackage by remember { mutableStateOf<FieldMindExportMediaPacker.ExtractedPackage?>(null) }
    var showImportResultDialog by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<FieldMindExport.ArchivePreview?>(null) }
    var importStepText by remember { mutableStateOf("") }
    var importProgress by remember { mutableFloatStateOf(0f) }
    var isEncryptedFile by remember { mutableStateOf(false) }
    var importPassword by remember { mutableStateOf("") }
    var showPasswordPrompt by remember { mutableStateOf(false) }

    // Backup tab state
    val privacyLockEnabled by settings.privacyLockEnabled.collectAsState()
    var backupEncrypt by remember { mutableStateOf(privacyLockEnabled) }
    // Sync encrypt with privacy lock changes
    LaunchedEffect(privacyLockEnabled) { if (privacyLockEnabled && !backupEncrypt) backupEncrypt = true }
    var backupPassword by remember { mutableStateOf("") }
    var backupScheduleEnabled by remember { mutableStateOf(autoBackupEnabled) }
    var backupInterval by remember { mutableStateOf(autoBackupInterval) }
    var backupPasswordConfirm by remember { mutableStateOf("") }
    var passwordsMatch by remember { mutableStateOf(true) }

    // Format state for export
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDialogFormat by remember { mutableStateOf(".fieldmind") }
    var includeMedia by remember { mutableStateOf(false) }

    // Folder picker launcher (export folder)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        exportDestinationUri = uri
        if (uri != null) {
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            settings.setBackupFolderUri(uri.toString())
        }
    }

    // Folder picker launcher (backup folder - persisted)
    val backupFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            // Persist the folder URI - will update backupFolderUri via Flow
            settings.setBackupFolderUri(uri.toString())
        }
    }

    // File picker launcher (import)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            importFileUri = uri
            // Read file name
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) importFileName = it.getString(nameIndex)
                }
            }
            // Read file size
            val sizeCursor = context.contentResolver.query(uri, null, null, null, null)
            sizeCursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        val size = it.getLong(sizeIndex)
                        importFileSize = formatFileSize(size)
                    }
                }
            }
            // Check if encrypted
            isEncryptedFile = importFileName.endsWith(".encrypted")
            if (isEncryptedFile) {
                // Show password prompt for encrypted files
                showPasswordPrompt = true
            } else {
                // Parse preview directly
                scope.launch {
                    try {
                        val raw = withContext(Dispatchers.IO) {
                            if (importFileName.endsWith(".fieldmind") || importFileName.endsWith(".zip")) {
                                FieldMindExportMediaPacker.extractPackage(context, uri)?.archiveJson ?: ""
                            } else {
                                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                            }
                        }
                        val preview = FieldMindExport.previewArchiveJson(raw)
                        importPreview = preview
                    } catch (e: Exception) {
                        showFastSnackbar(snackbar, scope, "Could not parse archive: ${e.localizedMessage}")
                        importPreview = null
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            FieldMindSnackbarOverlay(
                hostState = snackbar,
                modifier = Modifier
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ──
            item {
                StandardScreenHeader(
                    title = "Backup & Restore",
                    subtitle = "Export, import, and manage your research data.",
                    icon = FieldMindIcons.Export,
                    trailing = { BackButton(onClick = onBack) }
                )
            }

            // ── Hero status card ──
            item {
                HeroStatusCard(
                    lastBackupLabel = lastBackupLabel,
                    autoBackupEnabled = autoBackupEnabled,
                    autoBackupInterval = autoBackupInterval,
                    entityCounts = mapOf(
                        "observations" to observations.size,
                        "notes" to notes.size,
                        "questions" to questions.size,
                        "projects" to projects.size,
                        "sources" to sources.size
                    )
                )
            }

            // ── 3-tab pill selector ──
            item {
                TabPillSelector(
                    activeTab = activeTab,
                    onTabChange = { activeTab = it }
                )
            }

            // ── Tab content ──
            item {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn(tween(250)) + slideInHorizontally { it / 4 } togetherWith
                            fadeOut(tween(200)) + slideOutHorizontally { -it / 4 }
                    },
                    label = "tabContent"
                ) { tab ->
                    when (tab) {
                        BackupTab.EXPORT -> ExportTabContent(
                            entityCounts = mapOf(
                                "Observations" to observations.size,
                                "Notes" to notes.size,
                                "Questions" to questions.size,
                                "Hypotheses" to hypotheses.size,
                                "Projects" to projects.size,
                                "Sources" to sources.size,
                                "Data Records" to dataRecords.size,
                                "Reports" to reports.size,
                                "Flashcards" to flashcards.size
                            ),
                            context = context,
                            isExporting = isExporting,
                            exportProgress = exportProgress,
                            exportStepText = exportStepText,
                            gpsPrivacy = exportGpsPrivacy,
                            onGpsPrivacyChange = { settings.setExportGpsPrivacy(it) },
                            excludeMedia = exportExcludeMedia,
                            onExcludeMediaChange = { settings.setExportExcludeMedia(it) },
                            clearClipboard = clearClipboardAfterExport,
                            onClearClipboardChange = { settings.setClearClipboardAfterExport(it) },
                            onExport = { format, action ->
                                scope.launch {
                                    isExporting = true
                                    exportProgress = 0f
                                    exportStepText = "Building $format…"
                                    try {
                                        withContext(Dispatchers.IO) {
                                            exportProgress = 0.3f
                                            // Apply export privacy transforms before building the archive
                                            val exportObs = FieldMindExport.applyGpsPrivacy(observations, exportGpsPrivacy)
                                            val exportNotes = if (exportExcludeMedia) FieldMindExport.applyMediaExclusion(notes) else notes
                                            val json = FieldMindExport.archiveJson(
                                                observations = exportObs, notes = exportNotes,
                                                questions = questions, hypotheses = hypotheses,
                                                projects = projects, sources = sources,
                                                dataRecords = dataRecords, reports = reports,
                                                flashcards = flashcards
                                            )
                                            val dateStamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault()).format(Date())
                                            val ext = when (format) {
                                                "Markdown" -> "md"
                                                ".fieldmind" -> "fieldmind"
                                                ".zip" -> "zip"
                                                else -> format.lowercase().removePrefix(".")
                                            }
                                            val fileName = "fieldmind-export-$dateStamp.$ext"
                                            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                                            val exportFile = File(exportDir, fileName)

                                            exportProgress = 0.5f
                                            when (format) {
                                                "Markdown" -> exportFile.writeText(
                                                    observations.joinToString("\n\n---\n\n") { FieldMindExport.singleObservationMarkdown(it) }
                                                )
                                                "JSON" -> exportFile.writeText(json)
                                                "PDF" -> exportFile.writeBytes(
                                                    FieldMindExport.simplePdfBytes(
                                                        "FieldMind Export",
                                                        observations.joinToString("\n") { FieldMindExport.singleObservationMarkdown(it) }
                                                    )
                                                )
                                                ".fieldmind", ".zip" -> {
                                                    val allAttachments = mutableMapOf<Long, List<EvidenceAttachmentEntity>>()
                                                    if (!exportExcludeMedia) {
                                                        exportObs.forEach { obs ->
                                                            val atts = viewModel.attachmentsForObservation(obs.id).first()
                                                            if (atts.isNotEmpty()) allAttachments[obs.id] = atts
                                                        }
                                                    }
                                                    val result = FieldMindExportMediaPacker.buildPackage(
                                                        context = context, archiveJson = json,
                                                        observations = exportObs, notes = exportNotes, projects = projects, sources = sources,
                                                        attachments = allAttachments, outputDir = exportDir
                                                    )
                                                    result.packageFile.copyTo(exportFile, overwrite = true)
                                                }
                                            }

                                            exportProgress = 0.8f
                                            if (action == "share") {
                                                val shareUri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.provider",
                                                    exportFile
                                                )
                                                val mimeType = if (format == "PDF") "application/pdf" else "text/markdown"
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = mimeType
                                                    putExtra(Intent.EXTRA_STREAM, shareUri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share FieldMind Export"))
                                            } else {
                                                val destUri = exportDestinationUri
                                                if (destUri == null) {
                                                    throw java.io.IOException("Please select a destination folder first")
                                                }
                                                val mimeType = when (format) {
                                                    "PDF" -> "application/pdf"
                                                    ".fieldmind" -> "application/zip"
                                                    ".zip" -> "application/zip"
                                                    "JSON" -> "application/json"
                                                    "CSV" -> "text/csv"
                                                    "Markdown" -> "text/markdown"
                                                    else -> "application/octet-stream"
                                                }
                                                try {
                                                    val createdDoc = createFileInTree(context, destUri, mimeType, fileName)
                                                    if (createdDoc != null) {
                                                        val outStream = context.contentResolver.openOutputStream(createdDoc)
                                                        if (outStream != null) {
                                                            outStream.use { out ->
                                                                out.write(exportFile.readBytes())
                                                            }
                                                            // Verify the file was actually written
                                                            val fileSizeCursor = context.contentResolver.query(createdDoc, null, null, null, null)
                                                            var verified = false
                                                            fileSizeCursor?.use { cursor ->
                                                                if (cursor.moveToFirst()) {
                                                                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                                                    if (sizeIndex >= 0 && cursor.getLong(sizeIndex) > 0) verified = true
                                                                }
                                                            }
                                                            if (!verified) {
                                                                throw java.io.IOException("File created but could not verify content was written")
                                                            }
                                                        } else {
                                                            throw java.io.IOException("Could not open output stream - check folder permissions")
                                                        }
                                                    } else {
                                                        throw java.io.IOException("Could not create document in the selected folder. Ensure the folder has write permission and is not read-only.")
                                                    }
                                                } catch (e: Exception) {
                                                    throw java.io.IOException("Save failed: ${e.localizedMessage}")
                                                }
                                            }
                                        }
                                        showFastSnackbar(snackbar, scope, "Export complete")
                                    } catch (e: Exception) {
                                        showFastSnackbar(snackbar, scope, "Export failed: ${e.localizedMessage}")
                                    } finally {
                                        isExporting = false
                                        exportProgress = 0f
                                    }
                                }
                            },
                            onChooseFolder = { folderPickerLauncher.launch(null) },
                            destinationUri = exportDestinationUri,
                            onSwitchToImport = { activeTab = BackupTab.IMPORT }
                        )

                        BackupTab.IMPORT -> ImportTabContent(
                            selectedFileUri = importFileUri,
                            fileName = importFileName,
                            preview = importPreview,
                            importMode = importMode,
                            onModeChange = { importMode = it },
                            isImporting = isImporting,
                            onPickFile = { filePickerLauncher.launch(arrayOf("application/json", "application/octet-stream", "application/zip", "*/*")) },
                            onClearFile = {
                                importFileUri = null
                                importFileName = ""
                                importPreview = null
                            },
                            onImport = {
                                                scope.launch {
                                    if (importFileUri == null) return@launch
                                    isImporting = true
                                    importStepText = "Reading archive…"
                                    importProgress = 0.1f
                                    var decryptedTempFile: File? = null
                                    try {
                                        val raw = withContext(Dispatchers.IO) {
                                            importStepText = "Extracting data…"
                                            importProgress = 0.3f
                                            val isFieldMind = importFileName.endsWith(".fieldmind") || importFileName.endsWith(".zip") || importFileName.endsWith(".encrypted")
                                            val uriToRead = if (importFileName.endsWith(".encrypted")) {
                                                // Decrypt first
                                                importStepText = "Decrypting with password…"
                                                importProgress = 0.2f
                                                val tempDir = File(context.cacheDir, "import_decrypt")
                                                tempDir.mkdirs()
                                                val encryptedFile = File(context.cacheDir, "import_encrypted_${System.currentTimeMillis()}")
                                                context.contentResolver.openInputStream(importFileUri!!)?.use { input ->
                                                    encryptedFile.outputStream().use { output -> input.copyTo(output) }
                                                }
                                                val decrypted = FieldMindExportEncryption.decryptToFile(encryptedFile, importPassword, tempDir)
                                                decryptedTempFile = decrypted
                                                encryptedFile.delete()
                                                Uri.fromFile(decrypted)
                                            } else {
                                                importFileUri!!
                                            }
                                            if (isFieldMind) {
                                                val extracted = FieldMindExportMediaPacker.extractPackage(context, uriToRead)
                                                importedPackage = extracted
                                                extracted?.archiveJson ?: run {
                                                    context.contentResolver.openInputStream(uriToRead)?.bufferedReader()?.readText() ?: ""
                                                }
                                            } else {
                                                context.contentResolver.openInputStream(uriToRead)?.bufferedReader()?.readText() ?: ""
                                            }
                                        }
                                        importProgress = 0.6f
                                        importStepText = "Restoring records…"
                                        if (raw.isNotBlank()) {
                                            viewModel.restoreArchiveJson(raw) { result ->
                                                isImporting = false
                                                result.onSuccess { restored ->
                                                    importResult = restored
                                                    showImportResultDialog = true
                                                    importProgress = 1f
                                                }.onFailure { e ->
                                                    showFastSnackbar(snackbar, scope, "Restore failed: ${e.localizedMessage}")
                                                    isImporting = false
                                                }
                                            }
                                        } else {
                                            isImporting = false
                                            showFastSnackbar(snackbar, scope, "Could not read archive file.")
                                        }
                                    } catch (e: Exception) {
                                        isImporting = false
                                        showFastSnackbar(snackbar, scope, "Import failed: ${e.localizedMessage}")
                                    } finally {
                                        decryptedTempFile?.delete()
                                        importedPackage?.let { FieldMindExportMediaPacker.cleanupExtractedPackage(it) }
                                        importedPackage = null
                                        if (!showImportResultDialog) {
                                            importFileUri = null
                                            importFileName = ""
                                            importPreview = null
                                        }
                                    }
                                }
                            }
                        )

                        BackupTab.BACKUP -> BackupTabContent(
                            backupFolderUri = backupFolderUri,
                            onChooseBackupFolder = { backupFolderPickerLauncher.launch(null) },
                            encrypt = backupEncrypt,
                            onEncryptChange = { backupEncrypt = it },
                            password = backupPassword,
                            onPasswordChange = { backupPassword = it },
                            scheduleEnabled = backupScheduleEnabled,
                            onScheduleChange = {
                                backupScheduleEnabled = it
                                settings.setAutoBackupEnabled(it)
                            },
                            scheduleInterval = backupInterval,
                            onScheduleIntervalChange = {
                                backupInterval = it
                                settings.setAutoBackupInterval(it)
                            },
                            lastBackupLabel = lastBackupLabel,
                            entityCounts = mapOf(
                                "Observations" to observations.size,
                                "Notes" to notes.size,
                                "Projects" to projects.size,
                                "Sources" to sources.size
                            ),
                            gpsPrivacy = exportGpsPrivacy,
                            onGpsPrivacyChange = { settings.setExportGpsPrivacy(it) },
                            excludeMedia = exportExcludeMedia,
                            onExcludeMediaChange = { settings.setExportExcludeMedia(it) },
                            onCreateBackup = {
                                scope.launch {
                                    isExporting = true
                                    try {
                                        withContext(Dispatchers.IO) {
                                            // Apply export privacy transforms to backup data
                                            val backupObs = FieldMindExport.applyGpsPrivacy(observations, exportGpsPrivacy)
                                            val backupNotes = if (exportExcludeMedia) FieldMindExport.applyMediaExclusion(notes) else notes
                                            val json = FieldMindExport.archiveJson(backupObs, backupNotes, questions, hypotheses, projects, sources, dataRecords, reports, flashcards)
                                            val dateStamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(Date())
                                            val backupDir = backupDirectory(context)
                                            val baseName = "fieldmind-backup-$dateStamp"
                                            
                                            // Build .fieldmind package — skip media when exclusion is on
                                            val allAttachments = mutableMapOf<Long, List<EvidenceAttachmentEntity>>()
                                            if (!exportExcludeMedia) {
                                                backupObs.forEach { obs ->
                                                    val atts = viewModel.attachmentsForObservation(obs.id).first()
                                                    if (atts.isNotEmpty()) allAttachments[obs.id] = atts
                                                }
                                            }
                                            val packResult = FieldMindExportMediaPacker.buildPackage(
                                                context = context,
                                                archiveJson = json,
                                                observations = backupObs,
                                                notes = backupNotes,
                                                projects = projects,
                                                sources = sources,
                                                attachments = allAttachments,
                                                outputDir = backupDir
                                            )
                                            val fieldmindFile = packResult.packageFile
                                            
                                            if (backupEncrypt && backupPassword.isNotBlank()) {
                                                val encryptedFile = FieldMindExportEncryption.encryptFile(fieldmindFile, backupPassword)
                                                fieldmindFile.delete()
                                                encryptedFile.renameTo(File(backupDir, "$baseName.encrypted"))
                                            }
                                            
                                            // Try to save to the user's chosen folder
                                            val bkpUriStr = backupFolderUri
                                            if (bkpUriStr.isNotBlank()) {
                                                try {
                                                    val bkpUri = android.net.Uri.parse(bkpUriStr)
                                                    val srcFile = if (backupEncrypt && backupPassword.isNotBlank())
                                                        File(backupDir, "$baseName.encrypted")
                                                    else
                                                        fieldmindFile
                                                    val docName = if (backupEncrypt && backupPassword.isNotBlank())
                                                        "$baseName.encrypted"
                                                    else
                                                        "$baseName.fieldmind"
                                                    val createdDoc = createFileInTree(context, bkpUri, "application/octet-stream", docName)
                                                    if (createdDoc != null) {
                                                        context.contentResolver.openOutputStream(createdDoc)?.use { out ->
                                                            out.write(srcFile.readBytes())
                                                        }
                                                    }
                                                } catch (_: Exception) { }
                                            }
                                        }
                                        lastBackupRefresh++
                                        showFastSnackbar(snackbar, scope, "Backup saved")
                                    } catch (e: Exception) {
                                        showFastSnackbar(snackbar, scope, "Backup failed: ${e.localizedMessage}")
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ── Password prompt dialog ──
    if (showPasswordPrompt) {
        AlertDialog(
            onDismissRequest = {
                showPasswordPrompt = false
                importFileUri = null
                importFileName = ""
            },
            icon = { Icon(FieldMindIcons.Lock, null, size = 28.dp) },
            title = { Text("Encrypted file", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This backup is password-protected. Enter the password to decrypt it.")
                    OutlinedTextField(
                        value = importPassword,
                        onValueChange = { importPassword = it },
                        label = { Text("Password") },
                        placeholder = { Text("Enter backup password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                        trailingIcon = {
                            if (LocalPrivacyTypingEnabled.current) {
                                PrivacyTypingIndicator()
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPasswordPrompt = false
                        // Parse preview with decryption
                        scope.launch {
                            try {
                                val uri = importFileUri!!
                                val raw = withContext(Dispatchers.IO) {
                                    val tempDir = File(context.cacheDir, "import_decrypt")
                                    tempDir.mkdirs()
                                    val encryptedFile = File(context.cacheDir, "import_encrypted_${System.currentTimeMillis()}")
                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                        encryptedFile.outputStream().use { output -> input.copyTo(output) }
                                    }
                                    val decrypted = FieldMindExportEncryption.decryptToFile(encryptedFile, importPassword, tempDir)
                                    encryptedFile.delete()
                                    val isFieldMind = importFileName.replace(".encrypted", "").endsWith(".fieldmind")
                                    if (isFieldMind) {
                                        val extracted = FieldMindExportMediaPacker.extractPackage(context, Uri.fromFile(decrypted))
                                        importedPackage = extracted
                                        extracted?.archiveJson ?: ""
                                    } else {
                                        decrypted.readText()
                                    }
                                }
                                if (raw.isNotBlank()) {
                                    val preview = FieldMindExport.previewArchiveJson(raw)
                                    importPreview = preview
                                }
                            } catch (e: Exception) {
                                showFastSnackbar(snackbar, scope, "Decryption failed: ${e.localizedMessage}")
                                importPreview = null
                            }
                        }
                    },
                    enabled = importPassword.isNotBlank()
                ) { Text("Decrypt") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordPrompt = false
                    importFileUri = null
                    importFileName = ""
                }) { Text("Cancel") }
            }
        )
    }

    // ── Import result dialog ──
    if (showImportResultDialog && importResult != null) {
        val result = importResult!!
        AlertDialog(
            onDismissRequest = {
                showImportResultDialog = false
                importFileUri = null
                importFileName = ""
                importPreview = null
            },
            icon = { Icon(FieldMindIcons.Check, null, size = 28.dp) },
            title = { Text("Restore complete", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Successfully restored ${result.total} records.")
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${result.observations}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Text("Observations", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("${result.notes}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Text("Notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("${result.projects}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Text("Projects", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.tertiary, size = 18.dp)
                            Text(
                                "Your data has been restored. You may need to refresh the current view.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showImportResultDialog = false
                    importFileUri = null
                    importFileName = ""
                    importPreview = null
                    lastBackupRefresh++
                }) { Text("Done") }
            }
        )
    }

    // ── Share dialog (bottom sheet) ──
    if (showShareDialog) {
        ModalBottomSheet(
            onDismissRequest = { showShareDialog = false },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            ShareDialogContent(
                format = shareDialogFormat,
                onFormatChange = { shareDialogFormat = it },
                entityCounts = mapOf(
                    "Observations" to observations.size,
                    "Notes" to notes.size,
                    "Questions" to questions.size,
                    "Hypotheses" to hypotheses.size,
                    "Projects" to projects.size,
                    "Sources" to sources.size,
                    "Data Records" to dataRecords.size,
                    "Reports" to reports.size,
                    "Flashcards" to flashcards.size
                ),
                formatDescription = exportFormats.find { it.name == shareDialogFormat }?.desc ?: "",
                formatColor = exportFormats.find { it.name == shareDialogFormat }?.color ?: MaterialTheme.colorScheme.primary,
                onShare = {
                    showShareDialog = false
                    scope.launch {
                        isExporting = true
                        try {
                            val dateStamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault()).format(Date())
                            val ext = shareDialogFormat.lowercase().replace("markdown", "md").replace(" ", "")
                            val fileName = "fieldmind-export-$dateStamp.$ext"
                            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                            val exportFile = File(exportDir, fileName)

                            withContext(Dispatchers.IO) {
                                val json = FieldMindExport.archiveJson(observations, notes, questions, hypotheses, projects, sources, dataRecords, reports, flashcards)
                                when (shareDialogFormat) {
                                    "JSON" -> exportFile.writeText(json)
                                    "CSV" -> exportFile.writeText(FieldMindExport.observationsCsv(observations))
                                    "Markdown" -> exportFile.writeText(observations.joinToString("\n\n---\n\n") { FieldMindExport.singleObservationMarkdown(it) })
                                    "HTML" -> {
                                        if (includeMedia) {
                                            val mediaBundle = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                FieldMindExport.ExportMediaBundle.collect(
                                                    context = context,
                                                    observations = observations,
                                                    notes = notes,
                                                    projects = projects,
                                                    sources = sources
                                                )
                                            }
                                            exportFile.writeText(FieldMindExport.pdfReadyHtml(projects, observations, sources, reports, notes = notes, media = mediaBundle))
                                        } else {
                                            exportFile.writeText(FieldMindExport.pdfReadyHtml(projects, observations, sources, reports, notes = notes))
                                        }
                                    }
                                    "PDF" -> {
                                        val bodyText = observations.joinToString("\n") { FieldMindExport.singleObservationMarkdown(it) }
                                        if (includeMedia) {
                                            val mediaBundle = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                FieldMindExport.ExportMediaBundle.collect(
                                                    context = context,
                                                    observations = observations,
                                                    notes = notes,
                                                    projects = projects,
                                                    sources = sources
                                                )
                                            }
                                            val imgBytesArray = mediaBundle.observationImages.values.firstOrNull()
                                                ?.firstOrNull()?.second?.let { decodeBase64FromDataUri(it) }
                                            exportFile.writeBytes(FieldMindExport.simplePdfBytes("FieldMind Export", bodyText, embeddedImageBytes = imgBytesArray))
                                        } else {
                                            exportFile.writeBytes(FieldMindExport.simplePdfBytes("FieldMind Export", bodyText))
                                        }
                                    }
                                    "PNG" -> exportFile.writeBytes(FieldMindExport.dashboardPngBytes(observations, sources, projects, notes))
                                    "SVG" -> exportFile.writeText(FieldMindExport.dashboardSvg(observations, sources, projects, notes))
                                    ".fieldmind" -> {
                                        val result = FieldMindExportMediaPacker.buildPackage(
                                            context = context, archiveJson = json,
                                            observations = observations, notes = notes,
                                            projects = projects, sources = sources,
                                            attachments = emptyMap(), outputDir = exportDir
                                        )
                                        result.packageFile.renameTo(exportFile)
                                    }
                                }
                            }

                            val shareUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                exportFile
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = when (shareDialogFormat) {
                                    "PDF" -> "application/pdf"
                                    "PNG" -> "image/png"
                                    "SVG" -> "image/svg+xml"
                                    "CSV" -> "text/csv"
                                    "HTML" -> "text/html"
                                    "Markdown" -> "text/markdown"
                                    ".fieldmind" -> "application/octet-stream"
                                    else -> "application/json"
                                }
                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share FieldMind Export"))
                        } catch (e: Exception) {
                            showFastSnackbar(snackbar, scope, "Share failed: ${e.localizedMessage}")
                        } finally {
                            isExporting = false
                        }
                    }
                },
                onDismiss = { showShareDialog = false }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Hero Status Card
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun HeroStatusCard(
    lastBackupLabel: String,
    autoBackupEnabled: Boolean,
    autoBackupInterval: String,
    entityCounts: Map<String, Int>
) {
    val colors = FieldMindTheme.colors
    val totalRecords = entityCounts.values.sum()

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon + last backup
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        FieldMindIcons.Archive,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        size = 28.dp
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Data overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "$totalRecords total records • $lastBackupLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    )
                }
                // Auto-backup indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (autoBackupEnabled) colors.positive.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)
                ) {
                    Text(
                        if (autoBackupEnabled) "Auto: $autoBackupInterval" else "Manual",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (autoBackupEnabled) colors.positive
                        else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }

            // Entity count row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                entityCounts.entries.take(5).forEach { (key, value) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            value.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            key.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Tab Pill Selector
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun TabPillSelector(
    activeTab: BackupTab,
    onTabChange: (BackupTab) -> Unit
) {
    val tabs = listOf(
        Triple(BackupTab.EXPORT, FieldMindIcons.Export, "Export"),
        Triple(BackupTab.IMPORT, FieldMindIcons.Download, "Import"),
        Triple(BackupTab.BACKUP, FieldMindIcons.Archive, "Backup")
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEach { (tab, icon, label) ->
                val selected = activeTab == tab
                Surface(
                    onClick = { onTabChange(tab) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            icon = icon,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 18.dp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Export Tab
// ══════════════════════════════════════════════════════════════════════

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ExportTabContent(
    entityCounts: Map<String, Int>,
    context: android.content.Context,
    isExporting: Boolean,
    exportProgress: Float,
    exportStepText: String,
    onExport: (format: String, action: String) -> Unit,
    onChooseFolder: () -> Unit,
    destinationUri: Uri?,
    onSwitchToImport: (() -> Unit)? = null,
    gpsPrivacy: String = "Exact",
    onGpsPrivacyChange: (String) -> Unit = {},
    excludeMedia: Boolean = false,
    onExcludeMediaChange: (Boolean) -> Unit = {},
    clearClipboard: Boolean = true,
    onClearClipboardChange: (Boolean) -> Unit = {}
) {
    val totalEntities = entityCounts.values.sum()
    val colors = FieldMindTheme.colors
    var selectedExportFormat by remember { mutableStateOf(".fieldmind") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── Primary actions ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LargeBackupActionCard("Export / Save Backup", "Create a full .fieldmind, .zip, JSON, or readable export.", FieldMindIcons.Export, Modifier.weight(1f)) { onExport(selectedExportFormat, "save") }
            LargeBackupActionCard("Import / Restore", "Preview and restore .fieldmind, .zip, or JSON archives.", FieldMindIcons.Download, Modifier.weight(1f)) { onSwitchToImport?.invoke() }
        }

        // ── Export formats ──
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(FieldMindIcons.Archive, null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                    Text("Export format", fontWeight = FontWeight.SemiBold)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        ".fieldmind" to "Full backup: data + photos + attachments",
                        ".zip" to "Inspectable full backup package",
                        "JSON" to "Complete archive JSON only",
                        "Markdown" to "Readable notes and observations",
                        "PDF" to "Readable PDF summary"
                    ).forEach { (format, desc) ->
                        ExportFormatChip(format, desc, selectedExportFormat == format) { selectedExportFormat = format }
                    }
                }
            }
        }

        // ── Entity summary ──
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                Column {
                    Text("$totalEntities total records", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("All data included", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── Folder picker ──
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.clickable { onChooseFolder() }
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(FieldMindTheme.colors.data.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Icon(MaterialSymbolIcon("folder"), null, tint = FieldMindTheme.colors.data, size = 22.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Save folder", fontWeight = FontWeight.SemiBold)
                    val folderName = remember(destinationUri) {
                        destinationUri?.let { uri ->
                            val docId = try { android.provider.DocumentsContract.getTreeDocumentId(uri) } catch (_: Exception) { uri.lastPathSegment ?: "" }
                            docId.substringAfter(":").ifBlank { uri.lastPathSegment }
                        }
                    }
                    Text(folderName ?: "Tap to select destination", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(FieldMindIcons.Forward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
            }
        }

        // ── Privacy options ──
        ExportPrivacyOptionsCard(
            gpsPrivacy = gpsPrivacy,
            onGpsPrivacyChange = onGpsPrivacyChange,
            excludeMedia = excludeMedia,
            onExcludeMediaChange = onExcludeMediaChange,
            clearClipboard = clearClipboard,
            onClearClipboardChange = onClearClipboardChange,
            showClearClipboard = true
        )

        // ── Export progress ──
        AnimatedVisibility(visible = isExporting) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(exportStepText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    LinearProgressIndicator(progress = { exportProgress }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)))
                }
            }
        }

        // ── Action buttons ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { onExport(selectedExportFormat, "share") },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                enabled = !isExporting && totalEntities > 0
            ) { Icon(FieldMindIcons.Export, null, size = 18.dp); Spacer(Modifier.width(6.dp)); Text("Share") }
            Button(
                onClick = { onExport(selectedExportFormat, "save") },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                enabled = !isExporting && totalEntities > 0 && destinationUri != null
            ) { Icon(FieldMindIcons.Save, null, size = 18.dp); Spacer(Modifier.width(6.dp)); Text("Save") }
        }

        // ── Import callout ──
        if (onSwitchToImport != null) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = FieldMindTheme.colors.observation.copy(alpha = 0.08f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSwitchToImport() }
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(FieldMindTheme.colors.observation.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            FieldMindIcons.Download,
                            null,
                            tint = FieldMindTheme.colors.observation,
                            size = 26.dp
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Need to restore data?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FieldMindTheme.colors.observation
                        )
                        Text(
                            "Import a .json or .fieldmind backup to recover your observations, notes, and projects.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FilledTonalButton(
                        onClick = { onSwitchToImport() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = FieldMindTheme.colors.observation.copy(alpha = 0.18f)
                        )
                    ) {
                        Icon(FieldMindIcons.Download, null, size = 16.dp)
                        Spacer(Modifier.width(4.dp))
                        Text("Import", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}


@Composable
private fun LargeBackupActionCard(title: String, subtitle: String, icon: MaterialSymbolIcon, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, size = 28.dp)
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExportFormatChip(format: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null) {
        Column(Modifier.widthIn(min = 140.dp, max = 220.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(format, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Privacy options card shared by Export and Backup tabs ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExportPrivacyOptionsCard(
    gpsPrivacy: String,
    onGpsPrivacyChange: (String) -> Unit,
    excludeMedia: Boolean,
    onExcludeMediaChange: (Boolean) -> Unit,
    clearClipboard: Boolean,
    onClearClipboardChange: (Boolean) -> Unit,
    showClearClipboard: Boolean
) {
    val gpsModes = listOf(
        Triple("Exact", "Full coordinates", "Include precise latitude and longitude"),
        Triple("Approximate", "~1 km precision", "Round to 2 decimal places"),
        Triple("Remove", "No location", "Strip all GPS and location text")
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(FieldMindIcons.Lock, null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                Text("Privacy options", fontWeight = FontWeight.SemiBold)
            }

            // GPS precision selector
            Text(
                "GPS precision in export",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gpsModes.forEach { (mode, label, desc) ->
                    val selected = gpsPrivacy == mode
                    Surface(
                        onClick = { onGpsPrivacyChange(mode) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = "$label: $desc. ${if (selected) "Selected" else "Not selected"}"
                        }
                    ) {
                        Column(
                            Modifier.widthIn(min = 100.dp).padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Exclude media toggle
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onExcludeMediaChange(!excludeMedia) }
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Exclude media: ${if (excludeMedia) "enabled" else "disabled"}"
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Exclude media attachments", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        "Photos, audio, and files are not included in the export",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = excludeMedia, onCheckedChange = onExcludeMediaChange)
            }

            // Clear clipboard toggle — only shown on Export tab
            if (showClearClipboard) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onClearClipboardChange(!clearClipboard) }
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Clear clipboard after copy: ${if (clearClipboard) "enabled" else "disabled"}"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Clear clipboard after copy", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "Clears exported text from the clipboard after 60 seconds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = clearClipboard, onCheckedChange = onClearClipboardChange)
                }
            }
        }
    }
}

private fun createFileInTree(context: Context, treeUri: Uri, mimeType: String, displayName: String): Uri {
    val resolver = context.contentResolver
    val treeDocumentId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
    val parentDocumentUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)
    return android.provider.DocumentsContract.createDocument(resolver, parentDocumentUri, mimeType, displayName)
        ?: throw IOException("Could not create $displayName in selected folder")
}

// ══════════════════════════════════════════════════════════════════════
//  Import Tab
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImportTabContent(
    selectedFileUri: Uri?,
    fileName: String,
    preview: FieldMindExport.ArchivePreview?,
    importMode: String,
    onModeChange: (String) -> Unit,
    isImporting: Boolean,
    onPickFile: () -> Unit,
    onClearFile: () -> Unit,
    onImport: () -> Unit    ) {
    val pulsateTransition = rememberInfiniteTransition(label = "pulsate")
    val dashAlpha by pulsateTransition.animateFloat(
        1f, 0.4f,
        infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "dashPulse"
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── File drop zone ──
        if (selectedFileUri == null) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPickFile() }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        FieldMindIcons.Download,
                        null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = dashAlpha),
                        size = 48.dp
                    )
                    Text(
                        "Tap to select a .fieldmind, .zip, or .json archive",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Supports full packages with media, ZIP packages, JSON archives, and encrypted packages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ── File selected preview ──
        if (selectedFileUri != null) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                                .background(FieldMindTheme.colors.positive.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(FieldMindIcons.Archive, null, tint = FieldMindTheme.colors.positive, size = 24.dp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(fileName, fontWeight = FontWeight.SemiBold)
                            preview?.let {
                                Text(
                                    "${it.total} records • ${it.observations} obs, ${it.projects} projects",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onClearFile) {
                            Icon(FieldMindIcons.Close, "Clear", size = 20.dp)
                        }
                    }

                    // ── Entity preview grid ──
                    preview?.let { p ->
                        val items = listOf(
                            "Observations" to p.observations,
                            "Notes" to p.notes,
                            "Questions" to p.questions,
                            "Hypotheses" to p.hypotheses,
                            "Projects" to p.projects,
                            "Sources" to p.sources,
                            "Data Records" to p.dataRecords,
                            "Reports" to p.reports,
                            "Flashcards" to p.flashcards
                        ).filter { it.second > 0 }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items.forEach { (label, count) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            entityIconForLabel(label),
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            size = 14.dp
                                        )
                                        Text(
                                            "$count $label",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // ── Import mode ──
                    Text("Import mode", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "Merge" to "Add as new records, keep existing data",
                            "Replace" to "Clear all existing data before restoring"
                        ).forEach { (mode, desc) ->
                            val selected = importMode == mode
                            Surface(
                                onClick = { onModeChange(mode) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(mode, fontWeight = FontWeight.SemiBold)
                                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    RadioButton(selected = selected, onClick = { onModeChange(mode) })
                                }
                            }
                        }
                    }

                    if (importMode == "Merge") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                        ) {
                            Row(
                                Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.tertiary, size = 18.dp)
                                Text(
                                    "Duplicates (by subject + date) will be skipped",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    // ── Import button ──
                    Button(
                        onClick = onImport,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isImporting && preview != null
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Restoring…")
                        } else {
                            Icon(FieldMindIcons.Download, null, size = 18.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Backup Tab
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun BackupTabContent(
    backupFolderUri: String,
    onChooseBackupFolder: () -> Unit,
    encrypt: Boolean,
    onEncryptChange: (Boolean) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    scheduleEnabled: Boolean,
    onScheduleChange: (Boolean) -> Unit,
    scheduleInterval: String,
    onScheduleIntervalChange: (String) -> Unit,
    lastBackupLabel: String,
    entityCounts: Map<String, Int>,
    onCreateBackup: () -> Unit,
    gpsPrivacy: String = "Exact",
    onGpsPrivacyChange: (String) -> Unit = {},
    excludeMedia: Boolean = false,
    onExcludeMediaChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val colors = FieldMindTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── Folder picker (always .fieldmind format) ──
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.clickable { onChooseBackupFolder() }
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(FieldMindTheme.colors.data.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Icon(MaterialSymbolIcon("folder"), null, tint = FieldMindTheme.colors.data, size = 22.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Backup folder", fontWeight = FontWeight.SemiBold)
                    val folderName = remember(backupFolderUri) {
                        try { android.net.Uri.parse(backupFolderUri).lastPathSegment } catch (_: Exception) { null }
                    }
                    Text(
                        folderName ?: "Tap to choose a folder for backups",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(FieldMindIcons.Forward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
            }
        }

        // ── Format info card ──
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(FieldMindIcons.Archive, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 22.dp)
                Column {
                    Text(".fieldmind format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("All data + media attachments in a single portable package", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f))
                }
            }
        }

        // ── Privacy options ──
        ExportPrivacyOptionsCard(
            gpsPrivacy = gpsPrivacy,
            onGpsPrivacyChange = onGpsPrivacyChange,
            excludeMedia = excludeMedia,
            onExcludeMediaChange = onExcludeMediaChange,
            clearClipboard = false,
            onClearClipboardChange = {},
            showClearClipboard = false
        )

        // ── Backup options ──
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                // Encrypt
                Row(
                    Modifier.fillMaxWidth().clickable { onEncryptChange(!encrypt) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(FieldMindIcons.Lock, null, tint = if (encrypt) colors.warning else MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
                    Column(Modifier.weight(1f)) {
                        Text("Encrypt backup", fontWeight = FontWeight.SemiBold)
                        Text("Password-protect your backup file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = encrypt, onCheckedChange = onEncryptChange)
                }

                if (encrypt) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("Backup password") },
                        placeholder = { Text("Enter a strong password") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                        trailingIcon = {
                            if (LocalPrivacyTypingEnabled.current) {
                                PrivacyTypingIndicator()
                            }
                        }
                    )
                    val strength = remember(password) { FieldMindExportEncryption.PasswordStrength.evaluate(password) }
                    if (password.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { strength.score / 5f },
                                modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(strength.color),
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            Text(strength.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Color(strength.color))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Schedule
                Row(
                    Modifier.fillMaxWidth().clickable { onScheduleChange(!scheduleEnabled) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(FieldMindIcons.Today, null, tint = if (scheduleEnabled) colors.info else MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
                    Column(Modifier.weight(1f)) {
                        Text("Schedule backups", fontWeight = FontWeight.SemiBold)
                        Text("Automatically create .fieldmind packages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = scheduleEnabled, onCheckedChange = onScheduleChange)
                }

                if (scheduleEnabled) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Every 6 hours", "Every 12 hours", "Daily", "Weekly", "Monthly").forEach { interval ->
                            val selected = scheduleInterval == interval
                            Surface(
                                onClick = { onScheduleIntervalChange(interval) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(interval, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // ── Next backup countdown (only when scheduling is enabled) ──
        if (scheduleEnabled) {
            val intervalMs = remember(scheduleInterval) {
                fieldmind.research.app.features.field.data.background.FieldMindBackgroundScheduler.intervalToMillis(scheduleInterval)
            }
            val backupDir = remember { backupDirectory(context) }
            val lastBackupTimeMs = remember(backupDir, lastBackupLabel) {
                backupDir.listFiles { f -> f.isFile && (f.extension == "fieldmind" || f.extension == "encrypted" || f.extension == "json") }
                    ?.maxOfOrNull { it.lastModified() } ?: 0L
            }
            val nextBackupTime = lastBackupTimeMs + intervalMs
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

            // Tick every second to keep the countdown live
            LaunchedEffect(scheduleEnabled, scheduleInterval) {
                while (true) {
                    now = System.currentTimeMillis()
                    kotlinx.coroutines.delay(1000)
                }
            }

            val remainingMs = (nextBackupTime - now).coerceAtLeast(0L)
            val countdownText = when {
                remainingMs <= 0 -> "Due now"
                lastBackupTimeMs == 0L -> "After first backup"
                remainingMs >= 24 * 60 * 60 * 1000 -> {
                    val days = remainingMs / (24 * 60 * 60 * 1000)
                    val hours = (remainingMs % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
                    "${days}d ${hours}h"
                }
                remainingMs >= 60 * 60 * 1000 -> {
                    val hours = remainingMs / (60 * 60 * 1000)
                    val mins = (remainingMs % (60 * 60 * 1000)) / (60 * 1000)
                    "${hours}h ${mins}m"
                }
                else -> {
                    val mins = remainingMs / (60 * 1000)
                    val secs = (remainingMs % (60 * 1000)) / 1000
                    "${mins}m ${secs}s"
                }
            }
            val countdownFraction = if (intervalMs > 0) (1f - remainingMs.toFloat() / intervalMs.toFloat()).coerceIn(0f, 1f) else 0f

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        FieldMindIcons.Timer,
                        null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        size = 22.dp
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (lastBackupTimeMs > 0) "Next backup" else "First backup",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            countdownText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (remainingMs > 0 && lastBackupTimeMs > 0) {
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { countdownFraction },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        }
                    }
                }
            }
        }

        // ── Last backup info ──
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(FieldMindIcons.Archive, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                Column(Modifier.weight(1f)) {
                    Text("Last backup", fontWeight = FontWeight.SemiBold)
                    Text(lastBackupLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (entityCounts.isNotEmpty()) {
                    Text("${entityCounts.values.sum()} records", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ── Create backup button ──
        Button(
            onClick = onCreateBackup,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FieldMindTheme.colors.observation)
        ) {
            Icon(FieldMindIcons.Archive, null, size = 20.dp)
            Spacer(Modifier.width(8.dp))
            Text("Backup now", fontWeight = FontWeight.Bold)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Export History Item
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ExportHistoryItem(record: ExportRecord) {
    val formatColor = exportFormats.find { it.name == record.format }?.color ?: MaterialTheme.colorScheme.primary
    val dateStr = remember(record.exportedAt) {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(record.exportedAt))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(formatColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    exportFormats.find { it.name == record.format }?.icon ?: FieldMindIcons.File,
                    null,
                    tint = formatColor,
                    size = 20.dp
                )
            }
            Column(Modifier.weight(1f)) {
                Text(record.fileName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${record.format} • ${formatFileSize(record.fileSizeBytes)} • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                record.destination,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════��
//  Utility functions
// ══════════════════════════════════════════════════════════════════════

private fun entityIconForLabel(label: String): MaterialSymbolIcon = when (label) {
    "Observations" -> FieldMindIcons.Observation
    "Notes" -> FieldMindIcons.Note
    "Questions" -> FieldMindIcons.Question
    "Hypotheses" -> FieldMindIcons.Hypothesis
    "Projects" -> FieldMindIcons.Project
    "Sources" -> FieldMindIcons.Source
    "Data Records" -> FieldMindIcons.Data
    "Reports" -> FieldMindIcons.Report
    "Flashcards" -> FieldMindIcons.Flashcard
    else -> FieldMindIcons.Info
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes.toFloat() / (1024 * 1024))
}

private fun backupDirectory(context: Context): File = File(context.filesDir, "fieldmind/backups").apply { mkdirs() }

private fun lastBackupSummary(context: Context): String {
    val latest = backupDirectory(context).listFiles { file -> file.isFile && (file.extension == "json" || file.extension == "encrypted" || file.extension == "fieldmind") }
        ?.maxByOrNull { it.lastModified() }
    return latest?.let {
        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(it.lastModified()))
    } ?: "Never"
}

/**
 * Decode a base64 data URI (e.g., "data:image/jpeg;base64,/9j...") back to raw bytes.
 * Used for embedding images in PDF exports where we need to decode the base64
 * that was produced by ExportMediaBundle.collect().
 */
private fun decodeBase64FromDataUri(dataUri: String): ByteArray? {
    return try {
        val commaIdx = dataUri.indexOf(',')
        if (commaIdx < 0) return null
        val base64 = dataUri.substring(commaIdx + 1)
        android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
    } catch (e: Exception) { null }
}

// ══════════════════════════════════════════════════════════════════════
//  Share Dialog Content
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ShareDialogContent(
    format: String,
    onFormatChange: (String) -> Unit,
    entityCounts: Map<String, Int>,
    formatDescription: String,
    formatColor: Color,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val totalEntities = entityCounts.values.sum()
    val pulsateTransition = rememberInfiniteTransition(label = "sharePulse")
    val glowAlpha: Float by pulsateTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shareGlow"
    )

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Handle bar ──
        Box(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .width(40.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
        }

        // ── Header ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(formatColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    exportFormats.find { it.name == format }?.icon ?: FieldMindIcons.File,
                    null,
                    tint = formatColor,
                    size = 26.dp
                )
            }
            Column {
                Text("Share as $format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "$totalEntities records",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Format quick-picker row ──
        Text("Format", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            exportFormats.take(6).forEach { fmt ->
                val isSelected = format == fmt.name
                Surface(
                    onClick = { onFormatChange(fmt.name) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) fmt.color.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (isSelected) BorderStroke(1.5.dp, fmt.color) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            fmt.icon, null,
                            tint = if (isSelected) fmt.color else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 20.dp
                        )
                        Text(
                            fmt.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) fmt.color else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        // Remaining formats in a second row
        if (exportFormats.size > 6) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                exportFormats.drop(6).forEach { fmt ->
                    val isSelected = format == fmt.name
                    Surface(
                        onClick = { onFormatChange(fmt.name) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) fmt.color.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) BorderStroke(1.5.dp, fmt.color) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                fmt.icon, null,
                                tint = if (isSelected) fmt.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 20.dp
                            )
                            Text(
                                fmt.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) fmt.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ── Preview card ──
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(FieldMindIcons.Info, null, tint = formatColor.copy(alpha = glowAlpha), size = 18.dp)
                    Text("Preview", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    entityCounts.entries.filter { it.value > 0 }.take(4).forEach { (key, value) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                value.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = formatColor
                            )
                            Text(
                                key,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
                Text(
                    "$totalEntities total • ${formatDescription.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ── Action buttons ──
        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = formatColor)
            ) {
                Icon(FieldMindIcons.Export, null, size = 18.dp)
                Spacer(Modifier.width(6.dp))
                Text("Share $format")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Legacy Export Studio screen (kept for backward compat)
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ExportStudioContent(
    viewModel: FieldMindViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
    showHeader: Boolean = true,
    onMessage: (String) -> Unit
) {
    // Redirect to new BackupAndRestoreScreen
    BackupAndRestoreScreen(viewModel = viewModel, onBack = {})
}

@Composable
fun BackupExportScreen(viewModel: FieldMindViewModel) {
    BackupAndRestoreScreen(viewModel = viewModel, onBack = {})
}
