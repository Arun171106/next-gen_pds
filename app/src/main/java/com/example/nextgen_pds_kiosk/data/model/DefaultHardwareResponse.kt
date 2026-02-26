package com.example.nextgen_pds_kiosk.data.model

import com.google.gson.annotations.SerializedName

data class DefaultHardwareResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("message")
    val message: String?,
    
    @SerializedName("error")
    val error: String?
)
