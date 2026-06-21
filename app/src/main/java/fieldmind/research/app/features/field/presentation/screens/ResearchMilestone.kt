package fieldmind.research.app.features.field.presentation.screens

import fieldmind.research.app.features.field.data.learn.LearnResource
import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

/**
 * Represents a research milestone in the learning journey.
 * Used by [FieldMindLearnScreen] and [FieldMindLibraryScreen] for their
 * recommended-next-step hero sections.
 */
data class ResearchMilestone(
    val title: String,
    val body: String,
    val icon: MaterialSymbolIcon,
    val resource: LearnResource
)
