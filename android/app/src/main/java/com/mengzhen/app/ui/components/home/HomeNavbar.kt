package com.mengzhen.app.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.mengzhen.app.R
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.UserInfo
import com.mengzhen.app.data.model.parseUser
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.components.main.absoluteAvatarUrl
import com.mengzhen.app.ui.navigation.Screen
import com.mengzhen.app.ui.theme.*
import com.mengzhen.app.util.genderLabel
import com.mengzhen.app.util.isGenderSecret
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Navbar 紫渐变（Web 端 from-purple-500 via-purple-600 to-fuchsia-500） */
private val NavbarPurpleGradient
    @Composable get() = Brush.horizontalGradient(
        listOf(Color(0xFFA855F7), Color(0xFF9333EA), Color(0xFFD946EF))
    )

/**
 * 首页顶栏 —— 像素级复刻 Web 端 navbar.tsx 移动端（< md 断点）
 * - 左：logo 28dp + 紫渐变"梦枕"（中间导航链接与标语为 md+ 专属，移动端不渲染）
 * - 右：UserMenu + ThemeToggle（gap 8dp）
 * - nav-fade-edge：半透明底 + 底部 75% 渐隐 + 品牌色渐变光条（Compose 无 backdrop-blur，半透明近似）
 */
@Composable
fun HomeNavbar(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val dark = LocalIsDarkTheme.current
    Box(modifier = modifier.fillMaxWidth()) {
        // 渐隐底（Web：blur 6px + mask 底部渐隐 → 半透明渐变近似）
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp) // 48dp 栏高 + 8dp 渐隐余量
                .background(
                    Brush.verticalGradient(
                        0f to (if (dark) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.08f)),
                        0.75f to (if (dark) Color.Black.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.06f)),
                        1f to Color.Transparent,
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左：Logo + 品牌名
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = R.drawable.logo,
                    contentDescription = "梦枕",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "梦枕",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp, // tracking-tight
                    style = androidx.compose.ui.text.TextStyle(brush = NavbarPurpleGradient),
                )
            }
            // 右：用户菜单
            // 注：这里原本还有一个 HomeThemeToggle（Web theme-toggle.tsx 的 Compose 复刻版），
            // 已删除 —— 主页面顶栏已有同一功能的真本实现 QqThemeModePopupWindow
            // （含浅色/深色/自动），留着就是同一套主题切换的两份 UI。
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeUserMenu(navController)
            }
        }
        // 品牌色渐变光条（Web nav-fade-edge::after：bottom -5px, h-10px, blur 4px）
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 5.dp)
                .fillMaxWidth()
                .height(10.dp)
                .blur(4.dp)
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.35f to BrandStartThemed.copy(alpha = 0.22f * 0.72f),
                        0.65f to BrandEndThemed.copy(alpha = 0.18f * 0.72f),
                        1f to Color.Transparent,
                    )
                )
        )
    }
}

// ==================== 用户菜单 ====================

/**
 * UserMenu —— 复刻 Web 端 user-menu.tsx 移动端
 * - 未登录：36dp 圆形登录图标按钮 → 登录页
 * - 已登录：36dp 圆形头像（border brand-start/30）→ 点击展开下拉
 *   （ProfileCard + 我的音频 + 建议反馈 + 退出登录；hover 行为为桌面端专属，移动端仅点击切换）
 */
