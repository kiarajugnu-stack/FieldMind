# FieldMind Backup / Import / Export — Full Redesign Plan

## 1. Current State Analysis

### Existing Infrastructure

| Component | File | Status |
|---|---|---|
| **Export Engine** | `FieldMindExport.kt` | Core engine exists with JSON archive, CSV, Markdown, HTML, PDF, PNG, SVG. Archive format is `fieldmind-archive-v2`. Handles 9 entity types. |
| **Export Studio UI** | `FieldMindBackupExportScreen.kt` | Basic scope picker (All/Projects/Observations/Sources/Reports), restore dialog, hero card. **No actual export buttons.** |
| **Settings Hub** | `FieldMindSettingsScreen.kt` | Has "Backup & import" and "Export Studio" nav cards routing to sub-pages |
| **Backup/Import Settings** | `BackupImportSettingsPage` in settings screen | Auto-backup toggle, interval, export format selector, "Open Export Studio" button |
| **Background Workers** | `FieldMindBackupWorker.kt`, `FieldMindAutoBackupWorker.kt` | JSON archive save to `filesDir/fieldmind/backups/` |
| **Settings Data** | `FieldMindSettings.kt` | `autoBackupEnabled`, `autoBackupInterval`, `attachmentExportMode`, `defaultExportFormat` |
| **Navigation** | `FieldMindNavigation.kt` | Routes exist for Export Studio and BackupImport settings |
| **Bulk Operations** | `FieldBulkOperations.kt` | Bulk delete/archive/tag |

### Supported Entity Types (9 total)
- Observations, Notes, Questions, Hypotheses, Projects, Sources, DataRecords, Reports, Flashcards

### Existing Export Formats
- JSON archive (`fieldmind-archive-v2`)
- CSV (observations, sources, data records)
- Markdown (single observation, project, report, data record)
- HTML (combined export page)
- PDF (simple text-based PDF)
- PNG (dashboard snapshot)
- SVG (dashboard snapshot)

### What's Missing / Broken
1. **No actual export action buttons** in Export Studio — just a hero card with scope selection
2. **No folder chooser** (SAF/document tree) for export destination
3. **No share sheet integration** with beautiful preview
4. **No image/media bundling** in exports — only URI references
5. **No true import flow** — only a bare JSON restore dialog with no file picker
6. **No progress indicators** for large exports
7. **No export history/recent exports list**
8. **No scheduled/manual backup from Export Studio**
9. **No encrypted backups**
10. **No selective export** at individual entity level

---

## 2. Proposed Architecture

### 2.1 New/Modified Files

```
NEW: app/src/main/java/.../field/presentation/screens/BackupAndRestoreScreen.kt
    → Full redesign: backup, import, export in one beautiful screen

NEW: app/src/main/java/.../field/presentation/components/ExportProgressDialog.kt
    → Animated export progress overlay

NEW: app/src/main/java/.../field/presentation/components/SharePreviewDialog.kt
    → Beautiful share preview with format selection and thumbnail

NEW: app/src/main/java/.../field/presentation/components/FolderPickerCard.kt
    → SAF folder picker UI with breadcrumb path display

NEW: app/src/main/java/.../field/presentation/components/ExportHistoryCard.kt
    → Recent exports list with timestamps and file sizes

NEW: app/src/main/java/.../field/data/export/FieldMindExportManager.kt
    → Orchestrator that chains export, zip, share, save steps

NEW: app/src/main/java/.../field/data/export/FieldMindExportMediaPacker.kt
    → Zips media attachments alongside JSON into .fieldmind package

NEW: app/src/main/java/.../field/data/export/FieldMindExportEncryption.kt
    → AES-GCM encryption for password-protected backups

MODIFIED: FieldMindExport.kt
    → Add attachments/images export, add export metadata, add manifest

MODIFIED: FieldMindBackupExportScreen.kt
    → Replace with new redesigned BackupAndRestoreScreen

MODIFIED: FieldMindSettingsScreen.kt
    → Update Backup & Import nav card to point to new screen

MODIFIED: FieldMindNavigation.kt
    → Add new routes for full backup screens

MODIFIED: FieldMindSettings.kt
    → Add settings for encryption, folder path, export history
```

### 2.2 Data Flow

