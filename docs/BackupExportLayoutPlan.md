# Backup & Export UI — Layout Analysis & Fix Plan

> Generated: June 21, 2026
> File: `FieldMindBackupExportScreen.kt` (2332 lines)

---

## Current Issues

### 1. Massive Single-File Complexity

**File is 2332 lines / 125K chars** — one of the largest files in the project. The screen has 3 tabs (Export, Import, Backup) all crammed into a single file with no splitting.

**Impact:** Hard to navigate, find composables, or reason about layout. Any change risks breaking something unrelated.

### 2. ExportTabContent — LargeBackupActionCard Shows Wrong "Import" Button

Lines ~1170-1180:
```kotlin
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    LargeBackupActionCard("Export / Save Backup", ...) { ... }
    LargeBackupActionCard("Import / Restore", ...) { onSwitchToImport?.invoke() }
}
```

The **Export tab** shows a primary action card for **Import/Restore** — this is confusing. The import action belongs on the Import tab, not here. Tapping it switches tabs, which is a hidden navigation that disorients the user.

**Fix:** Replace the Import action card with a proper secondary export action (e.g., "Quick Share" or "Export as JSON only"). Or remove it entirely and rely on the tab pill selector above for tab switching.

### 3. Tab Content — Too Much Vertical Scroll

Each tab's content is a `Column` inside a LazyColumn item. The Export tab has:
- 2 primary action cards (Row)
- Export format card (FlowRow chips)
- Entity summary card (small)
- Folder picker card
- Privacy options card (GPS 3-mode + media toggle)
- Encryption card (toggle + password + strength bar)
- Progress card (conditional)
- **2 action buttons** (Share + Save)

That's **8-9 cards** stacked vertically, all inside a single scroll view. Users must scroll significantly to reach the action buttons.

**Fix:** Collapse secondary options (privacy, encryption) into expandable sections with `AnimatedVisibility`. Make the format picker more compact (switch to a single row of compact chips with scroll).

### 4. Duplicate "Export to Folder" Logic

The Export tab and Backup tab have **nearly identical folder picker cards**:
- Export tab: "Save folder" card
- Backup tab: "Backup folder" card

Both use the same `createFileInTree()` function, same folder picker launcher pattern. But they have **separate state** (`exportDestinationUri` vs `backupFolderUri`). This means the user selects a folder for export, and a *different* folder for backup — which is confusing. Most users would expect one backup folder for both.

**Fix:** Unify to a single backup folder URI. The Export tab should save to the same folder that the Backup tab uses. Remove the separate `exportDestinationUri` state.

### 5. Export — No "Select All" / Scope Selection

The Export tab exports **ALL** entity types with no way to select specific types. The entity summary card just shows a count with no interaction.

**Fix:** Add a scope selection section (as described in Prompt G — Selective Export).

### 6. Export Privacy Options Card — Clear Clipboard Toggle is Removed but Card Still Imports It

Lines ~1390: The clear clipboard toggle is disabled with a comment:
```kotlin
// REMOVED: clear clipboard is a privacy feature that belongs in Security settings
```

But the `ExportPrivacyOptionsCard` still accepts `clearClipboard`, `onClearClipboardChange`, and `showClearClipboard` parameters and renders the divider line. The dead parameters hang around.

**Fix:** Clean up unused parameters and simplify the card.

### 7. Backup Tab — Countdown Timer Uses Unnecessary LaunchedEffect Rerender

The countdown timer (visible when backup scheduling is enabled) uses a `LaunchedEffect` with `delay(1000)` to update every second. This triggers recomposition of the entire Backup tab content every second.

**Fix:** Isolate the countdown into its own composable so only the timer recomposes.

### 8. Import Tab — Drop Zone Has No Drag-and-Drop Support

The "Tap to select" area looks like a drop zone but Android Compose has no drag-and-drop file handling. The large pulsing icon suggests interactivity that isn't there.

**Fix:** Make the drop zone actionable (it already is clickable) but remove the pulsing animation that suggests drag-and-drop. Or add Android's drag-and-drop support.

### 9. ModalBottomSheet (Share Dialog) Not Using Navigation

The share dialog is a `ModalBottomSheet` managed by a `var showShareDialog` state. It contains a full mini-export flow with format selection, media toggle, and share button.

**Fix:** This is fine as a bottom sheet, but the content inside it duplicates much of what's in `ExportTabContent`. Consider extracting the export logic into a reusable composable.

### 10. No Loading/Progress for Backup Tab

The Export tab has a nice progress card with `CircularProgressIndicator` + `LinearProgressIndicator`. The Backup tab has... nothing. When "Backup now" is pressed, there's no visual feedback until completion.

**Fix:** Add the same progress indicator pattern to the Backup tab's `onCreateBackup` callback.

---

## Layout Fix Plan

### Phase 1: Structural (Low Effort, High Impact)

| # | Task | Effort | 
|---|------|--------|
| 1.1 | Remove duplicate Import action card from Export tab — replace with "Quick Share" button | 15 min |
| 1.2 | Remove unused `clearClipboard` / `showClearClipboard` params from `ExportPrivacyOptionsCard` | 15 min |
| 1.3 | Add progress indicator to Backup tab's onCreateBackup (mirror Export pattern) | 15 min |
| 1.4 | Isolate countdown timer into a separate composable | 10 min |

