package com.mengzhen.app.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.mengzhen.app.R
import com.mengzhen.app.auth.TurnstileChallengeActivity
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.parseUser
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.feedback.AppNotice
import com.mengzhen.app.ui.navigation.Screen
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SourceAuthMode {
    LOGIN,
    REGISTER,
}

private enum class SourceLoginStep {
    ACCOUNT,
    PASSWORD,
}

private data class PendingSourceAuth(
    val mode: SourceAuthMode,
    val username: String,
    val password: String,
)

/**
 * 视图层直接加载喜马拉雅 9.4.95.3 的原始登录 XML。
 *
 * 登录与注册只是同一源码视图的两种业务状态；尺寸、配色、图标、
 * 按钮状态、密码显隐、加载动画和返回行为均沿用客户端源码。
 */
@Composable
fun LoginScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { TaskStore.get(context) }
    val api = remember(context) { ApiClient.get(context) }

    var mode by rememberSaveable { mutableStateOf(SourceAuthMode.LOGIN) }
    var step by rememberSaveable { mutableStateOf(SourceLoginStep.ACCOUNT) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var pendingAuth by remember { mutableStateOf<PendingSourceAuth?>(null) }

    fun authenticate(request: PendingSourceAuth, turnstileToken: String) {
        val failureMessage =
            if (request.mode == SourceAuthMode.LOGIN) "登录失败" else "注册失败"
        scope.launch(Dispatchers.IO) {
            try {
                val result = when (request.mode) {
                    SourceAuthMode.LOGIN -> api.login(
                        request.username,
                        request.password,
                        turnstileToken,
                    )

                    SourceAuthMode.REGISTER -> api.register(
                        request.username,
                        request.password,
                        turnstileToken,
                    )
                }
                if (result.optBoolean("success", false)) {
                    val verified = api.me()
                    val user = parseUser(verified)
                    if (user == null) {
                        api.clearCookies()
                        store.clearSession()
                        val message = verified.optString("error")
                            .ifBlank { verified.optString("message") }
                            .ifBlank { "登录状态建立失败，请重试" }
                        withContext(Dispatchers.Main) {
                            AppNotice.error(context, message)
                        }
                        return@launch
                    }
                    store.saveUserSession("cookie_session", user)
                    withContext(Dispatchers.Main) {
                        AppNotice.success(
                            context,
                            if (request.mode == SourceAuthMode.LOGIN) "登录成功" else "注册成功",
                        )
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                        }
                    }
                } else {
                    val message = result.optString("error")
                        .ifBlank {
                            result.optString(
                                "message",
                                failureMessage,
                            )
                        }
                    withContext(Dispatchers.Main) {
                        AppNotice.error(context, message)
                    }
                }
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    AppNotice.error(context, authErrorMessage(error, failureMessage))
                }
            } finally {
                withContext(Dispatchers.Main) {
                    loading = false
                }
            }
        }
    }

    val turnstileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val request = pendingAuth
        pendingAuth = null
        val token = result.data?.getStringExtra(TurnstileChallengeActivity.EXTRA_TOKEN)
        if (result.resultCode == Activity.RESULT_OK && request != null && !token.isNullOrBlank()) {
            authenticate(request, token)
            return@rememberLauncherForActivityResult
        }

        loading = false
        val error = result.data?.getStringExtra(TurnstileChallengeActivity.EXTRA_ERROR)
        if (error.isNullOrBlank()) {
            AppNotice.warning(context, "安全验证已取消")
        } else {
            AppNotice.error(context, error)
        }
    }

    fun showAccount() {
        if (loading) return
        password = ""
        passwordVisible = false
        step = SourceLoginStep.ACCOUNT
    }

    fun switchMode(target: SourceAuthMode) {
        if (loading || mode == target) return
        mode = target
        showAccount()
    }

    fun showPassword() {
        if (username.isBlank()) {
            AppNotice.warning(context, "请输入用户名")
            return
        }
        step = SourceLoginStep.PASSWORD
    }

    fun changeAccount() {
        if (loading) return
        username = ""
        showAccount()
    }

    fun enterAsGuest() {
        if (!navController.popBackStack()) {
            navController.navigate(Screen.Settings.route) { launchSingleTop = true }
        }
    }

    fun submit() {
        if (loading) return
        val account = username.trim()
        if (account.isEmpty() || password.isEmpty()) {
            AppNotice.warning(context, "请输入用户名和密码")
            return
        }
        if (mode == SourceAuthMode.REGISTER && password.length < 6) {
            AppNotice.warning(context, "密码长度不能少于 6 位")
            return
        }

        val request = PendingSourceAuth(
            mode = mode,
            username = account,
            password = password,
        )
        loading = true
        pendingAuth = request
        runCatching {
            turnstileLauncher.launch(TurnstileChallengeActivity.createIntent(context))
        }.onFailure {
            pendingAuth = null
            loading = false
            AppNotice.error(context, "无法启动安全验证，请重试")
        }
    }

    BackHandler(
        enabled = step == SourceLoginStep.PASSWORD || mode == SourceAuthMode.REGISTER,
    ) {
        when {
            step == SourceLoginStep.PASSWORD -> showAccount()
            mode == SourceAuthMode.REGISTER -> switchMode(SourceAuthMode.LOGIN)
        }
    }

    key(mode, step) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                when (step) {
                    SourceLoginStep.ACCOUNT -> createAccountView(
                        context = viewContext,
                        mode = mode,
                        initialUsername = username,
                        onUsernameChanged = { username = it },
                        onBack = {
                            if (mode == SourceAuthMode.REGISTER) {
                                switchMode(SourceAuthMode.LOGIN)
                            } else {
                                navController.popBackStack()
                            }
                        },
                        onGuest = ::enterAsGuest,
                        onNext = ::showPassword,
                        onSwitchMode = {
                            switchMode(
                                if (mode == SourceAuthMode.LOGIN) {
                                    SourceAuthMode.REGISTER
                                } else {
                                    SourceAuthMode.LOGIN
                                },
                            )
                        },
                    )

                    SourceLoginStep.PASSWORD -> createPasswordView(
                        context = viewContext,
                        mode = mode,
                        username = username,
                        initialPassword = password,
                        initialPasswordVisible = passwordVisible,
                        onPasswordChanged = { password = it },
                        onPasswordVisibilityChanged = { passwordVisible = it },
                        onBack = ::showAccount,
                        onChangeAccount = ::changeAccount,
                        onSubmit = ::submit,
                    )
                }
            },
            update = { root ->
                // 切换 key 时，Compose 可能先用新状态更新一次旧 Android View。
                // 以当前实际挂载的原始 XML 为准，避免跨布局查找不存在的控件。
                if (root.findViewById<EditText>(R.id.login_v2_pwd_input) != null) {
                    updatePasswordView(
                        root = root,
                        password = password,
                        passwordVisible = passwordVisible,
                        loading = loading,
                        mode = mode,
                    )
                } else {
                    updateAccountButton(root, username, loading)
                }
            },
        )
    }
}

