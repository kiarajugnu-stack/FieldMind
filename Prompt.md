# Prompt.md — Research & Analysis Log

## DOX Framework

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `Prompt.md` (this file — work log)

## Request Summary

Rename package from `chromahub.rhythm.app` → `fieldmind.research.app` across the entire project:
- Move directory structure: `chromahub/rhythm/app/` → `fieldmind/research/app/`
- Update all remaining old package declarations
- Update all documentation, config, and XML files

## Context Gathered

### Build/Manifest Already Correct
- `app/build.gradle.kts`: namespace = `fieldmind.research.app`, applicationId = `fieldmind.research.app` ✓
- `AndroidManifest.xml`: All class references already use `fieldmind.research.app.*` ✓

### State of Source Files
- **100+ .kt files** in `app/src/main/java/chromahub/rhythm/app/` — already have `package fieldmind.research.app.*` declarations
- **4 .kt files** still have old `package chromahub.rhythm.app.*` declarations:
  - `ClipboardSecurityUtils.kt`
  - `HardenedTextFieldUtils.kt`
  - `AppLifecycleManager.kt`
  - `StatsTimeRangeTest.kt` (test)
- **Test directory**: `app/src/test/java/chromahub/rhythm/app/` — contains stats test

### Documentation Files with Old References
- `README.md`, `all_kt_files.txt`, `AGENTS.md`
- All wiki files: `Architecture.md`, `Build-Instructions.md`, `Contributing.md`, `FAQ.md`, `Getting-Started.md`, `Home.md`, `Installation-Guide.md`, `Permissions.md`, `Technology-Stack.md`, `Troubleshooting.md`
- `docs/FIELDMIND_VS_RHYTHM_FILE_ANALYSIS.md`
- `app/AGENTS.md`
- `app/src/main/res/values/strings.xml` (rhythm references)
- `app/src/main/res/xml/backup_rules.xml`