### Phase 2: Unification (Medium Effort, High Impact)

| # | Task | Effort |
|---|------|--------|
| 2.1 | Merge exportDestinationUri + backupFolderUri into a single backup folder | 30 min |
| 2.2 | Remove redundant folder picker from Export tab (use Backup tab's folder) | 15 min |
| 2.3 | Make privacy options collapsible (AnimatedVisibility) with expand/collapse arrow | 30 min |

### Phase 3: Enhancement (Medium Effort)

| # | Task | Effort |
|---|------|--------|
| 3.1 | Add export scope section with entity type checkboxes (Selective Export) | 2 hours |
| 3.2 | Split file into smaller composable files (ExportTab, ImportTab, BackupTab, ShareDialog) | 1 hour |
| 3.3 | Remove pulsing animation from Import drop zone (replace with static icon) | 10 min |

### Phase 4: Polish (Lower Effort)

| # | Task | Effort |
|---|------|--------|
| 4.1 | Extract shared export logic into reusable composable (shared between ExportTab and ShareDialog) | 30 min |
| 4.2 | Add success/failure snackbar messages to Backup tab operations | 15 min |
| 4.3 | Add entity type icons to export scope selector | 15 min |

---

## Priority Recommendation

**P0 (Do first):** 1.1, 2.1, 2.2 — these fix the most confusing layout issues immediately.

**P1 (Next):** 1.3, 2.3, 3.3 — improve the user experience with better feedback.

**P2 (Later):** 3.1, 3.2, 1.4 — deeper structural improvements.

**P3 (Nice to have):** 1.2, 4.1, 4.2, 4.3 — polish and cleanup.

---

## Visual Layout (Current vs Proposed)

### Export Tab — Current (too much vertical scroll)
```
┌───────────────────────────────┐
│  Header                       │
├───────────────────────────────┤
│  Hero Status Card             │
├───────────────────────────────┤
│  [Export] [Import] [Backup]   │  ← Tab pill
├───────────────────────────────┤
│  [Export Card] [Import Card]  │  ← Confusing! Import on Export tab
├───────────────────────────────┤
│  Export Format:               │
│  [chip][chip][chip][chip]     │
├───────────────────────────────┤
│  Total: N records             │
├───────────────────────────────┤
│  Save folder: tap to select   │
├───────────────────────────────┤
│  Privacy options:             │
│  GPS: Exact / Approx / Remove │
│  Exclude media: [switch]      │
├───────────────────────────────┤
│  Encrypt export: [switch]     │
│  [password field]             │
│  [████████░░] Strong          │
├───────────────────────────────┤
│  [progress indicator]         │  ← Conditional
├───────────────────────────────┤
│  [Share]  [Save]              │  ← Action buttons (scroll to reach!)
└───────────────────────────────┘
```

### Export Tab — Proposed (collapsible sections, less scroll)
```
┌───────────────────────────────┐
│  Header                       │
├───────────────────────────────┤
│  Hero Status Card             │
├───────────────────────────────┤
│  [Export] [Import] [Backup]   │
├───────────────────────────────┤
│  Export Format:               │
│  [.fieldmind] [.zip] [JSON]… │  ← Compact row, scrollable horizontally
├───────────────────────────────┤
│  Save to (folder name)  [▶]   │  ← Shows folder from Backup tab
├───────────────────────────────┤
│  ▶ Privacy & encryption       │  ← Collapsible
├───────────────────────────────┤
│  [progress indicator]         │  ← Conditional
├───────────────────────────────┤
│  [Share]  [Save]              │  ← Visible without scroll
└───────────────────────────────┘
```

### Import Tab — Current
```
┌───────────────────────────────┐
│  [Pulsing download icon]      │
│  Tap to select a file...      │
├───────────────────────────────┤
│  (When file selected)         │
│  [file info card]             │
│  [entity preview grid]        │
│  Import mode:                 │
│  ○ Merge  ○ Replace          │
│  [Restore button]             │
└───────────────────────────────┘
```

### Import Tab — Proposed (no pulsing, cleaner)
```
┌───────────────────────────────┐
│  [download icon]              │
│  Select archive to restore     │
│  Supports .fieldmind, .zip... │
├───────────────────────────────┤
│  (When file selected)         │
│  [file info card]             │
│  [entity preview grid]        │
│  Import mode:                 │
│  ● Merge  ○ Replace          │
│  [Restore button]             │
└───────────────────────────────┘
```

---

## How to Read the Full File for Reference

The file is at:
`app/src/main/java/fieldmind/research/app/features/field/presentation/screens/FieldMindBackupExportScreen.kt`

Key sections by line number (current):
- 1-72: Imports
- 73-120: Data models
- 123-933: `BackupAndRestoreScreen` (main composable)
- 933-1140: `HeroStatusCard`, `TabPillSelector`
- 1160-1490: `ExportTabContent`
- 1494-1710: `ImportTabContent`
- 1710-1950: `BackupTabContent`
- 1950-2120: `ShareDialogContent`
- 2120-2332: Helper functions
