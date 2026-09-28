package com.mengzhen.app.util

import com.mengzhen.app.data.model.UserInfo
import com.mengzhen.app.data.store.ProfileVisibilityStore

/**
 * 性别展示文案的唯一来源 —— 资料卡（「我的」页摘要行）与侧边栏资料卡共用。
 *
 * 为什么要抽出来：两处原本各写一份判断，其中一份是 `if (g == "male") "男" else "女"`，
 * 于是任何非 male 的值都会显示成「女」，而且「保密」用的是硬编码字面量 `"secret"`，
 * 与 [ProfileVisibilityStore.isSecret]（大小写不敏感）不一致，将来改编码会静默失效。
 *
 * ## 取值映射的真实依据
 *
 * 真本性别选择弹窗的列表项就是 `add("男")` / `add("女")`
 * （com.ximalaya.ting.android.main.dialog.c 的 AnonymousClass3），
 * 提交值走 male / female；`"1"` / `"2"` / 中文是历史两端数据里的兼容写法。
 *
 * 无法识别的值（含 [ProfileVisibilityStore.GENDER_SECRET]）返回 **null**，
 * 由调用方整行不渲染，而不是显示成「女」。
 */
fun UserInfo.genderLabel(): String? = genderLabel(gender)

/**
 * 同上，直接吃字符串 —— 供「编辑资料」页那种手上只有 `gender` 值、没有 UserInfo 的地方用。
 */
fun genderLabel(gender: String?): String? = when (gender?.lowercase()) {
    "male", "男", "1" -> "男"
    "female", "女", "2" -> "女"
    else -> null
}

/** 性别是否应当对外隐藏（服务端存 `gender = 'secret'`）。 */
fun UserInfo.isGenderSecret(): Boolean = ProfileVisibilityStore.isSecret(gender)

/** 同上，字符串版。 */
fun isGenderSecret(gender: String?): Boolean = ProfileVisibilityStore.isSecret(gender)
