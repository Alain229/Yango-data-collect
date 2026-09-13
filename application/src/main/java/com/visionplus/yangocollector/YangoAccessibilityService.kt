package com.visionplus.yangocollector

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityEvent

class YangoAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.DEFAULT or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        info.notificationTimeout = 100
        serviceInfo = info

        val prefs = getSharedPreferences("yango_collector_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("service_status", "connecte_v2").apply()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        val prefs = getSharedPreferences("yango_collector_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("last_package", packageName).apply()
    }

    override fun onInterrupt() {
    }
}