private fun authErrorMessage(error: Exception, fallback: String): String = when (error) {
    is UnknownHostException -> "网络连接不可用，请检查网络后重试"
    is SocketTimeoutException -> "请求超时，请稍后重试"
    else -> fallback
}

private fun createAccountView(
    context: Context,
    mode: SourceAuthMode,
    initialUsername: String,
    onUsernameChanged: (String) -> Unit,
    onBack: () -> Unit,
    onGuest: () -> Unit,
    onNext: () -> Unit,
    onSwitchMode: () -> Unit,
): View {
    val root = LayoutInflater.from(context)
        .inflate(R.layout.login_phone_email_v2_layout, null, false)

    root.findViewById<TextView>(R.id.login_v2_pe_title).text =
        if (mode == SourceAuthMode.LOGIN) "账号登录" else "注册账号"
    val input = root.findViewById<EditText>(R.id.login_v2_pe_input)
    input.hint = "请输入用户名"
    input.inputType = InputType.TYPE_CLASS_TEXT
    input.imeOptions = EditorInfo.IME_ACTION_NEXT
    (input.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
        params.marginStart = context.dp(16)
        input.layoutParams = params
    }
    input.setText(initialUsername)
    input.setSelection(input.text.length)

    val clear = root.findViewById<ImageView>(R.id.login_v2_pe_clear)
    val button = root.findViewById<FrameLayout>(R.id.login_v2_pe_btn)
    root.findViewById<TextView>(R.id.login_v2_pe_btn_text).text = "下一步"

    val switchMode = root.findViewById<TextView>(R.id.login_v2_pe_password_login)
    switchMode.text =
        if (mode == SourceAuthMode.LOGIN) "注册账号" else "已有账号？登录"
    switchMode.visibility = View.VISIBLE

    root.findViewById<View>(R.id.login_v2_pe_back_btn).setOnClickListener { onBack() }
    addSourceGuestEntry(root as ConstraintLayout, onGuest)
    clear.setOnClickListener {
        input.setText("")
        input.requestFocus()
    }
    button.setOnClickListener { onNext() }
    switchMode.setOnClickListener { onSwitchMode() }
    input.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_NEXT) {
            onNext()
            true
        } else {
            false
        }
    }
    input.addTextChangedListener(afterTextChanged { text ->
        onUsernameChanged(text)
        clear.visibility = if (text.isNotEmpty()) View.VISIBLE else View.INVISIBLE
        updateAccountButton(root, text, loading = false)
    })

    clear.visibility = if (initialUsername.isNotEmpty()) View.VISIBLE else View.INVISIBLE
    updateAccountButton(root, initialUsername, loading = false)
    input.post {
        input.requestFocus()
        input.showKeyboard()
    }
    return root
}

