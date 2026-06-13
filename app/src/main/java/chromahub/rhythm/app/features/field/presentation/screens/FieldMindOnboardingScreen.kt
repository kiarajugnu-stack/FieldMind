package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.shared.presentation.components.icons.Icon

/**
 * Simplified 3-step onboarding. Users can skip directly to the app from
 * any step via the "Enter FieldMind" button.
 */
@Composable
fun FieldMindOnboardingScreen(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val pages = listOf(
        Triple(FieldMindIcons.Nature, "Welcome to FieldMind", "A local-first research notebook for Observe, Question, Research, Hypothesize, Collect Data, Analyze, Conclude, and Archive.\n\nEverything stays on your device unless you export or back it up."),
        Triple(FieldMindIcons.Observation, "Capture, learn, and grow", "Log observations with facts, evidence, location, and confidence.\n\nBuild projects around questions, collect data, draft reports, and turn findings into review cards with spaced repetition."),
        Triple(FieldMindIcons.Bolt, "Start in two taps", "Tap Capture anywhere to log an observation. Use Field Mode for one-tap quick capture. Start with one observation and build from there.")
    )
    val totalSteps = pages.size

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(
                Modifier.weight(1f, fill = false).padding(top = 48.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon = pages[step].first, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 38.dp)
                }
                Text("Step ${step + 1} of $totalSteps", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(pages[step].second, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(pages[step].third, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    OutlinedButton(
                        onClick = onFinish,
                        Modifier.weight(1f)
                    ) { Text("Enter FieldMind") }
                    Button(
                        onClick = { if (step < totalSteps - 1) step++ else onFinish() },
                        Modifier.weight(1f)
                    ) { Text(if (step < totalSteps - 1) "Next" else "Start") }
                }
            }
        }
    }
}
