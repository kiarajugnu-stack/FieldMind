package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.BackButton
import fieldmind.research.app.features.field.presentation.components.StandardScreenHeader
import fieldmind.research.app.features.field.presentation.components.InfoChip
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon

internal data class FieldMindChangelogEntry(
    val version: String,
    val date: String,
    val title: String,
    val importance: String,
    val tags: List<String>,
    val sections: List<Pair<String, List<String>>>
)

private val fieldMindChangelog = listOf(
    FieldMindChangelogEntry(
        version = "1.5.3-infinite-canvas-removed",
        date = "2026-06-24",
        title = "Removed Infinite Canvas — Simplified to Page-Only Mode",
        importance = "Patch",
        tags = listOf("Canvas", "Simplification", "Cleanup"),
        sections = listOf(
            "🗑️ Infinite Canvas Removed" to listOf(
                "Removed InfiniteCanvas, CanvasBackground (dot grid), and CanvasMinimap composables — the app now uses PageCanvas exclusively.",
                "Removed CanvasMode enum and canvasMode toggle from CanvasState — the canvas is always in page (document) mode.",
                "Removed showGrid/toggleGrid from CanvasState — the dot-grid background was only used by the infinite canvas.",
                "Simplified CanvasTopBar overflow menu: removed canvas mode toggle and grid toggle; Figure Gallery and Drawing tools remain.",
                "Page indicator in the top bar now always visible since the canvas is always in page mode."
            ),
            "🧼 Cleanup" to listOf(
                "Deleted ~1,500 lines of code across InfiniteCanvas.kt, CanvasBackground.kt, and CanvasMinimap.kt.",
                "Removed ~250 lines of mode-switching logic from CanvasScreen and CanvasState.",
                "All canvas blocks, drawing, zoom, and block operations preserved under PageCanvas."
            )
        )
    ),
    FieldMindChangelogEntry(
        version = "1.5.2-predictive-back-canvas",
        date = "2026-06-23",
        title = "Predictive Back Peek Animation & Canvas Improvements",
        importance = "Patch",
        tags = listOf("Navigation", "Canvas", "Animation", "Fixes"),
        sections = listOf(
            "🔙 Predictive Back Peek Animation" to listOf(
                "Previous screen preview with parallax, label, and back-arrow icon slides in from the left during the back gesture — shows exactly where you're navigating to before you commit.",
                "Fixed blank/black screen appearing when the back gesture commits — offset is now reset to 0 before navigation triggers, preventing the graphicsLayer translation from shifting the exit transition off-screen.",
                "Swipe-back spring stiffness increased from 300 to 800 for a fast, fluid feel — no more sluggish settings back gesture.",
                "TabSwipeHost refactored from laggy animateFloatAsState to Animatable with snapTo for instant per-frame finger tracking during tab swipes.",
                "Edge tabs (Home, Library) use SwipeBackHost instead of TabSwipeHost — no more swiping into a non-existent screen from the first/last tab.",
                "PreviousScreenInfo wired to all 40+ SwipeBackHost calls — every screen with swipe-back shows the correct previous destination badge."
            ),
            "🖱️ Canvas Drag & Resize Stability" to listOf(
                "Fixed drag snap when re-dragging a block before Room finishes writing the previous position — onDragStart now captures from liveBlockPosition (if active) instead of the stale entity position.",
                "Fixed resize handle jumping to the old size on next gesture — resize now bases on displayWidth (which reads the live size) instead of block.width (entity, could be stale).",
                "Fixed visual snap at drag/resize end — setLiveBlockPosition/setLiveBlockSize now keeps the override active until Room emits the updated entity, with LaunchedEffect cleanup that removes the override when entity values match.",
                "Same drag/resize snap fixes applied to both InfiniteCanvas and PageCanvas modes."
            ),
            "📄 Text & Image Block Simplification" to listOf(
                "TextBlock redesigned: removed FormattingToolbar, CommandMenu, and LinkInsertDialog (~250 lines). Clean outline border when selected, no filled background, auto-expand height, placeholder text.",
                "ImageBlock simplified: removed inline caption editing and caption display bar. Tap image for full-screen viewer, tap empty for picker. Minimal, clean design."
            ),
            "🔍 Page Zoom & Per-Page Drawing" to listOf(
                "PageCanvas no longer forces zoomTo(1f) — pages now scale with the ZoomSlider (0.1x–5x) like InfiniteCanvas.",
                "Block positions and sizes scale by zoom; drag and resize deltas divided by zoom for correct document-space movement.",
                "ZoomSlider always visible in page mode; zoom controls in the top bar work in both modes.",
                "Drawing overlay moved from behind the scrollable pages to INSIDE each page Surface — strokes render on the page, not the gray background.",
                "Per-page drawing uses correct coordinate transforms (page-local ↔ document with zoom scaling) and filters saved drawings by Y range.",
                "All drawing tools supported per-page: pen, highlighter, shapes, eraser."
            ),
            "🔄 Viewport Culling Performance" to listOf(
                "InfiniteCanvas viewport culling padding increased from 100px to 300px — 3× wider recycling buffer keeps blocks composed during rapid scrolling, reducing composition/disposal churn.",
                "Block visibility computation moved from inline SubcomposeLayout filter to derivedStateOf outside the measurement pass, tracked reactively via zoom/pan/liveBlockPositions changes.",
                "Viewport size tracked via onSizeChanged for accurate culling; remember keys only on blocks list (not camera values that change every frame) to avoid recreating derivedStateOf.",
                "Manual BlockToolbar offset positioning in PageCanvas replaced with direct BlockToolbar using canvasToScreen() for correct zoom-aware placement."
            )
        )
    ),
    FieldMindChangelogEntry(
        version = "1.5.1-weather-crash-ui-polish",
        date = "2026-06-16",
        title = "Weather Animation Polish, Crash Fixes & Back Navigation",
        importance = "Patch",
        tags = listOf("Weather", "Fixes", "UI", "Navigation"),
        sections = listOf(
            "🌙 Weather Animation Polish" to listOf(
                "Shooting star now fades in gradually during the first 25% of its flight instead of appearing suddenly, then fades out smoothly for a natural appear-then-disappear arc.",
                "Moon repositioned to top-right corner with reduced size (0.07 normal / 0.09 compact) and tighter glow rings (2x/3x radius) so the glow doesn't wash over the whole scene.",
                "Sun repositioned to top-right corner with reduced size, shorter rays (1.3x–1.5x radius), slower rotation (20s), and slower glow pulse (5s) for a calmer day scene.",
                "Rain streaks reduced by ~40% (heavy 80→50, normal 50→30) with per-drop random phase delays so drops fall continuously as scattered random drops instead of synchronized sheets.",
                "Rain fall speed reduced (base animation slowed from 400ms→700ms heavy, 700ms→1200ms normal) for a more gentle, natural look.",
                "All rain types (drizzle, rain, showers, heavy rain) benefit from the continuous random-drop behavior."
            ),
            "🐛 Infinite-Height Scroll Crash Fix" to listOf(
                "Fixed crash when tapping 'Add Species' or 'Add Task' in Project Detail — the root cause was 'verticalScroll' being placed before 'heightIn(max=...)' in the modifier chain, causing infinite height constraints inside LazyColumn items.",
                "Reordered modifiers to Modifier.heightIn(max = 420.dp).verticalScroll(...) in SpeciesRegistryBuilder and ProjectTasksBuilder so height constraints clamp before the scroll check.",
                "Replaced nested LazyRow (which conflicted with parent verticalScroll) with FlowRow in ProjectTasksBuilder to eliminate the nested scrollable issue entirely."
            ),
            "🔙 Back Navigation & Confirmation Dialogs" to listOf(
                "Added BackHandler to all full-screen editors (projects, sources, questions, hypotheses, reports, observations) so the device back button works properly — previously dismissOnBackPress=false had no handler.",
                "Added isDirty tracking with confirmation dialog: when content is filled and the user presses back, shows an AlertDialog with Discard (red) and Keep Editing options.",
                "Dirty detection added to all 9 full-screen dialogs: NewQuestion, NewProject, NewSource, NewHypothesis, NewReport, EditObservation, EditProject, EditSource, EditReport.",
                "Added BackHandler with unsaved-data protection to the Observation/Add Observation screen — shows Save & Exit, Discard, or Keep Editing dialog when content exists and back is pressed.",
                "Fixed sharp edges on full-screen dialogs by styling the back button with a rounded Surface (RoundedCornerShape 14dp), adding HorizontalDivider below header, and proper bottom padding (32dp)."
            )
        )
    ),
    FieldMindChangelogEntry(
        version = "1.5.0-weather-v3-expand",
        date = "2026-06-16",
        title = "Random Thunderstorm, Expand Dashboard & Crash Fixes",
        importance = "Patch",
        tags = listOf("Weather", "UI", "Fixes", "Capture"),
        sections = listOf(
            "⛈️ Random Thunderstorm Flashes" to listOf(
                "Thunderstorm animation completely rewritten: replaces constant 150ms flashing (headache-inducing) with random 2-6 second intervals — each lightning strike is unpredictable.",
                "Random flash position (anywhere on screen) and intensity (subtle to bright) for natural variety.",
                "Jagged lightning bolts with branching offshoots, each strike unique per event.",
                "Optional double-flash (35% chance) and screen-edge glow afterglow with slow decay.",
                "Bolt path stays stable for the entire flash duration — no frame-to-frame jitter."
            ),
            "🎬 Orphe-Style Expand Dashboard" to listOf(
                "LiveWeatherDashboardWidget now expands full-screen with a smooth slide-up + fade-in transition (inspired by Orphe music player's beautiful full-screen animation).",
                "Full-screen overlay shows: time-of-day greeting, large gradient temperature, animated weather scene background.",
                "Detailed metrics card: humidity, wind speed, cloud cover, and atmospheric pressure at a glance.",
                "Sunrise/sunset times and moon phase indicators with color-coded info chips.",
                "Fieldwork conditions nudge with contextual advice based on current weather.",
                "Tap the close button or anywhere on the backdrop to dismiss with a reverse slide-out animation."
            ),
            "🐛 Infinite-Height Crash Fixes" to listOf(
                "Replaced AnimatedContent wrappers (which pass infinite maxHeight to scrollable children causing crashes) with static Box + when blocks in FieldMindProjectsScreen and FieldMindObservationsTimeline.",
                "Added Modifier.heightIn(max = ...) constraints to scrollable Column forms inside LazyColumn items (ProjectsScreen, DetailScreen, ScreenUtils).",
                "Added Modifier.fillMaxSize() to inner LazyColumn in ObservationsTimeline for safe layout sizing.",
                "Fix applies to ProjectCreationForm, SpeciesRegistryBuilder, ProjectTasksBuilder, InlineFormCard, and all 4 tab LazyColumns."
            ),
            "📸 Capture Flow & Custom Categories" to listOf(
                "Category picker confirm button now saves observations to database with photo as DraftEvidenceAttachment instead of just navigating away — no more lost captures.",
                "OutlinedTextField appears when 'Other' is selected in the category picker, letting you type any custom category (e.g. 'Reptile', 'Amphibian', 'Fungus').",
                "Confirm button label updates dynamically based on the selected category."
            )
        )
    ),
    FieldMindChangelogEntry(
        version = "1.4.1-build-fixes",
        date = "2026-06-14",
        title = "Build Fixes & Stability Improvements",
        importance = "Patch",
        tags = listOf("Fixes", "Stability", "Build"),
        sections = listOf(
            "Geo-fence Reminders" to listOf(
                "Fixed Long-to-Int type mismatch in loitering delay that caused a compile error.",
                "Fixed nullable GeofencingClient calls that could crash on devices without Google Play Services — all calls now use safe-call operators (?.) so geo-fencing degrades gracefully."
            ),
            "Map Drawing Tools" to listOf(
                "Removed unresolved 'geodesic' property references on Polyline overlays (not present in the bundled osmdroid version).",
                "Fixed DrawingInputHandler tempMarkerDrawer being declared as val while needing reassignment — changed to var."
            ),
            "Offline Tile Manager" to listOf(
                "Resolved 'Cannot infer type parameter R' errors on CacheManager.downloadAreaAsync — now called via reflection for version-safe compatibility.",
                "Fixed unresolved 'cleanArea' method reference (renamed in some osmdroid builds) — now called via reflection with graceful fallback."
            ),
            "Data Tools Screens" to listOf(
                "Fixed widespread escaped-quote syntax errors throughout FieldMindDataTools.kt (backslash-escaped \\\" sequences inside Kotlin string literals caused every string in the file to be invalid).",
                "Counter, Measurement, Weather Log, and Species Tool screens now compile and function correctly."
            ),
            "Navigation" to listOf(
                "MeasurementToolScreen, WeatherLogToolScreen, and SpeciesToolScreen are now correctly resolved in the navigation graph (unblocked by the DataTools syntax fix)."
            ),
            "Observation Detail Screen" to listOf(
                "Added missing onOpenDetail callback parameter to ObservationDetailContent so re-observation parent/child links correctly open detail views."
            ),
            "Home Screen" to listOf(
                "Fixed Triple(...) called with 4 arguments — replaced with Pair<Triple, Screen> so each tool entry correctly carries title, body, icon, and navigation target.",
                "Fixed destructuring to use (info, screen) → val (title, body, icon) = info pattern."
            ),
            "Map Screen" to listOf(
                "Added missing import for android.content.Context used in the Tracks tab.",
                "Added missing import for MaterialSymbolIcon used in DrawToolButton."
            ),
            "Capture Screen" to listOf(
                "Removed duplicate import for SpeciesMatch that caused an ambiguous reference error.",
                "Removed duplicate val context = LocalContext.current declaration in the same composable scope."
            ),
            "Settings Screen" to listOf(
                "Fixed nullable Long? comparison operator (obs.projectId > 0) in Data Integrity page — now safely unwraps with ?: 0L before comparing."
            ),
            "Charts" to listOf(
                "Removed unresolved geodesic property from Polyline in EnhancedOsmMap composable (both the factory block and the update block)."
            ),
            "UI Components" to listOf(
                "Added missing imports for Dialog, DialogProperties, wrapContentHeight, verticalScroll, rememberScrollState, and TextButton that were required by the ProtocolPicker dialog composable."
            )
        )
    ),
    FieldMindChangelogEntry(
        version = "1.4.0-species-id-map-tools",
        date = "2026-06-14",
        title = "Phase 4: Species ID, Offline Maps & Enhanced Observation",
        importance = "Major",
        tags = listOf("Species ID", "Offline Maps", "Observation", "PDF", "Attachments"),
        sections = listOf(
            "🔬 Species Identification Engine (NEW)" to listOf(
                "On-device TensorFlow Lite species classifier with bundled 500-species model (expandable via download).",
                "Post-capture 'Identify species' button in observation evidence row.",
                "Top-5 species matches with confidence scores displayed in a bottom sheet.",
                "Manual species search by common or scientific name for offline lookup.",
                "iNaturalist API fallback integration when online (with user permission toggle).",
                "Auto-populate species name, category (Bird/Mammal/Insect/Plant/etc.), and confidence.",
                "Supports regional model packs: North America, Europe, Asia, Tropical."
            ),
            "🌍 Offline Maps with Drawing Tools (NEW)" to listOf(
                "Offline tile manager downloads OSM tile regions for fully offline map use.",
                "Drawing tools: Polygon (survey boundary), Line (transect), Point (site marker) overlays.",
                "GPS track recording during research sessions with start/stop control.",
                "Geo-fence reminders: 'When you arrive at Site A, remind you to log water quality.'",
                "Multiple tile sources: OSM Standard, Satellite, Terrain.",
                "LRU tile cache with pruning to manage storage."
            ),
            "🔗 Hypothesis-Driven Observation Graph (NEW)" to listOf(
                "Live graph inference: semantic matching of observations to hypotheses as you add them.",
                "Weak signal detection suggests potential connections: 'These 3 observations might support Hypothesis #2'.",
                "Gap detection: 'You're testing H1 but missing observations for alternative H3.'",
                "Question generation: 'Based on your observations, you might ask...'",
                "Citation chains track evidence used to support each hypothesis.",
                "Enhanced detail screen with 'Related' section showing graph connections."
            ),
            "📄 PDF Reader with Native Rendering (NEW)" to listOf(
                "Native AndroidX PDF renderer replaces WebView fallback.",
                "Inline annotations: highlight, underline, sticky notes.",
                "Table of contents navigation for structured documents.",
                "Full-text search within PDF with page and context snippets.",
                "Continuous scroll and page thumbnail preview."
            ),
            "📁 Project Attachments (NEW)" to listOf(
                "Attach photos, PDFs, and documents to projects with folder organization.",
                "Quick preview in project detail with thumbnail gallery.",
                "Organize attachments by folder/type with bulk operations.",
                "Attachment count and type indicators in project cards."
            ),
            "🏠 Home Screen Data Tool Improvements (Enhanced)" to listOf(
                "Counter Tool: session grouping, running totals, chart view, CSV export.",
                "Measurement Tool: measurement history chart, batch entry, unit conversion.",
                "Weather Log Tool: Open-Meteo auto-fetch, 7-day history chart, data export.",
                "Species Tool: auto-categorization, species ID suggestions, photo upload.",
                "All tools now open full dedicated screens with rich interactions."
            )
        )
    ),
    FieldMindChangelogEntry(
        version = "1.3.0-interactive-data-tools",
        date = "2026-06-14",
        title = "Interactive Data Tools — Dedicated Mini-Tools",
        importance = "Major",
        tags = listOf("Data Tools", "Counter", "Measurement", "Weather", "Species"),
        sections = listOf(
            "Interactive Counter Tool" to listOf(
                "Dedicated full-screen counter UI with large +/− stepper buttons and auto-save on each increment.",
                "Real-time count display with pulse animation on increment.",
                "Auto-saves a data record to the database with every tap, building a time-stamped tally history.",
                "Label input to describe what you are counting (birds, trees, samples).",
                "Expandable recent tally history showing all saved counter records."
            ),
            "Measurement Tool" to listOf(
                "Structured measurement form with large value display, decimal place support, and inline unit selector.",
                "Quick unit preset chips (cm, m, mm, g, kg, °C) plus dropdown with 15+ common units.",
                "Details section for label, notes, and location input.",
                "Saves directly to the Data Records database as a Measurement Log entry."
            ),
            "Weather Log Tool" to listOf(
                "Quick weather conditions form with condition picker (Clear, Rain, Snow, Thunderstorm, etc.).",
                "Temperature (°C), humidity (%), and wind speed fields with number pads.",
                "One-tap GPS location fetch with place name resolution.",
                "Saves as a Weather Log data record with structured condition string."
            ),
            "Species Log Tool" to listOf(
                "Quick-capture species observation form with name, count stepper, and confidence selector.",
                "Behavior picker (Feeding, Flying, Calling, Resting, etc.) and habitat input.",
                "Auto-categorizes into Bird/Mammal/Insect/Plant based on species name keywords.",
                "Saves directly as a full observation with facts and species-tracking tag."
            ),
            "Wired from Home Screen" to listOf(
                "Home screen Data Tools card now opens each tool directly — Count, Measure, Weather, Species.",
                "Tap any tile to open its dedicated mini-tool instead of the generic listing screen.",
                "'Open all' button still available to see all data records in the Workspace Data tab."
            )
        )
    ),
    FieldMindChangelogEntry(
        version = "1.2.0-field-research-complete",
        date = "2026-06-14",
        title = "Complete Field Research Redesign (Phases 1-12)",
        importance = "Major",
        tags = listOf("Observations", "Projects", "Analysis", "Reports", "Library", "Journal", "Evidence", "Data"),
        sections = listOf(
            "Observations & Capture (Phase 3-4)" to listOf(
                "Species confidence selector (Certain, Likely, Unsure) with visual feedback.",
                "Distance from observer selector (2m, 10m, 50m, 100m+) for scale reference.",
                "Observation checklist (Seen, Heard, Smelled, Touched, Measured) tracking methods used.",
                "Structured measurements with specialized fields for height, width, length, diameter, weight, and fungi-specific properties.",
                "Quality score calculator (0-100%) showing data completeness and missing field checklist.",
                "Follow-up scheduling system (None, Tomorrow, 3 days, 1 week, Custom) with notifications.",
                "Image annotation tools (circle, arrow, highlight, label, measurement line) for visual notes.",
                "Capture mode toggle between single observation and each-photo-equals-observation modes.",
                "Collapsible 'Structured details' section reduces visual complexity while preserving advanced features."
            ),
            "Research Sessions (Phase 4)" to listOf(
                "Session persistence guarantees observations and notes are never lost when navigating away.",
                "Active session restoration shows elapsed timer and observation count on home screen.",
                "Session notification includes real-time timer updates (e.g., 'Running • 05:30 • 3 obs').",
                "Session summary displays total duration, observation count, evidence count, and linked project.",
                "Quick export to project journal for research storytelling.",
                "Audio recording moved to dedicated card below attachments for cleaner UI organization."
            ),
            "Projects & Workspace (Phase 5)" to listOf(
                "Project types: Observation, Investigation, Survey, Experiment, Monitoring for targeted workflows.",
                "Research method builder lets you select from 10+ methods (daily observations, photo documentation, audio recording, measurements, species counting, weather logging, behavior logging, comparison tables).",
                "Auto-recommended data fields and charts based on selected methods.",
                "Project templates (Species Survey, Behavior Study, Site Survey, Experiment, Monitoring, Weather Study, Phenology Study, Site Comparison) with pre-configured fields.",
                "Project journal auto-generates dated entries from observations, notes, hypotheses, and data.",
                "Project timeline visualizes observations chronologically for research storytelling.",
                "Project relationships graph shows connections: Questions → Observations → Evidence → Hypotheses → Reports."
            ),
            "Evidence Hub (Phase 6)" to listOf(
                "Advanced filtering by category, date, tag, location, confidence, project, evidence type, and completeness.",
                "Bulk management: select multiple, archive, delete, add tags, link to project, export as bundle.",
                "Evidence status tracking (Used in analysis, Needs review, Missing metadata) for quality control.",
                "Completeness indicator shows what metadata is present and what's missing.",
                "Grid view with checkbox selection for batch operations on evidence."
            ),
            "Data Workspace (Phase 7)" to listOf(
                "Question-first data collection: 'What are you tracking?' (Count, Measure, Compare, Track Changes, Record Weather, Track Species).",
                "Auto-generated dataset with suggested fields and live preview.",
                "Mobile-optimized data record cards instead of cramped tables.",
                "Quick tally counter for rapid tallying during observations.",
                "Each record captures label, value/unit, date, project link, and linked observation/evidence."
            ),
            "Hypotheses Redesign (Phase 8)" to listOf(
                "Hypothesis cards display prediction, confidence percentage, status, and evidence count at a glance.",
                "Status tracking (Supported, Contradicted, Inconclusive, Untested) for hypothesis lifecycle.",
                "Linked question display shows what research question this hypothesis tests.",
                "Evidence needed and support/contradiction criteria documented for test design.",
                "Next test/action indicator guides your research workflow."
            ),
            "Insights Dashboard (Phase 9)" to listOf(
                "GitHub-style calendar heatmap with proper month boundaries, week labels, and tap tooltips.",
                "By-hour and by-day horizontal ranking bars showing activity distribution.",
                "Daily trend card showing peak day and trend direction with sparkline.",
                "Category distribution bars with readable, non-cut-off labels.",
                "Research health card surfaces actionable issues: 'Add more evidence', 'Link questions to hypotheses', 'Enable GPS', 'Add weather'.",
                "Confidence score card displays overall research quality (Strong/Weak evidence).",
                "Knowledge graph with clustering by project/entity type and legend/filters.",
                "Open questions summary showing created date, linked observations, and status/priority."
            ),
            "Notes & Journal (Phase 10)" to listOf(
                "Rich block editor supports 11 block types: Text, Image, Drawing, Audio, Observation embed, Checklist, Quote, Table, Map, Reference link, Handwritten note.",
                "Rich text formatting toolbar with Bold, Italic, Underline, Strikethrough, and inline links.",
                "Split view on tablets showing photo on left, notes on right for efficient capture.",
                "Live observation embeds stay linked to original observation data.",
                "Categories converted to tags inside notes for flexible organization.",
                "Inline image annotation with drawing tools."
            ),
            "Reports Redesign (Phase 11)" to listOf(
                "7 report types grouped by difficulty: Observation, Species, Summary (Beginner); Site Survey, Field Report (Intermediate); Lab Report, Literature Review (Advanced).",
                "Generate from Project button auto-populates sections from project data.",
                "Generate Draft button uses AI-powered outlining for report structure.",
                "Auto-sections: Background, Methods, Results, Evidence Summary, Visual Evidence, Research Journey, Conclusion Draft, Limitations, Next Steps.",
                "Visual evidence gallery (photos, charts, maps, tables, timelines) auto-inserted from project.",
                "Multiple exports: PDF, DOCX-compatible HTML, Markdown, share link, presentation outline/slides.",
                "Knowledge extraction: highlight text to create note, quote, flashcard, question, or project evidence."
            ),
            "Library & Sources (Phase 12)" to listOf(
                "Native research reading workspace with PDF viewer, page thumbnails, navigation, search, bookmarks.",
                "Annotation tools: highlights, underline, strike-through, drawing/handwriting, comments on selection.",
                "Knowledge extraction menu extracts highlights as notes, quotes, flashcards, questions, or project evidence.",
                "8 source types: PDF, Image, Audio, Video, Document, Spreadsheet, Presentation, Web Link.",
                "Source metadata panel shows publisher, DOI, journal, credibility score, citation style.",
                "Backlinks tracking shows where source is referenced (notes, projects, hypotheses, flashcards, reports).",
                "OCR for image/page photos; DOI/ISBN metadata lookup; Cornell notes templates.",
                "Highlights collection shows all annotations with search and tag filtering."
            ),
            "Weather & Location (Phase 1 Refinement & Beyond)" to listOf(
                "Weather database screen displays offline weather records with statistics (avg temp, range, humidity, wind).",
                "Weather location display shows specific location or 'multiple locations' for multi-site research.",
                "Auto-weather toggle in settings automatically captures weather for every observation.",
                "GPS fetch button in observations, projects, and research sessions.",
                "Weather data persists correctly in observation details when captured during research sessions.",
                "Open-Meteo free API used for weather when no custom API key configured."
            ),
            "Insights Dashboard Fixes (Phase 1)" to listOf(
                "Calendar heatmap 'Swipe or use arrows' helper text removed for cleaner UI.",
                "Duplicate calendar day information display removed.",
                "Research metrics grid restructured from 3×3 to 2×2 layout for visual balance.",
                "'Data' button in home screen now navigates to Weather Database.",
                "Weather card shows location information instead of generic API text."
            ),
            "Home Screen Redesign (Phase 2)" to listOf(
                "Recent Captures card displays latest 3 observations with category, location, and time.",
                "Clickable observations open detail views for quick review.",
                "Data Tools card shows Count, Measure, Weather, Species tracking options.",
                "Live research session CTA shows active timer (e.g., 'Research Session A • 05:30').",
                "Button changes to 'Continue' when session is active with tertiaryContainer highlighting."
            )
        )
    ),
    FieldMindChangelogEntry(
        version = "0.9.0-field-research-dashboard",
        date = "2026-06-13",
        title = "Research Dashboard & Interactive Data Tools",
        importance = "Major",
        tags = listOf("Dashboard", "Charts", "Data Table", "Analytics", "Insights"),
        sections = listOf(
            "Research Dashboard" to listOf(
                "Insights screen completely redesigned into a 9-section Research Dashboard with profile card, performance metrics, time-series analytics, category/tag analysis, knowledge graph timeline, research health score, weather correlation, achievements, and data records table.",
                "New calendar heatmap shows daily observation activity across 12 months (GitHub-style contribution grid).",
                "New radar/spider chart compares observation categories across multiple dimensions.",
                "New tag co-occurrence matrix reveals which tags appear together most often.",
                "New activity-by-hour and day-of-week charts show when you observe most.",
                "New moving average chart with 7-day rolling overlay on daily counts.",
                "New weather correlation scatter plot with trend line (requires GPS + weather on capture).",
                "New data quality meter scores research health across 5 dimensions (evidence, questions, hypotheses, tags, GPS).",
                "New network graph timeline visualizes how your knowledge graph evolved over time with an interactive slider.",
                "15 achievements with progress tracking, tiered unlocks, and snackbar celebration."
            ),
            "Interactive Data Table" to listOf(
                "New FieldDataTable composable with sortable columns, search across all fields, column-specific filters, numerical aggregates (count/sum/avg), pivot table view, CSV export, and row selection.",
                "Data records now display in a spreadsheet-style table within the Research Dashboard."
            ),
            "Extended Chart Library" to listOf(
                "8 new Canvas-based chart composables: CalendarHeatmap, RadarChart, TagCoOccurrenceMatrix, ActivityByHourChart, DayOfWeekChart, MovingAverageChart, WeatherCorrelationChart, DataQualityMeter.",
                "All charts are fully offline, zero external dependencies, and built entirely with Compose Canvas."
            ),
            "Observation Improvements" to listOf("Observations now support structured category-specific fields (JSON), live stopwatch tracking, manual duration override, change timing markers, and context presets.", "Field mode buttons allow one-tap observation per category with undo support.")
        )
    ),
    FieldMindChangelogEntry(
        version = "0.8.0-field-redesign",
        date = "2026-06-13",
        title = "Immersive research workspace foundation",
        importance = "Major",
        tags = listOf("Camera", "Observations", "Projects", "Migration"),
        sections = listOf(
            "Capture" to listOf("Camera V2 now opens as an immersive full-screen surface.", "Quick Snap can attach GPS and weather metadata when permissions and settings allow it."),
            "Research records" to listOf("Observations gained stopwatch, manual duration, change timing, and structured category fields.", "Projects, reports, and data records gained metadata for connections, attachments, templates, and chart preferences."),
            "Data safety" to listOf("The FieldMind database now uses an explicit Room migration instead of destructive migration for the new schema."),
            "Navigation" to listOf("Bottom navigation icons are larger and use a subtle spring motion for a more tactile feel.")
        )
    )
)

