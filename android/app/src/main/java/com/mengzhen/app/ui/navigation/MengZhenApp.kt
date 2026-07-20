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
        }
    }
}
