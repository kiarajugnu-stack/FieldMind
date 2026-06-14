# FieldMind Implementation Roadmap — Phase-by-Phase Breakdown

> **Date:** June 14, 2026  
> **Based on:** STRATEGIC_FIELDMIND_PLAN_V2.md  
> **Status:** Ready for execution

---

## Phase A: Weeks 1-4 (Groundbreaking Features)

### Week 1-2: Species Identification Engine + Home Screen

#### Sprint A.1 — Species Classifier (Week 1)

**Goal:** Basic on-device species identification with TFLite

**Tasks:**
- [ ] Source pre-trained ML Kit model or TensorFlow Lite model (~500 species, <150MB)
- [ ] Create `SpeciesClassifier.kt` wrapper around TFLite inference
- [ ] Create `SpeciesDatabase.kt` — bundled model metadata (species name, common name, confidence threshold)
- [ ] Add `SpeciesIdentificationSheet.kt` composable (carousel, top 5 matches, confidence bars)
- [ ] Hook post-capture CameraV2 flow: "Identify" button → launches classifier → shows results

**Files:**
```
features/field/data/vision/
├── SpeciesClassifier.kt
├── SpeciesDatabase.kt
└── SpeciesModel.kt

features/field/presentation/components/
└── SpeciesIdentificationSheet.kt
```

**Testing:**
- Unit test: classifier inference with mock model
- UI test: bottom sheet interactions
- Integration: capture → classify → add to observation

**PR:** #49 "Add on-device species identification with TFLite"

---

#### Sprint A.2 — Home Screen Hero Banner (Week 1)

**Goal:** New home screen layout with active session + weather stub

**Tasks:**
- [ ] Replace 6-tile grid with new layout
- [ ] Add hero banner: "⏰ 45 min session running | 🎯 3 of 5 observations today"
- [ ] Add weather card (placeholder for now, integrate API later)
- [ ] Add daily stats card (observations, notes, questions)
- [ ] Update quick actions to 4 tiles (Capture, Note, Question, Search)
- [ ] Add recent observations feed

**Files:**
```
features/field/presentation/screens/
├── FieldMindHomeScreen.kt (rewrite)
├── HomeSessionCard.kt (new)
├── HomeWeatherCard.kt (new)
├── HomeDailyStatsCard.kt (new)
└── HomeRecentFeed.kt (new)
```

**PR:** #50 "Redesign home screen with hero banner + session CTA"

---

### Week 2-3: Voice-to-Observation + Capture Form Overhaul

#### Sprint A.3 — Voice-to-Observation Pipeline (Week 2-3)

**Goal:** Speak observation → auto-parse into structured fields

**Tasks:**
- [ ] Source distilled Whisper model (tiny.en, ~75MB) or use MediaPipe Audio Classifier
- [ ] Create `AudioTranscriber.kt` — record audio, transcribe to text
- [ ] Create `VoiceObservationParser.kt` — NLP parsing (subject, category, facts, tags, etc.)
- [ ] Create `VoiceObservationSheet.kt` — recording UI, playback, review parsed fields
- [ ] Add FAB on CaptureScreen for quick voice observation
- [ ] Wire into existing observation save flow

**Parsing examples:**
- "Three crows feeding on lawn" → Subject="Crows", Category="Bird", Count=3, Behavior="Feeding", Location="Lawn"
- "Clear sky 22 degrees" → Category="Weather", Facts="Clear", Temperature=22°C

**Files:**
```
features/field/data/transcription/
├── AudioTranscriber.kt
├── VoiceObservationParser.kt
└── TranscriptionModel.kt

features/field/presentation/components/
└── VoiceObservationSheet.kt
```

**PR:** #51 "Add voice-to-observation with offline transcription"

---

#### Sprint A.4 — Capture Form Evidence-First Redesign (Week 3)

**Goal:** Capture in <10 seconds (evidence first, minimal required fields)

**Tasks:**
- [ ] Rewrite `FieldMindObserveScreen.kt` layout:
  - Top: Large [Snap], [Gallery], [Voice] buttons
  - Middle: Evidence preview (photo thumbnails + captions)
  - Bottom: Quick details (Subject, Categories, Tags, Facts)
  - "Advanced" button reveals optional fields
