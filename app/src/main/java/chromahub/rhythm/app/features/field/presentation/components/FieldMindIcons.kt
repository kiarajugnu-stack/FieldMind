package fieldmind.research.app.features.field.presentation.components

import fieldmind.research.app.shared.presentation.components.icons.MaterialSymbolIcon

/**
 * Material Symbols icon set for FieldMind, rendered through the shared variable-font
 * [fieldmind.research.app.shared.presentation.components.icons.Icon] composable. Using real
 * symbols (instead of unicode glyph text) is the single biggest visual upgrade for the app.
 */
object FieldMindIcons {
    // Navigation (lifecycle tabs)
    val Today = MaterialSymbolIcon("today", defaultWeight = 500)
    val Capture = MaterialSymbolIcon("photo_camera", defaultWeight = 500)
    val Projects = MaterialSymbolIcon("science", defaultWeight = 500)
    val Library = MaterialSymbolIcon("menu_book", defaultWeight = 500)
    val Insights = MaterialSymbolIcon("insights", defaultWeight = 500)

    // Entity types
    val Observation = MaterialSymbolIcon("visibility")
    val Question = MaterialSymbolIcon("help")
    val Hypothesis = MaterialSymbolIcon("lightbulb")
    val Project = MaterialSymbolIcon("science")
    val Source = MaterialSymbolIcon("menu_book")
    val Data = MaterialSymbolIcon("bar_chart")
    val Report = MaterialSymbolIcon("description")
    val Flashcard = MaterialSymbolIcon("style")
    val Note = MaterialSymbolIcon("note_alt")
    val Tag = MaterialSymbolIcon("sell")

    // Actions
    val Add = MaterialSymbolIcon("add", defaultWeight = 500)
    val Bolt = MaterialSymbolIcon("bolt", filled = true, defaultWeight = 600)
    val Settings = MaterialSymbolIcon("settings")
    val Search = MaterialSymbolIcon("search")
    val Back = MaterialSymbolIcon("arrow_back")
    val Forward = MaterialSymbolIcon("chevron_right")
    val Up = MaterialSymbolIcon("expand_less")
    val Down = MaterialSymbolIcon("expand_more")
    val Camera = MaterialSymbolIcon("photo_camera")
    val Gallery = MaterialSymbolIcon("image")
    val File = MaterialSymbolIcon("attach_file")
    val Mic = MaterialSymbolIcon("mic", filled = true)
    val Stop = MaterialSymbolIcon("stop", filled = true)
    val Location = MaterialSymbolIcon("location_on")
    val Map = MaterialSymbolIcon("map")
    val Graph = MaterialSymbolIcon("hub")
    val Link = MaterialSymbolIcon("link")
    val Export = MaterialSymbolIcon("ios_share")
    val Streak = MaterialSymbolIcon("local_fire_department", filled = true)
    val Check = MaterialSymbolIcon("check_circle", filled = true)
    val Calendar = MaterialSymbolIcon("calendar_today")
    val Close = MaterialSymbolIcon("close")
    val Delete = MaterialSymbolIcon("delete")
    val Edit = MaterialSymbolIcon("edit")
    val Trend = MaterialSymbolIcon("trending_up")
    val School = MaterialSymbolIcon("school")
    val Archive = MaterialSymbolIcon("inventory_2")
    val Palette = MaterialSymbolIcon("palette")
    val Flip = MaterialSymbolIcon("flip")
    val Nature = MaterialSymbolIcon("eco")
    val Visibility = MaterialSymbolIcon("visibility")
    val VisibilityOff = MaterialSymbolIcon("visibility_off")
    val Book = MaterialSymbolIcon("auto_stories")
    val Article = MaterialSymbolIcon("article")
    val Sparkle = MaterialSymbolIcon("auto_awesome", filled = true)
    val Send = MaterialSymbolIcon("send", filled = true)
    val Play = MaterialSymbolIcon("play_arrow", filled = true)
    val Pause = MaterialSymbolIcon("pause", filled = true)
    val FiberManualRecord = MaterialSymbolIcon("fiber_manual_record", filled = true)
    val OpenLink = MaterialSymbolIcon("open_in_new")
    val Lock = MaterialSymbolIcon("lock")
    val Notifications = MaterialSymbolIcon("notifications")
    val Download = MaterialSymbolIcon("download")
    val Lightbulb = MaterialSymbolIcon("lightbulb")
    val Done = MaterialSymbolIcon("done")
    val Info = MaterialSymbolIcon("info")
    val Answer = MaterialSymbolIcon("question_answer")
    val MapFull = MaterialSymbolIcon("map", filled = true)
    val Favorite = MaterialSymbolIcon("favorite", filled = true)
    val FlashOn = MaterialSymbolIcon("flash_on", filled = true)
    val FlashAuto = MaterialSymbolIcon("flash_auto")
    val FlashOff = MaterialSymbolIcon("flash_off")
    val FlipCamera = MaterialSymbolIcon("flip_camera_android")
    val Timer = MaterialSymbolIcon("timer")
    val Session = MaterialSymbolIcon("stop_circle")

    // Observation category icons
    val Bird = MaterialSymbolIcon("raven")
    val Animal = MaterialSymbolIcon("pets")
    val Insect = MaterialSymbolIcon("bug_report")
    val Plant = MaterialSymbolIcon("local_florist")
    val Rock = MaterialSymbolIcon("landscape")
    val Weather = MaterialSymbolIcon("partly_cloudy_day")
    val Water = MaterialSymbolIcon("water_drop")
    val HumanBehavior = MaterialSymbolIcon("groups")
    val ReadingInsight = MaterialSymbolIcon("menu_book")
    val Category = MaterialSymbolIcon("category")

    /** Icon for an observation category (case-insensitive). Falls back to the observation icon. */
    fun iconForCategory(category: String): MaterialSymbolIcon = when (category.trim().lowercase()) {
        "bird", "birds" -> Bird
        "animal", "animals", "mammal" -> Animal
        "insect", "insects", "bug" -> Insect
        "plant", "plants", "flora", "fungus" -> Plant
        "rock", "rocks", "mineral", "geology" -> Rock
        "weather", "climate", "sky" -> Weather
        "water", "hydrology", "river", "lake" -> Water
        "human behavior", "human", "people", "social" -> HumanBehavior
        "reading insight", "reading", "insight" -> ReadingInsight
        "other" -> Category
        else -> Observation
    }

    /** Icon for an entity kind keyword (case-insensitive). */
    fun iconFor(kind: String): MaterialSymbolIcon = when (kind.trim().lowercase()) {
        "observation", "observations", "observe" -> Observation
        "question", "questions" -> Question
        "hypothesis", "hypotheses" -> Hypothesis
        "project", "projects" -> Project
        "source", "sources", "read", "reading", "library" -> Source
        "data", "data record", "datarecord" -> Data
        "report", "reports" -> Report
        "flashcard", "flashcards", "card", "cards" -> Flashcard
        "note", "notes" -> Note
        "tag", "tags" -> Tag
        else -> Nature
    }
}
