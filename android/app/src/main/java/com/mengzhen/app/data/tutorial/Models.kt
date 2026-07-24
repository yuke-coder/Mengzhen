package com.mengzhen.app.data.tutorial

/**
 * 权限教程数据模型 —— 对齐 permission-tutorial-content.md v2 数据结构约定
 * 数据与 UI 分离：教程文案/步骤/路径段全部在此层定义，UI 零硬编码文案。
 */

/** 品牌枚举，顺序 = 品牌权重（数析校准：份额 × 后台限制激进度） */
enum class TutorialBrand(val displayName: String) {
    HUAWEI_HONOR("华为/荣耀"),
    VIVO("VIVO/iQOO"),
    XIAOMI("小米/Redmi"),
    OPPO("OPPO/一加/真我"),
    GENERIC("其他品牌"),
    SAMSUNG("三星")
}

/** 权限项分组 */
enum class PermissionGroup(val title: String) {
    REQUIRED("必须完成"),
    ON_DEMAND("遇到问题再设置")
}

enum class PermissionKey(
    val prefKey: String,
    val title: String,
    val subtitle: String,
    val group: PermissionGroup,
) {
    // 必须完成组
    BATTERY_OPTIMIZATION(
        "battery_optimization", "忽略电池优化",
        "系统的省电策略可能让梦枕在后台被关掉。加入电池优化白名单，夜间播放才不容易中断",
        PermissionGroup.REQUIRED
    ),
    BACKGROUND_RUNNING(
        "background_running", "后台运行策略",
        "系统会限制后台运行的应用。完成这项设置，息屏后播放才不会被系统关掉",
        PermissionGroup.REQUIRED
    ),

    // 遇到问题再设置组
    AUTO_START(
        "auto_start", "自启动设置",
        "允许梦枕自行启动，定时播放才能准时唤醒",
        PermissionGroup.ON_DEMAND
    ),
    CLOSE_POWER_SAVE(
        "close_power_save", "关闭省电模式",
        "省电模式会让应用“睡着”，夜间播放可能中断",
        PermissionGroup.ON_DEMAND
    ),
    KEEP_NET_CONNECTION(
        "keep_net_connection", "休眠状态保持网络连接",
        "为避免手机休眠后断网、云端同步失败，可开启此项",
        PermissionGroup.ON_DEMAND
    ),
    NETWORK_CONTROL(
        "network_control", "联网控制",
        "若梦枕无法登录或同步数据，请检查移动数据 / WLAN 联网开关",
        PermissionGroup.ON_DEMAND
    ),
    BACKGROUND_DATA(
        "background_data", "后台获取数据",
        "若应用无法在后台同步播放进度，请检查此项并开启",
        PermissionGroup.ON_DEMAND
    ),
    SMART_DATA_SAVER(
        "smart_data_saver", "智能省流量",
        "开启省流量功能后，系统将阻止梦枕后台联网。若使用此功能，请将梦枕加入白名单",
        PermissionGroup.ON_DEMAND
    );

    companion object {
        fun fromPrefKey(key: String?): PermissionKey? = entries.firstOrNull { it.prefKey == key }
    }
}

data class TutorialStep(
    val index: Int,                       // 步骤序号，从 1 开始
    val text: String,                     // 步骤正文
    val pathSegments: List<String>? = null, // 高亮路径：["设置","电池","应用启动管理"]
    val tip: String? = null,              // 步骤级防错提示（内嵌步骤下方）
    val subItems: List<String>? = null,   // 并列子项 bullet（允许自启动/允许关联启动/允许后台活动）
)

data class TutorialCard(
    val id: String,     // 小卡标识（如 "huawei_bg_card_a"）
    val title: String,  // 小卡标题（如 "关闭自动管理"）
    val steps: List<TutorialStep>,
)

/** 教程完成判定规则：ALL = 所有卡都做完（如华为后台策略 3 卡）；ANY = 任一卡做完即完成（方案/路径二选一教程） */
enum class CompletionRule { ALL, ANY }

data class TutorialContent(
    val brand: TutorialBrand,
    val permission: PermissionKey,
    val whyNeed: String,               // "为什么需要"段落
    val cards: List<TutorialCard>,     // 单卡教程 = 1 张卡；华为后台策略 = 3 张卡
    val completionRule: CompletionRule = CompletionRule.ALL, // 多卡二选一教程用 ANY
    val notice: String? = null,        // 跨步骤注意事项（可空）
    val alternatePath: String? = null, // 备选路径说明（可空）
    val searchKeyword: String,         // 设置搜索框关键词（兜底）
)
