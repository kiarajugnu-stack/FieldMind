# FieldMind App — Comprehensive Audit Analysis

**Generated:** 2026-06-24
**Scope:** UI/UX, backup/export, settings, navigation, keyboard types, status bars

---

## 1. 🔴 CRITICAL: Backup/Export Pipeline Missing FolderEntity

**Severity:** High — data loss risk on restore

### Findings

`FolderEntity` is fully implemented in the data layer (Room DAO, Repository, ViewModel) but is **completely absent from the backup/export pipeline**:

| Component | FolderEntity Included? | File |
|-----------|----------------------|------|
| `FieldMindExport.archiveJson()` | ❌ MISSING — no `folders` parameter | `FieldMindExport.kt:1011` |
| `ArchivePreview` data class | ❌ MISSING — no `folders: Int = 0` field | `FieldMindExport.kt:14` |
| `ArchivePreview.total` / `summary()` | ❌ MISSING — folders not counted | `FieldMindExport.kt:31` |
| `parseArchiveJson()` | ❌ MISSING — folders not parsed | `FieldMindExport.kt` (search needed) |
| `BackupAndRestoreScreen` collection | ❌ MISSING — no `viewModel.folders` collected | `FieldMindBackupExportScreen.kt` |
| `collectAllCrossRefs()` | ❌ MISSING — no folder cross-refs | `FieldMindViewModel.kt:1214` |
| `restoreArchiveJson()` | ❌ MISSING — folders not restored | `FieldMindViewModel.kt:462` |
| `FieldMindAutoBackupWorker` | ❌ MISSING — folders not included | `FieldMindAutoBackupWorker.kt` |

### Fix Required

In **`FieldMindExport.kt`**:
- Add `folders: List<FolderEntity> = emptyList()` parameter to `archiveJson()`
- Add `folders` to the counts JSON object
- Add JSON serialization block for folders (all FolderEntity fields: id, name, color, icon, parentFolderId, projectId, etc.)
- Add `folders` to `ArchivePreview` data class
- Add `folders` to `parseArchiveJson()` parser

In **`FieldMindViewModel.kt`**:
- In `restoreArchiveJson()` — add folder import phase with old→new ID mapping
- In `collectAllCrossRefs()` — add folder-related cross-references

In **`FieldMindBackupExportScreen.kt`**:
- Collect `viewModel.folders` and pass to `archiveJson()` call

---

## 2. 🟡 Backup/Export: Missing EvidenceAttachmentEntity Cross-References

**Severity:** Medium — evidence links lost on restore

### Finding

`evidenceAttachments` is passed to `archiveJson()` but there are no cross-reference entries for evidence-to-observation or evidence-to-task links in `collectAllCrossRefs()`. The `parseArchiveJson()` needs to verify it properly re-wires attachment URIs with the old→new ID mapping.

---

## 3. 🟡 Backup/Export: FieldMindSettings.toExportJson() Coverage Check

**Severity:** Medium — some newer settings may not be persisted

### Verified Included ✅
- All security/PIN/decoy settings (appPinEnabled, appPinHash, appPinLength, decoyPinEnabled, decoyPinHash, decoyPinLabel, appPreviewMode, exportPasswordProtection, exportPasswordHash, exportEncryptionLevel, failedUnlockCooldown, failedUnlockBiometrics, failedUnlockPanicLock, metadataRemoveGps, metadataRemoveCamera, metadataRemoveDevice, metadataRemoveExif)
- entityColors (via Gson JSON serialization)
- All weather provider settings, species ID settings, lock/timeout settings
- All display, profile, AI settings
- `alwaysOnScreenEnabled`, `alwaysOnScreenDuration`, `clipboardCleanupEnabled`, `clipboardCleanupDelay`, `clearClipboardAfterExport`
- Screen visibility, user interests, onboarding state

### Fix Required

None found — `toExportJson()` and `applyFromJson()` appear comprehensive. But should be reviewed when new settings are added.

---

## 4. 🔴 Lock Screen: Decoy Mode Has No Way Back

