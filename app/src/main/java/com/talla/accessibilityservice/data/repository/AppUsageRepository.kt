package com.talla.accessibilityservice.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.talla.accessibilityservice.data.model.AppUsageInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class AppUsageRepository(private val context: Context) {
    private val TAG = "AppUsageRepository"
    private val database = FirebaseDatabase.getInstance()
    private val usageEventsRef = database.getReference("usage_events")

    suspend fun logAppUsageEvent(packageName: String, startTime: Long, endTime: Long) {
        try {
            val packageManger = context.packageManager
            val appName = try {
                val appInfo = packageManger.getApplicationInfo(packageName, 0)
                packageManger.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                packageName
            }
            val duration = if (endTime > startTime) endTime - startTime else 0
            val appUsageEvent = AppUsageInfo(
                packageName = packageName,
                appName = appName,
                startTimeMillis = startTime,
                endTimeMillis = endTime,
                durationMillis = duration,
                userId = "user123" // In a real app you would get a real userId
            )
            //Add the event to the firebase database
            usageEventsRef.child(appUsageEvent.id).setValue(appUsageEvent).await()
        } catch (e: Exception) {
            Log.e(TAG, "logAppUsageEvent: ")
        }
    }

    fun getTodayAppUsageEvents(): Flow<List<AppUsageInfo>> = callbackFlow {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val events = mutableListOf<AppUsageInfo>()
                for (childSnapShot in snapshot.children) {
                    val event = childSnapShot.getValue<AppUsageInfo>()
                    event?.let { eventObj ->
                        if (eventObj.startTimeMillis >= startOfDay) {
                            events.add(eventObj)
                        }
                    }
                }
                trySend(events.sortedByDescending { it.startTimeMillis })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled: Error getting app usage events", error.toException())
                trySend(emptyList())
            }
        }

        usageEventsRef.orderByChild("startTimeMillis").startAt(startOfDay.toDouble())
            .addValueEventListener(listener)
        awaitClose { usageEventsRef.removeEventListener(listener) }
    }

}