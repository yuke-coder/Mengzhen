package com.mengzhen.app.ui.navigation

sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Settings : Screen("settings")
    object Login : Screen("login")
    object Register : Screen("register")
    object History : Screen("history")
    object Templates : Screen("templates")
    object Profile : Screen("profile")
    object Feedback : Screen("feedback")
    object PermissionSettings : Screen("permission_settings")
    object PermissionTutorial : Screen("permission_tutorial/{permissionKey}") {
        fun createRoute(permissionKey: com.mengzhen.app.data.tutorial.PermissionKey) =
            "permission_tutorial/${permissionKey.prefKey}"
    }
}
