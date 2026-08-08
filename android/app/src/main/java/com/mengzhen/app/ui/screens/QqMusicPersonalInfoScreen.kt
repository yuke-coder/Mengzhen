package com.mengzhen.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import coil3.request.target
import com.mengzhen.app.R
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.UserInfo
import com.mengzhen.app.data.model.parseProfile
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.feedback.AppNotice
import com.mengzhen.app.ui.navigation.Screen
import com.tencent.qqmusic.activity.ImageCropActivity
import com.tencent.qqmusic.activity.UserNicknameModifyActivity
import com.tencent.qqmusic.common.pick.pickview.builder.b
import com.tencent.qqmusic.common.pick.pickview.listener.g
import com.tencent.qqmusic.fragment.profile.UserGender
import com.tencent.qqmusic.ui.UserGenderSheet
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QqMusicPersonalInfoScreen(navController: NavController) {
    val context = LocalContext.current
    val store = remember(context) { TaskStore.get(context) }
    val api = remember(context) { ApiClient.get(context) }
    val scope = rememberCoroutineScope()
    val sessionProfile by store.sessionUser.collectAsState()
    var profile by remember { mutableStateOf(sessionProfile) }
    var avatarVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(sessionProfile) {
        profile = sessionProfile
    }

    val fieldEditor = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val field = data.getStringExtra(UserNicknameModifyActivity.KEY_FIELD).orEmpty()
        val value = data.getStringExtra(UserNicknameModifyActivity.KEY_VALUE).orEmpty()
        profile = store.getSession()?.second ?: profile?.withField(field, value)
        AppNotice.success(context, context.getString(R.string.qq_profile_updated))
    }

    fun edit(field: String, value: String, title: String, hint: String, max: Int, multiline: Boolean = false) {
        fieldEditor.launch(
            Intent(context, UserNicknameModifyActivity::class.java)
                .putExtra(UserNicknameModifyActivity.KEY_FIELD, field)
                .putExtra(UserNicknameModifyActivity.KEY_VALUE, value)
                .putExtra(UserNicknameModifyActivity.KEY_TITLE, title)
                .putExtra(UserNicknameModifyActivity.KEY_HINT, hint)
                .putExtra(UserNicknameModifyActivity.KEY_MAX_LENGTH, max)
                .putExtra(UserNicknameModifyActivity.KEY_MULTILINE, multiline),
        )
    }

    fun save(update: () -> org.json.JSONObject, fallback: (UserInfo) -> UserInfo) {
        val current = profile ?: return
        scope.launch(Dispatchers.IO) {
            val result = runCatching(update)
            withContext(Dispatchers.Main) {
                result.onSuccess { response ->
                    if (response.optBoolean("success", false)) {
                        profile = parseProfile(response) ?: fallback(current)
                        profile?.let { store.saveUserSession("cookie_session", it) }
                        AppNotice.success(context, context.getString(R.string.qq_profile_updated))
                    } else {
                        AppNotice.error(
                            context,
                            response.optString("error", context.getString(R.string.qq_profile_save_failed)),
                        )
                    }
                }.onFailure {
                    AppNotice.error(context, context.getString(R.string.qq_profile_save_failed))
                }
            }
        }
    }

    val avatarCrop = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK) {
            val value = data?.getStringExtra(ImageCropActivity.KEY_PATH)?.ifBlank { null }
            profile = profile?.copy(avatarUrl = value)
            profile?.let { store.saveUserSession("cookie_session", it) }
            avatarVersion++
            AppNotice.success(context, context.getString(R.string.qq_profile_avatar_updated))
        } else {
            data?.getStringExtra(ImageCropActivity.KEY_ERROR_MSG)
                ?.takeIf(String::isNotBlank)
                ?.let { AppNotice.error(context, it) }
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val source = runCatching {
                uriToAvatarFile(
                    context,
                    uri,
                    context.contentResolver.getType(uri) ?: "image/jpeg",
                )
            }
            withContext(Dispatchers.Main) {
                source.onSuccess {
                    avatarCrop.launch(
                        Intent(context, ImageCropActivity::class.java)
                            .putExtra(ImageCropActivity.KEY_PATH, it.absolutePath),
                    )
                }.onFailure {
                    AppNotice.error(context, context.getString(R.string.qq_profile_save_failed))
                }
            }
        }
    }

    if (profile == null) {
        LaunchedEffect(Unit) {
            navController.navigate(Screen.Login.route) {
                launchSingleTop = true
            }
        }
        Box(Modifier.fillMaxSize())
        return
    }

    LaunchedEffect(Unit) {
        val response = withContext(Dispatchers.IO) { runCatching { api.getProfile() }.getOrNull() }
        if (response?.optBoolean("sessionExpired", false) == true) {
            store.clearSession()
            navController.navigate(Screen.Login.route) {
                launchSingleTop = true
            }
            return@LaunchedEffect
        }
        parseProfile(response ?: return@LaunchedEffect)?.let {
            profile = it
            store.saveUserSession("cookie_session", it)
        }
    }

    AndroidView(
        factory = { viewContext ->
            QqProfileBinding.inflate(
                context = viewContext,
                onAvatar = { avatarPicker.launch("image/*") },
                onNickname = { value ->
                    edit(
                        UserNicknameModifyActivity.FIELD_NICKNAME,
                        value,
                        viewContext.getString(R.string.qq_profile_set_nickname),
                        viewContext.getString(R.string.bum),
                        15,
                    )
                },
                onAccount = { value ->
                    edit(
                        UserNicknameModifyActivity.FIELD_USERNAME,
                        value,
                        viewContext.getString(R.string.qq_profile_account),
                        viewContext.getString(R.string.qq_profile_input_account),
                        30,
                    )
                },
                onLocation = { value ->
                    edit(
                        UserNicknameModifyActivity.FIELD_LOCATION,
                        value,
                        viewContext.getString(R.string.qq_profile_location),
                        viewContext.getString(R.string.qq_profile_input_location),
                        120,
                    )
                },
                onSignature = { value ->
                    edit(
                        UserNicknameModifyActivity.FIELD_SIGNATURE,
                        value,
                        viewContext.getString(R.string.qq_profile_signature),
                        viewContext.getString(R.string.qq_profile_input_signature),
                        100,
                    )
                },
                onBio = { value ->
                    edit(
                        UserNicknameModifyActivity.FIELD_BIO,
                        value,
                        viewContext.getString(R.string.qq_profile_bio),
                        viewContext.getString(R.string.qq_profile_input_bio),
                        500,
                        true,
                    )
                },
                onGender = { current ->
                    UserGenderSheet(
                        viewContext,
                        object : UserGenderSheet.SelectCallback {
                            override fun s(selected: UserGender) {
                                val value = if (selected == UserGender.MALE) "male" else "female"
                                save(
                                    update = { api.updateProfile(gender = value) },
                                    fallback = { it.copy(gender = value) },
                                )
                            }
                        },
                        current,
                    ).show()
                },
                onBirthday = {
                    val start = Calendar.getInstance().apply { set(1900, 0, 1) }
                    val end = Calendar.getInstance()
                    b(
                        viewContext,
                        g { date, _ ->
                            val value = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(date)
                            save(
                                update = { api.updateProfile(birthday = value) },
                                fallback = { it.copy(birthday = value) },
                            )
                        },
                    ).E("").o(5).r(1.8f)
                        .F(booleanArrayOf(true, true, true, false, false, false))
                        .p("", "月", "日", "", "", "")
                        .u(true).s(true).t(start, end).j(end).b().v()
                },
                onResetAvatar = { gender ->
                    val current = profile
                    scope.launch(Dispatchers.IO) {
                        val response = runCatching { api.resetAvatar(gender) }.getOrNull()
                        withContext(Dispatchers.Main) {
                            if (response?.optBoolean("success", false) == true) {
                                profile = current?.copy(avatarUrl = null)
                                profile?.let { store.saveUserSession("cookie_session", it) }
                                avatarVersion++
                                AppNotice.success(
                                    context,
                                    context.getString(R.string.qq_profile_avatar_reset),
                                )
                            } else {
                                AppNotice.error(context, context.getString(R.string.qq_profile_save_failed))
                            }
                        }
                    }
                },
                onFeedback = { navController.navigate(Screen.Feedback.route) },
                onLogout = {
                    scope.launch(Dispatchers.IO) {
                        runCatching { api.logout() }
                        store.clearSession()
                        withContext(Dispatchers.Main) {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                },
            ).root
        },
        update = { root ->
            (root.tag as QqProfileBinding).bind(profile!!, avatarVersion)
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private class QqProfileBinding private constructor(
    val root: View,
    private val avatar: ImageView,
    private val nickname: TextView,
    private val gender: TextView,
    private val birthday: TextView,
    private val accountRow: SourceRow,
    private val locationRow: SourceRow,
    private val signatureRow: SourceRow,
    private val bioRow: SourceRow,
    private val resetRow: SourceRow,
    private val feedbackRow: SourceRow,
    private val logoutRow: SourceRow,
) {
    private var profile: UserInfo? = null
    private var avatarModel: String? = null

    fun bind(value: UserInfo, version: Int) {
        profile = value
        nickname.text = value.nickname ?: value.username
        gender.text = value.genderLabel()
        gender.alpha = if (value.gender.isNullOrBlank()) 0.5f else 1f
        birthday.text = value.birthdayLabel(root.context)
        accountRow.value.text = value.username.orUnset(root.context)
        locationRow.value.text = value.location.orUnset(root.context)
        signatureRow.value.text = value.signature.orUnset(root.context)
        bioRow.value.text = value.bio.orUnset(root.context)
        resetRow.root.visibility = if (value.avatarUrl.isNullOrBlank()) View.GONE else View.VISIBLE

        val model = value.avatarUrl?.let(::absoluteAvatarUrl)?.let { url ->
            url + if ('?' in url) "&v=$version" else "?v=$version"
        }
        if (model == avatarModel) return
        avatarModel = model
        if (model == null) {
            avatar.setImageResource(R.drawable.default_user_avatar)
        } else {
            root.context.imageLoader.enqueue(
                ImageRequest.Builder(root.context)
                    .data(model)
                    .placeholder(R.drawable.default_user_avatar)
                    .error(R.drawable.default_user_avatar)
                    .target(avatar)
                    .build(),
            )
        }
    }

    companion object {
        fun inflate(
            context: Context,
            onAvatar: () -> Unit,
            onNickname: (String) -> Unit,
            onAccount: (String) -> Unit,
            onLocation: (String) -> Unit,
            onSignature: (String) -> Unit,
            onBio: (String) -> Unit,
            onGender: (UserGender) -> Unit,
            onBirthday: () -> Unit,
            onResetAvatar: (String) -> Unit,
            onFeedback: () -> Unit,
            onLogout: () -> Unit,
        ): QqProfileBinding {
            val inflater = LayoutInflater.from(context)
            val root = inflater.inflate(R.layout.a40, null, false)
            val account = SourceRow(root.findViewById(R.id.j_o)).apply {
                label.setText(R.string.qq_profile_account)
            }
            val extras = listOf(
                R.id.qq_profile_location_row to R.string.qq_profile_location,
                R.id.qq_profile_signature_row to R.string.qq_profile_signature,
                R.id.qq_profile_bio_row to R.string.qq_profile_bio,
                R.id.qq_profile_reset_avatar_row to R.string.qq_profile_reset_avatar,
                R.id.qq_profile_feedback_row to R.string.qq_profile_feedback,
                R.id.qq_profile_logout_row to R.string.qq_profile_logout,
            ).map { (rowId, label) ->
                SourceRow(root.findViewById(rowId)).also {
                    it.label.setText(label)
                }
            }
            val location = extras[0]
            val signature = extras[1]
            val bio = extras[2]
            val reset = extras[3]
            val feedback = extras[4]
            val logout = extras[5]
            val avatar = root.findViewById<ImageView>(R.id.m6).apply {
                clipToOutline = true
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
            }
            val binding = QqProfileBinding(
                root = root,
                avatar = avatar,
                nickname = root.findViewById(R.id.mno),
                gender = root.findViewById(R.id.mnm),
                birthday = root.findViewById(R.id.mni),
                accountRow = account,
                locationRow = location,
                signatureRow = signature,
                bioRow = bio,
                resetRow = reset,
                feedbackRow = feedback,
                logoutRow = logout,
            )
            root.tag = binding
            root.findViewById<View>(R.id.j_k).setOnClickListener { onAvatar() }
            root.findViewById<View>(R.id.j_n).setOnClickListener {
                binding.profile?.let { onNickname(it.nickname ?: it.username) }
            }
            root.findViewById<View>(R.id.j_m).setOnClickListener {
                binding.profile?.let { onGender(it.toGender()) }
            }
            root.findViewById<View>(R.id.j_l).setOnClickListener { onBirthday() }
            account.root.setOnClickListener { binding.profile?.let { onAccount(it.username) } }
            location.root.setOnClickListener { binding.profile?.let { onLocation(it.location.orEmpty()) } }
            signature.root.setOnClickListener { binding.profile?.let { onSignature(it.signature.orEmpty()) } }
            bio.root.setOnClickListener { binding.profile?.let { onBio(it.bio.orEmpty()) } }
            reset.root.setOnClickListener {
                binding.profile?.let { onResetAvatar(it.gender ?: "secret") }
            }
            feedback.root.setOnClickListener { onFeedback() }
            logout.root.setOnClickListener { onLogout() }
            return binding
        }
    }
}

private class SourceRow(val root: View) {
    val label: TextView = root.findViewById(R.id.qq_profile_row_label)
    val value: TextView = root.findViewById(R.id.qq_profile_row_value)
}

private fun UserInfo.withField(field: String, value: String): UserInfo = when (field) {
    UserNicknameModifyActivity.FIELD_USERNAME -> copy(username = value)
    UserNicknameModifyActivity.FIELD_LOCATION -> copy(location = value)
    UserNicknameModifyActivity.FIELD_SIGNATURE -> copy(signature = value)
    UserNicknameModifyActivity.FIELD_BIO -> copy(bio = value)
    else -> copy(nickname = value)
}

private fun UserInfo.toGender(): UserGender = when (gender?.lowercase(Locale.ROOT)) {
    "male", "1", "男" -> UserGender.MALE
    "female", "2", "女" -> UserGender.FEMALE
    else -> UserGender.UNKNOWN
}

private fun UserInfo.genderLabel(): String = when (toGender()) {
    UserGender.MALE -> "男"
    UserGender.FEMALE -> "女"
    else -> "请选择"
}

private fun UserInfo.birthdayLabel(context: Context): String {
    val value = birthday ?: return context.getString(R.string.qq_profile_unset)
    val date = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(value)
    }.getOrNull() ?: return value
    return SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA).format(date)
}

private fun String?.orUnset(context: Context): String =
    if (isNullOrBlank()) context.getString(R.string.qq_profile_unset) else this

private fun absoluteAvatarUrl(url: String): String =
    if (url.startsWith("http")) url else "${ApiClient.BASE_URL}$url"

private fun uriToAvatarFile(context: Context, uri: Uri, mime: String): File {
    val suffix = when (mime) {
        "image/png" -> ".png"
        "image/webp" -> ".webp"
        else -> ".jpg"
    }
    return File(context.cacheDir, "avatar_${System.currentTimeMillis()}$suffix").also { file ->
        context.contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(file).use { output -> input?.copyTo(output) }
        }
    }
}
