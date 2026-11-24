package com.university.queueping.navigation

sealed class Screen(val route: String) {
    object RoleSelection : Screen("role_selection")
    object CustomerHome : Screen("customer_home")
    object QRScanner : Screen("qr_scanner")
    object QueueConfirmation : Screen("queue_confirmation/{queueId}") {
        fun createRoute(queueId: String) = "queue_confirmation/$queueId"
    }
    object QueueStatus : Screen("queue_status/{queueId}") {
        fun createRoute(queueId: String) = "queue_status/$queueId"
    }
    object YourTurn : Screen("your_turn/{queueId}") {
        fun createRoute(queueId: String) = "your_turn/$queueId"
    }

    object StaffLogin : Screen("staff_login")
    object StaffHome : Screen("staff_home")
    object CreateQueue : Screen("create_queue")
    object QueueDashboard : Screen("queue_dashboard/{queueId}") {
        fun createRoute(queueId: String) = "queue_dashboard/$queueId"
    }
    object QueueQR : Screen("queue_qr/{queueId}") {
        fun createRoute(queueId: String) = "queue_qr/$queueId"
    }
}
