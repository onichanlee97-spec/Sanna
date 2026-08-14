package com.aistudio.futureagent.agxjyz.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

object SannaAccessibilityMonitor {
    var lastScrapedHierarchy: String = ""
        private set

    fun updateHierarchy(rootNode: AccessibilityNodeInfo?) {
        if (rootNode == null) return
        val builder = StringBuilder()
        builder.append("[Accessibility Screen Scrape]: Active Window Nodes:\n")
        traverseNode(rootNode, builder, 0)
        lastScrapedHierarchy = builder.toString()
    }

    private fun traverseNode(node: AccessibilityNodeInfo, builder: StringBuilder, depth: Int) {
        val indent = "  ".repeat(depth)
        val className = node.className?.toString()?.substringAfterLast('.') ?: "Unknown"
        val text = node.text?.toString() ?: ""
        val contentDescription = node.contentDescription?.toString() ?: ""
        val isClickable = node.isClickable
        
        if (text.isNotBlank() || contentDescription.isNotBlank()) {
            builder.append("$indent- [$className] text=\"$text\" contentDescription=\"$contentDescription\" clickable=$isClickable\n")
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNode(child, builder, depth + 1)
            }
        }
    }
}

class SannaAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            SannaAccessibilityMonitor.updateHierarchy(rootNode)
        }
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Service connected
    }
}
