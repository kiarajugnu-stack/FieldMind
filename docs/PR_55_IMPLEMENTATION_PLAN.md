# PR #55 — Implementation Plan & Analysis

> **Branch:** `fix/general-fixes`
> **Base:** `main`
> **Date:** June 14, 2026

---

## ✅ Completed Implementations

### 1. Weather System Enhancements

#### 1A. Humidity Parsing Fix
- **File:** `WeatherDatabaseScreen.kt`
- **Issue:** `toDoubleOrNull()` used on `Int?` type — that function only exists on `String`
- **Fix:** Replaced with `toDouble()` since `Int.toDouble()` always succeeds
- **Type inference fix:** Added explicit generic params `<ObservationEntity, Double>` to `mapNotNull` calls, and `<String, Any?>` to `mapOf`

#### 1B. Live Current Weather Card
- **File:** `WeatherDatabaseScreen.kt` — added `LiveCurrentWeatherCard` composable
- Displays: temperature, weather condition with icon, humidity, wind speed, cloud cover, pressure
- Shows when weather data is successfully fetched from Open-Meteo API
- Shows "Enable location for live weather" message when GPS permission is missing
- Shows loading spinner during refresh

#### 1C. Auto-Refresh Every 30 Minutes
- **File:** `WeatherDatabaseScreen.kt` — `LaunchedEffect(Unit)` with `while(true) { ... delay(30 * 60 * 1000L) }`
- Refreshes weather immediately on screen open
- Repeats every 30 minutes while the screen is visible

#### 1D. ViewModel Weather Method
- **File:** `FieldMindViewModel.kt` — added `suspend fun refreshWeatherFromLocation(): WeatherSnapshot?`
- Uses `FieldLocationProvider` to get device GPS coordinates
- Calls Open-Meteo API via existing `WeatherApiService`
- Handles missing permissions gracefully with `runCatching`

#### 1E. New Icons
- **File:** `FieldMindIcons.kt` — added `Cloud` and `Rainy` MaterialSymbolIcon entries
- Used by `weatherIconForCode()` function for weather condition display

---

### 2. Bluetooth Permission Lint Fix

- **File:** `AudioDeviceManager.kt`
- **Issue:** 3 calls to `bluetoothAdapter.getProfileConnectionState()` flagged as missing `BLUETOOTH_CONNECT` permission by lint
- **Fix:** Removed all 3 `getProfileConnectionState()` calls and replaced with simplified fallback logic (permission is already checked at runtime before reaching these call sites)
- **Cleanup:** Removed unused `BluetoothProfile` import

---

### 3. Database Migration (Room)

#### 3A. Proper MIGRATION_8_9
- **File:** `FieldMindDatabase.kt`
- **Issue:** Schema hash mismatch caused `IllegalStateException` crash
- **Fix:** Bumped database version 8→9 with a proper migration:
  - Adds `qualityScore`, `parentObservationId`, `followUpScheduledAt` columns to `field_observations` (if missing)
  - Adds `projectType`, `selectedMethods` columns to `field_projects` (if missing)
  - Creates `field_research_sessions` table (if not exists)
  - Creates `field_session_observations` table (if not exists)
- Uses existing `addColumnIfMissing()` helper pattern (safe, idempotent)
- Removed `fallbackToDestructiveMigration()` — data is preserved

---

### 4. Release Workflow Fixes

- **File:** `.github/workflows/release.yml`
- **Change 1:** Removed `assembleGithubDebug` build — only release builds on tags
- **Change 2:** Added `assembleFdroidRelease` — both flavors are built
- **Change 3:** Uploads both `github/release/*.apk` and `fdroid/release/*.apk` to the release
- **Version tracking:** Already works via `Version` object in `build.gradle.kts` — reads from `git describe --tags` (versionName) and `git rev-list --count HEAD` (versionCode)

---

## 🔴 Missing Features & Suggestions (Priority Order)

### P0 — Critical Fixes

