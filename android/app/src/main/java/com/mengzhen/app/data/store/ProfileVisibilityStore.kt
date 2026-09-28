package com.mengzhen.app.data.store

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.profileVisibilityDataStore by preferencesDataStore(name = "profile_visibility")

/**
 * 资料字段「不展示 / 保密」的本机镜像 —— Android 端「不展示」即 Web 端「保密」。
 *
 * ## 三个字段的落库方式
 *
 * | 字段 | 后端字段 | 跨端 / 跨设备 |
 * |---|---|---|
 * | 性别 | `user_profiles.gender = 'secret'` | ✅ |
 * | 生日 | `user_profiles.hide_birthday` | ✅ |
 * | 地区 | `user_profiles.hide_region` | ✅ |
 * | 性别真值 | `user_profiles.last_gender` | ✅（本机镜像作兜底，见 [lastGender]） |
 *
 * 后端字段由 `supabase/profile_gender_secret.sql`、`supabase/profile_field_visibility.sql`
 * 与 `supabase/profile_last_gender.sql` 建立，`/api/profile` 与 `/api/auth/me` 会下发、
 * PUT 会接收，Web 端也会据此隐藏。
 *
 * ## 那为什么还要留一个本机镜像？
 *
 * 只有一个原因：**线上部署可能落后于仓库**。
 * `UserInfo.hideBirthday/hideRegion` 为 `null` 表示服务端没下发该字段，
 * 此时显示层退回这里的镜像值；一旦服务端开始下发（部署后），就以服务端为准。
 * 读取规则统一写作 `user.hideBirthday ?: mirror.birthday`，
 * 因此不需要"迁移开关"，也不会出现本地值压过服务端的粘滞。
 *
 * 写入时两边都写：服务端（权威）+ 本机镜像（兜底）。
 *
 * 性别不走这里：它以 `gender` 字面量 `'secret'` 表达，值本身即状态
 * （见 [GENDER_SECRET] 与 [isSecret]）。梦枕 Web 端一直这么存：
 * `lib/auth-context.tsx` 的类型、网页设置页的 `["secret","保密"]`、`user-menu.tsx` 的
 * `gender !== 'secret'`、`api/avatar` 的 `DEFAULT_AVATARS.secret` 四处一致。
 *
 * 注：真本 Android 对性别的处理是 MyDetailInfo 上的独立布尔 `publicGender`
 * （MyDetailFragment 的 CheckBox 回调 `setPublicGender(bl2 ^ true)`），
 * 即「保留真值 + 单独开关」；本文件对生日/地区沿用同一思路，
 * 性别则改用梦枕 Web 端已有的 secret 编码，避免两端两套语义。
 *
 * TODO(清理)：等线上部署到带 hide_birthday / hide_region 的版本并稳定后，
 * 可删掉本文件与三处 `?:` 回退，直接读 `UserInfo` 上的字段。
 */
object ProfileVisibilityStore {

    /**
     * 性别「保密 / 不展示」的落库值，与 Web 端完全一致，改动需两端同步。
     * 对应的数据库约束见 `supabase/profile_gender_secret.sql`。
     */
    const val GENDER_SECRET = "secret"

    /** 后端 gender 值是否表示「不展示 / 保密」。 */
    fun isSecret(gender: String?): Boolean =
        gender?.equals(GENDER_SECRET, ignoreCase = true) == true

    /** 目前后端无字段、只能本机保存可见性的字段。性别不在此列（见类注释）。 */
    enum class Field { BIRTHDAY, REGION }

    /** 生日 / 地区的可见性快照；为 true 表示用户勾了「不展示」。 */
    data class Visibility(
        val birthday: Boolean = false,
        val region: Boolean = false,
    ) {
        companion object {
            /** 全可见，用作响应式收集的初值（DataStore 首次发射前的占位）。 */
            val NONE = Visibility()
        }
    }

    private val KEY_HIDE_BIRTHDAY = booleanPreferencesKey("hide_birthday")
    private val KEY_HIDE_REGION = booleanPreferencesKey("hide_region")

    /**
     * 最近一次真实性别（male / female）的**本机镜像**。
     *
     * 为什么需要「上次真值」这件事：性别用「值 = secret」表达保密，
     * **开启保密会把真实值覆写掉**，取消保密时后端已经不知道该回退到男还是女。
     * 生日 / 地区不需要：它们的值一直在库里，保密只是另一个布尔。
     *
     * 权威副本在后端 `user_profiles.last_gender`（由 PUT /api/profile 在 gender
     * 被写成真值时自动维护）。这里只是它的兜底镜像，读的时候写作
     * `user.lastGender ?: lastGender(context)`：线上还没部署到带该列的版本时，
     * 至少本机还记得用户上次选的是男是女。
     * （也正因如此，光在网页选过性别的用户在这里可能是空的 —— 那时靠服务端那份。）
     */
    private val KEY_LAST_GENDER = stringPreferencesKey("last_gender")

    private fun keyOf(field: Field) = when (field) {
        Field.BIRTHDAY -> KEY_HIDE_BIRTHDAY
        Field.REGION -> KEY_HIDE_REGION
    }

    private fun Preferences.toVisibility() = Visibility(
        birthday = this[KEY_HIDE_BIRTHDAY] ?: false,
        region = this[KEY_HIDE_REGION] ?: false,
    )

    /**
     * 响应式可见性：任一开关被改动都会重新发射。
     *
     * 资料卡（「我的」页）与侧边栏资料卡都要跟着开关实时变，所以用这个 Flow；
     * 「进页面读一次」的场景用 [current] 即可。
     */
    fun visibility(context: Context): Flow<Visibility> =
        context.profileVisibilityDataStore.data.map { it.toVisibility() }

    /** 一次性读取快照。 */
    suspend fun current(context: Context): Visibility = visibility(context).first()

    suspend fun setHidden(context: Context, field: Field, hidden: Boolean) {
        context.profileVisibilityDataStore.edit { prefs -> prefs[keyOf(field)] = hidden }
    }

    /**
     * 写入真实性别时同步记一份本机镜像，供取消「保密」时回退。
     *
     * 非 male / female（例如 'secret'、'other'）会被忽略，
     * 避免把保密标记本身记成"上一次真值"。
     */
    suspend fun rememberGender(context: Context, gender: String?) {
        if (gender != GENDER_REAL_MALE && gender != GENDER_REAL_FEMALE) return
        context.profileVisibilityDataStore.edit { prefs -> prefs[KEY_LAST_GENDER] = gender }
    }

    /**
     * 读取上次记录的真实性别；从未记录过则为 null
     * —— 此时没有可回退的选项，取消保密只能让用户重选一次。
     */
    suspend fun lastGender(context: Context): String? =
        context.profileVisibilityDataStore.data.first()[KEY_LAST_GENDER]

    const val GENDER_REAL_MALE = "male"
    const val GENDER_REAL_FEMALE = "female"
}
