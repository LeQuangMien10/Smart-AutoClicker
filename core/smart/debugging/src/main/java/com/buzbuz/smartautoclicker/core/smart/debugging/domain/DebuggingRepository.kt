/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.smart.debugging.domain

import com.buzbuz.smartautoclicker.core.processing.domain.model.DebugGestureInfo
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportCounterInitialValue
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


interface DebuggingRepository {

    /** Tells if a detection session is currently live debugged, iow, displaying the debug overlay while detecting. */
    val isLiveDebugging: Flow<Boolean>

    /** Tells if a detection session is currently drawing the detection area boxes on top of the screen. */
    val isLiveConditionOverlay: Flow<Boolean>

    /** Tells if a detection session is currently drawing the gesture (click/swipe) feedback on top of the screen. */
    val isLiveGestureOverlay: Flow<Boolean>

    /** The last event that has been matched, with all interpreted conditions results. */
    val lastImageEventProcessed: Flow<DebugLiveEventOccurrence?>

    /** The last gesture (click or swipe) that has been executed. */
    val lastGestureExecuted: Flow<DebugGestureInfo?>

    /** Tells if a debug report is available. */
    val isDebugReportAvailable: StateFlow<Boolean>

    /** Tells if the debugging overlay should be enabled while detecting. */
    fun isDebugViewEnabled(): Boolean

    /** Tells if the detection area boxes should be drawn on top of the screen while detecting. */
    fun isConditionOverlayEnabled(): Boolean

    /** Tells if the gesture (click/swipe) feedback should be drawn on top of the screen while detecting. */
    fun isGestureOverlayEnabled(): Boolean

    /** Tells if a debug report should be created while detecting. */
    fun isDebugReportEnabled(): Boolean

    /** Replace the existing debug configuration settings with the provided ones. */
    fun setDebuggingConfig(debugView: Boolean, conditionOverlay: Boolean, gestureOverlay: Boolean, debugReport: Boolean)

    /** Read the last detection session report overview, if any. */
    fun getLastReportOverview(): Flow<DebugReportOverview?>

    /** Read the counters initial values from the last detection session report. Null if no report is available. */
    fun getLastReportCountersInitialValues(): Flow<List<DebugReportCounterInitialValue>?>

    /** Read the last detection session events occurrences. List will be empty no rapport is available. */
    fun getLastReportEventsOccurrences(): Flow<List<DebugReportEventOccurrence>?>
}