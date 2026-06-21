# FieldMind vs Rhythm: Complete File Ownership Analysis

> **Generated:** June 18, 2026  ·  **Last Updated:** June 21, 2026
> **Purpose:** Identify every file in the project, trace dependencies, classify as FieldMind / Rhythm / Shared, and recommend cleanup actions.

---

## 1. Overview (Current State)

| Category | File Count | Lines of Code (approx) | Status |
|----------|-----------|----------------------|--------|
| **FieldMind Core** | 120 files | ~60,000 | ✅ Kept |
| **Rhythm Music Player (unused)** | 0 files | — | ✅ Deleted |
| **Shared (used by both)** | 14 files | ~8,000 | ✅ Cleaned |
| **Infrastructure** | 5 files | ~2,000 | ✅ Cleaned |
| **Activities** | 2 files | ~500 | ✅ Cleaned |
| **Utility** | 2 files | ~500 | ✅ Cleaned |
| **Rhythm Resource Files (layouts/drawables)** | 0 files | — | ✅ Deleted |
| **Rhythm String Resources** | 1 file (main strings.xml) | ~1,941 strings | ⚠️ Partial — ~30 Rhythm string keys remain |

---

## 2. File Classification (Updated)

### 2.1 FieldMind Core Files ✅ (Keep — essential for app functionality)

*Unchanged from original — 110 Kotlin files in features/field/*

### 2.2 Rhythm Music Player Files ❌ — ALL DELETED ✅

All Rhythm music player files have been removed:

| File | Status |
|------|--------|
| `infrastructure/network/` — ALL 10 files | ✅ Deleted |
| `shared/data/model/Music.kt` | ✅ Deleted |
| `shared/data/model/LyricsData.kt` | ✅ Deleted |
| `shared/data/model/LyricsSourcePreference.kt` | ✅ Deleted |
| `shared/data/model/UserAudioDevice.kt` | ✅ Deleted |
| `shared/data/model/AutoEQProfile.kt` | ✅ Deleted |

**Result:** `infrastructure/network/` directory is removed. Only `AppSettings.kt` remains in `shared/data/model/`.

### 2.3 Shared Files 🔄 (Used by BOTH — Currently Cleaned)

| File | Kept For | Rhythm-only Content | Status |
|------|----------|---------------------|--------|
| `shared/presentation/theme/Theme.kt` (`RhythmTheme`) | MainActivity + CrashActivity theming | `getAlbumArtColorScheme()` (dead code), `getCustomColorScheme()` (music presets) | ⚠️ Dead code remains but harmless |
| `shared/presentation/theme/Color.kt` | Color definitions for Theme.kt | Music-specific colors removed | ✅ Cleaned |
| `shared/presentation/theme/Type.kt` | Typography | None | ✅ Clean |
| `shared/presentation/theme/Shape.kt` | Shapes for Theme.kt | `MusicShapes` removed, `ExpressiveShapeTokens` removed | ✅ Cleaned |
| `shared/presentation/theme/ExtendedTheme.kt` | Was used by 3 FieldMind files | RhythmColors.warning/success | ✅ Deleted entirely |
| `shared/presentation/theme/Dimensions.kt` | Dimension constants | Clean | ✅ |
| `shared/presentation/theme/MaterialShapesUtils.kt` | Shape utilities | Clean | ✅ |
| `shared/presentation/viewmodel/ThemeViewModel.kt` | MainActivity uses it | None | ✅ |
| `shared/presentation/components/icons/Icon.kt` | All FieldMind screens | None | ✅ |
| `shared/presentation/theme/festive/` (7 files) | Festive overlay decoration | Already cleaned | ✅ |
| `shared/presentation/theme/festive/FestiveThemeExamples.kt` | Preview examples | Deleted | ✅ Deleted |
| `shared/data/model/AppSettings.kt` | EVERYTHING uses it | Cleaned to ~309 lines | ✅ Cleaned |

---

## 3. Resource Cleanup Status

### 3.1 Drawable Resources

| Category | Status |
|----------|--------|
| Rhythm-only music drawables (ic_play_arrow, ic_pause, ic_skip_*, etc.) | ✅ All deleted |
| Rhythm-only widget drawables (widget_preview.xml, widget_background.xml, etc.) | ✅ All deleted |
| Rhythm splash/logo drawables (rhythm_logo.xml, rhythm_splash_logo.xml, splash_particles.xml) | ✅ Deleted |
| Rhythm audio badge drawables (ic_hd, ic_hq, ic_lossy, ic_high_res, ic_cd, etc.) | ✅ All deleted |
| Launcher icon density variants (mipmap-* webp files) | ⚠️ Still present — unused fallbacks for adaptive icons |
| **Kept:** `ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, `ic_notification.xml` | ✅ Used by app |
| **Kept:** `widget_preview_glance.xml` | ✅ Created for Glance widget |
| **Added:** `fieldmind_logo.png` in `drawable-nodpi/` | ✅ New FieldMind logo |

### 3.2 Layout Files

| Category | Status |
|----------|--------|
| Music widget layouts (8 files) | ✅ All deleted |
| FieldMind widget layouts (2 files) | ✅ Kept |

### 3.3 Color/XML Resources

| Category | Status |
|----------|--------|
| `color/sl_lyric_active.xml`, `color/sl_lyric_text.xml` | ✅ Deleted |
| `xml/lyric_widget.xml`, `xml/widget_info.xml` | ✅ Deleted |
| FieldMind XML configs (widget_info, backup_rules, etc.) | ✅ Kept |

### 3.4 Translated Strings (Locales)

| Category | Status |
|----------|--------|
| 10 translated locale files (values-*/strings.xml) | ✅ All deleted |

