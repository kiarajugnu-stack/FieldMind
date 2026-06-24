# FieldMind Color Scheme — Unified Palette v2

## Brand Color
- **Forest Green**: `#1F6B4C` — Primary actions, brand identity, key UI elements

## Theme System
All accent colors come from `FieldMindTheme.colors` and auto-adapt to light/dark mode via `FieldMindTheme()`.

---

## Entity Colors (15 distinct colors)

| Token       | Light      | Dark        | Role                          |
|-------------|------------|-------------|-------------------------------|
| observation | `#2E7D32`  | `#A5D6A7`   | Observations                  |
| question    | `#1565C0`  | `#90CAF9`   | Questions                     |
| hypothesis  | `#8B5000`  | `#FFCC80`   | Hypotheses                    |
| project     | `#00695C`  | `#80CBC4`   | Projects                      |
| source      | `#5E35B1`  | `#B39DDB`   | Sources, library, reading     |
| note        | `#8E24AA`  | `#CE93D8`   | Notes (distinct from source)  |
| task        | `#00897B`  | `#4DB6AC`   | Tasks, action items           |
| folder      | `#6D4C41`  | `#BCAAA4`   | Folder containers             |
| species     | `#43A047`  | `#81C784`   | Species identification        |
| data        | `#006D7A`  | `#80DEEA`   | Data records                  |
| report      | `#A1531F`  | `#FFB74D`   | Reports                       |
| flashcard   | `#E91E63`  | `#F48FB1`   | Flashcards, quizzes           |

## State Colors (distinct from entity colors)

| Token    | Light      | Dark        | Role                              |
|----------|------------|-------------|-----------------------------------|
| positive | `#00A86B`  | `#69F0AE`   | Success, confirmations, enabled   |
| warning  | `#E67E22`  | `#FFB74D`   | Warnings, medium priority         |
| info     | `#546E7A`  | `#B0BEC5`   | Informational, neutral            |

## Confidence Colors (distinct from entity and state)

| Token            | Light      | Dark        | Role                    |
|------------------|------------|-------------|-------------------------|
| confidenceSure   | `#27AE60`  | `#81C784`   | High / Confirmed        |
| confidenceGuess  | `#F39C12`  | `#FFD54F`   | Low / Maybe             |
| confidenceVerify | `#E53935`  | `#EF9A9A`   | Needs verification      |

## Opacity Helpers

All extension functions on `Color`, available via `FieldMindTheme.colors`:

- `Color.cardBg()` — Background tint for cards/chips (auto-adjusts alpha: 0.14 light, 0.22 dark)
- `Color.cardBorder()` — Subtle border for selected state (alpha 0.40)
- `Color.muted()` — Muted text / secondary decoration (alpha 0.60)

Usage: `colors.observation.cardBg()` instead of `colors.observation.copy(alpha = 0.14f)`

---

## Implementation Rules

1. **Use `FieldMindTheme.colors.*`** for any semantic meaning (entity types, states, confidence)
2. **Use `MaterialTheme.colorScheme.primary`** for main brand interactions
3. **Use `MaterialTheme.colorScheme.error`** for error states (not hardcoded red)
4. **Use `Entity.cardBg()`** instead of `entity.copy(alpha = 0.14f)` for backgrounds
5. **Use `Entity.muted()`** instead of `entity.copy(alpha = 0.6f)` for muted text
6. **Never hardcode `Color(0xFF...)`** — always reference the theme system
7. **Never use an entity color for another entity type** — note uses `colors.note`, task uses `colors.task`, etc.

## Migration History

- v2 (this version): Added note, task, folder, species tokens. Made positive/warning/confidence colors distinct from entity colors. Added opacity helpers. Replaced 30+ hardcoded `Color(0xFF…)` values across the codebase.
- v1 (original): 8 entity colors, 3 confidence, 3 state — with duplicates (positive=observation, warning=hypothesis, confidenceSure=observation, confidenceGuess=hypothesis).
