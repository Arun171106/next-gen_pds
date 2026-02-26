package com.example.nextgen_pds_kiosk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgen_pds_kiosk.data.Beneficiary
import com.example.nextgen_pds_kiosk.data.BeneficiaryRepository
import com.example.nextgen_pds_kiosk.data.local.AnalyticsDao
import com.example.nextgen_pds_kiosk.data.local.InventoryLog
import com.example.nextgen_pds_kiosk.data.local.TransactionLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repository: BeneficiaryRepository,
    private val analyticsDao: AnalyticsDao
) : ViewModel() {

    // Directly bind the offline SQLite table to the compose UI LazyColumn
    val beneficiaries: Flow<List<Beneficiary>> = repository.getAllBeneficiariesFlow()
    val transactions: Flow<List<TransactionLog>> = analyticsDao.getAllTransactionsFlow()
    val inventoryLogs: Flow<List<InventoryLog>> = analyticsDao.getAllInventoryLogsFlow()

    fun deleteBeneficiary(beneficiary: Beneficiary) {
        viewModelScope.launch {
            // Depending on architecture, you'd add delete inside BeneficiaryDao and Repository.
            // Let's add delete to BeneficiaryDao directly if we don't have it on repository
             repository.deleteBeneficiary(beneficiary)
        }
    }
}
