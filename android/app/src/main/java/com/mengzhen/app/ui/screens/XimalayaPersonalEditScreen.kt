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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
 * 喜马拉雅 MyDetailFragment 完整闭包迁移：保留原页的头像、背景和资料行顺序，
 * 字段修改沿用原版的逐项保存语义。包含昵称编辑（带字数统计）、性别选择、
 * 生日 DatePicker、地区选择（省市联动）、个性签名/个人简介编辑（带字数统计与示例）。
 *
 * 对标源码：
 * - MyDetailFragment.java — 主资料编辑页
 * - EditPersonalInfoFragment.java — 昵称/生日/简介子页
 * - RegionSelectFragment.java — 地区选择子页
 * - main_fra_my_detail_new.xml — 主页布局
 * - main_fra_personal_edit.xml — 编辑子页布局
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
    var editor by remember { mutableStateOf<ProfileEditor?>(null) }
    var showGenderPicker by remember { mutableStateOf(false) }
    var showRegionPicker by remember { mutableStateOf(false) }
    var showBirthdayPicker by remember { mutableStateOf(false) }

    // 初始加载远端资料
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
        }.onFailure {
            // 加载失败也允许使用本地缓存的 sessionUser 继续编辑
        }
        loading = false
    }

    // 背景图选择器
    val backgroundPicker = rememberQqMusicImagePicker(maxSelection = 1) { selected ->
        val uri = selected.firstOrNull() ?: return@rememberQqMusicImagePicker
        scope.launch {
            saving = true
            uploadSelectedProfileBackground(context, uri, profile)?.let { profile = it }
            saving = false
        }
    }

    // 头像选择器
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

    fun saveField(field: ProfileEditor, value: String) {
        val current = profile ?: return
        val next = when (field) {
            ProfileEditor.USERNAME -> current.copy(username = value.ifBlank { current.username })
            ProfileEditor.NICKNAME -> current.copy(nickname = value.ifBlank { null })
            ProfileEditor.BIRTHDAY -> current.copy(birthday = value.ifBlank { null })
            ProfileEditor.LOCATION -> current.copy(location = value.ifBlank { null })
            ProfileEditor.SIGNATURE -> current.copy(signature = value.ifBlank { null })
            ProfileEditor.BIO -> current.copy(bio = value.ifBlank { null })
        }
        profile = next
        editor = null
        scope.launch {
            saving = true
            runCatching {
                withContext(Dispatchers.IO) {
                    when (field) {
                        ProfileEditor.USERNAME -> api.updateProfile(username = next.username)
                        ProfileEditor.NICKNAME -> api.updateProfile(nickname = next.nickname)
                        ProfileEditor.BIRTHDAY -> api.updateProfile(birthday = next.birthday)
                        ProfileEditor.LOCATION -> api.updateProfile(location = next.location)
                        ProfileEditor.SIGNATURE -> api.updateProfile(signature = next.signature)
                        ProfileEditor.BIO -> api.updateProfile(bio = next.bio)
                    }
                }
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

    fun saveBirthday(year: Int, month: Int, day: Int) {
        showBirthdayPicker = false
        val birthday = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        saveField(ProfileEditor.BIRTHDAY, birthday)
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
                    } else {
                        TextButton(onClick = navController::popBackStack) { Text("完成") }
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
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // 头部：背景图 + 头像 + 昵称
                item {
                    ProfileHero(
                        user = user,
                        onEditAvatar = avatarPicker,
                        onEditBackground = backgroundPicker,
                    )
                }
                // 资料行 — 顺序对标 main_fra_my_detail_new.xml
                item {
                    Spacer(Modifier.height(12.dp))
                    ProfileRow(
                        label = "用户名",
                        value = user.username,
                        onClick = { editor = ProfileEditor.USERNAME },
                    )
                    ProfileRow(
                        label = "昵称",
                        value = user.nickname ?: user.username,
                        onClick = { editor = ProfileEditor.NICKNAME },
                    )
                    ProfileRow(
                        label = "性别",
                        value = genderText(user.gender),
                        onClick = { showGenderPicker = true },
                    )
                    ProfileRow(
                        label = "生日",
                        value = user.birthday ?: "未填写",
                        onClick = { showBirthdayPicker = true },
                    )
                    ProfileRow(
                        label = "地区",
                        value = formatLocation(user.location),
                        onClick = { showRegionPicker = true },
                    )
                    ProfileRow(
                        label = "个性签名",
                        value = user.signature ?: "未填写",
                        onClick = { editor = ProfileEditor.SIGNATURE },
                    )
                    ProfileRow(
                        label = "个人简介",
                        value = user.bio ?: "未填写",
                        onClick = { editor = ProfileEditor.BIO },
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    // 昵称/签名/简介编辑器 — 对标 EditPersonalInfoFragment
    editor?.let { field ->
        NicknameOrBriefEditor(
            field = field,
            initialValue = field.value(user),
            onSave = { value -> saveField(field, value) },
            onDismiss = { editor = null },
        )
    }

    // 性别选择 — 对标喜马拉雅性别 Dialog (com.ximalaya.ting.android.main.dialog.c)
    if (showGenderPicker) {
        AlertDialog(
            onDismissRequest = { showGenderPicker = false },
            title = { Text("性别") },
            text = {
                Column {
                    listOf("male" to "男", "female" to "女", "secret" to "保密").forEach { (value, label) ->
                        TextButton(
                            onClick = { saveGender(value) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(label) }
                    }
                }
            },
            confirmButton = {},
        )
    }

    // 生日选择 — 对标 EditPersonalInfoFragment 的 DatePickerDialog
    if (showBirthdayPicker) {
        BirthdayPickerDialog(
            currentBirthday = user?.birthday,
            onConfirm = { year, month, day -> saveBirthday(year, month, day) },
            onDismiss = { showBirthdayPicker = false },
        )
    }

    // 地区选择 — 对标 RegionSelectFragment
    if (showRegionPicker) {
        RegionPickerDialog(
            currentLocation = user?.location,
            onConfirm = { region -> saveRegion(region) },
            onDismiss = { showRegionPicker = false },
        )
    }

}

// === 编辑器子页 ===

private enum class ProfileEditor(
    val title: String,
    val maxLength: Int,
    val isMultiLine: Boolean,
    val value: (UserInfo?) -> String,
) {
    USERNAME("用户名", 30, false, { it?.username.orEmpty() }),
    NICKNAME("昵称", 50, false, { it?.nickname ?: it?.username.orEmpty() }),
    SIGNATURE("个性签名", 200, false, { it?.signature.orEmpty() }),
    BIO("个人简介", 500, true, { it?.bio.orEmpty() }),
    BIRTHDAY("生日", 0, false, { it?.birthday.orEmpty() }),
    LOCATION("地区", 0, false, { it?.location.orEmpty() }),
}

/**
 * 用户名/昵称/签名/简介编辑器 — 对标 EditPersonalInfoFragment
 * 使用标准字符长度计数，与 Web API 验证规则对齐。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NicknameOrBriefEditor(
    field: ProfileEditor,
    initialValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }
    val charCount = text.length
    val maxCount = field.maxLength

    // 使用全屏 Dialog 对标 EditPersonalInfoFragment 的 startFragment 全页编辑
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(field.title) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "取消")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { onSave(text.trim()) },
                            enabled = charCount <= maxCount && text.isNotBlank(),
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
                    singleLine = !field.isMultiLine,
                    minLines = if (field.isMultiLine) 5 else 1,
                    maxLines = if (field.isMultiLine) 10 else 1,
                    placeholder = {
                        Text(
                            when (field) {
                                ProfileEditor.USERNAME -> "请输入用户名"
                                ProfileEditor.NICKNAME -> "请输入昵称"
                                ProfileEditor.SIGNATURE -> "请输入个性签名"
                                ProfileEditor.BIO -> "请输入个人简介"
                                else -> ""
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "$charCount/$maxCount",
                        fontSize = 12.sp,
                        color = if (charCount > maxCount) {
                            Color(0xFFCE2424)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                // 简介编辑时显示示例文案
                if (field == ProfileEditor.BIO) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "据说，写一段有趣的简介，被关注的概率会翻倍哦～",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "示例",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // 用户名编辑时显示规则提示
                if (field == ProfileEditor.USERNAME) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFFFF4444))) {
                                append(
                                    "1. 仅支持数字/字母/汉字/下划线\n" +
                                        "2. 用户名限 $maxCount 个字符\n" +
                                        "3. 修改用户名后需重新登录其他设备",
                                )
                            }
                        },
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                }
                // 昵称编辑时显示规则提示
                if (field == ProfileEditor.NICKNAME) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("剩余修改次数：不限\n")
                            withStyle(SpanStyle(color = Color(0xFFFF4444))) {
                                append(
                                    "1. 仅支持数字/字母/汉字/下划线\n" +
                                        "2. 昵称限 $maxCount 个字符\n" +
                                        "3. 禁止使用色情/违法/低俗昵称",
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
}

// === 生日 DatePicker — 对标 EditPersonalInfoFragment.c() ===

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

// === 地区选择 — 对标 RegionSelectFragment ===

private data class Province(val name: String, val cities: List<String>)

// 省市数据 — 对标喜马拉雅 RegionSelectFragment 使用的 Provinces 模型
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
                // 省份列表
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
                // 城市列表
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

// === 头部 Hero 区域 — 对标 main_fra_my_detail_new.xml 的头部布局 ===

@Composable
private fun ProfileHero(
    user: UserInfo,
    onEditAvatar: () -> Unit,
    onEditBackground: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp),
    ) {
        // 背景图
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
                contentDescription = "个人背景",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        // 渐变遮罩
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA000000)))),
        )
        // 头像 + 昵称
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
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
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑头像",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(16.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.padding(bottom = 4.dp)) {
                    Text(
                        user.nickname ?: user.username,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text("编辑个人资料", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onEditAvatar) {
                Text("更换头像", color = Color.White, fontSize = 12.sp)
            }
        }
        // 编辑背景按钮
        TextButton(
            onClick = onEditBackground,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 12.dp),
        ) { Text("编辑背景", color = Color.White) }
    }
}

@Composable
private fun ProfileRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Text(
            value,
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
}

// === 工具函数 ===

private fun genderText(value: String?): String = when (value?.lowercase()) {
    "male", "男", "1" -> "男"
    "female", "女", "2" -> "女"
    else -> "保密"
}

/**
 * 格式化地区显示 — 兼容 Web 端 5 级格式（地球/中国/省/市/区）和 Android 端 2 级格式（省 市）。
 * Web 端格式按 '/' 分割后取最后 2 段（省+市/区），Android 端格式原样显示。
 */
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
