package com.visionplus.yangocollector

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class YangoAccessibilityService : AccessibilityService() {

    private val targetPackage = "com.yango.driver"
    private val handler = Handler(Looper.getMainLooper())
    private var lastRunTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.DEFAULT or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        info.notificationTimeout = 100
        serviceInfo = info

        val prefs = getSharedPreferences("yango_collector_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("service_status", "connecte_v3").apply()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        val prefs = getSharedPreferences("yango_collector_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("last_package", packageName).apply()

        if (packageName == targetPackage) {
            val now = System.currentTimeMillis()
            if (now - lastRunTime < 800) return
            lastRunTime = now

            handler.postDelayed({ scanScreen(prefs) }, 600)
        }
    }

    private fun scanScreen(prefs: android.content.SharedPreferences) {
        val texts = mutableListOf<String>()
        var totalNodes = 0
        var windowCount = 0
        val classNames = mutableSetOf<String>()

        try {
            val windowList = windows
            windowCount = windowList.size

            for (window in windowList) {
                val root = window.root ?: continue
                fun explore(node: AccessibilityNodeInfo?) {
                    if (node == null) return
                    totalNodes++
                    classNames.add(node.className?.toString() ?: "?")
                    val text = node.text?.toString()
                    val desc = node.contentDescription?.toString()
                    if (!text.isNullOrBlank()) texts.add("T:$text")
                    if (!desc.isNullOrBlank()) texts.add("D:$desc")
                    for (i in 0 until node.childCount) {
                        explore(node.getChild(i))
                    }
                }
                explore(root)
            }

            if (texts.isEmpty()) {
                val rootFallback = rootInActiveWindow
                if (rootFallback != null) {
                    fun explore2(node: AccessibilityNodeInfo?) {
                        if (node == null) return
                        totalNodes++
                        val text = node.text?.toString()
                        val desc = node.contentDescription?.toString()
                        if (!text.isNullOrBlank()) texts.add("T:$text")
                        if (!desc.isNullOrBlank()) texts.add("D:$desc")
                        for (i in 0 until node.childCount) {
                            explore2(node.getChild(i))
                        }
                    }
                    explore2(rootFallback)
                }
            }
        } catch (e: Exception) {
            prefs.edit().putString("yango_screen_text", "EXCEPTION: ${e.message}").apply()
            return
        }

        val result = if (texts.isEmpty()) {
            "DIAG v3: $windowCount fenetres, $totalNodes noeuds, 0 texte. Classes vues: ${classNames.take(5).joinToString(",")}"
        } else {
            "[$windowCount fen, $totalNodes noeuds] " + texts.joinToString(" | ").take(1800)
        }

        prefs.edit().putString("yango_screen_text", result).apply()
    }

    override fun onInterrupt() {
    }
}
