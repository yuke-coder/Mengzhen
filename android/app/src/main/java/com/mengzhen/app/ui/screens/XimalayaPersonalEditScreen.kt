package com.mengzhen.app.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.UserInfo
import com.mengzhen.app.data.model.parseProfile
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.components.ChatGptLoadingSpinner
import com.mengzhen.app.ui.components.main.absoluteAvatarUrl
import com.mengzhen.app.ui.components.rememberQqMusicImagePicker
import com.mengzhen.app.ui.feedback.AppNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 喜马拉雅 MyDetailFragment 直接迁移 — 非仿写。
 *
 * 源码文件对照：
 * - MyDetailFragment.java → 主页面布局与数据绑定（a() 方法）
 * - EditPersonalInfoFragment.java → 昵称(type=1)/生日(type=2)/简介(type=3) 编辑子页
 * - RegionSelectFragment.java → 省市两级地区选择
 * - MyDetailInfo.java → 数据模型
 * - com.ximalaya.ting.android.main.dialog.c → 性别选择 Dialog
 *
 * 布局结构（从设备 UI dump 还原）：
 * 1. 标题栏：返回 | 编辑资料
 * 2. 背景图（375:200 比例）+ 阴影遮罩 + 居中头像(88dp) + 编辑头像图标 + "编辑主页背景"
 * 3. 完善资料引导（本地计算完成度百分比 + 进度条）
 * 4. 昵称行（完善度 +30%）
 * 5. 性别行（完善度 +20%）
 * 6. 生日行（完善度 +10%）
 * 7. 地区行（完善度 +10%）
 * 8. [间距]
 * 9. 简介行
 *
 * 不含：声音签名、标签、认证、同步微信/QQ（Mengzhen API 不支持）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XimalayaPersonalEditScreen(navController: NavController) {
    val context = LocalContext.current
    val store = remember(context) { TaskStore.get(context) }
    val api = remember(context) { ApiClient.get(context) }
    val scope = rememberCoroutineScope()
    val sessionUser by store.sessionUser.collectAsState()
    var profile by remember(sessionUser) { mutableStateOf(sessionUser) }
    var saving by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var editor by remember { mutableStateOf<EditorTarget?>(null) }
    var showGenderPicker by remember { mutableStateOf(false) }
    var showRegionPicker by remember { mutableStateOf(false) }
    var showBirthdayPicker by remember { mutableStateOf(false) }

    // MyDetailFragment.loadData() → e() → b.cG(map, callback)
    LaunchedEffect(Unit) {
        loading = true
        runCatching {
            withContext(Dispatchers.IO) { parseProfile(api.getProfile()) }
        }.onSuccess { remote ->
            if (remote != null) {
                val merged = remote.copy(backgroundUrl = remote.backgroundUrl ?: profile?.backgroundUrl)
                profile = merged
                store.getSession()?.first?.let { token -> store.saveUserSession(token, merged) }
            }
        }.onFailure { }
        loading = false
    }

    // MyDetailFragment.a(String, int) → 头像上传
    val avatarPicker = rememberQqMusicImagePicker(maxSelection = 1) { selected ->
        val uri = selected.firstOrNull() ?: return@rememberQqMusicImagePicker
        scope.launch {
            saving = true
            val file = withContext(Dispatchers.IO) {
                persistProfileFile(context, uri, "avatar")
            }
            if (file == null) {
                AppNotice.error(context, "头像读取失败，请重试")
            } else {
                runCatching {
                    withContext(Dispatchers.IO) {
                        api.uploadAvatar(file, context.contentResolver.getType(uri) ?: "image/jpeg")
                    }
                }.onSuccess { response ->
                    val url = response.optString("avatar_url", "").ifBlank { null }
                    if (response.optBoolean("success") && url != null) {
                        val next = (profile ?: UserInfo()).copy(avatarUrl = url)
                        profile = next
                        store.getSession()?.first?.let { token -> store.saveUserSession(token, next) }
                        AppNotice.success(context, "头像已更新")
                    } else {
                        AppNotice.error(context, response.optString("error", "头像更新失败"))
                    }
                }.onFailure { AppNotice.error(context, it.message ?: "头像更新失败") }
                file.delete()
            }
            saving = false
        }
    }

    // MyDetailFragment.d(String) → 背景图上传
    val backgroundPicker = rememberQqMusicImagePicker(maxSelection = 1) { selected ->
        val uri = selected.firstOrNull() ?: return@rememberQqMusicImagePicker
        scope.launch {
            saving = true
            uploadSelectedProfileBackground(context, uri, profile)?.let { profile = it }
            saving = false
        }
    }

    // === 保存函数 — 对标 EditPersonalInfoFragment.e() + MyDetailFragment.c() ===

    fun saveNickname(value: String) {
        val current = profile ?: return
        val next = current.copy(nickname = value.ifBlank { null })
        profile = next
        editor = null
        scope.launch {
            saving = true
            runCatching {
                withContext(Dispatchers.IO) { api.updateProfile(nickname = next.nickname) }
            }.onSuccess { response ->
                val server = parseProfile(response)
                val merged = (server ?: next).copy(
                    avatarUrl = next.avatarUrl,
                    backgroundUrl = next.backgroundUrl,
                )
                profile = merged
                store.getSession()?.first?.let { token -> store.saveUserSession(token, merged) }
                if (response.optBoolean("success")) {
                    AppNotice.success(context, "资料已更新")
                } else {
                    AppNotice.error(context, response.optString("error", "资料更新失败"))
                }
            }.onFailure { AppNotice.error(context, it.message ?: "资料更新失败") }
            saving = false
        }
    }

    fun saveBrief(value: String) {
        val current = profile ?: return
        val next = current.copy(bio = value.ifBlank { null })
        profile = next
        editor = null
        scope.launch {
            saving = true
            runCatching {
                withContext(Dispatchers.IO) { api.updateProfile(bio = next.bio) }
            }.onSuccess { response ->
                val server = parseProfile(response)
                val merged = (server ?: next).copy(
                    avatarUrl = next.avatarUrl,
                    backgroundUrl = next.backgroundUrl,
                )
                profile = merged
                store.getSession()?.first?.let { token -> store.saveUserSession(token, merged) }
                if (response.optBoolean("success")) {
                    AppNotice.success(context, "资料已更新")
                } else {
                    AppNotice.error(context, response.optString("error", "资料更新失败"))
                }
            }.onFailure { AppNotice.error(context, it.message ?: "资料更新失败") }
            saving = false
        }
    }

    // MyDetailFragment.c(String) → 性别保存
    fun saveGender(value: String) {
        showGenderPicker = false
        val current = profile ?: return
        val next = current.copy(gender = value)
        profile = next
        scope.launch {
            saving = true
            runCatching {
                withContext(Dispatchers.IO) { api.updateProfile(gender = value) }
            }.onSuccess { response ->
                val merged = (parseProfile(response) ?: next).copy(
                    avatarUrl = next.avatarUrl,
                    backgroundUrl = next.backgroundUrl,
                )
                profile = merged
                store.getSession()?.first?.let { token -> store.saveUserSession(token, merged) }
                AppNotice.success(context, "资料已更新")
            }.onFailure { AppNotice.error(context, it.message ?: "资料更新失败") }
            saving = false
        }
    }

    // RegionSelectFragment.a(City, String) → 地区保存
    fun saveRegion(region: String) {
        showRegionPicker = false
        val current = profile ?: return
        val next = current.copy(location = region)
        profile = next
        scope.launch {
            saving = true
            runCatching {
                withContext(Dispatchers.IO) { api.updateProfile(location = next.location) }
            }.onSuccess { response ->
                val merged = (parseProfile(response) ?: next).copy(
                    avatarUrl = next.avatarUrl,
                    backgroundUrl = next.backgroundUrl,
                )
                profile = merged
                store.getSession()?.first?.let { token -> store.saveUserSession(token, merged) }
                AppNotice.success(context, "资料已更新")
            }.onFailure { AppNotice.error(context, it.message ?: "资料更新失败") }
            saving = false
        }
    }

    // EditPersonalInfoFragment.b(String) → 生日保存
    fun saveBirthday(year: Int, month: Int, day: Int) {
        showBirthdayPicker = false
        val birthday = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        val current = profile ?: return
        val next = current.copy(birthday = birthday)
        profile = next
        scope.launch {
            saving = true
            runCatching {
                withContext(Dispatchers.IO) { api.updateProfile(birthday = next.birthday) }
            }.onSuccess { response ->
                val merged = (parseProfile(response) ?: next).copy(
                    avatarUrl = next.avatarUrl,
                    backgroundUrl = next.backgroundUrl,
                )
                profile = merged
                store.getSession()?.first?.let { token -> store.saveUserSession(token, merged) }
                AppNotice.success(context, "资料已更新")
            }.onFailure { AppNotice.error(context, it.message ?: "资料更新失败") }
            saving = false
        }
    }

    val user = profile
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑资料", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = navController::popBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (saving) {
                        ChatGptLoadingSpinner(
                            size = 22.dp,
                            loadingDescription = "正在保存资料",
                        )
                        Spacer(Modifier.width(16.dp))
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        if (loading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ChatGptLoadingSpinner(
                    color = MaterialTheme.colorScheme.onSurface,
                    loadingDescription = "正在加载资料",
                )
            }
        } else if (user == null) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("请先登录")
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = {
                        navController.navigate(com.mengzhen.app.ui.navigation.Screen.Login.route)
                    }) { Text("去登录") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                // MyDetailFragment.c() → 头部绑定
                item {
                    ProfileHero(
                        user = user,
                        onEditAvatar = avatarPicker,
                        onEditBackground = backgroundPicker,
                    )
                }
                // 完善资料引导 — 对标 ProfilePercentCouponInfo
                item {
                    ProfileCompletionGuide(user = user)
                }
                // 资料行 — 对标 MyDetailFragment onClick 顺序
                item {
                    // 昵称 — MyDetailFragment.i()
                    ProfileRow(
                        label = "昵称",
                        value = user.nickname,
                        percentGuide = "+30%",
                        onClick = { editor = EditorTarget.Nickname(user.nickname.orEmpty()) },
                    )
                    // 性别 — MyDetailFragment.y()
                    ProfileRow(
                        label = "性别",
                        value = genderText(user.gender),
                        percentGuide = "+20%",
                        onClick = { showGenderPicker = true },
                    )
                    // 生日 — MyDetailFragment.l()
                    ProfileRow(
                        label = "生日",
                        value = user.birthday,
                        percentGuide = "+10%",
                        onClick = { showBirthdayPicker = true },
                    )
                    // 地区 — MyDetailFragment.onClick → RegionSelectFragment
                    ProfileRow(
                        label = "地区",
                        value = formatLocation(user.location),
                        percentGuide = "+10%",
                        onClick = { showRegionPicker = true },
                    )
                }
                // 间距 — 对标 main_space_1
                item { Spacer(Modifier.height(24.dp)) }
                // 简介 — MyDetailFragment.k()
                item {
                    BriefRow(
                        value = user.bio,
                        onClick = { editor = EditorTarget.Brief(user.bio.orEmpty()) },
                    )
                }
                item { Spacer(Modifier.height(48.dp)) }
            }
        }
    }

    // === 编辑器 ===

    // EditPersonalInfoFragment type=1 → 昵称编辑
    editor?.let { target ->
        when (target) {
            is EditorTarget.Nickname -> NicknameEditorDialog(
                initialValue = target.value,
                onSave = { saveNickname(it) },
                onDismiss = { editor = null },
            )
            is EditorTarget.Brief -> BriefEditorDialog(
                initialValue = target.value,
                onSave = { saveBrief(it) },
                onDismiss = { editor = null },
            )
        }
    }

    // com.ximalaya.ting.android.main.dialog.c → 性别选择
    if (showGenderPicker) {
        GenderPickerDialog(
            currentGender = user?.gender,
            onConfirm = { saveGender(it) },
            onDismiss = { showGenderPicker = false },
        )
    }

    // EditPersonalInfoFragment type=2 → 生日 DatePicker
    if (showBirthdayPicker) {
        BirthdayPickerDialog(
            currentBirthday = user?.birthday,
            onConfirm = { year, month, day -> saveBirthday(year, month, day) },
            onDismiss = { showBirthdayPicker = false },
        )
    }

    // RegionSelectFragment → 地区选择
    if (showRegionPicker) {
        RegionPickerDialog(
            currentLocation = user?.location,
            onConfirm = { region -> saveRegion(region) },
            onDismiss = { showRegionPicker = false },
        )
    }
}

