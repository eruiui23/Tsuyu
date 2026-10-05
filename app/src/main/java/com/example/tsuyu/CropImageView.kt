package com.example.tsuyu

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class CropImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var bitmap: Bitmap? = null
        set(value) {
            field = value
            updateMatrix()
            invalidate()
        }

    var onCropConfirmed: ((Bitmap) -> Unit)? = null

    private val imageMatrix = Matrix()
    private val inverseMatrix = Matrix()

    private val dimPaint = Paint().apply {
        color = Color.parseColor("#80000000")
        style = Paint.Style.FILL
    }

    private val clearPaint = Paint().apply {
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
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateMatrix()
    }

    private fun updateMatrix() {
        val bmp = bitmap ?: return
        val scaleX = width.toFloat() / bmp.width
        val scaleY = height.toFloat() / bmp.height
        val scale = min(scaleX, scaleY) // Center crop scale logic
        val dx = (width - bmp.width * scale) / 2f
        val dy = (height - bmp.height * scale) / 2f

        imageMatrix.apply {
            reset()
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        // Save the inverse mapping so touch coordinates map perfectly back to the raw pixels
        imageMatrix.invert(inverseMatrix)
    }

    override fun onDraw(canvas: Canvas) {
        val bmp = bitmap
        if (bmp != null) {
            canvas.drawBitmap(bmp, imageMatrix, null)
        }

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
            }
            MotionEvent.ACTION_MOVE -> {
                currentX = event.x
                currentY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                currentX = event.x
                currentY = event.y
                isDrawing = false
                invalidate()

                val left = min(startX, currentX)
                val top = min(startY, currentY)
                val right = max(startX, currentX)
                val bottom = max(startY, currentY)

                // Minimum 20px drag to count as a crop to prevent accidental taps
                if (right - left > 20 && bottom - top > 20) {
                    val bmp = bitmap
                    if (bmp != null) {
                        val viewRect = RectF(left, top, right, bottom)
                        val cropped = BitmapSlicer.cropWithMatrix(bmp, viewRect, inverseMatrix)
                        if (cropped != null) {
                            onCropConfirmed?.invoke(cropped)
                        }
                    }
                }
            }
        }
        return true
    }
}
