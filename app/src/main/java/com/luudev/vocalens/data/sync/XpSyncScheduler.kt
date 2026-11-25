package com.luudev.vocalens.data.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class XpSyncScheduler(context: Context) {
    private val work = WorkManager.getInstance(context.applicationContext)

    fun enqueue(userId: String) {
        val request = OneTimeWorkRequestBuilder<XpSyncWorker>()
            .setInputData(workDataOf("userId" to userId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // Append avoids losing a completion arriving just as an existing worker finishes.
        work.enqueueUniqueWork("xp-sync-$userId", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun cancel(userId: String) { work.cancelUniqueWork("xp-sync-$userId") }
}
