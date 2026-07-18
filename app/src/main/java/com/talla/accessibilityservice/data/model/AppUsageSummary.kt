package com.talla.accessibilityservice.data.model

data class AppUsageSummary(
    val packageName: String,
    val appName: String,
    val totalDurationMillis: Long,
    val count: Int,
    val lastUsed: Long
)
