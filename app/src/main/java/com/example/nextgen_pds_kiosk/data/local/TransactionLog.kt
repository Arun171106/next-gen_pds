package com.example.nextgen_pds_kiosk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_logs")
data class TransactionLog(
    @PrimaryKey val transactionId: String,
    val beneficiaryId: String,
    val beneficiaryName: String,
    val amountDispensed: Float,
    val timestamp: Long,
    val isSynced: Boolean = false
)
