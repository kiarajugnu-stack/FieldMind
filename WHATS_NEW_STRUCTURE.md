# What's New - Visual Structure & Design Breakdown

## Screen Layout

### Changelog Screen (FieldMindChangelogScreen.kt)
```
┌─────────────────────────────────────────────────────────────┐
│  What's new                                             ← [X] │
│  Complete field research redesign with 12 phases...          │
├─────────────────────────────────────────────────────────────┤
│  [✨] Phases 1-12: Complete Redesign                        │
│       From dashboard foundations to a complete               │
│       research workspace...                                  │
├─────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────┐   │
│  │ [✨] Complete Field Research Redesign (1.2.0)    [MAJOR]│   │
│  │     June 14, 2026 • v1.2.0-field-research-complete    │   │
│  │                                                         │   │
│  │     [📌] [📊] [🎯] [📈] [📁] [📝] [💡] [📚]          │   │
│  │     Tags displayed as pill chips                       │   │
│  │                                                         │   │
│  │     ─────────────────────────────────────────────      │   │
│  │                                                         │   │
│  │     Observations & Capture (Phase 3-4)                │   │
│  │     ✓ Species confidence selector                      │   │
│  │     ✓ Distance from observer selector                  │   │
│  │     ✓ Observation checklist tracking                  │   │
│  │     ✓ Structured measurements fields                   │   │
│  │     ✓ Quality score calculator (0-100%)               │   │
│  │     ✓ Follow-up scheduling system                     │   │
│  │     [... more sections ...]                           │   │
│  │                                                         │   │
│  │     ┌─────────────────────────────────────────────┐   │   │
│  │     │ [✨] Latest version with comprehensive       │   │   │
│  │     │ research features                           │   │   │
│  │     └─────────────────────────────────────────────┘   │   │
│  └───────────────────────────────────────────────────────┘   │
│  [Similar cards for v0.9.0 and v0.8.0]                      │
└─────────────────────────────────────────────────────────────┘
```

## Component Design Details

### ChangelogEntryCard Features

#### 1. Header Section (Visual Hierarchy)
- **Icon Box (56×56dp)**
  - Latest: Primary color, Sparkle icon
  - Major: Error color, Info icon
  - Minor: Tertiary color, Info icon
  - Rounded corners: 18dp
  - Background: Color with 15% alpha

- **Title Column**
  - Title: typography.titleLarge, Bold weight
  - Date & Version: labelSmall, onSurfaceVariant color
  - Icons: Calendar (14dp) before date

- **Importance Badge (Right)**
  - Rounded: 12dp, padded 12×6dp
  - Major: ErrorContainer background, OnErrorContainer text
  - Minor: TertiaryContainer background

#### 2. Tags Row
- Pill-shaped chips with rounded corners (10dp)
- Secondary container background at 60% alpha
- Each tag has padding: 10×5dp
- Medium font weight, onSecondaryContainer text color

#### 3. Feature Sections
Each section contains:
- **Section Header**
  - Accent colored bullet (6×6dp, rounded 3dp)
  - Title: titleSmall, SemiBold, accent color
  - Fills width with wrapping support

- **Feature Bullets**
  - Material Check icon (18dp) with 70% alpha
  - Icon color: Accent with alpha
  - Text: bodyMedium, onSurface color
  - Top-aligned layout for multi-line text
  - Spacing: 10dp between icon and text

#### 4. Latest Version Badge
- Rounded corners: 14dp
- Background: Primary color at 10% alpha
- Contains:
  - Sparkle icon (16dp, primary color)
  - Text: labelSmall, SemiBold, primary color
  - Padding: 12dp all around

#### 5. Card Container
- Rounded corners: 28dp
- **Latest version card:**
  - Background: PrimaryContainer at 50% alpha
  - Elevation: 4dp
  - Accent: Primary color
- **Other versions:**
  - Background: SurfaceContainerLow
  - Elevation: 0dp
  - Accent: Error (Major) or Tertiary (Minor)
- Padding: 20dp all around
- Spacing between elements: 16dp

## Material Icons Used

```kotlin
// Icons from FieldMindIcons
Icon(FieldMindIcons.Sparkle, ...)      // Latest version, celebration
Icon(FieldMindIcons.Info, ...)          // Version information
Icon(FieldMindIcons.Calendar, ...)      // Release date
Icon(FieldMindIcons.Check, ...)         // Feature bullets
```

## Color System

### Latest Version (v1.2.0)
- Primary: PrimaryContainer (50% alpha background)
- Accent: Primary color
- Icon: Sparkle in primary
- Elevation: 4dp

### Major Updates (v0.9.0)
- Primary: Error color
- Container: ErrorContainer (badges)
- Icon: Info in error color

### Minor Updates
- Primary: Tertiary color
- Container: TertiaryContainer (badges)
- Icon: Info in tertiary color

## Typography Hierarchy

```
Title:        titleLarge (Bold)
Subtitle:     bodySmall (OnSurfaceVariant)
Sections:     titleSmall (SemiBold)
Features:     bodyMedium (OnSurface)
Metadata:     labelSmall (OnSurfaceVariant)
Tags:         labelSmall (Medium, OnSecondaryContainer)
Badge:        labelSmall (SemiBold, OnErrorContainer/OnTertiaryContainer)
```

## Spacing & Padding

```
Screen padding:         20dp (all sides)
Card padding:           20dp (all sides)
Element spacing:        16dp (vertical)
Icon-to-text spacing:   10dp
Tag spacing:            8dp (horizontal between tags)
Header element spacing: 14dp
```

## Content Structure

### Version 1.2.0 (Latest)
- 12 sections covering all redesign phases
- Approximately 80+ individual features listed
- 5+ commit references
- Tags: Observations, Projects, Analysis, Reports, Library, Journal, Evidence, Data

### Version 0.9.0
- 4 sections (Dashboard, Data Table, Charts, Observations)
- Focus on insights and analytics

### Version 0.8.0
- 4 sections (Capture, Research records, Data safety, Navigation)
- Foundation features

## Animation & Interaction

- **Card elevation change**: Latest version has 4dp elevation
- **Color transitions**: Smooth Material theme transitions
- **Text rendering**: ellipsize tail for long section content
- **Lists**: verticalArrangement with spacedBy for consistent spacing
