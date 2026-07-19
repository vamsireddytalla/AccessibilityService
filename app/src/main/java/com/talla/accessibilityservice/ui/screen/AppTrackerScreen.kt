package com.talla.accessibilityservice.ui.screen


import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.talla.accessibilityservice.data.model.AppUsageInfo
import com.talla.accessibilityservice.data.model.AppUsageSummary
import com.talla.accessibilityservice.ui.viewmodel.AppUsageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTrackerScreen(viewModel: AppUsageViewModel) {
    val context = LocalContext.current
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.observeAsState(initial = false)
    val isUsageStatsEnabled by viewModel.isUsageStatsEnabled.observeAsState(initial = false)
    val isServiceRunning by viewModel.isServiceRunning.observeAsState(initial = false)
    val appUsageSummary by viewModel.appUsageSummary.observeAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Usage Tracker") }
            )
        }
    ) { paddingsValues ->
        if (!isAccessibilityEnabled) {
            AccessibilityPermissionRequest(
                onRequestPermissions = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    Toast.makeText(
                        context,
                        "Enable AppTrackerService, then return to app",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.padding(paddingsValues)
            )
        } else if (!isUsageStatsEnabled) {
            AccessibilityPermissionRequest(
                onRequestPermissions = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    Toast.makeText(
                        context,
                        "Enable AppTrackerService, then return to app",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.padding(paddingsValues),
                title = "This app requires Usage Stats permission to track app usage",
                description = "Please accept the Usage Stats permission to continue",
                buttonTex = "Open Usage Stats Settings"
            )
        } else {
            AppUsageTabs(
                viewModel = viewModel,
                appUsageSummary = appUsageSummary,
                modifier = Modifier.padding(paddingsValues)
            )
        }
    }

}

@Composable
fun AccessibilityPermissionRequest(
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "This app requires accessibility Service permission to track app usage",
    description: String = "Please accept the Accessibility permission to continue",
    buttonTex: String = "Open Accessibility Settings"
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 32.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            modifier = Modifier.padding(horizontal = 32.dp),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermissions) {
            Text(buttonTex)
        }
    }
}

@Composable
fun AppUsageTabs(
    viewModel: AppUsageViewModel,
    appUsageSummary: List<AppUsageSummary>,
    modifier: Modifier = Modifier
) {
    val todayEvents by viewModel.todayEvents.observeAsState(initial = emptyList())
    var selectedTabIndex by remember { mutableStateOf(0) }
    Column(
        modifier = modifier
    ) {
        //Tab Row
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Summary") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Detailed Events") }
            )
        }
        //Display different content based on a selected tab
        when (selectedTabIndex) {
            0 -> {
                //Summary tab
                if (appUsageSummary.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No usage summary yet")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                "App Usage Summary",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        items(appUsageSummary) { summary ->
                            AppUsageSummaryItem(summary = summary, viewModel = viewModel)
                            Divider()
                        }
                    }
                }
            }

            1 -> {
                //Detailed Events Tab
                if (todayEvents.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No Detailed events available yet")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = "App Usage Events (${todayEvents.size}) events",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        items(todayEvents) { event ->
                            AppEventItem(event, viewModel)
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageSummaryItem(summary: AppUsageSummary, viewModel: AppUsageViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = summary.appName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Used ${viewModel.formatDuration(summary.totalDurationMillis)}",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Opened ${summary.count} times"
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Last Used: ${viewModel.formatDateTime(summary.lastUsed)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun AppEventItem(event: AppUsageInfo, viewModel: AppUsageViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = event.appName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Started at: ${viewModel.formatDateTime(event.startTimeMillis)}",
                style = MaterialTheme.typography.labelMedium
            )
            if (event.durationMillis > 0) {
                Text(
                    text = "Duration: ${viewModel.formatDuration(event.durationMillis)}",
                    style = MaterialTheme.typography.labelMedium
                )
            } else {
                Text(text = "Brief usage event")
            }
        }
    }
}