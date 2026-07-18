package com.talla.accessibilityservice.data.model

import java.util.UUID

data class AppUsageInfo(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String = "",
    val appName: String = "",
    val startTimeMillis: Long = 0,
    val endTimeMillis: Long = 0,
    val durationMillis: Long = 0,
    val userId: String = ""
)
