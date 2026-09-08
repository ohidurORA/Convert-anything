package com.example.orayva.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.orayva.MainActivity
import com.example.orayva.core.JobStatus
import com.example.orayva.native.EngineBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps the process alive while the C++ engine transcodes (spec §2:
 * conversions survive navigation + backgrounding). Idles to stopSelf()
 * when no job is queued/running. Paired 1:1 with engine state.
 */
class ConversionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var startedForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        scope.launch {
            EngineBridge.jobs.collect { jobs ->
                if (jobs.isEmpty()) {
                    // Only stop when the engine is truly empty: terminal jobs
                    // still need finalize() to publish to Downloads. Stopping
                    // with DONE jobs present loses foreground priority and the
                    // OS can kill the process mid-publish ("file not in output").
                    stopSelf()
                } else {
                    val running = jobs.firstOrNull { it.status == JobStatus.RUNNING }
                    val text = if (running != null) {
                        "${running.source.displayName} · ${running.progress}%"
                    } else {
                        "${jobs.size} queued…"
                    }
                    startOrUpdate(text)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startOrUpdate(text: String) {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("Orayva")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (!startedForeground) {
            startedForeground = true
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    startForeground(ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    @Suppress("DEPRECATION")
                    startForeground(ID, notif)
                }
            } catch (e: SecurityException) {
                // FGS denied (API 34+): engine keeps running without the notification.
                android.util.Log.w("ConversionService", "startForeground denied", e)
                startedForeground = false
            }
        } else {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(ID, notif)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL) == null) {
                    mgr.createNotificationChannel(
                            NotificationChannel(CHANNEL, "Orayva", NotificationManager.IMPORTANCE_LOW))
        }
    }

    companion object {
        private const val CHANNEL = "conversions"
        private const val ID = 41
    }
}
