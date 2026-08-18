package com.mengzhen.app.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mengzhen.app.R
import com.mengzhen.app.bilibili.BiliOfficialClient
import com.mengzhen.app.ui.components.ChatGptLoadingSpinner
import com.mengzhen.app.ui.navigation.Screen

@Composable
fun BiliAuthorizationScreen(navController: NavController) {
    val context = LocalContext.current
    val official = remember(context) { BiliOfficialClient(context) }
    var launchAttempted by rememberSaveable { mutableStateOf(false) }
    var launchFailed by rememberSaveable { mutableStateOf(false) }

    fun finishAuthorization() {
        val removedPreviousCache = navController.popBackStack(
            route = Screen.BiliCache.route,
            inclusive = true,
        )
        navController.navigate(Screen.BiliCache.route) {
            if (!removedPreviousCache) {
                popUpTo(Screen.BiliAuthorization.route) { inclusive = true }
            }
            launchSingleTop = true
        }
    }

    val authorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (
            result.resultCode == Activity.RESULT_OK &&
            !official.acceptAuthorizationResult(result.resultCode, result.data)
        ) {
            launchFailed = true
        } else {
            finishAuthorization()
        }
    }

    fun launchAuthorization() {
        launchFailed = false
        runCatching {
            authorizationLauncher.launch(official.authorizationIntent())
        }.onFailure {
            launchFailed = true
        }
    }

    BackHandler(onBack = ::finishAuthorization)

    LaunchedEffect(Unit) {
        if (!launchAttempted) {
            launchAttempted = true
            launchAuthorization()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        if (launchFailed) {
            Text(
                text = "授权未完成，请重试",
                color = colorResource(R.color.Ga8),
                fontSize = 15.sp,
            )
            Button(
                onClick = ::launchAuthorization,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.Pi5),
                    contentColor = Color.White,
                ),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(text = "重新授权")
            }
        } else {
            ChatGptLoadingSpinner(
                size = 28.dp,
                color = colorResource(R.color.Pi5),
                loadingDescription = "正在打开哔哩哔哩授权页",
            )
            Text(
                text = "正在打开哔哩哔哩授权页…",
                color = colorResource(R.color.Ga5),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
