package com.example.tsuyu

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView

class MainActivity : AppCompatActivity() {

    // CanHub Cropper Result Handler
    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val croppedImageUri = result.uriContent
            Log.d("TsuyuCrop", "Crop successful: $croppedImageUri")
            Toast.makeText(this, "Crop completed", Toast.LENGTH_SHORT).show()
            // Kept ready for Milestone 3 (ONNX Inference)
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

        val btnPickImage = findViewById<Button>(R.id.btnPickImage)
        btnPickImage.setOnClickListener {
            // Launch the photo picker, allowing only images
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Retain Settings Toggle for Dispatch Coordinator
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
