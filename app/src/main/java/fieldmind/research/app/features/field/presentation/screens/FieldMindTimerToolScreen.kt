package fieldmind.research.app.features.field.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.features.field.background.FieldMindTimerManager
import fieldmind.research.app.features.field.background.FieldMindTimerService
import fieldmind.research.app.features.field.presentation.components.*
import fieldmind.research.app.features.field.presentation.theme.FieldMindTheme
import fieldmind.research.app.shared.presentation.components.icons.Icon
import kotlinx.coroutines.delay

// ══════════════════════════════════════════════════════════════════════
//  Timer Tool — Simple timer & stopwatch with background notification
// ══════════════════════════════════════════════════════════════════════

enum class TimerMode { TIMER, STOPWATCH }

@Composable
fun TimerToolScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = FieldMindTheme.colors

    // Timer state — uses rememberSaveable so it survives navigation away
    var timerMode by rememberSaveable { mutableStateOf(TimerMode.STOPWATCH) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var isPaused by rememberSaveable { mutableStateOf(false) }
    var elapsedMs by rememberSaveable { mutableLongStateOf(0L) }
    // accumulatedElapsedMs tracks total elapsed time regardless of mode
    var accumulatedElapsedMs by rememberSaveable { mutableLongStateOf(0L) }
    var baseStart by rememberSaveable { mutableLongStateOf(0L) }
    var pausedAccumulated by rememberSaveable { mutableLongStateOf(0L) }
    var laps by rememberSaveable { mutableStateOf(listOf<Long>()) }

    // Countdown timer mode
    var countdownMinutes by rememberSaveable { mutableIntStateOf(5) }
    var countdownSeconds by rememberSaveable { mutableIntStateOf(0) }
    var countdownTotalMs by rememberSaveable { mutableLongStateOf(0L) }
    var countdownFinished by rememberSaveable { mutableStateOf(false) }

    fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        val millis = (ms % 1000) / 100
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else if (timerMode == TimerMode.STOPWATCH) "%02d:%02d.%d".format(minutes, seconds, millis)
        else "%02d:%02d".format(minutes, seconds)
    }

    // Timer tick — updates every 100ms
    // Self-corrects stale baseStart on screen re-entry via saved timer state
    LaunchedEffect(isRunning, isPaused, baseStart, pausedAccumulated, timerMode, countdownTotalMs) {
        if (isRunning && !isPaused) {
            // If baseStart is stale (e.g. after navigation away and back),
            // use saved state to prevent the timer jumping forward by the
            // time the composable was not ticking. The >3s threshold ensures
            // this only fires on re-entry, not on ordinary pause/resume where
            // baseStart is set to System.currentTimeMillis().
            if (System.currentTimeMillis() - baseStart > 3000L) {
                val savedState = FieldMindTimerManager.getSavedTimerState(context)
                if (savedState != null && savedState.type == FieldMindTimerService.TYPE_READING && savedState.elapsedMs > 0L) {
                    pausedAccumulated = savedState.elapsedMs
                    baseStart = System.currentTimeMillis()
                }
            }
            while (isRunning && !isPaused) {
                delay(100)
                val totalSinceBase = pausedAccumulated + (System.currentTimeMillis() - baseStart)
                accumulatedElapsedMs = totalSinceBase
                if (timerMode == TimerMode.STOPWATCH) {
                    elapsedMs = totalSinceBase
                } else {
                    // Countdown: remaining = total - elapsed
                    val remaining = countdownTotalMs - totalSinceBase
                    if (remaining <= 0) {
                        elapsedMs = 0L
                        isRunning = false
                        isPaused = false
                        countdownFinished = true
                        FieldMindTimerManager.clearSavedState(context)
                        FieldMindTimerManager.stopReadingTimer(context)
                        break
                    }
                    elapsedMs = remaining
                }
            }
        }
    }

    // Start foreground service notification when running
    LaunchedEffect(isRunning) {
        if (isRunning && !isPaused) {
            val title = if (timerMode == TimerMode.STOPWATCH) "Stopwatch" else "Timer"
            FieldMindTimerManager.startReadingTimer(context)
            FieldMindTimerManager.updateTimerNotification(
                context = context,
                title = title,
                text = formatTime(elapsedMs),
                elapsedMs = accumulatedElapsedMs,
                timerType = FieldMindTimerService.TYPE_READING
            )
        }
    }

    // Update notification every second (service ticks internally too)
    LaunchedEffect(isRunning) {
        if (isRunning && !isPaused) {
            while (isRunning && !isPaused) {
                delay(1000)
                val title = if (timerMode == TimerMode.STOPWATCH) "Stopwatch" else "Timer"
                FieldMindTimerManager.updateTimerNotification(
                    context = context,
                    title = title,
                    text = formatTime(elapsedMs),
                    elapsedMs = accumulatedElapsedMs,
                    timerType = FieldMindTimerService.TYPE_READING
                )
            }
        }
    }

    fun startTimer() {
        if (timerMode == TimerMode.TIMER && !countdownFinished) {
            countdownTotalMs = (countdownMinutes * 60L + countdownSeconds) * 1000L
            if (countdownTotalMs <= 0) return
        }
        countdownFinished = false
        if (isPaused) {
            // Resume: accumulatedElapsedMs already saved in pausedAccumulated
            pausedAccumulated = accumulatedElapsedMs
            baseStart = System.currentTimeMillis()
            isPaused = false
        } else {
            // Fresh start
            pausedAccumulated = 0L
            baseStart = System.currentTimeMillis()
            accumulatedElapsedMs = 0L
            elapsedMs = if (timerMode == TimerMode.STOPWATCH) 0L else countdownTotalMs
        }
        isRunning = true
    }

    fun pauseTimer() {
        if (isRunning && !isPaused) {
            // Save accumulated elapsed time
            accumulatedElapsedMs = pausedAccumulated + (System.currentTimeMillis() - baseStart)
            pausedAccumulated = accumulatedElapsedMs
            isPaused = true
        }
    }

    fun resetTimer() {
        isRunning = false
        isPaused = false
        elapsedMs = if (timerMode == TimerMode.STOPWATCH) 0L else countdownTotalMs
        accumulatedElapsedMs = 0L
        pausedAccumulated = 0L
        laps = emptyList()
        countdownFinished = false
        FieldMindTimerManager.stopReadingTimer(context)
    }

    fun recordLap() {
        if (isRunning && !isPaused && timerMode == TimerMode.STOPWATCH) {
            val currentTotal = accumulatedElapsedMs
            val prevLapTotal = laps.fold(0L) { acc, lap -> acc + lap }
            val lapDuration = currentTotal - prevLapTotal
            laps = laps + lapDuration
        }
    }

    val displayTime = formatTime(elapsedMs)

    // Pulse animation when running
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        1f, 0.3f,
        infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StandardScreenHeader(
                    title = if (timerMode == TimerMode.STOPWATCH) "Stopwatch" else "Timer",
                    subtitle = "Track elapsed time with laps. Runs in background with notification.",
                    icon = FieldMindIcons.Timer,
                    trailing = { BackButton(onClick = onBack) }
                )
            }

            // ── Timer mode toggle ──
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = timerMode == TimerMode.STOPWATCH,
                        onClick = { if (!isRunning) { timerMode = TimerMode.STOPWATCH; resetTimer() } },
                        label = { Text("Stopwatch") },
                        leadingIcon = { Icon(FieldMindIcons.Timer, null, size = 16.dp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    FilterChip(
                        selected = timerMode == TimerMode.TIMER,
                        onClick = { if (!isRunning) { timerMode = TimerMode.TIMER; resetTimer() } },
                        label = { Text("Timer") },
                        leadingIcon = { Icon(FieldMindIcons.Timer, null, size = 16.dp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            // ── Countdown setup (timer mode only, not running) ──
            if (timerMode == TimerMode.TIMER && !isRunning && !isPaused) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Set countdown time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Minutes
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Minutes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilledTonalIconButton(
                                            onClick = { if (countdownMinutes > 0) countdownMinutes-- },
                                            modifier = Modifier.size(40.dp)
                                        ) { Text("−", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                                        Text(
                                            "%02d".format(countdownMinutes),
                                            style = MaterialTheme.typography.displaySmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(60.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        FilledTonalIconButton(
                                            onClick = { if (countdownMinutes < 99) countdownMinutes++ },
                                            modifier = Modifier.size(40.dp)
                                        ) { Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                                    }
                                }
                                Text(":", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                // Seconds
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Seconds", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilledTonalIconButton(
                                            onClick = {
                                                if (countdownSeconds > 0) countdownSeconds--
                                                else if (countdownMinutes > 0) { countdownMinutes--; countdownSeconds = 59 }
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) { Text("−", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                                        Text(
                                            "%02d".format(countdownSeconds),
                                            style = MaterialTheme.typography.displaySmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(60.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        FilledTonalIconButton(
                                            onClick = {
                                                if (countdownSeconds < 59) countdownSeconds++
                                                else { countdownSeconds = 0; countdownMinutes++ }
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) { Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Timer display ──
            item {
                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            countdownFinished -> MaterialTheme.colorScheme.errorContainer
                            isRunning -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Status indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isRunning && !isPaused) {
                                Box(
                                    Modifier.size(10.dp)
                                        .clip(CircleShape)
                                        .graphicsLayer { alpha = pulseAlpha }
                                        .background(MaterialTheme.colorScheme.error)
                                )
                                Text(
                                    "Running",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else if (isPaused) {
                                Box(
                                    Modifier.size(10.dp)
                                        .clip(CircleShape)
                                        .background(colors.warning)
                                )
                                Text(
                                    "Paused",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.warning,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else if (countdownFinished) {
                                Text(
                                    "Time's up!",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    "Ready",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Time display
                        Text(
                            displayTime,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 64.sp
                            ),
                            color = when {
                                countdownFinished -> MaterialTheme.colorScheme.error
                                isRunning -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.graphicsLayer {
                                if (isRunning && !isPaused) {
                                    scaleX = 1f + (1f - pulseAlpha) * 0.02f
                                    scaleY = 1f + (1f - pulseAlpha) * 0.02f
                                }
                            }
                        )

                        if (isRunning && !isPaused && timerMode == TimerMode.STOPWATCH) {
                            Text(
                                "Tap Lap to record a split time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Control buttons
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isRunning || isPaused) {
                                // Start / Resume
                                FilledTonalButton(
                                    onClick = { startTimer() },
                                    modifier = Modifier.size(72.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = colors.positive.copy(alpha = 0.18f)
                                    )
                                ) {
                                    Icon(
                                        FieldMindIcons.Play,
                                        "Start",
                                        tint = colors.positive,
                                        size = 32.dp
                                    )
                                }
                            } else {
                                // Pause
                                FilledTonalButton(
                                    onClick = { pauseTimer() },
                                    modifier = Modifier.size(72.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = colors.warning.copy(alpha = 0.18f)
                                    )
                                ) {
                                    Icon(
                                        FieldMindIcons.Pause,
                                        "Pause",
                                        tint = colors.warning,
                                        size = 32.dp
                                    )
                                }
                            }

                            Spacer(Modifier.width(24.dp))

                            // Reset
                            FilledTonalButton(
                                onClick = { resetTimer() },
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                ),
                                enabled = isRunning || isPaused || countdownFinished
                            ) {
                                Icon(
                                    FieldMindIcons.Replay,
                                    "Reset",
                                    tint = MaterialTheme.colorScheme.error,
                                    size = 24.dp
                                )
                            }

                            Spacer(Modifier.width(24.dp))

                            // Lap (stopwatch only, when running)
                            FilledTonalButton(
                                onClick = { recordLap() },
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                enabled = isRunning && !isPaused && timerMode == TimerMode.STOPWATCH,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = colors.info.copy(alpha = 0.12f)
                                )
                            ) {
                                Icon(
                                    FieldMindIcons.Flag,
                                    "Lap",
                                    tint = colors.info,
                                    size = 24.dp
                                )
                            }
                        }
                    }
                }
            }

            // ── Laps list (stopwatch) ──
            if (laps.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Laps", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${laps.size} lap${if (laps.size != 1) "s" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            laps.reversed().forEachIndexed { index, lapMs ->
                                val lapNum = laps.size - index
                                val totalSec = lapMs / 1000
                                val minutes = totalSec / 60
                                val seconds = totalSec % 60
                                val millis = (lapMs % 1000) / 100
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            Modifier.size(28.dp)
                                                .clip(CircleShape)
                                                .background(colors.info.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "$lapNum",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.info
                                            )
                                        }
                                        Text(
                                            "Lap $lapNum",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        "%02d:%02d.%d".format(minutes, seconds, millis),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Background notification info ──
            if (isRunning) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.info.copy(alpha = 0.08f),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(FieldMindIcons.Info, null, tint = colors.info, size = 20.dp)
                            Column(Modifier.weight(1f)) {
                                Text("Running in background", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("Timer continues even if you leave this screen. Check notification for updates.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
