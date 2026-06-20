# Android Application Module — AGENTS.md

## Purpose

The `app/` module is a single-module Android application written in Kotlin that ships two integrated products:
1. **FieldMind** — a field research tool for scientists and citizen scientists (observations, species ID, hypotheses, evidence, maps, data collection)

## Ownership

- `app/src/main/java/chromahub/rhythm/app/features/field/` — Field research feature (core product)
- `app/src/main/java/chromahub/rhythm/app/features/local/` — Local music playback
- `app/src/main/java/chromahub/rhythm/app/features/streaming/` — Streaming music (Subsonic, Jellyfin)
- `app/src/main/java/chromahub/rhythm/app/shared/` — Shared UI, theme, navigation, components
- `app/src/main/java/chromahub/rhythm/app/infrastructure/` — Network, workers, widgets, services, audio processing
- `app/src/main/java/chromahub/rhythm/app/core/` — Domain models, repositories, use cases
- `app/src/main/java/chromahub/rhythm/app/util/` — Utility functions
- `app/src/main/java/chromahub/rhythm/app/activities/` — Android activities
- `app/src/main/res/` — Resources (layouts, drawables, strings, themes, animations)
- `app/src/main/assets/` — Static assets (privacy policy, autoEQ profiles)
- `app/build.gradle.kts` — Module build configuration

## Local Contracts

### Build System
- Kotlin 2.3.x, AGP 9.2.x, KSP
- `gradle/libs.versions.toml` is the **single source of truth** for dependency versions
- Two product flavors: `fdroid` and `github` (identical features, different distribution channels)
- Two build types: `debug` (`.debug` suffix) and `release` (minified, shrunk)
- APK outputs follow pattern: `FieldMind-{version}-{variant}-{signature}-universal.apk`

### Architecture
- **UI**: Jetpack Compose with Material3
- **Database**: Room (with KSP annotation processing)
- **Navigation**: Compose Navigation
- **Async**: Kotlin Coroutines
- **Image loading**: Coil
- **Networking**: Retrofit + OkHttp
- **Background work**: WorkManager
- **DI**: No DI framework currently; manual dependency injection via constructors
- **State**: ViewModel + MutableStateFlow

### Code Conventions
- Package structure: `chromahub.rhythm.app.{module}.{layer}.{feature}`
- Compose components use `@Composable` with explicit naming
- ViewModels expose state via `StateFlow`
- Room entities annotated with `@Entity`, DAOs with `@Dao`

### Key Dependencies
- `androidx.media3` — ExoPlayer-based media playback
- `androidx.glance` — App widgets (Material3 glance)
- `osmdroid` — Offline OpenStreetMap tiles
- `androidx.camera` — In-app camera capture
- `androidx.biometric` — Privacy lock
- `com.google.android.gms:play-services-location` — Geo-fencing

## Work Guidance

- When adding new dependencies, update `gradle/libs.versions.toml` first, then reference via `libs.`
- Compose compiler is managed by `kotlin.plugin.compose` — no manual compiler version needed
- Room schema exports should be committed for migration history
- Widget layouts in `res/xml/` must match glance composable specs

## Verification

- Run `./gradlew lint` for static analysis
- Run `./gradlew assembleGithubDebug` for a compile check
- APK outputs verified by CI (`app/build/outputs/apk/`)
- No unit test suite defined yet; manual testing on device/emulator

## Child DOX Index

No child AGENTS.md files defined yet. This is a single-module app without nested durable sub-boundaries at this time.