// === 编辑目标 ===

private sealed class EditorTarget {
    data class Nickname(val value: String) : EditorTarget()
    data class Brief(val value: String) : EditorTarget()
}

// === 头部 Hero — 对标 main_fra_my_detail_new.xml 头部布局 ===
// MyDetailFragment.f(): 背景图 ratio=375:200, 居中头像 88dp

@Composable
private fun ProfileHero(
    user: UserInfo,
    onEditAvatar: () -> Unit,
    onEditBackground: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(375f / 200f),
    ) {
        // main_iv_top_bg — 背景图
        if (user.backgroundUrl.isNullOrBlank()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFB7D8D0), Color(0xFFE9F2EF)),
                        ),
                    ),
            )
        } else {
            AsyncImage(
                model = user.backgroundUrl,
                contentDescription = "头像背景",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        // main_view_shadow — 渐变遮罩
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x66000000)))),
        )
        // main_iv_avatar — 居中头像 88dp，顶部偏上
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .clickable(onClick = onEditAvatar),
            contentAlignment = Alignment.Center,
        ) {
            if (!user.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = absoluteAvatarUrl(user.avatarUrl),
                    contentDescription = "头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            // main_iv_edit_avatar — 编辑图标，头像右下角
            Icon(
                Icons.Default.Edit,
                contentDescription = "编辑头像",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(20.dp),
            )
        }
        // main_tv_edit_bg — "编辑主页背景"，居中底部
        TextButton(
            onClick = onEditBackground,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
        ) {
            Text("编辑主页背景", color = Color.White, fontSize = 13.sp)
        }
    }
}

