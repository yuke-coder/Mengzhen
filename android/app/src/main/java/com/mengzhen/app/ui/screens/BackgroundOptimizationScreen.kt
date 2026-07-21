package com.mengzhen.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mengzhen.app.compat.VendorCompat
import com.mengzhen.app.ui.theme.*
import androidx.activity.ComponentActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundOptimizationScreen(navController: NavController) {
    val context = LocalContext.current
    val brand = remember { VendorCompat.detectBrand() }
    val brandName = when (brand) {
        VendorCompat.Brand.HUAWEI -> "华为"
        VendorCompat.Brand.HONOR -> "荣耀"
        VendorCompat.Brand.XIAOMI -> "小米"
        VendorCompat.Brand.OPPO -> "OPPO/一加/真我"
        VendorCompat.Brand.VIVO -> "VIVO/iQOO"
        VendorCompat.Brand.SAMSUNG -> "三星"
        VendorCompat.Brand.OTHER -> "通用"
    }

    val permissions = remember {
        VendorCompat.checkAllPermissions(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("后台保活设置") },
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
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // 品牌检测
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BatteryFull, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("检测到设备：$brandName", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Foreground)
                        Text("为确保息屏播放稳定运行，请开启以下权限", fontSize = 13.sp, color = MutedForeground)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (permissions.isEmpty()) {
                // 全部已开启
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("所有权限已开启", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Primary)
                    }
                }
            } else {
                // 权限列表
                permissions.forEach { item ->
                    PermissionCard(item.name, item.description) {
                        val activity = context as? ComponentActivity
                        activity?.let { item.openSettings(it) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    name: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MutedForeground, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Foreground)
                Text(description, fontSize = 13.sp, color = MutedForeground)
            }
            Text("去设置 ›", fontSize = 13.sp, color = Primary, fontWeight = FontWeight.Medium)
        }
    }
}
