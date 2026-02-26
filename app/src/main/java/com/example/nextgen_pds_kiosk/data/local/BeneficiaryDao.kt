package com.example.nextgen_pds_kiosk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nextgen_pds_kiosk.data.Beneficiary
import kotlinx.coroutines.flow.Flow

@Dao
interface BeneficiaryDao {

    @Query("SELECT * FROM beneficiaries")
    fun getAllBeneficiariesFlow(): Flow<List<Beneficiary>>

    @Query("SELECT * FROM beneficiaries")
    suspend fun getAllBeneficiariesList(): List<Beneficiary>

    @Query("SELECT * FROM beneficiaries WHERE beneficiaryId = :id LIMIT 1")
    suspend fun getById(id: String): Beneficiary?

    @Query("SELECT COUNT(*) FROM beneficiaries")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeneficiary(beneficiary: Beneficiary)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(beneficiaries: List<Beneficiary>)

    @Update
    suspend fun updateBeneficiary(beneficiary: Beneficiary)

    @Query("DELETE FROM beneficiaries")
    suspend fun deleteAll()

    @androidx.room.Delete
    suspend fun deleteBeneficiary(beneficiary: Beneficiary)
}
