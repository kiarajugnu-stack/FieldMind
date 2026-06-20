package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.data.export.FieldReportTemplates
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil

// ══════════════════════════════════════════════════════════════════════
//  Research Hub — Full redesign per spec
//  Features: Start Session, New Project, Templates (19 types + 19 templates),
//  Emoji picker, Priority/Dates/Tags, 5 tabs (Overview, Observations,
//  Hypotheses, Data, Reports)
// ══════════════════════════════════════════════════════════════════════

// ── Project Types (19 — each type has a matching template below) ──
internal val researchProjectTypes = listOf(
    "Species Survey", "Population Census", "Habitat Assessment",
    "Wildlife Monitoring", "Behavioral Study", "Migration Study",
    "Biodiversity Inventory", "Vegetation Survey", "Conservation Project",
    "Ecological Research", "Camera Trap Study", "Acoustic Monitoring",
    "Pollinator Survey", "Water Quality Study", "Citizen Science Project",
    "Environmental Impact Study", "Long Term Monitoring", "Species Discovery",
    "Custom Research Project"
)

// ── Project Templates (19 — one per project type) ──
internal data class ProjectTemplateDef(
    val name: String,
    val type: String,
    val icon: MaterialSymbolIcon,
    val description: String,
    val category: String,
    val priority: String = "Medium",
    val defaultMethods: Set<String>,
    val objective: String,
    val question: String,
    val background: String,
    val methodPlan: String,
    val hypothesis: String,
    val dataPlan: String,
    val analysisPlan: String,
    val nextAction: String,
    val tags: String
)

