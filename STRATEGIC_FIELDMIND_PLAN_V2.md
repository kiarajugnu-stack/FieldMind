# 🚀 FieldMind Strategic Redesign Plan V2 (Comprehensive)

> **Date:** June 14, 2026  
> **Status:** Complete strategic analysis + implementation roadmap  
> **Consolidated from:** 3 analysis files + research-backed suggestions  
> **Focus:** Groundbreaking features, UI overhaul, architecture restructuring

---

## Executive Summary

FieldMind has solid fundamentals (21 Room entities, Canvas charts, SM-2 spaced rep, AI assistant, offline-first). However, the path to market dominance requires:

1. **Competitive differentiation** — 3-4 groundbreaking features competitors lack
2. **UI/UX revolution** — Move from "toolkit" to "research operating system"
3. **Architecture modernization** — Eliminate god objects, add DI, refactor data layer
4. **Mobile-first paradigm** — Bigger touch targets, faster capture, minimal friction

This plan merges all existing analysis + new insights into a prioritized, implementable roadmap.

---

## Table of Contents

1. [Competitive Landscape Analysis](#1-competitive-landscape-analysis)
2. [Groundbreaking Features (New)](#2-groundbreaking-features-new)
3. [Missing Opportunities (Analysis Gap)](#3-missing-opportunities-analysis-gap)
4. [UI/UX Redesign Strategy](#4-uiux-redesign-strategy)
5. [Architecture Restructuring](#5-architecture-restructuring)
6. [Phased Implementation Roadmap](#6-phased-implementation-roadmap)
7. [Custom Research Recommendations](#7-custom-research-recommendations)

---

## 1. Competitive Landscape Analysis

### 1.1 Current FieldMind Competitive Advantages

| Feature | FieldMind | iNaturalist | ODK Collect | KoboToolbox | Fulcrum | QField |
|---------|-----------|-------------|-------------|------------|---------|--------|
| **Offline-first knowledge graph** | ✅ Unique | ❌ | ❌ | ❌ | ❌ | ❌ |
| **SM-2 spaced repetition** | ✅ Unique | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Canvas-based charts** (fully offline) | ✅ Unique | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Research Session (timer + multi-obs)** | ✅ Unique | ❌ | ❌ | ❌ | ❌ | ❌ |
| **AI assistant on-device** | ✅ | ❌ | ❌ | ✅ (cloud) | ❌ | ❌ |
| **8-format export** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Privacy biometric lock** | ✅ | ❌ | ❌ | ❌ | ✅ | ❌ |

### 1.2 Competitive Gaps FieldMind Must Fill

| Gap | Competitor | Impact | Implementation |
|-----|-----------|--------|-----------------|
| **Computer vision ID** | iNaturalist, Seek | 🔴 Critical — major hook | ML Kit + TFLite (Phase 1) |
| **Audio transcription** | KoboToolbox (80 langs) | 🔴 Critical — accessibility | Whisper distilled model (Phase 1) |
| **XLSForm import** | ODK, Kobo | 🟡 Medium — institutional users | XLSForm parser (Phase 3) |
| **Peer community** | iNaturalist, eBird | 🟡 Medium — validation loop | File-sharing validation v1 (Phase 2) |
| **Map tile caching** | QField, Survey123 | 🟡 Medium — GIS pro users | Offline tiles + drawing (Phase 2) |
| **Ethogram (event logging)** | None | 🔵 Future — wildlife research | Event logger (Phase 3) |

### 1.3 Market Positioning

**Current:** "Offline field notebook with AI assistant"  
**Target:** "The research operating system for field scientists"

**Unique value prop:** Only app that combines hypothesis-driven research workflow + community validation + spaced repetition learning + computer vision identification.

---

## 2. Groundbreaking Features (New)

### 2.1 🔬 Species Identification Engine (CRITICAL)

**Why:** iNaturalist dominates citizen science because ID is the primary UX. FieldMind can win in professional/institutional space.

**Implementation:**
- **On-device:** ML Kit object detection + bundled TensorFlow Lite model (500 species, expandable)
- **Fallback:** iNaturalist API integration (with user permission)
- **UX:** Post-capture → "Identify" button → top 5 matches with confidence → quick-add to observation

**Files to create:**
- `SpeciesClassifier.kt` — TFLite wrapper
- `SpeciesDatabase.kt` — bundled models + metadata
- `SpeciesIdentificationSheet.kt` — bottom sheet UI with carousel

**Timeline:** 4-6 weeks (model sourcing + integration)

---

### 2.2 🎙️ Voice-to-Observation AI Pipeline (CRITICAL)

**Why:** Speeds up capture 10x. Transcription is the gatekeeping feature in KoboToolbox.

**Implementation:**
- **On-device:** Distilled Whisper model (tiny.en, 75MB) via MediaPipe Audio
- **Cloud fallback:** OpenAI Whisper API
- **NLP:** Parse natural language → structured fields (subject, category, facts, tags)
- **UX:** "Voice observation" → speak 30 seconds → auto-parse → review & save

**Example:** "Spotted three crows feeding on the lawn near the oak tree at 2 PM, partly cloudy, about 15°C"
→ Auto-parsed: Subject="Crows", Count=3, Behavior="Feeding", Location="Lawn near oak", Category="Bird", Temp=15°C

**Files to create:**
- `AudioTranscriber.kt` — Whisper wrapper
- `VoiceObservationParser.kt` — NLP parsing
- `VoiceObservationSheet.kt` — Recording + playback UI

**Timeline:** 3-4 weeks (model selection + parsing logic)

---

### 2.3 🔗 Hypothesis-Driven Observation Graph (UNIQUE)

**Why:** No other app connects observations → questions → hypotheses → evidence → insights. This is the "research OS" differentiator.

**Current state:** Graph visualization exists but is static.

**New features:**
- **Live graph inference:** As you add observations, system suggests related hypotheses
- **Weak signal detection:** "These 3 observations might support Hypothesis #2"
- **Gap detection:** "You're testing H1 but missing observations for alternative H3"
- **Question generation:** "Based on your observations, you might ask..."
- **Citation chains:** Track evidence used to support each hypothesis

**Implementation:**
- New `GraphInferenceEngine.kt` — semantic matching of observations to hypotheses
- New `WeakSignalDetector.kt` — statistical correlation detection
- UI: Enhanced detail screen with "Related" section showing graph connections

**Timeline:** 6-8 weeks (NLP + graph algorithm work)

---

### 2.4 🌍 Offline Maps with Drawing Tools (PRO FEATURE)

**Why:** Separates from casual apps. Researchers need to draw survey areas, transects, site markers.

**Implementation:**
- **Tile caching:** User downloads OSM tiles for study area
- **Drawing tools:** Polygon (survey boundary), line (transect), point (site)
- **Track recording:** GPS path log during research session
- **Geo-fencing:** Reminder when arriving at marked site

**Files to create:**
- `OfflineTileManager.kt` — download + cache + prune
- `MapDrawingTools.kt` — polygon/line/point overlays
- `TrackRecorder.kt` — GPS logging with start/stop
- `GeoFenceReminder.kt` — WorkManager proximity alerts

**Timeline:** 5-7 weeks (tile caching + drawing canvas)

---

### 2.5 👥 Community Validation Network (v1: File Sharing)

**Why:** Builds data quality culture and community. iNaturalist's "Research Grade" is powerful.

**v1 Implementation (no backend needed):**
- Export observation as `.fieldmind` package (JSON + photos + metadata)
- Peer imports → adds ID/validation → updates validation status
- Track validators in new `ValidationEntity`
- Achievement: "Peer Reviewer" (10+ validations)

**v2 Implementation (future: cloud sync):**
- Firebase relay server for live sync
- Leaderboard + notifications
- Research Grade consensus (2/3 agreement)

**Files to create:**
- `ValidationEntity` — validator info + confidence
- `FieldMindPackageExporter.kt` — .fieldmind format
- `ValidationImporter.kt` — peer validation flow
- UI: "Share for validation" button on observation detail

**Timeline:** 2-3 weeks (v1), 4-5 weeks (v2 with backend)

---

### 2.6 📋 XLSForm Import Engine

**Why:** Opens institutional market (NGOs, universities using ODK/Kobo).

**Implementation:**
- Parse .xlsx XLSForm structure → FieldMind survey format
- Support: skip logic, required fields, repeats, calculations
- New `SurveyEntity` + `SurveyRunnerScreen`
- Export survey responses as CSV/JSON

**Files to create:**
- `XLSFormParser.kt` — XLSX parsing
- `SurveyEntity` + `SurveyResponseEntity`
- `SurveyRunnerScreen.kt` — step-through UI
- `FormLogicEngine.kt` — skip logic evaluation

**Timeline:** 4-6 weeks (XLSX parsing + logic engine)

---

### 2.7 ⚡ Behavioral Event Logger (Ethogram)

**Why:** Unique niche (ethology, animal behavior research). No other field app has this.

**Implementation:**
- Define ethogram: list of behaviors (Feeding, Grooming, Moving, Resting, etc.)
- Session mode: tap button for each behavior → logs start/end times
- Output: CSV with timestamps, behavior sequence, duration, transition matrix
- Chart: stacked bar showing duration per behavior

**Files to create:**
- `EventLogEntity` — session, behavior, start/end times
- `EventLoggerScreen.kt` — tap interface + timer
- `EthogramChart.kt` — Canvas chart for behavior data

**Timeline:** 3-4 weeks (relatively straightforward)

---

## 3. Missing Opportunities (Analysis Gap)

### 3.1 Weather Integration — Currently Stubbed

**Status:** Settings exist (`autoWeatherEnabled`, `gpsMode`), no API integration

**Missing:**
- `WeatherApiService.kt` — Open-Meteo REST calls
- Weather fields on `ObservationEntity` (temperature, humidity, wind, pressure, UV)
- Weather widget on Home screen (current conditions, forecast)
- Weather trend chart (Insights screen)
- Weather correlation analysis ("Your observations peak between 6-8 AM on clear days")

**Implementation:** 2-3 weeks

---

### 3.2 PDF Reader — Currently WebView Fallback

**Status:** Uses Google Docs viewer or browser

**Missing:**
- Native PDF renderer (e.g., PDFium or PdfBox)
- Inline annotations (highlight, underline, notes)
- Table of contents navigation
- Full-text search within PDF
- Continuous scroll + page thumbnails

**Implementation:** 3-4 weeks (library integration + UI)

---

### 3.3 Project Attachments

**Status:** Projects don't support file attachments

**Missing:**
- Attach photos, PDFs, documents to projects
- Organize by folder/type
- Quick preview in project detail

**Implementation:** 1-2 weeks (schema + UI)

---

### 3.4 Quick Annotation on Capture

**Status:** CameraV2 post-capture sheet doesn't support drawing

**Missing:**
- Canvas drawing tools (circle, arrow, text label, measurement line)
- Crop/rotate image
- Add caption overlay
- All within same screen

**Implementation:** 2-3 weeks

---

### 3.5 Auto-Question Generation

**Status:** Questions are manual

**Missing:**
- Rule-based Q-gen after each observation
- ML-based question suggestions
- "Based on your observation of X, you might ask..."

**Implementation:** 2-3 weeks (template rules + NLP)

---

## 4. UI/UX Redesign Strategy

### 4.1 Design Philosophy: "Research OS"

**Current:** Toolkit (many features, scattered UX)  
**Target:** OS (unified workflow, connected entities, fast capture)

**Key principles:**
- **Speed first:** Capture in <10 seconds
- **Connected:** Every entity shows related items
- **Smart defaults:** Pre-fill from context
- **Progressive:** Basic view → advanced view on demand
- **Responsive:** Phone, tablet, landscape modes

### 4.2 Home Screen Redesign

**Current:** Grid of 6 tiles (simple, boring)

**Redesigned:**
```
┌─────────────────────────────────┐
│  [Hero Banner]                   │
│  ⏰ 45 min session running      │
│  🎯 3 of 5 observations today    │
│  [📷 Quick Capture] [🔬 Resume] │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│  🌤 Weather Card                │
│  22°C, Partly cloudy, 60% RH    │
│  Best observation time: 6-8 AM  │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│  📊 Today's Stats               │
│  12 obs | 3 notes | 1 question  │
│  🔥 15-day streak               │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│  Quick Actions (4 tiles):        │
│  [📷 Capture]  [📝 Note]        │
│  [❓ Question] [🔍 Search]      │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│  Recent Observations            │
│  🐦 3 crows near oak | 2 min ago│
│  🌿 Fern frond | 15 min ago    │
│  🌤 Clear, 22°C | 45 min ago   │
└─────────────────────────────────┘
```

**Key changes:**
- Hero banner with session status
- Weather widget (weather integration)
- Daily stats + streak counter
- 4 quick action tiles (not 6)
- Recent observations feed below

**Implementation:** 2-3 weeks

---

### 4.3 Capture Screen Overhaul

**Current:** Multi-step (Mode → Category → Form)

**Redesigned (Evidence-First):**
```
┌─────────────────────────────────┐
│  [🎥 Snap] [🖼 Gallery] [🎤 Voice]
│                                  │
│  ┌─ Evidence Preview ──────────┐ │
│  │ [Photo] Caption _________   │ │
│  │ [Photo] Caption _________   │ │
│  └──────────────────────────────┘ │
│                                  │
│  ┌─ Quick Details ──────────────┐ │
│  │ Subject: ________________    │ │
│  │ Categories: [+] [▼]         │ │
│  │ Tags: ________________      │ │
│  │ Facts: ________________     │ │
│  │                             │ │
│  │ [Save] [Advanced]          │ │
│  └──────────────────────────────┘ │
└─────────────────────────────────┘
```

**Key changes:**
- Camera/gallery/voice at top (evidence-first)
- Evidence preview shows attached photos
- Minimal required fields (Subject + Category)
- Advanced fields collapsible
- Voice input creates observation in 30 seconds

**Implementation:** 4-6 weeks (requires form redesign)

---

### 4.4 Workspace Redesign (Already Done)

3 tabs: Projects | Evidence | Analysis ✅ (Done in PR #47)

---

### 4.5 Library → Knowledge Hub Unification

**Current:** 5 siloed tabs (Sources/Notes/Reading/Flashcards/Learn)

**Redesigned:** Unified knowledge hub
```
┌─────────────────────────────────┐
│  [← FieldMind]  📚 Knowledge Hub
│                                  │
│  [Search] [Filter ▼]            │
│                                  │
│  ┌─ Your Learning Path ────────┐ │
│  │ 📖 12 sources to read       │ │
│  │ 🗂 45 notes from reading    │ │
│  │ 📝 8 flashcards due today   │ │
│  │ 🎓 2 concepts to master     │ │
│  └──────────────────────────────┘ │
│                                  │
│  ┌─ Recent Sources ────────────┐ │
│  │ [PDF] "Species ID Guide"    │ │
│  │   2 highlights, 3 notes     │ │
│  │ [Link] Journal article      │ │
│  │   Saved 2 days ago          │ │
│  └──────────────────────────────┘ │
│                                  │
│  ┌─ Active Decks ──────────────┐ │
│  │ "Mammal ID" — 85% mastery  │ │
│  │ "Plant Morphology" — 60%   │ │
│  └──────────────────────────────┘ │
└─────────────────────────────────┘
```

**Key changes:**
- Single unified hub (no 5 tabs)
- "Your Learning Path" shows progress
- PDF reader integrated (native)
- Reading notes linked to sources
- Flashcard decks with progress

**Implementation:** 5-6 weeks

---

### 4.6 Insights → Research Dashboard

**Current:** Basic charts + achievements

**Redesigned:** "What am I discovering?"
```
┌─────────────────────────────────────┐
│  🔬 Research Dashboard              │
├─────────────────────────────────────┤
│                                      │
│  📊 Key Discoveries                 │
│  ├─ 24 observations (3 categories)  │
│  ├─ 2 questions → hypotheses        │
│  ├─ 1 weak signal: H1 supported     │
│  ├─ Pattern: Morning peaks          │
│  └─ Weather: Clear days optimal     │
│                                      │
│  🎯 Research Health: 85/100         │
│  ├─ Evidence density: 95%           │
│  ├─ Question coverage: 75%          │
│  ├─ Hypothesis support: 100%        │
│  └─ Data quality: 90%               │
│                                      │
│  🧠 AI Insights                     │
│  "Your bird observations cluster    │
│  around 6-8 AM on clear days with   │
│  18-22°C temps. Consider comparing  │
│  morning vs afternoon activity."    │
│                                      │
│  📈 Activity Heatmap (GitHub-style) │
│  [Show last 52 weeks]               │
│                                      │
│  🎖 Achievements                    │
│  [Show recent + progress]           │
│                                      │
│  📤 Generate Report                 │
│  [Create Weekly Summary PDF]        │
└─────────────────────────────────────┘
```

**Key changes:**
- Focus on "what am I discovering" not just stats
- Research health score (0-100)
- AI insights about patterns
- GitHub-style heatmap (52-week view)
- One-tap report generation

**Implementation:** 4-5 weeks

---

## 5. Architecture Restructuring

### 5.1 Phase 1: Dependency Injection (Critical)

**Current:** Single `FieldMindViewModel` instantiates dependencies directly. No unit testing.

**Target:** Hilt DI with module-based architecture.

**Steps:**
1. Add Hilt dependency
2. Create `RepositoryModule`, `ManagerModule`, `DatabaseModule`
3. Split `FieldMindViewModel` into feature-specific VMs
4. Add unit tests for critical flows

**Timeline:** 3-4 weeks
**Benefit:** Testability, modularity, ability to swap implementations

---

### 5.2 Phase 2: Data Layer Refactoring

**Current:** Single `FieldMindDao.kt` (60+ methods), single `FieldMindRepository.kt` (70+ methods)

**Target:** Feature-based DAOs + layered repositories

**Structure:**
```
data/
├── observation/
│   ├── ObservationDao.kt
│   ├── ObservationRepository.kt
│   └── ObservationModel.kt
├── question/
│   ├── QuestionDao.kt
│   ├── QuestionRepository.kt
│   └── QuestionModel.kt
├── hypothesis/
├── source/
├── project/
└── shared/
    └── BaseRepository.kt
```

**Timeline:** 4-5 weeks
**Benefit:** Single responsibility, easier testing, clearer data flow

---

### 5.3 Phase 3: Entity Organization

**Current:** All 21 entities in single `FieldEntities.kt`

**Target:** Feature-based entity files

```
entity/
├── ObservationEntity.kt
├── QuestionEntity.kt
├── HypothesisEntity.kt
├── SourceEntity.kt
├── ProjectEntity.kt
├── DataRecordEntity.kt
├── ReportEntity.kt
├── FlashcardEntity.kt
├── AchievementEntity.kt
├── ResearchSessionEntity.kt
├── ValidationEntity.kt
├── xref/
│   ├── ProjectObservationCrossRef.kt
│   ├── QuestionHypothesisCrossRef.kt
│   └── ... (other cross-refs)
└── SharedEntities.kt (base classes, enums)
```

**Timeline:** 2-3 weeks
**Benefit:** Navigation, maintainability

---

### 5.4 Phase 4: Screen Module Decomposition

**Current:** `FieldMindObserveScreen.kt` is 690+ lines

**Target:** Feature modules

```
presentation/
├── home/
│   ├── HomeScreen.kt
│   ├── HomeWeatherCard.kt
│   ├── HomeSessionCard.kt
│   └── HomeViewModel.kt
├── capture/
│   ├── CaptureScreen.kt
│   ├── CaptureForm.kt
│   ├── QuickSnapCard.kt
│   ├── AnnotationCanvas.kt
│   └── CaptureViewModel.kt
├── library/
├── workspace/
└── insights/
```

**Timeline:** 6-8 weeks (large refactor)
**Benefit:** Parallel development, clearer responsibility

---

## 6. Phased Implementation Roadmap

### Phase A: Weeks 1-4 (Immediate — Critical Path)

**Focus:** Groundbreaking features that differentiate

- [ ] Species Identification (ML Kit + TFLite model)
- [ ] Voice-to-Observation (Whisper + NLP parsing)
- [ ] Home screen redesign (weather widget, session CTA)
- [ ] Capture form overhaul (evidence-first layout)

**Commits:** ~15-20 PRs
**User Impact:** 🔴 HIGH — competitive features + UX improvement

### Phase B: Weeks 5-8 (Foundation — Architecture)

**Focus:** Stability + scalability

- [ ] Hilt dependency injection
- [ ] Data layer refactoring (split DAOs/Repos)
- [ ] Entity organization
- [ ] Unit test infrastructure

**Commits:** ~10-15 PRs
**User Impact:** 🟡 MEDIUM — internal only, enables future work

### Phase C: Weeks 9-12 (Differentiation — Community + Maps)

**Focus:** Pro features

- [ ] Hypothesis graph inference engine
- [ ] Offline maps + tile caching + drawing tools
- [ ] Community validation network (v1: file sharing)
- [ ] PDF reader integration

**Commits:** ~12-18 PRs
**User Impact:** 🟢 HIGH — new capabilities + knowledge hub

### Phase D: Weeks 13-16 (Completeness — Expansion)

**Focus:** Institutional adoption + niche markets

- [ ] XLSForm import engine
- [ ] Behavioral event logger (ethogram)
- [ ] Weather integration (Open-Meteo API)
- [ ] Research dashboard redesign

**Commits:** ~12-16 PRs
**User Impact:** 🟡 MEDIUM — power users + domain specialists

### Phase E: Weeks 17+ (Polish — Optimization)

**Focus:** Performance + refinement

- [ ] Auto-question generation
- [ ] Project attachments
- [ ] Quick annotation on capture
- [ ] Performance optimization
- [ ] Accessibility audit

**Commits:** ~8-12 PRs
**User Impact:** 🟢 LOW-MEDIUM — quality of life improvements

---

## 7. Custom Research Recommendations

### 7.1 Mobile-First Design Principles (v0 Analysis)

FieldMind is a mobile-first research app. Key design rules:

**1. Touch targets ≥ 48dp**
- Current icons (32dp) are too small for field work with gloves
- Increase to 64×64dp minimum
- Add haptic feedback on tap

**2. Capture < 10 seconds**
- Remove required fields that delay capture
- Pre-fill from context (last category used, GPS, time)
- One-tap camera launch

**3. Progressive disclosure**
- Show 3-4 key fields initially
- "Advanced" button reveals 10+ optional fields
- Save works with minimal input

**4. Minimal text**
- Use icons + color instead of labels
- Dark mode by default (saves battery in field)
- Reduce cognitive load

---

### 7.2 Competitive Feature Ranking (My Analysis)

**To beat iNaturalist:**
1. Species ID + confidence scoring (essential)
2. Hypothesis testing (iN doesn't have this)
3. Spaced repetition learning (iN doesn't have this)

**To beat ODK/Kobo:**
1. Nicer UX (taps are larger, capture faster)
2. AI transcription (both support, but KoboToolbox is 80 languages)
3. Knowledge graph (unique to FieldMind)

**To beat QField/Survey123:**
1. Offline ID + transcription
2. Research workflow (hypothesis-driven)
3. Spaced repetition + flashcards

**Recommendation:** Prioritize Species ID + Voice-to-Observation + Home redesign in Phase A. This creates immediate differentiation.

---

### 7.3 Monetization Opportunities (Bonus)

While FieldMind is free, consider these for sustainability:

**Free tier (current):**
- All core features (capture, analysis, export)
- Up to 500 observations
- Single local project

**Pro tier ($4.99/mo or $40/yr):**
- Unlimited observations
- Cloud backup + sync
- Community validation
- AI insights (enhanced)
- XLSForm import

**Institutional tier ($50/mo):**
- Team collaboration
- Advanced reports
- Custom training
- Priority support

---

### 7.4 Timeline Realism Check

**Phase A (4 weeks):** Species ID (4w) + Voice (3w) + UI (2w) = 9w (overload)
→ **Recommendation:** Delay Voice to week 3, do Species ID + Home redesign weeks 1-4

**Phase B (4 weeks):** Hilt DI + refactoring = realistic ✅

**Phase C (4 weeks):** Graph inference (6w) + Maps (6w) + PDF (3w) = 15w (overload)
→ **Recommendation:** Do Maps + PDF weeks 9-12, Graph inference weeks 13-16

**Total realistic timeline:** 24 weeks (6 months) for Phases A-D

---

## 8. Success Metrics

### 8.1 Adoption Metrics

- **Downloads:** 10K → 50K (Phase A impact)
- **Active users:** 2K → 10K
- **Weekly users:** 800 → 4K
- **Retention (Day 30):** 25% → 45%

### 8.2 Feature Adoption

- **Species ID usage:** >50% of photos
- **Voice capture:** >20% of observations
- **Hypothesis testing:** >30% of research sessions
- **Flashcard review:** >40% of learning users

### 8.3 Competitive Position

- **iNaturalist:** More pro/scientific, less community
- **ODK Collect:** Better UX, more flexible forms
- **Fulcrum:** Better price, worse AI
- **QField:** Better maps, worse knowledge graph

---

## 9. Implementation Priority Matrix

| Feature | Effort | Impact | Priority | Timeline |
|---------|--------|--------|----------|----------|
| Species ID | 5 weeks | 🔴 Critical | 1 | Week 1 |
| Voice-to-Obs | 4 weeks | 🔴 Critical | 2 | Week 4 |
| Home redesign | 3 weeks | 🟡 High | 3 | Week 1 |
| Hypothesis graph | 8 weeks | 🟡 High | 4 | Week 9 |
| Maps + drawing | 6 weeks | 🟡 High | 5 | Week 9 |
| PDF reader | 4 weeks | 🟡 Medium | 6 | Week 9 |
| XLSForm import | 5 weeks | 🟡 Medium | 7 | Week 13 |
| Ethogram logger | 4 weeks | 🟡 Medium | 8 | Week 13 |
| DI refactor | 4 weeks | 🟢 Internal | 9 | Week 5 |

---

## 10. Recommended Quick Wins (Immediate)

**Do NOW (1-2 weeks each):**

1. **Home screen hero banner** — Show active session + quick capture
2. **Toast → Snackbar** — Already done ✅
3. **Capture icon sizing** — 48dp → 64dp minimum
4. **Voice observation FAB** — Speech-to-text simple MVP
5. **Weather widget stub** — Show placeholder, integrate API later

**These provide immediate UX improvement without major refactoring.**

---

## 11. Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| ML Kit model too large | Use distilled models, support downloads |
| TFLite inference slow | Cache predictions, show loading state |
| Whisper too CPU-hungry | Fall back to cloud API, show disclaimer |
| Refactoring introduces bugs | Comprehensive unit tests, staged rollout |
| Performance regressions | Profiling before/after each phase |
| User confusion with new UI | Phased rollout with toggle, in-app guide |

---

---

## Summary

FieldMind has **strong fundamentals** (offline-first, knowledge graph, AI assistant). To dominate the market:

1. **Weeks 1-4:** Add Species ID + Voice capture (groundbreaking features)
2. **Weeks 5-8:** Refactor to DI + modular architecture (foundation)
3. **Weeks 9-12:** Add hypothesis inference + offline maps + community (differentiation)
4. **Weeks 13-16:** XLSForm + ethogram + weather (institutional + niche)
5. **Weeks 17+:** Polish + optimization

**Investment:** ~24 weeks, 80-100 PRs, 2-3 developers
**Payoff:** Market leadership in scientific field research tools

**Key differentiators:**
- 🎯 Species ID (iN strength, make ours better UX)
- 🎙️ Voice capture (Kobo strength, make ours offline)
- 🧠 Hypothesis graph (unique)
- 📚 Spaced repetition (unique)
- 🗺️ Offline maps (QField strength, make ours accessible)
