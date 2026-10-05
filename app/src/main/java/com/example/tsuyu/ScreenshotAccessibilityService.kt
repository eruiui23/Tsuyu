package com.example.tsuyu

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream

class ScreenshotAccessibilityService : AccessibilityService() {

    private var badgeManager: FloatingBadgeManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("TsuyuAccessibility", "Accessibility Service Connected")

        // Only show the badge on API 30+ since takeScreenshot is not available before
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            badgeManager = FloatingBadgeManager(this)
            badgeManager?.showBadge {
                captureScreen { bitmap ->
                    if (bitmap != null) {
                        Log.d("TsuyuCapture", "Screenshot saved to cache")
                        // In Milestone 2: We will launch CropActivity here using the cache URI
                    } else {
                        Log.e("TsuyuCapture", "Screenshot capture failed")
                    }
                }
            }
        } else {
            Log.w("TsuyuAccessibility", "Screenshot API requires Android 11+ (API 30+)")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun captureScreen(onResult: (Bitmap?) -> Unit) {
        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshotResult: ScreenshotResult) {
                    val hardwareBuffer = screenshotResult.hardwareBuffer
                    val colorSpace = screenshotResult.colorSpace
                    
                    try {
                        // HardwareBuffer from the OS needs to be wrapped, then copied to software format
                        val hwBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                        val softwareBitmap = hwBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                        
                        hardwareBuffer.close()

                        if (softwareBitmap != null) {
                            // Save to a temporary file in the cache directory to pass it safely
                            val file = File(cacheDir, "temp_screenshot.png")
                            FileOutputStream(file).use { out ->
                                softwareBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            onResult(softwareBitmap)
                        } else {
                            onResult(null)
                        }
                    } catch (e: Exception) {
                        Log.e("TsuyuCapture", "Failed to process screenshot buffer", e)
                        hardwareBuffer.close()
                        onResult(null)
                    }
                }

                override fun onFailure(errorCode: Int) {
                    Log.e("TsuyuCapture", "Screenshot failed with error code: $errorCode")
                    onResult(null)
                }
            }
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used, we only need the screenshot functionality
    }

    override fun onInterrupt() {
        // Not used
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("TsuyuAccessibility", "Accessibility Service Unbound")
        instance = null
        badgeManager?.hideBadge()
        badgeManager = null
        return super.onUnbind(intent)
    }

    companion object {
        var instance: ScreenshotAccessibilityService? = null
            private set
    }
}
