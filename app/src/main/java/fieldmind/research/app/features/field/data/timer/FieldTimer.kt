package fieldmind.research.app.features.field.data.timer

/**
 * A simple behavioral observation timer with lap recording.
 * Used for timed behavioral observations where you track
 * durations of specific behaviors.
 */
class FieldTimer {
    private var _elapsedMs: Long = 0L
    val elapsedMs: Long get() = if (_running) System.currentTimeMillis() - _startTime else _elapsedMs

    private var _running: Boolean = false
    val running: Boolean get() = _running

    private var _laps: MutableList<Long> = mutableListOf()
    val laps: List<Long> get() = _laps.toList()

    private var _startTime: Long = 0L

    fun start() {
        if (_running) return
        _running = true
        _startTime = System.currentTimeMillis() - _elapsedMs
    }

    fun pause() {
        if (!_running) return
        _running = false
        _elapsedMs = System.currentTimeMillis() - _startTime
    }

    fun reset() {
        _running = false
        _elapsedMs = 0L
        _laps.clear()
    }

    fun lap() {
        if (!_running) return
        val now = System.currentTimeMillis()
        val currentLap = now - _startTime
        _elapsedMs = currentLap
        _laps.add(currentLap)
        _startTime = now
    }

    fun formattedTime(): String {
        val totalMs = elapsedMs
        val totalSec = totalMs / 1000
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        val millis = (totalMs % 1000) / 100
        return "%02d:%02d.%d".format(minutes, seconds, millis)
    }

    fun formattedLap(index: Int): String {
        if (index < 0 || index >= _laps.size) return ""
        val lapMs = _laps[index]
        val sec = lapMs / 1000
        val millis = (lapMs % 1000) / 100
        return "Lap %d: %02d:%02d.%d".format(index + 1, sec / 60, sec % 60, millis)
    }

    /** Returns elapsed time formatted as readable text for saving to an observation. */
    fun toDataValue(): String = formattedTime()

    /** Returns all laps as a semicolon-separated string for storage. */
    fun lapsAsData(): String = _laps.joinToString(";") { lapMs ->
        "%02d:%02d.%d".format(lapMs / 60000, (lapMs % 60000) / 1000, (lapMs % 1000) / 100)
    }
}