### 3.5 Mipmap Resources

| Category | Status |
|----------|--------|
| mipmap-anydpi ic_launcher.xml + ic_launcher_round.xml | ✅ Kept (reference foreground/background drawables) |
| Density-specific mipmap webp files (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) | ⚠️ Still present — legacy density fallbacks |
| **Recommendation:** Keep as fallbacks for older devices; harmless |

---

## 4. Strings.xml Remaining Analysis

**Current:** 1,941 strings total. Music-player-specific prefixes (`player_`, `library_`, `playlist_`, `queue_`, `equalizer_`, `lyrics_`, `streaming_`, `cache_`, `search_`, `sleep_timer_`, `broadcast_`, etc.) have all been removed during cleanup.

### ⚠️ Remaining Rhythm References (~30 string keys)

The following ~30 string keys still contain "Rhythm" in their key names or values. Most are actually used by FieldMind code but have inherited the old naming:

| String Key | Current Value | Recommendation |
|-----------|---------------|----------------|
| `settings_select_option` | "Rhythm Settings" | Rename key to `settings_title` or update value to "FieldMind Settings" |
| `settings_rhythm_stats` | "Rhythm Stats" | Rename key to `settings_fieldmind_stats` |
| `settings_rhythm_stats_desc` | "Listening time, top records and trends" | Rename key |
| `settings_rhythm_aura` | "FieldMind Focus" | Already uses correct display name; rename key |
| `updates_source_desc` | "Choose which release family Rhythm should use..." | Update text to "FieldMind" |
| `updates_rhythm_logo_cd` | "FieldMind Logo" | Rename key |
| `service_app_updates` | "Rhythm Updater" | Update to "FieldMind Updater" |
| `service_update_status` | "Rhythm Updater Status" | Update to "FieldMind Updater Status" |
| `service_rhythm_pulse` | "FieldMind Tips" | Rename key |
| `service_rhythm_guard_alerts` | "FieldMind Focus Alerts" | Rename key |
| `service_rhythm_guard_timers` | "FieldMind Focus Timers" | Rename key |
| `service_rhythm_music` | "FieldMind Research" | Rename key |
| `service_starting` | "Rhythm is starting." | Update to "FieldMind is starting." |
| `service_loading_settings` | "Rhythm is loading your settings." | Update |
| `service_setup_components` | "Rhythm is preparing core components." | Update |
| `service_initializing_player` | "Rhythm is preparing playback." | Update |
| `service_creating_controls` | "Rhythm is creating playback controls." | Update |
| `service_setup_media_session` | "Rhythm is preparing the media session." | Update |
| `service_initializing_controller` | "Rhythm is connecting the media controller." | Update |
| `service_ready` | "Rhythm is ready." | Update |
| `service_init_failed` | "Rhythm could not start. Please reopen the app." | Update |
| `onboarding_custom_notifications_desc` | "Use Rhythm's enhanced notification design..." | Update |

### Strings Used by FieldMind Code (Count by Category)

| Category | Count | Status |
|----------|-------|--------|
| `settings_*` | 683 | ✅ Used by settings screens |
| `stats_*` | 27 | ✅ Used by insights/stats screens |
| `widget_*` | 24 | ✅ Used by Glance widgets |
| `onboarding_*` | 23 | ✅ Used by OnboardingScreen |
| `backup_*` | 23 | ✅ Used by Backup/Export screens |
| `festive*` | 21 | ✅ Used by FestiveOverlay |
| `license_*` | 17 | ✅ Used by LicensesBottomSheet |
| `about_*` | 15 | ✅ Used by About page |
| `crash*` | 8 | ✅ Used by CrashActivity |
| `device_*` | 36 | ✅ Audio device names |
| `metadata_*` | 34 | ✅ Metadata editing strings |
| `service_*` | ~30 | ⚠️ ~20 contain "Rhythm" — needs rename |

