# FieldMind — Full UI/UX & Functionality Redesign Plan

**Status:** Proposal / plan only (no code changes)
**Author:** Devin (for @firefly-sylestia)
**Scope:** End-to-end redesign of the FieldMind Android app — information architecture, visual/interaction design system, screen-by-screen experience, and a restructuring of the underlying logic and functionality so the product becomes a genuinely useful research app for enthusiasts.

---

## 1. Why this redesign

### 1.1 What FieldMind is today
FieldMind is a phone-first, offline-first Android research notebook (Kotlin + Jetpack Compose + Material 3 + Room) built on top of the old **Rhythm** music-player shell. The intended flow is:

> Observe → Question → Research → Hypothesize → Collect Data → Analyze → Conclude → Archive

The data layer is genuinely strong: a full relational Room schema already exists for observations, evidence attachments, questions, hypotheses, projects, sources, data records, reports, flashcards, tags, plus many cross-reference tables (`field_question_observations`, `field_project_sources`, `field_hypothesis_evidence`, etc.) and relation POJOs (`ProjectWorkspace`, `QuestionWithEvidence`, `ReportWithSources`). The "Observe → … → Archive" backbone is modeled correctly in the database.

### 1.2 The problem (your words: "a page with buttons and nothing… not user friendly at all")
The data model is rich, but the **presentation layer does not do it justice**. Concrete findings from the current code:

- **The whole app is essentially `LazyColumn` + `Card` + `Button`.** Nearly every screen is a vertical list of `ResearchCard` / `StatPill` plus a "Create X" button that opens a dialog (`ProjectsScreen`, `QuestionPanel`, `HypothesisPanel`, `DataToolPanel`, `ReportPanel` in `FieldMindScreens.kt`). There is little visual hierarchy beyond "title + grey subtitle."
- **Icons are literal Unicode glyphs as text.** The bottom navigation uses characters like `⌂`, `+`, `◇`, `□`, `△`, `?`, `≈`, `⇩` rendered with `Text(...)` (`FieldMindNavigation.kt`). This is the single biggest reason it reads as "unfinished / placeholder."
- **15 destinations, only 5 surfaced.** `FieldMindScreen` declares 15 routes (Home, Observe, Projects, Library, Learn, FieldMode, Questions, Hypotheses, DataTools, Analysis, Reports, Search, BackupExport, Progress, Settings) but the bottom bar shows 5 and the rest are reached through nested `TabRow` / buttons — discoverability is poor.
- **Capture is one long form.** `ObservationCaptureCard` is a tall single form (subject, category, facts, confidence, location, tags, evidence, context, project, attachments, audio). For "fast capture in the field" this is high-friction.
- **The relational power is invisible.** The schema supports linking questions↔observations↔sources↔projects↔hypotheses↔reports, but the UI never visualizes those links (no backlinks, no graph, no "evidence behind this hypothesis" view).
- **No data visualization.** "Analysis" and "Data tools" exist as records and text, but there are no charts, counts-over-time, maps, or comparisons — the core payoff of a research app.
- **Legacy debt & naming confusion.** The package is still `chromahub.rhythm.app`, music-player components remain (`player`, `lyrics`, `streaming`, `bottomsheets`), and FieldMind is wedged into `features/field`. This makes the product feel like a reskin rather than a purpose-built tool.

### 1.3 What "good" looks like
A research app for enthusiasts should feel like a **field companion + a thinking tool**: ruthless capture speed outside, rich structure and connections inside, and visible payoff (patterns, charts, maps, links, progress). The redesign below targets exactly that.

---

## 2. Research synthesis (from the web)

Key patterns pulled from current field-research, citizen-science, note/knowledge, and Android design sources:

1. **One-tap, button-per-type capture for the field.** Esri QuickCapture's design study found that under sun, motion, and divided attention, field users need *huge tap targets (≥72px), one button per data type, instant multi-channel confirmation (toast + sound + haptic), and progressive disclosure* to cut cognitive load. → *FieldMind needs a dedicated "Field Mode" that is big buttons + instant save, not a long form.* (qhuidesign.com/quickcapture; dawndesign.io Trimble Unity Field — "It wasn't about feature parity — it was about removing friction in the field.")

