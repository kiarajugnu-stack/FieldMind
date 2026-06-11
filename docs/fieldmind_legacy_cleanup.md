# FieldMind Legacy Cleanup Plan

FieldMind owns the active app shell through `MainActivity` and `FieldMindNavigation`. Remaining Rhythm-era modules are kept only so the repository continues to compile while data, UI, and navigation are migrated.

## Remaining inactive legacy areas to remove after a successful Android build

- `features/local`: local media library screens, metadata editing, playlist utilities, and `MusicRepositoryImpl`.
- `features/streaming`: provider clients and synchronization logic for remote media libraries.
- Player surfaces and utilities: player screens, queue utilities, playback services, media-session notifications, lyrics components, equalizer screens, sleep timer surfaces, and home-screen media widgets.
- Core music domain: `MusicRepository`, `SongUseCases`, `MediaItems`, `PlayableItem`, and old Room database code such as `RhythmDatabase` once no compiled call sites remain.
- Media dependencies: Media3 playback dependencies can be removed only after the inactive services/widgets and notification code are deleted from manifests, DI, and Gradle configuration.

## Safe order

1. Keep FieldMind navigation as the only active graph.
2. Remove or neutralize visible strings used by the launcher, onboarding, Settings, notifications, and active app surfaces.
3. Confirm a flavor build succeeds.
4. Delete unreachable music UI modules first.
5. Remove services/widgets/receivers and their resources.
6. Remove repositories/domain models and Media3 dependencies last.
7. Preserve image/vector/font/media assets unless explicit deletion approval is given.
