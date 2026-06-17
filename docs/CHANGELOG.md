# Changelog

All notable changes to Rhythm will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.1] - 2026-06-16

### Added
- BackHandler with confirmation dialog for all full-screen editors (projects, sources, questions, hypotheses, reports, observations)
- Dirty content tracking (isDirty) with Discard/Keep Editing dialog on back press
- BackHandler with Save & Exit / Discard dialog on ObserveScreen to prevent losing observation progress

### Changed
- Weather animations: shooting star fade-in, moon repositioned to top-right with smaller size and tighter glow, sun repositioned to top-right with shorter rays and slower animations
- Rain animation: reduced streak count by ~40%, per-drop random phase delays for continuous scattered drops, slower fall speed
- Full-screen dialog UI: rounded Surface back button, HorizontalDivider, proper bottom padding (32dp)

### Fixed
- Crash when tapping "Add Species" or "Add Task" in Project Detail (infinite height constraint in scrollable) — reordered heightIn before verticalScroll, replaced nested LazyRow with FlowRow
- Device back button not working in full-screen editors — added BackHandler component

## [Unreleased]

### Added
- Initial release of Rhythm music player
- Modern Material 3 design
- Chromecast support
- Advanced audio controls
- Playlist management
- Metadata editing
- Equalizer and audio effects
- Dark/Light theme support
- Android Auto support

### Changed
- N/A

### Deprecated
- N/A

### Removed
- N/A

### Fixed
- N/A

### Security
- N/A

---

## Types of changes
- `Added` for new features
- `Changed` for changes in existing functionality
- `Deprecated` for soon-to-be removed features
- `Removed` for now removed features
- `Fixed` for any bug fixes
- `Security` in case of vulnerabilities

## Version Format
This project uses [Semantic Versioning](https://semver.org/):
- **MAJOR.MINOR.PATCH** (e.g., 3.7.291)
  - MAJOR: Breaking changes
  - MINOR: New features (backward compatible)
  - PATCH: Bug fixes (backward compatible)