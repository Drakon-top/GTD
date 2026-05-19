package com.gtd.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.gtd.android.notification.NotificationHelper
import com.gtd.android.ui.GtdApp
import com.gtd.android.ui.theme.GtdTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val _pendingNavigation = MutableStateFlow<NotificationNavigation?>(null)
    val pendingNavigation: StateFlow<NotificationNavigation?> = _pendingNavigation.asStateFlow()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* no-op: user choice is respected */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        notificationHelper.createNotificationChannels()
        requestNotificationPermissionIfNeeded()
        handleNotificationIntent(intent)

        setContent {
            GtdTheme {
                GtdApp(pendingNavigation = pendingNavigation)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    fun consumeNavigation() {
        _pendingNavigation.value = null
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val taskId = intent?.getStringExtra(NotificationHelper.EXTRA_TASK_ID)
        val contextId = intent?.getStringExtra(NotificationHelper.EXTRA_CONTEXT_ID)
        if (!taskId.isNullOrBlank() && !contextId.isNullOrBlank()) {
            _pendingNavigation.value = NotificationNavigation(
                contextId = contextId,
                taskId = taskId,
            )
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }
}

data class NotificationNavigation(
    val contextId: String,
    val taskId: String,
)