internal val projectTemplates = listOf(
    ProjectTemplateDef("Bird Survey", "Species Survey", FieldMindIcons.Bird, "Point counts and call/photo evidence for birds at fixed sites.", "Ornithology", defaultMethods = setOf("Species counting", "Audio recording", "Photo documentation", "Weather logging"), objective = "Measure bird richness and relative abundance across selected points.", question = "Which bird species are present, and how do detections vary by time, weather, and habitat?", background = "Bird activity changes with habitat structure, time of day, season, and disturbance.", methodPlan = "Run 10-minute point counts at marked GPS points; record seen/heard species, count, distance band, behavior, weather, and audio/photo evidence.", hypothesis = "Sites with more layered vegetation will have higher species richness and more detections.", dataPlan = "species, count, detection type, distance band m, habitat, time, temperature C, wind, photo/audio URI", analysisPlan = "Compare richness and counts by point, habitat, and visit; flag uncertain IDs for review.", nextAction = "Create 3 fixed count points and run the first morning survey.", tags = "birds, point-count, audio, habitat"),
    ProjectTemplateDef("Mammal Track Survey", "Population Census", FieldMindIcons.Animal, "Tracks, scat, camera trap records, and direct mammal sightings.", "Mammalogy", defaultMethods = setOf("GPS tracking", "Photo documentation", "Camera trap", "Species counting"), objective = "Document mammal presence and activity signs along repeatable transects.", question = "Which mammals use this site, and where are signs concentrated?", background = "Mammals are often detected indirectly through tracks, scat, burrows, and camera-trap events.", methodPlan = "Walk fixed transects; photograph signs with scale, log substrate, freshness, GPS, and camera-trap station IDs.", hypothesis = "Mammal signs will cluster near water sources and edge habitats.", dataPlan = "species/sign type, confidence, GPS, substrate, freshness, camera station, photo URI", analysisPlan = "Map sign density by transect segment and compare direct vs indirect detections.", nextAction = "Set transect start/end points and place the first camera station.", tags = "mammals, tracks, camera-trap, transect"),
    ProjectTemplateDef("Butterfly Transect", "Pollinator Survey", FieldMindIcons.Insect, "Timed pollinator walks with host-plant and weather context.", "Entomology", defaultMethods = setOf("Species counting", "Photo documentation", "Weather logging", "Behavior logging"), objective = "Track butterfly abundance and plant associations along a fixed route.", question = "Which butterfly species visit which plants under different weather conditions?", background = "Pollinator activity is strongly shaped by temperature, wind, sunlight, and flowering stage.", methodPlan = "Walk the same transect at steady pace; log species, count, plant visited, behavior, temperature, wind, and cloud cover.", hypothesis = "Sunny low-wind periods will produce higher butterfly counts and more feeding behavior.", dataPlan = "species, count, plant species, behavior, temperature C, wind km/h, cloud %, photo URI", analysisPlan = "Summarize counts by plant species and weather band; identify peak activity windows.", nextAction = "Mark the transect and list flowering plants before first count.", tags = "butterflies, pollinators, transect, flowers"),
    ProjectTemplateDef("Vegetation Quadrat", "Vegetation Survey", FieldMindIcons.Plant, "Quadrat sampling for plant cover, growth stage, and evidence photos.", "Botany", defaultMethods = setOf("Measurement logging", "Photo documentation", "Species counting"), objective = "Estimate plant composition and percent cover in repeatable quadrats.", question = "How does plant cover and species composition differ among microhabitats?", background = "Quadrat surveys provide repeatable measurements for vegetation structure and change.", methodPlan = "Place quadrats using a consistent design; record species, percent cover, height, phenology, substrate, and overhead photos.", hypothesis = "Moister quadrats will have higher cover and different dominant species.", dataPlan = "quadrat ID, species, percent cover, height cm, phenology, substrate, soil moisture, photo URI", analysisPlan = "Calculate richness, mean cover, and dominant species per habitat/microhabitat.", nextAction = "Define quadrat size and sample locations.", tags = "plants, quadrat, cover, phenology"),
    ProjectTemplateDef("Nest Monitoring", "Wildlife Monitoring", FieldMindIcons.Observation, "Scheduled nest checks with disturbance-minimizing visit notes.", "Ecology", "High", setOf("Daily observations", "Photo documentation", "Behavior logging"), "Monitor nesting status, adult behavior, and outcome without disturbing wildlife.", "What nesting stages occur and what factors are associated with success or failure?", "Nest monitoring needs consistent timing and careful notes to avoid influencing outcomes.", "Record nest ID, stage, adult activity, contents from a safe distance, visit duration, weather, and disturbance signs.", "Nests with lower disturbance and better cover will show higher success.", "nest ID, stage, adult behavior, contents, visit duration min, weather, disturbance, photo URI", "Build a timeline per nest and compare outcomes with cover and disturbance notes.", "Create nest IDs and safe observation distances before first check.", "nesting, behavior, monitoring"),
    ProjectTemplateDef("Camera Trap Research", "Camera Trap Study", FieldMindIcons.Camera, "Camera station deployment, checks, and species-event classification.", "Wildlife", defaultMethods = setOf("Camera trap", "Photo documentation", "Species counting", "Weekly observations"), objective = "Estimate species presence and activity windows from camera-trap events.", question = "Which species pass each station, and at what times are they most active?", background = "Camera traps detect cryptic wildlife and support event-based activity analysis.", methodPlan = "Log station setup, height, bearing, habitat, lure/bait status, SD card checks, and classify independent events.", hypothesis = "Stations near game trails will record more independent events than random stations.", dataPlan = "station ID, species, event time, count, behavior, confidence, habitat, battery, SD card, media URI", analysisPlan = "Summarize events per station-night and activity by hour.", nextAction = "Deploy stations and record setup metadata.", tags = "camera-trap, mammals, activity"),
    ProjectTemplateDef("Water Quality Check", "Water Quality Study", FieldMindIcons.Water, "Repeatable water observations and measurements for streams/ponds.", "Hydrology", defaultMethods = setOf("Water testing", "Measurement logging", "Photo documentation", "Weather logging"), objective = "Track basic water conditions over repeat visits.", question = "How do clarity, temperature, flow, and visible organisms change after weather events?", background = "Water quality shifts with runoff, temperature, flow, algae, and surrounding land use.", methodPlan = "At fixed points measure temperature, clarity/turbidity, flow class, depth, odor/color, organisms, rainfall context, and photos.", hypothesis = "Recent rainfall will reduce clarity and increase flow at sampling points.", dataPlan = "site ID, water temp C, clarity, flow, depth cm, odor, color, organisms, rainfall, photo URI", analysisPlan = "Plot water measures by date and rainfall; compare upstream/downstream sites.", nextAction = "Choose sampling points and prepare measurement tools.", tags = "water, hydrology, rainfall, quality"),
    ProjectTemplateDef("Acoustic Survey", "Acoustic Monitoring", FieldMindIcons.Mic, "Audio-based detections for birds, frogs, bats, or soundscapes.", "Ecology", defaultMethods = setOf("Audio recording", "Species counting", "Weather logging"), objective = "Use repeat audio samples to detect vocal species and activity timing.", question = "Which species are detected by sound, and when are calling rates highest?", background = "Acoustic monitoring captures species that are hard to see and preserves evidence for review.", methodPlan = "Record fixed-duration clips at marked stations; log time, weather, habitat, device, noise level, and detected calls.", hypothesis = "Calling activity will peak near dawn/dusk and after suitable weather conditions.", dataPlan = "station, start time, duration, species/call, confidence, noise, weather, audio URI", analysisPlan = "Compare detections by station and time window; review uncertain calls.", nextAction = "Test recorder settings and create station IDs.", tags = "acoustic, audio, calls, monitoring"),
    ProjectTemplateDef("Habitat Assessment", "Habitat Assessment", FieldMindIcons.Nature, "Habitat structure, disturbance, vegetation, soil, and photo points.", "Ecology", defaultMethods = setOf("Measurement logging", "Photo documentation", "GPS tracking"), objective = "Describe habitat structure and condition across mapped assessment points.", question = "Which habitat features and disturbance indicators characterize this site?", background = "Habitat assessments connect species observations to vegetation, substrate, water, and human impact.", methodPlan = "At each point record canopy/ground cover, dominant plants, substrate, water presence, disturbance, slope/aspect, and repeat photos.", hypothesis = "Higher structural diversity will correspond with more wildlife observations.", dataPlan = "point ID, canopy %, ground cover %, dominant plants, substrate, disturbance score, GPS, photo URI", analysisPlan = "Map habitat scores and compare with observations by point.", nextAction = "Lay out assessment points and create a scoring rubric.", tags = "habitat, vegetation, disturbance, GPS"),
    ProjectTemplateDef("Behavioral Observation Log", "Behavioral Study", FieldMindIcons.Trend, "Focal follows and time-budget observations of animal behavior patterns.", "Behavioral Ecology", defaultMethods = setOf("Daily observations", "Behavior logging", "Photo documentation", "Timed observation"), objective = "Quantify behavioral patterns, time budgets, and social interactions of target species.", question = "How do behavioral patterns vary by time of day, season, and social context?", background = "Behavioral observations require systematic sampling to capture representative activity patterns.", methodPlan = "Conduct focal follows or scan samples at regular intervals; log behaviors, durations, social partners, and environmental context.", hypothesis = "Feeding behavior will peak in early morning and late afternoon, with rest during midday.", dataPlan = "subject ID, behavior, start time, duration, social context, habitat, temperature, photo/video URI", analysisPlan = "Calculate time budgets per behavior category and compare across time windows and social contexts.", nextAction = "Define ethogram and practice behavior coding before first full observation session.", tags = "behavior, ethogram, time-budget, social"),
    ProjectTemplateDef("Migration Route Census", "Migration Study", FieldMindIcons.Track, "Systematic counts of migrating species at fixed points and times.", "Ecology", defaultMethods = setOf("Species counting", "GPS tracking", "Photo documentation", "Daily observations"), objective = "Document migration timing, direction, and species composition at a fixed observation point.", question = "When do target species migrate through this site, and how do counts vary with weather conditions?", background = "Migration monitoring at fixed sites produces consistent data for phenology and population trends.", methodPlan = "Count individuals passing a fixed point at standard times; record species, direction, estimated altitude, group size, and weather.", hypothesis = "Peak migration counts will occur after cold fronts with favorable tailwinds.", dataPlan = "species, count, direction, altitude m, group size, time, wind, visibility, temperature C", analysisPlan = "Plot daily counts against weather variables; compare arrival dates across years.", nextAction = "Select observation point with clear view and begin daily counts at scheduled times.", tags = "migration, phenology, census, birds"),
    ProjectTemplateDef("Biodiversity Rapid Assessment", "Biodiversity Inventory", FieldMindIcons.Nature, "Multi-taxon rapid inventory of species richness at a site.", "Ecology", defaultMethods = setOf("Species counting", "Photo documentation", "GPS tracking", "Audio recording"), objective = "Survey as many species as possible across taxonomic groups in a defined area and time window.", question = "What is the species richness and composition of this site across major taxonomic groups?", background = "Rapid biodiversity assessments capture a snapshot of species presence under standardized effort.", methodPlan = "Survey all major taxa (birds, mammals, plants, insects, herps) within a defined boundary using timed transects, traps, and visual encounter surveys.", hypothesis = "Sites with greater habitat heterogeneity will support higher species richness.", dataPlan = "species, taxon group, abundance, detection method, GPS, habitat, time, specimen photo URI", analysisPlan = "Compile species list by taxon; calculate richness, Shannon diversity, and compare with habitat variables.", nextAction = "Define survey boundary, prepare species datasheets, and schedule the assessment window.", tags = "biodiversity, inventory, richness, rapid-assessment"),
    ProjectTemplateDef("Conservation Action Plan", "Conservation Project", FieldMindIcons.Flag, "Threat assessment, intervention planning, and outcome tracking for a conservation target.", "Conservation", "High", setOf("Weekly observations", "Photo documentation", "GPS tracking", "Species counting"), objective = "Identify threats and implement measurable conservation actions for a target species or habitat.", question = "What are the primary threats to the target, and which interventions are most effective?", background = "Conservation projects require baseline data, threat assessment, stakeholder engagement, and measurable outcomes.", methodPlan = "Map threats, establish monitoring transects, implement intervention(s), and track before-after indicators at fixed intervals.", hypothesis = "The selected intervention will reduce threat indicators by at least 20% within one year.", dataPlan = "site, threat type, severity score, indicator species, count, GPS, intervention, date, photo URI", analysisPlan = "Compare before-after indicator values with paired t-test or occupancy models.", nextAction = "Define conservation target, complete threat assessment, and establish baseline monitoring points.", tags = "conservation, threats, intervention, monitoring"),
    ProjectTemplateDef("Ecosystem Field Study", "Ecological Research", FieldMindIcons.School, "Multi-factor ecological study of species interactions and ecosystem processes.", "Ecology", defaultMethods = setOf("Measurement logging", "Photo documentation", "Behavior logging", "Weather logging"), objective = "Quantify ecological relationships and processes in a defined ecosystem.", question = "How do abiotic factors, species interactions, and disturbance shape ecosystem structure and function?", background = "Ecological research integrates species, environment, and process data across spatial and temporal scales.", methodPlan = "Establish study plots or transects; record species composition, abundance, environmental variables, and interaction observations.", hypothesis = "Nutrient availability and disturbance regime together explain more variation in community composition than either factor alone.", dataPlan = "plot ID, species, abundance, environmental variables, interactions, disturbance, GPS, photo URI", analysisPlan = "Use multivariate analysis (NMDS, PERMANOVA) to relate community composition to environmental gradients.", nextAction = "Select study area, establish plot locations, and begin environmental baseline measurements.", tags = "ecology, ecosystem, community, multivariate"),
    ProjectTemplateDef("Community Science Campaign", "Citizen Science Project", FieldMindIcons.HumanBehavior, "Engage community volunteers in structured data collection and public science.", "Citizen Science", defaultMethods = setOf("Daily observations", "Photo documentation", "GPS tracking", "Species counting"), objective = "Recruit and train community volunteers to collect standardized scientific data.", question = "Can community-collected data produce results comparable to professional surveys?", background = "Citizen science projects scale data collection across broad areas while building public engagement.", methodPlan = "Develop simple protocols, recruit volunteers, provide training materials, collect and review submitted data, and share results.", hypothesis = "Volunteer-collected data will show the same broad patterns as professional surveys with acceptable error margins.", dataPlan = "observer ID, species/observation, GPS, date, time, photo evidence, confidence, notes", analysisPlan = "Compare volunteer data quality against expert validation subset; summarize spatial coverage and trends.", nextAction = "Design simple protocol, recruit first 10 volunteers, and launch a pilot data collection week.", tags = "citizen-science, community, volunteers, outreach"),
    ProjectTemplateDef("Environmental Baseline Survey", "Environmental Impact Study", FieldMindIcons.Settings, "Pre-impact environmental assessment with multi-factor baselines and photo points.", "Conservation", "High", setOf("Measurement logging", "Photo documentation", "Water testing", "GPS tracking"), objective = "Establish a comprehensive environmental baseline before a planned development or land-use change.", question = "What are the current environmental conditions, and how will they be affected by the proposed change?", background = "Baseline surveys capture pre-impact conditions for soil, water, vegetation, and wildlife to enable impact assessment.", methodPlan = "Survey soil, water quality, vegetation cover, and wildlife at fixed baseline points using standardized methods and photo documentation.", hypothesis = "Sites closer to the planned development boundary will show different baseline characteristics from reference sites.", dataPlan = "site ID, parameter, value, unit, method, GPS, date, photo URI, disturbance level", analysisPlan = "Compare proposed-impact sites with control/reference sites using multivariate statistics.", nextAction = "Define project boundary, identify control sites, and conduct initial baseline measurements.", tags = "environmental-impact, baseline, EIA, assessment"),
    ProjectTemplateDef("Long-Term Monitoring Plan", "Long Term Monitoring", FieldMindIcons.Streak, "Repeated standardized measurements at fixed intervals to detect trends and change.", "Ecology", defaultMethods = setOf("Weekly observations", "Measurement logging", "Photo documentation", "Weather logging"), objective = "Establish a repeatable monitoring protocol to detect population or environmental trends over time.", question = "How are target indicators changing over time, and are the changes statistically significant?", background = "Long-term monitoring detects trends that short-term studies miss—requires consistent methods, timing, and record-keeping.", methodPlan = "Define indicators, set fixed monitoring points, schedule repeat visits at consistent intervals, and maintain a metadata log of any method changes.", hypothesis = "Target indicators will show directional change consistent with predicted climate or land-use impacts.", dataPlan = "site ID, date, indicator, value, method, observer, weather, equipment notes, photo URI", analysisPlan = "Analyze trends using linear mixed models or Mann-Kendall tests; flag significant deviations.", nextAction = "Define 3-5 key indicators, set monitoring schedule, and document a standard operating procedure.", tags = "long-term, monitoring, trends, time-series"),
    ProjectTemplateDef("Novel Species Investigation", "Species Discovery", FieldMindIcons.Sparkle, "Documentation and description of potentially unknown or cryptic species.", "Taxonomy", defaultMethods = setOf("Photo documentation", "Measurement logging", "Audio recording", "Species counting"), objective = "Collect evidence to confirm a species identity or document a potentially new species.", question = "Does this observation represent an undescribed species, a range extension, or a known species?", background = "Species discovery requires thorough documentation, comparison with type specimens, and expert verification.", methodPlan = "Document the specimen/organism with detailed photos, measurements, GPS, habitat, and behavioral notes. Collect samples where permitted. Consult reference collections and experts.", hypothesis = "Morphological and genetic comparison will confirm whether this represents a known, range-extending, or novel species.", dataPlan = "species candidate, GPS, measurements, photos, habitat, behavior, collector, date, expert consulted", analysisPlan = "Compare with taxonomic keys and reference images; submit to expert review or citizen science platform.", nextAction = "Take detailed photos from multiple angles, collect GPS coordinates, and consult a field guide or expert.", tags = "species-discovery, taxonomy, new-species, range-extension"),
    ProjectTemplateDef("Custom Blank Template", "Custom Research Project", FieldMindIcons.Project, "Start from a clean workspace with only core planning fields.", "Other", defaultMethods = emptySet(), objective = "", question = "", background = "", methodPlan = "", hypothesis = "", dataPlan = "", analysisPlan = "", nextAction = "", tags = "")
)

