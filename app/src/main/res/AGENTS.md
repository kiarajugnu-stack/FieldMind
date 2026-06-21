# Android Resources — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `app/AGENTS.md` ← `res/AGENTS.md` (this file)

Read parent docs first, then this file for resource-specific contracts.

## Purpose

The `res/` directory contains all Android application resources: layouts, drawables, strings, themes, colors, animations, fonts, and XML configuration files. These define the app's visual appearance and static content.

## Ownership

- `values/` — Core resource values: `strings.xml`, `colors.xml`, `themes.xml`, `dimens.xml`, `media3.xml`, `widget_colors.xml`
- `values-night/` — Night-mode overrides: `colors.xml`
- `layout/` — XML layouts for widgets: `widget_fieldmind_dashboard.xml`, `widget_fieldmind_quick_capture.xml`
- `drawable/` — Drawable resources: launcher icons, widget previews, notification icons
- `drawable-nodpi/` — Density-independent drawables
- `mipmap-*/` — Launcher icons at various densities (anydpi, hdpi, mdpi, xhdpi, xxhdpi, xxxhdpi)
- `anim/` — Animation resources: `slide_up_fade_in.xml`, `pulse_bounce.xml`
- `font/` — Custom fonts: `material_symbols_outlined.ttf`, `geom.ttf`
- `xml/` — XML configs: `file_paths.xml`, `backup_rules.xml`, `data_extraction_rules.xml`, `locale_config.xml`, widget info files (`widget_info_*.xml`)

## Local Contracts

### Naming Conventions
- Resource files use snake_case (e.g., `widget_fieldmind_dashboard.xml`)
- String keys use snake_case with feature prefix (e.g., `widget_fieldmind_quick_capture`)
- Drawables use descriptive names (e.g., `ic_launcher_foreground.xml`, `widget_preview_glance.xml`)
- Mipmap resources follow Android density naming conventions

### Widget Layouts
- Widget XML layouts in `layout/` match glance composable specs in `infrastructure/widget/glance/`
- Widget info configs in `xml/` (e.g., `widget_info_fieldmind_dashboard.xml`)
- Widget preview drawables in `drawable/`

### Themes & Colors
- Base theme in `values/themes.xml` — inherited from Material3
- Dynamic color supported via MaterialColorUtilities
- Night mode colors in `values-night/colors.xml`
- Widget-specific colors in `values/widget_colors.xml`

## Work Guidance

- Add new strings to `values/strings.xml` — never hardcode strings in Kotlin
- Add new colors to `values/colors.xml` with descriptive names
- Widget changes require updating both the Kotlin glance composable AND the corresponding XML files
- New fonts go in `font/` directory and reference via `@font/` in XML or `R.font` in Kotlin
- Night-mode variants go in `values-night/` — always provide both light and dark values

## Verification

- Resources compile correctly (validated by Android build)
- String resources have no missing translations (check with lint)
- Widget XML layouts match glance composable specs
- Theme applied correctly in both light and dark modes

## Child DOX Index

No child AGENTS.md files defined yet.
