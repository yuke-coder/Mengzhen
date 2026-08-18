package com.mengzhen.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.UserInfo
import com.mengzhen.app.data.model.parseProfile
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.components.ChatGptLoadingSpinner
import com.mengzhen.app.ui.fragments.EditPersonalInfoFragment
import com.mengzhen.app.ui.fragments.GenderSelectDialog
import com.mengzhen.app.ui.fragments.RegionSelectFragment
import com.mengzhen.app.ui.components.main.absoluteAvatarUrl
import com.mengzhen.app.ui.components.rememberQqMusicImagePicker
import com.mengzhen.app.ui.feedback.AppNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // EditPersonalInfoFragment type=1 → 昵称编辑（原版 Fragment 直接迁移）
    // 通过 FragmentManager 弹出 EditPersonalInfoFragment（DialogFragment 全屏），
    // 替换原 Compose NicknameEditorDialog 实现。
    // Compose 的 LocalContext 可能是 ContextWrapper 链（如 ContextThemeWrapper），
    // 需沿 baseContext 上溯定位真正的 FragmentActivity，否则 FragmentManager 为空。
    val hostActivity = remember(context) { context.resolveFragmentActivity() }
    val fragmentManager = remember(context) {
        hostActivity?.supportFragmentManager
    }
    LaunchedEffect(fragmentManager) {
        val fm = fragmentManager ?: return@LaunchedEffect
        val owner = hostActivity ?: return@LaunchedEffect
        fm.setFragmentResultListener(
            EditPersonalInfoFragment.RESULT_KEY,
            owner,
        ) { _, bundle ->
            if (bundle.containsKey(EditPersonalInfoFragment.RESULT_BIRTHDAY_YEAR)) {
                // type=2 生日：DatePicker 选择后回调保存
                val y = bundle.getInt(EditPersonalInfoFragment.RESULT_BIRTHDAY_YEAR)
                val m = bundle.getInt(EditPersonalInfoFragment.RESULT_BIRTHDAY_MONTH) + 1
                val d = bundle.getInt(EditPersonalInfoFragment.RESULT_BIRTHDAY_DAY)
                if (y > 0 && d > 0) saveBirthday(y, m, d)
                showBirthdayPicker = false
            } else if (bundle.containsKey(EditPersonalInfoFragment.RESULT_BRIEF)) {
                // type=3 简介：保存后回调
                saveBrief(bundle.getString(EditPersonalInfoFragment.RESULT_BRIEF).orEmpty())
                editor = null
            } else {
                val nickname = bundle.getString(EditPersonalInfoFragment.RESULT_NICKNAME)
                if (!nickname.isNullOrEmpty()) saveNickname(nickname)
                editor = null
            }
        }
    }
    // RegionSelectFragment → 地区编辑结果
    LaunchedEffect(fragmentManager) {
        val fm = fragmentManager ?: return@LaunchedEffect
        val owner = hostActivity ?: return@LaunchedEffect
        fm.setFragmentResultListener(
            RegionSelectFragment.RESULT_KEY,
            owner,
        ) { _, bundle ->
            val region = bundle.getString(RegionSelectFragment.RESULT_REGION)
            if (!region.isNullOrBlank()) saveRegion(region)
            showRegionPicker = false
        }
    }
    LaunchedEffect(editor) {
        val target = editor ?: return@LaunchedEffect
        val fm = fragmentManager ?: return@LaunchedEffect
        when (target) {
            is EditorTarget.Nickname -> {
                // 仅在尚未弹出时启动，避免重入
                if (fm.findFragmentByTag(NICKNAME_FRAGMENT_TAG) == null) {
                    EditPersonalInfoFragment.newInstance(target.value)
                        .show(fm, NICKNAME_FRAGMENT_TAG)
                }
            }
            is EditorTarget.Brief -> {
                if (fm.findFragmentByTag(BRIEF_FRAGMENT_TAG) == null) {
                    EditPersonalInfoFragment.newBriefInstance(target.value)
                        .show(fm, BRIEF_FRAGMENT_TAG)
                }
            }
        }
        // 立即清空，保证再次点击（即使同名）也能重新触发 LaunchedEffect
        editor = null
    }

    // com.ximalaya.ting.android.main.dialog.c → 性别选择（原版 MenuDialog 直接迁移）
    // 通过 hostActivity 弹出 GenderSelectDialog，替换原 Compose GenderPickerDialog。
    if (showGenderPicker) {
        val activity = hostActivity
        LaunchedEffect(showGenderPicker, activity) {
            // 立即重置标志，避免 Compose 重组时重入
            showGenderPicker = false
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                return@LaunchedEffect
            }
            GenderSelectDialog.show(
                activity = activity,
                currentGender = user?.gender,
                onSelected = { gender, _ -> saveGender(gender) },
                onDismiss = {},
            )
        }
    }

    // EditPersonalInfoFragment type=2 → 生日编辑（原版 DatePickerDialog + 星座，直接迁移）
    // 通过 FragmentManager 弹出 EditPersonalInfoFragment，替换原 Compose BirthdayPickerDialog。
    if (showBirthdayPicker) {
        val activity = hostActivity
        LaunchedEffect(showBirthdayPicker, activity) {
            // 立即重置标志，避免 Compose 重组时重入
            showBirthdayPicker = false
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                return@LaunchedEffect
            }
            val fm = activity.supportFragmentManager
            if (fm.findFragmentByTag(BIRTHDAY_FRAGMENT_TAG) != null) {
                return@LaunchedEffect
            }
            val (y, m, d) = parseBirthday(user?.birthday)
            EditPersonalInfoFragment.newInstance(y, m - 1, d, false)
                .show(fm, BIRTHDAY_FRAGMENT_TAG)
        }
    }

    // RegionSelectFragment → 地区编辑（原版 RegionSelectFragment 直接迁移）
    // 通过 FragmentManager 弹出 RegionSelectFragment，替换原 Compose RegionPickerDialog。
    if (showRegionPicker) {
        val activity = hostActivity
        LaunchedEffect(showRegionPicker, activity) {
            // 立即重置标志，避免 Compose 重组时重入
            showRegionPicker = false
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                return@LaunchedEffect
            }
            val fm = activity.supportFragmentManager
            if (fm.findFragmentByTag(REGION_FRAGMENT_TAG) != null) {
                return@LaunchedEffect
            }
            RegionSelectFragment.newInstance().show(fm, REGION_FRAGMENT_TAG)
        }
    }
}