2. **Camera-first, AI-assisted, offline identification.** The redesigned iNaturalist puts the **camera at the center**, offers in-camera/offline species suggestions, a "match screen" showing AI confidence, long-press for more capture modes (audio, multi-photo, bulk import), and an **Explore** map of nearby observations. → *FieldMind should make media + location + (optional) AI suggestion first-class in capture, and add a map/explore surface.* (inaturalist.org new app post; App Store listing)

3. **Bidirectional links & a knowledge graph turn notes into research.** Knovya/Glyph/Obsidian-style tools show that **typed backlinks, a backlinks panel in the editor, `[[wiki-link]]` autocomplete, automatic link maintenance on rename, and a graph view with color-coded edges** are what make a knowledge tool feel intelligent. → *FieldMind already has the cross-ref tables; the UI must expose backlinks and a graph.* (knovya.com/features/backlinks, /knowledge-graph; Glyph wikilinks)

4. **Digital field notebooks succeed when they connect capture → structure → reflection and support collaboration/peer learning.** The Frontiers digital-field-notebook study emphasized connecting "the field" to structured review and shared maps/entries. → *FieldMind should make the Observe→Analyze→Conclude loop visible and reflective, and leave room for export/share.* (frontiersin.org DFN study)

5. **Material 3 Expressive is the right visual target for Android in 2025.** M3 Expressive (already opt-in in this repo) updates theming, motion, typography, and components, brings back **shorter "flexible" navigation bars** with pill indicators, and pushes **adaptive navigation** (NavigationBar on phones, expandable NavigationRail on foldables/tablets). → *Adopt M3 Expressive components, real Material Symbols icons, expressive motion, and adaptive navigation.* (developer.android.com/develop/ui/compose/designsystems/material3; 9to5google M3 Expressive navigation)

---

## 3. Product vision, principles & audience

### 3.1 Vision
> **FieldMind is the notebook that thinks with you.** Capture an observation in two taps outside; come home to a workspace that links your evidence, questions, and hypotheses into real projects, shows you patterns on charts and maps, and helps you write up what you found — all offline, all yours.

### 3.2 Design principles (these drive every decision below)
1. **Capture in two taps, structure later.** Outdoor speed is non-negotiable; enrichment is progressive.
2. **Make the connections visible.** The relational model is the product — surface backlinks, evidence chains, and a graph.
3. **Show the payoff.** Every screen should reward the user with insight (charts, maps, streaks, "what changed").
4. **Calm, confident, content-first.** Real iconography, generous spacing, one clear primary action per screen, expressive but not noisy.
5. **Honest AI.** AI assists and suggests, never invents evidence; always shows confidence and is opt-in.
6. **Offline & owned.** Everything works without a network; export is first-class.
7. **Adaptive & accessible.** Phone-first, but scales to foldables/tablets; large tap targets, dynamic type, contrast, TalkBack.

### 3.3 Primary personas
- **The Naturalist Enthusiast** — logs birds/plants/insects on walks; wants fast capture + media + location + "what have I seen here before."
- **The Curious Investigator** — runs small structured projects (e.g., "does my balcony garden attract more pollinators after planting X?"); wants questions, hypotheses, data tools, and charts.
- **The Lifelong Learner / Student** — uses Learn modules, flashcards, paper-reading prompts, and report templates to build research skill.

### 3.4 Core jobs-to-be-done
- "When I see something interesting, **capture it before the moment passes.**"
- "When curiosity repeats, **turn it into a project** with a question and a method."
- "When I have data, **see the pattern** (chart/map/comparison)."
- "When I'm done, **write it up and keep it** (report + archive + export)."

---

## 4. Information architecture (restructured navigation)

### 4.1 Problem with today's IA
15 flat destinations, 5 in the bottom bar, the rest nested behind tabs/buttons. The mental model (the research lifecycle) is not reflected in navigation.

