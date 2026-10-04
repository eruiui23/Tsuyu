package com.example.tsuyu


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
	private lateinit var mediaProjectionManager: MediaProjectionManager
	private val notificationPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { isGranted ->
		if (isGranted) {
			checkOverlayPermissionThenStart()
		} else {
			Toast.makeText(this, "Notification permission is needed to control OCR", Toast.LENGTH_SHORT).show()
		}
	}

	// 2. Overlay Permission Launcher (Settings Screen)
	private val overlayPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) {
		if (Settings.canDrawOverlays(this)) {
			requestMediaProjectionPermission()
		} else {
			Toast.makeText(this, "Overlay permission is required to select crop regions", Toast.LENGTH_SHORT).show()
		}
	}

	// 3. MediaProjection Permission Launcher (The Screen Capture Dialog)
	private val mediaProjectionLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK && result.data != null) {
			// Success! We received the capture token. Pass it to our Foreground Service.
			startScreenCaptureService(result.resultCode, result.data!!)
		} else {
			Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

		val btnStart = findViewById<Button>(R.id.btnStartService)
		btnStart.setOnClickListener {
			initiatePermissionChain()
		}
	}

	/**
	 * Step A: Check notification permissions (Required on API 33+, auto-granted on API 24-32)
	 */
	private fun initiatePermissionChain() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
				notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
				return
			}
		}
		checkOverlayPermissionThenStart()
	}

	/**
	 * Step B: Check overlay permission (canDrawOverlays)
	 */
	private fun checkOverlayPermissionThenStart() {
		if (!Settings.canDrawOverlays(this)) {
			val intent = Intent(
				Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
				Uri.parse("package:$packageName")
			)
			overlayPermissionLauncher.launch(intent)
		} else {
			requestMediaProjectionPermission()
		}
	}

	/**
	 * Step C: Pop the system screen-capture dialog
	 */
	private fun requestMediaProjectionPermission() {
		val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
		mediaProjectionLauncher.launch(captureIntent)
	}

	/**
	 * Step D: Launch the Foreground Service with the MediaProjection result
	 */
	private fun startScreenCaptureService(resultCode: Int, data: Intent) {
		val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
			putExtra(EXTRA_RESULT_CODE, resultCode)
			putExtra(EXTRA_RESULT_DATA, data)
		}

		ContextCompat.startForegroundService(this, serviceIntent)
		Toast.makeText(this, "OCR Service Started! Check your notifications.", Toast.LENGTH_SHORT).show()
		finish() // Close the activity so the user is immediately back to their manga/game
	}

	companion object {
		const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
		const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
	}
}
