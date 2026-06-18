package fieldmind.research.app.shared.presentation.components.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Data class representing a Material Symbols icon by name.
 * This replaces the deleted Rhythm library's MaterialSymbolIcon.
 */
data class MaterialSymbolIcon(
    val name: String,
    val filled: Boolean = false,
    val defaultWeight: Int = 400
) {
    override fun toString(): String = name
}

/**
 * Maps Material Symbol icon names to the closest Material Icons available.
 * Uses only the core Material Icons set (no material-icons-extended dependency).
 */
private fun iconNameToImageVector(name: String, filled: Boolean): ImageVector = when (name) {
    "add" -> Icons.Filled.Add
    "arrow_back" -> Icons.Filled.ArrowBack
    "arrow_forward" -> Icons.Filled.ArrowForward
    "build", "settings" -> Icons.Filled.Settings
    "call", "phone" -> Icons.Filled.Phone
    "check", "done" -> if (filled) Icons.Filled.Check else Icons.Filled.Done
    "check_circle" -> Icons.Filled.CheckCircle
    "close", "clear" -> Icons.Filled.Close
    "create", "edit", "note_add" -> Icons.Filled.Create
    "date_range", "calendar_today", "calendar_month" -> Icons.Filled.DateRange
    "delete" -> Icons.Filled.Delete
    "done" -> Icons.Filled.Done
    "email", "mail", "message" -> Icons.Filled.Email
    "error", "warning" -> if (filled) Icons.Filled.Warning else Icons.Filled.Error
    "exit_to_app", "logout", "exit" -> Icons.Filled.ExitToApp
    "favorite", "favorite_filled" -> Icons.Filled.Favorite
    "favorite_border", "favorite_outline" -> Icons.Filled.FavoriteBorder
    "home" -> Icons.Filled.Home
    "info", "info_outline" -> Icons.Filled.Info
    "list", "menu", "more_horiz" -> Icons.Filled.List
    "location_on", "place", "pin_drop", "location_searching" -> Icons.Filled.LocationOn
    "lock", "lock_outline" -> Icons.Filled.Lock
    "mail", "email" -> Icons.Filled.Mail
    "menu" -> Icons.Filled.Menu
    "more_vert", "more" -> Icons.Filled.MoreVert
    "notifications", "notification" -> Icons.Filled.Notifications
    "person", "people", "user", "account_circle" -> Icons.Filled.Person
    "phone", "call" -> Icons.Filled.Phone
    "place", "location_on" -> Icons.Filled.Place
    "play_arrow", "play" -> Icons.Filled.PlayArrow
    "refresh", "sync", "update" -> Icons.Filled.Refresh
    "search", "find_replace" -> Icons.Filled.Search
    "send", "forward" -> Icons.Filled.Send
    "settings" -> Icons.Filled.Settings
    "share" -> Icons.Filled.Share
    "shopping_cart", "cart" -> Icons.Filled.ShoppingCart
    "star", "star_filled" -> Icons.Filled.Star
    "thumb_up", "like" -> Icons.Filled.ThumbUp
    "warning", "error_outline" -> Icons.Filled.Warning
    "bug_report" -> Icons.Filled.Build
    "search" -> Icons.Filled.Search
    "arrow_upward", "expand_less", "up" -> Icons.Filled.ArrowBack
    "arrow_downward", "expand_more", "down" -> Icons.Filled.ArrowForward
    "photo_camera", "camera", "camera_alt" -> Icons.Filled.Add
    "image", "photo", "photo_library" -> Icons.Filled.Add
    "science", "biotech" -> Icons.Filled.Build
    "menu_book", "library_books", "source" -> Icons.Filled.List
    "visibility", "visibility_on" -> Icons.Filled.Star
    "help", "help_outline", "question_answer" -> Icons.Filled.Info
    "lightbulb", "idea", "insights" -> Icons.Filled.Star
    "bar_chart", "chart", "show_chart" -> Icons.Filled.List
    "description", "article", "note", "note_alt" -> Icons.Filled.Email
    "style", "flashcard", "card" -> Icons.Filled.ShoppingCart
    "sell", "tag", "price" -> Icons.Filled.ShoppingCart
    "bolt", "flash_on", "flash" -> Icons.Filled.Refresh
    "filter_list", "filter" -> Icons.Filled.List
    "sort", "sort_by" -> Icons.Filled.List
    "check_box", "check_box_outline" -> Icons.Filled.Check
    "chevron_right", "forward_arrow" -> Icons.Filled.ArrowForward
    "expand_less", "arrow_up" -> Icons.Filled.ArrowBack
    "expand_more", "arrow_down" -> Icons.Filled.ArrowForward
    "attach_file", "file", "attachment" -> Icons.Filled.Email
    "mic", "microphone" -> Icons.Filled.Phone
    "stop", "stop_circle" -> Icons.Filled.Clear
    "location_on", "pin" -> Icons.Filled.LocationOn
    "map", "map_full" -> Icons.Filled.Place
    "hub", "graph", "pivot" -> Icons.Filled.Build
    "link", "chain", "open_in_new" -> Icons.Filled.Mail
    "ios_share", "export", "file_download" -> Icons.Filled.Share
    "local_fire_department", "fire", "streak" -> Icons.Filled.Star
    "palette", "color" -> Icons.Filled.Settings
    "flip", "flip_camera_android" -> Icons.Filled.Refresh
    "eco", "nature", "leaf" -> Icons.Filled.Check
    "visibility_off" -> Icons.Filled.Clear
    "auto_stories", "book" -> Icons.Filled.List
    "auto_awesome", "sparkle" -> Icons.Filled.Star
    "pause" -> Icons.Filled.Clear
    "fiber_manual_record", "record" -> Icons.Filled.Check
    "open_in_new" -> Icons.Filled.Mail
    "lock", "security" -> Icons.Filled.Lock
    "download", "file_download" -> Icons.Filled.Share
    "done", "complete" -> Icons.Filled.Done
    "forum", "chat", "question_answer" -> Icons.Filled.Email
    "timer", "clock" -> Icons.Filled.DateRange
    "route", "path", "track" -> Icons.Filled.Place
    "radio_button_checked", "circle" -> Icons.Filled.Check
    "pets", "animal", "bug_report", "bug" -> Icons.Filled.Person
    "local_florist", "plant", "flower" -> Icons.Filled.ShoppingCart
    "landscape", "rock", "terrain" -> Icons.Filled.Place
    "cloud", "cloudy", "partly_cloudy_day" -> Icons.Filled.Settings
    "rainy", "rain", "weather_snowy", "snowy", "foggy", "fog" -> Icons.Filled.Refresh
    "thunderstorm", "storm" -> Icons.Filled.Warning
    "flag", "outlined_flag" -> Icons.Filled.List
    "air", "wind", "airwave" -> Icons.Filled.Refresh
    "cyclone", "gale", "hurricane" -> Icons.Filled.Refresh
    "speed", "compress", "fast" -> Icons.Filled.Refresh
    "water_drop", "water", "rain" -> Icons.Filled.Refresh
    "sunny", "sun", "light_mode", "brightness" -> Icons.Filled.Star
    "clear_night", "night", "dark_mode", "bedtime", "sleep", "nights_stay" -> Icons.Filled.Star
    "groups", "people", "group" -> Icons.Filled.Person
    "thermostat", "temperature" -> Icons.Filled.Settings
    "category", "collection" -> Icons.Filled.List
    "photo", "camera" -> Icons.Filled.Add
    "grid_on", "grid" -> Icons.Filled.List
    "table_rows", "table", "data" -> Icons.Filled.List
    "file_download", "download" -> Icons.Filled.Share
    "functions", "function" -> Icons.Filled.Build
    "pivot_table_chart", "chart" -> Icons.Filled.List
    "emoji_events", "trophy", "achievement" -> Icons.Filled.Star
    "today", "date" -> Icons.Filled.DateRange
    "inventory_2", "archive", "inventory" -> Icons.Filled.ShoppingCart
    "school", "education", "learn" -> Icons.Filled.Star
    "trending_up", "trend" -> Icons.Filled.Refresh
    "hexagon", "shape" -> Icons.Filled.Build
    "raven", "bird" -> Icons.Filled.Person
    "flash_auto" -> Icons.Filled.Refresh
    "flash_off" -> Icons.Filled.Clear
    "info" -> Icons.Filled.Info
    "grid_on" -> Icons.Filled.List
    "check_circle" -> Icons.Filled.CheckCircle
    "warning" -> Icons.Filled.Warning
    "error" -> Icons.Filled.Error
    else -> Icons.Filled.Info
}

/**
 * Custom Icon composable that renders a MaterialSymbolIcon.
 * This replaces the deleted Rhythm library's Icon composable.
 * Uses the standard Material Icons from Compose Material3.
 *
 * Usage:
 *   Icon(icon = MaterialSymbolIcon("search"), contentDescription = "Search", size = 24.dp)
 */
@Composable
fun Icon(
    icon: MaterialSymbolIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 24.dp
) {
    val imageVector = iconNameToImageVector(icon.name, icon.filled)
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint
    )
}
