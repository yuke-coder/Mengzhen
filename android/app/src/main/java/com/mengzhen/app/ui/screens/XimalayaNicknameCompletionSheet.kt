package com.mengzhen.app.ui.screens

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil3.load
import com.mengzhen.app.R
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.UserInfo
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.components.rememberQqMusicImagePicker
import com.mengzhen.app.ui.feedback.AppNotice
import com.mengzhen.app.wechat.WechatProfileDraft
import com.mengzhen.app.wechat.WechatProfileSyncCoordinator
import com.mengzhen.app.wechat.WechatProfileSyncEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

private data class NicknameCompletionInput(
    val nickname: String,
    val selectedAvatar: Uri?,
    val wechatAvatarUrl: String?,
)

/**
 * Ximalaya 9.5.4.7 `NickNameSettingDialogFragment`, hosted without redrawing its XML.
 */
@Composable
internal fun XimalayaNicknameCompletionSheet(
    user: UserInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val api = remember(context) { ApiClient.get(context) }
    val store = remember(context) { TaskStore.get(context) }
    val scope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    var sourceDialog by remember { mutableStateOf<XimalayaNicknameCompletionDialog?>(null) }

    val avatarPicker = rememberQqMusicImagePicker(maxSelection = 1) { selected ->
        selected.firstOrNull()?.let { sourceDialog?.showSelectedAvatar(it) }
    }

    fun applyWechatEvent(event: WechatProfileSyncEvent) {
        when (event) {
            is WechatProfileSyncEvent.Success -> sourceDialog?.showWechatProfile(event.profile)
            is WechatProfileSyncEvent.Error -> AppNotice.error(context, event.message)
        }
        WechatProfileSyncCoordinator.acknowledge(context)
    }

    DisposableEffect(user.id) {
        lateinit var dialog: XimalayaNicknameCompletionDialog
        dialog = XimalayaNicknameCompletionDialog(
            context = context,
            onPickAvatar = avatarPicker,
            onSyncWechat = {
                WechatProfileSyncCoordinator.request(context)?.let {
                    AppNotice.error(context, it)
                }
            },
            onComplete = { input ->
                scope.launch {
                    dialog.setSaving(true)
                    runCatching {
                        var updated = user
                        val localAvatar = input.selectedAvatar
                        if (localAvatar != null) {
                            val mimeType = context.contentResolver.getType(localAvatar)
                                ?.substringBefore(';')
                                ?.lowercase()
                                .orEmpty()
                            if (mimeType !in SUPPORTED_AVATAR_TYPES) {
                                error("仅支持 JPG、PNG、GIF、WebP 格式")
                            }
                            val file = withContext(Dispatchers.IO) {
                                persistProfileFile(context, localAvatar, "avatar")
                            } ?: error("头像读取失败，请重试")
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    api.uploadAvatar(file, mimeType)
                                }
                                if (!response.optBoolean("success")) {
                                    error(response.optString("error", "头像更新失败"))
                                }
                                val avatarUrl = response.optString("avatar_url", "")
                                    .takeIf(String::isNotBlank)
                                    ?: error("头像更新失败")
                                updated = updated.copy(avatarUrl = avatarUrl)
                                store.getSession()?.first?.let { token ->
                                    store.saveUserSession(token, updated)
                                }
                            } finally {
                                file.delete()
                            }
                        } else if (!input.wechatAvatarUrl.isNullOrBlank()) {
                            updated = updated.copy(avatarUrl = input.wechatAvatarUrl)
                        }

                        val response = withContext(Dispatchers.IO) {
                            api.updateProfile(
                                nickname = input.nickname,
                                avatarUrl = updated.avatarUrl
                                    ?.takeIf { it != user.avatarUrl },
                            )
                        }
                        if (!response.optBoolean("success")) {
                            error(response.optString("error", "资料更新失败"))
                        }
                        updated = updated.copy(nickname = input.nickname)
                        store.getSession()?.first?.let { token ->
                            store.saveUserSession(token, updated)
                        }
                    }.onSuccess {
                        AppNotice.success(context, "资料已更新")
                        dialog.dismiss()
                    }.onFailure {
                        AppNotice.error(context, it.message ?: "资料更新失败")
                        dialog.setSaving(false)
                    }
                }
            },
        )
        sourceDialog = dialog
        dialog.setOnDismissListener { currentOnDismiss() }
        dialog.show()
        onDispose {
            dialog.setOnDismissListener(null)
            dialog.dismiss()
            if (sourceDialog === dialog) sourceDialog = null
        }
    }

    LaunchedEffect(sourceDialog) {
        val dialog = sourceDialog ?: return@LaunchedEffect
        WechatProfileSyncCoordinator.takePending(context)?.let(::applyWechatEvent)
        WechatProfileSyncCoordinator.events.collect { event ->
            if (sourceDialog === dialog) applyWechatEvent(event)
        }
    }
}

