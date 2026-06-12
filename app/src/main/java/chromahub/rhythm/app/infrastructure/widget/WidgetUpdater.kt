/*
 *     Copyright (C) 2025 nift4
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package fieldmind.research.app.infrastructure.widget

import android.content.Context
import fieldmind.research.app.shared.data.model.Song
import fieldmind.research.app.infrastructure.widget.glance.GlanceWidgetUpdater

/**
 * Thin shim that forwards widget update calls to the Glance-based updater.
 * The legacy RemoteViews widget (MusicWidgetProvider) has been removed.
 */
object WidgetUpdater {

    fun updateWidget(
        context: Context,
        song: Song?,
        isPlaying: Boolean,
        hasPrevious: Boolean = false,
        hasNext: Boolean = false,
        isFavorite: Boolean = false
    ) {
        GlanceWidgetUpdater.updateWidget(context, song, isPlaying, hasPrevious, hasNext, isFavorite)
    }

    fun clearWidget(context: Context) {
        GlanceWidgetUpdater.updateWidgetEmpty(context)
    }
}