// ── Research Categories ──
internal val researchCategories = listOf(
    "Ornithology", "Mammalogy", "Herpetology", "Entomology",
    "Botany", "Ecology", "Conservation", "Climate Science",
    "Hydrology", "Geology", "Oceanography", "Citizen Science",
    "Behavioral Ecology", "Evolution", "Taxonomy", "Other"
)

@Composable
fun ProjectsScreen(
    viewModel: FieldMindViewModel,
    onOpenDetail: (String, Long) -> Unit = { _, _ -> },
    startTab: Int = 0,
    onStartSession: (() -> Unit)? = null,
    onNavigate: ((FieldMindScreen) -> Unit)? = null
) {
    val questions by viewModel.questions.collectAsState()
    val hypotheses by viewModel.hypotheses.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val observations by viewModel.observations.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val data by viewModel.dataRecords.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val researchSessions by viewModel.researchSessions.collectAsState()
    var tab by remember(startTab) { mutableIntStateOf(startTab.coerceIn(0, 4)) }
    val haptics = rememberFieldMindHaptics()
    val workspaceSubNavTabs = listOf(
        SubNavTab("Overview", FieldMindIcons.Home),
        SubNavTab("Observations", FieldMindIcons.Observation),
        SubNavTab("Hypotheses", FieldMindIcons.Hypothesis),
        SubNavTab("Data", FieldMindIcons.Data),
        SubNavTab("Reports", FieldMindIcons.Report)
    )

    fun selectTab(next: Int) {
        val bounded = next.coerceIn(0, workspaceSubNavTabs.lastIndex)
        if (bounded != tab) { tab = bounded; haptics.light() }
    }

    Column(Modifier.fillMaxSize()) {
        // ── Research Hub Header (expanded) ──
        StandardScreenHeader(
            title = "Research Hub",
            subtitle = "Manage projects, templates, and research workflow",
            icon = FieldMindIcons.Projects,
            heroColor = FieldMindTheme.colors.project,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
        FieldMindSubNavBar(
            tabs = workspaceSubNavTabs,
            selectedIndex = tab,
            onTabSelected = { selectTab(it) },
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // Static tab content (no AnimatedContent to avoid infinite-height crash with LazyColumn)
        Box(
            modifier = Modifier.fillMaxSize().pointerInput(tab) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                        onDragEnd = { if (abs(totalDrag) > 96f) { if (totalDrag < 0) selectTab(tab + 1) else selectTab(tab - 1) } }
                    )
                }
            ) {
                when (tab) {
                    0 -> ResearchHubOverviewTab(viewModel, projects, observations, questions, hypotheses, sources, data, reports, researchSessions, onOpenDetail, onStartSession, onNavigate)
                    1 -> ObservationsTab(viewModel, observations, projects, onOpenDetail)
                    2 -> HypothesesTab(viewModel, hypotheses, questions, observations, onOpenDetail, onNavigate)
                    3 -> DataTab(viewModel, data, reports, observations, projects, onOpenDetail, onNavigate)
                    4 -> ReportsTab(viewModel, reports, projects, onOpenDetail, onNavigate)
                }
            }
        }
    }

