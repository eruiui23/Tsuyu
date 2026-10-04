package com.example.tsuyu

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class JapaneseOcrEngine {

    // Initialize the ML Kit client for Japanese text recognition
    private val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    suspend fun extractText(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)

        // Bridge the ML Kit Task API to Kotlin Coroutines safely
        val rawText = suspendCancellableCoroutine<String> { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (continuation.isActive) {
                        continuation.resume(visionText.text)
                    }
                }
                .addOnFailureListener { e ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
        }

        // Task 4.3: Text Normalization
        // Strip out all newline characters (\n and \r) and trim leading/trailing whitespace.
        // This merges multiple vertical lines of manga text into a single horizontal clean string.
        rawText
            .replace("\n", "")
            .replace("\r", "")
            .trim()
    }
}