// === 完善资料引导 — 对标 ProfilePercentCouponInfo ===
// 本地计算完成度：头像15% + 昵称30% + 性别20% + 生日10% + 地区10% + 简介15% = 100%

@Composable
private fun ProfileCompletionGuide(user: UserInfo) {
    val percent = calculateProfilePercent(user)
    if (percent >= 100) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "完善个人资料",
                fontSize = 15.sp,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "已完善 $percent%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun calculateProfilePercent(user: UserInfo): Int {
    var percent = 0
    if (!user.avatarUrl.isNullOrBlank()) percent += 15
    if (!user.nickname.isNullOrBlank()) percent += 30
    if (!user.gender.isNullOrBlank() && user.gender != "secret") percent += 20
    if (!user.birthday.isNullOrBlank()) percent += 10
    if (!user.location.isNullOrBlank()) percent += 10
    if (!user.bio.isNullOrBlank()) percent += 15
    return percent
}

// === 资料行 — 对标 main_rl_modify_nickname / sex / birth_date / region ===

@Composable
private fun ProfileRow(
    label: String,
    value: String?,
    percentGuide: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        if (value.isNullOrBlank()) {
            // 未填写时显示完善度引导 — 对标 MyDetailFragment.a() 中 guide 可见逻辑
            Text(
                text = "完善度 $percentGuide",
                color = Color(0xFFFF6B35),
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 6.dp),
            )
        } else {
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
    // main_divide_1/2/3 — 分隔线
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )
}

