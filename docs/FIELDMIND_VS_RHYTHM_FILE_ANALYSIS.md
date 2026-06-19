# FieldMind vs Rhythm: Complete File Ownership Analysis

> **Generated:** June 18, 2026
> **Purpose:** Identify every file in the project, trace dependencies, classify as FieldMind / Rhythm / Shared, and recommend cleanup actions.

---

## 1. Overview

| Category | File Count | Lines of Code (approx) |
|----------|-----------|----------------------|
| **FieldMind Core** | ~80 files | ~60,000 |
| **Rhythm Music Player (unused)** | ~25 files | ~15,000 |
| **Shared (used by both)** | ~20 files | ~8,000 |
| **Rhythm Resource Files (layouts/drawables)** | ~50+ files | ~3,000 |
| **Rhythm String Resources** | 10 locale files | ~320,000 chars |

---

## 2. File Classification

### 2.1 FieldMind Core Files ✅ (Keep — essential for app functionality)

These files implement the FieldMind research notebook application. **DO NOT DELETE.**

#### Presentation Layer (features/field/presentation/)

| File | Purpose |
|------|---------|
| `navigation/FieldMindNavigation.kt` | Navigation graph, tab bar, screen routing |
| `theme/FieldMindTheme.kt` | FieldMind brand color scheme and semantic colors |
| `viewmodel/FieldMindViewModel.kt` | Main ViewModel for all FieldMind screens |
| `screens/FieldMindHomeScreen.kt` | Today/home dashboard |
| `screens/FieldMindObserveScreen.kt` | Observation capture |
| `screens/FieldMindProjectsScreen.kt` | Project management |
| `screens/FieldMindLibraryScreen.kt` | Knowledge library |
| `screens/FieldMindSettingsScreen.kt` | Settings hub |
| `screens/FieldMindDataTools.kt` | Data tools hub (counters, checklists, etc.) |
| `screens/FieldMindResearchSession.kt` | Research session UI |
| `screens/FieldLogScreen.kt` | Field observation log |
| `screens/FieldMindMapScreen.kt` | Map screen |
| `screens/FieldMindOnboardingScreen.kt` | Onboarding flow |
| `screens/FieldMindDialogs.kt` | Shared dialog components |
| `screens/FieldMindLockScreen.kt` | Privacy lock screen |
| `screens/FieldMindDetailScreen.kt` | Detail view for observations/notes |
| `screens/FieldMindArchiveScreen.kt` | Archive/search screen |
| `screens/FieldMindBackupExportScreen.kt` | Backup/export screen |
| `screens/FieldMindChangelogScreen.kt` | Changelog/what's new |
| `screens/FieldMindQuestionsScreen.kt` | Research questions |
| `screens/FieldMindScreenUtils.kt` | Screen utility helpers |
| `screens/FieldMindScreens.kt` | Screen definitions |
| `screens/FlashcardSessionScreen.kt` | Flashcard review session |
| `screens/WeatherDatabaseScreen.kt` | Weather database screen |
| `screens/SpeciesBrowserScreen.kt` | Species browser |
| `screens/TaxonomicBrowserScreen.kt` | Taxonomic browser |
| `screens/InsightsScreen.kt` | Insights/analytics |
| `screens/species/SpeciesIdentificationSheet.kt` | Species ID bottom sheet |
| `components/FieldMindComponents.kt` | Reusable UI components |
| `components/FieldMindIcons.kt` | FieldMind icon definitions |
| `components/FieldMindMotion.kt` | Animation constants |
| `components/FieldMindSnackbar.kt` | Snackbar provider |
| `components/FieldMindObserveSnackbar.kt` | Capture snackbar actions |
| `components/FieldMindCameraCapture.kt` | Camera capture |
| `components/FieldMindCameraV2.kt` | Camera V2 capture |
| `components/FieldMindCharts.kt` | Chart components |
| `components/FieldMindChartsExtended.kt` | Extended charts |
| `components/FieldMindObservationsTimeline.kt` | Observation timeline |
| `components/FieldMindSharedTransitions.kt` | Shared element transitions |
| `components/FieldDataTable.kt` | Data table component |
| `components/ObservationQualityComponents.kt` | Quality/confidence indicators |
| `components/HomeSpeciesCatalogSection.kt` | Home species catalog |
| `components/SpeciesDetailSheet.kt` | Species detail sheet |
| `components/TaxonomyPickerDialog.kt` | Taxonomy picker |
| `components/MaplibreMapView.kt` | MapLibre map view |
| `components/AnimatedWeatherScene.kt` | Weather visualization |
| `components/RequiredFieldState.kt` | Required field state helper |
| `components/EvidenceHubPhase6.kt` | Evidence hub |
| `components/ProjectPhase5Components.kt` | Project phase components |
| `components/HypothesesPhase8.kt` | Hypotheses phase |
| `components/InsightsPhase9.kt` | Insights phase |
| `components/JournalPhase10.kt` | Journal phase |
| `components/ReportsPhase11.kt` | Reports phase |
| `components/DataWorkspacePhase7.kt` | Data workspace |
| `components/LibraryPhase12.kt` | Library phase |

