package com.example.nextgen_pds_kiosk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {

    @Query("SELECT * FROM transaction_logs ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionLog>>

    @Insert
    suspend fun insertTransaction(log: TransactionLog)

    @Query("SELECT * FROM inventory_logs ORDER BY timestamp DESC")
    fun getAllInventoryLogsFlow(): Flow<List<InventoryLog>>

    @Insert
    suspend fun insertInventoryLog(log: InventoryLog)
    
    @Query("SELECT currentTotal FROM inventory_logs WHERE itemName = :itemName ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestInventoryLevel(itemName: String): Float?
    
    @Query("SELECT * FROM transaction_logs WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionLog>

    @Query("UPDATE transaction_logs SET isSynced = 1 WHERE transactionId = :id")
    suspend fun markTransactionAsSynced(id: String)

    @Query("SELECT * FROM inventory_logs WHERE isSynced = 0")
    suspend fun getUnsyncedInventoryLogs(): List<InventoryLog>

    @Query("UPDATE inventory_logs SET isSynced = 1 WHERE logId = :id")
    suspend fun markInventoryLogAsSynced(id: Int)
}
