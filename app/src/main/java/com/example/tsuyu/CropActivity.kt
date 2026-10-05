package com.example.tsuyu

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CropActivity : AppCompatActivity() {
    
    private lateinit var cropImageView: CropImageView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop)
        
        cropImageView = findViewById(R.id.cropImageView)
        
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val cachePath = intent.getStringExtra(EXTRA_CACHE_PATH)
        
        lifecycleScope.launch {
            val bitmap = loadBitmap(imageUriString, cachePath)
            if (bitmap != null) {
                cropImageView.bitmap = bitmap
            } else {
                Toast.makeText(this@CropActivity, "Failed to load image", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        
        cropImageView.onCropConfirmed = { croppedBitmap ->
            Log.d("TsuyuCrop", "Sliced bitmap: ${croppedBitmap.width}x${croppedBitmap.height}")
            Toast.makeText(this, "Cropped: ${croppedBitmap.width}x${croppedBitmap.height}", Toast.LENGTH_SHORT).show()
            
            // Keep in memory for Milestone 3 or handle directly
            // For now, we remain on screen waiting for the upcoming ONNX integration in MS3.
        }
    }
    
    private suspend fun loadBitmap(uriString: String?, path: String?): Bitmap? = withContext(Dispatchers.IO) {
        try {
            when {
                !uriString.isNullOrEmpty() -> {
                    val uri = Uri.parse(uriString)
                    contentResolver.openInputStream(uri)?.use { 
                        BitmapFactory.decodeStream(it)
                    }
                }
                !path.isNullOrEmpty() -> {
                    BitmapFactory.decodeFile(path)
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e("TsuyuCrop", "Error decoding bitmap", e)
            null
        }
    }
    
    companion object {
        const val EXTRA_IMAGE_URI = "EXTRA_IMAGE_URI"
        const val EXTRA_CACHE_PATH = "EXTRA_CACHE_PATH"
    }
}