```
User taps "Export" →  
  1. Scope selection (All / Projects / Observations / Sources / Reports / Custom)
  2. Format selection (JSON / CSV / MD / HTML / PDF / PNG / SVG / .fieldmind)
  3. Options: include media? encrypt? folder path?
  4. Preview card shows estimated size, entity count
  5. Export runs with progress animation
  6. Result: Share sheet OR Save to folder

User taps "Import" →
  1. File picker (.json / .fieldmind)
  2. Preview dialog: entity counts, parse validation, date range
  3. Import mode: Merge (add as new) / Replace (remove existing)
  4. Conflict resolution: skip duplicates, overwrite, keep both
  5. Import runs with progress
  6. Result summary dialog

User taps "Backup" →
  1. Full JSON archive + media ZIP → .fieldmind package
  2. Optional encryption with password
  3. Save to chosen folder (SAF)
  4. Scheduled: daily/weekly/monthly via WorkManager
```

---

## 3. UI Design Specifications

### 3.1 Main Backup & Restore Screen

```
┌─────────────────────────────────────┐
│  ← Backup & Restore           [⚙]  │  Header
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐    │
│  │  📦  Last backup: never     │    │  Hero status card
│  │     Auto-backup: OFF        │    │  With gradient background
│  │     [Enable auto-backup]    │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─ 3-tab pill selector ───────┐    │
│  │  [Export] [Import] [Backup]  │    │  Glassmorphic pill tabs
│  └─────────────────────────────┘    │
│                                     │
│  ── Export Tab ──                   │
│  ┌─ Scope card ──────────────────┐  │
│  │  [All ▼]  [Include media ☑]   │  │  Dropdown + toggles
│  │  9 entity types with counts    │  │
│  └─────────────────────────────┘  │
│  ┌─ Format grid ────────────────┐  │
│  │  [📄 JSON] [📊 CSV] [📝 MD]  │  │  4-column icon grid
│  │  [🌐 HTML] [📑 PDF] [🖼 PNG]  │  │  Selected = accent border
│  │  [🎨 SVG] [📦 .fieldmind]     │  │  + checkmark
│  └─────────────────────────────┘  │
│  ┌─ Preview card ───────────────┐  │
│  │  245 obs • 12 projects       │  │  Animated count summary
│  │  Est. size: 4.2 MB           │  │
│  └─────────────────────────────┘  │
│                                     │
│  [  📁 Choose folder  ] [  ↗ Share ]  │  Primary actions
│                                     │
│  ── Import Tab ──                   │
│  ┌─ Drop zone / picker ──────────┐  │
│  │  📂  Tap to select .json      │  │  Dashed border, animated
│  │      or .fieldmind file       │  │
│  └─────────────────────────────┘  │
│  ┌─ Import mode ────────────────┐  │
│  │  ○ Merge (add as new)        │  │  Radio buttons
│  │  ○ Replace (clear & restore) │  │
│  └─────────────────────────────┘  │
│  [  ⬆ Import  ]                   │
│                                     │
│  ── Backup Tab ──                   │
│  ┌─ Backup options ─────────────┐  │
│  │  ☑ Include media attachments  │  │
│  │  ☑ Encrypt (password)        │  │
│  │  [Password input]            │  │
│  │  ☑ Schedule backups          │  │
│  │  [Daily ▼]                   │  │
│  └─────────────────────────────┘  │
│  [  📦 Create backup now  ]       │
│                                     │
│  ── Export History ──              │
│  ┌─ History cards ──────────────┐  │
│  │  📄 2025-06-20 JSON 2.1MB   │  │  Swipe to delete
│  │  📊 2025-06-19 CSV 845KB    │  │  Tap to re-share
│  │  📦 2025-06-18 backup 15MB  │  │
│  └─────────────────────────────┘  │
└─────────────────────────────────────┘
```

### 3.2 Share Preview Dialog

When user taps "Share", a beautiful bottom sheet appears:

```
┌─────────────────────────────────────┐
│  ✕                          Share   │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐    │
│  │    📦 FieldMind Export      │    │  Large icon with glow
│  │                             │    │
│  │  245 observations           │    │  Entity counts
│  │  12 projects                │    │
│  │  8 sources                  │    │
│  │  3 reports                  │    │
│  │                             │    │
│  │  JSON • 4.2 MB • Jun 20    │    │  Format badge
│  └─────────────────────────────┘    │
│                                     │
│  ┌─ Format quick-switch ────────┐   │
│  │  [📄JSON] [📊CSV] [📝MD]     │   │  Horizontal chip row
│  └─────────────────────────────┘   │
│                                     │
│  ┌─ Share via ──────────────────┐   │
│  │  [📱 Bluetooth] [💬 Messages]│   │  System share targets
│  │  [📧 Email] [☁️ Drive]       │   │  (from Android share sheet)
│  │  [💾 Save to device]         │   │
│  └─────────────────────────────┘   │
│                                     │
│  [  Share  ]                        │  Primary action
└─────────────────────────────────────┘
```

