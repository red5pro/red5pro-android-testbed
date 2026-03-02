package net.red5.testbed.utility

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Keeps WebRTC audio alive when the screen is locked.
 *
 * - Publish mode: foreground service type = microphone (prevents AudioRecord suspension)
 * - Subscribe mode: foreground service type = mediaPlayback (prevents AudioTrack suspension)
 */
class ConnectionForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_PUBLISH
        createNotificationChannel()
        val notification = buildNotification(mode)

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // API 30+: both microphone and mediaPlayback types available
                startForeground(
                    NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // API 29: only mediaPlayback type available
                startForeground(
                    NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            }
            else -> startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Streaming",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps stream audio active while the screen is off"
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(mode: String): Notification {
        val text = if (mode == MODE_SUBSCRIBE) "Receiving live stream" else "Publishing live stream"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Live stream active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "red5_streaming"
        private const val NOTIFICATION_ID = 9001
        private const val EXTRA_MODE = "mode"
        const val MODE_PUBLISH = "publish"
        const val MODE_SUBSCRIBE = "subscribe"

        fun startPublish(context: Context) = start(context, MODE_PUBLISH)

        fun startSubscribe(context: Context) = start(context, MODE_SUBSCRIBE)

        private fun start(context: Context, mode: String) {
            val intent = Intent(context, ConnectionForegroundService::class.java)
                .putExtra(EXTRA_MODE, mode)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ConnectionForegroundService::class.java))
        }
    }
}
