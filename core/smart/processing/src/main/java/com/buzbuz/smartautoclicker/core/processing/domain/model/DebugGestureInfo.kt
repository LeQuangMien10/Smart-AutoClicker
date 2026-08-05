/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.core.processing.domain.model

import android.graphics.Point

/** Info about a gesture (click or swipe) that has just been dispatched to the device, provided for live debugging. */
sealed interface DebugGestureInfo {

    /** The duration of the gesture in milliseconds, used to know how long the visual feedback should stay on screen. */
    val durationMs: Long

    /**
     * The time at which the gesture was dispatched, in milliseconds.
     * This is only used to make each instance distinct even when the position/duration are identical to the
     * previous one (e.g. an event clicking repeatedly at the same spot): without it, the backing StateFlow would
     * consider the new value equal to the previous one and silently skip the emission, keeping the visual feedback
     * stuck on its very first display.
     */
    val timestamp: Long

    data class Click(val position: Point, override val durationMs: Long, override val timestamp: Long) : DebugGestureInfo

    data class Swipe(val from: Point, val to: Point, override val durationMs: Long, override val timestamp: Long) : DebugGestureInfo
}
