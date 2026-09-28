package com.mengzhen.app.ui.fragments

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Space
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil3.load
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.mengzhen.app.R
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.UserInfo
import com.mengzhen.app.data.model.parseProfile
import com.mengzhen.app.data.store.ProfileVisibilityStore
import com.mengzhen.app.data.store.ProfileVisibilityStore.Field
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.components.main.absoluteAvatarUrl
import com.mengzhen.app.ui.feedback.AppNotice
import com.mengzhen.app.ui.screens.XimalayaTitleAction
import com.mengzhen.app.ui.screens.installXimalayaTitleBar
import com.mengzhen.app.ui.screens.persistProfileFile
import com.mengzhen.app.ui.screens.uploadSelectedProfileBackground
import com.mengzhen.app.wechat.WechatProfileDraft
import com.mengzhen.app.wechat.WechatProfileSyncCoordinator
import com.mengzhen.app.wechat.WechatProfileSyncEvent
import com.mengzhen.app.util.genderLabel
import com.mengzhen.app.util.isGenderSecret
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

/**
 * 喜马拉雅 Android 9.5.4.8 MyDetailFragment 真实迁移（资料编辑主页）。
 *
 * 来源：D:/xm_dex/cfr_out/com/ximalaya/ting/android/main/fragment/myspace/child/MyDetailFragment.java
 * 反编译方式：apktool 3.0.3 解码资源 + dex2jar classes5.dex + CFR 0.152 单类反编译。
 *
 * 约束：
 * - 字段定义、findViewById 顺序、onClick 分发、数据绑定逻辑均按真本 Java 逐行还原。
 * - 后端 API 复用梦枕真实现（ApiClient / parseProfile / persistSession / uploadAvatar /
 *   uploadSelectedProfileBackground / persistProfileFile / absoluteAvatarUrl），不捏造数据接口。
 * - 标题栏遵循 installXimalayaTitleBar 契约（宿主 id 仍用真本 main_title_bar）。
 * - 梦枕无对应后端/组件的认证入口：保留点击入口、
 *   补诚实提示（"XX功能暂未开放"），不伪造子页面。
 *   依据：梦枕 UserInfo 无 verifyUrl 字段，且无认证 WebView 可承载该能力。
 * - 原「标签」行（真本 MyLabelDialogFragment 标签）在梦枕无 label 字段，已改为「个性签名」行：
 *   绑定 UserInfo.signature（首页/个人页亦用之），点击经 EditPersonalInfoFragment type=4 真正可编辑并保存。
 */
class MyDetailFragment : Fragment(), View.OnClickListener {

    // === 真本字段映射（按 CFR 输出命名，保持与布局 id 一致） ===

    private lateinit var titleBar: RelativeLayout
    private lateinit var topBg: ImageView
    private lateinit var avatar: ImageView
    private lateinit var editAvatar: ImageView
    private lateinit var avatarSpace: Space
    private lateinit var reviewAvatar: TextView
    private lateinit var editBg: TextView

    // 资料行
    private lateinit var usernameEdit: TextView
    private lateinit var nicknameEdit: TextView
    private lateinit var nicknameGuide: TextView
    private lateinit var nicknameVerifyStatus: TextView
    private lateinit var labelEdit: TextView
    private lateinit var briefEdit: TextView
    private lateinit var briefGuide: TextView
    private lateinit var briefVerifyStatus: TextView
    private lateinit var sexEdit: TextView
    private lateinit var sexGuide: TextView
    private lateinit var birthEdit: TextView
    private lateinit var birthGuide: TextView
    private lateinit var regionEdit: TextView
    private lateinit var regionGuide: TextView

    // 认证 / 隐私 / 底部栏
    private lateinit var verifyStatus: TextView
    private lateinit var weixinSync: View
    private lateinit var profileSave: TextView
    private lateinit var profileCancel: TextView

    // 行容器（用于点击分发）
    private lateinit var rowUsername: View
    private lateinit var rowNickname: View
    private lateinit var rowLabel: View
    private lateinit var rowBrief: View
    private lateinit var rowSex: View
    private lateinit var rowBirth: View
    private lateinit var rowRegion: View
    private lateinit var rowVerify: View
    private lateinit var rowPrivacy: View

    // === 状态字段（真本 X / Y / Z / aa / ab / W / V） ===

    private var currentUser: UserInfo? = null
    private var refreshOnResume: Boolean = false
    private var applyingWechatProfile: Boolean = false

    /** 真本 V：初始 true，e() 成功/失败回调首行置 false 且永不复位——仅首次加载展示加载动效 */
    private var firstLoad: Boolean = true

    // === 页面加载动效（真本 BaseFragment2.mLoadingView2 / mLoadingLottieView） ===

    private var loadingView: View? = null
    private var loadingLottieView: LottieAnimationView? = null

