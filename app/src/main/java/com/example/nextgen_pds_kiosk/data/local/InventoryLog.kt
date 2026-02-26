package com.example.nextgen_pds_kiosk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_logs")
data class InventoryLog(
    @PrimaryKey(autoGenerate = true) val logId: Int = 0,
    val itemName: String,
    val amountAddedOrRemoved: Float,
    val currentTotal: Float,
    val timestamp: Long,
    val actionType: String, // e.g., "RESTOCK", "DISPENSE"
    val isSynced: Boolean = false
)
