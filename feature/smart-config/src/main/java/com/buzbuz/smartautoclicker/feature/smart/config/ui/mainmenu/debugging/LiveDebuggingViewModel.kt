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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.mainmenu.debugging

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import com.buzbuz.smartautoclicker.core.domain.model.action.Action

import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.processing.domain.model.DebugGestureInfo
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveEventConditionResult
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live.DebugLiveEventOccurrence
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.usecase.GetDebugLiveDetectionResultUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.uistate.ScreenConditionResultUiState

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject
import kotlin.collections.toMutableList
import kotlin.time.Duration.Companion.milliseconds

class LiveDebuggingViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val debuggingRepository: DebuggingRepository,
    debugDetectionResultUseCase: GetDebugLiveDetectionResultUseCase,
) : ViewModel() {

    /**
     * Tells if the detection area / gesture overlay window is needed right now, synchronously. Used to decide, at
     * menu creation, whether the screen overlay window used to draw them should be attached from the start: when
     * both are disabled (the common case), no such window should exist on screen, as it is an extra layer
     * composited on every frame captured by MediaProjection for detection. The "show debug view" text panel is not
     * part of this window (it lives inside the menu's own view), so it is intentionally not checked here.
     */
    fun isConditionOrGestureOverlayEnabled(): Boolean =
        debuggingRepository.isConditionOverlayEnabled() || debuggingRepository.isGestureOverlayEnabled()

    /** Tells if the current detection is running in debug mode. */
    val isDebugging = debuggingRepository.isLiveDebugging

    /** Tells if the detection area overlay should currently be drawn on screen. Independent of [isDebugging]. */
    val isConditionOverlayEnabled = debuggingRepository.isLiveConditionOverlay

    /** The info on the last positive detection. */
    val debugLastPositive: Flow<LiveDebuggingUiState?> = debugDetectionResultUseCase
        .invoke(minDisplayDuration = POSITIVE_VALUE_DISPLAY_TIMEOUT_MS)
        .combine(isDebugging) { results, isDebugging -> if (isDebugging) results else null }
        .map { result -> result?.toLastPositiveDebugInfo(context) }

    /**
     * The screen areas verified for the last event occurrence, used to draw the detection bounding boxes.
     * Gated on [isConditionOverlayEnabled] (a setting separate from [isDebugging]) and kept on screen for a much
     * shorter duration than [debugLastPositive]: these boxes are drawn on top of the screen that MediaProjection
     * also captures for the next detection pass, so leaving them up too long risks the box pixels themselves being
     * captured and interfering with conditions located nearby.
     */
    val debugConditionAreas: Flow<List<ScreenConditionResultUiState>> = debugDetectionResultUseCase
        .invoke(minDisplayDuration = CONDITION_AREA_DISPLAY_TIMEOUT_MS)
        .combine(isConditionOverlayEnabled) { results, isEnabled -> if (isEnabled) results else null }
        .map { result -> result?.conditionsResults?.toConditionAreaResults() ?: emptyList() }

    /** Tells if the gesture (click/swipe) overlay should currently be drawn on screen. Independent of the others. */
    val isGestureOverlayEnabled = debuggingRepository.isLiveGestureOverlay

    /**
     * The last gesture (click/swipe) that has been executed, shown for the duration of the gesture itself (with a
     * minimum so very short gestures are still visible), then cleared automatically.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val debugGesture: Flow<DebugGestureInfo?> = debuggingRepository.lastGestureExecuted
        .combine(isGestureOverlayEnabled) { gesture, isEnabled -> if (isEnabled) gesture else null }
        .transformLatest { gesture ->
            if (gesture == null) {
                emit(null)
                return@transformLatest
            }

            emit(gesture)
            delay(gesture.durationMs.coerceAtLeast(MIN_GESTURE_DISPLAY_MS))
            emit(null)
        }

    /**
     * Tells if the screen overlay window (used to draw the detection area boxes and/or the gesture feedback)
     * should currently be attached. Unlike [isConditionOrGestureOverlayEnabled], this reacts live to both the
     * settings and the detecting session state, so the window gets created/destroyed as needed even if a setting
     * is toggled on after the menu displaying it was already created.
     */
    val isAnyOverlayActive: Flow<Boolean> =
        combine(isConditionOverlayEnabled, isGestureOverlayEnabled) { condition, gesture -> condition || gesture }

}

private fun DebugLiveEventOccurrence.toLastPositiveDebugInfo(context: Context): LiveDebuggingUiState =
    LiveDebuggingUiState(
        eventIcon = event.getDebugIcon(),
        eventName = event.name,
        eventFulfilledCount = fulfilledCount.toString(),
        eventDuration = context.getDurationText(processingDurationMs),
        actions = event.actions.toActionItems(),
    )

private fun List<DebugLiveEventConditionResult>.toConditionAreaResults(): List<ScreenConditionResultUiState> =
    filterIsInstance<DebugLiveEventConditionResult.Screen>()
        .mapNotNull { result ->
            val area = result.detectionArea ?: return@mapNotNull null
            ScreenConditionResultUiState(
                positive = result.isFulfilled,
                coordinates = area,
                confidenceRate = result.confidenceRate,
            )
        }

private fun List<Action>.toActionItems(): List<LiveDebuggingActionsItem> =
    if (size <= 5) map { action -> LiveDebuggingActionsItem(action.getIconRes()) }
    else subList(0, 4)
        .map { action -> LiveDebuggingActionsItem(action.getIconRes()) }
        .toMutableList()
        .apply { add(LiveDebuggingActionsItem(R.drawable.ic_more)) }

@DrawableRes
private fun Event.getDebugIcon(): Int =
    when (this) {
        is ScreenEvent -> R.drawable.ic_condition
        is TriggerEvent -> R.drawable.ic_trigger_event
    }

private fun Context.getDurationText(durationMs: Long): String =
    "$durationMs${getString(R.string.dropdown_label_time_unit_ms)}"

/** Delay before removing the last positive result display in debug. */
private val POSITIVE_VALUE_DISPLAY_TIMEOUT_MS = 1500.milliseconds

/** Delay before removing the detection area bounding boxes from the screen. Kept short, see [LiveDebuggingViewModel.debugConditionAreas]. */
private val CONDITION_AREA_DISPLAY_TIMEOUT_MS = 700.milliseconds

/**
 * Minimum display duration for the gesture feedback (dot/line and its coordinates label), so very fast gestures
 * stay up long enough to actually be read.
 */
private const val MIN_GESTURE_DISPLAY_MS = 600L