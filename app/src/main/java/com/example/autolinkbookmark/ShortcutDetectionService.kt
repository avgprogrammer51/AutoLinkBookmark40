package com.example.autolinkbookmark

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*

class ShortcutDetectionService : AccessibilityService() {
    
    companion object {
        var isRunning = false
        var callback: ((String) -> Unit)? = null
        var progressCallback: ((Int, Int, String) -> Unit)? = null
    }
    
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val helper by lazy { HomeScreenHelper(this) }
    private var shortcuts = mutableListOf<Pair<String, String>>()
    private var currentIndex = 0
    private var isProcessing = false
    
    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_TREE
            notificationTimeout = 100
        }
        setServiceInfo(info)
        isRunning = true
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: ""
                
                if (packageName == "com.android.chrome" && isProcessing) {
                    scope.launch {
                        delay(2500)
                        processUrl()
                    }
                }
                
                if ((packageName.contains("launcher") || packageName == "com.android.systemui") && isProcessing) {
                    scope.launch {
                        delay(1000)
                        processNext()
                    }
                }
            }
        }
    }
    
    fun startExtraction() {
        if (isProcessing) return
        isProcessing = true
        scope.launch {
            try {
                callback?.invoke("Detecting...")
                performGlobalAction(GLOBAL_ACTION_HOME)
                delay(1000)
                
                val detected = helper.getAllShortcuts()
                callback?.invoke("Found ${detected.size}")
                
                shortcuts.clear()
                for ((label, _) in detected) {
                    shortcuts.add(Pair(label, ""))
                }
                
                currentIndex = 0
                processNext()
                
            } catch (e: Exception) {
                callback?.invoke("Error: ${e.message}")
                isProcessing = false
            }
        }
    }
    
    private suspend fun processNext() {
        if (currentIndex >= shortcuts.size) {
            callback?.invoke("✓ Complete!")
            isProcessing = false
            return
        }
        
        try {
            val (label, _) = shortcuts[currentIndex]
            progressCallback?.invoke(currentIndex, shortcuts.size, label)
            callback?.invoke("[${currentIndex + 1}/${shortcuts.size}] $label")
            
            val all = helper.getAllShortcuts()
            val target = all.find { it.first == label }
            
            if (target != null) {
                helper.clickNode(target.second)
            } else {
                currentIndex++
                delay(500)
                processNext()
            }
        } catch (e: Exception) {
            currentIndex++
            delay(500)
            processNext()
        }
    }
    
    private suspend fun processUrl() {
        try {
            val url = helper.getChromeUrl()
            
            if (url.isNotEmpty() && url.startsWith("http")) {
                shortcuts[currentIndex] = Pair(shortcuts[currentIndex].first, url)
                
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("chrome://bookmarks/add?url=$url"))
                    intent.setPackage("com.android.chrome")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                } catch (e: Exception) {
                }
                
                delay(1000)
            }
            
            performGlobalAction(GLOBAL_ACTION_BACK)
            currentIndex++
            delay(1500)
            
        } catch (e: Exception) {
            currentIndex++
        }
    }
    
    override fun onInterrupt() {}
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        isProcessing = false
        job.cancel()
    }
    
    fun stopExtraction() {
        isProcessing = false
        currentIndex = shortcuts.size
    }
}
