package com.luudev.vocalens.data.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class LibrarySyncScheduler(context: Context) {
    private val work = WorkManager.getInstance(context.applicationContext)
    fun enqueue(uid: String) {
        val request = OneTimeWorkRequestBuilder<LibrarySyncWorker>()
            .setInputData(workDataOf("userId" to uid))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        work.enqueueUniqueWork("library-sync-$uid", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
    fun cancel(uid: String) { work.cancelUniqueWork("library-sync-$uid") }
}