- [ ] Make category multi-select (not single)
- [ ] Auto-suggest category from last 5 observations
- [ ] Auto-fill time + date
- [ ] Auto-suggest tags from recent usage
- [ ] Save button available with just Subject + Evidence

**Files:**
```
features/field/presentation/screens/
├── FieldMindObserveScreen.kt (rewrite)
└── CaptureFormQuickCard.kt (new)
```

**PR:** #52 "Redesign capture form for evidence-first workflow"

---

### Week 4: Polish + Testing

#### Sprint A.5 — Integration + QA (Week 4)

**Tasks:**
- [ ] Test species classifier on real device
- [ ] Test voice transcription with various accents + background noise
- [ ] Test home screen navigation + session persistence
- [ ] Test capture form with all evidence types
- [ ] Fix visual alignment, spacing, colors
- [ ] Accessibility audit (text sizes, contrast, touch targets)

**PR:** #53 "Phase A polish and integration fixes"

---

## Phase B: Weeks 5-8 (Architecture Foundation)

### Sprint B.1 — Hilt Dependency Injection (Weeks 5-6)

**Goal:** Replace direct instantiation with Hilt

**Tasks:**
- [ ] Add Hilt dependency to build.gradle
- [ ] Create `AppModule.kt` — provide Repository, Settings, etc.
- [ ] Create `DatabaseModule.kt` — provide Room database
- [ ] Create `ManagerModule.kt` — provide managers (Privacy, Export, etc.)
- [ ] Add `@HiltViewModel` annotation to FieldMindViewModel
- [ ] Replace all `getInstance()` calls with `@Inject`
- [ ] Update FieldMindApplication with `@HiltAndroidApp`
- [ ] Add unit test skeleton for critical repositories

**Files:**
```
infrastructure/di/
├── AppModule.kt
├── DatabaseModule.kt
├── ManagerModule.kt
└── ViewModelModule.kt
```

**PR:** #54 "Add Hilt dependency injection"

---

### Sprint B.2 — Data Layer Refactoring (Weeks 6-7)

**Goal:** Split monolithic DAO + Repository into feature-based modules

**Tasks:**
- [ ] Create feature-specific DAOs:
  - `ObservationDao.kt`
  - `QuestionDao.kt`
  - `HypothesisDao.kt`
  - `SourceDao.kt`
  - `ProjectDao.kt`
  - `DataRecordDao.kt`
- [ ] Create feature-specific Repositories:
  - `ObservationRepository.kt`
  - `QuestionRepository.kt`
  - etc.
- [ ] Create `BaseRepository.kt` with common methods
- [ ] Update FieldMindViewModel to use feature repos instead of monolithic repository
- [ ] Add unit tests for critical data flows

**Files:**
```
features/field/data/repository/
├── observation/
│   ├── ObservationDao.kt
│   ├── ObservationRepository.kt
│   └── ObservationModel.kt
├── question/
├── hypothesis/
├── source/
├── project/
├── datarecord/
└── BaseRepository.kt
```

**PR:** #55 "Refactor data layer into feature-based DAOs + Repositories"

---

### Sprint B.3 — Entity Organization (Week 7)

**Goal:** Split 21 entities into feature-specific files

**Tasks:**
- [ ] Create entity directory structure (see above)
- [ ] Move entities from `FieldEntities.kt` into individual files
- [ ] Create `SharedEntities.kt` for base classes + enums
- [ ] Update database migrations if needed
- [ ] Update all imports in DAOs, Repos, ViewModel

**PR:** #56 "Reorganize entities into feature modules"

---

### Sprint B.4 — Testing Infrastructure (Week 8)

**Goal:** Set up unit + integration test suites

