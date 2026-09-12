package com.visionplus.yangocollector

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class YangoAccessibilityService : AccessibilityService() {

    private var lastPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        if (packageName != lastPackage) {
            lastPackage = packageName
            Toast.makeText(this, "App ouverte : $packageName", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInterrupt() {
    }
}
