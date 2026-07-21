package com.mengzhen.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.UserInfo
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { TaskStore.get(context) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val api = remember { ApiClient.get() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录") },
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(BrandStart, BrandEnd))),
                contentAlignment = Alignment.Center
            ) {
                Text("梦", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Destructive, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        error = "请输入用户名和密码"
                        return@Button
                    }
                    loading = true
                    error = null
                    scope.launch(Dispatchers.IO) {
                        try {
                            val result = api.login(username, password)
                            if (result.optBoolean("success", false)) {
                                val user = result.optJSONObject("user")
                                val userInfo = UserInfo(
                                    id = user?.optString("id") ?: "",
                                    username = user?.optString("username") ?: username,
                                )
                                // cookie 已由 OkHttp CookieJar 自动保存
                                store.saveUserSession("cookie_session", userInfo)
                                withContext(Dispatchers.Main) {
                                    navController.navigate("settings") {
                                        popUpTo("landing") { inclusive = false }
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    error = result.optString("error", "登录失败")
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                error = e.message ?: "登录失败"
                            }
                        } finally {
                            withContext(Dispatchers.Main) { loading = false }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                enabled = !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White, strokeWidth = 2.dp
                    )
                } else {
                    Text("登录", fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = { navController.navigate("register") }) {
                Text("没有账号？去注册", color = Primary, fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { TaskStore.get(context) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val api = remember { ApiClient.get() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("注册") },
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码（至少6位）") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Destructive, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        error = "请填写用户名和密码"
                        return@Button
                    }
                    if (password.length < 6) {
                        error = "密码长度不能少于 6 位"
                        return@Button
                    }
                    loading = true
                    error = null
                    scope.launch(Dispatchers.IO) {
                        try {
                            val result = api.register(username, password)
                            if (result.optBoolean("success", false)) {
                                val user = result.optJSONObject("user")
                                val userInfo = UserInfo(
                                    id = user?.optString("id") ?: "",
                                    username = user?.optString("username") ?: username,
                                )
                                store.saveUserSession("cookie_session", userInfo)
                                withContext(Dispatchers.Main) {
                                    navController.navigate("settings") {
                                        popUpTo("landing") { inclusive = false }
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    error = result.optString("error", "注册失败")
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                error = e.message ?: "注册失败"
                            }
                        } finally {
                            withContext(Dispatchers.Main) { loading = false }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                enabled = !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White, strokeWidth = 2.dp
                    )
                } else {
                    Text("注册", fontSize = 16.sp)
                }
            }
        }
    }
}