private fun createPasswordView(
    context: Context,
    mode: SourceAuthMode,
    username: String,
    initialPassword: String,
    initialPasswordVisible: Boolean,
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    onChangeAccount: () -> Unit,
    onSubmit: () -> Unit,
): View {
    val root = LayoutInflater.from(context)
        .inflate(R.layout.login_password_v2_layout, null, false)

    root.findViewById<TextView>(R.id.login_v2_pwd_title).text =
        if (mode == SourceAuthMode.LOGIN) "请输入密码" else "请设置密码"
    root.findViewById<TextView>(R.id.login_v2_pwd_account).text = username
    root.findViewById<TextView>(R.id.login_v2_pwd_change_account).text = "换账号"

    val input = root.findViewById<EditText>(R.id.login_v2_pwd_input)
    input.hint = if (mode == SourceAuthMode.LOGIN) "请输入密码" else "请设置至少 6 位密码"
    input.tag = PasswordTypefaces(
        default = input.typeface,
        visible = Typeface.createFromAsset(
            context.assets,
            "fonts/XmlyNumberV1.0-SemiBold.otf",
        ),
    )
    input.imeOptions = EditorInfo.IME_ACTION_DONE
    input.setText(initialPassword)
    setPasswordVisible(root, initialPasswordVisible)

    root.findViewById<View>(R.id.login_v2_pwd_close_btn).setOnClickListener { onBack() }
    root.findViewById<View>(R.id.login_v2_pwd_change_account).setOnClickListener {
        onChangeAccount()
    }
    root.findViewById<View>(R.id.login_v2_pwd_lock_icon).setOnClickListener {
        val visible = input.inputType == VISIBLE_PASSWORD_INPUT_TYPE
        onPasswordVisibilityChanged(!visible)
        setPasswordVisible(root, !visible)
    }
    root.findViewById<FrameLayout>(R.id.login_v2_pwd_btn).setOnClickListener { onSubmit() }
    input.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            onSubmit()
            true
        } else {
            false
        }
    }
    input.addTextChangedListener(afterTextChanged { text ->
        onPasswordChanged(text)
        updatePasswordView(
            root = root,
            password = text,
            passwordVisible = input.inputType == VISIBLE_PASSWORD_INPUT_TYPE,
            loading = false,
            mode = mode,
        )
    })

    updatePasswordView(
        root = root,
        password = initialPassword,
        passwordVisible = initialPasswordVisible,
        loading = false,
        mode = mode,
    )
    input.post {
        input.requestFocus()
        input.showKeyboard()
    }
    return root
}

private fun updateAccountButton(root: View, username: String, loading: Boolean) {
    val enabled = username.isNotBlank() && !loading
    val button = root.findViewById<FrameLayout>(R.id.login_v2_pe_btn)
    button.isEnabled = enabled
    button.setBackgroundResource(
        if (enabled) R.drawable.arg_res_0x7f080573 else R.drawable.arg_res_0x7f08178b,
    )
}

