# Shared Code Layer — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `app/AGENTS.md` ← `shared/AGENTS.md` (this file)

Read parent docs first, then this file for shared-layer-specific contracts.

## Purpose

The `shared/` package contains code shared across the entire app: base theme system, common UI components, shared data models, and cross-cutting presentation logic. Everything here is reusable and feature-agnostic.

## Ownership

### Data Layer (`shared/data/`)
- `model/AppSettings.kt` — Global application settings data model (onboarding, theme, beta, crash reporting, festive overlays)

### Presentation Layer (`shared/presentation/`)
- `theme/` — Base theme system
  - `Color.kt` — Color definitions and schemes
  - `Theme.kt` — MaterialTheme configuration (dynamic color, light/dark schemes)
  - `Type.kt` — Typography system
  - `Shape.kt` — Shape definitions
  - `Dimensions.kt` — Spacing and dimension constants
  - `festive/` — Festive overlay system (Christmas, New Year, Halloween, Valentine's)
    - `FestiveTheme.kt` — `FestiveThemeEngine` for date-based decoration activation
    - `FestiveSplashGreeting.kt`, `ChristmasDecorations.kt`, `SnowfallEffect.kt`, `Snowflake.kt`
- `viewmodel/ThemeViewModel.kt` — Theme state and preference management
- `components/icons/` — Shared icon components (`Icon.kt`)

## Local Contracts

### Theme System
- `Theme.kt` is the single entry point for MaterialTheme setup
- Dynamic color (Monet) is primary; custom schemes and album-art-based colors are fallbacks
- Night mode supported via `values-night/` resource overrides
- Festive themes auto-activate based on calendar dates; configurable via settings

### Shared Components
- Icons in `components/icons/` package — shared across all features
- No feature-specific logic in shared package
- All shared components are stateless or accept state as parameters

## Work Guidance

- Add to `shared/` only when code is used by multiple features or is truly app-wide
- Keep theme configuration centralized — do not create separate theme files in feature packages
- Festive overlays: new holiday themes go in `theme/festive/` and register in `FestiveThemeEngine`
- Do not add feature-specific business logic to this package

## Verification

- Theme renders correctly in both light and dark modes
- Festive overlays activate/deactivate on correct dates
- No feature-specific imports from feature packages (shared must not depend on features)

## Child DOX Index

No child AGENTS.md files defined yet.
