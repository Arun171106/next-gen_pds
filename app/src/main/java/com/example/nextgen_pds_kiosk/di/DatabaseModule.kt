package com.example.nextgen_pds_kiosk.di

import android.content.Context
import androidx.room.Room
import com.example.nextgen_pds_kiosk.data.local.AppDatabase
import com.example.nextgen_pds_kiosk.data.local.BeneficiaryDao
import com.example.nextgen_pds_kiosk.data.local.AnalyticsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nextgen_kiosk_database"
        ).fallbackToDestructiveMigration()
         .build()
    }

    @Provides
    fun provideBeneficiaryDao(appDatabase: AppDatabase): BeneficiaryDao {
        return appDatabase.beneficiaryDao()
    }

    @Provides
    fun provideAnalyticsDao(appDatabase: AppDatabase): AnalyticsDao {
        return appDatabase.analyticsDao()
    }
}