**Severity:** High — UX dead-end for honest users who enter decoy accidentally

### Finding

Once decoy mode is activated (`isDecoyMode = true` in `FieldMindApp`), the **DecoyAppContent** composable shows a clean empty app with **no way to return** to the real lock screen. The `onExitDecoy` callback on `DecoyAppContent` is an empty no-op.

The user must **force-kill and restart the app** to re-enter the real PIN. This is intentional security, but there should be:
- A hidden gesture (e.g., 5-tap on the app icon) to return to the lock screen
- Or a long-press on the logo to show "Exit decoy mode" that requires re-authentication

### Fix Suggestion

In **`FieldMindLockScreen.kt`**, add a hidden exit mechanism to `DecoyAppContent`:
```kotlin
var tapCount by remember { mutableIntStateOf(0) }
// In the logo Box:
.clickable { 
    tapCount++
    if (tapCount >= 5) {
        tapCount = 0
        onExitDecoy() // Wire this to dismiss decoy mode
    }
}
```

---

## 5. 🟡 Settings Exist But Not Wired/Working

**Severity:** Varies

### 5a. `alwaysOnScreenDuration` Setting — Not Used

**File:** `FieldMindSettings.kt:312-313`
**Finding:** Setting `alwaysOnScreenDuration` ("15 min", "30 min", "60 min") is stored but never read to schedule a timeout for `FLAG_KEEP_SCREEN_ON`. The `LaunchedEffect(alwaysOnScreen)` in `MainActivity.kt` just adds/clears the flag without any timeout scheduling.

**Fix:** Parse the duration and post a delayed `Handler` task to clear the flag after the configured duration, like `AppLifecycleManager.setScreenKeepAwake()` already supports.

### 5b. `clearClipboardAfterExport` Setting — Only Checks `clipboardAutoCleanupEnabled`

**File:** `MainActivity.kt` (onPause)
**Finding:** The clipboard is cleared on every `onPause()` when `clipboardAutoCleanupEnabled` is true. But there's also a separate `clearClipboardAfterExport` setting that's supposed to clear clipboard specifically *after an export*, not on every background. The current implementation clears on every background regardless of export activity.

