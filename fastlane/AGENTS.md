# Fastlane — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`. It follows the root `AGENTS.md` as its parent DOX rail.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `fastlane/AGENTS.md` (this file)

Read `master.md` and root `AGENTS.md` first, then this file for fastlane-specific contracts.

## Purpose

App store metadata, changelogs, and deployment configuration for distributing FieldMind on Android app stores.

## Ownership

- `fastlane/metadata/android/` — Store listings organized by locale
- `fastlane/metadata/android/en-US/` — English (US) store metadata
- `fastlane/metadata/android/en-US/full_description.txt` — Full store description
- `fastlane/metadata/android/en-US/short_description.txt` — Short tagline
- `fastlane/metadata/android/en-US/changelogs/` — Version-specific changelogs identified by version code
  - Files named `{versionCode}.txt` (e.g., `33275784.txt`, `45351940.txt`)

## Local Contracts

### Store Metadata
- `full_description.txt` — Comprehensive app description (up to 4000 characters)
- `short_description.txt` — Brief tagline (up to 80 characters)
- `changelogs/{versionCode}.txt` — What's new in each release (up to 500 characters)

### Naming
- Changelog files are named by Android `versionCode` (integer), not version name
- Locale codes follow Android convention (e.g., `en-US`, `de`, `fr`)

## Work Guidance

- When adding a new release, create a changelog file with the correct version code
- Update `full_description.txt` only when product positioning or major features change
- Keep `short_description.txt` concise and actionable
- For new locales, create a new locale directory (e.g., `de/`) and add translated metadata
- Version screenshots belong in `fastlane/metadata/android/{locale}/images/` (not yet created)

## Verification

- Metadata validated during app store submission
- Changelog content should match the release's actual changes
- Run `fastlane supply init` to validate metadata structure

## Child DOX Index

No child AGENTS.md files defined yet.
