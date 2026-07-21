package com.mengzhen.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var content by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    val api = remember { ApiClient.get() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("意见反馈") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
        ) {
            Spacer(Modifier.height(16.dp))

            Text("遇到问题了？有什么建议？告诉我们", color = MutedForeground, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("反馈内容") },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("联系方式（选填）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (content.isBlank()) {
                        result = "请输入反馈内容"
                        return@Button
                    }
                    loading = true
                    result = null
                    scope.launch(Dispatchers.IO) {
                        try {
                            val res = api.submitFeedback(content, contact.ifBlank { null })
                            withContext(Dispatchers.Main) {
                                if (res.optBoolean("success", false)) {
                                    result = "反馈已提交，感谢！"
                                    content = ""
                                    contact = ""
                                } else {
                                    result = res.optString("error", "提交失败")
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { result = e.message ?: "提交失败" }
                        } finally {
                            withContext(Dispatchers.Main) { loading = false }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                enabled = !loading,
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("提交反馈", fontSize = 16.sp)
                }
            }

            result?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = if (it.contains("感谢")) Primary else Destructive, fontSize = 14.sp)
            }
        }
    }
}
