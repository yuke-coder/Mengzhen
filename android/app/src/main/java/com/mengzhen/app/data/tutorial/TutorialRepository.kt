package com.mengzhen.app.data.tutorial

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.mengzhen.app.compat.VendorCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.tutorialDataStore by preferencesDataStore(name = "tutorial_progress")

/**
 * 权限教程内容仓库 —— 数据与 UI 分离，文案全部来自 permission-tutorial-content.md v2。
 *
 * 批次一：框架 + 华为/荣耀全部 7 项 + 通用兜底 4 项。
 * 批次二：VIVO 5 项 + 小米 5 项 + OPPO 6 项。
 * 批次四：三星 5 项（含后台数据随联网控制自动完成）——全品牌内容齐备。
 */
object TutorialRepository {

    // ==================== 品牌映射 ====================

    fun mapBrand(vendorBrand: VendorCompat.Brand): TutorialBrand = when (vendorBrand) {
        VendorCompat.Brand.HUAWEI, VendorCompat.Brand.HONOR -> TutorialBrand.HUAWEI_HONOR
        VendorCompat.Brand.VIVO -> TutorialBrand.VIVO
        VendorCompat.Brand.XIAOMI -> TutorialBrand.XIAOMI
        VendorCompat.Brand.OPPO -> TutorialBrand.OPPO
        VendorCompat.Brand.SAMSUNG -> TutorialBrand.SAMSUNG
        VendorCompat.Brand.OTHER -> TutorialBrand.GENERIC
    }

    /** 品牌展示列表（权重顺序），供切换品牌 BottomSheet 使用 */
    val brandsByWeight: List<TutorialBrand> = TutorialBrand.entries.toList()

    // ==================== 品牌矩阵（内容文档 §一：各权限项的展示品牌） ====================

    private val brandMatrix: Map<PermissionKey, Set<TutorialBrand>> = mapOf(
        PermissionKey.BATTERY_OPTIMIZATION to TutorialBrand.entries.toSet(),
        PermissionKey.BACKGROUND_RUNNING to TutorialBrand.entries.toSet(),
        PermissionKey.AUTO_START to TutorialBrand.entries.toSet(),
        PermissionKey.CLOSE_POWER_SAVE to TutorialBrand.entries.toSet(),
        PermissionKey.KEEP_NET_CONNECTION to setOf(TutorialBrand.HUAWEI_HONOR),
        PermissionKey.NETWORK_CONTROL to setOf(
            TutorialBrand.HUAWEI_HONOR, TutorialBrand.XIAOMI, TutorialBrand.OPPO,
            TutorialBrand.VIVO, TutorialBrand.SAMSUNG
        ),
        PermissionKey.BACKGROUND_DATA to setOf(
            TutorialBrand.XIAOMI, TutorialBrand.OPPO, TutorialBrand.SAMSUNG
        ),
        PermissionKey.SMART_DATA_SAVER to setOf(
            TutorialBrand.HUAWEI_HONOR, TutorialBrand.OPPO, TutorialBrand.VIVO
        ),
    )

    // ==================== 内容查询 ====================

    /** 精确查找品牌专属内容，缺失时 fallback 到通用兜底内容 */
    fun getContent(brand: TutorialBrand, permission: PermissionKey): TutorialContent? {
        return contents[brand to permission] ?: contents[TutorialBrand.GENERIC to permission]
    }

    /**
     * 该品牌在列表页应显示的权限项：
     * 品牌矩阵包含该品牌 ∩ 有教程内容（含通用 fallback）。无内容组合在列表层隐藏。
     */
    fun getVisiblePermissions(brand: TutorialBrand): List<PermissionKey> =
        PermissionKey.entries.filter { key ->
            brandMatrix[key]?.contains(brand) == true && getContent(brand, key) != null
        }

    /**
     * 「随其他项同入口完成」映射：完成源项教程后本项自动打勾置灰，无需重复设置。
     * - 华为「自启动」随「后台运行策略」小卡 B（允许自启动已包含）
     * - 三星「后台获取数据」随「联网控制」步骤 3（允许后台使用数据已包含，修订记录 #11 合并去重）
     */
    fun autoCompletedBy(brand: TutorialBrand, permission: PermissionKey): PermissionKey? = when {
        brand == TutorialBrand.HUAWEI_HONOR && permission == PermissionKey.AUTO_START ->
            PermissionKey.BACKGROUND_RUNNING
        brand == TutorialBrand.SAMSUNG && permission == PermissionKey.BACKGROUND_DATA ->
            PermissionKey.NETWORK_CONTROL
        else -> null
    }

    // ==================== 勾选进度持久化（DataStore） ====================

    private fun stepKey(brand: TutorialBrand, permission: PermissionKey, cardId: String, stepIndex: Int) =
        booleanPreferencesKey("tutorial_progress_${brand.name}_${permission.prefKey}_${cardId}_$stepIndex")

    private fun doneKey(brand: TutorialBrand, permission: PermissionKey) =
        booleanPreferencesKey("tutorial_done_${brand.name}_${permission.prefKey}")

    fun stepCheckedFlow(
        context: Context, brand: TutorialBrand, permission: PermissionKey,
        cardId: String, stepIndex: Int,
    ): Flow<Boolean> = context.tutorialDataStore.data.map { it[stepKey(brand, permission, cardId, stepIndex)] == true }

