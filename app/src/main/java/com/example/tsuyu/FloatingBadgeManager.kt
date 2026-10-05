package com.example.tsuyu

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import kotlin.math.abs

class FloatingBadgeManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var badgeView: View? = null
    private var params: WindowManager.LayoutParams? = null

    @SuppressLint("ClickableViewAccessibility")
    fun showBadge(onBadgeClicked: () -> Unit) {
        if (badgeView != null) return

        val size = (56 * context.resources.displayMetrics.density).toInt()

        val imageView = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_camera)
            setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent dark bg
            setPadding(24, 24, 24, 24)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100 // initial X offset
            y = 300 // initial Y offset
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        imageView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params!!.x
                    initialY = params!!.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params!!.x = initialX + dx.toInt()
                        params!!.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(imageView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        onBadgeClicked()
                    }
                    true
                }
                else -> false
            }
        }

        badgeView = imageView
        windowManager.addView(badgeView, params)
    }

    fun hideBadge() {
        badgeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Ignore if already removed by system
            }
            badgeView = null
        }
    }
}
