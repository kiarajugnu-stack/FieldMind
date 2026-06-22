package fieldmind.research.app.features.field.presentation.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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

// Data models moved to FieldMindBackupExportModels.kt
// BackupTab, ExportScopeType, FormatOption, exportFormats, ExportRecord, ExportHistoryStore, etc.

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
    val species by viewModel.speciesRegistry.collectAsState()
    val weatherCatalog by viewModel.weatherCatalog.collectAsState()
    val researchSessions by viewModel.researchSessions.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

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

    // Export uses the same folder as Backup
    val exportDestinationUri: Uri? = remember(backupFolderUri) {
        backupFolderUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
    }
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
    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictResolutionMode by remember { mutableStateOf("skip") } // skip, merge, replace
    var importStepText by remember { mutableStateOf("") }
    var importProgress by remember { mutableFloatStateOf(0f) }
    var isEncryptedFile by remember { mutableStateOf(false) }
    var importPassword by remember { mutableStateOf("") }
    var showPasswordPrompt by remember { mutableStateOf(false) }

    // Export encryption state
    var exportEncrypt by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }

    // Backup tab state
    val privacyLockEnabled by settings.privacyLockEnabled.collectAsState()
    var backupEncrypt by remember { mutableStateOf(privacyLockEnabled) }
    // Sync encrypt with privacy lock changes
    LaunchedEffect(privacyLockEnabled) { if (privacyLockEnabled && !backupEncrypt) backupEncrypt = true }
    var backupPassword by remember { mutableStateOf("") }
    var backupScheduleEnabled by remember { mutableStateOf(autoBackupEnabled) }
    var backupInterval by remember { mutableStateOf(autoBackupInterval) }
    val autoBackupRetention by settings.autoBackupRetention.collectAsState()
    var backupRetention by remember { mutableStateOf(autoBackupRetention) }
    var backupPasswordConfirm by remember { mutableStateOf("") }
    var passwordsMatch by remember { mutableStateOf(true) }
    var showBackupConfirmation by remember { mutableStateOf(false) }
    var showExportConfirmation by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf("") }
    var pendingExportAction by remember { mutableStateOf("") }
    var pendingExportScope by remember { mutableStateOf("") }

    // Format state for export
    var showShareDialog by remember { mutableStateOf(false) }
    var showSharePreview by remember { mutableStateOf(false) }
    var shareDialogFormat by remember { mutableStateOf(".fieldmind") }
    var sharePreviewFileSize by remember { mutableStateOf("0 KB") }
    var includeMedia by remember { mutableStateOf(false) }

    // Export history state
    val exportHistoryStore = remember { ExportHistoryStore(context) }
    var exportHistory by remember { mutableStateOf(emptyList<ExportRecord>()) }
    LaunchedEffect(lastBackupRefresh) {
        withContext(Dispatchers.IO) {
            exportHistory = exportHistoryStore.load()
        }
    }

    // Folder picker launcher (shared by Export + Backup tabs)
    val backupFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            settings.setBackupFolderUri(uri.toString())
        }
    }

    // File picker launcher (import) - accepts .fieldmind, .zip, .json, .encrypted, and all binary files
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
            contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 8.dp),
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
                    ),
                    onAutoBackupToggle = { settings.setAutoBackupEnabled(it) }
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
                            clearClipboard = false,
                            onClearClipboardChange = {},
                            encrypt = exportEncrypt,
                            onEncryptChange = { exportEncrypt = it },
                            password = exportPassword,
                            onPasswordChange = { exportPassword = it },
                            onExport = { format, action, scopeStr ->
                                pendingExportFormat = format
                                pendingExportAction = action
                                pendingExportScope = scopeStr
                                showExportConfirmation = true
                            },
                            onChooseFolder = { backupFolderPickerLauncher.launch(null) },
                            destinationUri = exportDestinationUri,
                            onSwitchToImport = { activeTab = BackupTab.IMPORT },
                            showSharePreview = showSharePreview,
                            onShowSharePreview = { showSharePreview = it },
                            showConflictDialog = showConflictDialog,
                            onShowConflictDialog = { showConflictDialog = it }
                        )

                        BackupTab.IMPORT -> ImportTabContent(
                            selectedFileUri = importFileUri,
                            fileName = importFileName,
                            importFileSize = importFileSize,
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
                                                // If already decrypted in password prompt, reuse the extracted data
                                                if (importedPackage != null && importedPackage!!.archiveJson.isNotBlank()) {
                                                    importStepText = "Using previously decrypted data…"
                                                    importProgress = 0.5f
                                                    return@withContext importedPackage!!.archiveJson
                                                }
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
                                            viewModel.restoreArchiveJson(
                                                raw,
                                                mediaFiles = importedPackage?.mediaFiles ?: emptyList()
                                            ) { result ->
                                                // Clean up temp files AFTER restore completes (not in finally block)
                                                // to avoid a race where cleanupExtractedPackage deletes media
                                                // files before the async restore coroutine can copy them.
                                                importedPackage?.let { FieldMindExportMediaPacker.cleanupExtractedPackage(it) }
                                                importedPackage = null
                                                decryptedTempFile?.delete()
                                                decryptedTempFile = null

                                                isImporting = false
                                                result.onSuccess { restored ->
                                                    importResult = restored
                                                    showImportResultDialog = true
                                                    importProgress = 1f
                                                }.onFailure { e ->
                                                    showFastSnackbar(snackbar, scope, "Restore failed: ${e.localizedMessage}")
                                                    isImporting = false
                                                    importFileUri = null
                                                    importFileName = ""
                                                    importPreview = null
                                                }
                                            }
                                        } else {
                                            isImporting = false
                                            showFastSnackbar(snackbar, scope, "Could not read archive file.")
                                            // Clean up temp files since restore won't be called
                                            importedPackage?.let { FieldMindExportMediaPacker.cleanupExtractedPackage(it) }
                                            importedPackage = null
                                            decryptedTempFile?.delete()
                                            decryptedTempFile = null
                                            importFileUri = null
                                            importFileName = ""
                                            importPreview = null
                                        }
                                    } catch (e: Exception) {
                                        isImporting = false
                                        showFastSnackbar(snackbar, scope, "Import failed: ${e.localizedMessage}")
                                    } finally {
                                        // Temp file cleanup is now done in the restore callback above to
                                        // avoid a race condition (async restore vs temp dir deletion).
                                        // Here we only reset UI state if the result dialog isn't showing.
                                        if (!showImportResultDialog) {
                                            importFileUri = null
                                            importFileName = ""
                                            importPreview = null
                                        }
                                    }
                                }
                            },
                            showConflictDialog = showConflictDialog,
                            onShowConflictDialog = { showConflictDialog = it }
                        )

                        BackupTab.BACKUP -> BackupTabContent(
                            backupFolderUri = backupFolderUri,
                            onChooseBackupFolder = { backupFolderPickerLauncher.launch(null) },
                            isExporting = isExporting,
                            exportProgress = exportProgress,
                            exportStepText = exportStepText,
                            encrypt = backupEncrypt,
                            onEncryptChange = { backupEncrypt = it },
                            password = backupPassword,
                            passwordConfirm = backupPasswordConfirm,
                            onPasswordConfirmChange = { val newVal = it; backupPasswordConfirm = newVal; passwordsMatch = newVal == backupPassword },
                            passwordsMatch = passwordsMatch,
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
                                                        onCreateBackup = { showBackupConfirmation = true }
                        )
                    }
                }
            }

            // ── Export History Section ──
            item {
                SectionHeader("Recent exports", "Your latest export and backup files")
            }
            if (exportHistory.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(FieldMindIcons.Export, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), size = 24.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "No exports yet. Create your first export above.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                items(exportHistory, key = { it.id }) { record ->
                    ExportHistoryItemCard(
                        record = record,
                        context = context,
                        onShare = {
                            try {
                                val backupDir = File(context.filesDir, "fieldmind/backups")
                                val exportDir = File(context.cacheDir, "exports")
                                val file = (backupDir.listFiles()?.find { it.name == record.fileName }
                                    ?: exportDir.listFiles()?.find { it.name == record.fileName })
                                if (file != null && file.exists()) {
                                    val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    val mimeType = when {
                                        record.fileName.endsWith(".pdf") -> "application/pdf"
                                        record.fileName.endsWith(".png") -> "image/png"
                                        record.fileName.endsWith(".svg") -> "image/svg+xml"
                                        record.fileName.endsWith(".csv") -> "text/csv"
                                        record.fileName.endsWith(".html") -> "text/html"
                                        record.fileName.endsWith(".md") -> "text/markdown"
                                        record.fileName.endsWith(".json") -> "application/json"
                                        else -> "application/octet-stream"
                                    }
                                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                        type = mimeType
                                        putExtra(Intent.EXTRA_STREAM, shareUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }, "Share ${record.fileName}"))
                                }
                            } catch (_: Exception) { }
                        },
                        onDelete = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val backupDir = File(context.filesDir, "fieldmind/backups")
                                    val exportDir = File(context.cacheDir, "exports")
                                    (backupDir.listFiles()?.find { it.name == record.fileName }
                                        ?: exportDir.listFiles()?.find { it.name == record.fileName })?.delete()
                                    exportHistoryStore.remove(record.id)
                                    exportHistory = exportHistoryStore.load()
                                }
                            }
                        }
                    )
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

    // ── Conflict Resolution Dialog ──
    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            icon = { Icon(icon = MaterialSymbolIcon("priority_high"), contentDescription = null, size = 32.dp, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Duplicate records detected") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Some records in the backup file match existing data. How would you like to handle them?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        tonalElevation = 0.dp
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    RadioButton(selected = conflictResolutionMode == "skip", onClick = { conflictResolutionMode = "skip" }, modifier = Modifier.size(20.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Skip duplicates", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Keep existing data, ignore duplicates", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    RadioButton(selected = conflictResolutionMode == "merge", onClick = { conflictResolutionMode = "merge" }, modifier = Modifier.size(20.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Merge & update", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Keep both, update conflicting records", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    RadioButton(selected = conflictResolutionMode == "replace", onClick = { conflictResolutionMode = "replace" }, modifier = Modifier.size(20.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Replace all", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Restore with backup data (deletes existing)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showConflictDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text("Continue import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConflictDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp)
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
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportResultDialog = false
                    importFileUri = null
                    importFileName = ""
                    importPreview = null
                }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    // ── Backup confirmation dialog ──
    if (showBackupConfirmation) {
        BackupConfirmationDialog(
            visible = showBackupConfirmation,
            entityCounts = mapOf(
                "Observations" to observations.size,
                "Notes" to notes.size,
                "Projects" to projects.size,
                "Sources" to sources.size
            ),
            onConfirm = {
                showBackupConfirmation = false
                scope.launch {
                    isExporting = true
                    try {
                        withContext(Dispatchers.IO) {
                            val backupObs = FieldMindExport.applyGpsPrivacy(observations, exportGpsPrivacy)
                            val backupNotes = if (exportExcludeMedia) FieldMindExport.applyMediaExclusion(notes) else notes
                            val evidAttachAll = mutableListOf<EvidenceAttachmentEntity>()
                            if (!exportExcludeMedia) {
                                backupObs.forEach { obs ->
                                    val atts = viewModel.attachmentsForObservation(obs.id).first()
                                    evidAttachAll.addAll(atts)
                                }
                            }
                            val crossRefs = viewModel.collectAllCrossRefs()
                            var settingsJson = viewModel.fieldSettings.toExportJson()
                            settingsJson = viewModel.mergeExtraBackupData(context, settingsJson)
                            val json = FieldMindExport.archiveJson(
                                backupObs, backupNotes, questions, hypotheses, projects, sources,
                                dataRecords, reports, flashcards, species, weatherCatalog,
                                researchSessions, tasks,
                                evidenceAttachments = evidAttachAll,
                                crossReferences = crossRefs,
                                settingsJson = settingsJson
                            )
                            val dateStamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(Date())
                            val backupDir = backupDirectory(context)
                            val baseName = "fieldmind-backup-$dateStamp"
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
                                encryptedFile.renameTo(File(backupDir, "${'$'}baseName.encrypted"))
                            }
                            val bkpUriStr = backupFolderUri
                            if (bkpUriStr.isNotBlank()) {
                                try {
                                    val bkpUri = android.net.Uri.parse(bkpUriStr)
                                    val srcFile = if (backupEncrypt && backupPassword.isNotBlank())
                                        File(backupDir, "${'$'}baseName.encrypted")
                                    else
                                        fieldmindFile
                                    val docName = if (backupEncrypt && backupPassword.isNotBlank())
                                        "${'$'}baseName.encrypted"
                                    else
                                        "${'$'}baseName.fieldmind"
                                    val createdDoc = createFileInTree(context, bkpUri, "application/octet-stream", docName)
                                    if (createdDoc != null) {
                                        context.contentResolver.openOutputStream(createdDoc)?.use { out ->
                                            out.write(srcFile.readBytes())
                                        }
                                    }
                                } catch (_: Exception) { }
                            }
                            val _backupFile = if (backupEncrypt && backupPassword.isNotBlank())
                                File(backupDir, "${'$'}baseName.encrypted")
                            else
                                fieldmindFile
                            exportHistoryStore.add(ExportRecord(
                                format = if (backupEncrypt && backupPassword.isNotBlank()) "Encrypted" else ".fieldmind",
                                fileName = _backupFile.name,
                                fileSizeBytes = _backupFile.length(),
                                exportedAt = System.currentTimeMillis(),
                                destination = "Backup saved",
                                entityCounts = mapOf("Observations" to observations.size, "Notes" to notes.size, "Projects" to projects.size)
                            ))
                        }
                        exportHistory = exportHistoryStore.load()
                        lastBackupRefresh++
                        showFastSnackbar(snackbar, scope, "Backup saved")
                    } catch (e: Exception) {
                        showFastSnackbar(snackbar, scope, "Backup failed: ${'$'}{e.localizedMessage}")
                    } finally {
                        isExporting = false
                    }
                }
            },
            onDismiss = { showBackupConfirmation = false }
        )
    }

    // ── Export confirmation dialog ──
    if (showExportConfirmation) {
        val exportFormat = defaultExportFormat
        val totalCount = observations.size + notes.size + questions.size + projects.size + sources.size
        val estimatedSize = estimateExportSize(exportFormat, observations.size, notes.size, projects.size, sources.size)
        ExportConfirmationDialog(
            visible = showExportConfirmation,
            format = exportFormat,
            entityCount = totalCount,
            estimatedSize = estimatedSize,
            onConfirm = {
                showExportConfirmation = false
                scope.launch {
                    isExporting = true
                    exportProgress = 0f
                    exportStepText = "Building $pendingExportFormat…"
                    try {
                        withContext(Dispatchers.IO) {
                            val format = pendingExportFormat
                            val action = pendingExportAction
                            exportProgress = 0.3f
                            val exportObs = FieldMindExport.applyGpsPrivacy(observations, exportGpsPrivacy)
                            val exportNotes = if (exportExcludeMedia) FieldMindExport.applyMediaExclusion(notes) else notes
                            val evidAttachAll = mutableListOf<EvidenceAttachmentEntity>()
                            if (!exportExcludeMedia) {
                                exportObs.forEach { obs ->
                                    val atts = viewModel.attachmentsForObservation(obs.id).first()
                                    evidAttachAll.addAll(atts)
                                }
                            }
                            val crossRefs = viewModel.collectAllCrossRefs()
                            var settingsJson = viewModel.fieldSettings.toExportJson()
                            settingsJson = viewModel.mergeExtraBackupData(context, settingsJson)
                            val json = FieldMindExport.archiveJson(
                                observations = exportObs, notes = exportNotes,
                                questions = questions, hypotheses = hypotheses,
                                projects = projects, sources = sources,
                                dataRecords = dataRecords, reports = reports,
                                flashcards = flashcards,
                                species = species,
                                weatherCatalog = weatherCatalog,
                                researchSessions = researchSessions,
                                tasks = tasks,
                                evidenceAttachments = evidAttachAll,
                                crossReferences = crossRefs,
                                settingsJson = settingsJson
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
                                "JSON" -> {
                                    if (!exportExcludeMedia) {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            FieldMindExport.ExportMediaBundle.collect(
                                                context = context,
                                                observations = exportObs,
                                                notes = exportNotes,
                                                projects = projects,
                                                sources = sources
                                            )
                                        }
                                    }
                                    exportFile.writeText(json)
                                }
                                "CSV" -> exportFile.writeText(FieldMindExport.observationsCsv(observations))
                                "HTML" -> {
                                    if (!exportExcludeMedia) {
                                        val mediaBundle = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            FieldMindExport.ExportMediaBundle.collect(
                                                context = context,
                                                observations = exportObs,
                                                notes = exportNotes,
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
                                    if (!exportExcludeMedia) {
                                        val mediaBundle = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            FieldMindExport.ExportMediaBundle.collect(
                                                context = context,
                                                observations = exportObs,
                                                notes = exportNotes,
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
                                    if (exportEncrypt && exportPassword.isNotBlank()) {
                                        val encryptedFile = FieldMindExportEncryption.encryptFile(exportFile, exportPassword)
                                        exportFile.delete()
                                        val encryptedName = exportFile.name.replace(".fieldmind", ".encrypted").replace(".zip", ".encrypted")
                                        encryptedFile.renameTo(File(exportFile.parentFile, encryptedName))
                                    }
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
                                    ".fieldmind" -> "application/octet-stream"
                                    ".zip" -> "application/zip"
                                    "JSON" -> "application/json"
                                    "CSV" -> "text/csv"
                                    "Markdown" -> "text/markdown"
                                    "HTML" -> "text/html"
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
                                        throw java.io.IOException("Could not create document in the selected folder")
                                    }
                                } catch (e: Exception) {
                                    throw java.io.IOException("Save failed: ${e.localizedMessage}")
                                }
                            }
                            val _exportFile = if (exportEncrypt && exportPassword.isNotBlank() && format in listOf(".fieldmind", ".zip")) {
                                File(exportDir, exportFile.name.replace(".fieldmind", ".encrypted").replace(".zip", ".encrypted"))
                            } else {
                                exportFile
                            }
                            exportHistoryStore.add(ExportRecord(
                                format = if (exportEncrypt && exportPassword.isNotBlank()) "Encrypted" else format,
                                fileName = _exportFile.name,
                                fileSizeBytes = _exportFile.length(),
                                exportedAt = System.currentTimeMillis(),
                                destination = if (action == "share") "Shared via intent" else "Saved to folder",
                                entityCounts = mapOf("Observations" to observations.size, "Notes" to notes.size, "Projects" to projects.size)
                            ))
                        }
                        exportHistory = exportHistoryStore.load()
                        showFastSnackbar(snackbar, scope, "Export complete")
                    } catch (e: Exception) {
                        showFastSnackbar(snackbar, scope, "Export failed: ${e.localizedMessage}")
                    } finally {
                        isExporting = false
                        exportProgress = 0f
                    }
                }
            },
            onDismiss = { showExportConfirmation = false }
        )
    }

    // ── Share Preview Dialog moved to ExportTabContent ──
    /*if (showSharePreview) {
        AlertDialog(
            onDismissRequest = { showSharePreview = false },
            icon = { Icon(icon = FieldMindIcons.Export, contentDescription = null, size = 32.dp, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Share Data Export") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 0.dp
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Format:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = exportFormats.find { it.name == selectedExportFormat }?.color?.copy(alpha = 0.12f) ?: MaterialTheme.colorScheme.primaryContainer,
                                    tonalElevation = 0.dp
                                ) {
                                    Text(selectedExportFormat, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = exportFormats.find { it.name == selectedExportFormat }?.color ?: MaterialTheme.colorScheme.primary)
                                }
                            }
                            Divider()
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(FieldMindIcons.Data, null, size = 20.dp, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Total Records", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$totalEntities items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(MaterialSymbolIcon("storage"), null, size = 20.dp, tint = FieldMindTheme.colors.observation)
                                Column {
                                    Text("Estimated Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(sharePreviewFileSize, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (includeMedia) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(shape = CircleShape, color = FieldMindTheme.colors.positive.copy(alpha = 0.2f), tonalElevation = 0.dp) {
                                        Icon(MaterialSymbolIcon("check"), null, size = 16.dp, tint = FieldMindTheme.colors.positive, modifier = Modifier.padding(4.dp))
                                    }
                                    Text("Media included", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    Text(
                        "This will create a ${selectedExportFormat.lowercase()} file with all your research data ready to share or backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showSharePreview = false; showShareDialog = true }, shape = RoundedCornerShape(12.dp)) {
                    Text("Continue to share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSharePreview = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp)
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
                                // Collect evidence attachments for v3 archive JSON
                                val evidAttachAll = mutableListOf<EvidenceAttachmentEntity>()
                                observations.forEach { obs ->
                                    val atts = viewModel.attachmentsForObservation(obs.id).first()
                                    evidAttachAll.addAll(atts)
                                }
                                val crossRefs = viewModel.collectAllCrossRefs()
                                var settingsJson = viewModel.fieldSettings.toExportJson()
                                settingsJson = viewModel.mergeExtraBackupData(context, settingsJson)
                                val json = FieldMindExport.archiveJson(
                                    observations, notes, questions, hypotheses, projects, sources,
                                    dataRecords, reports, flashcards, species, weatherCatalog,
                                    researchSessions, tasks,
                                    evidenceAttachments = evidAttachAll,
                                    crossReferences = crossRefs,
                                    settingsJson = settingsJson
                                )
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
    }*/
}

//  Export Tab — redesigned per spec
// ══════════════════════════════════════════════════════════════════════

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ExportTabContent(
    entityCounts: Map<String, Int>,
    context: android.content.Context,
    isExporting: Boolean,
    exportProgress: Float,
    exportStepText: String,
    onExport: (format: String, action: String, scope: String) -> Unit,
    onChooseFolder: () -> Unit,
    destinationUri: Uri?,
    onSwitchToImport: (() -> Unit)? = null,
    gpsPrivacy: String = "Exact",
    onGpsPrivacyChange: (String) -> Unit = {},
    excludeMedia: Boolean = false,
    onExcludeMediaChange: (Boolean) -> Unit = {},
    clearClipboard: Boolean = false,
    onClearClipboardChange: (Boolean) -> Unit = {},
    encrypt: Boolean = false,
    onEncryptChange: (Boolean) -> Unit = {},
    password: String = "",
    onPasswordChange: (String) -> Unit = {},
    showSharePreview: Boolean = false,
    onShowSharePreview: (Boolean) -> Unit = {},
    showConflictDialog: Boolean = false,
    onShowConflictDialog: (Boolean) -> Unit = {}
) {
    val totalEntities = entityCounts.values.sum()
    val colors = FieldMindTheme.colors
    var selectedExportFormat by remember { mutableStateOf(".fieldmind") }
    var exportScope by remember { mutableStateOf("All") }
    val scopeOptions = listOf("All", "Projects", "Observations", "Sources", "Reports")

    // Entity icon mapping
    fun entityIcon(key: String): MaterialSymbolIcon = when (key) {
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

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── Scope selector card ──
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(FieldMindIcons.Category, null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                    Text("Export scope", fontWeight = FontWeight.SemiBold)
                }
                // Scope dropdown
                OptionPickerField(
                    label = "Scope",
                    selected = exportScope,
                    options = scopeOptions,
                    onSelected = { exportScope = it },
                    icon = FieldMindIcons.Category
                )
                // Entity type chips with counts
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    entityCounts.filter { it.value > 0 }.entries.take(9).forEach { (key, value) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(entityIcon(key), null, tint = MaterialTheme.colorScheme.primary, size = 16.dp)
                                Text("$value $key", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                // Include media toggle
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(FieldMindIcons.Camera, null, tint = if (excludeMedia) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary, size = 18.dp)
                    Text("Include media attachments", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Switch(checked = !excludeMedia, onCheckedChange = { onExcludeMediaChange(!it) })
                }
            }
        }

        // ── 4-column format grid ──
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(FieldMindIcons.Archive, null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                    Text("Export format", fontWeight = FontWeight.SemiBold)
                }
                // 4-column icon grid — even spacing with equal-width items
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    exportFormats.chunked(4).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { fmt ->
                                val isSelected = selectedExportFormat == fmt.name
                                Surface(
                                    onClick = { selectedExportFormat = fmt.name },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) fmt.color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = if (isSelected) BorderStroke(1.5.dp, fmt.color) else null
                                ) {
                                    Column(
                                        Modifier.padding(10.dp).heightIn(min = 80.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) fmt.color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(fmt.icon, null, tint = fmt.color, size = 20.dp)
                                        }
                                        Text(fmt.name, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(fmt.desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                        if (isSelected) {
                                            Box(
                                                Modifier.size(18.dp).clip(CircleShape).background(fmt.color),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(FieldMindIcons.Check, null, tint = MaterialTheme.colorScheme.onPrimary, size = 12.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Preview card ──
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
                    Column(Modifier.weight(1f)) {
                        Text("$totalEntities total records", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(selectedExportFormat.let { fmt ->
                            val formatInfo = exportFormats.find { it.name == fmt }
                            val desc = formatInfo?.desc ?: ""
                            "${entityCounts.values.sum()} items • $fmt — $desc"
                        }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                // Estimated size
                val estimatedSize = remember(entityCounts, selectedExportFormat) {
                    val total = entityCounts.values.sum()
                    val bytes = total * when (selectedExportFormat) {
                        ".fieldmind" -> 50_000L
                        ".zip" -> 40_000L
                        "JSON" -> 2_000L
                        "CSV" -> 1_200L
                        "Markdown" -> 3_000L
                        "HTML" -> 4_000L
                        "PDF" -> 5_000L
                        "PNG" -> 150_000L
                        "SVG" -> 30_000L
                        else -> 2_000L
                    }
                    formatFileSize(bytes)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Est. size: $estimatedSize", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Text(
                            selectedExportFormat.let { fmt ->
                                when (fmt) {
                                    ".fieldmind" -> "📦 Full package"
                                    ".zip" -> "🗜 Compressed"
                                    "JSON" -> "📄 Structured"
                                    "CSV" -> "📊 Tabular"
                                    "Markdown" -> "📝 Readable"
                                    "HTML" -> "🌐 Web layout"
                                    "PDF" -> "📑 Document"
                                    "PNG" -> "🖼 Snapshot"
                                    "SVG" -> "🎨 Vector"
                                    else -> "📄 Export"
                                }
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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

        // ── Privacy & encryption ──
        ExportPrivacyOptionsCard(
            gpsPrivacy = gpsPrivacy,
            onGpsPrivacyChange = onGpsPrivacyChange,
            excludeMedia = excludeMedia,
            onExcludeMediaChange = onExcludeMediaChange,
            encrypt = encrypt,
            onEncryptChange = onEncryptChange,
            password = password,
            onPasswordChange = onPasswordChange
        )

        // ── Export progress ──
        AnimatedVisibility(visible = isExporting) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(exportStepText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    LinearProgressIndicator(progress = exportProgress, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)))
                }
            }
        }

        // ── Action buttons ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { 
                    onShowSharePreview(true)
                },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                enabled = !isExporting && totalEntities > 0
            ) { Icon(FieldMindIcons.Export, null, size = 18.dp); Spacer(Modifier.width(6.dp)); Text("Share") }
            Button(
                onClick = { onExport(selectedExportFormat, "save", exportScope) },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                enabled = !isExporting && totalEntities > 0 && destinationUri != null
            ) { Icon(FieldMindIcons.Save, null, size = 18.dp); Spacer(Modifier.width(6.dp)); Text("Save") }
        }

        // ── Import hint button ──
        OutlinedButton(
            onClick = { onSwitchToImport?.invoke() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) { Icon(FieldMindIcons.Download, null, size = 18.dp); Spacer(Modifier.width(6.dp)); Text("Import backup") }
    }
}

// ── Privacy options card shared by Export and Backup tabs ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImportTabContent(
    selectedFileUri: Uri?,
    fileName: String,
    importFileSize: String = "",
    preview: FieldMindExport.ArchivePreview?,
    importMode: String,
    onModeChange: (String) -> Unit,
    isImporting: Boolean,
    onPickFile: () -> Unit,
    onClearFile: () -> Unit,
    onImport: () -> Unit,
    showConflictDialog: Boolean = false,
    onShowConflictDialog: (Boolean) -> Unit = {}
) {


    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── File drop zone ──
        if (selectedFileUri == null) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        RoundedCornerShape(24.dp)
                    )
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
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(fileName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (fileName.endsWith(".encrypted")) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)) {
                                        Text("[Encrypted]", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    }
                                } else {
                                    val badge = when {
                                        fileName.endsWith(".fieldmind") -> "PKG"
                                        fileName.endsWith(".zip") -> "ZIP"
                                        fileName.endsWith(".json") -> "JSON"
                                        else -> fileName.substringAfterLast(".").uppercase()
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) {
                                        Text(badge, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(importFileSize, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                preview?.let {
                                    Text("${it.total} records", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                }
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
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.clickable { onShowConflictDialog(true) }
                        ) {
                            Row(
                                Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(FieldMindIcons.Info, null, tint = MaterialTheme.colorScheme.tertiary, size = 18.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Duplicates (by subject + date) will be skipped",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        "Tap to configure conflict handling",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                                Icon(MaterialSymbolIcon("chevron_right"), null, tint = MaterialTheme.colorScheme.tertiary, size = 18.dp)
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

// ═════════════════════════════════════════════════════════════���════════
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
    passwordConfirm: String = "",
    onPasswordConfirmChange: (String) -> Unit = {},
    passwordsMatch: Boolean = true,
    scheduleEnabled: Boolean,
    onScheduleChange: (Boolean) -> Unit,
    scheduleInterval: String,
    onScheduleIntervalChange: (String) -> Unit,
    lastBackupLabel: String,
    entityCounts: Map<String, Int>,
    onCreateBackup: () -> Unit,
    isExporting: Boolean = false,
    exportProgress: Float = 0f,
    exportStepText: String = "",
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

        // ── Privacy & encryption ──
        ExportPrivacyOptionsCard(
            gpsPrivacy = gpsPrivacy,
            onGpsPrivacyChange = onGpsPrivacyChange,
            excludeMedia = excludeMedia,
            onExcludeMediaChange = onExcludeMediaChange,
            encrypt = encrypt,
            onEncryptChange = onEncryptChange,
            password = password,
            onPasswordChange = onPasswordChange,
            isBackup = true,
            passwordConfirm = passwordConfirm,
            onPasswordConfirmChange = onPasswordConfirmChange,
            passwordsMatch = passwordsMatch
        )

        // ── Backup progress ──
        AnimatedVisibility(visible = isExporting) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(exportStepText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    LinearProgressIndicator(progress = exportProgress, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)))
                }
            }
        }

        // ── Backup options ─��
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
                    Icon(FieldMindIcons.Lock, null, tint = if (encrypt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
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
                    OutlinedTextField(
                        value = passwordConfirm,
                        onValueChange = onPasswordConfirmChange,
                        label = { Text("Confirm password") },
                        placeholder = { Text("Re-enter password") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.withPrivacyTyping(LocalPrivacyTypingEnabled.current),
                        trailingIcon = {
                            if (LocalPrivacyTypingEnabled.current) {
                                PrivacyTypingIndicator()
                            }
                        },
                        isError = passwordConfirm.isNotEmpty() && !passwordsMatch
                    )
                    Spacer(Modifier.height(8.dp))
                    if (passwordConfirm.isNotBlank() || passwordsMatch) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (passwordsMatch) FieldMindIcons.Check else FieldMindIcons.Close,
                                null,
                                tint = if (passwordsMatch) colors.positive else MaterialTheme.colorScheme.error,
                                size = 16.dp
                            )
                            Text(
                                if (passwordsMatch) "Passwords match" else "Passwords do not match",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (passwordsMatch) colors.positive else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
                    Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                        OptionPickerField(
                            label = "Interval",
                            selected = scheduleInterval,
                            options = listOf("Every 6 hours", "Every 12 hours", "Daily", "Weekly", "Monthly"),
                            onSelected = onScheduleIntervalChange,
                            icon = FieldMindIcons.Today
                        )
                    }
                }
            }
        }

        

                // ── Retention duration ──
                HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Keep backups for", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                        Text("Older backups are automatically removed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OptionPickerField(
                        label = "Retention",
                        selected = scheduleInterval,
                        options = listOf("7 days", "30 days", "90 days", "Keep all"),
                        onSelected = { onScheduleIntervalChange(it) },
                        icon = FieldMindIcons.Category
                    )
                }// ── Next backup countdown (only when scheduling is enabled) ──
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

// ═════════════════════════════════════════════════════════════════��════
