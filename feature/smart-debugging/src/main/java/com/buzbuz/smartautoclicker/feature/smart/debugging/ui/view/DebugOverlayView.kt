package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.view.View

import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.uistate.ScreenConditionResultUiState

/**
 * Displays a rectangle at the selected position to represents the detection, as well as a small dot/line to
 * represent the last click/swipe gesture executed.
 * @param context the Android context.
 */
class DebugOverlayView(context: Context) : View(context) {

    private val positiveResultPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }
    private val negativeResultPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    /** The margin between the actual condition position and the displayed borders. */
    private val conditionBordersMargin = 10

    private val results: MutableList<ScreenConditionResultUiState> = mutableListOf()
    private val displayedResults: MutableList<Pair<Paint, Rect>> = mutableListOf()

    /** Paint for the gesture feedback (click dot / swipe line). Kept small and thin on purpose, see [GESTURE_DOT_RADIUS]. */
    private val gesturePaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = GESTURE_LINE_WIDTH
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val gestureDotPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    /** Black outline drawn behind [gestureCoordinatesTextPaint] so the text stays readable on any background. */
    private val gestureCoordinatesTextOutlinePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = GESTURE_COORDINATES_TEXT_OUTLINE_WIDTH
        textSize = GESTURE_COORDINATES_TEXT_SIZE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val gestureCoordinatesTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = GESTURE_COORDINATES_TEXT_SIZE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private var gestureVisual: GestureVisual? = null

    fun setResults(newResults: List<ScreenConditionResultUiState>) {
        updateResults(newResults)
        postInvalidate()
    }

    fun clear() {
        updateResults(emptyList())
        postInvalidate()
    }

    /** Show a dot at [position] to represent a click gesture being executed. */
    fun setGestureClick(position: Point) {
        gestureVisual = GestureVisual.Dot(position)
        postInvalidate()
    }

    /** Show a line from [from] to [to] to represent a swipe gesture being executed. */
    fun setGestureSwipe(from: Point, to: Point) {
        gestureVisual = GestureVisual.Line(from, to)
        postInvalidate()
    }

    /** Remove the gesture feedback from the screen. */
    fun clearGesture() {
        gestureVisual = null
        postInvalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateResults(results)
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        displayedResults.forEach { (paint, coordinates) ->
            canvas.drawRect(coordinates, paint)
        }

        when (val visual = gestureVisual) {
            is GestureVisual.Dot -> {
                val x = visual.position.x.toFloat()
                val y = visual.position.y.toFloat()
                canvas.drawCircle(x, y, GESTURE_DOT_RADIUS, gestureDotPaint)
                canvas.drawGestureCoordinates(visual.position, x, y - GESTURE_DOT_RADIUS - GESTURE_COORDINATES_TEXT_MARGIN)
            }

            is GestureVisual.Line ->
                canvas.drawLine(
                    visual.from.x.toFloat(), visual.from.y.toFloat(),
                    visual.to.x.toFloat(), visual.to.y.toFloat(),
                    gesturePaint,
                )

            null -> Unit
        }
    }

    /** Draw the [position] coordinates centered above ([x], [y]), with a black outline for readability. */
    private fun Canvas.drawGestureCoordinates(position: Point, x: Float, y: Float) {
        val text = "(${position.x}, ${position.y})"
        drawText(text, x, y, gestureCoordinatesTextOutlinePaint)
        drawText(text, x, y, gestureCoordinatesTextPaint)
    }

    private fun updateResults(newResults: List<ScreenConditionResultUiState>) {
        if (results != newResults) {
            results.clear()
            results.addAll(newResults)
        }
        displayedResults.clear()

        // No condition matched ? Nothing to display
        if (results.isEmpty()) {
            return
        }

        results.forEach { result -> displayedResults.add(result.toDisplayResult()) }
    }

    private fun ScreenConditionResultUiState.toDisplayResult(): Pair<Paint, Rect> = Pair(
        if (positive) positiveResultPaint else negativeResultPaint,
        Rect(
            coordinates.left - conditionBordersMargin,
            coordinates.top - conditionBordersMargin,
            coordinates.right + conditionBordersMargin,
            coordinates.bottom + conditionBordersMargin,
        )
    )

    private sealed interface GestureVisual {
        data class Dot(val position: Point) : GestureVisual
        data class Line(val from: Point, val to: Point) : GestureVisual
    }
}

/** Radius in pixels of the dot representing a click. Kept small so it does not hide the condition below it. */
private const val GESTURE_DOT_RADIUS = 14f
/** Width in pixels of the line representing a swipe. Kept thin so it does not hide the condition below it. */
private const val GESTURE_LINE_WIDTH = 8f
/** Text size in pixels for the coordinates label shown above the click dot. Kept close to the dot's own diameter. */
private const val GESTURE_COORDINATES_TEXT_SIZE = GESTURE_DOT_RADIUS * 2f * 0.6f
/** Width in pixels of the black outline behind the coordinates label. */
private const val GESTURE_COORDINATES_TEXT_OUTLINE_WIDTH = 3f
/** Vertical gap in pixels between the click dot and its coordinates label. */
private const val GESTURE_COORDINATES_TEXT_MARGIN = 6f