// === 编辑目标 ===

private sealed class EditorTarget {
    data class Nickname(val value: String) : EditorTarget()
    data class Brief(val value: String) : EditorTarget()
}

/** 沿 ContextWrapper 链上溯，定位真正的 FragmentActivity（Compose LocalContext 常为包装 Context）。 */
private tailrec fun Context.resolveFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is android.content.ContextWrapper -> baseContext.resolveFragmentActivity()
    else -> null
}

private const val NICKNAME_FRAGMENT_TAG = "ximalaya_edit_personal_info_fragment"
private const val BIRTHDAY_FRAGMENT_TAG = "ximalaya_edit_birthday_fragment"
private const val REGION_FRAGMENT_TAG = "ximalaya_edit_region_fragment"
private const val BRIEF_FRAGMENT_TAG = "ximalaya_edit_brief_fragment"

/** 解析生日串 "YYYY-M-D" → (year, month1, day)；非法返回 (0,0,0)（Fragment 会自动弹 DatePicker）。 */
private fun parseBirthday(birthday: String?): Triple<Int, Int, Int> {
    if (!birthday.isNullOrBlank() && birthday.contains("-")) {
        val parts = birthday.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull()
        val m = parts.getOrNull(1)?.toIntOrNull()
        val d = parts.getOrNull(2)?.toIntOrNull()
        if (y != null && m != null && d != null && y > 0 && m in 1..12 && d > 0) {
            return Triple(y, m, d)
        }
    }
    return Triple(0, 0, 0)
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

// === 昵称编辑器 — 已迁移至 com.mengzhen.app.ui.fragments.EditPersonalInfoFragment ===
// 详见 main_fra_personal_edit.xml + EditPersonalInfoFragment.kt（原版 9.5.4.7 直接迁移）。
// Compose NicknameEditorDialog 与 countNicknameChars 已删除，逻辑全部回归原版 Fragment。

// === 简介编辑器 — 已迁移至 com.mengzhen.app.ui.fragments.EditPersonalInfoFragment (type=3) ===
// 原版 initUi i==3 分支直接迁移：main_change_brief 输入框（maxLength 300）+
// "还能输入X字/无法输入更多"计数（AnonymousClass13）+ main_tv_rule SpanUtils 链路。
// 详见 main_fra_personal_edit.xml + EditPersonalInfoFragment.kt。
// Compose BriefEditorDialog 已删除，逻辑全部回归原版 Fragment。

// === 生日编辑 — 已迁移至 com.mengzhen.app.ui.fragments.EditPersonalInfoFragment (type=2) ===
// 原版 EditPersonalInfoFragment.c() DatePickerDialog + com.ximalaya...util.ui.a 星座算法直接迁移。
// 详见 main_fra_personal_edit.xml + main_v_switch_info.xml + ConstellationUtils.kt。
// Compose BirthdayPickerDialog 已删除，逻辑全部回归原版 Fragment。

// === 性别选择 — 对标 com.ximalaya.ting.android.main.dialog.c ===
// 选项：男(male) / 女(female)；"不展示性别"作为 Dialog 内 CheckBox 开关
// 实现：com.mengzhen.app.ui.fragments.GenderSelectDialog（原版 MenuDialog 子类直接迁移）

// === 地区编辑 — 已迁移至 com.mengzhen.app.ui.fragments.RegionSelectFragment ===
// 原版 RegionSelectFragment 直接迁移：host_fra_list_2 + main_item_city + assets/province_cities.json。
// Compose RegionPickerDialog 与硬编码 chinaProvinces 已删除，逻辑全部回归原版 Fragment。

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
