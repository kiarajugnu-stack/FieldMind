# Infrastructure Layer — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `app/AGENTS.md` ← `infrastructure/AGENTS.md` (this file)

Read parent docs first, then this file for infrastructure-specific contracts.

## Purpose

The `infrastructure/` package contains system-level code that supports the application: background workers, app widgets, and cross-cutting services. This layer is feature-agnostic and provides reusable infrastructure for the feature modules.

## Ownership

### Workers (`worker/`)
- `FieldMindBackupWorker.kt` — Periodic data backups (settings, observations, notes, projects)
- `FieldMindStreakWorker.kt` — Streak tracking and statistics maintenance

### Widgets (`widget/glance/`)
- `FieldMindDashboardWidget.kt` — Glance dashboard widget (recent observations, stats, active projects)
- `FieldMindQuickCaptureWidget.kt` — One-tap observation quick capture widget
- `FieldMindWidgetReceiver.kt` — Widget broadcast receiver and click handling

## Local Contracts

### Workers
- Extend `CoroutineWorker` from WorkManager
- Scheduled via constraints (periodic time intervals, battery not low)
- Handle errors gracefully; report failures via `Result.retry()` or `Result.failure()`
- No long-running work; respect WorkManager's 10-minute execution limit

### Widgets
- Use `androidx.glance` with Material3 glance components
- Widget layouts defined in `app/src/main/res/xml/` matching glance composable specs
- Three widget layouts: dashboard (`widget_info_fieldmind_dashboard.xml`), quick capture (`widget_info_fieldmind_quick_capture.xml`), glance base (`widget_info_glance.xml`)
- Widget preview drawables in `app/src/main/res/drawable/`
- Widgets use Glance's `StateFlow` integration for live data updates

## Work Guidance

- New background worker: extend `CoroutineWorker`, add to `worker/`, schedule via `FieldMindBackgroundScheduler`
- New widget: create glance composable in `widget/glance/`, add XML config in `res/xml/`, register in `AndroidManifest.xml`
- Widget layout XML must match glance composable structure (same composable IDs)
- Ensure widgets handle empty/null states gracefully (no crashes on first load)

## Verification

- Workers execute correctly on device (test via WorkManager test APIs)
- Widgets render and update on home screen
- No compilation errors in CI

## Child DOX Index

No child AGENTS.md files defined yet.
