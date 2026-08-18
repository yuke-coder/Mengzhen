package com.mengzhen.app.ui.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mengzhen.app.R

/**
 * 喜马拉雅 Android 9.5.4.7 性别选择 MenuDialog 直接迁移 — 非仿写。
 *
 * 源码文件对照：
 * - com.ximalaya.ting.android.main.dialog.c  (性别选择 MenuDialog 子类，classes4.dex)
 * - com.ximalaya.ting.android.framework.view.dialog.MenuDialog  (父类，classes16.dex)
 * - com.ximalaya.ting.android.framework.view.dialog.d  (祖父类，仅做主线程检查)
 *
 * 迁移要点：
 * - 布局：main_dialog_select_sex.xml + main_dialog_menu_item.xml 整文件迁移
 * - 行为：标题"修改性别" + "男/女" 两项 ListView + "不展示性别" CheckBox + 选中行红色勾
 * - 资源：颜色 (#FF4444 / #E8E8E8 / #333333 / #111111 / #EA6347) 与原版同值
 * - 回调：原版 a 接口 → Kotlin 函数式回调 (onSelected / onHideSwitchToggled)
 *
 * 原版 c.java 构造函数 `c(Activity activity, int i)` 中 i 表示当前选中索引：
 * - i=1 → 选中第 0 项 ("男")
 * - i=2 → 选中第 1 项 ("女")
 * - 其他 → 直接当索引使用
 *
 * Mengzhen 入参用 currentGender: String? ("male" / "female" / null)。
 */
