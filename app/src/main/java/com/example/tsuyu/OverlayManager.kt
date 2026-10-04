package com.example.tsuyu

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.WindowManager

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var cropOverlayView: CropOverlayView? = null

    fun showOverlay(onCropSelected: (Rect) -> Unit) {
        // Clean up any existing overlay before showing a new one
        hideOverlay()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        )

        cropOverlayView = CropOverlayView(context) { rect ->
            // Immediately clean up the screen when the crop is validated
            hideOverlay()
            // Pass coordinates back to caller (ScreenCaptureService)
            onCropSelected(rect)
        }

        windowManager.addView(cropOverlayView, params)
    }

    fun hideOverlay() {
        cropOverlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // Safely ignore if the view was already removed or detached
            }
            cropOverlayView = null
        }
    }
}
