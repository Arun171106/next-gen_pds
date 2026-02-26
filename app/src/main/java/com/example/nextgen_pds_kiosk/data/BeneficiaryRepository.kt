package com.example.nextgen_pds_kiosk.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nextgen_pds_kiosk.data.local.BeneficiaryDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Entity(tableName = "beneficiaries")
data class Beneficiary(
    @PrimaryKey val beneficiaryId: String,   // smartCardNo used as PK
    val name: String,                         // headOfFamily.name
    val embeddingVector: FloatArray,
    val photoData: ByteArray? = null,
    val metadata: String = "",
    // Smart Card Fields
    val smartCardNo: String = "",
    val cardType: String = "",                // PHH / NPHH / AAY
    val address: String = "",
    val mobile: String = "",
    val membersJson: String = "",             // Gson-serialized List<FamilyMember>
    val riceKg: Int = 0,
    val wheatKg: Int = 0,
    val sugarKg: Int = 0,
    val dalKg: Int = 0,
    val keroseneL: Int = 0,
    val status: String = "ACTIVE"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Beneficiary
        if (beneficiaryId != other.beneficiaryId) return false
        return true
    }

    override fun hashCode(): Int {
        return beneficiaryId.hashCode()
    }
}

@Singleton
class BeneficiaryRepository @Inject constructor(
    private val beneficiaryDao: BeneficiaryDao
) {
    
    fun getAllBeneficiariesFlow(): Flow<List<Beneficiary>> {
        return beneficiaryDao.getAllBeneficiariesFlow()
    }
    
    suspend fun getAllBeneficiariesList(): List<Beneficiary> {
        return beneficiaryDao.getAllBeneficiariesList()
    }
    
    suspend fun addBeneficiary(beneficiary: Beneficiary) {
        beneficiaryDao.insertBeneficiary(beneficiary)
    }
    
    suspend fun getBeneficiaryById(id: String): Beneficiary? {
        return beneficiaryDao.getById(id)
    }
    
    suspend fun updateBeneficiary(beneficiary: Beneficiary) {
        beneficiaryDao.updateBeneficiary(beneficiary)
    }
    
    suspend fun clear() {
        beneficiaryDao.deleteAll()
    }
    
    suspend fun deleteBeneficiary(beneficiary: Beneficiary) {
        beneficiaryDao.deleteBeneficiary(beneficiary)
    }
}
