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

/**
 * Research Dashboard Widget — 4×3 cells
 * Overview of active research: observation count, open questions, active projects, session status.
 */
class FieldMindDashboardWidget : GlanceAppWidget() {

    companion object {
        const val KEY_OBSERVATION_COUNT = "observation_count"
        const val KEY_NOTE_COUNT = "note_count"
        const val KEY_QUESTION_COUNT = "question_count"
        const val KEY_PROJECT_COUNT = "project_count"
        const val KEY_SOURCE_COUNT = "source_count"
        const val KEY_REPORT_COUNT = "report_count"
        const val KEY_SESSION_ACTIVE = "session_active"
        const val KEY_SESSION_MINUTES = "session_minutes"
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val currentSize = LocalSize.current
            GlanceTheme {
                DashboardUi(currentSize)
            }
        }
    }

    @Composable
    private fun DashboardUi(size: DpSize) {
        val minWidth = size.width.value.toInt()

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(18.dp)
        ) {
            if (minWidth >= 250) {
                // Wide layout: header + stats grid + actions
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = GlanceModifier
                                .size(36.dp)
                                .background(GlanceTheme.colors.primaryContainer)
                                .cornerRadius(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(fieldmind.research.app.R.drawable.ic_notification),
                                contentDescription = null,
                                modifier = GlanceModifier.size(22.dp)
                            )
                        }
                        Spacer(GlanceModifier.width(10.dp))
                        Text(
                            text = "FieldMind Research",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                    }

                    Spacer(GlanceModifier.height(14.dp))

                    // Stats grid
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        StatItem("Observations", "0", GlanceTheme.colors.primary, GlanceModifier.defaultWeight())
                        Spacer(GlanceModifier.width(8.dp))
                        StatItem("Questions", "0", GlanceTheme.colors.tertiary, GlanceModifier.defaultWeight())
                    }

                    Spacer(GlanceModifier.height(8.dp))

                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        StatItem("Projects", "0", GlanceTheme.colors.secondary, GlanceModifier.defaultWeight())
                        Spacer(GlanceModifier.width(8.dp))
                        StatItem("Sources", "0", GlanceTheme.colors.error, GlanceModifier.defaultWeight())
                    }

                    Spacer(GlanceModifier.height(8.dp))

                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        StatItem("Notes", "0", GlanceTheme.colors.tertiary, GlanceModifier.defaultWeight())
                        Spacer(GlanceModifier.width(8.dp))
                        StatItem("Reports", "0", GlanceTheme.colors.secondary, GlanceModifier.defaultWeight())
                    }

                    Spacer(GlanceModifier.defaultWeight())

                    // Session status
                    Text(
                        text = "Tap to open FieldMind",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            } else {
                // Narrow layout: vertical stats
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Text(
                        text = "🔬 FieldMind",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                    Spacer(GlanceModifier.height(10.dp))

                    CompactStat("📷", "Observations", "0")
                    CompactStat("❓", "Questions", "0")
                    CompactStat("📊", "Projects", "0")
                    CompactStat("📝", "Notes", "0")

                    Spacer(GlanceModifier.defaultWeight())

                    Text(
                        text = "Tap to open",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun StatItem(label: String, count: String, accent: ColorProvider, modifier: GlanceModifier) {
        Column(
            modifier = modifier
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(12.dp)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 11.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
    }

    @Composable
    private fun CompactStat(emoji: String, label: String, count: String) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, style = TextStyle(fontSize = 14.sp))
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 13.sp,
                    color = GlanceTheme.colors.onSurface
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = count,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.primary
                )
            )
        }
    }
}
