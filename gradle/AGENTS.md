# Gradle Build Configuration — AGENTS.md

## Purpose

Centralized Gradle build system configuration for the FieldMind Android project. Manages dependency versions, plugin versions, and the Gradle wrapper.

## Ownership

- `gradle/libs.versions.toml` — **Version catalog**: single source of truth for all dependency and plugin versions
- `gradle/wrapper/gradle-wrapper.properties` — Gradle wrapper distribution configuration

## Local Contracts

### Version Catalog (`libs.versions.toml`)
- **All** dependency and plugin versions MUST be declared here, not hardcoded in `build.gradle.kts`
- Only versions that need explicit pinning belong here; Compose BOM-managed versions should not be duplicated
- Sections: `[versions]`, `[libraries]`, `[plugins]`

### Key Version Ground Rules
- AGP: `9.2.1`
- Kotlin: `2.3.21`
- KSP: `2.3.6` (matches Kotlin version)
- Compose BOM: `2026.05.01`
- Material3: `1.5.0-alpha20`
- Target/Compile SDK: `37`
- Min SDK: `26`

### Naming Convention
Library keys follow the pattern: `{group}-{artifact}` with dots replaced by dashes.
Examples:
- `androidx-core-ktx` → `androidx.core:core-ktx`
- `com-squareup-retrofit2-retrofit` → `com.squareup.retrofit2:retrofit`
- `io-coil-kt-coil-compose` → `io.coil-kt:coil-compose`

### Plugin Keys
- `android-application` → `com.android.application`
- `kotlin-compose` → `org.jetbrains.kotlin.plugin.compose`
- `ksp` → `com.google.devtools.ksp`

## Work Guidance

- **Always** add new dependencies to `libs.versions.toml` first, then reference via `libs.` in build files
- When updating Kotlin, ensure KSP and Compose Compiler plugin versions match
- Do not duplicate version declarations — `libs.versions.toml` is the authority
- The Gradle wrapper (`gradlew` / `gradlew.bat`) is checked in; update with `gradle wrapper --gradle-version {version}`

## Verification

- Run `./gradlew --version` to verify wrapper
- Run `./gradlew lint` to verify dependency resolution
- Run `./gradlew dependencies` to inspect full dependency tree

## Child DOX Index

No child AGENTS.md files defined yet.
