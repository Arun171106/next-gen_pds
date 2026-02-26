package com.example.nextgen_pds_kiosk.data.api

import com.example.nextgen_pds_kiosk.data.model.DefaultHardwareResponse
import com.example.nextgen_pds_kiosk.data.model.DispenserStateResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface DispenserApiService {

    @GET("status")
    suspend fun getStatus(): Response<DispenserStateResponse>

    @POST("tare")
    suspend fun tareScale(): Response<DefaultHardwareResponse>

    @POST("dispense")
    suspend fun startDispensing(
        @Query("target") targetWeightGrams: Float
    ): Response<DefaultHardwareResponse>

    @POST("pause")
    suspend fun pauseDispensing(): Response<DefaultHardwareResponse>

    @POST("resume")
    suspend fun resumeDispensing(): Response<DefaultHardwareResponse>

    @POST("stop")
    suspend fun stopDispensing(): Response<DefaultHardwareResponse>
}
