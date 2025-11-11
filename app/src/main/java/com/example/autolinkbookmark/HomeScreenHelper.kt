package com.example.autolinkbookmark

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class HomeScreenHelper(private val service: AccessibilityService) {
    
    fun getAllShortcuts(): List<Pair<String, AccessibilityNodeInfo>> {
        val shortcuts = mutableListOf<Pair<String, AccessibilityNodeInfo>>()
        try {
            val root = service.rootInActiveWindow ?: return shortcuts
            collectShortcuts(root, shortcuts)
        } catch (e: Exception) {
            Log.e("HomeScreenHelper", "Error: ${e.message}")
        }
        return shortcuts
    }
    
    private fun collectShortcuts(
        node: AccessibilityNodeInfo?,
        result: MutableList<Pair<String, AccessibilityNodeInfo>>
    ) {
        if (node == null) return
        try {
            if (node.isClickable) {
                val label = node.contentDescription?.toString() 
                    ?: node.text?.toString() 
                    ?: "Unknown"
                
                if (label != "Unknown" && !result.any { it.first == label }) {
                    result.add(Pair(label, node))
                }
            }
            
            for (i in 0 until (node.childCount ?: 0)) {
                collectShortcuts(node.getChild(i), result)
            }
        } catch (e: Exception) {
            Log.e("HomeScreenHelper", "Error: ${e.message}")
        }
    }
    
    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        return try {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } catch (e: Exception) {
            false
        }
    }
    
    fun getChromeUrl(): String {
        return try {
            val root = service.rootInActiveWindow ?: return ""
            findUrl(root)
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun findUrl(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        try {
            val text = node.text?.toString() ?: ""
            if (text.startsWith("http")) return text
            
            for (i in 0 until (node.childCount ?: 0)) {
                val result = findUrl(node.getChild(i))
                if (result.isNotEmpty()) return result
            }
        } catch (e: Exception) {
        }
        return ""
    }
}
