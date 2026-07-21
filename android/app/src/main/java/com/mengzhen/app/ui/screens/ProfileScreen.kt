package com.mengzhen.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.parseUser
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.navigation.Screen
import com.mengzhen.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { TaskStore.get(context) }
    val api = remember { ApiClient.get() }
    var userInfo by remember { mutableStateOf(store.getSession()?.second) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val res = api.me()
                val user = parseUser(res)
                if (user != null) {
                    store.saveUserSession("cookie_session", user)
                    withContext(Dispatchers.Main) { userInfo = user }
                }
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人中心") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // 头像
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(BrandStart, BrandEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                userInfo?.username ?: "未登录",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Foreground
            )
            Spacer(Modifier.height(32.dp))

            // 菜单项
            ProfileMenuItem("历史记录", "查看已保存的音频", Icons.Default.History) {
                navController.navigate(Screen.History.route)
            }
            ProfileMenuItem("设置", "播放偏好与定时配置", Icons.Default.Settings) {
                navController.navigate(Screen.Settings.route)
            }
            ProfileMenuItem("意见反馈", "问题反馈与建议", Icons.Default.Feedback) {
                navController.navigate(Screen.Feedback.route)
            }

            Spacer(Modifier.height(24.dp))

            // 退出登录
            if (userInfo != null) {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        try { api.logout() } catch (_: Exception) {}
                        store.clearSession()
                        withContext(Dispatchers.Main) {
                            navController.navigate("landing") { popUpTo(0) { inclusive = true } }
                        }
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Destructive, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("退出登录", color = Destructive, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Foreground)
                Text(subtitle, fontSize = 13.sp, color = MutedForeground)
            }
            Spacer(Modifier.weight(1f))
            Text("›", fontSize = 20.sp, color = MutedForeground)
        }
    }
}