// === 简介行 — 对标 main_rl_modify_brief ===

@Composable
private fun BriefRow(
    value: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("简介", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Text(
            value ?: "未填写",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(end = 6.dp),
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )
}

// === 昵称编辑器 — 对标 EditPersonalInfoFragment type=1 ===
// 中文占2字符，英文/数字占1字符，上限20
// 规则文案来自 configurecenter 默认值

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NicknameEditorDialog(
    initialValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }
    val charCount = remember(text) { countNicknameChars(text) }
    val maxCount = 20

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("编辑昵称") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "取消")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { onSave(text.trim()) },
                            enabled = charCount <= maxCount && charCount > 0 && text.trim() != initialValue.trim(),
                        ) { Text("保存") }
                    },
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { newText ->
                        if (countNicknameChars(newText) <= maxCount) {
                            text = newText
                        }
                    },
                    singleLine = true,
                    placeholder = { Text("请输入昵称") },
                    trailingIcon = {
                        if (text.isNotEmpty()) {
                            IconButton(onClick = { text = "" }) {
                                Icon(
                                    androidx.compose.material.icons.Icons.Default.Clear,
                                    contentDescription = "清除",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default,
                )
                Spacer(Modifier.height(8.dp))
                // 字数统计 — 对标 EditPersonalInfoFragment.a(int)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("$charCount")
                            if (charCount > maxCount) {
                                withStyle(SpanStyle(color = Color(0xFFCE2424))) {
                                    append("/$maxCount")
                                }
                            } else {
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                    append("/$maxCount")
                                }
                            }
                        },
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(16.dp))
                // 规则文案 — 来自 configurecenter "nickname_modify_new" 默认值
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append("剩余修改次数：不限\n")
                        }
                        withStyle(SpanStyle(color = Color(0xFFFF4444))) {
                            append(
                                "1.昵称修改次数：非认证用户，每自然月可修改1次；认证用户，每自然年可修改4次；\n" +
                                    "2.每天最多修改3次；\n" +
                                    "3. 仅支持数字/字母/汉字/下划线；不建议使用生僻字；\n" +
                                    "4. 昵称限20字符，中文占2字符，英文/数字占1字符；\n" +
                                    "5. 使用健康/财经/司法/教育类昵称，请先完成相关资质认证；\n" +
                                    "6. 禁止使用色情/违法/低俗昵称；",
                            )
                        }
                    },
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

