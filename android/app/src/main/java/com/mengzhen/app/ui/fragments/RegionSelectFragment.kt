package com.mengzhen.app.ui.fragments

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.mengzhen.app.R
import com.mengzhen.app.ui.screens.XimalayaTitleAction
import com.mengzhen.app.ui.screens.installXimalayaTitleBar
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * 喜马拉雅 Android 9.5.4.7 RegionSelectFragment 直接迁移 — 地区编辑页。
 *
 * 源码对照：
 * - com.ximalaya.ting.android.main.fragment.myspace.child.RegionSelectFragment.java
 * - res/layout/host_fra_list_2.xml（容器，RefreshLoadMoreListView 已禁用刷新 → 适配为 ListView）
 * - res/layout/main_item_city.xml（列表项）
 * - res/layout/main_v_switch_info.xml（"不展示地区"开关头，复用已迁移布局）
 * - assets/province_cities.json（省市数据，原版用 Gson 解析，此处改用 org.json）
 *
 * 行为（对标原版 initUi / onItemClick / onBackPressed / b()）：
 * - 省份态：显示省份列表（含"更多"箭头），点省份切到城市态
 * - 城市态：隐藏箭头，点城市 → 结果回调并关闭；返回键回到省份态
 * - 开关头："不展示地区" CheckBox（原版 switch 60），仅本地跟踪
 * - 标题栏：返回 | 编辑地区，背景 浅 #FFFFFF / 深 #121212（原版 setTitleBar）
 */
class RegionSelectFragment : DialogFragment() {

    private var provinces: Provinces? = null
    private var showingCities = false
    private var selectedProvince: Province? = null
    private var hideRegion = false

