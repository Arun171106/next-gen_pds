package com.example.nextgen_pds_kiosk.data

import android.content.Context
import com.example.nextgen_pds_kiosk.data.local.BeneficiaryDao
import com.example.nextgen_pds_kiosk.data.model.FamilyMember
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val beneficiaryDao: BeneficiaryDao
) {

    private val gson = Gson()

    /**
     * Seeds the database from assets if it is empty (first launch).
     * This is idempotent — safe to call on every app start.
     */
    suspend fun seedIfEmpty() {
        withContext(Dispatchers.IO) {
            val count = beneficiaryDao.getCount()
            if (count > 0) return@withContext // Already seeded

            val beneficiaries = mutableListOf<Beneficiary>()

            // =====================================================
            // 1. Special Real Beneficiary Card (Pravin + Priyan)
            // =====================================================
            val pravinPhoto = loadAssetBytes("sampledata/pravin_arun.png")
            val priyanPhoto = loadAssetBytes("sampledata/priyan.png")

            val realMembers = listOf(
                FamilyMember("M001", "Pravin Arun RJ", "Head", "Male", 26, "XXXX-XXXX-0001"),
                FamilyMember("M002", "Priyan U", "Brother", "Male", 23, "XXXX-XXXX-0002")
            )
            val realMembersJson = gson.toJson(realMembers)

            // Pravin's record (face photo linked)
            beneficiaries.add(
                Beneficiary(
                    beneficiaryId = "TN-KL-2026-PRAVIN-001",
                    name = "Pravin Arun RJ",
                    embeddingVector = FloatArray(0),
                    photoData = pravinPhoto,
                    metadata = "Head of Family",
                    smartCardNo = "TN-KL-2026-PRAVIN-001",
                    cardType = "PHH",
                    address = "1, RJ Nagar, Coimbatore, Tamil Nadu",
                    mobile = "9800000001",
                    membersJson = realMembersJson,
                    riceKg = 20,
                    wheatKg = 5,
                    sugarKg = 2,
                    dalKg = 2,
                    keroseneL = 0,
                    status = "ACTIVE"
                )
            )

            // Priyan's record (same card, different face photo)
            beneficiaries.add(
                Beneficiary(
                    beneficiaryId = "TN-KL-2026-PRIYAN-001",
                    name = "Priyan U",
                    embeddingVector = FloatArray(0),
                    photoData = priyanPhoto,
                    metadata = "Member",
                    smartCardNo = "TN-KL-2026-PRAVIN-001",   // Same card as Pravin
                    cardType = "PHH",
                    address = "1, RJ Nagar, Coimbatore, Tamil Nadu",
                    mobile = "9800000001",
                    membersJson = realMembersJson,
                    riceKg = 20,
                    wheatKg = 5,
                    sugarKg = 2,
                    dalKg = 2,
                    keroseneL = 0,
                    status = "ACTIVE"
                )
            )

            // Insert only the real beneficiaries
            if (beneficiaries.isNotEmpty()) {
                beneficiaryDao.insertAll(beneficiaries)
            }
        }
    }

    private fun loadAssetBytes(path: String): ByteArray? {
        return try {
            context.assets.open(path).use { it.readBytes() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
