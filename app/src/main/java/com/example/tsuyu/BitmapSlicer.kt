package com.example.tsuyu

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

object BitmapSlicer {

    /**
     * Slices a Bitmap based on the user's view rectangle and the inverse transformation matrix
     * used to scale/translate the image in the CropImageView.
     */
    fun cropWithMatrix(source: Bitmap, viewRect: RectF, inverseMatrix: Matrix): Bitmap? {
        // Map the screen coordinate rectangle back to the original bitmap pixel coordinates
        val pts = floatArrayOf(viewRect.left, viewRect.top, viewRect.right, viewRect.bottom)
        inverseMatrix.mapPoints(pts)

        val mappedLeft = min(pts[0], pts[2]).toInt()
        val mappedTop = min(pts[1], pts[3]).toInt()
        val mappedRight = max(pts[0], pts[2]).toInt()
        val mappedBottom = max(pts[1], pts[3]).toInt()

        // Strict bounds checking to ensure we never throw IllegalArgumentException
        val cropLeft = max(0, mappedLeft)
        val cropTop = max(0, mappedTop)
        val cropRight = min(source.width, mappedRight)
        val cropBottom = min(source.height, mappedBottom)

        val cropWidth = cropRight - cropLeft
        val cropHeight = cropBottom - cropTop

        if (cropWidth <= 0 || cropHeight <= 0) {
            return null
        }

        return Bitmap.createBitmap(source, cropLeft, cropTop, cropWidth, cropHeight)
    }
}
