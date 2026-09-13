package com.visionplus.yangocollector

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat

class YangoAccessibilityService : AccessibilityService() {

    private var lastPackage: String? = null
    private val channelId = "yango_collector_channel"

    override fun onServiceConnected() {
        super.onServiceConnected()
        createNotificationChannel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        if (packageName != lastPackage) {
            lastPackage = packageName
            showNotification(packageName)
        }
    }

    private fun showNotification(packageName: String) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("App detectee")
            .setContentText(packageName)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Yango Collector",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onInterrupt() {
    }
}
