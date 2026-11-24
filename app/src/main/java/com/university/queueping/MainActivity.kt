package com.university.queueping

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.content.ContextCompat

// ... rest of imports
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.university.queueping.data.MockDataRepository
import com.university.queueping.model.EntryStatus
import com.university.queueping.navigation.AppNavigation
import com.university.queueping.ui.theme.QueuePingTheme
import com.university.queueping.util.NotificationHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var lastNotifiedEntryId: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. Notifications can be shown.
        } else {
            // Explain to the user that the feature is unavailable because the
            // feature requires a permission that the user has denied.
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        askNotificationPermission()

        val notificationHelper = NotificationHelper(applicationContext)
        // Collect outside Compose so notifications still fire when the activity is stopped/minimized.
        lifecycleScope.launch {
            MockDataRepository.activeCustomerEntry.collect { entry ->
                if (entry?.status == EntryStatus.CALLED) {
                    if (entry.entryId != lastNotifiedEntryId) {
                        lastNotifiedEntryId = entry.entryId
                        val businessName = MockDataRepository.getQueueById(entry.queueId)?.businessName ?: "the counter"
                        notificationHelper.showQueueNotification(
                            title = "Your Turn!",
                            message = "Please head to the counter at $businessName. Your ticket ${entry.queueNumber} is being called."
                        )
                    }
                } else {
                    lastNotifiedEntryId = null
                }
            }
        }
        
        setContent {
            QueuePingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
