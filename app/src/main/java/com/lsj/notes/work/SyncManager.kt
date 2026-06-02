package com.lsj.notes.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncManager {
    
    /**
     * Schedule periodic sync of notes with Supabase
     * Syncs every 15 minutes when device has network connection
     */
    fun scheduleSyncWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncWorker = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES // Sync every 15 minutes
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorker
        )
    }

    /**
     * Cancel the periodic sync worker
     */
    fun cancelSyncWorker(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.TAG)
    }
}