package com.example.tsuyu

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private var ocrEngine: OnnxOcrEngine? = null

    // CanHub Cropper Result Handler & Full OCR Pipeline
    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val croppedImageUri = result.uriContent ?: return@registerForActivityResult
            Log.d("TsuyuCrop", "Crop successful: $croppedImageUri")

            // Full End-to-End Pipeline
            lifecycleScope.launch {
                try {
                    // 1. Load cropped URI into a Bitmap on Dispatchers.IO
                    val bitmap = withContext(Dispatchers.IO) {
                        contentResolver.openInputStream(croppedImageUri)?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        }
                    }

                    if (bitmap != null) {
                        // 2. Pass Bitmap to OnnxOcrEngine on Dispatchers.Default
                        val extractedText = withContext(Dispatchers.Default) {
                            val engine = ocrEngine ?: OnnxOcrEngine(this@MainActivity)
                            engine.extractText(bitmap)
                        }
                        bitmap.recycle() // Recycle buffer immediately after inference

                        Log.d("TsuyuOCR", "OCR Extracted Text: $extractedText")

                        // 3. Dispatch text on Dispatchers.Main
                        DispatchCoordinator.dispatch(this@MainActivity, extractedText)
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to load cropped image", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("TsuyuOCR", "OCR Pipeline failed", e)
                    Toast.makeText(this@MainActivity, "OCR Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val exception = result.error
            Log.e("TsuyuCrop", "Crop failed", exception)
            Toast.makeText(this, "Crop failed: ${exception?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Task 1.2: Universal Gallery Picker fallback
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d("Tsuyu", "Selected URI: $uri")
            launchCropper(uri)
        } else {
            Log.d("Tsuyu", "No media selected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ONNX Engine
        ocrEngine = OnnxOcrEngine(this)

        val btnPickImage = findViewById<Button>(R.id.btnPickImage)
        btnPickImage.setOnClickListener {
            // Launch the photo picker, allowing only images
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Settings Toggle for Dispatch Coordinator
        val switchFirefox = findViewById<SwitchCompat>(R.id.switchFirefox)
        val prefs = getSharedPreferences(DispatchCoordinator.PREFS_NAME, Context.MODE_PRIVATE)
        switchFirefox.isChecked = prefs.getBoolean(DispatchCoordinator.PREF_USE_FIREFOX, false)

        switchFirefox.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(DispatchCoordinator.PREF_USE_FIREFOX, isChecked).apply()
        }

        // Check if we were launched from the Accessibility Service
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrEngine?.close()
        ocrEngine = null
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val uriString = intent?.getStringExtra(EXTRA_CROP_URI)
        if (!uriString.isNullOrEmpty()) {
            intent.removeExtra(EXTRA_CROP_URI) // Consume it
            launchCropper(Uri.parse(uriString))
        }
    }

    private fun launchCropper(uri: Uri) {
        val options = CropImageContractOptions(uri, CropImageOptions().apply {
            guidelines = CropImageView.Guidelines.ON
            initialCropWindowPaddingRatio = 0.1f // Adds margin so handles don't overlap system bars
            fixAspectRatio = false
            outputCompressFormat = Bitmap.CompressFormat.PNG
        })
        cropImageLauncher.launch(options)
    }

    companion object {
        const val EXTRA_CROP_URI = "EXTRA_CROP_URI"
    }
}