### 3.3 Export Progress Dialog

```
┌─────────────────────────────────────┐
│         Exporting...                │
│                                     │
│  ┌─────────────────────────────┐    │
│  │  ◌◌◌◌◌◌◌◌◌◌◌◌◌◌◌◌◌◌◌◌◌◌◌  │    │  Animated indeterminate
│  │        45%                  │    │  + percentage
│  └─────────────────────────────┘    │
│                                     │
│  📄 Exporting observations...       │  Current step text
│  ✅ Observations exported (245)     │  Completed steps
│  ⏳ Packing media attachments...    │  In-progress step
│                                     │
│  [  Cancel  ]                       │
└─────────────────────────────────────┘
```

### 3.4 Import Preview Dialog

When a file is selected for import:

```
┌─────────────────────────────────────┐
│  ⬆ Restore from backup             │
├─────────────────────────────────────┤
│  ┌─ File info ──────────────────┐   │
│  │  📄 fieldmind-archive.json   │   │  File name + icon
│  │  4.2 MB • Jun 20, 2025      │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─ Entity preview ─────────────┐   │
│  │  📝 245 observations          │   │  Beautiful stats grid
│  │  📋 12 projects              │   │
│  │  📚 8 sources                │   │
│  │  📄 3 reports                │   │
│  │  🗂 45 notes                 │   │
│  │  📊 67 data records          │   │
│  │  ❓ 23 questions             │   │
│  │  🔬 5 hypotheses            │   │
│  │  🃏 15 flashcards            │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─ Import mode ────────────────┐   │
│  │  ○ Merge — add as new records│   │  Radio with description
│  │  ● Replace — clear all       │   │
│  │    existing data first       │   │
│  └─────────────────────────────┘   │
│                                     │
│  ⚠️ "Merge will skip 12 duplicate  │  Warning banner
│     observations (by subject+date)" │
│                                     │
│  [  Cancel  ]  [  ⬆ Restore  ]     │
└─────────────────────────────────────┘
```

---

## 4. Implementation Phases

### Phase 1: Core Export — Export Studio Redesign (Priority: High)
**Files:** `BackupAndRestoreScreen.kt` (new), `FieldMindExportManager.kt` (new)

- [ ] Redesign Export Studio as a 3-tab screen (Export / Import / Backup)
- [ ] Add actual export action buttons (currently none exist)
- [ ] Add SAF folder picker via `ActivityResultContracts.OpenDocumentTree()`
- [ ] Add scoped export: All / Projects / Observations / Sources / Reports + entity detail counts
- [ ] Add format grid selector with live preview
- [ ] Add export progress dialog with step-by-step animation
- [ ] Wire export actions to `FieldMindExport` engine
- [ ] Add share intent with `FileProvider` URI
- [ ] Replace old `ExportStudioContent` with new implementation

### Phase 2: Media & Package Export (Priority: High)
**Files:** `FieldMindExportMediaPacker.kt` (new), `FieldMindExport.kt` (modified)

- [ ] Create `.fieldmind` package format (ZIP containing archive.json + media folder)
- [ ] Media attachment resolution: copy from `content://` URIs to temp files
- [ ] Manifest file inside package with version, date, checksum
- [ ] Update `archiveJson` to include media manifest URIs
- [ ] Add estimated size calculation before export

### Phase 3: Encrypted Backups (Priority: Medium)
**Files:** `FieldMindExportEncryption.kt` (new)

- [ ] AES-256-GCM encryption for `.fieldmind` packages
- [ ] Password input UI in backup options
- [ ] Password confirmation with strength meter
- [ ] Decryption prompt on import
- [ ] Store password hint (optional)

### Phase 4: Import Flow Redesign (Priority: High)
**Files:** `BackupAndRestoreScreen.kt` (modified)

- [ ] Add file picker for `.json` and `.fieldmind` archives
- [ ] Parse and display preview before import
- [ ] Import mode selection: Merge vs Replace
- [ ] Duplicate detection (by subject+date, by hash)
- [ ] Import progress with step-by-step feedback
- [ ] Result summary dialog with counts
- [ ] Conflict resolution UI

