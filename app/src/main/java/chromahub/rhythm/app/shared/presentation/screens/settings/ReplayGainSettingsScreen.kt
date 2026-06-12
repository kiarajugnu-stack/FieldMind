@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package fieldmind.research.app.shared.presentation.screens.settings

import fieldmind.research.app.shared.presentation.components.icons.RhythmIcons
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon
import fieldmind.research.app.shared.presentation.components.icons.Icon

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fieldmind.research.app.R
import fieldmind.research.app.shared.data.model.AppSettings
import fieldmind.research.app.util.HapticUtils
import fieldmind.research.app.util.HapticType
import fieldmind.research.app.shared.presentation.components.common.CollapsibleHeaderScreen
import fieldmind.research.app.shared.presentation.components.common.ExpressiveButtonGroup
import fieldmind.research.app.shared.presentation.components.Material3SettingsGroup
import fieldmind.research.app.shared.presentation.components.Material3SettingsItem
import fieldmind.research.app.shared.presentation.screens.settings.TunerAnimatedSwitch

@Composable
fun ReplayGainSettingsScreen(
    onBackClick: () -> Unit,
    onNavigateTo: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val haptic = LocalHapticFeedback.current

    val replayGain by appSettings.replayGain.collectAsState()
    val replayGainMode by appSettings.replayGainMode.collectAsState()
    val replayGainDrc by appSettings.replayGainDrc.collectAsState()
    val replayGainPreamp by appSettings.replayGainPreamp.collectAsState()
    val replayGainPreampUntagged by appSettings.replayGainPreampUntagged.collectAsState()

    CollapsibleHeaderScreen(
        title = context.getString(R.string.replay_gain),
        showBackButton = true,
        onBackClick = onBackClick,
        headerContent = {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (replayGain)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = MaterialSymbolIcon("volume_up"),
                        contentDescription = null,
                        tint = if (replayGain) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(35.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (replayGain) "Active" else "Disabled",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TunerAnimatedSwitch(
                        checked = replayGain,
                        onCheckedChange = { enabled ->
                            HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                            appSettings.setReplayGain(enabled)
                        }
                    )
                }
            }
        }
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {
            item(key = "replay_gain_controls") {
                AnimatedVisibility(
                    visible = replayGain,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val settingItems = listOf(
                        Material3SettingsItem(
                            icon = MaterialSymbolIcon("music_note"),
                            title = { Text("ReplayGain Mode") },
                            description = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Adjust volume levels based on track metadata or album metadata.")
                                    Spacer(modifier = Modifier.height(10.dp))
                                    ExpressiveButtonGroup(
                                        items = listOf("Track", "Album"),
                                        selectedIndex = if (replayGainMode == 2) 1 else 0,
                                        onItemClick = { index ->
                                            HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                            appSettings.setReplayGainMode(if (index == 1) 2 else 1)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        ),
                        Material3SettingsItem(
                            icon = MaterialSymbolIcon("graphic_eq"),
                            title = { Text("Prevent Clipping (DRC)") },
                            description = {
                                Text("Dynamically compress peak levels to prevent audio distortion.")
                            },
                            trailingContent = {
                                TunerAnimatedSwitch(
                                    checked = replayGainDrc,
                                    onCheckedChange = {
                                        HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                        appSettings.setReplayGainDrc(it)
                                    }
                                )
                            },
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                appSettings.setReplayGainDrc(!replayGainDrc)
                            }
                        ),
                        Material3SettingsItem(
                            icon = MaterialSymbolIcon("volume_up"),
                            title = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Pre-amplifier (Tagged)")
                                    Text(
                                        text = "${replayGainPreamp.toInt()} dB",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            description = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Volume adjustment for tracks with ReplayGain tags.")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Slider(
                                        value = replayGainPreamp,
                                        onValueChange = { appSettings.setReplayGainPreamp(it) },
                                        valueRange = -15f..15f,
                                        steps = 30,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        ),
                        Material3SettingsItem(
                            icon = MaterialSymbolIcon("volume_down"),
                            title = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Pre-amplifier (Untagged)")
                                    Text(
                                        text = "${replayGainPreampUntagged.toInt()} dB",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            description = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Volume adjustment for tracks without ReplayGain tags.")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Slider(
                                        value = replayGainPreampUntagged,
                                        onValueChange = { appSettings.setReplayGainPreampUntagged(it) },
                                        valueRange = -15f..15f,
                                        steps = 30,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        )
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Material3SettingsGroup(
                            title = "Configuration",
                            items = settingItems,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