#### Data Layer (features/field/data/)

| File | Purpose |
|------|---------|
| `database/FieldMindDatabase.kt` | Room database |
| `database/entity/FieldEntities.kt` | All entity definitions |
| `database/dao/FieldMindDao.kt` | Data access objects |
| `settings/FieldMindSettings.kt` | FieldMind-specific settings |
| `repository/FieldMindRepository.kt` | Repository layer |
| `ai/GeminiResearchAssistant.kt` | AI assistant integration |
| `vision/SpeciesDatabase.kt` | Species identification database |
| `vision/SpeciesImageAnalyzer.kt` | Species image analysis |
| `vision/SpeciesClassifier.kt` | Species classifier |
| `vision/PhashDatabase.kt` | Perceptual hash database |
| `learn/LearnLibrary.kt` | Learning library |
| `export/FieldMindExport.kt` | Export functionality |
| `export/FieldTemplate.kt` | Export templates |
| `security/FieldMindPrivacyManager.kt` | Privacy/security manager |
| `weather/WeatherModels.kt` | Weather data models |
| `weather/WeatherProvider.kt` | Weather provider interface |
| `weather/OpenMeteoProvider.kt` | Open-Meteo implementation |
| `weather/OpenWeatherMapProvider.kt` | OpenWeatherMap implementation |
| `weather/MetNorwayProvider.kt` | Met Norway implementation |
| `weather/WeatherApiDotComProvider.kt` | WeatherAPI.com implementation |
| `weather/IndiaMeteorologicalDepartmentProvider.kt` | IMD implementation |
| `weather/NationalWeatherServiceProvider.kt` | NWS implementation |
| `weather/WeatherUnitConverter.kt` | Unit conversion |
| `location/FieldLocationProvider.kt` | Location services |
| `location/MapDrawingTools.kt` | Map drawing tools |
| `location/TrackRecorder.kt` | GPS track recording |
| `location/GeoFenceReminder.kt` | Geofence reminders |
| `location/MaplibreOfflineManager.kt` | Offline map tiles |
| `flashcard/SM2Engine.kt` | Spaced repetition engine |
| `timer/FieldTimer.kt` | Field timer |
| `stats/FieldMindStreaks.kt` | Streak tracking |
| `bulk/FieldBulkOperations.kt` | Bulk operations |
| `attachment/FieldAttachmentManager.kt` | File attachment manager |
| `undo/FieldUndoHelper.kt` | Undo helper |
| `undo/FieldUndoManager.kt` | Undo manager |
| `background/FieldMindReminderWorker.kt` | Reminder worker |
| `background/FieldMindBackgroundScheduler.kt` | Background scheduler |
| `background/FieldMindAutoBackupWorker.kt` | Auto-backup worker |
| `species/TaxonomyData.kt` | Taxonomy data |

#### App Shell