### 4.2 New top-level IA — 5 tabs mapped to the lifecycle, plus an omnipresent capture FAB

| Tab | Icon (Material Symbols) | Purpose | Absorbs today's |
|-----|------------------------|---------|-----------------|
| **Today** (Home) | `home` / `today` | Daily dashboard: goal, streak, current project, nearby, recent activity, suggested next action | Home, Progress |
| **Capture** | `add_a_photo` / `bolt` | Field Mode + observation journal; camera/audio/location first | Observe, FieldMode |
| **Projects** | `science` / `lab_profile` | Project workspaces that contain questions, hypotheses, data, sources, analysis, reports | Projects, Questions, Hypotheses, DataTools, Reports |
| **Library** | `menu_book` | Sources, paper-reading, flashcards, Learn modules | Library, Learn |
| **Insights** | `insights` / `bar_chart` | Charts, maps, tag patterns, the knowledge graph, archive search | Analysis, Search |

- **Global Capture FAB** (center-docked, expressive) on Today/Projects/Library/Insights → opens the two-tap capture sheet from anywhere. (iNaturalist-style "camera always reachable.")
- **Settings + Export** move to a profile/overflow entry in the top app bar (not a primary tab). (`BackupExport`, `Settings`.)
- **Adaptive:** `NavigationBar` (flexible/short, M3 Expressive) on phones; **expandable `NavigationRail`** on foldables/tablets/landscape. Drawer is deprecated in M3 Expressive — use the expanded rail for secondary destinations.

### 4.3 Navigation depth rules
- Tab → list/overview → detail (max 3 levels).
- Detail screens use a **collapsing large top app bar** with the entity title, type badge, and contextual actions; **backlinks/related entities are a section inside the detail**, not a separate screen.

---

## 5. Design system

The repo already has a Material 3 theme (`shared/.../theme/`) and opts into `ExperimentalMaterial3ExpressiveApi`. We build a dedicated **FieldMind design system** on top rather than reusing Rhythm's player styling.

### 5.1 Brand & color
- **Identity:** "Observe. Question. Discover." Calm, outdoorsy, focused. A field-notebook feel with a modern expressive twist.
- **Palette:** Define FieldMind seed colors (proposal: deep forest green primary, warm ochre/amber secondary for accents/streaks, slate neutrals). Generate full tonal palettes; keep **Material You dynamic color** as an opt-in.
- **Semantic color tokens for research concepts** (used consistently across cards, chips, graph edges, charts):
  - Confidence: `Sure` = green, `Guess` = amber, `Needs Verification` = red/orange.
  - Entity types: Observation, Question, Hypothesis, Project, Source, Data, Report each get a stable hue + icon (drives badges *and* graph edge colors — see §7.3).
- Full light/dark via tokens (already a stated principle); verify WCAG AA contrast.

### 5.2 Typography
- Adopt an expressive type scale (display/headline for screen titles and key numbers; clear body for notes). Consider a distinctive display face for headers to break the "generic Material" look while keeping body legible. Support **dynamic type / font scaling**.

### 5.3 Iconography (highest-impact quick win)
- **Replace every Unicode-glyph "icon" with real `Material Symbols` (Compose `Icons`/extended) vector icons** in navigation, badges, actions, and entity types. This alone removes most of the "placeholder" feel.
- Each entity type gets a canonical icon (e.g., Observation `visibility`, Question `help`, Hypothesis `lightbulb`, Project `science`, Source `menu_book`, Data `bar_chart`, Report `description`).

### 5.4 Core component library (redesigned + new)
Refactor the thin component set (`ResearchCard`, `StatPill`, `EntityTypeBadge`, `ChoiceChips`, `EmptyResearchState`, `LabeledTextField`, `FieldSectionTitle`, `ActionGrid`, `TimelineItem`) into a richer, consistent kit:

