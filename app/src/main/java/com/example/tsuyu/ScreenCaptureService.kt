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
import android.widget.Toast
import androidx.core.app.NotificationCompat

class ScreenCaptureService : Service() {

	private var mediaProjection: MediaProjection? = null
	private lateinit var mediaProjectionManager: MediaProjectionManager

	override fun onCreate() {
		super.onCreate()
		mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
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
					initMediaProjection(resultCode, resultData)
					startForegroundWithNotification()
				} else if (mediaProjection == null) {
					stopSelf()
				}
			}
		}

		return START_STICKY
	}

	private fun initMediaProjection(resultCode: Int, data: Intent) {
		mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

		// Mandatory callback registration for Android 14+ (API 34) stability
		mediaProjection?.registerCallback(object : MediaProjection.Callback() {
			override fun onStop() {
				super.onStop()
				mediaProjection = null
				stopForegroundService()
			}
		}, null)
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
	}

	private fun buildServiceNotification(): Notification {
		// PendingIntent for the "Capture" action button
		val captureIntent = Intent(this, ScreenCaptureService::class.java).apply {
			action = ACTION_TRIGGER_CAPTURE
		}
		val capturePendingIntent = PendingIntent.getService(
			this,
			101,
			captureIntent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)

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
			.setContentText("Tap Capture when Japanese text is on screen")
			.setSmallIcon(R.drawable.ic_scan)
			.setOngoing(true)
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.addAction(R.drawable.ic_scan, "Capture", capturePendingIntent)
			.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
			.build()
	}

	private fun handleCaptureTrigger() {
		if (mediaProjection == null) {
			Toast.makeText(this, "Capture session expired. Reopen app.", Toast.LENGTH_SHORT).show()
			stopForegroundService()
			return
		}

		Toast.makeText(this, "Capture triggered! Ready to snap frame.", Toast.LENGTH_SHORT).show()
		// Next step: Call the ImageReader frame capturer and launch the crop overlay
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
		mediaProjection?.stop()
		mediaProjection = null
	}

	override fun onBind(intent: Intent?): IBinder? = null

	companion object {
		const val CHANNEL_ID = "MANGA_OCR_SERVICE_CHANNEL"
		const val NOTIFICATION_ID = 2001
		const val ACTION_TRIGGER_CAPTURE = "ACTION_TRIGGER_CAPTURE"
		const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
	}
}