| File | Purpose |
|------|---------|
| `FieldMindApplication.kt` | Application class |
| `activities/MainActivity.kt` | Main activity |
| `activities/CrashActivity.kt` | Crash handler activity |
| `util/CrashReporter.kt` | Crash reporting |
| `util/ANRWatchdog.kt` | ANR detection |

#### Infrastructure — Workers (FieldMind)

| File | Purpose |
|------|---------|
| `infrastructure/worker/FieldMindStreakWorker.kt` | Daily streak check |
| `infrastructure/worker/FieldMindBackupWorker.kt` | Auto-backup worker |

#### Infrastructure — Widgets (FieldMind)

| File | Purpose |
|------|---------|
| `infrastructure/widget/glance/FieldMindDashboardWidget.kt` | Dashboard widget |
| `infrastructure/widget/glance/FieldMindQuickCaptureWidget.kt` | Quick capture widget |
| `infrastructure/widget/glance/FieldMindWidgetReceiver.kt` | Widget receivers |

---

### 2.2 Rhythm Music Player Files ❌ (Unused by FieldMind — Safe to Delete)

These are music player files from the original "Rhythm" app. **FieldMind does not use any of them.**

| File | Purpose | Delete? |
|------|---------|---------|
| `infrastructure/network/NetworkClient.kt` | Network client for Deezer/Spotify/LRCLib/etc APIs | ✅ Yes |
| `infrastructure/network/DeezerApiService.kt` | Deezer music API | ✅ Yes |
| `infrastructure/network/LRCLibApiService.kt` | LRCLib lyrics API | ✅ Yes |
| `infrastructure/network/YTMusicApiService.kt` | YouTube Music API | ✅ Yes |
| `infrastructure/network/SpotifySearchApiService.kt` | Spotify search API | ✅ Yes |
| `infrastructure/network/ITunesSearchApiService.kt` | iTunes search API | ✅ Yes |
| `infrastructure/network/RhythmLyricsApiService.kt` | Apple Music lyrics API | ✅ Yes |
| `infrastructure/network/RhythmLyricsModels.kt` | Lyrics models | ✅ Yes |
| `infrastructure/network/NetworkManager.kt` | GitHub API network manager | ✅ Yes |
| `infrastructure/network/GitHubApiService.kt` | GitHub API service | ✅ Yes |
| `shared/data/model/Music.kt` | Song model | ✅ Yes |
| `shared/data/model/LyricsData.kt` | Lyrics data models | ✅ Yes |
| `shared/data/model/LyricsSourcePreference.kt` | Lyrics source enum | ✅ Yes |
| `shared/data/model/UserAudioDevice.kt` | Audio device model | ✅ Yes |
| `shared/data/model/AutoEQProfile.kt` | Auto-EQ profile model | ✅ Yes |

**⚠️ Pre-requisite:** Before deleting the network files, update `FieldMindApplication.kt` to remove the `NetworkClient.initialize()` call.

---

### 2.3 Shared Files 🔄 (Used by BOTH — Needs Careful Handling)

These files serve as the "shared app shell" that both FieldMind and the original Rhythm used. FieldMind cannot function without them, but they also contain Rhythm-specific code.

