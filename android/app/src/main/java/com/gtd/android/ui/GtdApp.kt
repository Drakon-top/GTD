package com.gtd.android.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gtd.android.NotificationNavigation
import com.gtd.android.ui.navigation.GtdNavHost
import kotlinx.coroutines.flow.StateFlow

@Composable
fun GtdApp(pendingNavigation: StateFlow<NotificationNavigation?>? = null) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        GtdNavHost(pendingNavigation = pendingNavigation)
    }
}
