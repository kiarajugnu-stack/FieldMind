package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.database.entity.*
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════
//  Project Types & Templates — preserved for NewProjectScreen
// ══════════════════════════════════════════════════════════════════════

internal val researchProjectTypes = listOf(
    "Species Survey", "Population Census", "Habitat Assessment",
    "Wildlife Monitoring", "Behavioral Study", "Migration Study",
    "Biodiversity Inventory", "Vegetation Survey", "Conservation Project",
    "Ecological Research", "Camera Trap Study", "Acoustic Monitoring",
    "Pollinator Survey", "Water Quality Study", "Citizen Science Project",
    "Environmental Impact Study", "Long Term Monitoring", "Species Discovery",
    "Custom Research Project"
)

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

internal val researchCategories = listOf(
    "Ornithology", "Mammalogy", "Herpetology", "Entomology",
    "Botany", "Ecology", "Conservation", "Climate Science",
    "Hydrology", "Geology", "Oceanography", "Citizen Science",
    "Behavioral Ecology", "Evolution", "Taxonomy", "Other"
)

// ══════════════════════════════════════════════════════════════════════
//  Projects Screen — Simplified per HTML spec
// ══════════════════════════════════════════════════════════════════════

@Composable
fun ProjectsScreen(
    viewModel: FieldMindViewModel,
    onOpenDetail: (String, Long) -> Unit = { _, _ -> },
    startTab: Int = 0,
    onStartSession: (() -> Unit)? = null,
    onNavigate: ((FieldMindScreen) -> Unit)? = null
) {
    val projects by viewModel.projects.collectAsState()
    val observations by viewModel.observations.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val colors = FieldMindTheme.colors
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }
    var sortOption by remember { mutableStateOf("Updated") }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val filterOptions = listOf("All", "Active", "Archived", "Not Synced")

    // Compute stats
    val totalCount = projects.size
    val activeCount = projects.count { it.status == "Active" }
    val archivedCount = projects.count { it.status == "Archived" }
    val notSyncedCount = projects.count { it.status == "Not Synced" }

    // Filter and sort projects
    val filteredProjects = remember(projects, selectedFilter, searchQuery, sortOption) {
        var result = projects

        // Apply filter
        when (selectedFilter) {
            "Active" -> result = result.filter { it.status == "Active" }
            "Archived" -> result = result.filter { it.status == "Archived" }
            "Not Synced" -> result = result.filter { it.status == "Not Synced" }
        }

        // Apply search
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                it.topicType.lowercase().contains(q) ||
                it.objective.lowercase().contains(q)
            }
        }

        // Apply sort
        when (sortOption) {
            "Name" -> result.sortedBy { it.title.lowercase() }
            "Created" -> result.sortedByDescending { it.createdAt }
            "Records" -> result.sortedByDescending { p ->
                observations.count { it.projectId == p.id } +
                notes.count { it.projectId == p.id }
            }
            else -> result.sortedByDescending { it.updatedAt } // Updated
        }
    }

    // ── Helper to format relative time ──
    fun relativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / 60_000
        val hours = diff / 3600_000
        val days = diff / 86_400_000
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            days < 30 -> "${days / 7}w ago"
            days < 365 -> "${days / 30}mo ago"
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Header: Projects with StandardScreenHeader ──
        item {
            StandardScreenHeader(
                title = "Projects",
                subtitle = "${projects.size} total · Manage your research projects",
                icon = FieldMindIcons.Projects,
                heroColor = colors.project,
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalIconButton(
                            onClick = { showSearch = !showSearch },
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(if (showSearch) MaterialSymbolIcon("close") else FieldMindIcons.Search, null, size = 20.dp)
                        }
                        FilledTonalButton(
                            onClick = { onNavigate?.invoke(FieldMindScreen.NewProject) },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.project.copy(alpha = 0.16f))
                        ) {
                            Icon(FieldMindIcons.Add, null, size = 18.dp, tint = colors.project)
                            Spacer(Modifier.size(6.dp))
                            Text("New", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            )
        }

        // ── Search ──
        if (showSearch) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search projects...") },
                    leadingIcon = { Icon(FieldMindIcons.Search, null, size = 20.dp) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(MaterialSymbolIcon("close"), "Clear", size = 18.dp)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.project.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }

        // ── Filter chips ──
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filterOptions.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        onClick = { selectedFilter = filter; searchQuery = "" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.project.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            filter,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.project else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // ── Stats row: Total | Active | Archive | Not Synced ──
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatChip("${totalCount}", "Total", colors.project)
                    StatChip("${activeCount}", "Active", colors.positive)
                    StatChip("${archivedCount}", "Archived", MaterialTheme.colorScheme.onSurfaceVariant)
                    StatChip("${notSyncedCount}", "Not Synced", colors.warning)
                }
            }
        }

        // ── Section header: My Projects + Sort ──
        if (filteredProjects.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "My Projects",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Sort button
                    Box {
                        Surface(
                            onClick = { showSortMenu = true },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 0.dp
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(FieldMindIcons.Sort, null, size = 16.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Sort: $sortOption", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Icon(FieldMindIcons.Down, null, size = 14.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            listOf("Updated", "Name", "Created", "Records").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, style = MaterialTheme.typography.bodySmall) },
                                    onClick = { sortOption = option; showSortMenu = false },
                                    leadingIcon = if (sortOption == option) ({ Icon(FieldMindIcons.Check, null, size = 18.dp) }) else null
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Projects list ──
        if (filteredProjects.isEmpty()) {
            // Empty state per HTML spec
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(40.dp, 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            Modifier.size(72.dp).clip(RoundedCornerShape(20.dp))
                                .background(colors.project.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(FieldMindIcons.Project, null, tint = colors.project, size = 36.dp)
                        }
                        Text(
                            if (searchQuery.isNotBlank() || selectedFilter != "All") "No projects match" else "No Projects Yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (searchQuery.isNotBlank() || selectedFilter != "All")
                                "Try adjusting your search or filter"
                            else
                                "Create your first project to start collecting observations, notes, questions and sources.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        if (searchQuery.isBlank() && selectedFilter == "All") {
                            FilledTonalButton(
                                onClick = { onNavigate?.invoke(FieldMindScreen.NewProject) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.project.copy(alpha = 0.16f))
                            ) {
                                Icon(FieldMindIcons.Add, null, size = 18.dp)
                                Spacer(Modifier.size(6.dp))
                                Text("Create Project")
                            }
                        } else {
                            TextButton(onClick = { selectedFilter = "All"; searchQuery = "" }) {
                                Text("Clear filters")
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredProjects, key = { it.id }) { project ->
                ProjectCard(
                    project = project,
                    recordCount = observations.count { it.projectId == project.id } +
                        notes.count { it.projectId == project.id } +
                        questions.count { it.relatedProjectId == project.id } +
                        sources.count { it.relatedProjectId == project.id },
                    relativeTime = relativeTime(project.updatedAt),
                    viewModel = viewModel,
                    showSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                    onClick = { onOpenDetail("project", project.id) }
                )
            }
        }
    }

    // ── Snackbar overlay (inside outer Box, outside LazyColumn) ──
    Box(Modifier.align(Alignment.BottomCenter).padding(16.dp).padding(bottom = 80.dp)) {
        SnackbarHost(hostState = snackbarHostState)
    }
}
}

// ══════════════════════════════════════════════════════════════════════
//  Project Card — Per HTML spec: icon, name, description, records, updated, status, menu
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun ProjectCard(
    project: ProjectEntity,
    recordCount: Int,
    relativeTime: String,
    viewModel: FieldMindViewModel,
    showSnackbar: (String) -> Unit = {},
    onClick: () -> Unit
) {
    val colors = FieldMindTheme.colors
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var renameText by remember(project.id) { mutableStateOf(project.title) }

    Card(
        modifier = Modifier.fillMaxWidth().expressivePress(scaleDown = 0.98f).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Project icon
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(colors.project.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(FieldMindIcons.Project, null, tint = colors.project, size = 26.dp)
            }

            // Content
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Name + status badge
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        project.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    StatusBadge(project.status)
                }

                // Description
                if (project.objective.isNotBlank()) {
                    Text(
                        project.objective,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Metadata row: record count + updated time
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "$recordCount records",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.project,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        relativeTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Three-dot menu
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        MaterialSymbolIcon("more_vert"),
                        "Project menu",
                        size = 20.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { showMenu = false; showRenameDialog = true },
                        leadingIcon = { Icon(MaterialSymbolIcon("edit"), null, size = 18.dp) }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = {
                            showMenu = false
                            viewModel.addProject(
                                title = "${project.title} (Copy)",
                                topicType = project.topicType,
                                objective = project.objective,
                                researchQuestion = project.researchQuestion,
                                methods = project.methods,
                                futureQuestions = project.futureQuestions,
                                backgroundNotes = project.backgroundNotes,
                                hypothesisSummary = project.hypothesisSummary,
                                dataSummary = project.dataSummary,
                                analysis = project.analysis,
                                conclusion = project.conclusion,
                                projectType = project.projectType ?: "Observation",
                                selectedMethods = project.selectedMethods ?: "",
                                connectionMap = project.connectionMap ?: ""
                            )
                            showSnackbar("${project.title} duplicated")
                        },
                        leadingIcon = { Icon(MaterialSymbolIcon("content_copy"), null, size = 18.dp) }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            showMenu = false
                            val shareText = buildString {
                                appendLine("📁 ${project.title}")
                                if (project.objective.isNotBlank()) appendLine(project.objective)
                                appendLine()
                                appendLine("Type: ${project.topicType}")
                                appendLine("Status: ${project.status}")
                                appendLine("Records: $recordCount")
                                appendLine("Updated: $relativeTime")
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Project: ${project.title}")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Project"))
                        },
                        leadingIcon = { Icon(MaterialSymbolIcon("share"), null, size = 18.dp) }
                    )
                    DropdownMenuItem(
                        text = { Text("Export") },
                        onClick = {
                            showMenu = false
                            val exportText = buildString {
                                appendLine("# ${project.title}")
                                appendLine()
                                appendLine("**Topic:** ${project.topicType}")
                                appendLine("**Status:** ${project.status}")
                                appendLine("**Created:** ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(project.createdAt))}")
                                appendLine("**Records:** $recordCount")
                                if (project.objective.isNotBlank()) appendLine("\n## Objective\n${project.objective}")
                                if (project.researchQuestion.isNotBlank()) appendLine("\n## Research Question\n${project.researchQuestion}")
                                if (project.methods.isNotBlank()) appendLine("\n## Methods\n${project.methods}")
                                if (project.conclusion.isNotBlank()) appendLine("\n## Conclusion\n${project.conclusion}")
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/markdown"
                                putExtra(Intent.EXTRA_SUBJECT, "${project.title} — FieldMind Export")
                                putExtra(Intent.EXTRA_TEXT, exportText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export Project As"))
                        },
                        leadingIcon = { Icon(MaterialSymbolIcon("file_download"), null, size = 18.dp) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (project.status == "Archived") "Unarchive" else "Archive",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            showMenu = false
                            viewModel.updateProjectEntity(
                                project.copy(
                                    status = if (project.status == "Archived") "Active" else "Archived",
                                    archivedAt = if (project.status == "Archived") null else System.currentTimeMillis()
                                )
                            )
                            showSnackbar(
                                if (project.status == "Archived") "${project.title} restored"
                                else "${project.title} archived"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                MaterialSymbolIcon("archive"),
                                null,
                                size = 18.dp,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; showDeleteConfirm = true },
                        leadingIcon = { Icon(MaterialSymbolIcon("delete"), null, size = 18.dp, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }

    // ── Rename Dialog ──
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            icon = { Icon(FieldMindIcons.Edit, null, size = 28.dp) },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Project name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.updateProjectEntity(project.copy(title = renameText.trim()))
                        }
                        showRenameDialog = false
                    },
                    enabled = renameText.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Delete Confirmation Dialog ──
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(MaterialSymbolIcon("delete_forever"), null, size = 28.dp, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Project?") },
            text = {
                Text(
                    "Are you sure you want to delete \"${project.title}\"? This action cannot be undone. All observations, notes, questions, and sources linked to this project will also be removed.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(project.id)
                        showDeleteConfirm = false
                        showSnackbar("${project.title} deleted")
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Shared composables
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun StatChip(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


