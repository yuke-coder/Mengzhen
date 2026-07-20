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
    object BackgroundOptimization : Screen("bg_optimization")
}
