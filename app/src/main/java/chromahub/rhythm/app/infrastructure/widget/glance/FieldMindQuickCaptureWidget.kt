package fieldmind.research.app.infrastructure.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import fieldmind.research.app.activities.MainActivity
import fieldmind.research.app.R

/**
 * Quick Capture Widget — 2×1 cells
 * One-tap observation capture from the Android home screen.
 * Shows camera icon + "Quick Observe" + observation count.
 */
class FieldMindQuickCaptureWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val currentSize = LocalSize.current
            GlanceTheme {
                QuickCaptureUi(currentSize)
            }
        }
    }

    @Composable
    private fun QuickCaptureUi(size: DpSize) {
        val minWidth = size.width.value.toInt()

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(16.dp)
        ) {
            if (minWidth >= 180) {
                // Wide layout: icon + text + count
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(48.dp)
                            .background(GlanceTheme.colors.primaryContainer)
                            .cornerRadius(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_notification),
                            contentDescription = "Capture",
                            modifier = GlanceModifier.size(28.dp)
                        )
                    }
                    Spacer(GlanceModifier.width(12.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Quick Observe",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = "Tap to capture an observation",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                // Narrow layout: icon + text vertical
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(40.dp)
                            .background(GlanceTheme.colors.primaryContainer)
                            .cornerRadius(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_notification),
                            contentDescription = "Capture",
                            modifier = GlanceModifier.size(24.dp)
                        )
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    Text(
                        text = "Observe",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                }
            }
        }
    }
}
