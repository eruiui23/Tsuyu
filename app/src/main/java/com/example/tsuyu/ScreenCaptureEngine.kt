package com.example.tsuyu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ScreenCaptureEngine(private val context: Context) {

    suspend fun captureFrame(mediaProjection: MediaProjection): Bitmap? = withContext(Dispatchers.Default) {
        var virtualDisplay: VirtualDisplay? = null
        var imageReader: ImageReader? = null

        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            
            // Using real metrics to ensure we cover the entire physical screen including nav bars
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val densityDpi = metrics.densityDpi

            // Task 2.1: Instantiate short-lived ImageReader
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

            val bitmap = suspendCancellableCoroutine<Bitmap?> { continuation ->
                var isResumed = false

                // Task 2.4 / Asynchronous extraction: listen for the single latest frame
                imageReader?.setOnImageAvailableListener({ reader ->
                    if (isResumed) return@setOnImageAvailableListener
                    
                    val image = try {
                        reader.acquireLatestImage()
                    } catch (e: Exception) {
                        null
                    }

                    if (image != null) {
                        isResumed = true
                        try {
                            // Task 2.2: Implement plane buffer to Bitmap conversion with row stride/padding math
                            val plane = image.planes[0]
                            val buffer = plane.buffer
                            val pixelStride = plane.pixelStride
                            val rowStride = plane.rowStride
                            val rowPadding = rowStride - pixelStride * width

                            val tempBitmap = Bitmap.createBitmap(
                                width + rowPadding / pixelStride,
                                height,
                                Bitmap.Config.ARGB_8888
                            )
                            tempBitmap.copyPixelsFromBuffer(buffer)

                            // Extract the unpadded screen frame
                            val cleanBitmap = Bitmap.createBitmap(tempBitmap, 0, 0, width, height)
                            if (cleanBitmap != tempBitmap) {
                                tempBitmap.recycle()
                            }
                            
                            continuation.resume(cleanBitmap)
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        } finally {
                            image.close()
                        }
                    }
                }, Handler(Looper.getMainLooper())) // Handler on main looper so listener runs properly

                // Create VirtualDisplay using the MediaProjection token
                virtualDisplay = mediaProjection.createVirtualDisplay(
                    "TsuyuScreenCapture",
                    width,
                    height,
                    densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader?.surface,
                    null,
                    null
                )
            }

            return@withContext bitmap
        } finally {
            // Task 2.4: Clean up VirtualDisplay and ImageReader immediately after we acquire the frame
            virtualDisplay?.release()
            imageReader?.close()
        }
    }
}
