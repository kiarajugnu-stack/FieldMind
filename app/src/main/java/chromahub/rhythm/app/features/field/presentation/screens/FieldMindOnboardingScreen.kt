package fieldmind.research.app.features.field.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import fieldmind.research.app.features.field.data.settings.*
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.FieldMindMotion
import fieldmind.research.app.features.field.presentation.components.expressivePress
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

// ══════════════════════════════════════════════════════════════════════
//  Onboarding Entry Point
// ══════════════════════════════════════════════════════════════════════

/**
 * Multi-screen onboarding wizard with animated M3 Expressive design.
 *
 * Flow:
 *   [1. Welcome & Identity] → [2. Fields of Interest] → [3. Permissions]
 *   → [4. Theme & Quick Settings] → [5. Review & Done]
 *   → [Continue Tour → Screens 6-9] or [Finish Tour → dialog]
 */
@Composable
fun FieldMindOnboardingScreen(
    settings: FieldMindSettings,
    onFinish: () -> Unit
) {
    // ── Shared onboarding state ──
    var currentPage by remember { mutableIntStateOf(0) }
    val totalCorePages = 5

    var profileName by remember { mutableStateOf("") }
    var profileRole by remember { mutableStateOf("Field learner") }
    var interests by remember { mutableStateOf(UserInterests()) }

    // Permission states
    var cameraGranted by remember { mutableStateOf(false) }
    var locationGranted by remember { mutableStateOf(false) }
    var audioGranted by remember { mutableStateOf(false) }
    var notificationGranted by remember { mutableStateOf(false) }

    var selectedTheme by remember { mutableStateOf("System") }
    var useDynamicColors by remember { mutableStateOf(true) }
    var tempUnit by remember { mutableStateOf("Celsius") }
    var distanceUnit by remember { mutableStateOf("km") }
    var timeFormat by remember { mutableStateOf("24h") }
    var dailyGoal by remember { mutableIntStateOf(1) }

    var showFinishDialog by remember { mutableStateOf(false) }
    var showContinueTour by remember { mutableStateOf(false) }
    var showExtendedTour by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Permission launchers
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        locationGranted = result.values.any { it }
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        audioGranted = granted
    }
    val notificationLauncher = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationGranted = granted
        }
    } else null

    // Check currently granted permissions at start
    LaunchedEffect(Unit) {
        cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            notificationGranted = true // Pre-13 auto-granted
        }
    }

    fun finishOnboarding() {
        // Save all settings
        settings.setProfileName(profileName)
        settings.setProfileRole(profileRole)
        settings.setProfileFocus(
            buildList {
                if (interests.zoology.isNotEmpty()) add("Zoology: ${interests.zoology.joinToString { it.displayName }}")
                if (interests.botany.isNotEmpty()) add("Botany: ${interests.botany.joinToString { it.displayName }}")
                if (interests.ecologyEnvironment) add("Ecology & Environment")
                if (interests.astronomy) add("Astronomy")
                if (interests.geology) add("Geology")
                interests.customInterests.forEach { add(it) }
            }.joinToString(", ").ifEmpty { "Wildlife & ecology" }
        )
        settings.setUserInterests(interests)
        settings.setThemeMode(selectedTheme)
        settings.setDynamicColorEnabled(useDynamicColors)
        settings.setTempUnit(tempUnit)
        settings.setDistanceUnit(distanceUnit)
        settings.setTimeFormat(timeFormat)
        settings.setDailyObservationGoal(dailyGoal)
        settings.setMediaAttachmentsEnabled(cameraGranted)
        settings.setAudioRecordingEnabled(audioGranted)
        onFinish()
    }

    BackHandler(enabled = currentPage > 0) {
        currentPage--
    }

    // Derive dark theme from current selection so the UI reflects changes immediately
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (selectedTheme) {
        "Dark" -> true
        "Light" -> false
        else -> systemDark
    }

    FieldMindTheme(darkTheme = isDarkTheme, dynamicColor = useDynamicColors) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Animated page content with sliding transitions
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(
                    animationSpec = tween(400, easing = FastOutSlowInEasing),
                    initialOffsetX = { direction * it / 3 }
                ) + fadeIn(tween(300)))
                    .togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(300),
                            targetOffsetX = { -direction * it / 4 }
                        ) + fadeOut(tween(200))
                    )
            },
            label = "onboardingPage"
        ) { page ->
            when (page) {
                0 -> OnboardingWelcomePage(
                    name = profileName,
                    onNameChange = { profileName = it },
                    role = profileRole,
                    onRoleChange = { profileRole = it },
                    onNext = { currentPage = 1 }
                )
                1 -> OnboardingInterestsPage(
                    interests = interests,
                    onInterestsChange = { interests = it },
                    onNext = { currentPage = 2 },
                    onBack = { currentPage = 0 }
                )
                2 -> OnboardingPermissionsPage(
                    cameraGranted = cameraGranted,
                    locationGranted = locationGranted,
                    audioGranted = audioGranted,
                    notificationGranted = notificationGranted,
                    onRequestCamera = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                    onRequestLocation = { locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                    onRequestAudio = { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onRequestNotification = {
                        if (notificationLauncher != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onNext = { currentPage = 3 },
                    onBack = { currentPage = 1 }
                )
                3 -> OnboardingThemePage(
                    selectedTheme = selectedTheme,
                    useDynamicColors = useDynamicColors,
                    tempUnit = tempUnit,
                    distanceUnit = distanceUnit,
                    timeFormat = timeFormat,
                    dailyGoal = dailyGoal,
                    onThemeChange = { selectedTheme = it },
                    onDynamicColorsChange = { useDynamicColors = it },
                    onTempUnitChange = { tempUnit = it },
                    onDistanceUnitChange = { distanceUnit = it },
                    onTimeFormatChange = { timeFormat = it },
                    onDailyGoalChange = { dailyGoal = it },
                    onNext = { currentPage = 4 },
                    onBack = { currentPage = 2 }
                )
                4 -> OnboardingReviewPage(
                    profileName = profileName,
                    profileRole = profileRole,
                    interests = interests,
                    cameraGranted = cameraGranted,
                    locationGranted = locationGranted,
                    audioGranted = audioGranted,
                    selectedTheme = selectedTheme,
                    dailyGoal = dailyGoal,
                    onFinish = { showFinishDialog = true },
                    onContinueTour = { showExtendedTour = true },
                    onBack = { currentPage = 3 },
                    onEditPage = { currentPage = it }
                )
                // Extended tour pages (6-9)
                5 -> if (showExtendedTour) OnboardingScreenVisibilityPage(
                    visibility = ScreenVisibility.fromInterests(interests),
                    interests = interests,
                    onApply = { vis ->
                        settings.setScreenVisibility(vis)
                        currentPage = 6
                    },
                    onSkip = { currentPage = 6 },
                    onBack = { currentPage = 4 }
                )
                6 -> OnboardingAiFeaturesPage(
                    onNext = { currentPage = 7 },
                    onBack = { currentPage = 5 }
                )
                7 -> OnboardingBackupPage(
                    onNext = { currentPage = 8 },
                    onBack = { currentPage = 6 }
                )
                8 -> OnboardingDataToolsPage(
                    onFinish = { finishOnboarding() },
                    onBack = { currentPage = 7 }
                )
            }
        }

        // ── Finish Tour Dialog ──
        if (showFinishDialog) {
            AlertDialog(
                onDismissRequest = { showFinishDialog = false },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(FieldMindIcons.Check, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                        }
                        Text("You're all set!", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "You can access additional setup and preferences anytime from Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider()
                        Text(
                            "From Settings you can customize:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        val laterItems = listOf(
                            "Screen visibility — show/hide any screen",
                            "AI Assistant — Gemini or OpenAI integration",
                            "Backup and auto-backup schedules",
                            "Privacy lock with biometrics",
                            "Reminders and daily streak settings",
                            "Units, date format, map preferences"
                        )
                        laterItems.forEach { item ->
                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(item, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showFinishDialog = false; finishOnboarding() },
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Start exploring") }
                },
                dismissButton = {                        TextButton(onClick = {
                            showFinishDialog = false
                            showExtendedTour = true
                        }) {
                            Text("Continue tour")
                        }
                }
            )
        }
    }

    }
    // END FieldMindTheme wrapper

    // Navigate to extended tour from review page
    LaunchedEffect(showExtendedTour, showContinueTour) {
        if (showExtendedTour) {
            showExtendedTour = false
            showContinueTour = false
            currentPage = 5
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Page Indicator Dots
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun PageIndicator(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (i == current) 24.dp else 8.dp)
                    .graphicsLayer {
                        val animProgress = if (i <= current) 1f else 0f
                        alpha = if (i == current) 1f else 0.4f
                    }
                    .background(
                        if (i <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Screen 1: Welcome & Identity
// ══════════════════════════════════════════════════════════════════════

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun OnboardingWelcomePage(
    name: String,
    onNameChange: (String) -> Unit,
    role: String,
    onRoleChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val roles = listOf("Field learner", "Citizen scientist", "Biology student", "Educator", "Professional researcher", "Hobbyist naturalist", "Conservationist")
    var roleExpanded by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showContent = true
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Decorative gradient orb
        Box(
            Modifier
                .size(280.dp)
                .offset(x = 60.dp, y = (-80).dp)
                .graphicsLayer { alpha = 0.08f }
                .background(
                    Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary, Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier
                .size(200.dp)
                .offset(x = (-40).dp, y = 100.dp)
                .graphicsLayer { alpha = 0.06f }
                .background(
                    Brush.radialGradient(listOf(MaterialTheme.colorScheme.tertiary, Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Logo + Tagline
            Column(
                Modifier.weight(1f).padding(top = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(visible = showContent, enter = fadeIn(tween(400, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.8f, animationSpec = tween(400, easing = FastOutSlowInEasing))) {
                    Box(
                        Modifier.size(72.dp).background(
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)),
                            RoundedCornerShape(24.dp)
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(FieldMindIcons.Nature, null, tint = MaterialTheme.colorScheme.onPrimary, size = 40.dp)
                    }
                }

                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(FieldMindMotion.expressiveFloat) + slideInVertically { it / 3 }
                ) {
                    Text(
                        "Welcome to\nFieldMind",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 40.sp)
                    )
                }

                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(FieldMindMotion.expressiveFloat) + slideInVertically { it / 4 }
                ) {
                    Text(
                        "Your personal field research companion — observe, question, research, and learn. Everything stays on your device.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Name field
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(FieldMindMotion.expressiveFloat) + slideInVertically { it / 4 }
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Your name (optional)") },
                        placeholder = { Text("e.g. Alex") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        leadingIcon = { Icon(icon = FieldMindIcons.User, contentDescription = null, size = 20.dp) }
                    )
                }

                // Role dropdown
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(FieldMindMotion.expressiveFloat) + slideInVertically { it / 5 }
                ) {
                    ExposedDropdownMenuBox(
                        expanded = roleExpanded,
                        onExpandedChange = { roleExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = role,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("I am a…") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(18.dp),
                            leadingIcon = { Icon(icon = FieldMindIcons.School, contentDescription = null, size = 20.dp) }
                        )
                        ExposedDropdownMenu(
                            expanded = roleExpanded,
                            onDismissRequest = { roleExpanded = false }
                        ) {
                            roles.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { onRoleChange(option); roleExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom: Page indicator + Next
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                PageIndicator(current = 0, total = 5)
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth().height(54.dp).expressivePress(scaleDown = 0.96f),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Get started", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.size(8.dp))
                    Icon(FieldMindIcons.Forward, null, size = 20.dp)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Screen 2: Fields of Interest
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun OnboardingInterestsPage(
    interests: UserInterests,
    onInterestsChange: (UserInterests) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var customInput by remember { mutableStateOf("") }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { showContent = true }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                AnimatedVisibility(visible = showContent, enter = fadeIn() + slideInVertically { it / 3 }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("What do you study?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text("Select all that apply. We'll tailor the app to match your fields.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // ── Zoology ──
                AnimatedVisibility(visible = showContent, enter = fadeIn(FieldMindMotion.expressiveFloat)) {
                    InterestCategorySection(
                        title = "Zoology",
                        icon = FieldMindIcons.Nature,
                        accent = Color(0xFF43A047),
                        options = ZoologySubfield.all().toList(),
                        selectedOptions = interests.zoology.toList(),
                        onToggle = { field ->
                            val current = interests.zoology.toMutableSet()
                            if (field in current) current.remove(field) else current.add(field)
                            onInterestsChange(interests.copy(zoology = current))
                        }
                    )
                }

                // ── Botany ──
                AnimatedVisibility(visible = showContent, enter = fadeIn(FieldMindMotion.expressiveFloat)) {
                    InterestCategorySection(
                        title = "Botany",
                        icon = FieldMindIcons.Nature,
                        accent = Color(0xFF8BC34A),
                        options = BotanySubfield.all().toList(),
                        selectedOptions = interests.botany.toList(),
                        onToggle = { field ->
                            val current = interests.botany.toMutableSet()
                            if (field in current) current.remove(field) else current.add(field)
                            onInterestsChange(interests.copy(botany = current))
                        }
                    )
                }

                // ── Other fields ──
                AnimatedVisibility(visible = showContent, enter = fadeIn(FieldMindMotion.expressiveFloat)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Other fields", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val otherFields = listOf(
                            Triple("Ecology & Environment", FieldMindIcons.Weather, Color(0xFFFB8C00)),
                            Triple("Astronomy", FieldMindIcons.MoonFull, Color(0xFF7B1FA2)),
                            Triple("Geology", FieldMindIcons.Rock, Color(0xFF6D4C41))
                        )
                        otherFields.forEach { (label, icon, accent) ->
                            val isSelected = when (label) {
                                "Ecology & Environment" -> interests.ecologyEnvironment
                                "Astronomy" -> interests.astronomy
                                "Geology" -> interests.geology
                                else -> false
                            }
                            val bgColor = if (isSelected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh
                            val borderCol = if (isSelected) accent.copy(alpha = 0.5f) else Color.Transparent
                            Surface(
                                onClick = {
                                    onInterestsChange(
                                        when (label) {
                                            "Ecology & Environment" -> interests.copy(ecologyEnvironment = !isSelected)
                                            "Astronomy" -> interests.copy(astronomy = !isSelected)
                                            "Geology" -> interests.copy(geology = !isSelected)
                                            else -> interests
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = bgColor,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, borderCol) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(if (isSelected) 0.22f else 0.1f)), contentAlignment = Alignment.Center) {
                                        Icon(icon, null, tint = accent, size = 20.dp)
                                    }
                                    Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    if (isSelected) {
                                        Icon(FieldMindIcons.Check, null, tint = accent, size = 22.dp)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Custom interests ──
                AnimatedVisibility(visible = showContent, enter = fadeIn(FieldMindMotion.expressiveFloat)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Custom interests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = customInput,
                            onValueChange = { customInput = it },
                            label = { Text("Add custom field") },
                            placeholder = { Text("e.g. Mycology, Entomology, Hydrology…") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (customInput.isNotBlank()) {
                                    val updated = interests.customInterests + customInput.trim()
                                    onInterestsChange(interests.copy(customInterests = updated))
                                    customInput = ""
                                }
                            })
                        )
                        // Show added custom interests
                        if (interests.customInterests.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                interests.customInterests.forEach { item ->
                                    InputChip(
                                        selected = true,
                                        onClick = {
                                            onInterestsChange(interests.copy(customInterests = interests.customInterests - item))
                                        },
                                        label = { Text(item, style = MaterialTheme.typography.labelMedium) },
                                        trailingIcon = { Icon(FieldMindIcons.Close, null, size = 16.dp) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom navigation
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.expressivePress(scaleDown = 0.96f)
                ) { Text("Back") }
                PageIndicator(current = 1, total = 5, modifier = Modifier.weight(1f))
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(48.dp).expressivePress(scaleDown = 0.96f)
                ) { Text("Continue") }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun <T> InterestCategorySection(
    title: String,
    icon: MaterialSymbolIcon,
    accent: Color,
    options: List<T>,
    selectedOptions: List<T>,
    onToggle: (T) -> Unit
) where T : Enum<T> {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = accent, size = 20.dp)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { field ->
                val isSelected = field in selectedOptions
                val fieldName = (field as? ZoologySubfield)?.displayName ?: (field as? BotanySubfield)?.displayName ?: field.name
                Surface(
                    onClick = { onToggle(field) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, accent.copy(alpha = 0.4f)) else null
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (isSelected) FieldMindIcons.Check else FieldMindIcons.Add,
                            null,
                            tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 16.dp
                        )
                        Text(fieldName, style = MaterialTheme.typography.labelLarge, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Screen 3: Permissions
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun OnboardingPermissionsPage(
    cameraGranted: Boolean,
    locationGranted: Boolean,
    audioGranted: Boolean,
    notificationGranted: Boolean,
    onRequestCamera: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestAudio: () -> Unit,
    onRequestNotification: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(visible = showContent, enter = fadeIn() + slideInVertically { it / 3 }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Permissions", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text("Each permission helps you capture richer field data. Deny any — you can grant later from Settings.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                val permissions = listOf(
                    PermissionItem("Camera", "Take photos of your observations for visual evidence.", FieldMindIcons.Camera, Color(0xFF5C6BC0), cameraGranted, onRequestCamera),
                    PermissionItem("Location", "Auto-tag GPS coordinates and fetch live weather for your field site.", FieldMindIcons.Location, Color(0xFF26A69A), locationGranted, onRequestLocation),
                    PermissionItem("Microphone", "Record audio field notes — ideal for hands-free observation logging.", FieldMindIcons.Mic, Color(0xFFEF5350), audioGranted, onRequestAudio),                        PermissionItem("Notifications", "Optional daily reminders, streak updates, and backup notifications.", FieldMindIcons.Notifications, Color(0xFFFFA726), notificationGranted, onRequestNotification)
                )

                permissions.forEach { perm ->
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(FieldMindMotion.expressiveFloat) + slideInVertically { it / 3 }
                    ) {
                        PermissionCard(perm)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(16.dp)) { Text("Back") }
                PageIndicator(current = 2, total = 5, modifier = Modifier.weight(1f))
                Button(onClick = onNext, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(48.dp)) { Text("Continue") }
            }
        }
    }
}

private data class PermissionItem(
    val title: String,
    val description: String,
    val icon: MaterialSymbolIcon,
    val accent: Color,
    val granted: Boolean,
    val onRequest: () -> Unit
)

@Composable
private fun PermissionCard(item: PermissionItem) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.granted) item.accent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = if (item.granted) androidx.compose.foundation.BorderStroke(1.dp, item.accent.copy(alpha = 0.3f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(
                    if (item.granted) item.accent.copy(alpha = 0.2f) else item.accent.copy(alpha = 0.1f)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, null, tint = if (item.granted) item.accent else item.accent.copy(alpha = 0.7f), size = 22.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (item.granted) {
                        Surface(shape = RoundedCornerShape(8.dp), color = item.accent.copy(alpha = 0.15f)) {
                            Text("Granted", style = MaterialTheme.typography.labelSmall, color = item.accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!item.granted) {
                Button(
                    onClick = item.onRequest,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = item.accent)
                ) { Text("Grant", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) }
            } else {
                Icon(FieldMindIcons.Check, null, tint = item.accent, size = 24.dp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Screen 4: Theme & Quick Settings
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun OnboardingThemePage(
    selectedTheme: String,
    useDynamicColors: Boolean,
    tempUnit: String,
    distanceUnit: String,
    timeFormat: String,
    dailyGoal: Int,
    onThemeChange: (String) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
    onTempUnitChange: (String) -> Unit,
    onDistanceUnitChange: (String) -> Unit,
    onTimeFormatChange: (String) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(visible = showContent, enter = fadeIn() + slideInVertically { it / 3 }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Make it yours", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text("Choose your look, units, and daily goal.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Theme
                AnimatedVisibility(visible = showContent, enter = fadeIn(FieldMindMotion.expressiveFloat)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("System", "Light", "Dark").forEach { theme ->
                                val isSelected = selectedTheme == theme
                                Surface(
                                    onClick = { onThemeChange(theme) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = when {
                                        isSelected && theme == "Dark" -> Color(0xFF1A1A2E)
                                        isSelected && theme == "Light" -> Color(0xFFF5F0E8)
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier.weight(1f).height(80.dp)
                                ) {
                                    Column(
                                        Modifier.fillMaxSize().padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            when (theme) {
                                                "System" -> FieldMindIcons.Settings
                                                "Light" -> FieldMindIcons.Weather
                                                "Dark" -> FieldMindIcons.MoonFull
                                                else -> FieldMindIcons.Settings
                                            },
                                            null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            size = 24.dp
                                        )
                                        Text(theme, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }

                // Dynamic colors
                AnimatedVisibility(visible = showContent, enter = fadeIn(FieldMindMotion.expressiveFloat)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Use device colors", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Match your system wallpaper palette", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = useDynamicColors,
                            onCheckedChange = onDynamicColorsChange
                        )
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                // Units
                AnimatedVisibility(visible = showContent, enter = fadeIn(FieldMindMotion.expressiveFloat)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Units & format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Temperature", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                SegmentedButtonRow(options = listOf("Celsius", "Fahrenheit"), selected = tempUnit, onSelect = onTempUnitChange)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Distance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                SegmentedButtonRow(options = listOf("km", "mi"), selected = distanceUnit, onSelect = onDistanceUnitChange)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Time format", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            SegmentedButtonRow(options = listOf("24h", "12h"), selected = timeFormat, onSelect = onTimeFormatChange)
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                // Daily goal
                AnimatedVisibility(visible = showContent, enter = fadeIn(FieldMindMotion.expressiveFloat)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Daily observation goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("$dailyGoal / day", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = dailyGoal.toFloat(),
                            onValueChange = { onDailyGoalChange(it.roundToInt()) },
                            valueRange = 0f..10f,
                            steps = 9,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("None", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("10/day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(16.dp)) { Text("Back") }
                PageIndicator(current = 3, total = 5, modifier = Modifier.weight(1f))
                Button(onClick = onNext, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(48.dp)) { Text("Continue") }
            }
        }
    }
}

@Composable
private fun SegmentedButtonRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        options.forEachIndexed { index, option ->
            val isSelected = selected == option
            val isFirst = index == 0
            val isLast = index == options.lastIndex
            Surface(
                onClick = { onSelect(option) },
                shape = if (isFirst && isLast) RoundedCornerShape(12.dp)
                    else if (isFirst) RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    else if (isLast) RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                    else RoundedCornerShape(0.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Screen 5: Review & Done
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun OnboardingReviewPage(
    profileName: String,
    profileRole: String,
    interests: UserInterests,
    cameraGranted: Boolean,
    locationGranted: Boolean,
    audioGranted: Boolean,
    selectedTheme: String,
    dailyGoal: Int,
    onFinish: () -> Unit,
    onContinueTour: () -> Unit,
    onBack: () -> Unit,
    onEditPage: (Int) -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    val grantedCount = listOf(cameraGranted, locationGranted, audioGranted).count { it }

    LaunchedEffect(Unit) { showContent = true }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // decorative orb
        Box(
            Modifier.size(200.dp).offset(x = (-60).dp, y = 200.dp)
                .graphicsLayer { alpha = 0.05f }
                .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary, Color.Transparent)), CircleShape)
        )

        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                AnimatedVisibility(visible = showContent, enter = fadeIn() + scaleIn(initialScale = 0.9f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            Modifier.size(80.dp).background(
                                Brush.linearGradient(listOf(Color(0xFF43A047), Color(0xFF66BB6A))),
                                RoundedCornerShape(28.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(FieldMindIcons.Check, null, tint = Color.White, size = 44.dp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("You're all set!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        Text(
                            "Your field research companion is ready.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Summary card
                AnimatedVisibility(visible = showContent, enter = fadeIn(FieldMindMotion.expressiveFloat) + slideInVertically { it / 4 }) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            ReviewRow("Name", profileName.ifBlank { "Not set" }, "Edit", onClick = { onEditPage(0) })
                            HorizontalDivider()
                            ReviewRow("Role", profileRole, "Edit", onClick = { onEditPage(0) })

                            HorizontalDivider()
                            val interestLabel = buildList {
                                if (interests.zoology.isNotEmpty()) add("${interests.zoology.size} zoology")
                                if (interests.botany.isNotEmpty()) add("${interests.botany.size} botany")
                                if (interests.ecologyEnvironment) add("Ecology")
                                if (interests.astronomy) add("Astronomy")
                                if (interests.geology) add("Geology")
                                interests.customInterests.forEach { add(it) }
                            }.joinToString(", ").ifEmpty { "Not specified" }
                            ReviewRow("Interests", interestLabel, "Edit", onClick = { onEditPage(1) })

                            HorizontalDivider()
                            ReviewRow("Permissions", "$grantedCount/4 granted", "Edit", onClick = { onEditPage(2) })

                            HorizontalDivider()
                            ReviewRow("Theme", selectedTheme, "Edit", onClick = { onEditPage(3) })

                            HorizontalDivider()
                            ReviewRow("Daily goal", "$dailyGoal observation${if (dailyGoal == 1) "" else "s"} per day", "Edit", onClick = { onEditPage(3) })
                        }
                    }
                }
            }

            // Bottom actions
            AnimatedVisibility(visible = showContent, enter = fadeIn(FieldMindMotion.expressiveFloat)) {
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onBack,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("Back") }
                        Button(
                            onClick = onContinueTour,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) { Text("Continue tour") }
                    }
                    Button(
                        onClick = onFinish,
                        modifier = Modifier.fillMaxWidth().height(54.dp).expressivePress(scaleDown = 0.96f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Finish tour", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String, actionLabel: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            Text(actionLabel, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Extended Tour — Screen 6: Screen Visibility
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun OnboardingScreenVisibilityPage(
    visibility: ScreenVisibility,
    interests: UserInterests,
    onApply: (ScreenVisibility) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    var currentVis by remember { mutableStateOf(visibility) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { showContent = true }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(visible = showContent, enter = fadeIn() + slideInVertically { it / 3 }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Screen visibility", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text("Choose which screens appear in your navigation. Hidden screens stay accessible from Settings.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                val screenToggles = listOf(
                    ScreenToggle("Capture", "Quick photo + observation capture", FieldMindIcons.Capture, currentVis.showCapture) { currentVis = currentVis.copy(showCapture = it) },
                    ScreenToggle("Workspace", "Projects, data tools, reports", FieldMindIcons.Projects, currentVis.showProjects) { currentVis = currentVis.copy(showProjects = it) },
                    ScreenToggle("Insights", "Analytics, trends, progress", FieldMindIcons.Insights, currentVis.showInsights) { currentVis = currentVis.copy(showInsights = it) },
                    ScreenToggle("Library", "Sources, species, learn, flashcards", FieldMindIcons.Library, currentVis.showLibrary) { currentVis = currentVis.copy(showLibrary = it) },
                    ScreenToggle("Map", "Spatial view of observations", FieldMindIcons.Map, currentVis.showMap) { currentVis = currentVis.copy(showMap = it) },
                    ScreenToggle("Weather", "Live weather and climate data", FieldMindIcons.Weather, currentVis.showWeather) { currentVis = currentVis.copy(showWeather = it) },
                    ScreenToggle("Species Browser", "Browse and identify species", FieldMindIcons.Nature, currentVis.showSpeciesBrowser) { currentVis = currentVis.copy(showSpeciesBrowser = it) },
                    ScreenToggle("Flashcards", "Spaced repetition review", FieldMindIcons.Flashcard, currentVis.showFlashcards) { currentVis = currentVis.copy(showFlashcards = it) }
                )

                screenToggles.forEach { toggle ->
                    AnimatedVisibility(visible = showContent, enter = fadeIn(FieldMindMotion.expressiveFloat)) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(toggle.icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
                            Column(Modifier.weight(1f)) {
                                Text(toggle.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(toggle.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = toggle.visible, onCheckedChange = toggle.onToggle)
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(16.dp)) { Text("Back") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSkip) { Text("Skip") }
                Button(onClick = { onApply(currentVis) }, shape = RoundedCornerShape(16.dp)) { Text("Apply & continue") }
            }
        }
    }
}

private data class ScreenToggle(val title: String, val description: String, val icon: MaterialSymbolIcon, val visible: Boolean, val onToggle: (Boolean) -> Unit)

// ══════════════════════════════════════════════════════════════════════
//  Extended Tour — Screen 7: AI & Features
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun OnboardingAiFeaturesPage(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(visible = showContent, enter = fadeIn() + slideInVertically { it / 3 }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("AI & Features", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text("These features can be configured anytime from Settings.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                listOf(
                    Triple("AI Assistant", "Use Gemini or OpenAI to auto-suggest species, analyze observations, and draft reports. Configure API keys in Settings.", FieldMindIcons.Sparkle),
                    Triple("Species Identification", "Offline-first AI that identifies species from photos. Gets smarter over time as you confirm matches.", FieldMindIcons.Nature),
                    Triple("Privacy Lock", "Secure your research data with biometric or PIN lock. Auto-locks when the app goes to background.", FieldMindIcons.Lock)
                ).forEach { (title, desc, icon) ->
                    AnimatedVisibility(visible = showContent, enter = fadeIn(FieldMindMotion.expressiveFloat)) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(16.dp)) { Text("Back") }
                Spacer(Modifier.weight(1f))
                Button(onClick = onNext, shape = RoundedCornerShape(16.dp)) { Text("Continue") }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Extended Tour — Screen 8: Backup & Reminders
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun OnboardingBackupPage(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(visible = showContent, enter = fadeIn() + slideInVertically { it / 3 }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Backup & reminders", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text("Keep your research safe and stay on track.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                listOf(
                    Triple("Auto-backup", "Schedule automatic backups of all your observations, projects, and settings. Configurable frequency.", FieldMindIcons.Export),
                    Triple("Daily reminders", "Gentle nudges if you haven't logged an observation. Helps maintain your research streak.", FieldMindIcons.Notifications),
                    Triple("Research streaks", "Track your daily observation streak for motivation. Visual streak cards on the home screen.", FieldMindIcons.Streak)
                ).forEach { (title, desc, icon) ->
                    AnimatedVisibility(visible = showContent, enter = fadeIn(FieldMindMotion.expressiveFloat)) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, size = 22.dp)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(16.dp)) { Text("Back") }
                Spacer(Modifier.weight(1f))
                Button(onClick = onNext, shape = RoundedCornerShape(16.dp)) { Text("Continue") }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  Extended Tour — Screen 9: Data Tools
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun OnboardingDataToolsPage(
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Decorative orb
        Box(
            Modifier.size(180.dp).offset(x = 80.dp, y = (-60).dp)
                .graphicsLayer { alpha = 0.05f }
                .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.tertiary, Color.Transparent)), CircleShape)
        )

        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                AnimatedVisibility(visible = showContent, enter = fadeIn() + scaleIn(initialScale = 0.92f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            Modifier.size(72.dp).background(
                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)),
                                RoundedCornerShape(24.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(FieldMindIcons.Data, null, tint = MaterialTheme.colorScheme.onPrimary, size = 38.dp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Data tools", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        Text("Specialized tools for field research data collection.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }

                val tools = listOf(
                    Pair(Triple("Counter", "Track frequencies — bird calls, insect encounters, plant counts", FieldMindIcons.Add), Color(0xFF43A047)),
                    Pair(Triple("Measure", "Log transect distances, plot sizes, and GPS waypoints", FieldMindIcons.Graph), Color(0xFF5C6BC0)),
                    Pair(Triple("Weather Log", "Record temperature, humidity, wind, and pressure at your field site", FieldMindIcons.Weather), Color(0xFF26A69A)),
                    Pair(Triple("Species Survey", "Log species sightings with abundance estimates and habitat notes", FieldMindIcons.Nature), Color(0xFFFFA726))
                )

                tools.forEach { (info, accent) ->
                    val (title, desc, icon) = info
                    AnimatedVisibility(visible = showContent, enter = fadeIn(FieldMindMotion.expressiveFloat)) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                    Icon(icon, null, tint = accent, size = 24.dp)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom actions — last page, so "Finish" with the dialog trigger
            AnimatedVisibility(visible = showContent, enter = fadeIn(FieldMindMotion.expressiveFloat)) {
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onBack, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) { Text("Back") }
                        Button(
                            onClick = onFinish,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Start exploring") }
                    }
                    Text(
                        "Everything can be customized from Settings.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}