- **EntityCard** (replaces generic `ResearchCard`): leading type icon + colored accent, title, smart metadata row (date/place/confidence chips), optional media thumbnail, optional mini-stats (e.g., "3 linked observations"), overflow menu, swipe actions (archive/delete/link).
- **CaptureSheet / FieldButton**: large (≥72dp) one-tap capture buttons per observation type; instant save with toast + haptic + (optional) sound confirmation.
- **StatCard / MetricTile**: prominent numbers with trend (▲/▼ vs last week) — not flat pills.
- **Chart components**: line (counts over time), bar (by category/tag), simple comparison table viz — built on Compose Canvas or a lightweight charts lib.
- **MapCard / ObservationMap**: cluster pins for geotagged observations (offline-friendly tiles where possible).
- **LinkChip / BacklinksPanel**: shows related entities with type icon + color; tap to navigate; "link to…" action.
- **GraphView**: interactive node-edge canvas of the knowledge graph (color-coded typed edges).
- **TimelineItem / ActivityFeed**: richer recent-activity with icons and grouping by day.
- **EmptyState**: illustration + one clear primary CTA (not just two lines of text).
- **FilterBar / SearchField**: persistent, with chips for type/date/tag/confidence/project.
- **ConfidenceChip, TagChip, StatusChip**: standardized, color-coded.

### 5.5 Motion & feedback
- Adopt **M3 Expressive motion** (spring-based, container transforms between list and detail, shared-element for media). Keep the existing slide transitions but upgrade to expressive specs.
- **Multi-channel confirmation** on capture/critical actions: snackbar + haptic + optional sound (per the QuickCapture findings). Honor reduce-motion / accessibility settings.

### 5.6 Accessibility
Min 48dp touch targets (72dp in Field Mode), content descriptions on all icons, dynamic type, AA contrast, TalkBack labels for charts/graph (text summaries), high-contrast theme option.

---

## 6. Screen-by-screen redesign

### 6.1 Onboarding (refresh)
- Keep the 5-message lifecycle intro but make it visual (illustration per step, progress dots, an interactive "log your first observation" final step). Ask 1–2 personalization questions (interests → default categories; daily goal). Permissions explained in-context, requested at point-of-use.

### 6.2 Today (Home)
From a list of cards to a **purposeful dashboard**:
- **Hero:** today's goal + streak as a prominent ring/progress, with one suggested next action ("You usually log birds in the morning — capture one?").
- **Current project** card with real progress (questions answered / data collected / report status).
- **Metric tiles** (Observations, Open Questions, Sources, Data points) with **trends**.
- **Nearby** mini-map of recent geotagged observations (Explore entry).
- **Patterns teaser:** top tag, "subject you revisit most," 1 chart.
- **Activity feed** grouped by day with entity icons.

### 6.3 Capture
- **Field Mode** (default when entering from FAB outside): full-screen, big buttons per observation type, camera/audio/location one-tap, instant save, "undo" snackbar. Minimal typing.
- **Detailed capture** (progressive disclosure): start with subject + photo; expand to facts, confidence, evidence, location, tags, project link, context. Save partial drafts.
- **Capture = camera-first** (iNaturalist pattern): open to camera/preview; long-press FAB for audio / multi-photo / import.
- **Observation journal:** rich `EntityCard`s with thumbnails, filterable by type/date/place/confidence; map toggle.

### 6.4 Projects (the research workspace)
- **Project overview:** objective, status, method, and **linked counts** (questions, observations, sources, data, hypotheses, reports) as navigable sections — powered by the existing `ProjectWorkspace` relation.
- Inside a project, **Questions / Hypotheses / Data / Sources / Analysis / Report** are sub-sections of the *workspace* (not separate top-level tabs), each showing only items linked to this project.
- **Hypothesis detail** shows its **evidence chain** (supporting/weakening observations via `field_hypothesis_evidence`) with a confidence meter.
- **Data tools** become structured, typed mini-tools (Counter, Measurement, Checklist, Event/Weather/Site log, Species tracker, Comparison table) each with an appropriate input UI and an **instant chart** of what's been collected.

