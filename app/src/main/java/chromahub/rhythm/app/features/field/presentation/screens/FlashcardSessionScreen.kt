package chromahub.rhythm.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.features.field.data.database.entity.FlashcardEntity
import chromahub.rhythm.app.features.field.presentation.components.EmptyState
import chromahub.rhythm.app.features.field.presentation.components.FieldMindIcons
import chromahub.rhythm.app.features.field.presentation.components.FieldScreenHeader
import chromahub.rhythm.app.features.field.presentation.components.InfoChip
import chromahub.rhythm.app.features.field.presentation.theme.FieldMindTheme
import chromahub.rhythm.app.features.field.presentation.viewmodel.FieldMindViewModel
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

/**
 * In-memory spaced-repetition review session. Cards rated "Again" are re-queued to the end of the
 * deck; "Good"/"Easy" advance. Review state persistence can be layered on later via the repository.
 */
@Composable
fun FlashcardSessionScreen(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    val flashcards by viewModel.flashcards.collectAsState()
    var queue by remember(flashcards) { mutableStateOf(flashcards) }
    var index by remember(flashcards) { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    var reviewed by remember(flashcards) { mutableIntStateOf(0) }
    var again by remember(flashcards) { mutableIntStateOf(0) }

    if (flashcards.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            FieldScreenHeader("Review", "Spaced repetition", icon = FieldMindIcons.Flashcard, actionIcon = FieldMindIcons.Close, onAction = onBack)
            EmptyState("No cards to review", "Create flashcards in the Library to start a review session.", icon = FieldMindIcons.Flashcard)
        }
        return
    }

    val finished = index >= queue.size
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        FieldScreenHeader(
            title = if (finished) "Session complete" else "Card ${index + 1} of ${queue.size}",
            subtitle = "$reviewed reviewed • $again to revisit",
            icon = FieldMindIcons.Flashcard,
            actionIcon = FieldMindIcons.Close,
            onAction = onBack
        )

        if (finished) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon = FieldMindIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 44.dp)
                    Text("Reviewed $reviewed card${if (reviewed == 1) "" else "s"}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Spaced repetition turns notes into durable memory. Come back tomorrow to keep the streak.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { queue = flashcards; index = 0; flipped = false; reviewed = 0; again = 0 }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Review again") }
            OutlinedButton(onClick = onBack, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Done") }
        } else {
            val card = queue[index]
            ReviewCard(card, flipped) { flipped = !flipped }
            Spacer(Modifier.weight(1f))

            if (!flipped) {
                Button(onClick = { flipped = true }, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(icon = FieldMindIcons.Flip, contentDescription = null, size = 20.dp); Spacer(Modifier.size(8.dp)); Text("Show answer")
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val advance: (Boolean) -> Unit = { requeue ->
                        if (requeue) { queue = queue + card; again++ }
                        reviewed++
                        index++
                        flipped = false
                    }
                    OutlinedButton(onClick = { advance(true) }, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Again") }
                    FilledTonalButton(onClick = { advance(false) }, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Good") }
                    Button(onClick = { advance(false) }, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Easy") }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ReviewCard(card: FlashcardEntity, flipped: Boolean, onFlip: () -> Unit) {
    val accent = FieldMindTheme.colors.flashcard
    Card(
        modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 220.dp).clickable(onClick = onFlip),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = if (flipped) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(48.dp).background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon = FieldMindIcons.Flashcard, contentDescription = null, tint = accent, size = 26.dp) }
            InfoChip(if (flipped) "Answer" else "Question · ${card.type}")
            AnimatedContent(targetState = flipped, transitionSpec = { (fadeIn() togetherWith fadeOut()) }, label = "flip") { showBack ->
                Text(
                    if (showBack) card.back else card.front,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
            if (!flipped) Text("Tap to reveal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
