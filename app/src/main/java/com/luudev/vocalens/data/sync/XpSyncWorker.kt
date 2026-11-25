package com.luudev.vocalens.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.functions.FirebaseFunctionsException
import com.luudev.vocalens.data.remote.FirebaseServices
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.data.repository.FirebaseXpSyncRepository
import com.luudev.vocalens.domain.usecase.SyncPendingXp
import kotlinx.coroutines.CancellationException
import java.io.IOException

class XpSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val uid = inputData.getString("userId") ?: return Result.failure()
        if (runAttemptCount >= XpSyncRetryPolicy.MAX_ATTEMPTS) return Result.failure()
        val auth = FirebaseServices.auth()
        if (auth.currentUser?.uid != uid) return Result.success()
        return try {
            SyncPendingXp(FirebaseXpSyncRepository(VocabLensDatabase.getInstance(applicationContext)))(uid)
            Result.success()
        } catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            when {
                auth.currentUser?.uid != uid -> Result.success()
                XpSyncRetryPolicy.shouldRetry(
                    runAttemptCount = runAttemptCount,
                    functionsCode = (error as? FirebaseFunctionsException)?.code?.name,
                    networkFailure = error is FirebaseNetworkException || error is IOException
                ) -> Result.retry()
                // Keep sessions PENDING. A new explicit sync can retry after the cause is fixed.
                else -> Result.failure()
            }
        }
    }
}