class GenderSelectDialog(
    context: Context,
    private val currentGender: String?,
    private val initialHideGender: Boolean = false,
    private val onSelected: (gender: String, hideGender: Boolean) -> Unit,
    private val onDismiss: () -> Unit = {},
) : Dialog(context, R.style.XmGenderSelectDialog) {

    private var listView: ListView? = null
    private var titleView: TextView? = null
    private var hideSwitch: CheckBox? = null
    private var hideSwitchLabel: TextView? = null

    /** 当前选中项索引（0=男, 1=女），-1 表示无选中（"不展示性别"激活时） */
    private var selectedIndex: Int = -1

    /** "不展示性别" CheckBox 当前是否勾选 */
    private var hideGenderChecked: Boolean = initialHideGender

    /** 列表数据（原版 AnonymousClass3: add("男"); add("女");） */
    private val items: List<String> = listOf("男", "女")

    /** 当前 MenuAdapter（原版 f48308d，子类为 AnonymousClass4） */
    private var adapter: MenuAdapter? = null

    /** 选中标记图的 tint 颜色（原版 0x7f060d0f = #EA6347） */
    private val checkTint: Int by lazy {
        ContextCompat.getColor(context, R.color.xm_gender_select_check_tint)
    }

    /** 浅色模式项文字颜色（原版 0x7f060ac4 = #333333） */
    private val itemTextColor: Int by lazy {
        ContextCompat.getColor(context, R.color.xm_gender_select_switch_text)
    }

    init {
        // 原版 c(Activity, int i) 中 i 映射：
        // 1 → 0（男）, 2 → 1（女）, 其他 → 直接当索引
        selectedIndex = when (currentGender) {
            "male" -> 0
            "female" -> 1
            else -> -1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        // 原版框架弹窗统一 setGravity(17)=CENTER + addFlags(DIM_FLAG) + setDimAmount(0.5f)
        window?.setGravity(Gravity.CENTER)
        window?.setDimAmount(0.5f)
        setContentView(R.layout.main_dialog_select_sex)

        // 原版 c.onCreate 中若 h=true 会把 main_ll_layout 宽度设为 350dp，
        // c 的 h 默认 false，因此该路径不执行。

        listView = findViewById(R.id.main_ll_listview)
        titleView = findViewById<TextView>(R.id.main_title_tv).apply {
            // 原版硬编码：setText("修改性别")；XML 已通过 @string 设置，无需重复。
        }
        hideSwitch = findViewById<CheckBox>(R.id.main_cb_sex).apply {
            isChecked = hideGenderChecked
            setOnCheckedChangeListener { _, isChecked ->
                hideGenderChecked = isChecked
                adapter?.notifyDataSetChanged()
                // 原版 m.a(z) 仅在外部回调存在时触发；
                // Mengzhen 在用户点击列表项后才回调 onSelected，CheckBox 切换不立即关闭弹窗。
            }
        }
        hideSwitchLabel = findViewById(R.id.main_tv_switch_sex_title)

        // 原版 c.a(1)：f48311g = 1（影响 MenuDialog 父类中 ImageView 默认 src 选择），
        // 但 c 的 AnonymousClass4.a() 中立即覆盖 setImageResource(0x7f0828d4)，
        // 故对最终视觉无影响，迁移时无需保留。

        adapter = MenuAdapter()
        listView?.adapter = adapter
        listView?.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            // 原版 AnonymousClass2: m.a(position, c.this.l)
            val gender = if (position == 0) "male" else "female"
            onSelected(gender, hideGenderChecked)
            dismiss()
        }

        setOnDismissListener {
            onDismiss()
        }
    }

    /**
     * 原版 com.ximalaya.ting.android.framework.view.dialog.MenuDialog.MenuAdapter 直接迁移。
     *
     * 行为：
     * - getView 中 inflate framework_menu_dialog_item.xml（Mengzhen 命名：main_dialog_menu_item）
     * - 持有 b (TextView + ImageView) 复用结构
     * - 当前选中项显示 ImageView，否则 GONE
     * - 调用子类 AnonymousClass4.a() 设置默认图与 tint
     */
    private inner class MenuAdapter : BaseAdapter() {

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): String = items[position]

        override fun getItemId(position: Int): Long = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            val holder: ItemHolder
            if (convertView == null) {
                view = LayoutInflater.from(context).inflate(
                    R.layout.main_dialog_menu_item,
                    parent,
                    false,
                )
                holder = ItemHolder(
                    textView = view.findViewById(R.id.group_item),
                    imageView = view.findViewById(R.id.new_feature),
                )
                view.tag = holder
            } else {
                view = convertView
                holder = view.tag as ItemHolder
            }

            holder.textView.text = items[position]

            // 原版 MenuDialog.getView：if (i == this.i) VISIBLE else GONE
            holder.imageView.visibility = if (position == selectedIndex) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // 原版 c.AnonymousClass4.a(String, bVar)：
            // bVar.f48314a.setImageResource(0x7f0828d4)  → 已通过 XML android:src 设置
            // bVar.f48315b.setTextColor(#333333)         → 浅色模式（Mengzhen 默认）
            // bVar.f48314a.setColorFilter(#EA6347, SRC_ATOP)
            holder.textView.setTextColor(itemTextColor)
            holder.imageView.setColorFilter(checkTint, PorterDuff.Mode.SRC_ATOP)

            return view
        }
    }

    /** 原版 MenuDialog.b 持有结构（TextView + ImageView） */
    private data class ItemHolder(
        val textView: TextView,
        val imageView: ImageView,
    )

    companion object {
        private const val TAG = "GenderSelectDialog"

        /**
         * 便捷入口：从 Activity 弹出性别选择 Dialog。
         * 对应原版 c.a aVar 接口的 a(int, boolean) 与 a(boolean) 回调。
         */
        fun show(
            activity: Activity,
            currentGender: String?,
            initialHideGender: Boolean = false,
            onSelected: (gender: String, hideGender: Boolean) -> Unit,
            onDismiss: () -> Unit = {},
        ): GenderSelectDialog {
            return GenderSelectDialog(
                context = activity,
                currentGender = currentGender,
                initialHideGender = initialHideGender,
                onSelected = onSelected,
                onDismiss = onDismiss,
            ).also { it.show() }
        }
    }
}