private class XimalayaNicknameCompletionDialog(
    context: Context,
    private val onPickAvatar: () -> Unit,
    private val onSyncWechat: () -> Unit,
    private val onComplete: (NicknameCompletionInput) -> Unit,
) : Dialog(context, R.style.XimalayaNicknameCompletionDialogTheme) {
    private val content = LayoutInflater.from(context)
        .inflate(R.layout.main_dialog_nick_name_setting, null, false)
    private val avatar = content.findViewById<ImageView>(R.id.main_iv_avatar)
    private val uploadAvatar = content.findViewById<TextView>(R.id.main_tv_upload_head)
    private val nickname = content.findViewById<EditText>(R.id.main_et_nickname)
    private val syncWechat = content.findViewById<TextView>(R.id.main_tv_sync_info)
    private val complete = content.findViewById<TextView>(R.id.main_tv_complete)
    private var selectedAvatar: Uri? = null
    private var wechatProfile: WechatProfileDraft? = null

    init {
        setContentView(content)
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        complete.isSelected = true
        content.findViewById<TextView>(R.id.main_tv_skip).setOnClickListener { dismiss() }
        avatar.setOnClickListener { onPickAvatar() }
        uploadAvatar.setOnClickListener { onPickAvatar() }
        syncWechat.setOnClickListener { onSyncWechat() }
        complete.setOnClickListener {
            val value = nickname.text?.toString().orEmpty()
            when {
                value.isEmpty() -> AppNotice.warning(context, "请填写昵称")
                value.trim().isEmpty() -> AppNotice.warning(context, "请输入正确的昵称!")
                else -> onComplete(
                    NicknameCompletionInput(
                        nickname = value,
                        selectedAvatar = selectedAvatar,
                        wechatAvatarUrl = wechatProfile?.avatarUrl,
                    ),
                )
            }
        }
        nickname.setOnEditorActionListener { view, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_DONE) return@setOnEditorActionListener false
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.hideSoftInputFromWindow(view.windowToken, 0)
            true
        }
    }

    override fun onStart() {
        super.onStart()
        window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setWindowAnimations(R.style.XimalayaNicknameCompletionDialogAnimation)
            attributes = attributes.apply {
                gravity = Gravity.BOTTOM
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    fun showSelectedAvatar(uri: Uri) {
        wechatProfile = null
        selectedAvatar = uri
        avatar.background = null
        avatar.load(uri)
        uploadAvatar.setText(R.string.ximalaya_nickname_sheet_change_avatar)
    }

    fun showWechatProfile(profile: WechatProfileDraft) {
        selectedAvatar = null
        wechatProfile = profile
        nickname.setText(profile.nickname)
        nickname.setSelection(profile.nickname.length)
        avatar.background = null
        avatar.load(profile.avatarUrl)
        uploadAvatar.setText(R.string.ximalaya_nickname_sheet_change_avatar)
    }

    fun setSaving(saving: Boolean) {
        complete.isEnabled = !saving
    }
}

/** Direct port of source=3's login/profile flag + one-day frequency gate. */
internal object XimalayaNicknameCompletionTrigger {
    private const val PREFS_NAME = "dream_pillow"
    private const val LAST_SHOW_KEY = "key_nick_name_dialog_last_show_date_new_3"

    fun shouldShow(context: Context, user: UserInfo): Boolean {
        // Dream's nullable profile nickname is the local equivalent of isNeedChangeNickname.
        if (user.id.isBlank() || !user.nickname.isNullOrBlank()) return false
        val lastShown = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(LAST_SHOW_KEY, -1L)
        if (lastShown == -1L) return true

        val last = Calendar.getInstance().apply {
            time = Date(lastShown)
            firstDayOfWeek = Calendar.MONDAY
        }
        val now = Calendar.getInstance().apply {
            time = Date(System.currentTimeMillis())
            firstDayOfWeek = Calendar.MONDAY
        }
        val elapsedDays = (now.get(Calendar.YEAR) - last.get(Calendar.YEAR)) * 365 +
            (now.get(Calendar.MONTH) - last.get(Calendar.MONTH)) * 30 +
            (now.get(Calendar.DAY_OF_MONTH) - last.get(Calendar.DAY_OF_MONTH))
        return elapsedDays >= 1
    }

    fun markShown(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_SHOW_KEY, System.currentTimeMillis())
            .apply()
    }
}

private val SUPPORTED_AVATAR_TYPES = setOf(
    "image/jpeg",
    "image/png",
    "image/gif",
    "image/webp",
)
