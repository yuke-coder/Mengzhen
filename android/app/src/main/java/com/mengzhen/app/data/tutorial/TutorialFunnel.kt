package com.mengzhen.app.data.tutorial

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.funnelDataStore by preferencesDataStore(name = "tutorial_funnel")

/**
 * 教程漏斗埋点（批次四 · 数析基线，审核报告行动清单 #6）
 *
 * 计数事件：
 * - `funnel_enter_{brand}_{permission}`：进入教程页（含页内切换品牌）
 * - `funnel_done_{brand}_{permission}`：教程完成翻转（未完成→已完成的瞬间，翻转才计）
 * - `funnel_emergency_exit`：应急出口点击（全局计数）
 *
 * 步骤勾选明细复用 tutorial_progress_*（TutorialRepository 的 DataStore），此处不重复计；
 * 基线分析时两边 join：进入 → 逐步勾选 → 完成翻转 → 应急出口流失。
 *
 * 纯本地累积，无网络上报。用 [snapshot] 导出全量（后续接查看页/分享导出）。
 */
object TutorialFunnel {

    private fun enterKey(brand: TutorialBrand, permission: PermissionKey) =
        intPreferencesKey("funnel_enter_${brand.name}_${permission.prefKey}")

    private fun doneKey(brand: TutorialBrand, permission: PermissionKey) =
        intPreferencesKey("funnel_done_${brand.name}_${permission.prefKey}")

    private val emergencyExitKey = intPreferencesKey("funnel_emergency_exit")

    /** 进入教程页（教程页 LaunchedEffect 调用） */
    suspend fun trackEnter(context: Context, brand: TutorialBrand, permission: PermissionKey) {
        val key = enterKey(brand, permission)
        context.funnelDataStore.edit { it[key] = (it[key] ?: 0) + 1 }
    }

    /** 教程完成翻转（仅 false→true 时由 TutorialRepository.setStepChecked 调用） */
    suspend fun trackDone(context: Context, brand: TutorialBrand, permission: PermissionKey) {
        val key = doneKey(brand, permission)
        context.funnelDataStore.edit { it[key] = (it[key] ?: 0) + 1 }
    }

    /** 应急出口点击（教程页 EmergencyExit onClick 调用） */
    suspend fun trackEmergencyExit(context: Context) {
        context.funnelDataStore.edit { it[emergencyExitKey] = (it[emergencyExitKey] ?: 0) + 1 }
    }

    /** 全量快照：funnel_* 全部计数（基线导出用） */
    suspend fun snapshot(context: Context): Map<String, Int> {
        val prefs = context.funnelDataStore.data.first()
        return prefs.asMap()
            .filterKeys { it.name.startsWith("funnel_") }
            .mapKeys { it.key.name }
            .mapValues { it.value as Int }
    }
}
