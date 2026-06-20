# FieldMind vs Rhythm: Complete File Ownership Analysis

> **Generated:** June 18, 2026  ·  **Last Updated:** June 20, 2026
> **Purpose:** Identify every file in the project, trace dependencies, classify as FieldMind / Rhythm / Shared, and recommend cleanup actions.

---

## 1. Overview (Current State)

| Category | File Count | Lines of Code (approx) | Status |
|----------|-----------|----------------------|--------|
| **FieldMind Core** | ~110 files | ~60,000 | ✅ Kept |
| **Rhythm Music Player (unused)** | 0 files | — | ✅ Deleted |
| **Shared (used by both)** | ~15 files | ~8,000 | ✅ Cleaned |
| **Rhythm Resource Files (layouts/drawables)** | 0 files | — | ✅ Deleted |
| **Rhythm String Resources** | 1 file (main strings.xml) | ~320,000 chars | ⚠️ Partial — 95% Rhythm content remains |

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

**Result:** `infrastructure/network/` directory is now empty. Only `AppSettings.kt` remains in `shared/data/model/`.

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

**Current:** 3,802 strings, 319,802 bytes, ~95% Rhythm content.

### Strings Actually Used by FieldMind Code (14 strings):

```
crashactivity_dont_fret_our_app
crashactivity_secret_crash_scrolls
crashactivity_share
crashactivity_uh_oh_looks_like
crash_bug_report
crash_restart_app
festivesplashgreeting_happy_halloween
festivesplashgreeting_happy_new_year
festivesplashgreeting_happy_valentines
festivesplashgreeting_love_is_in_the
festivesplashgreeting_merry_christmas
festivesplashgreeting_seasons_greetings
festivesplashgreeting_spooky_season
updates_rhythm_logo_cd
```

Plus potentially: `app_name`, widget strings (used by Glance), onboarding strings, and settings strings (used by FieldMind screens via string references).

### Rhythm-Only String Prefixes to Remove (~2,000+ strings):

| Prefix | Count (approx) | Category |
|--------|----------------|----------|
| `player_*` | ~50 | Player screen text |
| `library_*` | ~117 | Music library |
| `playlist_*` | ~74 | Playlist management |
| `queue_*` | ~30 | Playback queue |
| `equalizer_*`, `bass_boost_*`, `virtualizer_*` | ~40 | Audio equalizer |
| `lyrics_*` | ~30 | Synchronized lyrics |
| `sleep_timer_*` | ~20 | Sleep timer |
| `search_*` | ~40 | Music search |
| `streaming_*` | ~105 | Streaming service |
| `home_quote_*`, `home_mood_*`, `home_stat_*` | ~30 | Home screen music content |
| `rhythm_guard_*` | ~20 | Hearing safety |
| `cache_*` | ~20 | Cache management |
| `broadcast_status_*`, `bluetooth_lyrics_*`, `scrobbling_*`, `discord_*` | ~40 | External integrations |
| `canvas_*`, `beta_*`, `badge_*`, `song_info_*`, `miniplayer_*` | ~40 | Music player features |
| `notification_*` (media scan, updater, streaming) | ~80 | Music notifications |
| `theme_*`, `settings_*` music-specific | ~200 | Theme/settings (partial overlap) |
| `festive_greeting_*` | 7 | **KEPT** — used by FestiveOverlay |

**Note:** `settings_*` and `theme_*` prefixes overlap with FieldMind settings. These need case-by-case review.

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

### Phase 3: Major Refactoring — ⚠️ REMAINING

| Task | Priority | Status |
|------|----------|--------|
| Strip Rhythm strings from strings.xml | 🔵 P2 | ⏳ Pending — 3,802 strings, ~95% Rhythm |
| Replace RhythmTheme with FieldMindBaseTheme | 🔵 P2 | ⏳ Pending — needs careful M3 refactor |
| Remove music color presets from Theme.kt Color.kt | 🔵 P2 | ⏳ Pending — low risk, low impact |

---

## 6. Current Quick Stats

- **Total Kotlin source files:** 135
- **FieldMind Core:** 110 (81%)
- **Shared (cleaned):** 15 (11%)
- **Infrastructure:** 5 (4%)
- **Activities:** 2 (2%)
- **Remaining unused mipmap fallbacks:** ~10 files
- **String resources remaining:** 3,802 strings, 319K chars (~95% Rhythm)
- **Rhythm music code:** 0 files ✅

---

## 7. Risks & Mitigations (Updated)

| Risk | Impact | Status |
|------|--------|--------|
| `RhythmColors` used by 3 FieldMind files | Compilation error if removed | ✅ Resolved — ExtendedTheme deleted, replaced with MaterialTheme.colorScheme |
| `rhythm_splash_logo` appears in CrashActivity | Crash screen shows old logo | ✅ Resolved — Changed to new FieldMind logo via ic_launcher_foreground |
| `RhythmTheme` used by MainActivity + CrashActivity | App won't compile without it | ⚠️ Still present — needs FieldMindBaseTheme refactor (P2) |
| Theme presets (Warm, Cool, etc.) unused | Dead code but harmless | ⚠️ Still present — harmless (P2) |
| strings.xml 95% Rhythm content | Bloated APK, unused resources | ⚠️ Needs targeted cleanup (P2) |