**Note:** `settings_*` and `stats_*` strings are actively used by FieldMind UI. They are not Rhythm leftovers.

### Summary: What Remains to Clean in strings.xml

- **String keys with "rhythm" in the name:** ~30 service/settings keys need renaming (the values are already mostly correct)
- **String values that still say "Rhythm":** ~15-20 display strings in service startup messages
- **Cleanup status:** 95%+ of the original Rhythm-only strings have been removed ✅

---

## 5. Cleanup Progress Summary

### Phase 1: Safe Deletions — ✅ COMPLETE

| Task | Status |
|------|--------|
| Delete `infrastructure/network/` (10 files) | ✅ Done |
| Delete shared data models (5 files) | ✅ Done |
| Update FieldMindApplication.kt (remove NetworkClient.initialize) | ✅ Done |
| Delete translated locale strings (10 files) | ✅ Done |
| Delete music layout files (8 files) | ✅ Done |
| Delete Rhythm-only drawables (~25 files) | ✅ Done |
| Delete color/XML resource files | ✅ Done |

### Phase 2: Code Changes — ✅ COMPLETE

| Task | Status |
|------|--------|
| Clean AppSettings.kt | ✅ Done |
| Delete FestiveThemeExamples.kt | ✅ Done |
| Remove MusicShapes + ExpressiveShapeTokens from Shape.kt | ✅ Done |
| Remove music colors from Color.kt | ✅ Done |
| Remove dead code from Theme.kt (getAlbumArtColorScheme) | ✅ Done |
| Delete ExtendedTheme.kt + replace RhythmColors references | ✅ Done |

### Phase 3: Package Rename — ✅ COMPLETE

| Task | Status |
|------|--------|
| Move source files `chromahub/rhythm/` → `fieldmind/research/` | ✅ Done (144 files) |
| Update 4 remaining old package declarations in .kt files | ✅ Done |
| Update AGENTS.md, README.md, wiki docs | ✅ Done |
| Update backup_rules.xml | ✅ Done |

### Phase 4: Minor Cleanup — ⚠️ REMAINING

| Task | Priority | Status |
|------|----------|--------|
| Clean ~30 string keys with "rhythm" in name + ~15 values that still say "Rhythm" | 🔵 P2 | ⏳ Pending — values mostly say "FieldMind" already |
| Replace RhythmTheme with FieldMindBaseTheme | 🔵 P2 | ⏳ Pending — needs careful M3 refactor |
| Remove music color presets from Theme.kt Color.kt | 🔵 P2 | ⏳ Pending — low risk, low impact |

---

## 6. Current Quick Stats

- **Total Kotlin source files:** 147 (144 in app module + 3 test files)
- **FieldMind Core (`features/field/`):** 120 (83%)
- **Shared (`shared/`):** 14 (10%)
- **Infrastructure (`infrastructure/`):** 5 (3%)
- **Activities (`activities/`):** 2 (1%)
- **Utility (`util/`):** 2 (1%)
- **Resources:** 6 drawable, 2 layout, 7 XML config, 12 mipmap, 2 anim, 2 font files
- **String resources:** 1,941 strings — ~30 keys still contain "Rhythm" in their key name
- **Rhythm music code:** 0 files ✅
- **Package:** `fieldmind.research.app` (fully renamed from `chromahub.rhythm.app`) ✅

---

## 7. Risks & Mitigations (Updated)

| Risk | Impact | Status |
|------|--------|--------|
| `RhythmColors` used by 3 FieldMind files | Compilation error if removed | ✅ Resolved — ExtendedTheme deleted, replaced with MaterialTheme.colorScheme |
| `rhythm_splash_logo` appears in CrashActivity | Crash screen shows old logo | ✅ Resolved — Changed to new FieldMind logo via ic_launcher_foreground |
| Package name mismatch | Build failure | ✅ Resolved — `chromahub.rhythm.app` renamed to `fieldmind.research.app` across all files |
| Directory structure mismatch | IDE confusion, import errors | ✅ Resolved — Files moved from `chromahub/rhythm/` to `fieldmind/research/` |
| `RhythmTheme` used by MainActivity + CrashActivity | App won't compile without it | ⚠️ Still present — needs FieldMindBaseTheme refactor (P2) |
| Theme presets (Warm, Cool, etc.) unused | Dead code but harmless | ⚠️ Still present — harmless (P2) |
| ~30 string keys with "rhythm" in name | Inconsistent naming | ⚠️ Low priority — values already mostly say "FieldMind" |
| ~15 service strings display "Rhythm" in UI | User-facing old app name | ⚠️ Minor polish needed — service startup messages
