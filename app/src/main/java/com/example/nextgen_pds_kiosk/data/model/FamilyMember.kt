package com.example.nextgen_pds_kiosk.data.model

data class FamilyMember(
    val memberId: String,
    val name: String,
    val relation: String,
    val gender: String,
    val age: Int,
    val aadhaarMasked: String
)