| # | Feature | Description |
|---|---------|-------------|
| 1 | **Dependency Injection** | `FieldMindViewModel` directly instantiates repository/settings. No Hilt/Dagger = no unit testing. |
| 2 | **Split monolithic DAO** | Single `FieldMindDao.kt` (60+ queries), single `FieldEntities.kt` (21 entities), single repository (70+ methods). |
| 3 | **Full-text search (FTS)** | All 6 search methods use `LIKE '%query%'` — no FTS virtual tables. Scans entire tables. |
| 4 | **Fix settings init** | `FieldMindSettings.init` calls `BackgroundScheduler.syncAll()` every `getInstance()`, re-scheduling jobs. |
| 5 | **Loading/error states** | Most screens assume data is always available with no loading indicators or error handling. |
| 6 | **Remove duplicate workers** | `FieldMindBackupWorker` in `infrastructure/worker` is orphaned (active one is in `features/field/data/background/`). |

### P1 — High Priority

| # | Feature | Description |
|---|---------|-------------|
| 7 | **Weather as standalone entity** | Weather only captured as observation metadata. No forecast, no standalone weather records. |
| 8 | **Photo gallery** | No dedicated grid/lightbox for browsing observation evidence photos. |
| 9 | **Batch operations** | No multi-select for deleting/editing/exporting multiple observations. |
| 10 | **CSV/Excel export** | Only JSON and Markdown export exist. Researchers need CSV for data analysis. |
| 11 | **Evidence-first capture** | Category is required before writing. Should be: snap photo → add details freely. |
| 12 | **Home screen redesign** | No weather widget, no map section, no research session CTA. |
| 13 | **Rich text notes** | No markdown, no inline images, no checklists in notes. |
| 14 | **Progressive source form** | 15+ fields all visible at once. Needs progressive disclosure. |
|  
### P2 — Medium Priority

| # | Feature | Description |
|---|---------|-------------|
| 16 | **Audio transcription** | Voice notes recorded via Mic have no AI transcription. |
| 17 | **Species identification** | No iNaturalist/eBird integration or field guide lookup. |
| 18 | **Offline map tiles** | Map screen likely depends on network for tile loading. |
| 19 | **PDF reader** | Uses WebView/Google Docs fallback instead of native `androidx.pdf`. |
| 20 | **Custom form builder** | No way for users to design observation templates. |
| 21 | **Weather forecast** | Only current conditions; no 3–7 day forecast. |
| 22 | **Skeleton loaders** | No shimmer/loading placeholders while data loads. |
| 23 | **Empty state improvements** | Most screens show passive text instead of guided prompts. |

### P3 — Nice-to-Have

| # | Feature | Description |
|---|---------|-------------|
| 24 | **QR/Barcode scanner** | Specimen tracking in field research. |
| 25 | **Citizen science export** | Direct upload to iNaturalist, eBird, or GBIF. |
| 26 | **Collaboration/sharing** | Share projects or observations with other researchers. |
| 27 | **Photo geotagging** | Direct EXIF GPS embedding in captured photos. |
| 28 | **Behavioral event logger** | Timer-based tool for ethological behavior logging. |
| 29 | **Side-by-side hypothesis comparison** | View two hypotheses next to each other. |
| 30 | **Observation templates** | Pre-built forms for Species Observation, Water Quality, Transect Survey, etc. |
| 31 | **Cloud sync preparation** | UUID-based content hashing, sync-ready entity fields. |

---

## 📋 PR #55 File Summary

| File | Change |
|------|--------|
| `WeatherDatabaseScreen.kt` | Live weather card, 30-min auto-refresh, fix humidity parsing, type inference fixes |
| `FieldMindViewModel.kt` | Added `refreshWeatherFromLocation()` method |
| `FieldMindIcons.kt` | Added `Cloud` and `Rainy` icons |
| `AudioDeviceManager.kt` | Removed `getProfileConnectionState()` calls (lint fix), removed unused import |
| `FieldMindDatabase.kt` | Bumped v8→v9, proper MIGRATION_8_9, removed destructive fallback |
| `.github/workflows/release.yml` | Removed debug build, added fdroid flavor, uploads both APK flavors |

---

*Generated for PR #55 — `fix/general-fixes` → `main`*
