package fieldmind.research.app.features.field.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fieldmind.research.app.features.field.data.settings.*
import fieldmind.research.app.features.field.data.vision.SpeciesDatabase
import fieldmind.research.app.features.field.presentation.navigation.FieldMindScreen
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon
import kotlinx.coroutines.delay

// ══════════════════════════════════════════════════════════════════════
//  Home Species Catalog Section — Compact card with count + Browse button
// ══════════════════════════════════════════════════════════════════════

@Composable
fun HomeSpeciesCatalogSection(
    onNavigate: (FieldMindScreen) -> Unit,
    userInterests: UserInterests = UserInterests()
) {
    val context = LocalContext.current
    val database = remember { SpeciesDatabase(context) }
    var totalCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // Load total species count on first composition
    LaunchedEffect(Unit) {
        totalCount = database.getTotalSpeciesCount()
        isLoading = false
    }

    val colors = FieldMindTheme.colors
    val accentColor = colors.observation

    // Interest-aware subtitle
    val subtitle = remember(userInterests) {
        buildString {
            append("Browse species by name, category, and taxonomy")
            val focusAreas = buildList {
                if (userInterests.zoology.contains(ZoologySubfield.Birds)) add("ornithology")
                if (userInterests.zoology.contains(ZoologySubfield.Mammals)) add("mammalogy")
                if (userInterests.zoology.contains(ZoologySubfield.Herps)) add("herpetology")
                if (userInterests.zoology.contains(ZoologySubfield.Insects)) add("entomology")
                if (userInterests.zoology.contains(ZoologySubfield.Marine)) add("marine biology")
                if (userInterests.botany.contains(BotanySubfield.Wildflowers)) add("wildflowers")
                if (userInterests.botany.contains(BotanySubfield.Trees)) add("trees")
                if (userInterests.botany.contains(BotanySubfield.Fungi)) add("fungi")
                if (userInterests.botany.contains(BotanySubfield.Mosses)) add("bryophytes")
                if (userInterests.ecologyEnvironment) add("ecology")
                if (userInterests.astronomy) add("astronomy")
                if (userInterests.geology) add("geology")
            }
            if (focusAreas.isNotEmpty()) {
                append(" \u2022 Focus: ")
                append(focusAreas.joinToString(", "))
            }
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(FieldMindIcons.Nature, null, tint = accentColor, size = 24.dp)
            }

            // Text
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Species catalog", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (!isLoading) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "$totalCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Browse button
            FilledTonalButton(
                onClick = { onNavigate(FieldMindScreen.SpeciesBrowser) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = accentColor.copy(alpha = 0.12f)
                )
            ) {
                Text("Browse", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.size(4.dp))
                Icon(FieldMindIcons.Forward, null, size = 16.dp)
            }
        }
    }
}
