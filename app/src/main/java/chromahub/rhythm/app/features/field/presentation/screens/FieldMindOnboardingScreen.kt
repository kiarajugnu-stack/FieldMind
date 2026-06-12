package chromahub.rhythm.app.features.field.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import chromahub.rhythm.app.features.field.presentation.components.FieldMindIcons
import chromahub.rhythm.app.features.field.presentation.theme.FieldMindTheme
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
)

private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

// ══════════════════════════════════════════════════════════════════════
//  Onboarding
// ══════════════════════════════════════════════════════════════════════

@Composable
fun FieldMindOnboardingScreen(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val pages = listOf(
        Triple(FieldMindIcons.Nature, "Welcome to FieldMind", "A local-first research notebook for Observe → Question → Research → Hypothesize → Collect Data → Analyze → Conclude → Archive."),
        Triple(FieldMindIcons.Observation, "Research begins with evidence", "Capture facts, media, location, questions, and confidence without inventing conclusions."),
        Triple(FieldMindIcons.Project, "Build projects", "Turn repeated curiosity into objectives, methods, data, sources, reports, and next steps."),
        Triple(FieldMindIcons.Insights, "Set your research profile", "Add your name, role, focus area, local model, and backup rhythm in Settings. It personalizes Insights without any FieldMind server."),
        Triple(FieldMindIcons.Bolt, "Capture in two taps", "Field mode logs an observation instantly with one big button, so you never miss a moment outdoors."),
        Triple(FieldMindIcons.Export, "Own your work", "Everything core works offline and can be exported as Markdown, CSV, JSON, PNG, SVG, PDF, or plain text.")
    )
    val totalSteps = pages.size + 1
    val isPermissionStep = step == pages.size
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f, fill = false).padding(top = 48.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(
                    Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon = if (isPermissionStep) FieldMindIcons.Lock else pages[step].first, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 38.dp) }
                Text("Step ${step + 1} of $totalSteps", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                if (isPermissionStep) {
                    Text("Optional permissions", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text("FieldMind has no app server for your archive. Permissions only unlock specific device features: GPS for location, camera/media for evidence, microphone for audio notes, and notifications for reminders. You can skip all of them and still use notes, sources, questions, projects, exports, and local backups.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OnboardingPermissions()
                } else {
                    Text(pages[step].second, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(pages[step].third, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(totalSteps) { i ->
                        Box(
                            Modifier
                                .height(6.dp)
                                .weight(1f)
                                .background(
                                    if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onFinish, Modifier.weight(1f)) { Text(if (isPermissionStep) "Skip all" else "Skip") }
                    Button(onClick = { if (step < totalSteps - 1) step++ else onFinish() }, Modifier.weight(1f)) { Text(if (isPermissionStep) "Enter FieldMind" else "Next") }
                }
            }
        }
    }
}

/**
 * Optional permission requests shown on the last onboarding page. Each is independent and may be
 * skipped; nothing here is required for the app to function. Granted state updates live so the
 * user sees confirmation.
 */
@Composable
private fun OnboardingPermissions() {
    val context = LocalContext.current
    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    var locationGranted by remember { mutableStateOf(granted(Manifest.permission.ACCESS_FINE_LOCATION)) }
    var micGranted by remember { mutableStateOf(granted(Manifest.permission.RECORD_AUDIO)) }
    val mediaPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
    var mediaGranted by remember { mutableStateOf(granted(mediaPerm)) }
    var notifGranted by remember { mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || granted("android.permission.POST_NOTIFICATIONS")) }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { locationGranted = it }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { micGranted = it }
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { mediaGranted = it }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifGranted = it }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PermissionRow(FieldMindIcons.Location, "Location", "Tag observations with GPS coordinates and place names.", locationGranted) { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
        PermissionRow(FieldMindIcons.Gallery, "Photos & media", "Attach images as visual evidence to observations.", mediaGranted) { mediaLauncher.launch(mediaPerm) }
        PermissionRow(FieldMindIcons.Mic, "Microphone", "Record short audio notes in the field.", micGranted) { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRow(FieldMindIcons.Notifications, "Notifications", "Optional reminders to keep your research streak.", notifGranted) { notifLauncher.launch("android.permission.POST_NOTIFICATIONS") }
        }
    }
}

@Composable
private fun PermissionRow(icon: MaterialSymbolIcon, title: String, desc: String, granted: Boolean, onRequest: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 20.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon = FieldMindIcons.Check, contentDescription = null, tint = FieldMindTheme.colors.positive, size = 18.dp)
                Text("Allowed", style = MaterialTheme.typography.labelMedium, color = FieldMindTheme.colors.positive, fontWeight = FontWeight.SemiBold)
            }
        } else {
            FilledTonalButton(onClick = onRequest, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) { Text("Allow") }
        }
    }
}