// EditPersonalInfoFragment.a(CharSequence) — 中文字符=2，其他=1
private fun countNicknameChars(text: String): Int {
    var count = 0
    for (c in text) {
        count += if (Character.UnicodeScript.of(c.code) == Character.UnicodeScript.HAN) 2 else 1
    }
    return count
}

// === 简介编辑器 — 对标 EditPersonalInfoFragment type=3 ===
// 上限300字，显示"还能输入X字"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BriefEditorDialog(
    initialValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }
    val maxCount = 300
    val remaining = maxCount - text.length

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("编辑简介") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "取消")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { onSave(text.trim()) },
                            enabled = text.isNotEmpty() && text.trim() != initialValue.trim(),
                        ) { Text("保存") }
                    },
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { newText ->
                        if (newText.length <= maxCount) {
                            text = newText
                        }
                    },
                    singleLine = false,
                    minLines = 5,
                    maxLines = 10,
                    placeholder = { Text("请输入个人简介") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default,
                )
                Spacer(Modifier.height(8.dp))
                // EditPersonalInfoFragment.13 → "还能输入X字" / "无法输入更多"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (text.isEmpty()) {
                        Text("", fontSize = 12.sp)
                    } else if (remaining <= 0) {
                        Text("无法输入更多", fontSize = 12.sp, color = Color(0xFFCE2424))
                    } else {
                        Text(
                            "还能输入 $remaining 字",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "据说，写一段有趣的简介，被关注的概率会翻倍哦～",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// === 生日 DatePicker — 对标 EditPersonalInfoFragment.c() ===
// minDate=1900-01-01, maxDate=今天, title="填写生日信息，当天会有神秘惊喜噢~"

@Composable
private fun BirthdayPickerDialog(
    currentBirthday: String?,
    onConfirm: (year: Int, month: Int, day: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val calendar = remember {
        if (currentBirthday != null && currentBirthday.contains("-")) {
            val parts = currentBirthday.split("-")
            val cal = Calendar.getInstance()
            cal.set(
                parts.getOrNull(0)?.toIntOrNull() ?: 2000,
                (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1,
                parts.getOrNull(2)?.toIntOrNull() ?: 1,
            )
            cal
        } else {
            Calendar.getInstance()
        }
    }

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val minCal = Calendar.getInstance().apply { set(1900, 0, 1) }
    val maxCal = Calendar.getInstance()

    DatePickerDialog(
        LocalContext.current,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            onConfirm(selectedYear, selectedMonth + 1, selectedDay)
        },
        year,
        month,
        day,
    ).apply {
        datePicker.minDate = minCal.timeInMillis
        datePicker.maxDate = maxCal.timeInMillis
        setOnCancelListener { onDismiss() }
        setTitle("填写生日信息，当天会有神秘惊喜噢~")
    }.show()
}

// === 性别选择 — 对标 com.ximalaya.ting.android.main.dialog.c ===
// 选项：男(male) / 女(female) / 不展示(secret)

@Composable
private fun GenderPickerDialog(
    currentGender: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("性别") },
        text = {
            Column {
                listOf("male" to "男", "female" to "女", "secret" to "不展示").forEach { (value, label) ->
                    TextButton(
                        onClick = { onConfirm(value) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            label,
                            color = if (currentGender == value) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}

// === 地区选择 — 对标 RegionSelectFragment ===
// 省市两级选择，数据来自 province_cities.json（本地等价）

private data class Province(val name: String, val cities: List<String>)

private val chinaProvinces = listOf(
    Province("北京", listOf("东城区", "西城区", "朝阳区", "海淀区", "丰台区", "石景山区", "通州区", "昌平区", "大兴区", "顺义区")),
    Province("上海", listOf("黄浦区", "徐汇区", "长宁区", "静安区", "普陀区", "虹口区", "杨浦区", "浦东新区", "闵行区", "宝山区")),
    Province("广东", listOf("广州", "深圳", "珠海", "佛山", "东莞", "中山", "惠州", "汕头", "江门", "湛江")),
    Province("浙江", listOf("杭州", "宁波", "温州", "绍兴", "嘉兴", "金华", "台州", "湖州", "丽水", "衢州")),
    Province("江苏", listOf("南京", "苏州", "无锡", "常州", "南通", "徐州", "扬州", "泰州", "镇江", "盐城")),
    Province("四川", listOf("成都", "绵阳", "德阳", "南充", "宜宾", "泸州", "乐山", "自贡", "内江", "达州")),
    Province("湖北", listOf("武汉", "宜昌", "襄阳", "荆州", "十堰", "黄石", "荆门", "孝感", "黄冈", "咸宁")),
    Province("湖南", listOf("长沙", "株洲", "湘潭", "衡阳", "岳阳", "常德", "郴州", "益阳", "永州", "怀化")),
    Province("山东", listOf("济南", "青岛", "烟台", "潍坊", "淄博", "威海", "日照", "临沂", "济宁", "泰安")),
    Province("河南", listOf("郑州", "洛阳", "开封", "新乡", "南阳", "安阳", "焦作", "许昌", "商丘", "信阳")),
    Province("河北", listOf("石家庄", "唐山", "保定", "邯郸", "廊坊", "秦皇岛", "张家口", "承德", "沧州", "邢台")),
    Province("福建", listOf("福州", "厦门", "泉州", "漳州", "莆田", "龙岩", "宁德", "三明", "南平")),
    Province("安徽", listOf("合肥", "芜湖", "蚌埠", "淮南", "马鞍山", "淮北", "铜陵", "安庆", "黄山", "滁州")),
    Province("江西", listOf("南昌", "九江", "上饶", "抚州", "宜春", "吉安", "赣州", "景德镇", "萍乡", "新余")),
    Province("辽宁", listOf("沈阳", "大连", "鞍山", "抚顺", "本溪", "丹东", "锦州", "营口", "盘锦", "朝阳")),
    Province("吉林", listOf("长春", "吉林", "四平", "辽源", "通化", "白山", "松原", "白城")),
    Province("黑龙江", listOf("哈尔滨", "齐齐哈尔", "牡丹江", "佳木斯", "大庆", "鸡西", "双鸭山", "伊春", "七台河", "鹤岗")),
    Province("陕西", listOf("西安", "宝鸡", "咸阳", "渭南", "延安", "汉中", "榆林", "安康", "商洛", "铜川")),
    Province("山西", listOf("太原", "大同", "阳泉", "长治", "晋城", "朔州", "晋中", "运城", "忻州", "临汾")),
    Province("云南", listOf("昆明", "曲靖", "玉溪", "保山", "昭通", "丽江", "普洱", "临沧", "大理", "楚雄")),
    Province("贵州", listOf("贵阳", "遵义", "六盘水", "安顺", "毕节", "铜仁", "黔东南", "黔南", "黔西南")),
    Province("广西", listOf("南宁", "柳州", "桂林", "梧州", "北海", "防城港", "钦州", "贵港", "玉林", "百色")),
    Province("海南", listOf("海口", "三亚", "儋州", "三沙", "五指山", "琼海", "文昌", "万宁", "东方")),
    Province("甘肃", listOf("兰州", "天水", "白银", "庆阳", "平凉", "酒泉", "张掖", "武威", "定西", "陇南")),
    Province("青海", listOf("西宁", "海东", "海北", "黄南", "海南州", "果洛", "玉树", "海西")),
    Province("宁夏", listOf("银川", "石嘴山", "吴忠", "固原", "中卫")),
    Province("新疆", listOf("乌鲁木齐", "克拉玛依", "吐鲁番", "哈密", "昌吉", "博尔塔拉", "巴音郭楞", "阿克苏", "喀什", "伊犁")),
    Province("内蒙古", listOf("呼和浩特", "包头", "乌海", "赤峰", "通辽", "鄂尔多斯", "呼伦贝尔", "巴彦淖尔", "乌兰察布")),
    Province("西藏", listOf("拉萨", "日喀则", "昌都", "林芝", "山南", "那曲", "阿里")),
    Province("天津", listOf("和平区", "河东区", "河西区", "南开区", "河北区", "红桥区", "东丽区", "西青区", "津南区", "北辰区")),
    Province("重庆", listOf("渝中区", "江北区", "南岸区", "九龙坡区", "沙坪坝区", "大渡口区", "渝北区", "巴南区", "北碚区", "万州区")),
    Province("香港", listOf("香港岛", "九龙", "新界")),
    Province("澳门", listOf("澳门半岛", "氹仔", "路环")),
    Province("台湾", listOf("台北", "新北", "桃园", "台中", "台南", "高雄", "基隆", "新竹", "嘉义")),
    Province("海外", listOf("美国", "日本", "韩国", "新加坡", "马来西亚", "澳大利亚", "加拿大", "英国", "法国", "德国", "其他")),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionPickerDialog(
    currentLocation: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedProvince by remember { mutableStateOf<Province?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (selectedProvince == null) "选择省份" else "选择城市") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (selectedProvince != null) {
                                selectedProvince = null
                            } else {
                                onDismiss()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) { padding ->
            if (selectedProvince == null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    items(chinaProvinces) { province ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProvince = province }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(province.name, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.weight(1f))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    items(selectedProvince!!.cities) { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val region = "${selectedProvince!!.name} $city"
                                    onConfirm(region)
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(city, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.weight(1f))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// === 工具函数 ===

// MyDetailFragment.a(MyDetailInfo) → 性别显示
// gender: 1/male → 男, 2/female → 女, secret/其他 → 不展示
private fun genderText(value: String?): String = when (value?.lowercase()) {
    "male", "男", "1" -> "男"
    "female", "女", "2" -> "女"
    "secret" -> "不展示"
    else -> "未填写"
}

// MyDetailFragment.a(MyDetailInfo) → 地区显示
// 格式兼容：Web 端 "地球/中国/省/市/区" → 取最后两段；Android 端 "省 市" → 原样
private fun formatLocation(location: String?): String {
    if (location.isNullOrBlank()) return "未填写"
    if (location.contains('/')) {
        val parts = location.split('/').filter { it.isNotBlank() }
        return when (parts.size) {
            in 0..1 -> location
            2 -> parts.joinToString(" ")
            else -> parts.takeLast(2).joinToString(" ")
        }
    }
    return location
}