### Phase 5: Auto-Backup Scheduler (Priority: Medium)
**Files:** `FieldMindSettings.kt` (modified), `FieldMindBackupWorker.kt` (modified)

- [ ] Upgrade auto-backup to `.fieldmind` package format
- [ ] Add folder selection for backup destination
- [ ] Retention policy: keep last N backups
- [ ] Auto-backup status in hero card
- [ ] Manual trigger from Backup tab
- [ ] Backup history list

### Phase 6: Export History & Management (Priority: Low)
**Files:** `ExportHistoryCard.kt` (new)

- [ ] Track export history in SharedPreferences/Room
- [ ] Display recent exports with file name, size, format, date
- [ ] Tap to re-share, swipe to delete local file
- [ ] "Open folder" button to navigate to export directory

### Phase 7: Beautiful Share Dialog (Priority: Medium)
**Files:** `SharePreviewDialog.kt` (new)

- [ ] Bottom sheet with format preview card
- [ ] Quick format switch chips
- [ ] System share sheet integration
- [ ] File size and entity summary

### Phase 8: Settings Integration (Priority: High)
**Files:** `FieldMindSettingsScreen.kt` (modified), `FieldMindNavigation.kt` (modified)

- [ ] Update "Backup & Import" nav card → "Backup & Restore" with new screen
- [ ] Add "Export Studio" as quick action from settings
- [ ] Add default export folder preference
- [ ] Add default format preference
- [ ] Add encryption preferences
- [ ] Add export history settings

---

## 5. UI Component Tree

```
BackupAndRestoreScreen
├── StandardScreenHeader ("Backup & Restore")
├── HeroStatusCard
│   ├── Last backup timestamp
│   ├── Auto-backup toggle
│   └── Gradient accent background
├── TabPillSelector (Export | Import | Backup)
│
├── ExportTab
│   ├── ScopeSelectorCard
│   │   ├── Dropdown: All / Projects / Obs / Sources / Reports
│   │   ├── Entity count chips (live counts from ViewModel)
│   │   └── "Include media attachments" toggle
│   ├── FormatGrid
│   │   └── 8 format cards in 4×2 grid with icon, label, desc, selected state
│   ├── ExportPreviewCard
│   │   ├── Animated count summary
│   │   ├── Estimated file size
│   │   └── Format description
│   ├── FolderPickerCard
│   │   ├── SAF document tree launcher
│   │   └── Breadcrumb path display
│   └── ActionRow
│       ├── "Export & Save" button
│       └── "Export & Share" button
│
├── ImportTab
│   ├── FileDropZone
│   │   └── Dashed border, tap to pick .json/.fieldmind
│   ├── ImportPreviewCard (shown after file selected)
│   │   ├── File info (name, size, date)
│   │   ├── Entity type grid with counts
│   │   └── Date range
│   ├── ImportModeSelector
│   │   ├── Merge radio (add as new)
│   │   ├── Replace radio (clear & restore)
│   │   └── Warning banner about duplicates
│   └── ImportButton
│
├── BackupTab
│   ├── BackupOptionsCard
│   │   ├── Include media toggle
│   │   ├── Encrypt toggle
│   │   ├── Password field (with strength meter)
│   │   ├── Schedule toggle
│   │   └── Interval dropdown (Daily/Weekly/Monthly)
│   ├── BackupHistoryCard
│   │   ├── "Last backup: never" status
│   │   └── Recent backups list
│   └── "Create Backup Now" button
│
└── ExportHistorySection
    ├── SectionHeader "Recent exports"
    ├── HistoryItem × N
    │   ├── Format icon
    │   ├── File name + size + date
    │   └── Share / Delete action icons
    └── "Clear history" text button
```

---

## 6. Data Model Additions

### ExportOptions data class (new)
```kotlin
data class ExportOptions(
    val scope: ExportScope = ExportScope.ALL,
    val format: ExportFormat = ExportFormat.JSON,
    val includeMedia: Boolean = false,
    val encrypt: Boolean = false,
    val password: String = "",
    val destination: Uri? = null,  // SAF tree URI
    val shareAfterExport: Boolean = false
)

enum class ExportScope { ALL, PROJECTS, OBSERVATIONS, SOURCES, REPORTS, CUSTOM }
enum class ExportFormat { JSON, CSV, MARKDOWN, HTML, PDF, PNG, SVG, FIELD_MIND_PACKAGE }
```