@Composable
fun FieldMindChangelogScreen(onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StandardScreenHeader(
                title = "What's New",
                subtitle = "Complete field research redesign with 12 phases of new features",
                icon = FieldMindIcons.Info,
                trailing = {
                    BackButton(onClick = onBack)
                }
            )
        }
        
        // Introduction card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            FieldMindIcons.Sparkle,
                            null,
                            size = 24.dp,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Phases 1-12: Complete Redesign",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "From dashboard foundations to a complete research workspace. Observations, projects, analysis, hypotheses, reports, and knowledge management—all redesigned for modern field research.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        items(fieldMindChangelog) { entry -> ChangelogEntryCard(entry) }
    }
}

@Composable
private fun ChangelogEntryCard(entry: FieldMindChangelogEntry) {
    val isLatest = entry.version == fieldMindChangelog.first().version
    val accentColor = when {
        isLatest -> MaterialTheme.colorScheme.primary
        entry.importance == "Major" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }
    
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLatest) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                          else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLatest) 4.dp else 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header with version badge
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isLatest) FieldMindIcons.Sparkle else FieldMindIcons.Info,
                        null,
                        tint = accentColor,
                        size = 32.dp
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(FieldMindIcons.Calendar, null, size = 14.dp, 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            entry.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            entry.version,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Importance badge
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .background(
                            when (entry.importance) {
                                "Major" -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.tertiaryContainer
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        entry.importance,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (entry.importance) {
                            "Major" -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onTertiaryContainer
                        }
                    )
                }
            }

            // Tags with icons
            if (entry.tags.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    entry.tags.forEach { tag ->
                        Box(
                            Modifier.clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                tag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Divider
            Spacer(
                Modifier.fillMaxWidth().height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            )

            // Feature sections with icons
            entry.sections.forEach { (heading, bullets) ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Section heading with icon
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            Modifier.size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(accentColor)
                        )
                        Text(
                            heading,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Feature bullets with improved spacing
                    bullets.forEach { bullet ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                FieldMindIcons.Check,
                                null,
                                size = 18.dp,
                                tint = accentColor.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                bullet,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            // Latest badge for current version
            if (isLatest) {
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            FieldMindIcons.Sparkle,
                            null,
                            size = 16.dp,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Latest version with comprehensive research features",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