### 6.5 Library & Learn
- **Sources:** card grid with cover/type icon, reading status ring, and "active reading" prompts; `[[link]]`-style linking to questions/projects.
- **Paper-reading mode:** guided prompts (question, method, findings, limitations) saved as structured notes.
- **Flashcards:** spaced-repetition review session UI (not just a list).
- **Learn:** the Beginner/Intermediate/Advanced modules become a **skill path** with progress, tied to real actions in the app ("you've logged 10 observations → 'Observation' skill complete").

### 6.6 Insights (new payoff surface)
- **Charts:** observations over time, by category, by tag; question funnel (New→Researching→Tested→Answered); data-record trends.
- **Map / Explore:** clustered observation pins; filter by category/date.
- **Knowledge graph:** interactive nodes (entities) + color-coded typed edges (links). Tap node → detail; tap edge → relationship.
- **Archive search:** universal, fast, full-text across all entities + attachments, with filter chips (type, date, tag, confidence, project) and saved searches. (Today's `Search` + `AttachmentSearchResult` already support the backend.)

### 6.7 Settings, Export, Profile
- Profile/overflow entry: observation defaults, location/media toggles, daily goal, AI (Gemini) key + send-confirmation controls, theme (dynamic color, dark, contrast), and **Export** (Markdown / CSV / JSON / plain text) + data ownership/delete.

---

## 7. Functionality & logic restructuring

The DB is solid; most work is in the domain/presentation layers and a few schema additions.

### 7.1 Capture pipeline
- Introduce a **draft/observation state machine** (draft → saved → enriched → linked → archived) so Field Mode can save instantly and enrich later. (`status`/`archivedAt` fields already exist on `ObservationEntity`.)
- First-class **media + audio + location** capture services with permission-at-point-of-use and graceful fallback (text-only still works). Store via `EvidenceAttachmentEntity`.
- Optional **on-device/AI suggestion** step (category/subject suggestion) shown as a *preview with confidence*, never auto-applied.

### 7.2 Linking everywhere (surface the cross-refs)
- A reusable **"Link to…" action** on every entity that writes the appropriate cross-ref (`field_question_observations`, `field_project_sources`, `field_hypothesis_evidence`, etc.).
- A **BacklinksPanel** on every detail screen that reads relation POJOs (`QuestionWithEvidence`, `SourceWithLinks`, `ReportWithSources`, `ProjectWorkspace`) and lists incoming/outgoing links with type icons.
- Optional **`[[wiki-link]]` autocomplete** in note/facts fields that creates real links + maintains them (knowledge-app pattern).

### 7.3 Knowledge graph
- Build a graph model over existing entities + cross-ref tables; render with color-coded typed edges (entity-type colors from §5.1). Edge types: evidence-for, linked-source, in-project, answers-question, etc.

### 7.4 Analysis / data viz engine
- A small **stats/aggregation layer** (counts over time, by category/tag, status funnels, comparison tables) feeding the chart components. All computed locally from Room.

### 7.5 Reports
- **Template-driven report builder** (Summary / Field Report / Literature Review / Project Draft / Findings / Final) that pre-fills from linked observations, data, and sources, with section prompts. Export to Markdown/PDF.

### 7.6 AI assistant (Gemini) — honest & optional
- Centralized assistant for: observation review, question-quality feedback, hypothesis phrasing, summarization, flashcard generation, report drafting. Always: opt-in, key-gated, **shows exactly what will be sent**, labels output as suggestion, never fabricates evidence. (Already the stated philosophy in onboarding.)

### 7.7 Search
- Full-text index across entities + attachment captions; filter chips; recent/saved searches; jump-to-detail.

### 7.8 Technical / architectural cleanup (enables the above without churn)
- **Rename/relocate the FieldMind feature out of the `chromahub.rhythm` package** into a clear `app`/`fieldmind` namespace; quarantine or remove the music shell (`player`, `lyrics`, `streaming`, `bottomsheets`) behind the existing asset-policy (don't delete assets without approval).
- **Split `FieldMindScreens.kt` (one 438-line file of dense one-liners) into per-feature screen files + a real component module.** Improves readability, testability, and parallel work.
- Confirm **MVVM + unidirectional state** (UI state holders, immutable state, events) and a clean domain layer between Room and Compose.
- Add **Room migrations** for any new fields (graph metadata, draft state, AI metadata) — never destructive.
- Keep **offline-first**; AI/network are strictly additive.

---

## 8. Phased roadmap

A pragmatic sequence — each phase ships something visibly better.

### Phase 0 — Foundations & quick wins (highest impact / lowest risk)
- Replace all Unicode-glyph icons with Material Symbols across nav, badges, actions.
- Stand up the FieldMind design tokens (color, type, spacing) + dynamic color.
- Restructure navigation to the 5 lifecycle tabs + capture FAB (adaptive bar/rail).
- Split `FieldMindScreens.kt` into modules; introduce the redesigned `EntityCard`, `MetricTile`, `EmptyState`.
*Outcome: the app immediately stops looking like "a page with buttons."*

### Phase 1 — Capture that delights
- Field Mode (big-button two-tap capture) + camera-first detailed capture + drafts.
- Media/audio/location services with point-of-use permissions and multi-channel confirmation.
- Redesigned observation journal with thumbnails, filters, and map toggle.

### Phase 2 — Make connections visible
- "Link to…" everywhere + BacklinksPanel on all detail screens.
- Project workspace redesign (sections from `ProjectWorkspace`); hypothesis evidence chains.
- Optional `[[wiki-link]]` autocomplete.

### Phase 3 — Show the payoff (Insights)
- Charts engine + Insights tab; observation map/Explore.
- Knowledge graph view.
- Universal archive search with filter chips.

### Phase 4 — Deepen the research tools
- Structured data tools with instant charts; template-driven report builder + export (MD/CSV/JSON/PDF).
- Learn skill-path tied to real actions; flashcard spaced-repetition sessions.

### Phase 5 — Honest AI & polish
- Centralized Gemini assistant (opt-in, transparent) across review/summarize/draft.
- Expressive motion pass, accessibility audit (TalkBack, contrast, dynamic type), foldable/tablet rail polish.

### Phase 6 (optional, future) — Collaboration & sharing
- Shared maps/projects and peer learning (per the digital-field-notebook research), if/when a backend is desired. Stays optional to preserve offline-first/ownership.

---

## 9. Success metrics
- **Capture friction:** taps & seconds to save a field observation (target: ≤2 taps in Field Mode).
- **Engagement:** observations/active day, streak retention, % observations later enriched/linked.
- **Research depth:** % observations linked to a question/project; projects reaching "report" stage.
- **Payoff usage:** Insights/graph/map opens; searches per session.
- **Quality:** crash-free sessions, cold-start time, accessibility audit pass.

## 10. Risks & open questions
- **Scope vs. effort:** this is a large redesign; Phase 0–1 deliver most of the perceived improvement — recommend shipping incrementally.
- **Maps offline:** fully offline map tiles add complexity; may start with online tiles + cached extents.
- **Charts/graph library:** build on Compose Canvas (no deps, full control) vs. a library (faster, heavier) — decision needed.
- **De-Rhythm-ing:** renaming the package and removing the music shell is valuable but touches many files; sequence carefully behind the asset policy.
- **AI cost/privacy:** keep strictly opt-in and transparent; default fully offline.

### Questions for you
1. **Priority order** — agree with Phase 0–1 first (icons + nav + capture), or do you want a specific area first (e.g., Insights/graph)?
2. **Brand direction** — happy with the forest-green/ochre "field notebook" palette, or do you have brand colors/identity in mind?
3. **De-Rhythm-ing** — OK to rename the package away from `chromahub.rhythm` and quarantine the music shell (keeping assets per policy)?
4. **Charts & maps** — preference for dependency-free Compose Canvas vs. pulling in a charts/maps library?
5. **AI** — keep Gemini optional/opt-in as designed, or expand its role?

---

*This document is a plan only — no application code has been changed. On your go-ahead I can turn any phase into concrete implementation PRs.*
