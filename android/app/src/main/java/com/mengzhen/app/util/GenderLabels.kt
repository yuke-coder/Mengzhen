package com.mengzhen.app.util

import com.mengzhen.app.data.model.UserInfo

/**
 * 性别编码 + 展示文案的唯一来源 —— 资料卡（「我的」页摘要行）、侧边栏资料卡
 * 与编辑资料页共用。
 *
 * 为什么要抽出来：两处原本各写一份判断，其中一份是 `if (g == "male") "男" else "女"`，
 * 于是任何非 male 的值都会显示成「女」，而且「保密」用的是硬编码字面量 `"secret"`，
 * 与 [isGenderSecret]（大小写不敏感）不一致，将来改编码会静默失效。
 *
 * ## 取值映射的真实依据
 *
 * 真本性别选择弹窗的列表项就是 `add("男")` / `add("女")`
 * （com.ximalaya.ting.android.main.dialog.c 的 AnonymousClass3），
 * 提交值走 male / female；`"1"` / `"2"` / 中文是历史两端数据里的兼容写法。
 *
 * 无法识别的值（含 [GENDER_SECRET]）返回 **null**，
 * 由调用方整行不渲染，而不是显示成「女」。
 */
/**
 * 性别「保密 / 不展示」的落库值，与 Web 端完全一致，改动需两端同步。
 * 对应的数据库约束见 `supabase/profile_gender_secret.sql`。
 *
 * 为什么用「值 = secret」而不是独立的布尔开关：梦枕 Web 端一直这么存 ——
 * `lib/auth-context.tsx` 的类型、网页设置页的 `["secret","保密"]`、`user-menu.tsx` 的
 * `gender !== 'secret'`、`api/avatar` 的 `DEFAULT_AVATARS.secret` 四处一致。
 * 安卓端沿用同一编码，才谈得上两端同源。
 */
const val GENDER_SECRET = "secret"

fun UserInfo.genderLabel(): String? = genderLabel(gender)

/**
 * 同上，直接吃字符串 —— 供「编辑资料」页那种手上只有 `gender` 值、没有 UserInfo 的地方用。
 */
fun genderLabel(gender: String?): String? = when (gender?.lowercase()) {
    "male", "男", "1" -> "男"
    "female", "女", "2" -> "女"
    else -> null
}

/** 后端 gender 值是否表示「不展示 / 保密」。大小写不敏感。 */
fun isGenderSecret(gender: String?): Boolean =
    gender?.equals(GENDER_SECRET, ignoreCase = true) == true

/** 性别是否应当对外隐藏（服务端存 `gender = 'secret'`）。 */
fun UserInfo.isGenderSecret(): Boolean = isGenderSecret(gender)