    fun tutorialDoneFlow(context: Context, brand: TutorialBrand, permission: PermissionKey): Flow<Boolean> =
        context.tutorialDataStore.data.map { it[doneKey(brand, permission)] == true }

    suspend fun setStepChecked(
        context: Context, brand: TutorialBrand, permission: PermissionKey,
        cardId: String, stepIndex: Int, checked: Boolean,
    ) {
        context.tutorialDataStore.edit { it[stepKey(brand, permission, cardId, stepIndex)] = checked }
        // 联动更新整个教程的完成标记
        val content = getContent(brand, permission) ?: return
        val prefs = context.tutorialDataStore.data.first()
        fun cardDone(card: TutorialCard) =
            card.steps.all { step -> prefs[stepKey(brand, permission, card.id, step.index)] == true }
        val allDone = when (content.completionRule) {
            // ALL：所有卡所有步骤勾选（如华为后台策略 3 卡缺一不可）
            CompletionRule.ALL -> content.cards.all(::cardDone)
            // ANY：任一卡全部勾选即完成（方案/路径二选一教程，如智能省流量 A/B、自启动双路径）
            CompletionRule.ANY -> content.cards.any(::cardDone)
        }
        val wasDone = prefs[doneKey(brand, permission)] == true
        context.tutorialDataStore.edit { it[doneKey(brand, permission)] = allDone }
        // 漏斗埋点：仅未完成→已完成翻转时计一次（批次四基线）
        if (allDone && !wasDone) TutorialFunnel.trackDone(context, brand, permission)
    }

    suspend fun isTutorialDone(context: Context, brand: TutorialBrand, permission: PermissionKey): Boolean =
        context.tutorialDataStore.data.first()[doneKey(brand, permission)] == true

    suspend fun isStepChecked(
        context: Context, brand: TutorialBrand, permission: PermissionKey,
        cardId: String, stepIndex: Int,
    ): Boolean =
        context.tutorialDataStore.data.first()[stepKey(brand, permission, cardId, stepIndex)] == true

    // ==================== 教程内容表 ====================

    private val contents: Map<Pair<TutorialBrand, PermissionKey>, TutorialContent> = buildMap {

        // ========== 华为 / 荣耀（HarmonyOS 4.x、EMUI、MagicOS） ==========

        // 3.1 忽略电池优化（必要 · 手动兜底路径，优先系统弹窗直达）
        put(TutorialBrand.HUAWEI_HONOR to PermissionKey.BATTERY_OPTIMIZATION, TutorialContent(
            brand = TutorialBrand.HUAWEI_HONOR,
            permission = PermissionKey.BATTERY_OPTIMIZATION,
            whyNeed = "华为的系统省电策略会让后台应用“睡着”。加入电池优化白名单后，梦枕才能在息屏后持续播放。",
            cards = listOf(TutorialCard(
                id = "huawei_battery_main",
                title = "手动设置路径",
                steps = listOf(
                    TutorialStep(1, "打开「设置」", pathSegments = listOf("设置")),
                    TutorialStep(2, "在顶部搜索框输入「电池优化」，点按进入"),
                    TutorialStep(3, "点按顶部的「不允许」下拉框，切换为「所有应用」"),
                    TutorialStep(4, "在列表中找到「梦枕」"),
                    TutorialStep(5, "选择「不允许」",
                        tip = "选「不允许」才是加入白名单，这里的文案逻辑是反的，别选错。"),
                ),
            )),
            notice = "梦枕会优先帮你一键跳转到系统设置。如果没有弹窗、或弹窗被系统拦截，再按下面的手动路径操作。",
            searchKeyword = "电池优化",
        ))

        // 3.2 后台运行策略（必要 · 3 张可勾小卡）
        put(TutorialBrand.HUAWEI_HONOR to PermissionKey.BACKGROUND_RUNNING, TutorialContent(
            brand = TutorialBrand.HUAWEI_HONOR,
            permission = PermissionKey.BACKGROUND_RUNNING,
            whyNeed = "华为默认的“自动管理”会在应用退到后台一段时间后让它“睡着”，播放会被中断。做完下面 3 小步，才算完成。每完成一小步就打一个勾，回来接着做下一步。",
            cards = listOf(
                TutorialCard(
                    id = "huawei_bg_card_a",
                    title = "关闭自动管理",
                    steps = listOf(
                        TutorialStep(1, "打开「设置」", pathSegments = listOf("设置")),
                        TutorialStep(2, "进入「应用和服务」>「应用启动管理」",
                            pathSegments = listOf("设置", "应用和服务", "应用启动管理")),
                        TutorialStep(3, "找到「梦枕」，关闭右侧的「自动管理」开关"),
                    ),
                ),
                TutorialCard(
                    id = "huawei_bg_card_b",
                    title = "打开三个允许",
                    steps = listOf(
                        TutorialStep(1, "在弹出的手动管理窗口中，全部打开三项：",
                            subItems = listOf("允许自启动", "允许关联启动", "允许后台活动")),
                    ),
                ),
                TutorialCard(
                    id = "huawei_bg_card_c",
                    title = "多任务加锁",
                    steps = listOf(
                        TutorialStep(1, "返回桌面"),
                        TutorialStep(2, "从屏幕底部上滑并停顿，进入多任务界面"),
                        TutorialStep(3, "下拉「梦枕」的任务卡片"),
                        TutorialStep(4, "看到卡片右上角出现小锁图标，即完成"),
                    ),
                ),
            ),
            notice = "3 小步缺一，播放仍可能被中断——做完记得检查 3 个勾都打上了。",
            searchKeyword = "应用启动管理",
        ))

        // 3.3 自启动设置（随后台运行策略自动完成）
        put(TutorialBrand.HUAWEI_HONOR to PermissionKey.AUTO_START, TutorialContent(
            brand = TutorialBrand.HUAWEI_HONOR,
            permission = PermissionKey.AUTO_START,
            whyNeed = "完成「后台运行策略」小卡 B 后，此项自动完成（「允许自启动」已包含在内），列表中会自动打勾置灰，无需重复设置。",
            cards = listOf(TutorialCard(
                id = "huawei_autostart_main",
                title = "自动完成",
                steps = listOf(
                    TutorialStep(1, "无需手动操作——完成「后台运行策略」的「打开三个允许」后，系统已自动允许梦枕自启动。"),
                ),
            )),
            searchKeyword = "应用启动管理",
        ))

        // 3.4 关闭省电模式
        put(TutorialBrand.HUAWEI_HONOR to PermissionKey.CLOSE_POWER_SAVE, TutorialContent(
            brand = TutorialBrand.HUAWEI_HONOR,
            permission = PermissionKey.CLOSE_POWER_SAVE,
            whyNeed = "省电模式会限制后台活动与网络，可能直接导致播放中断。",
            cards = listOf(TutorialCard(
                id = "huawei_powersave_main",
                title = "关闭省电模式",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「电池」", pathSegments = listOf("设置", "电池")),
                    TutorialStep(2, "关闭「省电模式」开关"),
                    TutorialStep(3, "如开启过「超级省电」，一并关闭"),
                ),
            )),
            notice = "睡前如果习惯开省电模式，改用「忽略电池优化 + 后台运行策略」保护梦枕即可，不必整晚关闭省电模式——两个必要项设置好，播放就有保障。",
            searchKeyword = "省电模式",
        ))

