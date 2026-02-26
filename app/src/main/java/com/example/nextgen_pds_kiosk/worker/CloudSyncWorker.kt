package com.example.nextgen_pds_kiosk.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nextgen_pds_kiosk.data.local.AnalyticsDao
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val analyticsDao: AnalyticsDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("CloudSyncWorker", "Checking for offline unsynced records...")
            
            val unsyncedTransactions = analyticsDao.getUnsyncedTransactions()
            val unsyncedInventoryLogs = analyticsDao.getUnsyncedInventoryLogs()

            if (unsyncedTransactions.isEmpty() && unsyncedInventoryLogs.isEmpty()) {
                Log.d("CloudSyncWorker", "No new records to sync. All caught up!")
                return@withContext Result.success()
            }

            Log.d("CloudSyncWorker", "Found ${unsyncedTransactions.size} txs and ${unsyncedInventoryLogs.size} inv logs to sync to Firebase.")

            // Push to Firestore Collections
            unsyncedTransactions.forEach { tx ->
                firestore.collection("transactions").add(tx).await()
                analyticsDao.markTransactionAsSynced(tx.transactionId)
            }
            
            unsyncedInventoryLogs.forEach { log ->
                firestore.collection("inventory").add(log).await()
                analyticsDao.markInventoryLogAsSynced(log.logId)
            }

            Log.d("CloudSyncWorker", "Successfully pushed all offline payload to Firebase!")
            return@withContext Result.success()
            
        } catch (e: Exception) {
            Log.e("CloudSyncWorker", "Firebase exception during sync. Will retry later.", e)
            return@withContext Result.retry() 
        }
    }
}
