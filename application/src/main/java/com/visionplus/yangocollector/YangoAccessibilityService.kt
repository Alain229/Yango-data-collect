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
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.notificationTimeout = 100
        serviceInfo = info

        val prefs = getSharedPreferences("yango_collector_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("service_status", "connecte_v2").apply()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        val prefs = getSharedPreferences("yango_collector_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("last_package", packageName).apply()

        if (packageName == targetPackage) {
            val rootNode = rootInActiveWindow

            if (rootNode == null) {
                prefs.edit().putString("yango_screen_text", "ERREUR: rootInActiveWindow est null").apply()
                return
            }

            val texts = mutableListOf<String>()
            collectText(rootNode, texts)

            if (texts.isEmpty()) {
                prefs.edit().putString("yango_screen_text", "ERREUR: aucun texte trouve dans l'arbre (childCount racine: ${rootNode.childCount})").apply()
            } else {
                val combined = texts.joinToString(" | ").take(2000)
                prefs.edit().putString("yango_screen_text", combined).apply()
            }
        }
    }

    private fun collectText(node: AccessibilityNodeInfo?, output: MutableList<String>) {
        if (node == null) return
        val text = node.text?.toString()
        if (!text.isNullOrBlank()) {
            output.add(text)
        }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), output)
        }
    }

    override fun onInterrupt() {
    }
}
