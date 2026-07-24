package com.mengzhen.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mengzhen.app.ui.screens.LandingScreen
import com.mengzhen.app.ui.screens.SettingsScreen
import com.mengzhen.app.ui.screens.LoginScreen
import com.mengzhen.app.ui.screens.RegisterScreen
import com.mengzhen.app.ui.screens.HistoryScreen
import com.mengzhen.app.ui.screens.ProfileScreen
import com.mengzhen.app.ui.screens.FeedbackScreen
import com.mengzhen.app.ui.screens.PermissionSettingsScreen
import com.mengzhen.app.ui.screens.PermissionTutorialScreen

@Composable
fun MengZhenApp() {
    val navController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Screen.Landing.route) {
            composable(Screen.Landing.route) {
                LandingScreen(navController = navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
            composable(Screen.Login.route) {
                LoginScreen(navController = navController)
            }
            composable(Screen.Register.route) {
                RegisterScreen(navController = navController)
            }
            composable(Screen.History.route) {
                HistoryScreen(navController = navController)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(navController = navController)
            }
            composable(Screen.Feedback.route) {
                FeedbackScreen(navController = navController)
            }
            composable(Screen.PermissionSettings.route) {
                PermissionSettingsScreen(navController = navController)
            }
            composable(Screen.PermissionTutorial.route) { backStackEntry ->
                PermissionTutorialScreen(
                    navController = navController,
                    permissionKey = backStackEntry.arguments?.getString("permissionKey") ?: "",
                )
            }
        }
    }
}
