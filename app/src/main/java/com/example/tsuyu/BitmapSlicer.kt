package com.example.tsuyu

import android.graphics.Bitmap
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

object BitmapSlicer {

    suspend fun crop(source: Bitmap, rect: Rect): Bitmap? = withContext(Dispatchers.Default) {
        try {
            // Strict bounds checking to prevent IllegalArgumentException
            val left = max(0, rect.left)
            val top = max(0, rect.top)
            val right = min(source.width, rect.right)
            val bottom = min(source.height, rect.bottom)

            val width = right - left
            val height = bottom - top

            // If the user drags completely off-screen or makes a negative box, return null
            if (width <= 0 || height <= 0) {
                return@withContext null
            }

            Bitmap.createBitmap(source, left, top, width, height)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
