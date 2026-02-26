package com.example.nextgen_pds_kiosk.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.nextgen_pds_kiosk.data.Beneficiary

@Database(entities = [Beneficiary::class, TransactionLog::class, InventoryLog::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun beneficiaryDao(): BeneficiaryDao
    abstract fun analyticsDao(): AnalyticsDao
}