        // 3.5 休眠状态保持网络连接
        put(TutorialBrand.HUAWEI_HONOR to PermissionKey.KEEP_NET_CONNECTION, TutorialContent(
            brand = TutorialBrand.HUAWEI_HONOR,
            permission = PermissionKey.KEEP_NET_CONNECTION,
            whyNeed = "手机休眠后会断网省电。若你需要在夜间同步播放进度或使用云端音频，断网会导致同步失败。纯本地播放的用户可跳过。",
            cards = listOf(TutorialCard(
                id = "huawei_keepnet_main",
                title = "保持网络连接",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「电池」", pathSegments = listOf("设置", "电池")),
                    TutorialStep(2, "进入「更多电池设置」", pathSegments = listOf("设置", "电池", "更多电池设置")),
                    TutorialStep(3, "打开「休眠时始终保持网络连接」"),
                ),
            )),
            notice = "开启后，无 WLAN 环境会消耗移动数据。",
            searchKeyword = "更多电池设置",
        ))

        // 3.6 联网控制
        put(TutorialBrand.HUAWEI_HONOR to PermissionKey.NETWORK_CONTROL, TutorialContent(
            brand = TutorialBrand.HUAWEI_HONOR,
            permission = PermissionKey.NETWORK_CONTROL,
            whyNeed = "华为手机管家可能单独禁止了梦枕联网，表现为登录失败、音频无法同步。",
            cards = listOf(TutorialCard(
                id = "huawei_network_main",
                title = "允许梦枕联网",
                steps = listOf(
                    TutorialStep(1, "打开「手机管家」", pathSegments = listOf("手机管家")),
                    TutorialStep(2, "进入「流量管理」>「应用联网」",
                        pathSegments = listOf("手机管家", "流量管理", "应用联网")),
                    TutorialStep(3, "找到「梦枕」"),
                    TutorialStep(4, "勾选「移动数据」和「WLAN」"),
                ),
            )),
            searchKeyword = "应用联网",
        ))

        // 3.7 智能省流量（二选一，两张小卡）
        put(TutorialBrand.HUAWEI_HONOR to PermissionKey.SMART_DATA_SAVER, TutorialContent(
            brand = TutorialBrand.HUAWEI_HONOR,
            permission = PermissionKey.SMART_DATA_SAVER,
            whyNeed = "开启智能省流量后，系统会阻止梦枕在后台使用网络数据。两种方案任选其一。",
            completionRule = CompletionRule.ANY,
            cards = listOf(
                TutorialCard(
                    id = "huawei_datasaver_a",
                    title = "方案 A · 关闭总开关（推荐）",
                    steps = listOf(
                        TutorialStep(1, "打开「手机管家」>「流量管理」>「智能省流量」，关闭总开关",
                            pathSegments = listOf("手机管家", "流量管理", "智能省流量")),
                    ),
                ),
                TutorialCard(
                    id = "huawei_datasaver_b",
                    title = "方案 B · 保留开启，加白名单",
                    steps = listOf(
                        TutorialStep(1, "保留智能省流量开启，进入「不受数据用量限制的应用」列表"),
                        TutorialStep(2, "单独打开「梦枕」"),
                    ),
                ),
            ),
            notice = "方案 B 更适合需要控制流量消耗的用户。",
            searchKeyword = "智能省流量",
        ))

        // ========== VIVO / iQOO（OriginOS 4/5、FuntouchOS） ==========

        // 4.1 后台运行策略（必要）
        put(TutorialBrand.VIVO to PermissionKey.BACKGROUND_RUNNING, TutorialContent(
            brand = TutorialBrand.VIVO,
            permission = PermissionKey.BACKGROUND_RUNNING,
            whyNeed = "VIVO 对后台应用的限制最为激进——默认策略下应用退到后台很快就会被系统关掉，必须手动完成以下设置。",
            cards = listOf(TutorialCard(
                id = "vivo_bg_main",
                title = "后台运行策略",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「电池」", pathSegments = listOf("设置", "电池")),
                    TutorialStep(2, "进入「后台耗电管理」（部分机型显示为「后台高耗电」）",
                        pathSegments = listOf("设置", "电池", "后台耗电管理")),
                    TutorialStep(3, "找到「梦枕」，选择「允许后台高耗电」"),
                    TutorialStep(4, "返回桌面，进入多任务界面"),
                    TutorialStep(5, "下拉「梦枕」卡片，将其加入加速白名单（卡片出现锁定标识）"),
                ),
            )),
            searchKeyword = "后台耗电管理",
        ))

        // 4.2 自启动设置（路径 A/B 二选一，两张小卡）
        put(TutorialBrand.VIVO to PermissionKey.AUTO_START, TutorialContent(
            brand = TutorialBrand.VIVO,
            permission = PermissionKey.AUTO_START,
            whyNeed = "允许梦枕自行启动，定时播放才能准时唤醒。两条路径任选其一。",
            completionRule = CompletionRule.ANY,
            cards = listOf(
                TutorialCard(
                    id = "vivo_autostart_a",
                    title = "路径 A · 系统设置",
                    steps = listOf(
                        TutorialStep(1, "打开「设置」>「应用与权限」>「权限管理」>「自启动管理」，打开「梦枕」",
                            pathSegments = listOf("设置", "应用与权限", "权限管理", "自启动管理")),
                    ),
                ),
                TutorialCard(
                    id = "vivo_autostart_b",
                    title = "路径 B · i 管家",
                    steps = listOf(
                        TutorialStep(1, "打开「i 管家」>「应用管理 / 软件管理」>「权限管理」>「权限」>「自启动」，打开「梦枕」",
                            pathSegments = listOf("i 管家", "应用管理", "权限管理", "自启动")),
                    ),
                ),
            ),
            notice = "同页的「关联启动」建议一并打开。",
            searchKeyword = "自启动",
        ))

        // 4.3 关闭省电模式
        put(TutorialBrand.VIVO to PermissionKey.CLOSE_POWER_SAVE, TutorialContent(
            brand = TutorialBrand.VIVO,
            permission = PermissionKey.CLOSE_POWER_SAVE,
            whyNeed = "省电模式会让应用“睡着”，夜间播放可能中断。",
            cards = listOf(TutorialCard(
                id = "vivo_powersave_main",
                title = "关闭省电模式",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「电池」", pathSegments = listOf("设置", "电池")),
                    TutorialStep(2, "关闭「省电模式」（如开启过「超级省电」一并关闭）"),
                ),
            )),
            searchKeyword = "省电模式",
        ))

        // 4.4 联网控制（路径 A/B 二选一，两张小卡）
        put(TutorialBrand.VIVO to PermissionKey.NETWORK_CONTROL, TutorialContent(
            brand = TutorialBrand.VIVO,
            permission = PermissionKey.NETWORK_CONTROL,
            whyNeed = "若梦枕无法登录或同步数据，请检查联网开关——被禁网的应用，图标上会显示红色「/」标记。两条路径任选其一。",
            completionRule = CompletionRule.ANY,
            cards = listOf(
                TutorialCard(
                    id = "vivo_network_a",
                    title = "路径 A · 系统设置",
                    steps = listOf(
                        TutorialStep(1, "打开「设置」>「移动网络」>「流量管理」>「联网管理」",
                            pathSegments = listOf("设置", "移动网络", "流量管理", "联网管理")),
                        TutorialStep(2, "找到「梦枕」，「数据」与「WLAN」均设为允许"),
                    ),
                ),
                TutorialCard(
                    id = "vivo_network_b",
                    title = "路径 B · i 管家",
                    steps = listOf(
                        TutorialStep(1, "打开「i 管家」>「流量管理」>「联网管理」",
                            pathSegments = listOf("i 管家", "流量管理", "联网管理")),
                        TutorialStep(2, "找到「梦枕」，「数据」与「WLAN」均设为允许"),
                    ),
                ),
            ),
            searchKeyword = "联网管理",
        ))

        // 4.5 智能省流量（省流量模式）
        put(TutorialBrand.VIVO to PermissionKey.SMART_DATA_SAVER, TutorialContent(
            brand = TutorialBrand.VIVO,
            permission = PermissionKey.SMART_DATA_SAVER,
            whyNeed = "开启省流量模式后，后台应用禁止联网，云端同步会被中断。",
            cards = listOf(TutorialCard(
                id = "vivo_datasaver_main",
                title = "加入白名单",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「移动网络」>「流量管理」>「省流量模式」（或「i 管家」>「流量管理」>「省流量模式」）",
                        pathSegments = listOf("设置", "移动网络", "流量管理", "省流量模式")),
                    TutorialStep(2, "若保留开启，进入「白名单管理」"),
                    TutorialStep(3, "勾选「梦枕」加入白名单"),
                ),
            )),
            notice = "只开模式、不加白名单，夜间联网仍会中断。OriginOS 开启后状态栏有常驻提示，属正常现象。",
            searchKeyword = "省流量模式",
        ))

        // ========== 小米 / Redmi（MIUI 14、HyperOS 澎湃OS） ==========

        // 5.1 后台运行策略（必要）
        put(TutorialBrand.XIAOMI to PermissionKey.BACKGROUND_RUNNING, TutorialContent(
            brand = TutorialBrand.XIAOMI,
            permission = PermissionKey.BACKGROUND_RUNNING,
            whyNeed = "小米默认的“智能限制后台”会让应用“睡着”，播放会被中断。必须手动改为无限制。",
            cards = listOf(TutorialCard(
                id = "xiaomi_bg_main",
                title = "后台运行策略",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「应用设置」>「应用管理」",
                        pathSegments = listOf("设置", "应用设置", "应用管理")),
                    TutorialStep(2, "找到并点按「梦枕」"),
                    TutorialStep(3, "点按「省电策略」"),
                    TutorialStep(4, "选择「无限制」（共三档：智能限制后台 / 禁止后台运行 / 无限制）",
                        tip = "出厂默认多为“智能限制后台”，必须改成「无限制」。"),
                    TutorialStep(5, "返回桌面"),
                    TutorialStep(6, "进入多任务界面，下拉「梦枕」的任务卡片，完成加锁"),
                ),
            )),
            notice = "多数情况不重启手机也生效；如果当晚播放仍被中断，第二天再重启一次手机。",
            alternatePath = "HyperOS 快捷路径：「设置」>「省电与电池」> 右上角齿轮图标 >「应用智能省电」> 找到「梦枕」> 选「无限制」。",
            searchKeyword = "省电策略",
        ))

        // 5.2 自启动设置（HyperOS / MIUI 14 二选一 + 备选路径）
        put(TutorialBrand.XIAOMI to PermissionKey.AUTO_START, TutorialContent(
            brand = TutorialBrand.XIAOMI,
            permission = PermissionKey.AUTO_START,
            whyNeed = "允许梦枕自行启动，定时播放才能准时唤醒。按你的系统版本选对应路径。",
            completionRule = CompletionRule.ANY,
            cards = listOf(
                TutorialCard(
                    id = "xiaomi_autostart_hyperos",
                    title = "HyperOS 澎湃OS",
                    steps = listOf(
                        TutorialStep(1, "打开「设置」>「应用设置」>「授权管理」>「自启动管理」，打开「梦枕」",
                            pathSegments = listOf("设置", "应用设置", "授权管理", "自启动管理")),
                    ),
                ),
                TutorialCard(
                    id = "xiaomi_autostart_miui",
                    title = "MIUI 14",
                    steps = listOf(
                        TutorialStep(1, "打开「设置」>「应用设置」>「权限管理」>「自启动管理」，打开「梦枕」",
                            pathSegments = listOf("设置", "应用设置", "权限管理", "自启动管理")),
                    ),
                ),
            ),
            alternatePath = "备选：「手机管家」>「应用管理」>「权限」>「自启动管理」。",
            searchKeyword = "自启动",
        ))

        // 5.3 关闭省电模式
        put(TutorialBrand.XIAOMI to PermissionKey.CLOSE_POWER_SAVE, TutorialContent(
            brand = TutorialBrand.XIAOMI,
            permission = PermissionKey.CLOSE_POWER_SAVE,
            whyNeed = "省电模式会让应用“睡着”，夜间播放可能中断。",
            cards = listOf(TutorialCard(
                id = "xiaomi_powersave_main",
                title = "关闭省电模式",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「省电与电池」",
                        pathSegments = listOf("设置", "省电与电池")),
                    TutorialStep(2, "关闭「省电模式」（如开启过「超级省电」一并关闭）"),
                    TutorialStep(3, "点右上角齿轮，检查是否设置了“定时开启省电模式”，如有则关闭"),
                ),
            )),
            searchKeyword = "省电模式",
        ))

        // 5.4 联网控制
        put(TutorialBrand.XIAOMI to PermissionKey.NETWORK_CONTROL, TutorialContent(
            brand = TutorialBrand.XIAOMI,
            permission = PermissionKey.NETWORK_CONTROL,
            whyNeed = "若梦枕无法登录或同步数据，请检查移动数据 / WLAN 联网开关。",
            cards = listOf(TutorialCard(
                id = "xiaomi_network_main",
                title = "允许梦枕联网",
                steps = listOf(
                    TutorialStep(1, "打开「手机管家」", pathSegments = listOf("手机管家")),
                    TutorialStep(2, "进入「网络助手」>「联网控制」",
                        pathSegments = listOf("手机管家", "网络助手", "联网控制")),
                    TutorialStep(3, "找到「梦枕」，勾选「移动数据」和「WLAN」"),
                ),
            )),
            searchKeyword = "联网控制",
        ))

        // 5.5 后台获取数据
        put(TutorialBrand.XIAOMI to PermissionKey.BACKGROUND_DATA, TutorialContent(
            brand = TutorialBrand.XIAOMI,
            permission = PermissionKey.BACKGROUND_DATA,
            whyNeed = "禁止后台数据后，梦枕在后台无法同步播放进度。",
            cards = listOf(TutorialCard(
                id = "xiaomi_bgdata_main",
                title = "允许后台联网",
                steps = listOf(
                    TutorialStep(1, "打开「手机管家」>「网络助手」>「联网控制」",
                        pathSegments = listOf("手机管家", "网络助手", "联网控制")),
                    TutorialStep(2, "点右上角「⋮」菜单 >「后台联网权限」"),
                    TutorialStep(3, "找到「梦枕」，打开开关"),
                ),
            )),
            alternatePath = "备选路径：「设置」>「应用设置」>「应用管理」>「梦枕」>「联网控制」> 打开「后台数据」（与移动数据 / WLAN 并列的第三个开关）。",
            searchKeyword = "后台联网权限",
        ))

        // ========== OPPO / 一加 / 真我（ColorOS 14/15、realme UI） ==========

        // 6.1 后台运行策略（必要）
        put(TutorialBrand.OPPO to PermissionKey.BACKGROUND_RUNNING, TutorialContent(
            brand = TutorialBrand.OPPO,
            permission = PermissionKey.BACKGROUND_RUNNING,
            whyNeed = "ColorOS 默认“智能优化后台运行”，息屏后会逐步限制应用。必须改为“完全允许”，才能保证整夜播放。",
            cards = listOf(TutorialCard(
                id = "oppo_bg_main",
                title = "后台运行策略",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「电池」", pathSegments = listOf("设置", "电池")),
                    TutorialStep(2, "进入「省电设置」>「应用耗电管理」",
                        pathSegments = listOf("设置", "电池", "省电设置", "应用耗电管理")),
                    TutorialStep(3, "找到并点按「梦枕」"),
                    TutorialStep(4, "选择「完全允许后台行为」（共三项：智能优化后台运行 / 限制后台运行 / 完全允许后台行为）"),
                    TutorialStep(5, "返回「电池」设置，进入「更多设置」"),
                    TutorialStep(6, "关闭「睡眠待机优化」",
                        tip = "「睡眠待机优化」是容易被忽略的开关——开启后夜间会自动断网断后台，必须关闭。"),
                    TutorialStep(7, "进入多任务界面，下拉「梦枕」卡片完成加锁"),
                ),
            )),
            searchKeyword = "应用耗电管理",
        ))

        // 6.2 自启动设置
        put(TutorialBrand.OPPO to PermissionKey.AUTO_START, TutorialContent(
            brand = TutorialBrand.OPPO,
            permission = PermissionKey.AUTO_START,
            whyNeed = "允许梦枕自行启动，定时播放才能准时唤醒。",
            cards = listOf(TutorialCard(
                id = "oppo_autostart_main",
                title = "自启动设置",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「应用」>「自启动」",
                        pathSegments = listOf("设置", "应用", "自启动")),
                    TutorialStep(2, "找到「梦枕」，打开开关"),
                ),
            )),
            alternatePath = "ColorOS 14/15 整合入口：「设置」>「应用」>「应用启动管理」>「梦枕」> 选「手动管理」，勾选「允许自启动 + 允许关联启动 + 允许后台活动」。",
            searchKeyword = "自启动",
        ))

        // 6.3 关闭省电模式
        put(TutorialBrand.OPPO to PermissionKey.CLOSE_POWER_SAVE, TutorialContent(
            brand = TutorialBrand.OPPO,
            permission = PermissionKey.CLOSE_POWER_SAVE,
            whyNeed = "省电模式会让应用“睡着”，夜间播放可能中断。",
            cards = listOf(TutorialCard(
                id = "oppo_powersave_main",
                title = "关闭省电模式",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「电池」", pathSegments = listOf("设置", "电池")),
                    TutorialStep(2, "关闭「省电模式」"),
                    TutorialStep(3, "检查是否开启了“按电量自动开启”，如有则关闭"),
                ),
            )),
            searchKeyword = "省电模式",
        ))

        // 6.4 联网控制
        put(TutorialBrand.OPPO to PermissionKey.NETWORK_CONTROL, TutorialContent(
            brand = TutorialBrand.OPPO,
            permission = PermissionKey.NETWORK_CONTROL,
            whyNeed = "若梦枕无法登录或同步数据，请检查移动数据 / WLAN 联网开关。",
            cards = listOf(TutorialCard(
                id = "oppo_network_main",
                title = "允许梦枕联网",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「移动网络」>「流量管理」>「应用联网管理」",
                        pathSegments = listOf("设置", "移动网络", "流量管理", "应用联网管理")),
                    TutorialStep(2, "找到「梦枕」"),
                    TutorialStep(3, "选择「WLAN 与移动数据」"),
                ),
            )),
            searchKeyword = "应用联网管理",
        ))

        // 6.5 后台获取数据
        put(TutorialBrand.OPPO to PermissionKey.BACKGROUND_DATA, TutorialContent(
            brand = TutorialBrand.OPPO,
            permission = PermissionKey.BACKGROUND_DATA,
            whyNeed = "若应用无法在后台同步播放进度，请检查此项并开启。",
            cards = listOf(TutorialCard(
                id = "oppo_bgdata_main",
                title = "允许后台使用数据",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「应用」>「应用管理」>「梦枕」",
                        pathSegments = listOf("设置", "应用", "应用管理", "梦枕")),
                    TutorialStep(2, "进入「流量使用情况 / 联网管理」"),
                    TutorialStep(3, "确认未勾选「禁用网络」，并打开「后台使用移动数据」（如有该开关；没有这一项就跳过，属正常）"),
                ),
            )),
            searchKeyword = "联网管理",
        ))

        // 6.6 智能省流量（流量节省）
        put(TutorialBrand.OPPO to PermissionKey.SMART_DATA_SAVER, TutorialContent(
            brand = TutorialBrand.OPPO,
            permission = PermissionKey.SMART_DATA_SAVER,
            whyNeed = "开启流量节省后，后台应用默认被禁用移动数据——锁屏即断流，是对梦枕影响最直接的开关。",
            cards = listOf(TutorialCard(
                id = "oppo_datasaver_main",
                title = "加入白名单",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「移动网络」>「流量管理」>「流量节省」",
                        pathSegments = listOf("设置", "移动网络", "流量管理", "流量节省")),
                    TutorialStep(2, "若保留流量节省开启，进入「不受限制的应用」"),
                    TutorialStep(3, "勾选「梦枕」加入白名单"),
                ),
            )),
            notice = "只开流量节省、不加白名单，夜间联网仍会中断。二选一：关闭流量节省，或加白名单。",
            searchKeyword = "流量节省",
        ))

        // ========== 通用兜底（其他品牌 / 原生 Android） ==========

        // 7.1 忽略电池优化（必要 · 手动兜底路径）
        put(TutorialBrand.GENERIC to PermissionKey.BATTERY_OPTIMIZATION, TutorialContent(
            brand = TutorialBrand.GENERIC,
            permission = PermissionKey.BATTERY_OPTIMIZATION,
            whyNeed = "系统的省电策略可能让梦枕在后台被关掉。加入电池优化白名单，夜间播放才不容易中断。",
            cards = listOf(TutorialCard(
                id = "generic_battery_main",
                title = "手动设置路径",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「应用」>「特殊应用权限」（部分机型：「设置」>「电池」>「电池优化」）",
                        pathSegments = listOf("设置", "应用", "特殊应用权限")),
                    TutorialStep(2, "进入「电池优化」"),
                    TutorialStep(3, "找到「梦枕」，选择「不优化 / 不允许」"),
                ),
            )),
            notice = "梦枕会优先帮你一键跳转到系统设置。如果没有弹窗、或弹窗被系统拦截，再按下面的手动路径操作。",
            searchKeyword = "电池优化",
        ))

        // 7.2 后台运行策略（必要）
        put(TutorialBrand.GENERIC to PermissionKey.BACKGROUND_RUNNING, TutorialContent(
            brand = TutorialBrand.GENERIC,
            permission = PermissionKey.BACKGROUND_RUNNING,
            whyNeed = "系统会限制后台运行的应用。完成这项设置，息屏后播放才不会被系统关掉。",
            cards = listOf(TutorialCard(
                id = "generic_bg_main",
                title = "后台运行策略",
                steps = listOf(
                    TutorialStep(1, "先完成「忽略电池优化」设置"),
                    TutorialStep(2, "打开「设置」>「应用」>「梦枕」>「电池」，选择「不受限制」（没有这一项就跳过，属正常）",
                        pathSegments = listOf("设置", "应用", "梦枕", "电池")),
                    TutorialStep(3, "进入多任务界面，下拉「梦枕」卡片加锁（没有这一项就跳过，属正常）"),
                    TutorialStep(4, "进入「梦枕」的应用信息页，确认未开启任何“停用 / 强制停止”类限制"),
                ),
            )),
            notice = "原生 Android 一般不会主动清理后台，完成电池优化设置后通常即可稳定播放。如仍被中断，优先检查是否安装了第三方“清理 / 省电”类应用，将梦枕加入其白名单。",
            searchKeyword = "电池",
        ))

        // 7.3 自启动设置
        put(TutorialBrand.GENERIC to PermissionKey.AUTO_START, TutorialContent(
            brand = TutorialBrand.GENERIC,
            permission = PermissionKey.AUTO_START,
            whyNeed = "允许梦枕自行启动，定时播放才能准时唤醒。",
            cards = listOf(TutorialCard(
                id = "generic_autostart_main",
                title = "自启动设置",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「应用」>「梦枕」",
                        pathSegments = listOf("设置", "应用", "梦枕")),
                    TutorialStep(2, "进入应用信息页，查找「自启动 / 自动启动」相关开关并打开（没有这一项就跳过，属正常）"),
                ),
            )),
            notice = "原生 Android 无“自启动”概念，此项仅部分国产 ROM 提供。找不到属正常，完成电池优化设置即可。",
            searchKeyword = "自启动",
        ))

        // 7.4 关闭省电模式
        put(TutorialBrand.GENERIC to PermissionKey.CLOSE_POWER_SAVE, TutorialContent(
            brand = TutorialBrand.GENERIC,
            permission = PermissionKey.CLOSE_POWER_SAVE,
            whyNeed = "省电模式会让应用“睡着”，夜间播放可能中断。",
            cards = listOf(TutorialCard(
                id = "generic_powersave_main",
                title = "关闭省电模式",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「电池」>「省电模式」",
                        pathSegments = listOf("设置", "电池", "省电模式")),
                    TutorialStep(2, "关闭开关，并检查“按电量自动开启”计划，如有则关闭"),
                ),
            )),
            searchKeyword = "省电模式",
        ))

        // ========== 三星（One UI 6/7） ==========

        // 8.1 后台运行策略（必要）
        put(TutorialBrand.SAMSUNG to PermissionKey.BACKGROUND_RUNNING, TutorialContent(
            brand = TutorialBrand.SAMSUNG,
            permission = PermissionKey.BACKGROUND_RUNNING,
            whyNeed = "三星的“深度休眠”会完全停止应用后台运行。必须将梦枕加入“不受限制”名单。",
            cards = listOf(TutorialCard(
                id = "samsung_bg_main",
                title = "后台运行策略",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「电池」>「后台使用限制」（部分国行版本显示为「后台控制」）",
                        pathSegments = listOf("设置", "电池", "后台使用限制")),
                    TutorialStep(2, "点按「不受限制的应用程序」"),
                    TutorialStep(3, "点右上角「+」，把「梦枕」加入列表"),
                    TutorialStep(4, "返回上一级，点按「深度休眠应用程序」"),
                    TutorialStep(5, "打开列表看看：有「梦枕」就长按移除，没有就跳过"),
                    TutorialStep(6, "辅助确认：「设置」>「应用程序」>「梦枕」>「电池」> 选择「不受限制」",
                        pathSegments = listOf("设置", "应用程序", "梦枕", "电池")),
                ),
            )),
            notice = "省电模式开启时，全局后台限制会覆盖上述设置——请勿在夜间开启省电模式。",
            searchKeyword = "后台使用限制",
        ))

        // 8.2 自启动设置
        put(TutorialBrand.SAMSUNG to PermissionKey.AUTO_START, TutorialContent(
            brand = TutorialBrand.SAMSUNG,
            permission = PermissionKey.AUTO_START,
            whyNeed = "允许梦枕自行启动，定时播放才能准时唤醒。",
            cards = listOf(TutorialCard(
                id = "samsung_autostart_main",
                title = "自启动设置",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「应用程序」",
                        pathSegments = listOf("设置", "应用程序")),
                    TutorialStep(2, "点右上角「⋮」>「特殊访问权限」"),
                    TutorialStep(3, "进入「自启动（自动运行）」"),
                    TutorialStep(4, "打开「梦枕」开关"),
                ),
            )),
            notice = "One UI 5.x 及更早版本入口在「智能管理器」>「自动运行应用程序」。",
            searchKeyword = "自启动",
        ))

        // 8.3 关闭省电模式
        put(TutorialBrand.SAMSUNG to PermissionKey.CLOSE_POWER_SAVE, TutorialContent(
            brand = TutorialBrand.SAMSUNG,
            permission = PermissionKey.CLOSE_POWER_SAVE,
            whyNeed = "省电模式会让应用“睡着”，夜间播放可能中断。",
            cards = listOf(TutorialCard(
                id = "samsung_powersave_main",
                title = "关闭省电模式",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「电池」", pathSegments = listOf("设置", "电池")),
                    TutorialStep(2, "关闭「省电模式」"),
                    TutorialStep(3, "检查“按电量自动开启”计划，如有则关闭"),
                ),
            )),
            alternatePath = "快捷方式：下拉通知栏两次展开快捷设置面板，点按「省电模式」图标。",
            searchKeyword = "省电模式",
        ))

        // 8.4 联网控制（步骤 3 含后台获取数据——超越喜马拉雅原版的增强项，修订记录 #11）
        put(TutorialBrand.SAMSUNG to PermissionKey.NETWORK_CONTROL, TutorialContent(
            brand = TutorialBrand.SAMSUNG,
            permission = PermissionKey.NETWORK_CONTROL,
            whyNeed = "若梦枕无法登录或同步数据，请检查移动数据 / WLAN 联网开关。",
            cards = listOf(TutorialCard(
                id = "samsung_network_main",
                title = "允许梦枕联网",
                steps = listOf(
                    TutorialStep(1, "打开「设置」>「连接」>「数据使用量」>「联网管理」",
                        pathSegments = listOf("设置", "连接", "数据使用量", "联网管理")),
                    TutorialStep(2, "选择「梦枕」，设为「数据和 WLAN」"),
                    TutorialStep(3, "辅助确认：「设置」>「应用程序」>「梦枕」>「移动数据」> 打开「允许后台使用数据」",
                        pathSegments = listOf("设置", "应用程序", "梦枕", "移动数据"),
                        tip = "此步即「后台获取数据」设置，完成后该项在列表中自动打勾，无需重复设置。"),
                    TutorialStep(4, "确认「数据节省程序」未拦截梦枕，或将其加入白名单"),
                ),
            )),
            searchKeyword = "数据使用量",
        ))

        // 8.4 附 · 后台获取数据（随「联网控制」步骤 3 自动完成，内容合并去重）
        put(TutorialBrand.SAMSUNG to PermissionKey.BACKGROUND_DATA, TutorialContent(
            brand = TutorialBrand.SAMSUNG,
            permission = PermissionKey.BACKGROUND_DATA,
            whyNeed = "完成「联网控制」步骤 3 后，此项自动完成（「允许后台使用数据」已包含在内），列表中会自动打勾置灰，无需重复设置。",
            cards = listOf(TutorialCard(
                id = "samsung_bgdata_main",
                title = "自动完成",
                steps = listOf(
                    TutorialStep(1, "无需手动操作——完成「联网控制」的步骤 3 后，系统已允许梦枕后台使用数据。"),
                ),
            )),
            searchKeyword = "移动数据",
        ))
    }
}
