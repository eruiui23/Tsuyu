package com.example.tsuyu

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

@SuppressLint("ViewConstructor")
class CropOverlayView(
    context: Context,
    private val onCropSelected: (Rect) -> Unit
) : View(context) {

    private val dimPaint = Paint().apply {
        color = Color.parseColor("#80000000") // Semi-transparent black dim
        style = Paint.Style.FILL
    }

    private val clearPaint = Paint().apply {
        // PorterDuff.Mode.CLEAR punches a hole through the view's canvas
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            2f,
            context.resources.displayMetrics
        )
    }

    private var startX = 0f
    private var startY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var isDrawing = false

    init {
        // Required for PorterDuff.Mode.CLEAR to compose correctly with the background
        // by rendering to an offscreen buffer first.
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Dim the entire screen
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        if (isDrawing) {
            val left = min(startX, currentX)
            val top = min(startY, currentY)
            val right = max(startX, currentX)
            val bottom = max(startY, currentY)

            // 2. Punch a transparent hole for the selected area
            canvas.drawRect(left, top, right, bottom, clearPaint)

            // 3. Draw a solid border around the selection
            canvas.drawRect(left, top, right, bottom, borderPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                currentX = event.x
                currentY = event.y
                isDrawing = true
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentX = event.x
                currentY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                currentX = event.x
                currentY = event.y
                isDrawing = false
                invalidate()

                // Normalize coordinates into an Android Rect
                val left = min(startX, currentX).toInt()
                val top = min(startY, currentY).toInt()
                val right = max(startX, currentX).toInt()
                val bottom = max(startY, currentY).toInt()

                val rectWidth = right - left
                val rectHeight = bottom - top

                // Validate minimum dimensions (e.g. > 10px) to ignore accidental taps
                if (rectWidth > 10 && rectHeight > 10) {
                    onCropSelected(Rect(left, top, right, bottom))
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
