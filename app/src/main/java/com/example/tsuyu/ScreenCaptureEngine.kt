package com.example.tsuyu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ScreenCaptureEngine(private val context: Context) {
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var activeContinuation: CancellableContinuation<Bitmap?>? = null

    private var width = 0
    private var height = 0

    fun start(mediaProjection: MediaProjection) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        
        width = metrics.widthPixels
        height = metrics.heightPixels
        val densityDpi = metrics.densityDpi

        // 1. Instantiate short-lived ImageReader ONCE
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        // 2. Continually listen for images but only process them if activeContinuation is set
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = try {
                reader.acquireLatestImage()
            } catch (e: Exception) {
                null
            }

            if (image != null) {
                val cont = activeContinuation
                if (cont != null && cont.isActive) {
                    activeContinuation = null
                    try {
                        val bitmap = processImage(image)
                        cont.resume(bitmap)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    } finally {
                        image.close()
                    }
                } else {
                    // Discard frame if no capture was requested to avoid filling the maxImages buffer
                    image.close()
                }
            }
        }, Handler(Looper.getMainLooper()))

        // 3. Create VirtualDisplay ONCE
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

    suspend fun captureFrame(): Bitmap? = withContext(Dispatchers.Default) {
        return@withContext suspendCancellableCoroutine { continuation ->
            // Register our intent to capture the next available frame
            activeContinuation = continuation
            
            // Safety measure: if the coroutine cancels before a frame arrives, clear the reference
            continuation.invokeOnCancellation {
                if (activeContinuation == continuation) {
                    activeContinuation = null
                }
            }
        }
    }

    fun stop() {
        virtualDisplay?.release()
        virtualDisplay = null
        
        imageReader?.close()
        imageReader = null
        
        activeContinuation?.cancel()
        activeContinuation = null
    }

    private fun processImage(image: Image): Bitmap {
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

        val cleanBitmap = Bitmap.createBitmap(tempBitmap, 0, 0, width, height)
        if (cleanBitmap != tempBitmap) {
            tempBitmap.recycle()
        }
        return cleanBitmap
    }
}