    private var listView: ListView? = null
    private var provinceAdapter: RegionAdapter? = null
    private var cityAdapter: RegionAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Light_NoTitleBar)
        hideRegion = arguments?.getBoolean(ARG_HIDE_REGION, false) ?: false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        // 原版 onBackPressed：城市态返回省份态，省份态关闭
        dialog.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (showingCities) {
                    showProvinces()
                } else {
                    dismiss()
                }
                true
            } else {
                false
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.main_fra_region_select, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 原版 setTitleBar：背景 浅 #FFFFFF / 深 #121212，仅返回键
        val titleBar = view.findViewById<android.widget.RelativeLayout>(R.id.main_title_bar)
        installXimalayaTitleBar(
            host = titleBar,
            title = getString(R.string.xm_personal_edit_region_title),
            left = XimalayaTitleAction.Back { dismiss() },
        )
        titleBar.setBackgroundColor(
            if (isDarkTheme()) Color.parseColor("#121212") else Color.WHITE,
        )

        listView = view.findViewById(R.id.host_listview1)
        provinceAdapter = RegionAdapter(requireContext(), citiesMode = false)
        cityAdapter = RegionAdapter(requireContext(), citiesMode = true)

        // 原版 b()：复用 main_v_switch_info 作"不展示地区"开关头
        val header = layoutInflater.inflate(R.layout.main_v_switch_info, listView, false)
        header.findViewById<TextView>(R.id.main_tv_switch_title).text =
            getString(R.string.xm_personal_edit_region_hide)
        header.findViewById<CheckBox>(R.id.main_cb_switch).apply {
            isChecked = hideRegion
            setOnCheckedChangeListener { _, checked ->
                this@RegionSelectFragment.hideRegion = checked
            }
        }
        listView?.addHeaderView(header)

        listView?.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val index = position - (listView?.headerViewsCount ?: 0)
            onItemSelected(index)
        }

        loadProvinces()
    }

    // === 原版 loadData() → AnonymousClass3 读取 assets/province_cities.json ===
    private fun loadProvinces() {
        val parsed = runCatching {
            val json = requireContext().assets.open("province_cities.json")
                .bufferedReader().use { it.readText() }
            parseProvinces(json)
        }.getOrNull()
        provinces = parsed
        showProvinces()
    }

    private fun parseProvinces(json: String): Provinces? = try {
        val root = JSONObject(json)
        val arr = root.optJSONArray("provinces") ?: JSONArray()
        val provinces = mutableListOf<Province>()
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            val citiesArr = p.optJSONArray("cities") ?: JSONArray()
            val cities = mutableListOf<City>()
            for (j in 0 until citiesArr.length()) {
                cities.add(City(city = citiesArr.getJSONObject(j).optString("city")))
            }
            provinces.add(Province(province = p.optString("province"), cities = cities))
        }
        Provinces(provinces)
    } catch (e: JSONException) {
        null
    }

    // 原版 onItemClick：省份态 → 城市态；城市态 → 结果回调并关闭
    private fun onItemSelected(index: Int) {
        if (showingCities) {
            val p = selectedProvince ?: return
            val city = p.cities?.getOrNull(index)?.city ?: return
            // 原版 sb 拼接 "省 市"；"海外"省份只取城市名（原版会拼成 "美国 美国"，此处取简洁结果）
            val region = if (p.province == "海外") city else "${p.province} $city".trim()
            deliverResult(region)
            dismiss()
        } else {
            val p = provinces?.provinces?.getOrNull(index) ?: return
            showCities(p)
        }
    }

    private fun showProvinces() {
        showingCities = false
        provinceAdapter?.items = provinces?.provinces ?: emptyList()
        listView?.adapter = provinceAdapter
    }

    private fun showCities(province: Province) {
        showingCities = true
        selectedProvince = province
        cityAdapter?.items = province.cities ?: emptyList()
        listView?.adapter = cityAdapter
    }

    private fun deliverResult(region: String) {
        setFragmentResult(
            RESULT_KEY,
            Bundle().apply {
                putString(RESULT_REGION, region)
                putBoolean(RESULT_HIDE_REGION, hideRegion)
            },
        )
    }

    private fun isDarkTheme(): Boolean =
        (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    companion object {
        const val ARG_HIDE_REGION = "hideRegion"
        const val RESULT_KEY = "region_select_result"
        const val RESULT_REGION = "region"
        const val RESULT_HIDE_REGION = "region_hide"

        /** 原版 RegionSelectFragment.a(boolean hideRegion) */
        fun newInstance(hideRegion: Boolean = false): RegionSelectFragment =
            RegionSelectFragment().apply {
                arguments = Bundle().apply { putBoolean(ARG_HIDE_REGION, hideRegion) }
            }
    }
}

/** 原版 com.ximalaya.ting.android.main.model.city.{Provinces,Province,City}（org.json 解析对应结构） */
internal data class Provinces(val provinces: List<Province>? = null)
internal data class Province(val province: String? = null, val cities: List<City>? = null)
internal data class City(val city: String? = null, val country: String? = null, val parent: String? = null)

/**
 * 原版 MyAdapter（HolderAdapter 子类）直接迁移：
 * - 首项隐藏顶部分隔线 main_divider1（原版 bindViewDatas i==0 → GONE）
 * - 城市态（citiesMode=true）隐藏"更多"箭头 main_ic_more（原版 f101883a → GONE）
 * - main_tv_city 显示省份/城市名
 */
private class RegionAdapter(
    private val context: Context,
    private val citiesMode: Boolean,
) : BaseAdapter() {

    var items: List<Any> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val v = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.main_item_city, parent, false)
        v.findViewById<View>(R.id.main_divider1).visibility =
            if (position == 0) View.GONE else View.VISIBLE
        v.findViewById<ImageView>(R.id.main_ic_more).visibility =
            if (citiesMode) View.GONE else View.VISIBLE
        v.findViewById<TextView>(R.id.main_tv_city).text = when (val item = items[position]) {
            is Province -> item.province.orEmpty()
            is City -> item.city.orEmpty()
            else -> ""
        }
        return v
    }
}