// ══════════════════════════════════════════════════════════════════════
//  Research Hub Overview Tab — Full redesign with templates, emoji, dates
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ResearchHubOverviewTab(
    viewModel: FieldMindViewModel,
    projects: List<ProjectEntity>,
    observations: List<ObservationEntity>,
    questions: List<QuestionEntity>,
    hypotheses: List<HypothesisEntity>,
    sources: List<SourceEntity>,
    data: List<DataRecordEntity>,
    reports: List<ReportEntity>,
    researchSessions: List<ResearchSessionEntity>,
    onOpenDetail: (String, Long) -> Unit,
    onStartSession: (() -> Unit)? = null,
    onNavigate: ((FieldMindScreen) -> Unit)? = null
) {
    var showNewProject by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var showTypes by remember { mutableStateOf(false) }

    // Project creation state (full spec)
    var projTitle by remember { mutableStateOf("") }
    var projType by remember { mutableStateOf(researchProjectTypes[0]) }
    var projTemplate by remember { mutableStateOf(projectTemplates.last().name) }
    var projDesc by remember { mutableStateOf("") }
    var projCategory by remember { mutableStateOf(researchCategories[0]) }
    var projPriority by remember { mutableStateOf("Medium") }
    var projStatus by remember { mutableStateOf("Planning") }
    var projStartDate by remember { mutableStateOf("") }
    var projEndDate by remember { mutableStateOf("") }
    var projTeam by remember { mutableStateOf("") }
    var projTags by remember { mutableStateOf("") }
    var projQuestion by remember { mutableStateOf("") }
    var projMethods by remember { mutableStateOf(setOf("Photo documentation", "Daily observations")) }
    var projHypothesis by remember { mutableStateOf("") }

    val haptics = rememberFieldMindHaptics()

    // Computed metrics
    val totalProjects = projects.size
    val totalObs = observations.size
    val uniqueSites = observations.mapNotNull { it.manualLocation.ifBlank { if (it.latitude != null && it.longitude != null) "${it.latitude},${it.longitude}" else null } }.distinct().size
    val openQuestions = questions.count { it.answer.isBlank() }
    val supportedHypotheses = hypotheses.count { it.resultStatus.equals("Supported", true) }
    val refutedHypotheses = hypotheses.count { it.resultStatus.equals("Refuted", true) }
    val untestedHypotheses = hypotheses.size - supportedHypotheses - refutedHypotheses
    val totalFieldHours = researchSessions.sumOf { it.totalDurationMs } / 3600_000.0
    val totalSessions = researchSessions.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 14.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. Start Research Session ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { haptics.light(); onStartSession?.invoke() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = FieldMindTheme.colors.project.copy(alpha = 0.12f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(FieldMindTheme.colors.project.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(FieldMindIcons.Bolt, null, tint = FieldMindTheme.colors.project, size = 28.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Start Research Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Timer-based multi-observation capture with GPS tracking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(FieldMindIcons.Forward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
                }
            }
        }

        // ── 2. Dashboard Metrics ──
        item { ResearchHubDashboard(totalObs, uniqueSites, openQuestions, supportedHypotheses, refutedHypotheses, untestedHypotheses, totalFieldHours, totalSessions) }

        // ── 3. New Project Button + Templates + Types ──
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onNavigate?.invoke(FieldMindScreen.NewProject) ?: run { showNewProject = !showNewProject; if (!showNewProject) { projTitle = ""; projDesc = ""; projQuestion = "" } } },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(FieldMindIcons.Add, null, size = 18.dp)
                    Spacer(Modifier.size(6.dp))
                    Text(if (showNewProject) "Cancel" else "Project", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = { showTemplates = !showTemplates; if (showTemplates) showTypes = false },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(FieldMindIcons.Project, null, size = 18.dp)
                    Spacer(Modifier.size(6.dp))
                    Text(if (showTemplates) "Hide" else "Templates", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = { showTypes = !showTypes; if (showTypes) showTemplates = false },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(FieldMindIcons.Category, null, size = 18.dp)
                    Spacer(Modifier.size(6.dp))
                    Text(if (showTypes) "Hide" else "Types", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // ── 7. Projects List ──
        if (totalProjects == 0) {
            item {
                EmptyState("No projects yet", "Start with a name and a question. Use templates or pick a project type to get started.", icon = FieldMindIcons.Project, actionLabel = "Create project") { onNavigate?.invoke(FieldMindScreen.NewProject) ?: run { showNewProject = true } }
            }
        } else {
            item { SectionHeader("Projects ($totalProjects)", "Tap any project to open the workspace") }
            items(projects) { project -> ProjectDashboardCardCompact(project, observations, questions, hypotheses, sources, data, reports, researchSessions) { onOpenDetail("project", project.id) } }
        }
    }
    // Dialogs outside LazyColumn
    if (showTypes) {
        OptionPickerDialog(
            title = "Project Types",
            subtitle = "Select a type to set the project category and field suggestions.",
            options = researchProjectTypes,
            selected = projType,
            onSelect = { type ->
                val preset = projectTemplates.firstOrNull { it.type == type }
                projType = type
                projTemplate = preset?.name ?: "Custom Blank Template"
                projTitle = ""
                // Only pre-fill category — keep all fields blank for user to fill
                projCategory = preset?.category ?: researchCategories.first()
                projDesc = ""
                projQuestion = ""
                projHypothesis = ""
                projTags = ""
                projPriority = "Medium"
                projMethods = setOf()
                showTypes = false
                showNewProject = true
            },
            onDismiss = { showTypes = false },
            accentColor = FieldMindTheme.colors.project,
            iconProvider = { FieldMindIcons.Project }
        )
    }
    if (showTemplates) {
        OptionPickerDialog(
            title = "Project Templates",
            subtitle = "Choose a template to use as a research guide for your project.",
            options = projectTemplates.map { it.name },
            selected = projTemplate,
            onSelect = { templateName ->
                val template = projectTemplates.firstOrNull { it.name == templateName }!!
                projTemplate = template.name
                projType = template.type
                projTitle = ""
                projCategory = template.category
                projDesc = ""
                projQuestion = ""
                projHypothesis = ""
                projTags = ""
                projMethods = setOf()
                showTemplates = false
                showNewProject = true
            },
            onDismiss = { showTemplates = false },
            accentColor = FieldMindTheme.colors.project,
            iconProvider = { name -> projectTemplates.firstOrNull { it.name == name }?.icon }
        )
    }
    if (showNewProject) {
        NewProjectScreen(viewModel = viewModel, onBack = { showNewProject = false })
    }
}

// ════════════════════════════════════════════════════════════════��═════
//  Research Hub Dashboard
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ResearchHubDashboard(
    totalObs: Int, uniqueSites: Int,
    openQuestions: Int, supportedHyp: Int, refutedHyp: Int, untestedHyp: Int,
    totalFieldHours: Double, totalSessions: Int
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(FieldMindIcons.Insights, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                Text("Research Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DashboardMetric(totalObs.toString(), "Total obs", FieldMindTheme.colors.observation)
                DashboardMetric(uniqueSites.toString(), "Sites", FieldMindTheme.colors.info)
                DashboardMetric(openQuestions.toString(), "Open Qs", FieldMindTheme.colors.question)
                DashboardMetric("%.1f".format(totalFieldHours), "Field hours", FieldMindTheme.colors.warning)
                DashboardMetric(totalSessions.toString(), "Sessions", FieldMindTheme.colors.data)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Hypotheses: $supportedHyp supported · $refutedHyp refuted · $untestedHyp untested",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DashboardMetric(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Templates Grid
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun TemplatesGrid(templates: List<ProjectTemplateDef>, onSelect: (ProjectTemplateDef) -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Templates (${templates.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Choose a template to pre-fill your project", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(templates) { template ->
                    Card(
                        modifier = Modifier.width(200.dp).clickable { onSelect(template) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(template.icon, null, tint = FieldMindTheme.colors.project, size = 28.dp)
                            Text(template.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(template.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                template.defaultMethods.take(3).forEach { method ->
                                    Text(method.take(12), style = MaterialTheme.typography.labelSmall, color = FieldMindTheme.colors.project, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Project Creation Form — Full spec with all fields
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResearchMethodBuilder(selected: Set<String>, onSelected: (Set<String>) -> Unit) {
    val options = listOf("Daily observations", "Weekly observations", "Photo documentation", "Audio recording",
        "Video documentation", "Measurement logging", "Species counting", "Weather logging",
        "Behavior logging", "Comparison table", "GPS tracking", "Water testing", "Camera trap")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option in selected,
                onClick = { onSelected(if (option in selected) selected - option else selected + option) },
                label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = if (option in selected) ({ Icon(FieldMindIcons.Check, null, size = 16.dp) }) else null
            )
        }
    }
}

@Composable
private fun ResearchPreviewCard(projectType: String, methods: Set<String>) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Auto workspace preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Type: $projectType", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Recommended data: ${recommendedDatasetFor(methods)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Recommended evidence: ${recommendedEvidenceFor(methods)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

private fun recommendedDatasetFor(methods: Set<String>): String = when {
    methods.any { "Species" in it } -> "Species Tracker"
    methods.any { "Measurement" in it } -> "Measurement Log"
    methods.any { "Weather" in it } -> "Weather Log"
    methods.any { "Comparison" in it } -> "Comparison Table"
    methods.any { "Camera" in it } -> "Event Log"
    else -> "Observation Timeline"
}

private fun recommendedEvidenceFor(methods: Set<String>): String = listOfNotNull(
    "photos".takeIf { methods.any { "Photo" in it } },
    "audio".takeIf { methods.any { "Audio" in it } },
    "video".takeIf { methods.any { "Video" in it } },
    "measurements".takeIf { methods.any { "Measurement" in it } },
    "GPS + weather".takeIf { methods.any { "Weather" in it || "Daily" in it || "GPS" in it } },
    "water data".takeIf { methods.any { "Water" in it } },
    "camera trap".takeIf { methods.any { "Camera" in it } }
).ifEmpty { listOf("notes", "observations") }.joinToString(", ")

// ══════════════════════════════════════════════════════════════════════
//  Project Card (preserved from original with expanded metrics)
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProjectDashboardCardCompact(
    project: ProjectEntity,
    observations: List<ObservationEntity>,
    questions: List<QuestionEntity>,
    hypotheses: List<HypothesisEntity>,
    sources: List<SourceEntity>,
    data: List<DataRecordEntity>,
    reports: List<ReportEntity>,
    researchSessions: List<ResearchSessionEntity>,
    onClick: () -> Unit
) {
    val relatedObs = observations.count { it.projectId == project.id }
    val relatedQs = questions.count { it.relatedProjectId == project.id }
    val relatedSources = sources.count { it.relatedProjectId == project.id }
    val relatedData = data.count { it.projectId == project.id }
    val relatedReports = reports.count { it.projectId == project.id }
    val relatedHypotheses = hypotheses.count { h -> questions.any { it.id == h.linkedQuestionId && it.relatedProjectId == project.id } }
    val projectSessions = researchSessions.count { it.projectId == project.id }
    val projectFieldHours = researchSessions.filter { it.projectId == project.id }.sumOf { it.totalDurationMs } / 3600_000.0
    val relatedObsRecent = observations.count { it.projectId == project.id && it.timestamp > System.currentTimeMillis() - 7 * 24 * 3600_000L }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(FieldMindTheme.colors.project.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(FieldMindIcons.Project, null, tint = FieldMindTheme.colors.project, size = 24.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(project.topicType, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("•", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$relatedObs obs", style = MaterialTheme.typography.labelMedium, color = FieldMindTheme.colors.observation)
                        if (relatedObsRecent > 0) {
                            Text("•", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$relatedObsRecent new", style = MaterialTheme.typography.labelMedium, color = FieldMindTheme.colors.positive)
                        }
                    }
                }
                InfoChip(project.status)
            }
            Text(project.objective.ifBlank { project.researchQuestion.ifBlank { "Open project workspace" } }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ProjectMetricChip(relatedObs, "obs", FieldMindTheme.colors.observation)
                ProjectMetricChip(relatedQs, "Qs", FieldMindTheme.colors.question)
                ProjectMetricChip(relatedSources, "src", FieldMindTheme.colors.source)
                ProjectMetricChip(relatedData, "data", FieldMindTheme.colors.data)
                ProjectMetricChip(relatedReports, "reports", FieldMindTheme.colors.report)
                ProjectMetricChip(projectSessions, "sessions", FieldMindTheme.colors.warning)
                if (projectFieldHours >= 0.5) ProjectMetricChip(ceil(projectFieldHours).toInt(), "hrs", FieldMindTheme.colors.project)
            }
        }
    }
}

@Composable
private fun ProjectMetricChip(value: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.clip(RoundedCornerShape(99.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("$value $label", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

// ── Remaining tabs (Observations, Hypotheses, Data, Reports) preserved from original ──

@Composable
internal fun AddButton(label: String, onClick: () -> Unit) {
    val haptics = rememberFieldMindHaptics()
    Button(onClick = { haptics.light(); onClick() }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Icon(icon = FieldMindIcons.Add, contentDescription = null, size = 20.dp); Spacer(Modifier.size(8.dp)); Text(label)
    }
}

internal fun panelPadding() = PaddingValues(20.dp, 4.dp, 20.dp, 96.dp)

// ── Tab 1: Observations ──

@Composable
private fun ObservationsTab(
    viewModel: FieldMindViewModel,
    observations: List<ObservationEntity>,
    projects: List<ProjectEntity>,
    onOpenDetail: (String, Long) -> Unit
) {
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var sortOption by rememberSaveable { mutableStateOf("Date (newest)") }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectMode by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    val projectOptions = remember(projects) { listOf(null to "All projects") + projects.map { it.id to it.title } }
    val filtered = remember(observations, selectedProjectId) {
        if (selectedProjectId == null) observations else observations.filter { it.projectId == selectedProjectId }
    }
    val sorted = remember(filtered, sortOption) {
        when (sortOption) {
            "Date (oldest)" -> filtered.sortedBy { it.timestamp }
            "Category" -> filtered.sortedBy { it.category }
            "Confidence" -> filtered.sortedByDescending { it.confidenceLevel }
            "Location" -> filtered.sortedBy { it.manualLocation }
            else -> filtered.sortedByDescending { it.timestamp } // Date (newest)
        }
    }

    // ── New observation dialog state ──
    var showNewObservation by remember { mutableStateOf(false) }
    val obsHaptics = rememberFieldMindHaptics()

    // If selectMode exits, deselect all
    if (!selectMode && selectedIds.isNotEmpty()) {
        selectedIds = emptySet()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = panelPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeader("Observations", "${filtered.size} of ${observations.size} total") }
        
        // ── Add observation button ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { obsHaptics.light(); showNewObservation = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FieldMindTheme.colors.observation.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(FieldMindTheme.colors.observation.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(FieldMindIcons.Add, null, tint = FieldMindTheme.colors.observation, size = 22.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Add observation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Quick-capture a new observation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(FieldMindIcons.Forward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
                }
            }
        }
        
        // ── Toolbar: Filter, Sort, Select, Delete ──
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Project filter chips
                    Text("Filter by project", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(projectOptions.take(12)) { (id, label) ->
                            FilterChip(selected = selectedProjectId == id, onClick = { selectedProjectId = id }, label = { Text(label, maxLines = 1) })
                        }
                    }
                    
                    // Action row: Sort, Select, Delete
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Sort button
                        Box {
                            FilledTonalButton(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(FieldMindIcons.Sort, null, size = 16.dp)
                                Spacer(Modifier.size(4.dp))
                                Text(sortOption.take(14), style = MaterialTheme.typography.labelSmall)
                                Spacer(Modifier.size(2.dp))
                                Icon(FieldMindIcons.Down, null, size = 14.dp)
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                listOf("Date (newest)", "Date (oldest)", "Category", "Confidence", "Location").forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s, style = MaterialTheme.typography.bodySmall) },
                                        onClick = { sortOption = s; showSortMenu = false },
                                        leadingIcon = if (sortOption == s) ({ Icon(FieldMindIcons.Check, null, size = 18.dp) }) else null
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.weight(1f))
                        
                        // Select mode toggle
                        FilledTonalButton(
                            onClick = { selectMode = !selectMode; if (!selectMode) selectedIds = emptySet() },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = if (selectMode) ButtonDefaults.filledTonalButtonColors(containerColor = FieldMindTheme.colors.observation.copy(alpha = 0.18f)) else ButtonDefaults.filledTonalButtonColors(),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(FieldMindIcons.Select, null, size = 16.dp)
                            Spacer(Modifier.size(4.dp))
                            Text(if (selectMode) "Cancel (${selectedIds.size})" else "Select", style = MaterialTheme.typography.labelSmall)
                        }
                        
                        // Delete selected (only visible in select mode with items)
                        if (selectMode && selectedIds.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { showDeleteConfirm = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(FieldMindIcons.Delete, null, size = 16.dp, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.size(4.dp))
                                Text("Delete (${selectedIds.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }
        }
        
        items(sorted) { obs ->
            EntityCard(
                title = obs.subject.ifBlank { "Observation" },
                kind = "observation",
                body = "${obs.category} • ${obs.date}",
                meta = listOfNotNull(obs.weatherCondition.takeIf { it.isNotBlank() }, obs.confidenceLevel),
                selected = selectMode && obs.id in selectedIds,
                onSelect = if (selectMode) {{ 
                    selectedIds = if (obs.id in selectedIds) selectedIds - obs.id else selectedIds + obs.id 
                }} else null,
                onClick = { 
                    if (selectMode) {
                        selectedIds = if (obs.id in selectedIds) selectedIds - obs.id else selectedIds + obs.id 
                    } else {
                        onOpenDetail("observation", obs.id)
                    }
                }
            )
        }
        if (sorted.isEmpty()) {
            item { EmptyState("No observations", "Observations will appear here, filtered by project.", icon = FieldMindIcons.Observation, actionLabel = "Start observing") { onOpenDetail("observe", 0) } }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(icon = FieldMindIcons.Delete, contentDescription = null, size = 28.dp) },
            title = { Text("Delete observations?") },
            text = {
                Text(
                    "This will permanently delete ${selectedIds.size} selected observation${if (selectedIds.size != 1) "s" else ""}. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedIds.forEach { viewModel.deleteObservation(it) }
                        selectedIds = emptySet()
                        selectMode = false
                        showDeleteConfirm = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete ${selectedIds.size}") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // ── New observation dialog ──
    if (showNewObservation) {
        NewQuickObservationDialog(
            viewModel = viewModel,
            projectId = selectedProjectId,
            onDismiss = { showNewObservation = false }
        )
    }
}

@Composable
private fun NewQuickObservationDialog(
    viewModel: FieldMindViewModel,
    projectId: Long?,
    onDismiss: () -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var facts by remember { mutableStateOf("") }
    var confidence by remember { mutableStateOf("Sure") }
    val haptics = rememberFieldMindHaptics()

    fun save() {
        if (subject.isBlank()) return
        haptics.confirm()
        viewModel.addObservation(
            subject = subject,
            category = category.ifBlank { "Other" },
            facts = facts,
            confidence = confidence,
            manualLocation = "",
            tags = "",
            evidence = "",
            context = "",
            projectId = projectId
        ) {
            onDismiss()
        }
    }

    DialogWrapper(onDismiss = onDismiss) {
        DialogHeader(FieldMindIcons.Observation, "Quick Observation", "Capture a fact from the field.", accent = FieldMindTheme.colors.observation)
        FieldTextField(subject, { subject = it }, "Subject", required = true, supportingText = "e.g. Red-tailed hawk, Maple leaf color change, Creek water level")
        FieldTextField(category, { category = it }, "Category", supportingText = "e.g. Bird, Plant, Weather, Water, Insect, Mammal, Other")
        FieldTextField(facts, { facts = it }, "Facts / notes", minLines = 3, supportingText = "What did you observe? Describe factually.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Confidence", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceChips(listOf("Unsure", "Likely", "Sure"), confidence) { confidence = it }
        }
        if (projectId != null) {
            InfoChip("Linked to project", icon = FieldMindIcons.Project)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.weight(1f))
            Button(onClick = ::save, shape = RoundedCornerShape(16.dp), enabled = subject.isNotBlank()) { Text("Save") }
        }
    }
}

// ── Hypothesis Form ──

@Composable
private fun HypothesesTab(
    viewModel: FieldMindViewModel,
    hypotheses: List<HypothesisEntity>,
    questions: List<QuestionEntity>,
    observations: List<ObservationEntity>,
    onOpenDetail: (String, Long) -> Unit,
    onNavigate: ((FieldMindScreen) -> Unit)? = null
) {
    var showNewHypDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = panelPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeader("Hypotheses", "${hypotheses.size} predictions • ${hypotheses.count { it.resultStatus == "Supported" }} supported, ${hypotheses.count { it.resultStatus == "Refuted" }} refuted") }
        item {
            AddButton("New hypothesis") {
                if (onNavigate != null) {
                    onNavigate?.invoke(FieldMindScreen.NewHypothesis)
                } else {
                    showNewHypDialog = true
                }
            }
        }
        if (hypotheses.isEmpty()) item { EmptyState("No hypotheses yet", "Predict what evidence would show, then test it.", icon = FieldMindIcons.Hypothesis) }
        items(hypotheses.sortedByDescending { it.createdAt }) { h ->
            val supportColor = when (h.resultStatus.lowercase()) {
                "supported" -> FieldMindTheme.colors.positive
                "refuted" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            EntityCard(h.prediction, "hypothesis", body = h.reasoning.take(120), meta = listOf("${h.confidencePercent}%", h.resultStatus), onClick = { onOpenDetail("hypothesis", h.id) })
        }
    }
    // Inline dialog outside LazyColumn
    if (showNewHypDialog) {
        NewHypothesisScreen(viewModel = viewModel, onBack = { showNewHypDialog = false })
    }
}

// ── Tab 3: Data ──

@Composable
private fun DataTab(
    viewModel: FieldMindViewModel,
    data: List<DataRecordEntity>,
    reports: List<ReportEntity>,
    observations: List<ObservationEntity>,
    projects: List<ProjectEntity>,
    onOpenDetail: (String, Long) -> Unit,
    onNavigate: ((FieldMindScreen) -> Unit)? = null
) {
    var showNewDataDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = panelPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeader("Data Records", "${data.size} records across ${data.map { it.datasetKind }.distinct().size} datasets") }
        item { TrackingFlowCards() }
        item {
            AddButton("Add data record") {
                if (onNavigate != null) {
                    onNavigate?.invoke(FieldMindScreen.NewDataRecord)
                } else {
                    showNewDataDialog = true
                }
            }
        }
        if (data.isEmpty()) item { EmptyState("No data records yet", "Measure, count, compare, or log with offline tools.", icon = FieldMindIcons.Data) }
        items(data.sortedByDescending { it.timestamp }) { d -> EntityCard(d.label, "data", body = "${d.value} ${d.unit}", meta = listOf(d.toolType, d.datasetKind), onClick = { onOpenDetail("data", d.id) }) }
    }
    // Inline dialog outside LazyColumn
    if (showNewDataDialog) {
        NewDataRecordScreen(viewModel = viewModel, onBack = { showNewDataDialog = false })
    }
}

// ── Tab 4: Reports ──

@Composable
private fun ReportsTab(
    viewModel: FieldMindViewModel,
    reports: List<ReportEntity>,
    projects: List<ProjectEntity>,
    onOpenDetail: (String, Long) -> Unit,
    onNavigate: ((FieldMindScreen) -> Unit)? = null
) {
    var showNewReportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = panelPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeader("Reports", "${reports.size} reports • ${reports.count { it.status == "Published" }} published") }
        item {
            AddButton("Build report") {
                if (onNavigate != null) {
                    onNavigate?.invoke(FieldMindScreen.NewReport)
                } else {
                    showNewReportDialog = true
                }
            }
        }
        if (reports.isEmpty()) item { EmptyState("No reports yet", "Write up your findings with background, methods, results, and conclusions.", icon = FieldMindIcons.Report) }
        items(reports.sortedByDescending { it.createdAt }) { r -> EntityCard(r.title, "report", body = r.conclusion.ifBlank { r.question }, meta = listOf(r.type, r.status), onClick = { onOpenDetail("report", r.id) }) }
    }
    // Inline dialog outside LazyColumn
    if (showNewReportDialog) {
        NewReportScreen(viewModel = viewModel, onBack = { showNewReportDialog = false })
    }
}

// ── Tracking Flow Cards ──

@Composable
private fun TrackingFlowCards() {
    val modes = listOf(
        "Count things" to "Creates a Counter dataset with bar charts.",
        "Measure something" to "Creates a Measurement Log with trend charts.",
        "Compare locations" to "Creates a Comparison Table with grouped charts.",
        "Track changes over time" to "Creates a Measurement Log with trend charts.",
        "Record weather" to "Creates a Weather Log linked to observations.",
        "Track species" to "Creates a Species Tracker with evidence requirements."
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(modes) { (title, body) ->
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(Modifier.width(180.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(FieldMindIcons.Data, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