@Composable
fun HomeUserMenu(navController: NavController) {
    val context = LocalContext.current
    val store = remember { TaskStore.get(context) }
    val api = remember(context) { ApiClient.get(context) }
    val scope = rememberCoroutineScope()
    val user by store.sessionUser.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    // 进入时静默刷新用户信息（失败忽略，与资料页同链路）
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val res = api.me()
                if (res.optBoolean("authenticated", false)) {
                    val fresh = parseUser(res)
                    if (fresh != null) store.saveUserSession("cookie_session", fresh)
                } else if (res.optBoolean("authenticated", true) == false) {
                    store.clearSession()
                }
            } catch (_: Exception) {
            }
        }
    }

    if (user == null) {
        // 未登录：圆形登录图标按钮（w-9 h-9, LogIn 18dp, muted）
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { navController.navigate(Screen.Login.route) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Login,
                contentDescription = "登录",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        return
    }

    val u = user!!
    Box {
        // 头像按钮（36dp 圆 + border-2 brand-start/30）
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            BrandStartThemed.copy(alpha = 0.2f),
                            BrandEndThemed.copy(alpha = 0.2f),
                        )
                    )
                )
                .clickable { expanded = !expanded },
            contentAlignment = Alignment.Center,
        ) {
            if (!u.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = absoluteAvatarUrl(u.avatarUrl),
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 4.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            modifier = Modifier.widthIn(min = 240.dp, max = 300.dp),
        ) {
            UserProfileCard(u, onEditProfile = {
                expanded = false
                navController.navigate(Screen.ProfileEdit.route)
            })

            // 菜单项
            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                UserMenuItem(
                    icon = { Icon(Icons.Default.History, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    label = "我的音频",
                ) {
                    expanded = false
                    navController.navigate(Screen.History.route)
                }
                UserMenuItem(
                    icon = { Icon(Icons.Default.EditNote, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    label = "建议反馈",
                ) {
                    expanded = false
                    navController.navigate(Screen.Feedback.route)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                UserMenuItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) },
                    label = "退出登录",
                    labelColor = MaterialTheme.colorScheme.error,
                ) {
                    expanded = false
                    scope.launch(Dispatchers.IO) {
                        try { api.logout() } catch (_: Exception) {}
                        store.clearSession()
                        withContext(Dispatchers.Main) {
                            navController.navigate(Screen.Landing.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** ProfileCard（Web user-menu.tsx 内部组件）：头像 + 昵称 + 签名 + 编辑资料 + 信息行 */
@Composable
private fun UserProfileCard(user: UserInfo, onEditProfile: () -> Unit) {
    // 与 Web user-menu.tsx 的 ProfileCard 行为保持一致：保密中的字段整行不渲染。
    // 可见性直接读服务端下发的 hide_birthday / hide_region（与 Web 端共用同一后端字段），
    // 只有明确为 true 才隐藏 —— null 表示服务端没下发该字段，按可见处理。
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 头像 48dp + border-2 brand-start/30
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                BrandStartThemed.copy(alpha = 0.2f),
                                BrandEndThemed.copy(alpha = 0.2f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!user.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = absoluteAvatarUrl(user.avatarUrl),
                        contentDescription = "头像",
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    user.nickname ?: user.username,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                if (user.nickname != null) {
                    Text(
                        "@${user.username}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            user.signature ?: "暂无签名",
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic,
            color = if (user.signature != null) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            maxLines = 2,
            modifier = Modifier.padding(start = 4.dp),
        )

        // 编辑个人资料按钮
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                .clickable(onClick = onEditProfile)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "编辑个人资料",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 信息行（位置/性别/生日/简介）—— 保密中的不渲染
        val infoRows = buildList<@Composable () -> Unit> {
            user.location
                ?.takeIf { user.hideRegion != true }
                ?.let { loc ->
                    add { UserInfoRow(Icons.Default.Place, loc) }
                }
            // 性别：保密态整行消失；文案走共享的 genderLabel()，
            // 不用手写 `if (g == "male") "男" else "女"` —— 那样任何其它值都会被显示成「女」
            if (!user.isGenderSecret()) {
                user.genderLabel()?.let { label ->
                    add { UserInfoRow(Icons.Default.Favorite, label) }
                }
            }
            user.birthday
                ?.takeIf { user.hideBirthday != true }
                ?.let { b ->
                    add { UserInfoRow(Icons.Default.DateRange, b) }
                }
            user.bio?.let { bio ->
                add { UserInfoRow(Icons.Default.Description, bio, maxLines = 3) }
            }
        }
        if (infoRows.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            infoRows.forEach { row ->
                row()
                Spacer(Modifier.height(6.dp))
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun UserInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    maxLines: Int = 1,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = BrandStartThemed.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
        )
    }
}

@Composable
private fun UserMenuItem(
    icon: @Composable () -> Unit,
    label: String,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = labelColor)
    }
}
