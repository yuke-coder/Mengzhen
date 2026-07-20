package com.mengzhen.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mengzhen.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun LandingScreen(navController: NavController) {
    var logoVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var cardsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        logoVisible = true
        delay(300)
        subtitleVisible = true
        delay(200)
        cardsVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Logo + 标题
            Box(
                modifier = Modifier
                    .alpha(if (logoVisible) 1f else 0f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Logo 圆形
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(BrandStart, BrandEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("梦", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "梦枕",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // 副标题
            Text(
                "上传音频 · 自定义定时 · 自动助眠播放",
                fontSize = 14.sp,
                color = MutedForeground,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .alpha(if (subtitleVisible) 1f else 0f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 功能卡片
            if (cardsVisible) {
                FeatureCard(
                    icon = Icons.Default.Upload,
                    title = "上传音频",
                    desc = "支持 MP3、WAV、M4A 等格式，上传后随时播放"
                )
                Spacer(Modifier.height(12.dp))
                FeatureCard(
                    icon = Icons.Default.Schedule,
                    title = "定时播放",
                    desc = "设置时间，到点自动播放，支持每天/工作日/节假日重复"
                )
                Spacer(Modifier.height(12.dp))
                FeatureCard(
                    icon = Icons.Default.GraphicEq,
                    title = "渐入渐出",
                    desc = "音量平滑渐入开始、渐出结束，温和不突兀"
                )
                Spacer(Modifier.height(12.dp))
                FeatureCard(
                    icon = Icons.Default.Shield,
                    title = "息屏保活",
                    desc = "前台服务 + WakeLock，息屏后持续播放不被杀死"
                )
                Spacer(Modifier.height(12.dp))
                FeatureCard(
                    icon = Icons.Default.Headphones,
                    title = "线控支持",
                    desc = "耳机按键控制播放/暂停、上一首/下一首"
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // CTA 按钮
            Button(
                onClick = { navController.navigate("settings") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("开始使用", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { navController.navigate("login") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("登录 / 注册", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 底部信息
            Text(
                "深夜助眠播放器 · 自定义音频",
                fontSize = 11.sp,
                color = MutedForeground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(desc, fontSize = 12.sp, color = MutedForeground, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
