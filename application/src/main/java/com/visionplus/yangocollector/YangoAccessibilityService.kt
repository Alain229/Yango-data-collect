package com.visionplus.yangocollector

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class YangoAccessibilityService : AccessibilityService() {

    private val targetPackage = "com.yango.driver"

    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.DEFAULT or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        info.notificationTimeout = 0
        serviceInfo = info

        val prefs = getSharedPreferences("yango_collector_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("service_status", "connecte_v4").apply()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        val prefs = getSharedPreferences("yango_collector_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("last_package", packageName).apply()

        if (packageName == targetPackage) {
            scanScreen(prefs, event)
        }
    }

    private fun scanScreen(prefs: android.content.SharedPreferences, event: AccessibilityEvent) {
        val texts = mutableListOf<String>()
        var totalNodes = 0

        fun explore(node: AccessibilityNodeInfo?) {
            if (node == null) return
            totalNodes++
            val text = node.text?.toString()
            val desc = node.contentDescription?.toString()
            if (!text.isNullOrBlank()) texts.add("T:$text")
            if (!desc.isNullOrBlank()) texts.add("D:$desc")
            for (i in 0 until node.childCount) {
                explore(node.getChild(i))
            }
        }

        val eventSource = event.source
        if (eventSource != null) {
            explore(eventSource)
        }

        if (texts.isEmpty()) {
            val root = rootInActiveWindow
            if (root != null) {
                explore(root)
            }
        }

        if (texts.isNotEmpty()) {
            val combined = texts.joinToString(" | ").take(2000)
            prefs.edit().putString("yango_screen_text", "[$totalNodes noeuds] $combined").apply()
        }
        // Si vide, on ne touche pas a la valeur precedente (garde la derniere capture reussie)
    }

    override fun onInterrupt() {
    }
}
