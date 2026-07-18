package com.talla.accessibilityservice.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.talla.accessibilityservice.data.model.AppUsageSummary
import com.talla.accessibilityservice.data.repository.AppUsageRepository
import com.talla.accessibilityservice.data.services.AppTrackerServices
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppUsageViewModel(application: Application) : AndroidViewModel(application = application) {
    private val appUsageRepository = AppUsageRepository(application)
    private val _isPermissionGranted = MutableLiveData<Boolean>()
    val isPermissionGranted = _isPermissionGranted

    val isServiceRunning: LiveData<Boolean> = AppTrackerServices.isServiceRunning
    val todayEvents = appUsageRepository.getTodayAppUsageEvents().asLiveData()

    fun setPermissionGranted(granted: Boolean) {
        _isPermissionGranted.value = granted
    }

    val appUsageSummary = appUsageRepository.getTodayAppUsageEvents().map { events ->
        //Group events by app and calculate total usage time
        events.groupBy { it.packageName }.map { (packageName, appEvents) ->
            val appName = appEvents.firstOrNull()?.appName ?: packageName
            val totalDuration = appEvents.sumOf { it.durationMillis }
            val count = appEvents.size
            val lastUsed = appEvents.maxOfOrNull { it.startTimeMillis } ?: 0
            AppUsageSummary(packageName, appName, totalDuration, count, lastUsed)
        }.sortedByDescending { it.totalDurationMillis }
    }.asLiveData()


    fun formatDuration(durationMillis: Long): String {
        val hours = durationMillis / (1000 * 60 * 60)
        val minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (durationMillis % (1000 * 60)) / 1000

        return when {
            hours > 0 -> String.format("%d h %d min", hours, minutes)
            minutes > 0 -> String.format("%d min %d sec", minutes, seconds)
            else -> String.format("%d sec", seconds)
        }
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

}