**Tasks:**
- [ ] Add TestRepository implementations
- [ ] Create fake database for tests (using Room's in-memory database)
- [ ] Write unit tests for:
  - ObservationRepository
  - QuestionRepository
  - FlashcardEngine
  - StreakCalculator
- [ ] Write UI tests for:
  - Home screen navigation
  - Capture screen save flow

**PR:** #57 "Add unit + integration tests"

---

## Phase C: Weeks 9-12 (Differentiation)

### Sprint C.1 — Hypothesis Graph Inference Engine (Weeks 9-10)

**Goal:** Suggest related hypotheses + weak signal detection

**Tasks:**
- [ ] Create `GraphInferenceEngine.kt` — semantic matching of observations to hypotheses
- [ ] Create `WeakSignalDetector.kt` — correlation detection between observations + hypotheses
- [ ] Create `QuestionGenerator.kt` — rule-based question generation from observations
- [ ] Update `DetailScreen` to show "Related Hypotheses" section
- [ ] Add inference badge on observations: "✓ Supports H1" / "✗ Contradicts H2"

**Algorithm sketch:**
```
For each observation:
  - Extract keywords (species, location, behavior)
  - Compare to each hypothesis keywords
  - If > 80% match → suggest link
  - If > 60% match → "weak signal"
  - Generate follow-up questions
```

**PR:** #58 "Add hypothesis graph inference engine"

---

### Sprint C.2 — Offline Maps + Tile Caching (Weeks 10-11)

**Goal:** Download OSM tiles, draw survey areas, record tracks

**Tasks:**
- [ ] Create `OfflineTileManager.kt` — download + cache + prune OSM tiles
- [ ] Create `MapDrawingTools.kt` — polygon/line/point overlay canvas
- [ ] Create `TrackRecorder.kt` — GPS path logging with start/stop
- [ ] Create `GeoFenceReminder.kt` — WorkManager proximity alerts
- [ ] Add "Download Region" dialog on MapScreen
- [ ] Add drawing mode toggle (Pan / Draw Polygon / Draw Line / Mark Point)
- [ ] Show active track during research session
- [ ] Save tracks with observations

**Files:**
```
features/field/data/map/
├── OfflineTileManager.kt
├── MapDrawingTools.kt
├── TrackRecorder.kt
└── GeoFenceManager.kt

features/field/presentation/screens/
└── FieldMindMapScreen.kt (update)
```

**PR:** #59 "Add offline maps with tile caching + drawing tools"

---

### Sprint C.3 — PDF Reader Integration (Week 11)

**Goal:** Native PDF rendering + annotations

**Tasks:**
- [ ] Add PDF rendering library (PDFium or PdfBox)
- [ ] Create `PdfViewerScreen.kt` with:
  - Full-page PDF display
  - Page thumbnails sidebar
  - Continuous scroll + page navigation
  - Full-text search
  - Highlight + annotation UI (highlight, underline, note)
  - Table of contents if available
- [ ] Update `SourceDetailScreen` to embed PDF viewer
- [ ] Link annotations to reading notes

**PR:** #60 "Add native PDF reader with annotations"

---

### Sprint C.4 — Community Validation Network v1 (Week 12)

**Goal:** File-based peer validation (no backend)

**Tasks:**
- [ ] Create `ValidationEntity` — validator info + confidence + notes
- [ ] Create `FieldMindPackageExporter.kt` — export observation as .fieldmind (JSON + photos)
- [ ] Create `ValidationImporter.kt` — import peer validations
- [ ] Add "Share for Validation" button on observation detail
- [ ] Add "Validate Observation" UI (choose species + confidence + notes)
- [ ] Track validators, update validation status
- [ ] Achievement: "Peer Reviewer" (10+ validations)

**PR:** #61 "Add community validation network (v1: file sharing)"

---

## Phase D: Weeks 13-16 (Institutional Adoption)

### Sprint D.1 — XLSForm Import Engine (Weeks 13-14)

**Goal:** Import .xlsx XLSForm → FieldMind survey

**Tasks:**
- [ ] Add XLSX parsing library
- [ ] Create `XLSFormParser.kt` — parse XLSForm structure
- [ ] Create `SurveyEntity` + `SurveyResponseEntity`
- [ ] Create `SurveyRunnerScreen.kt` — step-through survey UI
- [ ] Implement `FormLogicEngine.kt` — skip logic, calculations
- [ ] Export survey responses as CSV/JSON

**PR:** #62 "Add XLSForm import engine"

---

### Sprint D.2 — Behavioral Event Logger (Weeks 14-15)

**Goal:** Ethogram for animal behavior research

**Tasks:**
- [ ] Create `EventLogEntity` — session, behavior, start/end times
- [ ] Create `EventLoggerScreen.kt` — define ethogram, tap buttons for behaviors
- [ ] Create `EthogramChart.kt` — stacked bar, transition matrix, duration breakdown
- [ ] Export as CSV with timestamps for analysis

**PR:** #63 "Add behavioral event logger (ethogram)"

---

### Sprint D.3 — Weather Integration + Dashboard (Week 15-16)

**Goal:** Open-Meteo API + weather trends + research dashboard redesign

**Tasks:**
- [ ] Create `WeatherApiService.kt` — Open-Meteo REST calls
- [ ] Add weather fields to `ObservationEntity` (temperature, humidity, wind, pressure)
- [ ] Update HomeScreen weather widget to show real data
- [ ] Create `InsightsScreen.kt` redesign:
  - "What am I discovering?" focus
  - Research health score (0-100)
  - AI insights + pattern detection
  - GitHub-style heatmap (52-week view)
  - Weather correlation charts
  - Achievements showcase

**PR:** #64 "Integrate weather API + redesign research dashboard"

---

## Phase E: Weeks 17+ (Polish & Optimization)

### Sprint E.1 — Auto-Question Generation

**Goal:** Rule-based questions from observations

**Tasks:**
- [ ] Create `QuestionGenerator.kt` with templates
- [ ] Add "Generate Question" button on observation detail
- [ ] Generate follow-up questions after capture

**PR:** #65 "Add auto-question generation"

---

### Sprint E.2 — Project Attachments

**Goal:** Attach files to projects

**Tasks:**
- [ ] Add file attachment support to `ProjectEntity`
- [ ] Create attachment UI in project detail
- [ ] Support photos, PDFs, documents

**PR:** #66 "Add project attachments"

---

### Sprint E.3 — Quick Annotation on Capture

**Goal:** Draw on photos immediately after capture

**Tasks:**
- [ ] Extend post-capture sheet with drawing tools
- [ ] Canvas for circle, arrow, text label, crop, rotate
- [ ] Save annotated photo with observation

**PR:** #67 "Add quick photo annotation tools"

---

## Total Effort Summary

| Phase | Weeks | PRs | Estimated Dev Days | Focus |
|-------|-------|-----|-------------------|-------|
| A | 4 | 5 | 30 | Groundbreaking |
| B | 4 | 4 | 25 | Architecture |
| C | 4 | 4 | 30 | Differentiation |
| D | 4 | 3 | 20 | Institutional |
| E | 4+ | 3 | 15+ | Polish |
| **Total** | **24** | **19** | **120+** | **Comprehensive** |

---

## Success Metrics per Phase

### Phase A (Weeks 1-4)
- ✅ Species ID works on 80% of plant/animal photos
- ✅ Voice capture <15 seconds end-to-end
- ✅ Home screen loads in <500ms
- ✅ Capture form saves in 1 tap (evidence-first)
- ✅ Download count increases 2x

### Phase B (Weeks 5-8)
- ✅ Unit test coverage >70% on critical paths
- ✅ DI fully integrated, no more getInstance()
- ✅ ViewModel testable + splittable
- ✅ Build time unchanged or improved

### Phase C (Weeks 9-12)
- ✅ Hypothesis suggestions >80% accurate
- ✅ Map tile download >1GB with <100MB app size
- ✅ PDF viewer renders all 80 test PDFs
- ✅ Validation network used in 10+ observations

### Phase D (Weeks 13-16)
- ✅ XLSForm import supports 95% of common forms
- ✅ Event logger captures 1000+ behavior events
- ✅ Weather correlation detected (morning peak, clear days)
- ✅ Dashboard loads in <1 second

### Phase E (Weeks 17+)
- ✅ 0 crashes, <2% ANRs
- ✅ Battery usage <50mAh/hour
- ✅ Accessibility score >90

---

**Ready to start Phase A? Let's build the future of field research.** 🚀
