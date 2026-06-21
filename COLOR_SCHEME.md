# FieldMind Color Scheme Unified

## Primary Brand Color
- **Forest Green**: `#1F6B4C` - Used for primary actions, brand identity, and key UI elements

## Semantic Colors (Theme System)
All accent colors come from `FieldMindTheme.colors` and are theme-aware (auto-adapt to light/dark mode):

### Research Entity Types
- **Observation**: `#2E7D32` (observation green)
- **Question**: `#1565C0` (question blue)
- **Hypothesis**: `#8B5000` (hypothesis amber)
- **Project**: `#00695C` (project teal)
- **Source**: `#5E35B1` (source purple)
- **Data**: `#006D7A` (data teal)
- **Report**: `#A1531F` (report burnt orange)
- **Flashcard**: `#AD1457` (flashcard magenta)

### Confidence Levels
- **Sure/High**: `#2E7D32` (confident green)
- **Guess/Low**: `#8B5000` (guessing amber)
- **Verify**: `#C62828` (needs verification red)

### States
- **Positive**: `#2E7D32` (green)
- **Warning**: `#8B5000` (amber)
- **Info**: `#455A64` (slate)

## Categorical Colors (10-color palette)
Used for field categories in setup and other categorical data:
1. `#2E7D32` - Green (Zoology)
2. `#1565C0` - Blue (Astronomy)
3. `#8B5000` - Amber (Ecology)
4. `#5E35B1` - Violet (Other)
5. `#006D7A` - Teal (Botany)
6. `#AD1457` - Magenta
7. `#00695C` - Deep Teal
8. `#D84315` - Burnt Orange
9. `#455A64` - Slate
10. `#6D4C41` - Brown (Geology)

## Pages Updated
✅ Onboarding Screen (Interests & Permissions)
✅ Report Screen (Category colors)
✅ Backup/Export Screen (Format colors)

## Implementation Rules
1. **Use `FieldMindTheme.colors.*`** for any semantic meaning (observations, questions, etc.)
2. **Use `MaterialTheme.colorScheme.primary`** for main brand interactions
3. **Use `FieldMindTheme.colors.categorical[index]`** for arbitrary categories
4. **Avoid hardcoded color values** - always reference the theme system
5. Keep light mode observation green (`#2E7D32`) as default for things matching primary brand intent