| File | Kept For | Rhythm-only Content |
|------|----------|---------------------|
| `shared/presentation/theme/Theme.kt` (`RhythmTheme`) | MainActivity + CrashActivity use it for theming | `getAlbumArtColorScheme()` is dead code (color extractor deleted). `getCustomColorScheme()` has ~15 music-themed presets (Warm, Cool, Forest, etc.) |
| `shared/presentation/theme/Color.kt` | Color definitions for `Theme.kt` | Music-specific colors (`MusicPrimary*`, `PlayerButtonColor*`, `PlayerProgressColor*`, `PlayerBackground*`) are defined but never referenced except via `RhythmColors` |
| `shared/presentation/theme/Type.kt` | Typography for `Theme.kt` | None — pure typography scale |
| `shared/presentation/theme/Shape.kt` | Shapes for `Theme.kt` | `MusicShapes` object and `ExpressiveShapeTokens` are unused |
| `shared/presentation/theme/ExtendedTheme.kt` (`RhythmColors`) | **Used by FieldMind** — `ProjectPhase5Components`, `InsightsPhase9`, `HypothesesPhase8` import `RhythmColors` | Uses `RhythmColors.warning` and `RhythmColors.success` |
| `shared/presentation/theme/Dimensions.kt` | Dimension constants for Theme | Unknown (haven't read) |
| `shared/presentation/theme/MaterialShapesUtils.kt` | Shape utilities | Unknown |
| `shared/presentation/viewmodel/ThemeViewModel.kt` | MainActivity uses it for theme state | None — purely about dark mode, system theme, dynamic colors |
| `shared/presentation/components/icons/Icon.kt` | FieldMind screens render icons | None — custom icon rendering |
| `shared/presentation/theme/festive/FestiveOverlay.kt` | Festive overlay decoration | Already cleaned up — references to deleted settings replaced with defaults |
| `shared/presentation/theme/festive/FestiveTheme.kt` | Festive theme types/engine | None |
| `shared/presentation/theme/festive/ChristmasDecorations.kt` | Christmas decoration rendering | None |
| `shared/presentation/theme/festive/SnowfallEffect.kt` | Snowfall particle effect | None |
| `shared/presentation/theme/festive/Snowflake.kt` | Snowflake model | None |
| `shared/presentation/theme/festive/FestiveSplashGreeting.kt` | Festive splash greeting | None |
| `shared/presentation/theme/festive/FestiveThemeExamples.kt` | Preview examples | None |
| `shared/data/model/Transition.kt` | Transition animation model | Unknown |
| `shared/data/model/AppSettings.kt` | EVERYTHING uses it | Already cleaned up — stripped from ~5300 to ~309 lines |

---

## 3. Cross-Dependency Analysis

### 3.1 Rhythm Colors Used by FieldMind

FieldMind uses `RhythmColors` from `ExtendedTheme.kt` in 3 components:

```
RhythmColors.warning → ProjectPhase5Components.kt (line 65, 72, 277)
RhythmColors.warning → InsightsPhase9.kt (line 88)
RhythmColors.warning → HypothesesPhase8.kt (line 43)
RhythmColors.success → ProjectPhase5Components.kt (line 279)
```

**Action:** Replace with `MaterialTheme.colorScheme` equivalents to break dependency on `ExtendedTheme.kt`, or keep `ExtendedTheme.kt` as-is (it's lightweight).

### 3.2 RhythmTheme Used by FieldMind

Both `MainActivity.kt` and `CrashActivity.kt` use `RhythmTheme` from `Theme.kt`:

```
MainActivity.kt:   RhythmTheme(darkTheme, amoledTheme, dynamicColor, ...) { ... }
CrashActivity.kt:  RhythmTheme { CrashScreen(...) }
```

The `RhythmTheme` composable provides the Material3 `MaterialTheme` with color scheme, typography, and shapes. FieldMind's own `FieldMindTheme` wraps inside it for brand colors. **This cannot be removed** without creating a standalone FieldMind theme that provides the same M3 foundation.

### 3.3 AppSettings as Cross-Cutting Concern

`AppSettings.kt` was already cleaned up (309 lines). It's used by:
- `FieldMindNavigation.kt` → `onboardingCompleted`
- `MainActivity.kt` → theme settings
- `ThemeViewModel.kt` → theme settings
- `CrashReporter.kt` → `addCrashLogEntry()`
- `FestiveOverlay.kt` → festive settings
- `FieldMindApplication.kt` → initialization
- `NetworkClient.kt` → **dead code** — safe to remove once NetworkClient is deleted

### 3.4 Icon.kt Dependency

`Icon.kt` at `shared/presentation/components/icons/Icon.kt` is used by ALL FieldMind screens via `FieldMindIcons` (in `features/field/presentation/components/FieldMindIcons.kt`). This is pure utility — no Rhythm content.

---

## 4. String Resources Analysis

### 4.1 Strings.xml (~320,000 chars)

The main `strings.xml` is shared but **~95% is Rhythm music-player content**.

#### Strings that ARE used by FieldMind:

| String Key | Used By |
|------------|---------|
| `app_name` | Manifest / launcher |
| `onboarding_welcome_*` | `FieldMindOnboardingScreen.kt` |
| `widget_fieldmind_*` | Glance widgets |
| `media3_notification_channel_*` | Legacy notification config |
| `crash_*` | `CrashActivity.kt` |
| `updates_rhythm_logo_cd`, `cd_rhythm_splash` | `CrashActivity.kt` |
| `common_rhythm` | Unclear (the app name) |

#### Strings that are Rhythm-ONLY (estimated count: 2,000+ strings):

- ALL `player_*` strings (player screen text)
- ALL `library_*` strings (music library text)
- ALL `playlist_*` strings (playlist management)
- ALL `queue_*` strings (playback queue)
- ALL `equalizer_*`, `bass_boost_*`, `virtualizer_*` strings
- ALL `lyrics_*` strings
- ALL `sleep_timer_*` strings
- ALL `search_*` strings (music search)
- ALL `streaming_*` strings
- ALL `home_quote_*` strings (time-based quotes)
- ALL `home_mood_*` strings
- ALL `home_stat_*` strings
- ALL `recently_played_*` strings
- ALL `rhythm_guard_*` strings (renamed from hearing health to "FieldMind Focus" in some places)
- ALL `cache_*` strings (cache management)
- ALL `broadcast_status_*` strings
- ALL `bluetooth_lyrics_*` strings
- ALL `scrobbling_*` strings
- ALL `discord_*` strings
- ALL `backup_*` strings (overlaps with FieldMind backup)
- ALL `canvas_*` strings
- ALL `beta_*` strings
- ALL `badge_*` strings (audio quality badges)
- ALL `song_info_*` strings
- ALL `miniplayer_*` strings
- ALL `festive_greeting_*` strings
- ALL `notification_*` strings (media scan, updater, streaming, etc.)
- ALL `operation_*` strings
- ALL `theme_*` strings (overlaps with FieldMind theme settings)
- ALL `settings_*` strings that reference music features

### 4.2 Translated Strings (10 locales)

All translated `values-*/strings.xml` files contain **100% Rhythm music-player content**. FieldMind strings in English were never translated. These can be deleted entirely.

**Locales to delete (10 files):**
- `values-it/strings.xml`
- `values-pt/strings.xml`
- `values-pt-rBR/strings.xml`
- `values-ko/strings.xml`
- `values-ja/strings.xml`
- `values-sv/strings.xml`
- `values-uk/strings.xml`
- `values-in/strings.xml`
- `values-ru/strings.xml`
- `values-pl/strings.xml`
- `values-nl/strings.xml`

### 4.3 Drawable Resources

The following Rhythm drawables are used by FieldMind:

| Drawable | Used By |
|----------|---------|
| `ic_notification.xml` | `FieldMindStreakWorker.kt`, `FieldMindDashboardWidget.kt`, `FieldMindQuickCaptureWidget.kt` |

The following are Rhythm-only (safe to delete):

| Drawable | Purpose |
|----------|---------|
| `ic_play_arrow.xml` | Music playback |
| `ic_pause.xml` | Music playback |
| `ic_skip_next.xml` | Music playback |
| `ic_skip_previous.xml` | Music playback |
| `ic_music_note.xml` | Music player |
| `ic_hd.xml` | Audio quality badge |
| `ic_hq.xml` | Audio quality badge |
| `ic_lossy.xml` | Audio quality badge |
| `ic_high_res.xml` | Audio quality badge |
| `ic_cd.xml` | Music player |
| `ic_dts.xml` | Audio format |
| `ic_dolby.xml` | Audio format |
| `ic_surround_sound.xml` | Audio format |
| `ic_favorite_filled.xml` | Favorites |
| `ic_favorite_border.xml` | Favorites |
| `ic_small.xml` | Widget |
| `album_art_background.xml` | Player |
| `rhythm_logo.xml` | Splash |
| `rhythm_icon_small.xml` | Launcher |
| `rhythm_splash_logo.xml` | **⚠️ Used by CrashActivity.kt** (`R.drawable.rhythm_splash_logo`) |
| `splash_particles.xml` | Splash screen |
| `widget_preview.xml` | Widget preview |
| `widget_preview_lyric.xml` | Widget preview |
| `widget_preview_glance.xml` | **⚠️ Used by Glance widgets config** |
| `widget_button_background.xml` | Widget style |
| `widget_button_background_primary.xml` | Widget style |
| `widget_background.xml` | Widget style |

### 4.4 Layout Files

| Layout | Purpose | Delete? |
|--------|---------|---------|
| `layout/widget_music_extra_large.xml` | Music widget | ✅ Yes |
| `layout/widget_music_large.xml` | Music widget | ✅ Yes |
| `layout/widget_music_medium.xml` | Music widget | ✅ Yes |
| `layout/widget_music_small.xml` | Music widget | ✅ Yes |
| `layout/widget_music_extra_small.xml` | Music widget | ✅ Yes |
| `layout/widget_music_5x5.xml` | Music widget | ✅ Yes |
| `layout/widget_music_wide.xml` | Music widget | ✅ Yes |
| `layout/widget_music_vertical.xml` | Music widget | ✅ Yes |
| `layout/widget_fieldmind_dashboard.xml` | FieldMind widget config | ❌ Keep |
| `layout/widget_fieldmind_quick_capture.xml` | FieldMind widget config | ❌ Keep |

### 4.5 Color/Font/XML Resources

| File | Purpose | Delete? |
|------|---------|---------|
| `color/sl_lyric_active.xml` | Active lyric color | ✅ Yes |
| `color/sl_lyric_text.xml` | Lyric text color | ✅ Yes |
| `font/material_symbols_outlined.ttf` | Required by Icon.kt | ❌ Keep |
| `font/geom.ttf` | Default FieldMind font | ❌ Keep |
| `xml/lyric_widget.xml` | Lyric widget config | ✅ Yes |
| `xml/widget_info.xml` | Music widget config | ✅ Yes |
| `xml/widget_info_glance.xml` | Glance widget config | ❌ Keep |
| `xml/widget_info_fieldmind_dashboard.xml` | FieldMind widget info | ❌ Keep |
| `xml/widget_info_fieldmind_quick_capture.xml` | FieldMind widget info | ❌ Keep |

---

## 5. Dependency Graph Summary

```
                    ┌─────────────────────────────────────┐
                    │         FieldMind Core               │
                    │  (features/field/ ~80 files)         │
                    └────┬──────────┬──────────┬───────────┘
                         │          │          │
              ┌──────────┘          │          └──────────┐
              ▼                     ▼                      ▼
     ┌──────────────┐    ┌──────────────────┐   ┌────────────────┐
     │  RhythmTheme  │    │  AppSettings.kt  │   │   Icon.kt      │
     │  (Theme.kt)   │    │  (cleaned up)    │   │  (pure util)   │
     └──────┬───────┘    └──────────────────┘   └────────────────┘
            │
     ┌──────┴──────────┐
     ▼                 ▼
┌──────────┐   ┌──────────────┐
│ Color.kt │   │ Shape.kt     │
│ Type.kt  │   │ Dims.kt      │
└──────────┘   └──────────────┘
     ▲
     │
┌────┴────────────┐
│ ExtendedTheme.kt│ ← Used by ProjectPhase5, InsightsPhase9, HypothesesPhase8
│ (RhythmColors)  │
└─────────────────┘
```

---

## 6. Deletion Recommendations (Priority Order)

### Phase 1: Safe to Delete Now (no dependencies)

| Priority | Files | Risk |
|----------|-------|------|
| 🔴 P0 | `infrastructure/network/` — ALL 10 files | Low — must update FieldMindApplication.kt first |
| 🔴 P0 | `shared/data/model/Music.kt` | Low |
| 🔴 P0 | `shared/data/model/LyricsData.kt` | Low |
| 🔴 P0 | `shared/data/model/LyricsSourcePreference.kt` | Low |
| 🔴 P0 | `shared/data/model/UserAudioDevice.kt` | Low |
| 🔴 P0 | `shared/data/model/AutoEQProfile.kt` | Low |
| 🔴 P0 | Translated `values-*/strings.xml` (10 files) | None — not referenced |
| 🟡 P1 | All music layout files (8 files) | Low |
| 🟡 P1 | Rhythm-only drawables (most of drawable/) | Medium — check `rhythm_splash_logo` used by CrashActivity |
| 🟡 P1 | `color/sl_lyric_active.xml`, `color/sl_lyric_text.xml` | Low |
| 🟡 P1 | `xml/lyric_widget.xml`, `xml/widget_info.xml` | Low |

### Phase 2: Requires Code Changes

| Priority | Files | Action Needed |
|----------|-------|---------------|
| 🟡 P1 | `shared/data/model/AppSettings.kt` | Already cleaned ✅ |
| 🟡 P1 | `shared/presentation/theme/festive/FestiveThemeExamples.kt` | Only used for previews — can delete |
| 🟡 P1 | `shared/presentation/theme/Shape.kt` `MusicShapes` | Can remove `MusicShapes` and `ExpressiveShapeTokens` objects |
| 🟡 P1 | `shared/presentation/theme/Color.kt` music colors | Can remove `MusicPrimary*`, `Player*`, `Success*`, `Warning*` if refactoring `RhythmColors` |
| 🟡 P1 | `shared/presentation/theme/Theme.kt` `getAlbumArtColorScheme()` | Dead code — already returns default |
| 🟡 P1 | `shared/presentation/theme/ExtendedTheme.kt` | Replace `RhythmColors.warning/success` with `MaterialTheme.colorScheme.*` in 3 files |

### Phase 3: Major Refactoring

| Priority | Files | Action Needed |
|----------|-------|---------------|
| 🔵 P2 | `strings.xml` | Strip ~95% Rhythm strings — time-consuming to identify all |
| 🔵 P2 | `shared/presentation/theme/Theme.kt` | Replace `RhythmTheme` with a slimmer `FieldMindBaseTheme` that provides M3 foundation |
| 🔵 P2 | `shared/presentation/theme/Color.kt` | Remove music color presets after refactoring Theme.kt |

---

## 7. Pre-requisite: Update FieldMindApplication.kt

Before deleting `infrastructure/network/`, remove the `NetworkClient.initialize()` call:

```kotlin
// DELETE these lines:
fieldmind.research.app.network.NetworkClient.initialize(
    AppSettings.getInstance(applicationContext)
)
Log.d(TAG, "✓ NetworkClient initialized")
```

This removes the only runtime reference to the entire network package.

---

## 8. Quick Stats

- **Total Kotlin source files:** ~125
- **FieldMind Core:** ~80 (64%)
- **Rhythm (unused):** ~15 (12%)
- **Shared (both):** ~20 (16%)
- **Infrastructure/network:** ~10 (8%)
- **String resources to clean:** ~320,000 chars → ~95% Rhythm
- **Drawable resources to clean:** ~35 files → ~25 Rhythm
- **Layout resources to clean:** 8 music widgets

---

## 9. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| `rhythm_splash_logo` appears in CrashActivity | Crash screen shows Rhythm logo | Replace with FieldMind logo or remove image |
| `RhythmColors` used by 3 FieldMind files | Compilation error if removed | Replace with `MaterialTheme.colorScheme` |
| `RhythmTheme` used by MainActivity + CrashActivity | App won't compile without it | Keep as shared shell until FieldMind has its own M3 base theme |
| Theme presets (Warm, Cool, etc.) unused | Dead code but harmless | Remove when refactoring Theme.kt |