**Fix:** Separate the two behaviors:
- `clipboardAutoCleanupEnabled` → clear on every background (current behavior)
- `clearClipboardAfterExport` → only clear after an explicit export action (add a flag that's set after export and checked in onPause)

### 5c. `lockTimeout` Setting — Not Passed to AppLifecycleManager

**File:** `MainActivity.kt` → `onCreate()` calls `AppLifecycleManager.initialize()` with no timeout argument
**Finding:** `AppLifecycleManager.initialize()` defaults to `lockTimeoutSeconds = 0` (immediate lock). The user's `lockTimeout` setting ("Immediate", "1 minute", "5 minutes", "15 minutes") is not passed to the lifecycle manager, so the auto-lock always triggers immediately regardless of the setting.

**Fix:** In `onCreate()` (or better, reactively), parse the lockTimeout setting and pass it:
```kotlin
val timeoutSeconds = when (fieldMindViewModel.fieldSettings.lockTimeout.value) {
    "1 minute" -> 60
    "5 minutes" -> 300
    "15 minutes" -> 900
    else -> 0
}
AppLifecycleManager.initialize(this, lockTimeoutSeconds = timeoutSeconds)
```

### 5d. `clipboardCleanupDelay` Setting — Not Used

**File:** `FieldMindSettings.kt:318-319`
**Finding:** `clipboardCleanupDelay` ("30 sec", "1 min", "5 min") is stored but never used. The clipboard is cleared immediately on `onPause()` without respecting the delay.

**Fix:** Apply the delay in `onPause()`:
```kotlin
val delayMs = when (fieldMindViewModel.fieldSettings.clipboardCleanupDelay.value) {
    "1 min" -> 60_000L; "5 min" -> 300_000L; else -> 30_000L
}
Handler(Looper.getMainLooper()).postDelayed({ /* clear clipboard */ }, delayMs)
```

---

## 6. 🟡 Navigation Issues — Wrong Destinations

**Severity:** Medium

### 6a. Settings "Backup & Restore" Nav Card Goes to Export Studio

**File:** `FieldMindNavigation.kt:1022` / `FieldMindSettingsScreen.kt`
**Finding:** The Settings hub's "Backup & Restore" nav card calls `onOpenBackup` which navigates to `FieldMindScreen.ExportStudio.route`. This is the same screen as "Export Studio" — there's no dedicated backup settings screen. The `SettingsBackup` route exists but navigates to `BackupImportSettingsPage` which contains different content than the full "Backup & Restore" screen.

**Fix:** Either:
- Make the Settings hub nav card navigate to `FieldMindScreen.SettingsBackup.route` (the sub-page), OR
- Make the hub nav card navigate to Export Studio (as it does now) but rename the label to "Export Studio"

### 6b. Security Settings "Encrypted Backups" Toggle Toggles Auto-Backup

**File:** `FieldMindSettingsScreen.kt` (SecuritySettingsPage)
**Finding:** In the Security page, "Encrypted backups" toggle calls `settings::setAutoBackupEnabled` — this toggles automatic backup scheduling, not export encryption. There's a separate "Export Encryption Level" setting that's the actual encryption control. The toggle is mislabeled — it should say "Auto backup" not "Encrypted backups".

---

## 7. 🟡 Status Bar Padding Audit

**Severity:** Low — visual polish

### Screens WITH `statusBarsPadding()` ✅
Home, Observe, Projects, Library, Insights, Settings (hub + sub-pages), Detail screens, Canvas, Data Tools, Timer Tool, Archive, Questions, Tasks, Changelog, Lock Screen, DecoyAppContent, Learn, Species Browser, Taxonomic Browser, Weather Database, Report, Entity Accent Colors, Image Viewer, PDF Viewer, Figure Gallery

### Screens WITHOUT `statusBarsPadding()` ❌ — *Potential issue*
- **Note:** All the main screens already have it. The navigation bar itself handles edge-to-edge via `statusBarsPadding()` on the outer container.
- Some dialog-based screens (e.g., `ObservationQuickAddDialog`, entity picker dialogs) don't have statusBarsPadding, but they're dialogs so it's acceptable.

**Verdict:** Status bar padding coverage is good across all primary screens. No critical gaps found.

---

## 8. 🟢 Number Field Keyboard Type Audit

**Severity:** Good — most are correctly configured

### Verified ✅
- All PIN fields: `KeyboardType.NumberPassword` ✓ (numpad, masked)
- NumberField composable: `KeyboardType.Number` or `KeyboardType.Decimal` ✓
- Latitude/longitude: `KeyboardType.Decimal` ✓ (accepts decimals, plus/minus)
- All `OutlinedTextField` with `KeyboardType.Number`: Properly filtered in `FieldTextField` ✓
- Observation duration fields: `KeyboardType.Number` ✓

### Potential Issue
- The `FieldTextField` composable accepts a `keyboardType` parameter and uses `KeyboardOptions(keyboardType = keyboardType)`. But `KeyboardType.Number` on Android *does* allow typing alphabets in some IMEs (e.g., Gboard shows the number row but user can switch to letters). The `NumberField` composable has a custom `filterValue` lambda that strips non-numeric characters, but `FieldTextField` with `keyboardType = KeyboardType.Number` does NOT have this filter! 

**File:** `FieldMindComponents.kt` — `FieldTextField` composable (around line 926)
**Fix:** Add an input filter to `FieldTextField` when `keyboardType` is `Number` or `Decimal`:
```kotlin
val filteredValue = if (keyboardType == KeyboardType.Number) {
    value.filter { it.isDigit() || it == '-' }
} else value
```
Or use the same pattern as `NumberField` with a `filterValue` callback parameter.

---

## 9. 🟡 UI Elements Present But Invisible / Non-Functional

### 9a. `clearClipboardAfterExport` Setting Has No UI in Settings

**File:** `FieldMindSettings.kt:323-324`
**Finding:** The `clearClipboardAfterExport` setting exists in `FieldMindSettings` (stored, with setter/getter) but has **no toggle or UI** anywhere in the settings screens. The user can enable "Auto clear clipboard" (which uses `clipboardAutoCleanupEnabled`) but cannot independently control clipboard clearing after exports.

### 9b. `alwaysOnScreenDuration` Has No UI in Settings

**File:** `FieldMindSettings.kt:312-313`
**Finding:** The `alwaysOnScreenDuration` setting ("15 min", "30 min", "60 min") is stored but has **no picker/selector** in any settings screen. The user can toggle "Keep screen on" but cannot choose *how long* the screen stays on.

### 9c. `exportGpsPrivacy` and `exportExcludeMedia` Not in Settings UI

**File:** `FieldMindSettings.kt:326-336`
**Finding:** These settings exist in the data layer but have **no UI controls** in the Security or Export settings pages. Users can't control GPS precision in exports or exclude media from exports.

### 9d. Decoy PIN "Exit Decoy" Button Does Nothing

**File:** `FieldMindLockScreen.kt` — `DecoyAppContent`
**Finding:** The `onExitDecoy` callback parameter exists but is never wired with actual functionality. An honest user who enters the decoy PIN accidentally has no way to return.

---

## 10. 🟢 What's Working Well

- All main screens have `statusBarsPadding()` ✅
- PIN fields use `KeyboardType.NumberPassword` (numpad keyboard) ✅
- Number fields use `Number`/`Decimal` keyboard types ✅
- Decoy PIN detection and switching to decoy mode works ✅
- Auto-lock lifecycle wired into onPause/onResume ✅
- Keep screen on reactive via LaunchedEffect ✅
- Clipboard auto-clear on background ✅
- Backup includes almost all entity types ✅
- Settings export/import comprehensive ✅

---

## 11. 💡 Thoughtful User Suggestions

*If I were using FieldMind as my daily research notebook, here's what I'd ask for:*

1. **"Can I export my data and be sure everything comes back?"** — The FolderEntity gap in backups is my biggest worry. I'd love a "Verify backup integrity" button that checks all entity types are included before writing the file.

2. **"Why does the Settings hub list 'Backup & Restore' and 'Security' as separate items, but the Security page also has backup encryption settings?"** — I'd consolidate: put ALL backup/export settings under one "Backup & Export" hub section, and ALL lock/PIN/biometrics under "Security".

3. **"Can I get a guest/decoy mode that actually feels like a real app?"** — The current empty-state decoy is a great start, but I'd make it look more convincing: add some randomly generated (fake) observations, a working search that returns nothing, and proper empty-states in every section. A suspicious person would see the dead-empty app and know something's up.

4. **"I want to share a single observation as a formatted card, not export everything."** — The detail screen has Copy/Share buttons, but they dump raw Markdown. I'd love a native Android share sheet with a pretty card image, formatted text, and the option to include/exclude GPS coordinates.

5. **"Let me know when I'm about to lose data."** — Before the app locks or auto-backup runs, show a quick summary: "Backing up 12 observations, 3 notes, 2 folders..." so I know nothing's missing.

6. **"My PIN setup is confusing — I pick a length but then I can enter any length during setup."** — The PIN setup should respect the current length selection. If I pick "5 digits", the setup form should auto-submit when I've entered exactly 5 digits, not require me to press Save.

7. **"Keep screen on for 30 minutes while I'm doing field work, not forever."** — Please add the duration picker to the settings UI so I can choose 15/30/60 minutes instead of it being invisible.

8. **"Why does 'Auto clear clipboard' empty my clipboard every time I switch apps?"** — I'd rename this to "Clear clipboard on background" and add a delay option. If I copy a species name and switch to my notes app to paste it, I don't want the clipboard already empty.

9. **"Can I preview what a backup will contain before exporting?"** — The current backup screen is good, but it doesn't show entity counts before the export runs. A pre-export summary with checkboxes ("Include media? Include settings?") would build trust.

10. **"The category picker dialogs are nice, but they always scroll to the top when I open them"** — If I've used a picker before, remember my last selection and scroll to it.
