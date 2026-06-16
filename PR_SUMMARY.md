# Weather UI Re-architecture + Build Stabilization

This PR delivers a major weather visualization overhaul along with extensive build error fixes across the codebase.

## 🎨 Weather UI Enhancements

### Animated Weather Scenes
- **Thunderstorm**: Lightning flashes, cloud flashes, and visual depth layers
- **Rain**: Falling rain streaks with varying speed and opacity
- **Snow**: Gentle snow particle system
- **Clear Sky**: Fireflies, moon phase rendering, star field with twinkle animation
- **Cloudy**: Layered cloud drift with parallax
- **Fog/Mist**: Soft particle overlay
- **Drizzle**: Fine rain particle system
- **All scenes**: Theme-adaptive color palettes (muted for light theme, deep for dark theme) with glass-morphism overlays

### Weather Dashboard Widget
- Expandable Orphe-style weather card with slide-in transition
- Time-of-day greeting banner
- Full-screen metric overlays (temperature, humidity, wind, pressure, UV index, visibility)
- 7-day forecast preview with animated cards
- Adaptive text coloring for readability across all weather conditions
- Haptic feedback on interactions

### Moon Phase Rendering
- Dedicated moon phase composable with rotating glow and texture
- All 8 major moon phases rendered with dedicated PNG assets
- Phase-aware alignment relative to observer position

## 🛠️ UI/UX Refactoring (Inline-to-Dialog Migration)

- **FieldMindDialogs overhaul**: Replaced `ChoiceChips` with `OptionPickerDialog` for category, behavior, life stage, sex, habitat, conservation status, and quality selection
- **MultiSelectPickerField**: New component for checklists (context presets, observation checklists)
- **Keyboard type audit**: All text fields now use proper `KeyboardType` (Number, Decimal, Password, Uri)
- **Consolidated observation form**: Merged scattered inline cards into a single structured dialog flow
- **ConfirmDeleteDialog**: Proper dismiss handling (fixes crash after onboarding)

## 🐛 Build Error Fixes

### 7 files with syntax/structural fixes:

| File | Fix |
|---|---|
| **AnimatedWeatherScene.kt** | `delayMs` → `delayMillis` in tween(); removed `.value` on delegated property |
| **AnimatedWeatherScene.kt** | Fixed modifier chaining on `ObservationWeatherCard` (`.padding()` → `Modifier.padding()`) |
| **FieldMindDetailScreen.kt** | Added missing function-closing `}` for `ObservationDetailContent` — the previous `}` was embedded in a comment and ignored by Kotlin, causing **28 cascading errors** (unresolved composables: sharePlainText, DetailActionBar, NoteDetailContent, QuestionDetailContent, HypothesisDetailContent, ProjectDetailContent, DetailBody, SourcePreviewPanel, SourceActionPanel, DataRecordDetailContent, ReportDetailContent, FlashcardDetailContent, ConfirmDeleteDialog, deleteEntityByKind, etc.) |
| **FieldMindDetailScreen.kt** | Fixed `OptionPickerField` missing closing `)` after `onSelected` lambda (line 2379) |
| **FieldMindDetailScreen.kt** | Removed extra closing `}` at line 205 that was prematurely closing `DetailScreen` (brace imbalance of -4) |
| **FieldMindHomeScreen.kt** | Removed duplicate `reshWeatherFromLocation` text from `refreshWeatherFromLocation()` call |
| **FieldMindHomeScreen.kt** | Fixed brace closure for `FieldMindHomeScreen` — was prematurely closed before `SessionObservationsCard` |
| **FieldMindLibraryScreen.kt** | Made `continuation()` a `LazyListScope` extension function (added import) |
| **FieldMindLibraryScreen.kt** | Moved `continuation()` inside parent function scope for closure access to local variables |
| **FieldMindLibraryScreen.kt** | Added `continuation()` call inside LazyColumn; restructured FlashcardPanel with NewFlashcardDialog |
| **FieldMindObserveScreen.kt** | Added missing `import androidx.compose.ui.text.input.KeyboardType` |
| **FieldMindObserveScreen.kt** | Fixed `pagerState` → `rememberPagerState` (API migration) in capture flow |
| **FieldMindQuestionsScreen.kt** | Moved 4 `item {}`/`items {}` blocks (filters, empty state, question cards, evidence correlation) inside `LazyColumn` scope |
| **FieldMindChangelogScreen.kt** | Fixed syntax errors |
| **Multiple screens** | Resolved compilation errors across Phase components, navigation, and evidence hub |
| **Infinite height constraint crash** | Replaced `AnimatedContent` inside `LazyColumn` with stable `Box` + `heightIn()` layout |
| **Capture flow** | Fixed category picker integration and custom category input handling |

### Stats
- **68 files changed**
- **+7,220 / −1,511 lines**
- **18+ commits** stabilizing the build after weather UI implementation
