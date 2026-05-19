package com.gtd.android.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.gtd.android.data.NetworkMonitor
import com.gtd.android.data.local.dao.PendingChangeDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncStatus {
    IDLE,
    SYNCING,
    PENDING,
    OFFLINE,
}

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor,
    private val pendingChangeDao: PendingChangeDao,
) {
    private val workManager = WorkManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        observeNetworkAndAutoSync()
    }

    fun requestSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(SyncWorker.TAG)
            .build()

        workManager.enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest,
        )
    }

    private fun observeNetworkAndAutoSync() {
        scope.launch {
            var wasOffline = false
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline && wasOffline) {
                    requestSync()
                }
                wasOffline = !isOnline
            }
        }
    }

    val syncStatus: Flow<SyncStatus> = combine(
        networkMonitor.isOnline,
        workManager.getWorkInfosForUniqueWorkFlow(SyncWorker.WORK_NAME)
            .map { workInfos ->
                workInfos.any { it.state == WorkInfo.State.RUNNING }
            },
        pendingChangeDao.observeUnsyncedCount(),
    ) { isOnline, isSyncing, unsyncedCount ->
        when {
            isSyncing -> SyncStatus.SYNCING
            !isOnline -> SyncStatus.OFFLINE
            unsyncedCount > 0 -> SyncStatus.PENDING
            else -> SyncStatus.IDLE
        }
    }.distinctUntilChanged()
}
