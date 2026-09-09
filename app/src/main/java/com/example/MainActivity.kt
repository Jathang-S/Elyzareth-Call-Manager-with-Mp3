package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.service.CallMonitoringService
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.PermissionHelper
import com.example.util.RuntimePermissionHandler
import com.example.viewmodel.CallSmsViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: CallSmsViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Start call monitoring service if permissions are already granted
    if (PermissionHelper.hasPermission(this, Manifest.permission.READ_PHONE_STATE)) {
      CallMonitoringService.startService(this)
    }

    setContent {
      val currentAppTheme by viewModel.appTheme.collectAsState()
      MyApplicationTheme(appTheme = currentAppTheme) {
        RuntimePermissionHandler(
          onPermissionsResult = { results ->
            if (results[Manifest.permission.READ_PHONE_STATE] == true) {
              CallMonitoringService.startService(this@MainActivity)
            }
          }
        ) {
          MainScreen(viewModel = viewModel)
        }
      }
    }
  }
}
