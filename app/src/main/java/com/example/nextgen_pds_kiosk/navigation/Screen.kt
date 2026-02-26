package com.example.nextgen_pds_kiosk.navigation

sealed class Screen(val route: String) {
    object Welcome         : Screen("welcome_screen")
    object QrScan          : Screen("qr_scan_screen")
    object CardDetails     : Screen("card_details/{cardNo}") {
        fun createRoute(cardNo: String) = "card_details/$cardNo"
    }
    object FaceVerify      : Screen("face_verify/{cardNo}/{memberId}") {
        fun createRoute(cardNo: String, memberId: String) = "face_verify/$cardNo/$memberId"
    }
    object QuantitySelection : Screen("quantity_selection_screen")
    object Dispensing      : Screen("dispensing_screen")
    object Completion      : Screen("completion_screen")
    object AdminLogin      : Screen("admin_login_screen")
    object AdminDashboard  : Screen("admin_dashboard_screen")
    object Enrollment      : Screen("enrollment_screen")
    // Legacy — kept for admin enrollment camera only
    object Identification  : Screen("identification_screen")
}
