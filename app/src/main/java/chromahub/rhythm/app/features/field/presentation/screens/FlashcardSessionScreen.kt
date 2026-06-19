package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import fieldmind.research.app.features.field.data.database.entity.FlashcardEntity
import fieldmind.research.app.features.field.data.flashcard.SM2Engine
import fieldmind.research.app.features.field.presentation.components.EmptyState
import fieldmind.research.app.features.field.presentation.components.FieldMindIcons
import fieldmind.research.app.features.field.presentation.components.BackButton
import fieldmind.research.app.features.field.presentation.components.StandardScreenHeader
import fieldmind.research.app.features.field.presentation.components.FieldScreenHeader
import fieldmind.research.app.features.field.presentation.components.InfoChip
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.features.field.presentation.viewmodel.FieldMindViewModel
import fieldmind.research.app.shared.presentation.components.icons.Icon

/**
 * Flashcard review session screen supporting both basic flip and SM-2 spaced repetition modes.
 *
 * Per-deck mode is determined by the first card's deckMode field.
 * - "basic": simple flip with Again/Good/Easy
 * - "sm2": full SM-2 scheduling with ease factor visualization, interval hints, and stats
 */
@Composable
fun FlashcardSessionScreen(viewModel: FieldMindViewModel, onBack: () -> Unit) {
    BackHandler(enabled = true) { onBack() }
    val flashcards by viewModel.flashcards.collectAsState()
    var queue by remember(flashcards) { mutableStateOf(flashcards) }
    var index by remember(flashcards) { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    var reviewed by remember(flashcards) { mutableIntStateOf(0) }
    var againCount by remember(flashcards) { mutableIntStateOf(0) }
    var goodCount by remember(flashcards) { mutableIntStateOf(0) }
    var easyCount by remember(flashcards) { mutableIntStateOf(0) }

    // Determine deck mode from first card
    val isSm2Mode = remember(flashcards) {
        flashcards.firstOrNull()?.deckMode == "sm2"
    }

    if (flashcards.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            StandardScreenHeader(
                title = "Review",
                subtitle = "Spaced repetition",
                icon = FieldMindIcons.Flashcard,
                heroColor = FieldMindTheme.colors.flashcard,
                trailing = {
                    BackButton(onClick = onBack, icon = FieldMindIcons.Close, contentDescription = "Close")
                }
            )
            EmptyState("No cards to review", "Create flashcards in the Library to start a review session.", icon = FieldMindIcons.Flashcard)
        }
        return
    }

    val finished = index >= queue.size
    // Session progress
    val progress = if (queue.isEmpty()) 0f else index.toFloat() / queue.size

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Progress bar
        AnimatedVisibility(visible = !finished, enter = fadeIn(), exit = fadeOut()) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }

        StandardScreenHeader(
            title = if (finished) "Session complete" else "Card ${index + 1} of ${queue.size}",
            subtitle = buildString {
                append("$reviewed reviewed")
                if (isSm2Mode) {
                    append(" • ")
                    append("A:$againCount G:$goodCount E:$easyCount")
                } else {
                    if (againCount > 0) append(" • $againCount to revisit")
                }
            },
            icon = FieldMindIcons.Flashcard,
            heroColor = FieldMindTheme.colors.flashcard,
            trailing = {
                BackButton(onClick = onBack, icon = FieldMindIcons.Close, contentDescription = "Close")
            }
        )

        if (finished) {
            SessionCompleteCard(reviewed, againCount, goodCount, easyCount, isSm2Mode)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    // For SM-2, only requeue "again" cards; for basic, all cards
                    queue = if (isSm2Mode) {
                        flashcards.filter { it.deckMode != "sm2" || SM2Engine.isDue(it.nextReviewAt) }
                    } else {
                        flashcards
                    }
                    index = 0; flipped = false; reviewed = 0; againCount = 0; goodCount = 0; easyCount = 0
                },
                Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Review again") }
            OutlinedButton(onClick = onBack, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) { Text("Done") }
        } else {
            val card = queue[index]
            ReviewCard(card, flipped) { flipped = !flipped }
            Spacer(Modifier.weight(1f))

            if (!flipped) {
                Button(
                    onClick = { flipped = true },
                    Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(icon = FieldMindIcons.Flip, contentDescription = null, size = 20.dp)
                    Spacer(Modifier.size(8.dp))
                    Text("Show answer")
                }
            } else {
                if (isSm2Mode) {
                    Sm2RatingButtons(card) { rating ->
                        val result = SM2Engine.calculate(
                            rating = rating,
                            currentEaseFactor = card.easeFactor,
                            currentInterval = card.intervalDays,
                            currentRepetition = card.repetitionCount
                        )
                        val updated = card.copy(
                            easeFactor = result.easeFactor,
                            intervalDays = result.intervalDays,
                            repetitionCount = result.repetitionCount,
                            nextReviewAt = result.nextReviewAt,
                            lastReviewedAt = result.lastReviewedAt,
                            reviewCount = card.reviewCount + 1,
                            updatedAt = System.currentTimeMillis()
                        )
                        viewModel.updateFlashcardEntity(updated)
                        when (rating) {
                            0 -> againCount++
                            1, 2 -> goodCount++
                            3 -> easyCount++
                        }
                        if (rating == 0) queue = queue + card
                        reviewed++
                        index++
                        flipped = false
                    }
                } else {
                    BasicRatingButtons { requeue ->
                        if (requeue) { queue = queue + card; againCount++ }
                        reviewed++
                        index++
                        flipped = false
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCompleteCard(reviewed: Int, again: Int, good: Int, easy: Int, isSm2: Boolean) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Animated check icon
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }
            AnimatedVisibility(visible = visible, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()) {
                Icon(icon = FieldMindIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 44.dp)
            }
            Text("Reviewed $reviewed card${if (reviewed == 1) "" else "s"}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (isSm2) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatPill("Again", again, MaterialTheme.colorScheme.error)
                    StatPill("Good", good, MaterialTheme.colorScheme.tertiary)
                    StatPill("Easy", easy, MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                "Spaced repetition turns notes into durable memory. Come back tomorrow to keep the streak.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatPill(label: String, count: Int, color: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text("$count $label", style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

/** SM-2 rating buttons with interval preview. */
@Composable
private fun Sm2RatingButtons(card: FlashcardEntity, onRate: (Int) -> Unit) {
    val ratings = listOf(
        Triple(0, "Again", MaterialTheme.colorScheme.error),
        Triple(2, "Good", MaterialTheme.colorScheme.tertiary),
        Triple(3, "Easy", MaterialTheme.colorScheme.primary)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Interval preview
        val previewResults = remember(card) {
            ratings.map { (rating, _, _) ->
                SM2Engine.calculate(rating, card.easeFactor, card.intervalDays, card.repetitionCount)
            }
        }
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                ratings.forEachIndexed { i, (rating, label, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
                        Text(
                            SM2Engine.nextReviewLabel(previewResults[i].intervalDays),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        // Rating buttons
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ratings.forEach { (rating, label, color) ->
                val animatedColor by animateColorAsState(targetValue = color.copy(alpha = 0.12f), label = "btnBg")
                Button(
                    onClick = { onRate(rating) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.12f), contentColor = color)
                ) { Text(label, fontWeight = FontWeight.SemiBold) }
            }
        }
        // Ease factor display
        val easeLabel = SM2Engine.easeLabel(card.easeFactor)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Ease: ${String.format("%.1f", card.easeFactor)} ($easeLabel)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Basic rating buttons for simple flip mode. */
@Composable
private fun BasicRatingButtons(onRate: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { onRate(true) }, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Again") }
        FilledTonalButton(onClick = { onRate(false) }, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Good") }
        Button(onClick = { onRate(false) }, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Easy") }
    }
}

@Composable
private fun ColumnScope.ReviewCard(card: FlashcardEntity, flipped: Boolean, onFlip: () -> Unit) {
    val accent = FieldMindTheme.colors.flashcard
    val rotation by animateFloatAsState(targetValue = if (flipped) 180f else 0f, animationSpec = tween(420), label = "cardTurn")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .heightIn(min = 220.dp)
            .graphicsLayer { rotationY = rotation; cameraDistance = 32f }
            .clickable(onClick = onFlip),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = if (flipped) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.fillMaxWidth()
                .graphicsLayer { if (rotation > 90f) rotationY = 180f }
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(48.dp).background(accent.copy(alpha = if (FieldMindTheme.colors.isDark) 0.22f else 0.14f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon = FieldMindIcons.Flashcard, contentDescription = null, tint = accent, size = 26.dp) }
            InfoChip(if (flipped) "Answer" else "Question · ${card.type}")
            AnimatedContent(targetState = flipped, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "flip") { showBack ->
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