    private val pickAvatar = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { uploadAvatar(it) } }

    private val pickBackground = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { uploadBackground(it) } }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.xm_my_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi(view)
        registerResultListeners()
        observeWechatProfileSync()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        WechatProfileSyncCoordinator.takePending(requireContext())
            ?.let(::handleWechatProfileSyncEvent)
        if (refreshOnResume) {
            refreshOnResume = false
            loadData()
        }
    }

    // === initUi (真本 c()/d() 合并) ===

    private fun initUi(view: View) {
        // 真本 initUi(Bundle): setTitle(2131887972) -> "编辑资料"
        // 标题栏安装由 installXimalayaTitleBar 完成，容器 id 对齐真本 main_title_bar
        titleBar = view.findViewById(R.id.main_title_bar)
        installXimalayaTitleBar(
            titleBar,
            getString(R.string.xm_my_detail_7f120764),
            XimalayaTitleAction.Back { requireActivity().onBackPressedDispatcher.onBackPressed() },
        )

        // 真本 c() 方法：按 id 顺序 findViewById
        topBg = view.findViewById(R.id.main_iv_top_bg)
        val shadow = view.findViewById<View>(R.id.main_view_shadow)
        avatar = view.findViewById(R.id.main_iv_avatar)
        avatarSpace = view.findViewById(R.id.main_space_avatar)
        editAvatar = view.findViewById(R.id.main_iv_edit_avatar)
        reviewAvatar = view.findViewById(R.id.main_tv_review_avatar)
        editBg = view.findViewById(R.id.main_tv_edit_bg)

        rowUsername = view.findViewById(R.id.main_rl_modify_username)
        usernameEdit = view.findViewById(R.id.main_username_edit)

        rowNickname = view.findViewById(R.id.main_rl_modify_nickname)
        nicknameEdit = view.findViewById(R.id.main_nickname_edit)
        nicknameGuide = view.findViewById(R.id.main_tv_nickname_guide)
        nicknameVerifyStatus = view.findViewById(R.id.main_tv_nickname_verify_status)

        rowLabel = view.findViewById(R.id.main_rl_modify_label)
        labelEdit = view.findViewById(R.id.main_label_edit)

        rowBrief = view.findViewById(R.id.main_rl_modify_brief)
        briefEdit = view.findViewById(R.id.main_brief_edit)
        briefGuide = view.findViewById(R.id.main_tv_brief_guide)
        briefVerifyStatus = view.findViewById(R.id.main_tv_brief_verify_status)

        rowSex = view.findViewById(R.id.main_rl_modify_sex)
        sexEdit = view.findViewById(R.id.main_sex_edit)
        sexGuide = view.findViewById(R.id.main_tv_sex_guide)

        rowBirth = view.findViewById(R.id.main_rl_modify_birth_date)
        birthEdit = view.findViewById(R.id.main_birth_date_edit)
        birthGuide = view.findViewById(R.id.main_tv_birthday_guide)

        rowRegion = view.findViewById(R.id.main_rl_modify_region)
        regionEdit = view.findViewById(R.id.main_region_edit)
        regionGuide = view.findViewById(R.id.main_tv_region_guide)

        rowVerify = view.findViewById(R.id.main_rl_verify_layout)
        verifyStatus = view.findViewById(R.id.main_tv_verify_status)

        rowPrivacy = view.findViewById(R.id.main_rl_privacy)
        weixinSync = view.findViewById(R.id.main_tv_weixin)
        profileSave = view.findViewById(R.id.main_tv_profile_save)
        profileCancel = view.findViewById(R.id.main_tv_profile_cancel)

        // 真本 d()：设置 OnClickListener
        listOf(
            avatar, editAvatar,
            editBg,
            rowUsername, rowNickname, rowLabel, rowBrief,
            rowSex, rowBirth, rowRegion,
            rowVerify,
            rowPrivacy, weixinSync, profileSave, profileCancel,
        ).forEach { it.setOnClickListener(this) }
    }

    private fun registerResultListeners() {
        // 昵称 / 简介 / 生日 / 地区编辑结果
        childFragmentManager.setFragmentResultListener(
            EditPersonalInfoFragment.RESULT_KEY,
            viewLifecycleOwner,
        ) { _, bundle -> onEditPersonalInfoResult(bundle) }

        childFragmentManager.setFragmentResultListener(
            RegionSelectFragment.RESULT_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            // region 可为 null：只勾「保密地区」开关而未选地区时，值不动、只改可见性
            val region = bundle.getString(RegionSelectFragment.RESULT_REGION)
            val hide = bundle.getBoolean(RegionSelectFragment.RESULT_HIDE_REGION, false)
            // 两边都写：后端字段（权威，与 Web 端共用）+ 本机镜像（部署落后时兜底）
            viewLifecycleOwner.lifecycleScope.launch {
                ProfileVisibilityStore.setHidden(requireContext(), Field.REGION, hide)
            }
            if (region == null) {
                saveField(hideRegion = hide) { bindRegion(currentUser, hidden = hide) }
            } else {
                saveField(location = region, hideRegion = hide) { bindRegion(currentUser, hidden = hide) }
            }
        }
    }

    // === loadData (真本 loadData/e()) ===

    private fun loadData() {
        // 真本 loadData()：V=true 时 onPageLoadingCompleted(LOADING) 展示加载动效，随后 e() 拉取数据
        if (firstLoad) {
            showPageLoading()
        }
        val ctx = requireContext()
        val sessionUser = TaskStore.get(ctx).getSession()?.second
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { ApiClient.get(ctx).getProfile() }
            }
            val json = result.getOrNull()
            val remote = json?.let { parseProfile(it) }
            // /api/profile uses the username as a display fallback when no nickname has
            // been chosen. Keep the raw /api/auth/me state so the source nickname-sheet
            // trigger is not accidentally cleared merely by opening this page.
            val user = if (
                remote != null &&
                sessionUser?.id == remote.id &&
                sessionUser.nickname.isNullOrBlank()
            ) {
                remote.copy(nickname = null)
            } else {
                remote
            }
            // 真本 e() 回调首行 a(this,false)：V=false，此后刷新不再展示加载动效
            firstLoad = false
            if (user != null) {
                // 真本 onSuccess：数据非空才 a(MyDetailInfo) 绑定（bindUser(null) 同样早退）
                currentUser = user
                bindUser(user)
            } else if (json == null || !json.optBoolean("success", false)) {
                // 真本 onError：j.e(错误信息)，信息为空不提示
                val message = json?.optString("error")?.takeIf(String::isNotBlank)
                    ?: result.exceptionOrNull()?.message?.takeIf(String::isNotBlank)
                if (!message.isNullOrBlank()) {
                    AppNotice.error(ctx, message)
                }
            }
            // 真本成功/失败回调均以 onPageLoadingCompleted(OK) 收尾：移除加载动效
            hidePageLoading()
        }
    }

    // === 页面加载动效（真本 BaseFragment2.getLoadingView / loadingViewCallback / onPageLoadingCompleted） ===

    private fun showPageLoading() {
        val root = view as? ViewGroup ?: return
        var loading = loadingView
        if (loading == null) {
            // 真本 BaseFragment2.getLoadingView()：View.inflate(host_loading_view_progress)
            // + setImageAssetsFolder + setAnimation + loop(true)
            loading = layoutInflater.inflate(R.layout.host_loading_view_progress, null)
            loading.findViewById<LottieAnimationView>(R.id.host_loading_view_progress_xmlottieview)
                ?.let { lottie ->
                    loadingLottieView = lottie
                    lottie.setImageAssetsFolder("lottie/host_loading/")
                    lottie.setAnimation("lottie/host_loading/loading.json")
                    lottie.repeatCount = LottieDrawable.INFINITE
                }
            loadingView = loading
        }
        // 真本 onPageLoadingCompleted(LOADING)：无父容器 addLoadStateView 居中添加，已挂载 bringToFront
        if (loading.parent == null) {
            val lp = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }
            root.addView(loading, lp)
        } else {
            loading.bringToFront()
        }
        loading.visibility = View.VISIBLE
        // 真本 loadingViewCallback(SHOW)：setProgress(0) + playAnimation
        loadingLottieView?.let { lottie ->
            lottie.progress = 0f
            lottie.playAnimation()
        }
    }

    private fun hidePageLoading() {
        val loading = loadingView ?: return
        // 真本 onPageLoadingCompleted(OK)：仅视图仍挂载时移除并 loadingViewCallback(HIDE)->cancelAnimation
        val parent = loading.parent as? ViewGroup
        if (parent != null) {
            parent.removeView(loading)
            loadingLottieView?.cancelAnimation()
        }
    }

    // === onClick 分发（真本 onClick(View) 逐 id 还原） ===

    override fun onClick(v: View) {
        when (v.id) {
            R.id.main_iv_avatar, R.id.main_iv_edit_avatar -> pickAvatar()
            R.id.main_tv_edit_bg, R.id.main_iv_top_bg -> pickBackground()
            R.id.main_rl_modify_username -> openUsernameEditor()
            R.id.main_rl_modify_nickname -> openNicknameEditor()
            R.id.main_rl_modify_label -> openSignatureEditor()
            R.id.main_rl_modify_brief -> openBriefEditor()
            R.id.main_rl_modify_sex -> openGenderEditor()
            R.id.main_rl_modify_birth_date -> openBirthdayEditor()
            R.id.main_rl_modify_region -> openRegionEditor()
            R.id.main_rl_verify_layout -> openVerify()
            R.id.main_rl_privacy -> openPrivacy()
            R.id.main_tv_weixin -> syncWeixin()
            R.id.main_tv_profile_save -> saveProfile()
            R.id.main_tv_profile_cancel -> cancelProfile()
        }
    }

    // === 子页面入口（接口对齐真本 i()/k()/l() 等） ===

    private fun pickAvatar() {
        pickAvatar.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                .build(),
        )
    }

    private fun pickBackground() {
        pickBackground.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                .build(),
        )
    }

    private fun openNicknameEditor() {
        val user = currentUser
        val content = user?.nickname.orEmpty()
        val verified = false // 梦枕无 verifyType 字段，按未认证处理
        EditPersonalInfoFragment.newInstance(content, leftModifyCount = 1)
            .show(childFragmentManager, "edit_nickname")
    }

    private fun openUsernameEditor() {
        val user = currentUser ?: return
        EditPersonalInfoFragment.newUsernameInstance(
            initialContent = user.username,
            leftModifyCount = user.usernameChangeCount ?: 1,
        ).show(childFragmentManager, "edit_username")
    }

    /** 梦枕无对应后端/组件时，给诚实提示而非静默死点击（不伪造子页面）。 */
    private fun showUnavailable(feature: String) {
        AppNotice.info(requireContext(), "${feature}功能暂未开放")
    }

    private fun openSignatureEditor() {
        // 梦枕扩展：标签行改为个性签名（signature）编辑。
        // 真本 j() 打开 MyLabelDialogFragment（标签）；梦枕 UserInfo 无 label 字段，但有独立的 signature 字段，
        // 复用 EditPersonalInfoFragment 的 type=4 签名编辑模式，真正可编辑并保存。
        EditPersonalInfoFragment.newSignatureInstance(currentUser?.signature)
            .show(childFragmentManager, "edit_signature")
    }

    private fun openBriefEditor() {
        EditPersonalInfoFragment.newBriefInstance(currentUser?.bio)
            .show(childFragmentManager, "edit_brief")
    }

    private fun openGenderEditor() {
        val ctx = requireContext()
        val user = currentUser
        val secret = ProfileVisibilityStore.isSecret(user?.gender)
        viewLifecycleOwner.lifecycleScope.launch {
            // 取消「保密」要回退到开启保密前勾选的那一项，但保密会把 gender 覆写成 'secret'，
            // 真值只能另存：优先服务端下发的 last_gender，线上还没部署到带该列的版本时
            // 退回本机镜像（见 ProfileVisibilityStore.KEY_LAST_GENDER 注释）。
            // 真本同样是「保留真值 + 单独开关」（MyDetailInfo.publicGender），
            // 这里只是把真值挪到了 last_gender。
            val fallback = user?.lastGender ?: ProfileVisibilityStore.lastGender(ctx)
            GenderSelectDialog.show(
                requireActivity(),
                // 保密状态下列表勾中的正是这个回退值；两边都拿不到时才是 null
                // —— 没有任何可回退的项，此时取消保密也不写值，用户需重选一次。
                currentGender = if (secret) fallback else user?.gender,
                initialHideGender = secret,
                onSelected = { gender, hide ->
                    // 「保密」= 往 gender 写 Web 端同款字面量 "secret"（跨端、跨设备一致）。
                    // 绝不把 gender 清空 —— 清空 = 删数据且不可还原。
                    // 不勾保密时 gender 即用户选中的项：既可能是他新选的，也可能是这个回退值，
                    // 两种都由 saveField 写回并同步本机镜像。
                    val value = if (hide) ProfileVisibilityStore.GENDER_SECRET else gender
                    if (value == null) {
                        bindGender(currentUser?.gender)
                    } else {
                        saveField(gender = value) { bindGender(currentUser?.gender) }
                    }
                },
                onDismiss = {},
            )
        }
    }

    private fun openBirthdayEditor() {
        val user = currentUser
        val (y, m, d) = parseBirthday(user?.birthday)
        viewLifecycleOwner.lifecycleScope.launch {
            val mirror = ProfileVisibilityStore.current(requireContext())
            val hidden = user?.hideBirthday ?: mirror.birthday
            // 真本 EditPersonalInfoFragment.a(year, month, day, hideBirthday) 的 month 是 **0 基**
            // （DatePicker / Calendar.MONTH 语义，见 ConstellationUtils 的参数说明），
            // 而 parseBirthday 给的是 1 基，所以这里必须减 1。
            // 曾经直接传 m，导致每次打开生日小窗都多算一个月（2 月显示成 3 月，
            // 一勾「保密」就把 2011-02-25 回写成 2011-04-25）。
            val fragment = if (y > 0 && m in 1..12 && d > 0) {
                EditPersonalInfoFragment.newInstance(y, m - 1, d, hidden)
            } else {
                EditPersonalInfoFragment.newInstance(0, 0, 0, hidden)
            }
            fragment.show(childFragmentManager, "edit_birthday")
        }
    }

    private fun openRegionEditor() {
        val user = currentUser
        viewLifecycleOwner.lifecycleScope.launch {
            val mirror = ProfileVisibilityStore.current(requireContext())
            val hidden = user?.hideRegion ?: mirror.region
            RegionSelectFragment.newInstance(hideRegion = hidden, currentRegion = user?.location)
                .show(childFragmentManager, "edit_region")
        }
    }

    private fun openVerify() {
        // 真本：若 userVerifyUrl 非空则打开 NativeHybridFragment；梦枕 UserInfo 无 userVerifyUrl 字段，
        // 且无通用 WebView/Hybrid 页承载，保留入口但暂空。补诚实提示消除静默死点击。
        showUnavailable("认证")
    }
    private fun openPrivacy() {
        parentFragmentManager.setFragmentResult(OPEN_PRIVACY_RESULT_KEY, bundleOf())
    }

    private fun syncWeixin() {
        WechatProfileSyncCoordinator.request(requireContext())?.let { message ->
            AppNotice.error(requireContext(), message)
        }
    }

    private fun observeWechatProfileSync() {
        viewLifecycleOwner.lifecycleScope.launch {
            WechatProfileSyncCoordinator.events.collect(::handleWechatProfileSyncEvent)
        }
    }

    private fun handleWechatProfileSyncEvent(event: WechatProfileSyncEvent) {
        WechatProfileSyncCoordinator.acknowledge(requireContext())
        when (event) {
            is WechatProfileSyncEvent.Success -> applyWechatProfile(event.profile)
            is WechatProfileSyncEvent.Error -> AppNotice.error(requireContext(), event.message)
        }
    }

    private fun applyWechatProfile(profile: WechatProfileDraft) {
        if (applyingWechatProfile) return
        applyingWechatProfile = true
        val ctx = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val previous = currentUser
                    ?: TaskStore.get(ctx).getSession()?.second
                    ?: UserInfo()
                val mirror = ProfileVisibilityStore.current(ctx)
                val nickname = profile.nickname?.trim()?.takeIf(String::isNotBlank)
                val avatarUrl = profile.avatarUrl?.trim()?.takeIf(String::isNotBlank)
                // A non-empty WeChat value is authoritative, including when
                // the local profile previously used "secret".
                val gender = profile.gender?.trim()?.takeIf(String::isNotBlank)
                val location = profile.location?.trim()?.takeIf(String::isNotBlank)
                    ?.takeUnless { previous.hideRegion == true || mirror.region }
                val response = withContext(Dispatchers.IO) {
                    ApiClient.get(ctx).updateProfile(
                        nickname = nickname,
                        avatarUrl = avatarUrl,
                        gender = gender,
                        location = location,
                    )
                }
                if (!response.optBoolean("success")) {
                    error(response.optString("error", "微信资料同步失败，请重试"))
                }

                val remote = parseProfile(response)
                val updated = (remote ?: previous).copy(
                    // The PUT response supplies a username fallback for nickname;
                    // don't turn a missing WeChat nickname into that fallback.
                    nickname = if (nickname != null) {
                        remote?.nickname?.takeIf(String::isNotBlank) ?: nickname
                    } else {
                        previous.nickname
                    },
                    avatarUrl = if (avatarUrl != null) {
                        remote?.avatarUrl?.takeIf(String::isNotBlank) ?: avatarUrl
                    } else {
                        previous.avatarUrl
                    },
                    gender = if (gender != null) remote?.gender ?: gender else previous.gender,
                    location = if (location != null) {
                        remote?.location?.takeIf(String::isNotBlank) ?: location
                    } else {
                        previous.location
                    },
                    email = remote?.email?.takeIf(String::isNotBlank) ?: previous.email,
                    backgroundUrl = remote?.backgroundUrl ?: previous.backgroundUrl,
                    mobile = remote?.mobile ?: previous.mobile,
                    hideBirthday = remote?.hideBirthday ?: previous.hideBirthday,
                    hideRegion = remote?.hideRegion ?: previous.hideRegion,
                    lastGender = remote?.lastGender ?: previous.lastGender,
                )
                persistSession(updated)
                currentUser = updated
                bindUser(updated)
                AppNotice.success(
                    ctx,
                    if (nickname == null && avatarUrl == null && gender == null && location == null) {
                        "微信未返回新的资料，已保留原内容"
                    } else {
                        "微信资料已同步"
                    },
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppNotice.error(
                    ctx,
                    error.message?.takeIf(String::isNotBlank)
                        ?: "微信资料同步失败，请重试",
                )
            } finally {
                applyingWechatProfile = false
            }
        }
    }

    private fun saveProfile() {
        // 梦枕各字段在进入子编辑页时即已通过 updateProfile 落库（saveField），
        // 底部「保存」即提交并退出资料编辑页。
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    private fun cancelProfile() {
        // 「取消」与「保存」语义一致：梦枕无草稿缓冲，退出即结束编辑。
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    // === 数据绑定（真本 a(MyDetailInfo) 核心逻辑逐块还原） ===

    private fun bindUser(user: UserInfo?) {
        if (user == null) return

        bindTopBackground(user)
        bindUsername(user)
        bindNickname(user)
        bindSignature(user)
        bindBrief(user)
        // 生日 / 地区的可见性：优先用服务端字段（hide_birthday / hide_region，与 Web 端共用），
        // 服务端未下发时（线上还没部署到该版本）退回本机镜像，否则重进页面状态会丢
        viewLifecycleOwner.lifecycleScope.launch {
            val mirror = ProfileVisibilityStore.current(requireContext())
            bindBirthday(user, hidden = user.hideBirthday ?: mirror.birthday)
            bindRegion(user, hidden = user.hideRegion ?: mirror.region)
            // 服务端下发的性别若为真值，顺手记进本机镜像，
            // 这样即使用户是在网页选的性别，本机也有可回退的值（保密覆写后要靠它）
            ProfileVisibilityStore.rememberGender(requireContext(), user.gender)
        }
        // 性别不在此列：它走后端 gender='secret'，值本身即状态
        bindGender(user.gender)
        bindVerify(user)
        bindAvatar(user)
        bindVerifyStatus(user)
        bindLabelRowVisibility(user)
    }

    /** 真本 f()：加载背景图（比例已在 XML 写死 375:240，真本按 backgroundSetting 切换的分支在梦枕无字段，不迁移空逻辑） */
    private fun bindTopBackground(user: UserInfo) {
        val bgUrl = user.backgroundUrl
        if (!bgUrl.isNullOrBlank()) {
            topBg.load(absoluteAvatarUrl(bgUrl))
        }
    }

    /** 真本 a(MyDetailInfo) 昵称块 */
    private fun bindUsername(user: UserInfo) {
        usernameEdit.visibility = View.VISIBLE
        usernameEdit.text = user.username.ifBlank { getString(R.string.xm_my_detail_7f120812) }
    }

    /** 真本 a(MyDetailInfo) 昵称块 */
    private fun bindNickname(user: UserInfo) {
        val nickname = user.nickname
        if (!nickname.isNullOrBlank()) {
            nicknameEdit.visibility = View.VISIBLE
            nicknameEdit.text = nickname
            nicknameGuide.visibility = View.GONE
        } else {
            nicknameGuide.visibility = View.GONE
            nicknameEdit.visibility = View.VISIBLE
            nicknameEdit.text = getString(R.string.xm_my_detail_7f120812)
        }
    }

    /** 梦枕扩展：原标签行改为个性签名（signature）块，绑定 UserInfo.signature */
    private fun bindSignature(user: UserInfo) {
        val sig = user.signature
        if (!sig.isNullOrBlank()) {
            labelEdit.visibility = View.VISIBLE
            labelEdit.text = sig
        } else {
            // 无签名时保留原页面的空值/提示；“保密”只用于显式隐私状态。
            labelEdit.visibility = View.VISIBLE
            labelEdit.text = ""
        }
    }

    /** 真本 a(MyDetailInfo) 简介块 */
    private fun bindBrief(user: UserInfo) {
        val bio = user.bio
        if (!bio.isNullOrBlank()) {
            briefEdit.visibility = View.VISIBLE
            briefGuide.visibility = View.GONE
            briefEdit.text = bio
        } else {
            briefEdit.visibility = View.VISIBLE
            briefGuide.visibility = View.GONE
            briefEdit.text = getString(R.string.xm_my_detail_7f120812)
        }
    }

    /** 真本 a(MyDetailInfo) 性别块 */
    private fun bindGender(gender: String?) {
        sexGuide.visibility = View.GONE
        sexEdit.visibility = View.VISIBLE
        // 编辑页这一行是「带标签的列表行」，保密时要显示文字（资料卡那边则是整段消失）。
        // 文案映射走共享实现（util/GenderLabels.kt），不在这里再写一份。
        sexEdit.text = when {
            isGenderSecret(gender) -> SECRET_TEXT
            else -> genderLabel(gender) ?: getString(R.string.xm_my_detail_7f120812)
        }
    }

    /** 真本 a(MyDetailInfo) 生日块 */
    private fun bindBirthday(user: UserInfo, hidden: Boolean) {
        val (y, m, d) = parseBirthday(user.birthday)
        birthGuide.visibility = View.GONE
        birthEdit.visibility = View.VISIBLE
        birthEdit.text = when {
            hidden -> SECRET_TEXT
            y > 0 && m in 1..12 && d in 1..31 -> String.format(Locale.CHINA, "%d-%d-%d", y, m, d)
            else -> getString(R.string.xm_my_detail_7f120812)
        }
    }

    /** 真本 a(MyDetailInfo) 地区块 */
    private fun bindRegion(user: UserInfo?, hidden: Boolean) {
        regionGuide.visibility = View.GONE
        val location = user?.location
        val display = when {
            hidden -> SECRET_TEXT
            !location.isNullOrBlank() -> formatLocation(location)
            else -> getString(R.string.xm_my_detail_7f120812)
        }
        regionEdit.visibility = View.VISIBLE
        regionEdit.text = display
    }

    /**
     * Web profile responses may include the complete hierarchy (for example
     * "地球/中国/云南/昆明/呈贡区").  The source profile page displays the
     * province and city only.  Keep the Android-native two-part format intact.
     */
    private fun formatLocation(location: String): String {
        val parts = location.split('/').map(String::trim).filter(String::isNotEmpty)
        val hasWebHierarchy = parts.any { it == "地球" } || parts.firstOrNull() == "中国"
        if (!hasWebHierarchy) return location
        val visible = parts.filterNot { it == "地球" || it == "中国" }
        return visible.take(2).joinToString(" ").ifBlank { location }
    }

    /** 真本 a(MyDetailInfo) 认证状态块（userVerifyState 0=未认证,1=?,2=审核中,3=已认证） */
    private fun bindVerify(user: UserInfo) {
        // 梦枕无认证状态，固定显示"未认证"
        verifyStatus.text = getString(R.string.xm_my_detail_7f120738) // "未认证"
    }

    /** 真本 a(MyDetailInfo) 头像加载 + 审核状态 */
    private fun bindAvatar(user: UserInfo) {
        val avatarUrl = user.avatarUrl
        if (!avatarUrl.isNullOrBlank()) {
            avatar.load(absoluteAvatarUrl(avatarUrl))
        } else {
            avatar.setImageResource(R.drawable.xm_my_detail_7f0808a9) // 真本默认头像
        }
        // 梦枕无头像审核状态字段，隐藏
        reviewAvatar.visibility = View.GONE
    }

    /** 真本 a(MyDetailInfo) 昵称 / 简介审核状态 */
    private fun bindVerifyStatus(user: UserInfo) {
        // 梦枕无审核状态字段，隐藏两个 "审核中" 标签
        nicknameVerifyStatus.visibility = View.GONE
        briefVerifyStatus.visibility = View.GONE
    }

    /** 真本 a(MyDetailInfo) 末段：标签行可见性（AnchorAbUtil） */
    private fun bindLabelRowVisibility(user: UserInfo) {
        // 真本：AnchorAbUtil 为 true 且存在标签时显示；梦枕默认显示。
        rowLabel.visibility = View.VISIBLE
    }

    // === 保存入口（统一走梦枕 updateProfile，对应真本各字段提交） ===

    private fun saveField(
        username: String? = null,
        nickname: String? = null,
        bio: String? = null,
        signature: String? = null,
        gender: String? = null,
        birthday: String? = null,
        location: String? = null,
        hideBirthday: Boolean? = null,
        hideRegion: Boolean? = null,
        onUi: () -> Unit,
    ) {
        val ctx = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.get(ctx).updateProfile(
                        username = username,
                        nickname = nickname,
                        bio = bio,
                        signature = signature,
                        gender = gender,
                        birthday = birthday,
                        location = location,
                        hideBirthday = hideBirthday,
                        hideRegion = hideRegion,
                    )
                }
                if (!response.optBoolean("success")) {
                    error(response.optString("error", "资料更新失败"))
                }

                val previous = currentUser ?: UserInfo()
                val submitted = previous.copy(
                    username = username ?: previous.username,
                    nickname = nickname ?: previous.nickname,
                    bio = bio ?: previous.bio,
                    signature = signature ?: previous.signature,
                    gender = gender ?: previous.gender,
                    birthday = birthday ?: previous.birthday,
                    location = location ?: previous.location,
                    hideBirthday = hideBirthday ?: previous.hideBirthday,
                    hideRegion = hideRegion ?: previous.hideRegion,
                )
                val remote = parseProfile(response)
                val updated = (remote ?: submitted).copy(
                    username = if (username == null) previous.username else remote?.username ?: username,
                    // A response to an unrelated field update contains the same username
                    // fallback. Only a submitted nickname is allowed to complete the gate.
                    nickname = if (nickname == null) previous.nickname else remote?.nickname ?: nickname,
                    email = remote?.email?.takeIf(String::isNotBlank) ?: previous.email,
                    backgroundUrl = remote?.backgroundUrl ?: previous.backgroundUrl,
                    mobile = remote?.mobile ?: previous.mobile,
                    // 「保密」前的真值：服务端自动维护的那份优先；线上还没部署到该列时
                    // （remote 不带 last_gender）用刚写进去的真值兜底，再不济保留原值。
                    lastGender = remote?.lastGender
                        ?: gender?.takeIf { !ProfileVisibilityStore.isSecret(it) }
                        ?: previous.lastGender,
                )
                persistSession(updated)
                currentUser = updated
                // 写入真值时同步本机镜像（取消保密时的回退值）；null / 'secret' 会被忽略，
                // 所以这里无条件调用。
                ProfileVisibilityStore.rememberGender(ctx, gender)
                onUi()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppNotice.error(
                    ctx,
                    error.message?.takeIf(String::isNotBlank) ?: "资料更新失败",
                )
            }
        }
    }

    private fun persistSession(user: UserInfo) {
        val store = TaskStore.get(requireContext())
        store.getSession()?.first?.let { token -> store.saveUserSession(token, user) }
    }

    // === 子页面回传处理 ===

    private fun onEditPersonalInfoResult(bundle: Bundle) {
        // 数据尚未加载完成时忽略回传（后续 bindUser 会刷新），避免 currentUser 为 null 触发 NPE 闪退
        if (currentUser == null) return
        when {
            bundle.containsKey(EditPersonalInfoFragment.RESULT_USERNAME) -> {
                val name = bundle.getString(EditPersonalInfoFragment.RESULT_USERNAME) ?: return
                saveField(username = name) { bindUsername(currentUser!!) }
            }
            bundle.containsKey(EditPersonalInfoFragment.RESULT_NICKNAME) -> {
                val name = bundle.getString(EditPersonalInfoFragment.RESULT_NICKNAME) ?: return
                saveField(nickname = name) { bindNickname(currentUser!!) }
            }
            bundle.containsKey(EditPersonalInfoFragment.RESULT_BRIEF) -> {
                val brief = bundle.getString(EditPersonalInfoFragment.RESULT_BRIEF) ?: return
                saveField(bio = brief) { bindBrief(currentUser!!) }
            }
            bundle.containsKey(EditPersonalInfoFragment.RESULT_SIGNATURE) -> {
                val sig = bundle.getString(EditPersonalInfoFragment.RESULT_SIGNATURE) ?: return
                saveField(signature = sig) { bindSignature(currentUser!!) }
            }
            bundle.containsKey(EditPersonalInfoFragment.RESULT_BIRTHDAY_YEAR) -> {
                val y = bundle.getInt(EditPersonalInfoFragment.RESULT_BIRTHDAY_YEAR)
                val m = bundle.getInt(EditPersonalInfoFragment.RESULT_BIRTHDAY_MONTH)
                val d = bundle.getInt(EditPersonalInfoFragment.RESULT_BIRTHDAY_DAY)
                // 回传里本就带 hide，此前漏读 →「保密生日」勾了等于没勾
                val hide = bundle.getBoolean(EditPersonalInfoFragment.RESULT_BIRTHDAY_HIDE, false)
                val text = String.format(Locale.CHINA, "%d-%d-%d", y, m + 1, d)
                // 两边都写：后端字段（权威，与 Web 端共用）+ 本机镜像（部署落后时兜底）
                viewLifecycleOwner.lifecycleScope.launch {
                    ProfileVisibilityStore.setHidden(requireContext(), Field.BIRTHDAY, hide)
                }
                saveField(birthday = text, hideBirthday = hide) {
                    bindBirthday(currentUser!!, hidden = hide)
                }
            }
        }
    }

    // === 头像 / 背景上传（复用梦枕既有真实实现） ===

    private fun uploadAvatar(uri: Uri) {
        val ctx = requireContext()
        val mime = ctx.contentResolver.getType(uri) ?: "image/jpeg"
        viewLifecycleOwner.lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) { persistProfileFile(ctx, uri, "avatar") }
            if (file == null) {
                AppNotice.error(ctx, "头像读取失败，请重试")
                return@launch
            }
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.get(ctx).uploadAvatar(file, mime)
                }
                val url = response.optString("avatar_url")
                    .ifBlank { response.optJSONObject("data")?.optString("avatar_url").orEmpty() }
                    .ifBlank { null }
                if (!response.optBoolean("success") || url == null) {
                    error(response.optString("error").ifBlank { "头像上传失败，请重试" })
                }

                val updated = (currentUser ?: UserInfo()).copy(avatarUrl = url)
                persistSession(updated)
                currentUser = updated
                avatar.load(absoluteAvatarUrl(url))
                AppNotice.success(ctx, "头像已更新")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppNotice.error(
                    ctx,
                    error.message?.takeIf(String::isNotBlank)
                        ?: "头像上传失败，请重试",
                )
            } finally {
                file.delete()
            }
        }
    }

    private fun uploadBackground(uri: Uri) {
        val ctx = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            val updated = withContext(Dispatchers.IO) {
                uploadSelectedProfileBackground(ctx, uri, currentUser)
            }
            if (updated != null) {
                currentUser = updated
                withContext(Dispatchers.Main) {
                    updated.backgroundUrl?.let { topBg.load(absoluteAvatarUrl(it)) }
                }
            }
        }
    }

    // === 工具 ===

    private fun parseBirthday(text: String?): Triple<Int, Int, Int> {
        if (!text.isNullOrBlank()) {
            val m = Regex("(\\d{4})-(\\d{1,2})-(\\d{1,2})").matchEntire(text.trim())
            if (m != null) {
                val y = m.groupValues[1].toIntOrNull() ?: 0
                val mo = m.groupValues[2].toIntOrNull() ?: 0
                val d = m.groupValues[3].toIntOrNull() ?: 0
                if (y > 0 && mo in 1..12 && d in 1..31) return Triple(y, mo, d)
            }
        }
        val today = Calendar.getInstance(Locale.CHINA)
        return Triple(0, 0, 0)
    }

    companion object {
        internal const val OPEN_PRIVACY_RESULT_KEY = "my_detail_open_privacy"

        /**
         * 字段处于保密状态时，资料行内显示的文案。
         *
         * 真本原版此处硬编码"不展示"（CFR 输出 `\u4e0d\u5c55\u793a`，性别/生日/地区三处一致）。
         * 梦枕统一改用 Web 端"保密"一词：两端同义、共用同一个后端字段
         * （性别 `gender='secret'`，生日 `hide_birthday`，地区 `hide_region`），
         * 用词一致可避免用户以为两端是两回事。属刻意的产品用词调整，非迁移错误。
         */
        private const val SECRET_TEXT = "保密"
    }
}
