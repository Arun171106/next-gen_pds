package com.example.nextgen_pds_kiosk.data.model

import com.google.gson.annotations.SerializedName

data class DispenserStateResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("current_weight_g")
    val currentWeightG: Float,
    
    @SerializedName("target_weight_g")
    val targetWeightG: Float
)