private fun updatePasswordView(
    root: View,
    password: String,
    passwordVisible: Boolean,
    loading: Boolean,
    mode: SourceAuthMode,
) {
    val input = root.findViewById<EditText>(R.id.login_v2_pwd_input)
    if ((input.inputType == VISIBLE_PASSWORD_INPUT_TYPE) != passwordVisible) {
        setPasswordVisible(root, passwordVisible)
    }
    updatePasswordTypography(input, passwordVisible)

    val button = root.findViewById<FrameLayout>(R.id.login_v2_pwd_btn)
    val loadingView = root.findViewById<ImageView>(R.id.login_v2_pwd_loading)
    val buttonText = root.findViewById<TextView>(R.id.login_v2_pwd_btn_text)
    val enabled = password.isNotEmpty() && !loading

    button.isEnabled = enabled
    button.setBackgroundResource(
        if (enabled) R.drawable.arg_res_0x7f080573 else R.drawable.arg_res_0x7f08178b,
    )

    if (loading) {
        loadingView.visibility = View.VISIBLE
        if (loadingView.animation == null) {
            loadingView.startAnimation(sourceLoadingAnimation())
        }
        buttonText.text = if (mode == SourceAuthMode.LOGIN) "登录中" else "注册中"
        input.hideKeyboard()
    } else {
        loadingView.clearAnimation()
        loadingView.visibility = View.GONE
        buttonText.text = if (mode == SourceAuthMode.LOGIN) "登录" else "注册"
    }
}

private fun setPasswordVisible(root: View, visible: Boolean) {
    val input = root.findViewById<EditText>(R.id.login_v2_pwd_input)
    input.inputType = if (visible) VISIBLE_PASSWORD_INPUT_TYPE else HIDDEN_PASSWORD_INPUT_TYPE
    root.findViewById<ImageView>(R.id.login_v2_pwd_lock_icon).setImageResource(
        if (visible) R.drawable.arg_res_0x7f0817cd else R.drawable.arg_res_0x7f0817c8,
    )
    input.setSelection(input.text.length)
    updatePasswordTypography(input, visible)
}

private fun updatePasswordTypography(input: EditText, visible: Boolean) {
    val typefaces = input.tag as? PasswordTypefaces
    input.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (visible && input.text.isNotEmpty()) 18f else 14f)
    input.typeface = if (visible && input.text.isNotEmpty()) {
        typefaces?.visible ?: input.typeface
    } else {
        typefaces?.default ?: input.typeface
    }
}

private fun addSourceGuestEntry(root: ConstraintLayout, onGuest: () -> Unit) {
    val context = root.context
    val view = TextView(context).apply {
        id = View.generateViewId()
        text = "随便逛逛"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setTextColor(ContextCompat.getColor(context, R.color.arg_res_0x7f060224))
        gravity = android.view.Gravity.CENTER
        setPadding(context.dp(10), context.dp(10), context.dp(10), context.dp(10))
        setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            ContextCompat.getDrawable(context, R.drawable.arg_res_0x7f08098f),
            null,
        )
        compoundDrawablePadding = context.dp(3)
        setOnClickListener { onGuest() }
    }
    root.addView(
        view,
        ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = context.dp(40)
            marginEnd = context.dp(12)
        },
    )
}

private fun afterTextChanged(action: (String) -> Unit): TextWatcher =
    object : TextWatcher {
        override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(text: Editable?) {
            action(text?.toString().orEmpty())
        }
    }

private fun sourceLoadingAnimation(): Animation =
    RotateAnimation(
        0f,
        360f,
        Animation.RELATIVE_TO_SELF,
        0.5f,
        Animation.RELATIVE_TO_SELF,
        0.5f,
    ).apply {
        duration = 1_000L
        repeatCount = Animation.INFINITE
    }

private fun View.showKeyboard() {
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
        ?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

private fun View.hideKeyboard() {
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
        ?.hideSoftInputFromWindow(windowToken, 0)
}

private fun Context.dp(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()

private data class PasswordTypefaces(
    val default: Typeface,
    val visible: Typeface,
)

private const val VISIBLE_PASSWORD_INPUT_TYPE =
    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

private const val HIDDEN_PASSWORD_INPUT_TYPE =
    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
