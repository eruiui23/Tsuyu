package com.example.tsuyu

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    // Task 1.2: Universal Gallery Picker fallback
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d("Tsuyu", "Selected URI: $uri")
            Toast.makeText(this, "Image selected, ready for crop", Toast.LENGTH_SHORT).show()
            // Next step: Pass this URI to CropActivity
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
    }
}
