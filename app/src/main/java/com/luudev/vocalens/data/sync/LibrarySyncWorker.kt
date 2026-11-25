package com.luudev.vocalens.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestoreException
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.data.remote.FirebaseServices
import com.luudev.vocalens.data.repository.FirebaseLibrarySyncRepository
import com.luudev.vocalens.domain.usecase.SyncLibrary
import kotlinx.coroutines.CancellationException

class LibrarySyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val uid = inputData.getString("userId") ?: return Result.failure()
        if (FirebaseServices.auth().currentUser?.uid != uid) return Result.success()
        val repository = FirebaseLibrarySyncRepository(VocabLensDatabase.getInstance(applicationContext),
            FirebaseServices.firestore(), { FirebaseServices.auth().currentUser?.uid }, LibrarySyncScheduler(applicationContext))
        return try {
            SyncLibrary(repository)(uid)
            Result.success()
        } catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            if (FirebaseServices.auth().currentUser?.uid != uid) return Result.success()
            repository.recordError(uid, error.javaClass.simpleName)
            val temporary = error is FirebaseFirestoreException && error.code in setOf(
                FirebaseFirestoreException.Code.UNAVAILABLE, FirebaseFirestoreException.Code.ABORTED,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED, FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED)
            if (temporary && runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }
}
