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
package com.buzbuz.smartautoclicker.core.common.actions.gesture

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import com.buzbuz.smartautoclicker.core.base.extensions.nextIntInOffset
import com.buzbuz.smartautoclicker.core.base.extensions.nextLongInOffset
import com.buzbuz.smartautoclicker.core.base.extensions.safeLineTo
import com.buzbuz.smartautoclicker.core.base.extensions.safeMoveTo
import com.buzbuz.smartautoclicker.core.common.actions.utils.MAXIMUM_STROKE_DURATION_MS
import com.buzbuz.smartautoclicker.core.common.actions.utils.MINIMUM_STROKE_DURATION_MS
import com.buzbuz.smartautoclicker.core.common.actions.utils.RANDOMIZATION_DURATION_MAX_OFFSET_MS
import com.buzbuz.smartautoclicker.core.common.actions.utils.RANDOMIZATION_POSITION_MAX_OFFSET_PX
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


/**
 * Move to [position], applying the anti-detection randomization offset (if [random] is provided).
 * @return the actual position that was set on the path, i.e. [position] after the randomization offset.
 */
fun Path.moveTo(position: Point, random: Random?): Point {
    val actualPosition = position.randomize(random)
    safeMoveTo(actualPosition.x, actualPosition.y)
    return actualPosition
}

/**
 * Draw a line from [from] to [to], applying the anti-detection randomization offset (if [random] is provided) on
 * both ends.
 * @return the actual from/to positions that were set on the path, after the randomization offset.
 */
fun Path.line(from: Point, to: Point, random: Random?): Pair<Point, Point> {
    val actualFrom = moveTo(from, random)
    val actualTo = lineTo(to, random)
    return actualFrom to actualTo
}

private fun Path.lineTo(position: Point, random: Random?): Point {
    val actualPosition = position.randomize(random)
    safeLineTo(actualPosition.x, actualPosition.y)
    return actualPosition
}

private fun Point.randomize(random: Random?): Point =
    if (random == null) this
    else Point(
        random.nextIntInOffset(x, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
        random.nextIntInOffset(y, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
    )

fun GestureDescription.Builder.buildSingleStroke(
    path: Path,
    durationMs: Long,
    startTime: Long = 0,
    random: Random?,
): GestureDescription {

    val actualDurationMs = random
        ?.nextLongInOffset(durationMs, RANDOMIZATION_DURATION_MAX_OFFSET_MS)
        ?: durationMs

    try {
        addStroke(
            GestureDescription.StrokeDescription(
                path,
                startTime.toNormalizedStrokeStartTime(),
                actualDurationMs.toNormalizedStrokeDurationMs(),
            )
        )
    } catch (ex: IllegalStateException) {
        throw IllegalStateException("Invalid gesture; Duration=$durationMs", ex)
    } catch (ex: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid gesture; Duration=$durationMs", ex)
    }

    return build()
}

private fun Long.toNormalizedStrokeStartTime(): Long =
    max(0, this)

private fun Long.toNormalizedStrokeDurationMs(): Long =
    max(MINIMUM_STROKE_DURATION_MS, min(MAXIMUM_STROKE_DURATION_MS, this))
