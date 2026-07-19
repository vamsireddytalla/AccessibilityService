package com.talla.accessibilityservice

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.talla.accessibilityservice.data.services.AppTrackerServices
import com.talla.accessibilityservice.ui.screen.AppTrackerScreen
import com.talla.accessibilityservice.ui.theme.AccessibilityServiceTheme
import com.talla.accessibilityservice.ui.viewmodel.AppUsageViewModel

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private val viewModel: AppUsageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermission()
        setContent {
            AccessibilityServiceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppTrackerScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    private fun checkPermission() {
        //check both permissions every time when the user come back to the app
        viewModel.setUsageStatsPermissionGranted(hasUsageStatsPermission(this))
        viewModel.setAccessibilityEnabled(isAccessibilityServiceEnabled())
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = packageName + "/" + AppTrackerServices::class.java.canonicalName
        val accessibilityEnabled = try {
            Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return settingValue?.split(":")?.contains(serviceName) == true
        }
        return false
    }
}
