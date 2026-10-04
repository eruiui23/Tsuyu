package com.example.tsuyu

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ScreenCaptureService : Service() {

	private var mediaProjection: MediaProjection? = null
	private lateinit var mediaProjectionManager: MediaProjectionManager
	
	private val serviceJob = SupervisorJob()
	private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
	private lateinit var captureEngine: ScreenCaptureEngine
	private lateinit var overlayManager: OverlayManager
	private lateinit var badgeManager: FloatingBadgeManager

	override fun onCreate() {
		super.onCreate()
		mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
		captureEngine = ScreenCaptureEngine(this)
		overlayManager = OverlayManager(this)
		badgeManager = FloatingBadgeManager(this)
		createNotificationChannel()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (intent == null) return START_NOT_STICKY

		when (intent.action) {
			ACTION_STOP_SERVICE -> {
				stopForegroundService()
				return START_NOT_STICKY
			}
			ACTION_TRIGGER_CAPTURE -> {
				handleCaptureTrigger()
				return START_STICKY
			}
			else -> {
				// Initialize the MediaProjection token passed from MainActivity
				val resultCode = intent.getIntExtra(MainActivity.EXTRA_RESULT_CODE, 0)
				val resultData = intent.getParcelableExtra<Intent>(MainActivity.EXTRA_RESULT_DATA)

				if (resultCode != 0 && resultData != null) {
					startForegroundWithNotification()
					initMediaProjection(resultCode, resultData)
				} else if (mediaProjection == null) {
					stopSelf()
				}
			}
		}

		return START_STICKY
	}

	private fun initMediaProjection(resultCode: Int, data: Intent) {
		val projection = mediaProjectionManager.getMediaProjection(resultCode, data) ?: return
		mediaProjection = projection

		// Mandatory callback registration for Android 14+ (API 34) stability
		// Must be called BEFORE creating virtual display in engine.start()
		projection.registerCallback(object : MediaProjection.Callback() {
			override fun onStop() {
				super.onStop()
				mediaProjection = null
				stopForegroundService()
			}
		}, null)

		// Start our long-lived engine once
		captureEngine.start(projection)
		
		// Show our floating drag-and-drop trigger
		badgeManager.showBadge { handleCaptureTrigger() }
	}

	private fun startForegroundWithNotification() {
		val notification = buildServiceNotification()

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			startForeground(
				NOTIFICATION_ID,
				notification,
				ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
			)
		} else {
			startForeground(NOTIFICATION_ID, notification)
		}
		
		// Notify MainActivity that it's safe to finish
		sendBroadcast(Intent(ACTION_SERVICE_STARTED))
	}

	private fun buildServiceNotification(): Notification {
		// PendingIntent for the "Stop" action button
		val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
			action = ACTION_STOP_SERVICE
		}
		val stopPendingIntent = PendingIntent.getService(
			this,
			102,
			stopIntent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)

		return NotificationCompat.Builder(this, CHANNEL_ID)
			.setContentTitle("Manga OCR Active")
			.setContentText("Tap the floating badge to capture Japanese text")
			.setSmallIcon(R.drawable.ic_scan)
			.setOngoing(true)
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
			.build()
	}

	private fun handleCaptureTrigger() {
		val projection = mediaProjection
		if (projection == null) {
			Toast.makeText(this, "Capture session expired. Reopen app.", Toast.LENGTH_SHORT).show()
			stopForegroundService()
			return
		}

		serviceScope.launch {
			try {
				// Hide the badge temporarily so it doesn't get captured in the screenshot!
				badgeManager.setVisible(false)
				kotlinx.coroutines.delay(50) 
				
				// Pull the latest frame from our permanent engine
				val bitmap = captureEngine.captureFrame()
				
				if (bitmap != null) {
					Log.d("TsuyuCapture", "Captured frame: ${bitmap.width}x${bitmap.height}")
					
					overlayManager.showOverlay { rect ->
						Log.d("TsuyuCrop", "Crop area: $rect")
						Toast.makeText(this@ScreenCaptureService, "Crop selected: ${rect.width()}x${rect.height()}", Toast.LENGTH_SHORT).show()
						
						// Prepared for Milestone 4 (where we will slice the bitmap using the rect)
						// val croppedBitmap = sliceBitmap(bitmap, rect)
						
						// Restore badge
						badgeManager.setVisible(true)
					}
				} else {
					Log.e("TsuyuCapture", "Capture returned null bitmap")
					Toast.makeText(this@ScreenCaptureService, "Capture failed: Empty frame", Toast.LENGTH_SHORT).show()
					badgeManager.setVisible(true)
				}
			} catch (e: Exception) {
				Log.e("TsuyuCapture", "Error capturing screen", e)
				Toast.makeText(this@ScreenCaptureService, "Capture error: ${e.message}", Toast.LENGTH_SHORT).show()
				badgeManager.setVisible(true)
			}
		}
	}

	private fun createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				CHANNEL_ID,
				"Screen Capture Service",
				NotificationManager.IMPORTANCE_LOW
			).apply {
				description = "Controls for the on-demand manga OCR capture engine"
			}
			val manager = getSystemService(NotificationManager::class.java)
			manager.createNotificationChannel(channel)
		}
	}

	private fun stopForegroundService() {
		mediaProjection?.stop()
		mediaProjection = null
		stopForeground(STOP_FOREGROUND_REMOVE)
		stopSelf()
	}

	override fun onDestroy() {
		super.onDestroy()
		serviceJob.cancel()
		overlayManager.hideOverlay()
		badgeManager.hideBadge()
		captureEngine.stop()
		mediaProjection?.stop()
		mediaProjection = null
	}

	override fun onBind(intent: Intent?): IBinder? = null

	companion object {
		const val CHANNEL_ID = "MANGA_OCR_SERVICE_CHANNEL"
		const val NOTIFICATION_ID = 2001
		const val ACTION_TRIGGER_CAPTURE = "ACTION_TRIGGER_CAPTURE"
		const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
		const val ACTION_SERVICE_STARTED = "com.example.tsuyu.ACTION_SERVICE_STARTED"
	}
}