### ExportRecord data class (new)
```kotlin
data class ExportRecord(
    val id: String = UUID.randomUUID().toString(),
    val format: ExportFormat,
    val fileName: String,
    val fileSizeBytes: Long,
    val entityCounts: Map<String, Int>,
    val exportedAt: Long = System.currentTimeMillis(),
    val destination: String = "",  // "Share" or file path
    val success: Boolean = true
)
```

### Settings additions (FieldMindSettings.kt)
```kotlin
val defaultExportFolder: StateFlow<String>  // SAF URI string
val exportHistory: StateFlow<List<ExportRecord>>
val backupEncryptionEnabled: StateFlow<Boolean>
val backupRetentionCount: StateFlow<Int>  // keep last N
```

---

## 7. Key UX Principles

1. **Offline-first** — All export/import runs locally. No server dependency.
2. **Confidence through preview** — Always show what will be exported/imported before action.
3. **Progress transparency** — Animated steps with entity count. No frozen screens.
4. **Format flexibility** — Let users choose the right format for their use case.
5. **Privacy by default** — Media not included unless opted in. Encryption available.
6. **Beautiful at every step** — Even the progress dialog and share sheet should feel polished.
7. **Action feedback** — Haptic confirmation, snackbar toasts, result dialogs.
8. **Recovery** — Export history lets users re-share recent exports.

---

## 8. Navigation Updates

```kotlin
// New routes
data object BackupRestore : FieldMindScreen("field_backup_restore", "Backup & Restore", FieldMindIcons.Archive)
data object BackupRestoreExport : FieldMindScreen("field_backup_restore_export", "Export", FieldMindIcons.Export)
data object BackupRestoreImport : FieldMindScreen("field_backup_restore_import", "Import", FieldMindIcons.Download)
data object BackupRestoreScheduler : FieldMindScreen("field_backup_restore_schedule", "Scheduled Backup", FieldMindIcons.Today)

// Updated settings nav card
"Backup & Restore" → BackupRestore route (replaces old BackupImport)
"Export Studio" → BackupRestoreExport route (quick access to export tab)
```

---

## 9. Key Technical Decisions

| Decision | Choice | Rationale |
|---|---|---|
| File picker | SAF `OpenDocumentTree()` | Persistent permissions, user chooses location |
| Package format | ZIP archive (.fieldmind) | Universal, compressed, supports media |
| Encryption | AES-256-GCM | Hardware-accelerated on Android, authenticated |
| Encrypted format | Encrypted ZIP entries | Maintains structure while encrypted |
| Media handling | Copy to temp → bundle | Avoid modifying originals, clean up after |
| Large exports | Background coroutine | UI stays responsive, progress updates via StateFlow |
| Share | `FileProvider` + `Intent.ACTION_SEND` | Standard Android sharing, works everywhere |
| Export history | Room database table | Survives app restarts, queryable |
| Duplicate detection | Hash (SHA-256 of subject+date+notes) | Fast, reliable for merge conflicts |

---

## 10. Summary of Changes by File

| File | Action | Change |
|---|---|---|
| `BackupAndRestoreScreen.kt` | **NEW** | 3-tab backup/import/export screen with full UI |
| `ExportProgressDialog.kt` | **NEW** | Animated progress overlay composable |
| `SharePreviewDialog.kt` | **NEW** | Bottom sheet share preview with format switch |
| `FolderPickerCard.kt` | **NEW** | SAF folder picker with path breadcrumb |
| `ExportHistoryCard.kt` | **NEW** | Recent exports list component |
| `FieldMindExportManager.kt` | **NEW** | Export orchestration, progress, error handling |
| `FieldMindExportMediaPacker.kt` | **NEW** | Media ZIP packing for .fieldmind format |
| `FieldMindExportEncryption.kt` | **NEW** | AES-GCM encryption/decryption |
| `FieldMindExport.kt` | **MODIFY** | Add media manifest, export metadata, manifest |
| `FieldMindBackupExportScreen.kt` | **DELETE** | Replaced by BackupAndRestoreScreen |
| `FieldMindSettingsScreen.kt` | **MODIFY** | Update nav card, add new settings |
| `FieldMindNavigation.kt` | **MODIFY** | Add new routes for backup screens |
| `FieldMindSettings.kt` | **MODIFY** | Add export/backup preference fields |
| `FieldMindBackupWorker.kt` | **MODIFY** | Upgrade to .fieldmind format, add retention |

---

*Analysis prepared for Phase 1 implementation.*
