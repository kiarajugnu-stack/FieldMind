package fieldmind.research.app.features.field.presentation.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fieldmind.research.app.shared.presentation.components.icons.Icon
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

/**
 * Shows a dialog when GPS is off, prompting the user to enable Location in system settings.
 *
 * Usage:
 *   val showGpsDialog = remember { mutableStateOf(false) }
 *   if (showGpsDialog.value) {
 *       GpsOffDialog(onDismiss = { showGpsDialog.value = false })
 *   }
 */
@Composable
fun GpsOffDialog(
    onDismiss: () -> Unit,
    /** null = opens system Location Settings automatically */
    onOpenSettings: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val effectiveOnOpenSettings = remember(context) {
        onOpenSettings ?: {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon = MaterialSymbolIcon("location_off", filled = true),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        size = 22.dp
                    )
                }
                Text("Location is off", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "GPS is currently disabled on your device. Enable location services to auto-tag coordinates, fetch weather, and log observation sites.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                Text(
                    "You can also continue without GPS — location will be missing from new observations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    effectiveOnOpenSettings()
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    icon = MaterialSymbolIcon("settings", filled = true),
                    contentDescription = null,
                    size = 18.dp
                )
                Spacer(Modifier.size(6.dp))
                Text("Open Location Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Continue without GPS")
            }
        }
    )
}

