package com.mengzhen.app.ui.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.mengzhen.app.R
import com.mengzhen.app.ui.screens.XimalayaTitleAction
import com.mengzhen.app.ui.screens.installXimalayaTitleBar
import com.mengzhen.app.util.ConstellationUtils
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 喜马拉雅 9.5.4.7 EditPersonalInfoFragment 直接迁移 — 昵称(type=1)/生日(type=2) 编辑路径。
 *
 * 源码对照：
 * - com.ximalaya.ting.android.main.fragment.myspace.child.EditPersonalInfoFragment.java
 * - res/layout/main_fra_personal_edit.xml + main_v_switch_info.xml
 * - SpanUtils 链式调用（com.ximalaya.ting.android.host.util.common.SpanUtils）
 * - 星座算法 com.ximalaya.ting.android.main.util.ui.a.a(int, int)（见 ConstellationUtils）
 *
 * type=1 昵称路径：a() 方法 SpanUtils 链路（逐段对照）：
 *   .a("剩余修改次数：").a(13, true)           // 标签 13sp 粗体，默认色
 *   .a(count + "\n").a(#FF4444)               // 次数数字红色
 *   .a(strReplace)                            // 规则 1-6，默认色（灰）
 *   .a(" 平台审核规则 ").a(13, true)           // 链接 13sp 粗体
 *   .a(linkColor, false, clickListener)       // 链接着色 + 可点击
 *   .a(arrowDrawable, 2)                      // 箭头 ImageSpan
 *   .a(linkColor, false, clickListener)       // 箭头着色
 *   [条件] .a(resetTitle)                     // 规则 7 前缀，默认色
 *   [条件] .a(resetButton + " ").a(13, true)  // "立即重置 " 13sp 粗体
 *   [条件] .a(linkColor, false, clickListener)
 *   [条件] .a(arrowDrawable, 2)
 *   [条件] .a(linkColor, false, clickListener)
 *
 * type=2 生日路径：c() 方法 — DatePickerDialog（主题 0x7f13038c，min=1900-01-01，max=今天，
 * 标题"填写生日信息，当天会有神秘惊喜噢~"）；选择日期后更新生日/星座行并回调宿主保存。
 *
 * type=3 简介路径：initUi i==3 分支 — main_change_brief 输入框（maxLength 300）+
 * "还能输入X字/无法输入更多"计数（AnonymousClass13）+ main_tv_rule SpanUtils 链路
 * （"平台审核规则 " 链接色可点击 + 箭头，无 13sp 粗体段）+ 保存按钮联动
 * （len>0 且 ≠ 初始内容才 enabled；颜色仅按 len>0 切换 #ff4444/#bbbbbb）；
 * 示例引导区依赖远端配置 gryjjsl，为空时 GONE（Mengzhen 恒走该路径）；
 * 初始内容为 "mark_no_content" 时视为未填写。
 *
 * 深色模式：背景 #000000 / 输入文字 #CFCFCF / 规则文字 #888888，
 * 通过 values-night/ 资源自动切换；生日页标题栏背景 浅#FFFFFF / 深#121212（原版 setTitleBar type==2）。
 */
class EditPersonalInfoFragment : DialogFragment() {

    private var type: Int = TYPE_NICKNAME
    private var initialContent: String? = null
    private var leftModifyCount: Int = DEFAULT_LEFT_COUNT

    // type=2 生日字段（原版 s/t/u/A 对应 year/month/day/hideBirthday）
    private var year: Int = -1
    private var month: Int = -1
    private var day: Int = -1
    private var hideBirthday: Boolean = false

    private var inputEditText: EditText? = null
    private var clearButton: ImageView? = null
    private var wordCountView: TextView? = null
    private var hintView: TextView? = null
    private var saveButton: TextView? = null
    private var inputMethodManager: InputMethodManager? = null

    // type=3 简介视图（原版 k/r 字段）
    private var briefEditText: EditText? = null
    private var briefCountView: TextView? = null

    // type=2 生日视图
    private var birthdayText: TextView? = null
    private var constellationText: TextView? = null
    private var hideSwitch: CheckBox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Light_NoTitleBar)
        type = arguments?.getInt(ARG_TYPE, TYPE_NICKNAME) ?: TYPE_NICKNAME
        initialContent = arguments?.getString(ARG_CONTENT)
        leftModifyCount = arguments?.getInt(ARG_LEFT_COUNT, DEFAULT_LEFT_COUNT) ?: DEFAULT_LEFT_COUNT
        year = arguments?.getInt(ARG_YEAR, -1) ?: -1
        month = arguments?.getInt(ARG_MONTH, -1) ?: -1
        day = arguments?.getInt(ARG_DAY, -1) ?: -1
        hideBirthday = arguments?.getBoolean(ARG_HIDE_BIRTHDAY, false) ?: false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        // 原版 EditPersonalInfoFragment 为 Activity 内全屏 Fragment；此处 DialogFragment 需显式铺满全屏，
        // 否则 Theme_Light_NoTitleBar 下对话框窗口会收缩为 wrap-content 导致页面不显示。
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        // 原版仅昵称(type=1)/简介(type=3) 输入路径在 onReady 后弹软键盘；生日页无输入框，不拉键盘
        if (type != TYPE_BIRTHDAY) {
            dialog.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE,
            )
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.main_fra_personal_edit, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (type) {
            TYPE_BIRTHDAY -> setupBirthday(view)
            TYPE_BRIEF -> setupBrief(view)
            else -> setupNickname(view)
        }
    }

    // === type=1 昵称编辑路径（原版 initUi type==1 分支） ===
    private fun setupNickname(view: View) {
        view.findViewById<View>(R.id.main_change_nickname).visibility = View.VISIBLE
        inputEditText = view.findViewById(R.id.main_et_edit_nickname)
        clearButton = view.findViewById(R.id.main_clear_nickname)
        wordCountView = view.findViewById(R.id.main_tv_left_total)
        hintView = view.findViewById<TextView>(R.id.main_hint_vertified).apply {
            visibility = View.VISIBLE
            movementMethod = LinkMovementMethod.getInstance()
        }

        setupHintText()
        setupTitleBar(view)

        inputEditText?.setOnClickListener(::onEditTextClick)
        clearButton?.setOnClickListener { inputEditText?.setText("") }

        if (initialContent.isNullOrEmpty()) {
            wordCountView?.visibility = View.GONE
            clearButton?.visibility = View.GONE
        } else {
            inputEditText?.setText(initialContent)
            inputEditText?.setSelection(initialContent!!.length)
            updateWordCount(countNicknameChars(initialContent!!))
        }

        inputEditText?.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            addTextChangedListener(NicknameTextWatcher())
        }

        inputEditText?.post {
            inputMethodManager = requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputEditText?.let { inputMethodManager?.showSoftInput(it, 0) }
        }
    }

    // === type=2 生日编辑路径（原版 initUi type==2 分支 + c()） ===
    private fun setupBirthday(view: View) {
        val titleBar = view.findViewById<RelativeLayout>(R.id.main_title_bar)
        installXimalayaTitleBar(
            host = titleBar,
            title = getString(R.string.xm_personal_edit_birthday_title),
            left = XimalayaTitleAction.Back { dismiss() },
        )
        // 原版 setTitleBar type==2：标题栏背景 浅 #FFFFFF / 深 #121212（其余 type 用页面背景色）
        titleBar.setBackgroundColor(
            if (isDarkTheme()) Color.parseColor(BIRTHDAY_TITLE_BAR_DARK) else Color.WHITE,
        )

        // 生日行 + 星座行 + 不展示开关
        view.findViewById<LinearLayout>(R.id.main_change_birth_date).visibility = View.VISIBLE
        birthdayText = view.findViewById(R.id.main_tv_change_birth_date)
        birthdayText?.setOnClickListener { showDatePicker() }

        view.findViewById<LinearLayout>(R.id.main_change_constellation).visibility = View.VISIBLE
        constellationText = view.findViewById(R.id.main_tv_change_constellation)
        constellationText?.setOnClickListener { showDatePicker() }

        view.findViewById<View>(R.id.main_v_switch).visibility = View.VISIBLE
        hideSwitch = view.findViewById<CheckBox>(R.id.main_cb_switch).apply {
            isChecked = hideBirthday
            setOnCheckedChangeListener { _, checked -> this@EditPersonalInfoFragment.hideBirthday = checked }
        }

        if (year > 0 && month >= 0 && day > 0) {
            birthdayText?.text = "$year-${month + 1}-$day"
            val constellation = ConstellationUtils.calculate(month, day)
            constellationText?.text = constellation
        }

        // 原版 initUi：无有效生日（s<=0）时自动弹出 DatePicker
        if (year <= 0) {
            showDatePicker()
        }
    }

    // === type=3 简介编辑路径（原版 initUi i==3 分支） ===
    private fun setupBrief(view: View) {
        view.findViewById<View>(R.id.main_change_brief).visibility = View.VISIBLE
        briefEditText = view.findViewById(R.id.main_et_edit_brief)
        briefCountView = view.findViewById(R.id.main_left_word_count)

        setupBriefRuleText(view)
        setupTitleBar(view)

        // 原版 onClick id==main_et_edit_brief：focusable/requestFocus/showSoftInput
        briefEditText?.setOnClickListener(::onEditTextClick)

        // 原版：content 非空且 != "mark_no_content" 时回填并刷新剩余字数
        if (!initialContent.isNullOrEmpty() && initialContent != NO_CONTENT_MARK) {
            briefEditText?.apply {
                setText(initialContent)
                setSelection(initialContent!!.length)
            }
            briefCountView?.text = "还能输入${MAX_BRIEF_COUNT - initialContent!!.length}字"
        }

        briefEditText?.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            addTextChangedListener(BriefTextWatcher())
        }

        briefEditText?.post {
            inputMethodManager = requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            briefEditText?.let { inputMethodManager?.showSoftInput(it, 0) }
        }

        // 原版：configurecenter "gryjjsl" 为空 → 示例引导区 GONE（Mengzhen 无远端配置，恒为空）
        view.findViewById<View>(R.id.main_rl_brief_guide).visibility = View.GONE
    }

    // === 原版 initUi type==3：main_tv_rule SpanUtils 链路 ===
    // .a("平台审核规则 ").a(linkColor, false, D).a(arrow, 2).a(linkColor, false, D)
    // （与昵称路径不同：无 13sp 粗体段；箭头区域同样附带链接色与点击）
    private fun setupBriefRuleText(view: View) {
        val ruleView = view.findViewById<TextView>(R.id.main_tv_rule).apply {
            movementMethod = LinkMovementMethod.getInstance()
        }
        val ctx = requireContext()
        val linkColor = ContextCompat.getColor(ctx, R.color.xm_personal_edit_link_text)
        val builder = SpannableStringBuilder()
        val start = builder.length
        builder.append(getString(R.string.xm_personal_edit_audit_rules_link))
        builder.setSpan(
            ForegroundColorSpan(linkColor),
            start, builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        builder.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    Toast.makeText(ctx, "平台审核规则", Toast.LENGTH_SHORT).show()
                }
            },
            start, builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        val arrowStart = builder.length
        appendArrowSpan(builder, ctx, linkColor)
        builder.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    Toast.makeText(ctx, "平台审核规则", Toast.LENGTH_SHORT).show()
                }
            },
            arrowStart, builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        ruleView.text = builder
    }

    // === EditPersonalInfoFragment.c() — 生日 DatePickerDialog ===
    private fun showDatePicker() {
        if (!isAdded) return
        val minTime = Calendar.getInstance(Locale.getDefault()).apply {
            set(1900, 0, 0, 0, 0, 0)
        }.timeInMillis
        val maxTime = System.currentTimeMillis()
        if (year <= 0 || month < 0 || day < 0) {
            val today = Calendar.getInstance(Locale.CHINA).apply { time = Date() }
            year = today.get(Calendar.YEAR)
            month = today.get(Calendar.MONTH)
            day = today.get(Calendar.DAY_OF_MONTH)
        }
        val dialog = DatePickerDialog(
            requireContext(),
            R.style.XmDatePickerDialog,
            { _, y, m, d ->
                // 原版 AnonymousClass16.onDateSet：更新年月日 → 刷新生日/星座行 → e() 保存；
                // 页面不关闭，由用户按返回退出（返回路径 Back{dismiss()} 不再重复交付）
                year = y
                month = m
                day = d
                birthdayText?.text = "$year-${month + 1}-$day"
                constellationText?.text = ConstellationUtils.calculate(month, day)
                deliverBirthdayResult()
            },
            year,
            month,
            day,
        )
        dialog.datePicker.minDate = minTime
        dialog.datePicker.maxDate = maxTime
        dialog.setTitle(getString(R.string.xm_personal_edit_birthday_picker_title))
        dialog.show()
    }

    private fun deliverBirthdayResult() {
        setFragmentResult(
            RESULT_KEY,
            Bundle().apply {
                putInt(RESULT_BIRTHDAY_YEAR, year)
                putInt(RESULT_BIRTHDAY_MONTH, month)
                putInt(RESULT_BIRTHDAY_DAY, day)
                putString(RESULT_BIRTHDAY_CONSTELLATION, ConstellationUtils.calculate(month, day))
                putBoolean(RESULT_BIRTHDAY_HIDE, hideBirthday)
            },
        )
    }

    private fun isDarkTheme(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    override fun onDestroyView() {
        view?.windowToken?.let { token ->
            inputMethodManager?.hideSoftInputFromWindow(token, 0)
        }
        inputEditText = null
        clearButton = null
        wordCountView = null
        hintView = null
        saveButton = null
        briefEditText = null
        briefCountView = null
        super.onDestroyView()
    }

    // === EditPersonalInfoFragment.a(CharSequence) — 字符数计算 ===
    private fun countNicknameChars(text: CharSequence): Int {
        if (text.isEmpty()) return 0
        var count = 0
        for (i in text.indices) {
            val c = text[i]
            count += if (Build.VERSION.SDK_INT < 24) {
                if (isHanLegacy(c)) 2 else 1
            } else {
                if (Character.UnicodeScript.of(c.code) == Character.UnicodeScript.HAN) 2 else 1
            }
        }
        return count
    }

    private fun isHanLegacy(c: Char): Boolean =
        (c in '\u4E00'..'\u9FFF') ||
            (c in '\u3400'..'\u4DBF') ||
            (c in '\uF900'..'\uFAFF')

    // === EditPersonalInfoFragment.a(int) — 字数统计显示 ===
    private fun updateWordCount(count: Int) {
        val view = wordCountView ?: return
        val builder = SpannableStringBuilder()
        val countStr = count.toString()
        if (count > MAX_NICKNAME_COUNT) {
            builder.append(countStr)
            builder.setSpan(
                ForegroundColorSpan(Color.parseColor(OVER_LIMIT_COLOR_HEX)),
                0,
                countStr.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        } else {
            builder.append(countStr)
        }
        builder.append("/$MAX_NICKNAME_COUNT")
        view.text = builder
    }

    // === EditPersonalInfoFragment.a() — 规则文案 Spannable 构建 ===
    // 逐段对照原版 SpanUtils 链路，颜色/大小/点击区域严格一致。
    private fun setupHintText() {
        val view = hintView ?: return
        val ctx = requireContext()
        val linkColor = ContextCompat.getColor(ctx, R.color.xm_personal_edit_link_text)
        val countColor = ContextCompat.getColor(ctx, R.color.xm_personal_edit_count_highlight)
        val rulesText = getString(R.string.xm_personal_edit_nickname_modify_rules)
            .replace("\\n", "\n")

        val builder = SpannableStringBuilder()

        // "剩余修改次数：" — 13sp 粗体，默认色（TextView XML textColor）
        val labelStart = builder.length
        builder.append(getString(R.string.xm_personal_edit_nickname_modify_left))
        builder.setSpan(
            RelativeSizeSpan(LABEL_TEXT_SIZE_RATIO),
            labelStart, builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        builder.setSpan(
            StyleSpan(android.graphics.Typeface.BOLD),
            labelStart, builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        // count + "\n" — 红色 #FF4444
        val countStart = builder.length
        builder.append(leftModifyCount.toString()).append("\n")
        builder.setSpan(
            ForegroundColorSpan(countColor),
            countStart, builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        // 规则 1-6 — 默认色（灰色，来自 XML textColor）
        builder.append(rulesText)

        // " 平台审核规则 " — 13sp 粗体 + 链接色 + 可点击
        val auditStart = builder.length
        builder.append(getString(R.string.xm_personal_edit_audit_rules_link))
        applyLinkStyle(builder, auditStart, builder.length, linkColor) {
            Toast.makeText(ctx, "平台审核规则", Toast.LENGTH_SHORT).show()
        }

        // 箭头 ">" ImageSpan（原版 n.a(context, 0x7f0819cc, linkColor)）
        appendArrowSpan(builder, ctx, linkColor)

        // 规则 7：重置回初始化昵称（换行后显示）
        // 原版条件：resetTitle / resetButtonTitle / resetUrl 均非空时显示完整链接。
        builder.append("\n")
        builder.append(getString(R.string.xm_personal_edit_reset_title))
        val resetBtnStart = builder.length
        builder.append(getString(R.string.xm_personal_edit_reset_button)).append(" ")
        applyLinkStyle(builder, resetBtnStart, builder.length, linkColor) {
            Toast.makeText(ctx, "立即重置", Toast.LENGTH_SHORT).show()
        }
        appendArrowSpan(builder, ctx, linkColor)

        view.text = builder
    }

    /** 应用链接段样式：13sp 粗体 + 链接色 + 可点击 */
    private fun applyLinkStyle(
        builder: SpannableStringBuilder,
        start: Int,
        end: Int,
        linkColor: Int,
        onClick: () -> Unit,
    ) {
        builder.setSpan(
            RelativeSizeSpan(LABEL_TEXT_SIZE_RATIO),
            start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        builder.setSpan(
            StyleSpan(android.graphics.Typeface.BOLD),
            start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        builder.setSpan(
            ForegroundColorSpan(linkColor),
            start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        builder.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) = onClick()
            },
            start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }

    /** 追加箭头 ImageSpan（原版 0x7f0819cc chevron，tint 为链接色） */
    private fun appendArrowSpan(
        builder: SpannableStringBuilder,
        ctx: Context,
        tintColor: Int,
    ) {
        val drawable = ContextCompat.getDrawable(ctx, R.drawable.xm_personal_edit_arrow_right)
            ?: return
        val wrapped = DrawableCompat.wrap(drawable.mutate())
        DrawableCompat.setTint(wrapped, tintColor)
        val lineHeight = (12f * ctx.resources.displayMetrics.density).toInt()
        wrapped.setBounds(0, 0, lineHeight, lineHeight)
        val arrowStart = builder.length
        builder.append(" ")
        builder.setSpan(
            ImageSpan(wrapped, ImageSpan.ALIGN_BOTTOM),
            arrowStart, builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }

    // === EditPersonalInfoFragment.setTitleBar() ===
    // 标题与初始禁用色按 type 区分：昵称 disabled 0x7f06365e(#CCCCCC) / 简介 0x7f060c62(#BBBBBB)
    private fun setupTitleBar(root: View) {
        val titleBar = root.findViewById<RelativeLayout>(R.id.main_title_bar)
        val titleRes = when (type) {
            TYPE_BRIEF -> R.string.xm_personal_edit_brief_title
            else -> R.string.xm_personal_edit_nickname_title
        }
        installXimalayaTitleBar(
            host = titleBar,
            title = getString(titleRes),
            left = XimalayaTitleAction.Back { dismiss() },
            rightText = getString(R.string.xm_personal_edit_save),
            onRight = ::onSave,
        )
        saveButton = titleBar.findViewById(R.id.ximalaya_title_right)
        saveButton?.isEnabled = false
        val disabledRes = when (type) {
            TYPE_BRIEF -> R.color.xm_personal_edit_brief_save_disabled
            else -> R.color.xm_personal_edit_save_disabled
        }
        saveButton?.setTextColor(ContextCompat.getColor(requireContext(), disabledRes))
    }

    private fun onSave() {
        if (type == TYPE_BRIEF) {
            // 原版 e() type=3 → a(String)：输入空 → Toast"请输入正确的简介信息!"；
            // trim 后为空 → 提交空串；否则提交原文
            val raw = briefEditText?.text?.toString()
            if (raw == null || raw.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    R.string.xm_personal_edit_invalid_brief,
                    Toast.LENGTH_SHORT,
                ).show()
                return
            }
            val value = if (raw.trim().isEmpty()) "" else raw
            setFragmentResult(
                RESULT_KEY,
                Bundle().apply { putString(RESULT_BRIEF, value) },
            )
            dismiss()
            return
        }
        val editText = inputEditText ?: return
        val raw = editText.text?.toString().orEmpty()
        if (raw.isEmpty()) {
            Toast.makeText(requireContext(), R.string.xm_personal_edit_invalid_nickname, Toast.LENGTH_SHORT).show()
            return
        }
        if (raw.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.xm_personal_edit_nickname_empty, Toast.LENGTH_SHORT).show()
            return
        }
        setFragmentResult(
            RESULT_KEY,
            Bundle().apply { putString(RESULT_NICKNAME, raw) },
        )
        dismiss()
    }

    // 原版 onClick：昵称(2131372990)/简介(2131372989) 两个 EditText 同一处理
    // （focusable → requestFocus → showSoftInput）
    private fun onEditTextClick(view: View) {
        val editText = when (type) {
            TYPE_BRIEF -> briefEditText
            else -> inputEditText
        } ?: return
        editText.isFocusable = true
        editText.isFocusableInTouchMode = true
        editText.requestFocus()
        if (inputMethodManager == null) {
            inputMethodManager = requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        }
        inputMethodManager?.showSoftInput(editText, 0)
    }

    private inner class NicknameTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            clearButton?.visibility = visibility
            wordCountView?.visibility = visibility
        }

        override fun afterTextChanged(s: Editable?) {
            updateWordCount(countNicknameChars(s ?: ""))
            val enabled = !TextUtils.equals(s, initialContent)
            saveButton?.isEnabled = enabled
            val colorRes = if (enabled) R.color.xm_personal_edit_save_enabled
            else R.color.xm_personal_edit_save_disabled
            saveButton?.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        }
    }

    // === AnonymousClass13 — type=3 简介输入监听 ===
    // onTextChanged：>300 "无法输入更多"；>0 "还能输入X字"；空 ""
    // afterTextChanged：len>0 且 ≠ 初始内容才 enabled；颜色仅按 len>0 切换（原版行为）
    private inner class BriefTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            briefCountView?.text = when {
                s == null || s.length > MAX_BRIEF_COUNT -> "无法输入更多"
                s.isNotEmpty() -> "还能输入${MAX_BRIEF_COUNT - s.length}字"
                else -> ""
            }
        }

        override fun afterTextChanged(s: Editable?) {
            val text = s?.toString().orEmpty()
            saveButton?.isEnabled = text.isNotEmpty() && text != initialContent
            val colorRes = if (text.isNotEmpty()) R.color.xm_personal_edit_brief_save_enabled
            else R.color.xm_personal_edit_brief_save_disabled
            saveButton?.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        }
    }

    companion object {
        const val ARG_TYPE = "type"
        const val ARG_CONTENT = "content"
        const val ARG_LEFT_COUNT = "leftModifyCount"
        const val ARG_YEAR = "year"
        const val ARG_MONTH = "month"
        const val ARG_DAY = "day"
        const val ARG_HIDE_BIRTHDAY = "hideBirthday"
        const val RESULT_KEY = "edit_personal_info_result"
        const val RESULT_NICKNAME = "nickname"
        const val RESULT_BRIEF = "brief"
        const val RESULT_BIRTHDAY_YEAR = "birthday_year"
        const val RESULT_BIRTHDAY_MONTH = "birthday_month"
        const val RESULT_BIRTHDAY_DAY = "birthday_day"
        const val RESULT_BIRTHDAY_CONSTELLATION = "birthday_constellation"
        const val RESULT_BIRTHDAY_HIDE = "birthday_hide"

        private const val TYPE_NICKNAME = 1
        private const val TYPE_BIRTHDAY = 2
        private const val TYPE_BRIEF = 3
        private const val MAX_NICKNAME_COUNT = 20
        private const val MAX_BRIEF_COUNT = 300
        private const val NO_CONTENT_MARK = "mark_no_content"
        private const val LABEL_TEXT_SIZE_RATIO = 13f / 12f
        private const val OVER_LIMIT_COLOR_HEX = "#CE2424"
        private const val DEFAULT_LEFT_COUNT = 1
        // 原版 setTitleBar type==2 深色标题栏背景 -15592942 = #FF121212
        private const val BIRTHDAY_TITLE_BAR_DARK = "#121212"

        fun newInstance(initialContent: String?, leftModifyCount: Int = DEFAULT_LEFT_COUNT): EditPersonalInfoFragment =
            EditPersonalInfoFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TYPE, TYPE_NICKNAME)
                    putString(ARG_CONTENT, initialContent.orEmpty())
                    putInt(ARG_LEFT_COUNT, leftModifyCount)
                }
            }

        /** 原版 EditPersonalInfoFragment.a(int type, String content) — type=3 简介路径 */
        fun newBriefInstance(initialContent: String?): EditPersonalInfoFragment =
            EditPersonalInfoFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TYPE, TYPE_BRIEF)
                    putString(ARG_CONTENT, initialContent.orEmpty())
                }
            }

        /** 原版 EditPersonalInfoFragment.a(int year, int month, int day, boolean hideBirthday) */
        fun newInstance(year: Int, month: Int, day: Int, hideBirthday: Boolean): EditPersonalInfoFragment =
            EditPersonalInfoFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TYPE, TYPE_BIRTHDAY)
                    putInt(ARG_YEAR, year)
                    putInt(ARG_MONTH, month)
                    putInt(ARG_DAY, day)
                    putBoolean(ARG_HIDE_BIRTHDAY, hideBirthday)
                }
            }
    }
}
