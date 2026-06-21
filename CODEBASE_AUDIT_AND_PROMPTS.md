# FieldMind Codebase Audit — Placeholders, Broken Features & Backup Issues

> Generated: June 21, 2026
> DOX chain: `master.md` ← `AGENTS.md` ← `app/AGENTS.md`

---

## Table of Contents

1. [Analysis Overview](#1-analysis-overview)
2. [Prompt A: Fix Duplicate Backup Workers & Consolidate](#2-prompt-a-fix-duplicate-backup-workers--consolidate)
3. [Prompt B: Implement Placeholder Click Handlers in Settings](#3-prompt-b-implement-placeholder-click-handlers-in-settings)
4. [Prompt C: Fix Festive Holiday Effects (Halloween & Valentine's)](#4-prompt-c-fix-festive-holiday-effects-halloween--valentines)
5. [Prompt D: Implement TFLite Species Classification](#5-prompt-d-implement-tflite-species-classification)
6. [Prompt E: Create FieldAttachmentManager (File Not Found)](#6-prompt-e-create-fieldattachmentmanager-file-not-found)
7. [Prompt F: Add Scheduled Auto-Backup via WorkManager](#7-prompt-f-add-scheduled-auto-backup-via-workmanager)
8. [Prompt G: Add Selective Export (Choose Entity Types)](#8-prompt-g-add-selective-export-choose-entity-types)
9. [Prompt H: Add Cloud Backup Integration (Google Drive)](#9-prompt-h-add-cloud-backup-integration-google-drive)
10. [Prompt I: Add Export History Tracking](#10-prompt-i-add-export-history-tracking)
11. [Prompt J: Fix CSV/XLSX Data Import Feature](#11-prompt-j-fix-csvxlsx-data-import-feature)
12. [Prompt K: Add Backup Verification & Integrity Checking](#12-prompt-k-add-backup-verification--integrity-checking)

---

## 1. Analysis Overview

### Critical Issues

| # | Issue | Severity | Location | Description |
|---|-------|----------|----------|-------------|
| 1 | **Duplicate backup workers** | HIGH | `data/background/FieldMindAutoBackupWorker.kt` + `infrastructure/worker/FieldMindBackupWorker.kt` | Two separate workers doing nearly the same job. AutoBackupWorker is correct (uses .fieldmind package), BackupWorker is in wrong package with broken flow collection. |
| 2 | **FieldAttachmentManager missing** | HIGH | `features/field/attachment/FieldAttachmentManager.kt` | Referenced in `app/AGENTS.md` as "File attachment management" but the file doesn't exist at any path discovered. |
| 3 | **Placeholder click handlers** | MEDIUM | `FieldMindSettingsScreen.kt:1491,1663` | Two buttons have empty `{ /* Placeholder */ }` onClick handlers — one for "Data integrity repair" and one unnamed. |
| 4 | **Duplicate `navigateToTab` + startDest pattern** | MEDIUM | `FieldMindNavigation.kt` | Recent fix removed `inclusive = true` but the `restoreState = true` + `saveState = true` combo still needs verification. |
| 5 | **Broken flow collection in BackupWorker** | MEDIUM | `FieldMindBackupWorker.kt` | Uses `.let { ... }` on Flow which never collects; `var result` is never assigned. |

### Placeholder Issues

| # | Issue | Severity | Location | Description |
|---|-------|----------|----------|-------------|
| 6 | **TFLite inference not implemented** | LOW | `SpeciesClassifier.kt:866,886` | `// TODO: Implement actual TFLite Interpreter inference.` |
| 7 | **Halloween effects not implemented** | LOW | `FestiveOverlay.kt:55,200` | `// TODO: Implement Halloween effects` |
| 8 | **Valentine's effects not implemented** | LOW | `FestiveOverlay.kt:59,204` | `// TODO: Implement Valentine's effects` |

### Missing Features

| # | Issue | Severity | Location | Description |
|---|-------|----------|----------|-------------|
| 9 | **No cloud backup** | MEDIUM | System-wide | No Google Drive or other cloud backup integration |
| 10 | **No export history** | LOW | System-wide | No log/recent exports tracking |
| 11 | **No selective export** | LOW | `FieldMindBackupExportScreen.kt` | Only all-or-nothing export — can't pick specific entity types |
| 12 | **No CSV/XLSX data import** | MEDIUM | System-wide | The CSV/XLSX format option is API-response only, not file import |
| 13 | **No backup integrity verification** | LOW | `FieldMindExportMediaPacker.kt` | No checksum verification on import |

---

## 2. Prompt A: Fix Duplicate Backup Workers & Consolidate

```
## Context

The FieldMind codebase has TWO backup worker implementations that do nearly the same thing:

1. `app/src/main/java/fieldmind/research/app/features/field/data/background/FieldMindAutoBackupWorker.kt`
   - Package: `fieldmind.research.app.features.field.data.background`
   - CORRECT: Uses .fieldmind package format with media attachments
   - CORRECT: Reads settings from `FieldMindSettings.getInstance()`
   - CORRECT: Respects `autoBackupEnabled` flag
   - CORRECT: Prunes old backups
   - ISSUE: Only saves to SAF folder or private storage — no backup to external public location

2. `app/src/main/java/fieldmind/research/app/infrastructure/worker/FieldMindBackupWorker.kt`
   - Package: `fieldmind.research.app.worker` (WRONG — doesn't follow project structure!)
   - WRONG: Uses `fieldmind.research.app.worker` package instead of proper hierarchy
   - WRONG: Creates plain `.json` files, not .fieldmind packages (no media)
   - BROKEN: Flow collection pattern is broken:
     ```kotlin
     val observations = dao.observeObservations().let { flow ->
         var result: List<ObservationEntity> = emptyList()
         flow.first()  // This never assigns to result!
     }
     ```
   - MISSING: No respect for user's backup privacy settings
   - MISSING: Saves to `getExternalFilesDir` which may not be accessible on all devices
   - Has companion `WORK_NAME = "fieldmind_auto_backup"` which CONFLICTS with AutoBackupWorker

## Task

1. DELETE `app/src/main/java/fieldmind/research/app/infrastructure/worker/FieldMindBackupWorker.kt` entirely
2. UPDATE `FieldMindAutoBackupWorker.kt` to:
   - Add proper error reporting for each step (archive building, media collection, file writing)
   - Add a result/notification mechanism so the user knows when backup succeeds/fails
   - Ensure it works correctly when the user hasn't set a SAF folder (currently falls back to private storage)
   - Add backup to a shared/external location option (like Downloads folder)
3. UPDATE `FieldMindBackgroundScheduler.kt` to only schedule the correct worker (AutoBackupWorker)
4. UPDATE any references to the old BackupWorker in existing code

## Research References
- WorkManager docs: https://developer.android.com/topic/libraries/architecture/workmanager
- SAF (Storage Access Framework): https://developer.android.com/guide/topics/providers/document-provider
- Android external storage: https://developer.android.com/training/data-storage/shared/media

## Verification
- Confirm compilation succeeds after deletion/refactoring
- Verify only ONE backup worker class exists in the codebase
- Verify all references to `FieldMindBackupWorker` are updated to `FieldMindAutoBackupWorker`
- Confirm `WORK_NAME` is unique

## Files to Touch
- DELETE: `app/src/main/java/fieldmind/research/app/infrastructure/worker/FieldMindBackupWorker.kt`
- EDIT: `app/src/main/java/fieldmind/research/app/features/field/data/background/FieldMindAutoBackupWorker.kt`
- EDIT: `app/src/main/java/fieldmind/research/app/features/field/data/background/FieldMindBackgroundScheduler.kt`
- DELETE: `app/src/main/java/fieldmind/research/app/infrastructure/worker/` directory if empty after deletion
```

---

## 3. Prompt B: Implement Placeholder Click Handlers in Settings

```
## Context

The `FieldMindSettingsScreen.kt` file has two buttons with placeholder click handlers:

### Location 1: Data Integrity Repair (Line ~1491)
```kotlin
onClick = { /* Placeholder: would trigger integrity repair */ }
```
This is in the "Data Integrity" settings section. The user expects this button to actually repair data integrity issues.

### Location 2: Unlabeled placeholder (Line ~1663)
```kotlin
onClick = { /* Placeholder */ }
```
This placeholder doesn't have a description — read the surrounding UI context to determine what feature this button is meant to trigger.

## Task

### For Data Integrity Repair:
1. Read the full `DataIntegritySettingsPage` composable function to understand the context
2. Implement the following integrity checks:
   - Verify all ObservationTagCrossRef entries have valid observationIds and tagIds
   - Verify all ProjectObservationCrossRef entries have valid IDs
   - Check for orphaned evidence attachments (references to deleted observations)
   - Count and report total issues found
3. Display results to the user (e.g., Snackbar or dialog):
   - "X integrity issues found and repaired"
   - Remove duplicates in cross-reference tables
   - Delete orphaned cross-references

### For the unnamed placeholder (Line ~1663):
1. Read the surrounding UI to determine what feature it belongs to
2. Implement the expected behavior
3. If the context is unclear, add a proper TODO with a descriptive comment explaining what should be implemented

## Files to Touch
- `app/src/main/java/fieldmind/research/app/features/field/presentation/screens/FieldMindSettingsScreen.kt`

## Verification
- Confirm both buttons do something when tapped (show feedback, trigger action)
- No more `/* Placeholder */` comments in the file
```

---

## 4. Prompt C: Fix Festive Holiday Effects (Halloween & Valentine's)

```
## Context

In `app/src/main/java/fieldmind/research/app/shared/presentation/theme/festive/FestiveOverlay.kt`, there are four placeholder comments:

- Line 55: `// Placeholder for future Halloween effects (falling leaves, bats, etc.)`
- Line 56: `// TODO: Implement Halloween effects`
- Line 59: `// Placeholder for future Valentine's effects (hearts, rose petals, etc.)`
- Line 60: `// TODO: Implement Valentine's effects`

And the same two comments again at lines 200-205 in a second composable function.

The Christmas overlay is already fully implemented in `ChristmasDecorations.kt` and `SnowfallEffect.kt` — study those files as reference.

## Task

### For Halloween Effects (~October 1-31):
1. Create falling leaves animation (orange, red, brown leaves using Canvas)
2. Add bat silhouettes that fly across the screen
3. Use `Canvas` composable with physics-based animation (gravity for falling leaves)
4. Reference the snowfall animation in `SnowfallEffect.kt` for the animation pattern
5. Color palette: #E65100 (orange), #4E342E (dark brown), #6A1B9A (purple)

### For Valentine's Effects (~February 1-14):
1. Create floating hearts animation
2. Add gentle rose petal fall
3. Use warm pink/red color palette: #E91E63, #FF4081, #FCE4EC
4. Add subtle pulsing glow effect on hearts

### Implementation Pattern (Study from ChristmasDecorations.kt):
- Create `HalloweenOverlay.kt` and `ValentineOverlay.kt` files
- Follow the same composable pattern as `ChristmasDecorations.kt`
- Register the new overlays in `FestiveOverlay.kt` where the placeholders are
- Use the same `Calendar`-based date detection pattern
- Each overlay should be a `@Composable` function that takes `Modifier` and renders Canvas-based animations

## Files to Touch
- CREATE: `app/src/main/java/fieldmind/research/app/shared/presentation/theme/festive/HalloweenOverlay.kt`
- CREATE: `app/src/main/java/fieldmind/research/app/shared/presentation/theme/festive/ValentineOverlay.kt`
- EDIT: `app/src/main/java/fieldmind/research/app/shared/presentation/theme/festive/FestiveOverlay.kt`

## Research References
- Compose Canvas API: https://developer.android.com/jetpack/compose/graphics/draw/canvas
- Compose animation: https://developer.android.com/jetpack/compose/animation
- Reference file: `app/src/main/java/fieldmind/research/app/shared/presentation/theme/festive/ChristmasDecorations.kt`
- Reference file: `app/src/main/java/fieldmind/research/app/shared/presentation/theme/festive/SnowfallEffect.kt`

## Verification
- Compilation succeeds
- No more "TODO: Implement Halloween effects" or "TODO: Implement Valentine's effects" comments
```

---

## 5. Prompt D: Implement TFLite Species Classification

```
## Context

In `app/src/main/java/fieldmind/research/app/features/field/data/vision/SpeciesClassifier.kt`, there are two TODO comments:

- Line 866: `// TODO: Implement actual TFLite Interpreter inference.`
- Line 886: `// TODO: Implement actual TFLite Interpreter inference from a file-path model.`

The app already has the `SpeciesImageAnalyzer.kt` file which does image analysis, and `PerenualSpeciesProvider.kt` which provides species data via API. The TFLite path was designed for offline/on-device species classification but was never implemented.

## Task

1. Read the full `SpeciesClassifier.kt` file (it's ~900 lines) to understand:
   - The existing interface/abstract class structure
   - How classification results are structured
   - How the results feed into the UI
   - What model input/output format is expected

2. Implement TFLite model loading and inference at the two TODO locations:

   ### Location 1 (~Line 866): `classifyImage(context, imageBitmap)`
   - Load TFLite model from assets folder (model file: `species_model.tflite`)
   - Preprocess image: resize to model input dimensions, normalize pixel values
   - Run inference using `Interpreter`
   - Parse output labels from `species_labels.txt` in assets
   - Return results as `List<ClassificationResult>` with confidence scores

   ### Location 2 (~Line 886): `classifyImageFromFile(context, imagePath)`
   - Load image from file path
   - Decode to Bitmap
   - Call the same inference logic from Location 1
   - Return results

3. Create placeholder model files in assets:
   - Create `app/src/main/assets/species_model.tflite` (small dummy model or download script)
   - Create `app/src/main/assets/species_labels.txt` with common species names
   - Add graceful fallback if model files don't exist (show error message)

4. Add TFLite dependency to `gradle/libs.versions.toml`:
   - `org.tensorflow:tensorflow-lite:2.16.1`
   - `org.tensorflow:tensorflow-lite-support:0.4.4` (for image preprocessing)

## Research References
- TFLite Android guide: https://www.tensorflow.org/lite/android
- TFLite Interpreter API: https://www.tensorflow.org/lite/api_docs/java/org/tensorflow/lite/Interpreter
- TFLite GPU delegate: https://www.tensorflow.org/lite/performance/gpu
- Compose integration with ML: https://developer.android.com/ml

## Files to Touch
- EDIT: `app/src/main/java/fieldmind/research/app/features/field/data/vision/SpeciesClassifier.kt`
- CREATE: `app/src/main/assets/species_model.tflite` (or download script)
- CREATE: `app/src/main/assets/species_labels.txt`
- EDIT: `gradle/libs.versions.toml`

## Verification
- Compilation succeeds with TFLite dependency
- No more "TODO: Implement actual TFLite Interpreter inference" comments
- App gracefully handles missing model files without crashing
```

---

## 6. Prompt E: Create FieldAttachmentManager (File Not Found)

```
## Context

The `app/AGENTS.md` file lists `attachment/` as a data layer package with ownership of "File attachment management." However, the file `app/src/main/java/fieldmind/research/app/features/field/data/attachment/FieldAttachmentManager.kt` does not exist at any discovered path.

The check at `app/src/main/java/fieldmind/research/app/features/field/attachment/FieldAttachmentManager.kt` also returned FILE_DOES_NOT_EXIST.

There IS attachment management code scattered across the codebase:
- `FieldMindExport.kt` has `ExportMediaBundle.collect()` for gathering attachments during export
- `FieldMindExportMediaPacker.kt` has `collectMediaEntries()` for MediaEntry collection
- `EvidenceAttachmentEntity` exists in the database entities
- `FieldMindDao.kt` has attachment queries
- The capture screen has attachment handling logic

## Task

1. Search for all places that handle file attachments (URIs, content URIs, file paths, EvidenceAttachmentEntity)
2. Search for any import of `FieldAttachmentManager` to see where it's expected
3. Create `app/src/main/java/fieldmind/research/app/features/field/data/attachment/FieldAttachmentManager.kt` with:

### Core Functionality:
- `fun saveAttachment(context: Context, sourceUri: Uri, observationId: Long): EvidenceAttachmentEntity`
  - Copy file from source URI to app-private storage
  - Generate a unique filename
  - Create and return EvidenceAttachmentEntity with localPath set
  - Detect MIME type from content resolver or file extension

- `fun deleteAttachmentFile(attachment: EvidenceAttachmentEntity): Boolean`
  - Delete the physical file at localPath
  - Update attachment status to "Deleted"

- `fun getAttachmentUri(attachment: EvidenceAttachmentEntity): Uri`
  - Return FileProvider URI or content URI for sharing/display

- `suspend fun copyAttachmentsBetweenObservations(context: Context, fromObsId: Long, toObsId: Long)`
  - Copy all attachments from one observation to another (for merge/duplicate operations)

- `fun getStorageUsage(context: Context): Long`
  - Calculate total bytes used by all attachment files in app storage

- `fun orphanedAttachments(dao: FieldMindDao): Flow<List<EvidenceAttachmentEntity>>`
  - Find attachments whose observation no longer exists (for cleanup)

4. Add FileProvider entry to `AndroidManifest.xml` if not already present:
   ```xml
   <provider
       android:name="androidx.core.content.FileProvider"
       android:authorities="${applicationId}.provider"
       android:exported="false"
       android:grantUriPermissions="true">
       <meta-data
           android:name="android.support.FILE_PROVIDER_PATHS"
           android:resource="@xml/file_paths" />
   </provider>
   ```

5. Verify `app/src/main/res/xml/file_paths.xml` exists and includes paths for attachments

## Research References
- FileProvider: https://developer.android.com/reference/androidx/core/content/FileProvider
- Android content resolver: https://developer.android.com/training/secure-file-sharing
- Storage best practices: https://developer.android.com/training/data-storage/app-specific

## Files to Touch
- CREATE: `app/src/main/java/fieldmind/research/app/features/field/data/attachment/FieldAttachmentManager.kt`
- EDIT: `app/AGENTS.md` (update to reflect actual file structure)
- VERIFY: `app/src/main/res/xml/file_paths.xml`

## Verification
- File exists and compiles
- Can save, delete, and query attachments
- All existing attachment-related code still works
```

---

## 7. Prompt F: Add Scheduled Auto-Backup via WorkManager

```
## Context

The `FieldMindAutoBackupWorker.kt` is properly defined but the scheduling mechanism in `FieldMindBackgroundScheduler.kt` needs verification and improvement. The user can toggle auto-backup in Settings but the periodic WorkManager schedule may not be properly connected.

Current state:
- `FieldMindSettings.kt` has `autoBackupEnabled` and `autoBackupInterval` settings 
- `FieldMindAutoBackupWorker.kt` does the actual backup work
- `FieldMindBackgroundScheduler.kt` should schedule it periodically
- The Settings screen toggles `settings.setAutoBackupEnabled(it)` but may not call the scheduler

## Task

1. Read `FieldMindBackgroundScheduler.kt` fully
2. Read the backup section in `FieldMindSettingsScreen.kt` (BackupTabContent composable)
3. Ensure the following flow works:

### When user enables auto-backup in Settings → Backup tab:
- Call `FieldMindBackgroundScheduler.scheduleAutoBackup(context)` with the selected interval
- Update WorkManager periodic work request with new interval when changed
- Show confirmation Snackbar: "Auto-backup scheduled: {interval}"

### When user disables auto-backup:
- Call `FieldMindBackgroundScheduler.cancelAutoBackup(context)`
- Cancel the periodic WorkManager work
- Show confirmation: "Auto-backup canceled"

### For the scheduler itself:
- Use `PeriodicWorkRequestBuilder` with the appropriate interval
- Convert interval strings ("Every 6 hours", "Daily", "Weekly", "Monthly") to appropriate `Duration` values
- Set constraints: `NetworkType.CONNECTED` (requires network? or not, since SAF works offline)
- Add a unique work name constraint so only one periodic job runs
- Use `ExistingPeriodicWorkPolicy.UPDATE` when re-scheduling with new interval

### Add a "Run backup now" button:
- Schedule one-time `OneTimeWorkRequest` to trigger backup immediately
- Show progress indicator while it runs
- Show result via Snackbar

## Research References
- WorkManager periodic work: https://developer.android.com/topic/libraries/architecture/workmanager/how-to/define-work
- WorkManager constraints: https://developer.android.com/reference/androidx/work/Constraints
- Kotlin Duration API: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/

## Files to Touch
- EDIT: `app/src/main/java/fieldmind/research/app/features/field/data/background/FieldMindBackgroundScheduler.kt`
- EDIT: `app/src/main/java/fieldmind/research/app/features/field/presentation/screens/FieldMindSettingsScreen.kt` (BackupTabContent section)
- VERIFY: `app/src/main/java/fieldmind/research/app/features/field/data/settings/FieldMindSettings.kt` (auto-backup settings exist)

## Verification
- Enabling auto-backup in Settings creates a periodic WorkManager job
- Disabling cancels it
- "Run backup now" button works
- Changing interval updates the schedule
```

---

## 8. Prompt G: Add Selective Export (Choose Entity Types)

```
## Context

The current export flow (`FieldMindBackupExportScreen.kt` → `ExportTabContent`) always exports ALL entity types. Users may want to export only specific types (e.g., only observations and notes, excluding flashcards and data records).

Current entity types available:
- Observations
- Notes
- Questions
- Hypotheses
- Projects
- Sources
- Data Records
- Reports
- Flashcards
- Species
- Weather Catalog
- Research Sessions
- Tasks

## Task

1. Read `ExportTabContent` in `FieldMindBackupExportScreen.kt`
2. Read how the export data is collected in the `onExport` lambda (lines ~560-680)
3. Add a "Select what to export" section:

### UI Changes:
- Add an expandable card in `ExportTabContent` titled "Export scope"
- Pre-check all boxes by default
- Each entity type gets a checkbox row with:
  - Entity type icon + name
  - Entity count from current data
  - Checkbox toggle
- Add "Select all" / "Deselect all" quick actions
- Show a summary: "Exporting {n} of {total} entity types ({count} records)"

### Data Changes (onExport lambda):
- Pass a `Set<String>` of enabled entity types to the export builder
- Only call `FieldMindExport.archiveJson()` with the selected entity lists
- For .fieldmind packages, only include media for selected entity types
- Show export size estimate based on selection

### Persistence:
- Save the last selection to `FieldMindSettings` so the user doesn't have to re-select every time
- Add `lastExportSelection: Set<String>` to settings

## Files to Touch
- EDIT: `app/src/main/java/fieldmind/research/app/features/field/presentation/screens/FieldMindBackupExportScreen.kt`
- EDIT: `app/src/main/java/fieldmind/research/app/features/field/data/settings/FieldMindSettings.kt`
- EDIT: `app/src/main/java/fieldmind/research/app/features/field/data/export/FieldMindExport.kt` (if needed)

## Verification
- User can uncheck entity types and verify they're excluded from export
- "Select all" works
- Selection persists between export sessions
- FieldMind package excludes deselected entities and their media
```

---

## 9. Prompt H: Add Cloud Backup Integration (Google Drive)

```
## Context

FieldMind currently only supports local backups (SAF folder or private storage). There is no cloud backup option. Users who lose their device would lose all data.

The app already has Google Play Services dependency (for location), and the architecture supports adding Google Drive API for cloud backups.

## Task

1. Use the Gravity Index tool (`gravity_index`) to search for Google Drive API integration options
2. Choose between:
   - **Google Drive REST API** (full control, requires OAuth)
   - **Android Backup Service** (automatic, limited control)
   - **Google Drive SAF** (user picks Drive folder via SAF)
3. Add cloud backup as an OPTION (never force it — some users want local-only)

### Implementation (if using Google Drive REST API):
- Add `com.google.android.gms:play-services-auth` and Google Drive API dependencies
- Add OAuth consent screen configuration for `https://www.googleapis.com/auth/drive.file` scope
- Create `FieldMindCloudBackupManager.kt` with:
  - `fun signIn(activity: Activity)` — launch Google Sign-In
  - `fun uploadBackup(context: Context, backupFile: File): Result<String>` — upload to Drive
  - `fun listBackups(context: Context): List<CloudBackupEntry>` — list available backups
  - `fun downloadBackup(context: Context, backupId: String): File` — download and restore
  - `fun deleteBackup(backupId: String)` — delete a cloud backup
- Add "Google Drive" tab/option in the Backup tab of BackupExportScreen
- Show: "Last cloud backup: never" / "Last cloud backup: {date}"
- Add "Back up to Drive" and "Restore from Drive" buttons
- Add a confirmation dialog before restoring: "This will overwrite all local data. Continue?"

### Settings:
- Add to `FieldMindSettings.kt`:
  - `cloudBackupEnabled: StateFlow<Boolean>`
  - `lastCloudBackupTimestamp: StateFlow<Long>`
  - `cloudBackupInterval: StateFlow<String>` (for auto cloud backup)

### Auto Cloud Backup:
- Add option in Backup tab: "Auto cloud backup"
- Same interval options as local auto-backup
- Schedule via WorkManager with `NetworkType.CONNECTED` constraint

## Files to Touch
- CREATE: `app/src/main/java/fieldmind/research/app/features/field/data/backup/FieldMindCloudBackupManager.kt`
- EDIT: `app/src/main/java/fieldmind/research/app/features/field/data/settings/FieldMindSettings.kt`
- EDIT: `app/src/main/java/fieldmind/research/app/features/field/presentation/screens/FieldMindBackupExportScreen.kt`
- EDIT: `gradle/libs.versions.toml`

## Research References
- Google Drive API for Android: https://developers.google.com/drive/api/guides/android
- Google Sign-In: https://developers.google.com/identity/sign-in/android
- OAuth scopes: https://developers.google.com/identity/protocols/oauth2/scopes

## Verification
- User can sign in with Google account
- Backup file is uploaded to Drive
- Backup can be downloaded and restored
- Auto cloud backup works on schedule
- Works offline gracefully (queues for next connectivity)
```

---

## 10. Prompt I: Add Export History Tracking

```
## Context

There is no record of past exports. Users don't know when they last exported, what format they used, or where files were saved. Adding export history improves user confidence and helps track backup cadence.

## Task

1. Create an export history data model and persistence layer:

### Data Model (`ExportHistoryEntry`):
```kotlin
data class ExportHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val format: String,           // "JSON", ".fieldmind", "HTML", etc.
    val fileName: String,
    val fileSizeBytes: Long,
    val entityCounts: Map<String, Int>,
    val exportedAt: Long = System.currentTimeMillis(),
    val destination: String,       // "Local folder", "Share", "Drive"
    val success: Boolean,
    val errorMessage: String = ""
)
```

### Storage:
- Store in `FieldMindSettings.kt` as JSON-serialized list in SharedPreferences
- Keep last 50 entries (auto-prune oldest)
- Add methods:
  - `addExportEntry(entry: ExportHistoryEntry)`
  - `exportHistory: Flow<List<ExportHistoryEntry>>`
  - `clearExportHistory()`

### UI:
- Add "Export History" section/card in the Export tab of BackupExportScreen
- Show as a list: date, format, file size, success/fail badge
- Tap to expand: show entity counts, destination, file name
- "Clear history" button
- Auto-refresh when new export completes

### Integration:
- After each export completes in the `onExport` lambda, create and save an `ExportHistoryEntry`
- On error, save an entry with `success = false` and the error message

## Files to Touch
- EDIT: `app/src/main/java/fieldmind/research/app/features/field/data/settings/FieldMindSettings.kt`
- EDIT: `app/src/main/java/fieldmind/research/app/features/field/presentation/screens/FieldMindBackupExportScreen.kt`

## Verification
- After exporting, an entry appears in export history
- History persists across app restarts
- History capped at 50 entries
- Failed exports are recorded with error message
- "Clear history" works
```

---

## 11. Prompt J: Fix CSV/XLSX Data Import Feature

```
## Context

The "Response format" selector in Weather Settings lets users choose json/csv/xlsx for how the Open-Meteo API responds. The parser (`OpenMeteoProvider.kt`) can read CSV and XLSX API responses. But there is NO feature to import a CSV/XLSX file from the user's device to populate weather data or observations.

Additionally, the Backup & Restore screen only supports importing `.fieldmind`, `.zip`, and `.json` archive files — NOT standalone CSV/XLSX files.

## Task

1. Add CSV file import capability to the Import tab:

### Detection:
- When user picks a file, detect if it's CSV (`.csv`), XLSX (`.xlsx`), or one of the existing formats
- Show appropriate parsing UI based on file type

### CSV Import for Weather Data:
- Parse CSV file with columns matching Open-Meteo CSV API output format
- Map columns to `WeatherCatalogEntity` fields
- Show preview of parsed records before importing
- Allow user to map CSV columns to database fields if auto-detection fails
- Import parsed records into `weatherCatalog` table

### CSV Import for Observations:
- Parse CSV with columns: subject, date, time, category, confidence, location, tags, notes
- Show preview with row count
- Import as `ObservationEntity` records
- Skip duplicates (by subject + date combination)

### Generic CSV Import:
- Add a "generic data import" option that creates `DataRecordEntity` entries from CSV rows
- User picks column for label, value, unit
- Remaining columns stored as notes

### XLSX Support:
- Use Apache POI (`org.apache.poi:poi-ooxml`) for XLSX parsing (check if already in dependencies)
- Parse first sheet only
- Same mapping logic as CSV
- Add dependency if needed

### Settings:
- Add `csvImportColumnMapping` to `FieldMindSettings.kt` (persisted per-session mapping preferences)

## Research References
- OpenCSV library: https://opencsv.sourceforge.net/
- Apache POI for XLSX: https://poi.apache.org/components/spreadsheet/
- Kotlin CSV parsing: https://github.com/doyaaaaaken/kotlin-csv

## Files to Touch
- EDIT: `app/src/main/java/fieldmind/research/app/features/field/presentation/screens/FieldMindBackupExportScreen.kt` (ImportTabContent)
- CREATE: `app/src/main/java/fieldmind/research/app/features/field/data/import/FieldCsvImporter.kt`
- CREATE: `app/src/main/java/fieldmind/research/app/features/field/data/import/FieldXlsxImporter.kt`
- EDIT: `gradle/libs.versions.toml` (add CSV/XLSX parsing libraries)

## Verification
- Can pick a CSV file from device and import weather data
- Can pick a CSV file and import observations
- Column mapping UI works
- XLSX import works
- Duplicate detection works
- Preview shows parsed records before confirming import
```

---

## 12. Prompt K: Add Backup Verification & Integrity Checking

```
## Context

When importing a `.fieldmind` package, the manifest contains a `checksumSha256` field but it's never verified against the actual archive content. This means corrupted or tampered archives could be imported silently.

Currently in `FieldMindExportMediaPacker.kt`:
- `manifest.json` contains `checksumSha256`
- `extractPackage()` reads manifest but doesn't validate checksum
- No integrity verification before import

## Task

1. Add checksum verification to the import flow:

### During .fieldmind extraction (in `FieldMindExportMediaPacker.extractPackage()`):
- Read `manifest.json` and extract the expected checksum
- Re-compute SHA-256 of the actual `archive.json` content
- Compare expected vs actual
- Store verification result in `ExtractedPackage`:
  ```kotlin
  data class ExtractedPackage(
      val archiveJson: String,
      val manifest: String,
      val mediaFiles: List<MediaEntry>,
      val tempDir: File,
      val checksumValid: Boolean,
      val checksumExpected: String,
      val checksumActual: String
  )
  ```

### During import UI (in `FieldMindBackupExportScreen.kt`):
- Show integrity status in the file preview card:
  - ✅ Checksum verified
  - ❌ Checksum mismatch — warn user before importing
  - ⚠️ No checksum (legacy archive) — minor warning

### After import:
- Verify imported record counts match the archive counts
- Show "Integrity check passed: {n} records imported successfully"
- Or show "Warning: Expected {n} records but imported {m}" if there's a mismatch

### Additional integrity checks:
- Validate JSON structure before parsing (catch malformed archives)
- Check that all cross-references (observation-project links, etc.) can be resolved
- Check that all referenced media files exist in the package

## Files to Touch
- EDIT: `app/src/main/java/fieldmind/research/app/features/field/data/export/FieldMindExportMediaPacker.kt`
- EDIT: `app/src/main/java/fieldmind/research/app/features/field/presentation/screens/FieldMindBackupExportScreen.kt`

## Verification
- Importing a valid .fieldmind shows "Checksum verified"
- Modifying archive.json inside a .fieldmind before importing shows "Checksum mismatch" warning
- Legacy archives without checksum show informational notice
- Import still succeeds (with warnings) for mismatched checksums (user can choose to proceed)
```

---

## Execution Priority

| Priority | Prompt | Effort | Impact | Dependency |
|----------|--------|--------|--------|------------|
| 🔴 P0 | A: Fix duplicate backup workers | 2 hours | High | None |
| 🔴 P0 | B: Implement placeholder click handlers | 1 hour | Medium | None |
| 🔴 P0 | E: Create FieldAttachmentManager | 3 hours | High | None |
| 🟡 P1 | F: Add scheduled auto-backup | 3 hours | High | Prompt A |
| 🟡 P1 | K: Add backup verification | 2 hours | Medium | None |
| 🟡 P1 | J: Fix CSV/XLSX import | 4 hours | Medium | None |
| 🟢 P2 | G: Add selective export | 3 hours | Medium | None |
| 🟢 P2 | I: Add export history | 2 hours | Low | None |
| 🟢 P2 | H: Add cloud backup | 8 hours | High | Prompt A, F |
| 🔵 P3 | D: Implement TFLite | 6 hours | Medium | None |
| 🔵 P3 | C: Fix festive effects | 3 hours | Low | None |

---

*End of analysis. Each prompt is designed to be copy-pasted to an AI agent along with the DOX chain instructions: `read master.md → AGENTS.md → app/AGENTS.md → relevant child AGENTS.md → implement → review → commit & push`.*
