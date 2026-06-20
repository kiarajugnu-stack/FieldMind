package fieldmind.research.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material Design 3 Expressive Shape System
// Uses more organic, rounded shapes for expressive design language
val Shapes = Shapes(
    // Extra small components (small chips, badges)
    extraSmall = RoundedCornerShape(8.dp),
    
    // Small components (chips, small buttons)
    small = RoundedCornerShape(12.dp),
    
    // Medium components (buttons, cards, FABs)
    medium = RoundedCornerShape(16.dp),
    
    // Large components (sheets, dialogs, large cards)
    large = RoundedCornerShape(24.dp),
    
    // Extra large components (full-width modals, prominent surfaces)
    extraLarge = RoundedCornerShape(32.dp)
)


