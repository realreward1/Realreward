package com.example.ui.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val TASKS = "tasks"
    const val WALLET = "wallet"
    const val VIP = "vip"
    const val PROFILE = "profile"

    const val TASK_DETAILS = "task_details/{taskId}"
    fun taskDetails(taskId: String) = "task_details/$taskId"

    const val WITHDRAWAL = "withdrawal"
    const val LEADERBOARD = "leaderboard"
    const val NOTIFICATIONS = "notifications"
    const val SUPPORT = "support"

    const val LEGAL = "legal/{section}"
    fun legal(section: String) = "legal/$section"

    // Owner-Only Admin Routes (gated by email verification + passcode lock)
    const val ADMIN_LOCK = "owner_admin_lock"
    const val ADMIN_PANEL = "owner_admin_panel"
}
