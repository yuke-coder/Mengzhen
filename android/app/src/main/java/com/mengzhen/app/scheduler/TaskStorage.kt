package com.mengzhen.app.scheduler

import android.content.Context
import org.json.JSONArray

class TaskStorage private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("dream_pillow_alarms", Context.MODE_PRIVATE)

    fun saveTask(task: TaskInfo) {
        val tasks = getAllTasks().filterNot { it.taskId == task.taskId }.toMutableList()
        tasks.add(task)
        persist(tasks)
    }

    fun removeTask(taskId: String) {
        persist(getAllTasks().filterNot { it.taskId == taskId })
    }

    fun getTask(taskId: String): TaskInfo? = getAllTasks().find { it.taskId == taskId }

    fun getAllTasks(): List<TaskInfo> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { taskInfoFromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persist(tasks: List<TaskInfo>) {
        val arr = JSONArray()
        tasks.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    /** 清理已完成的一次性任务 - 对标 Web 端 cleanupCompletedOnceTasks */
    fun cleanupCompletedOnceTasks(): Int {
        val tasks = getAllTasks().toMutableList()
        val before = tasks.size
        tasks.removeAll { it.status == "completed" && it.repeatType == 0 && it.repeatDays == 0 }
        val removed = before - tasks.size
        if (removed > 0) persist(tasks)
        return removed
    }

    /** 清理已取消的任务 - 对标 Web 端 cleanupCancelledTasks */
    fun cleanupCancelledTasks(): Int {
        val tasks = getAllTasks().toMutableList()
        val before = tasks.size
        tasks.removeAll { it.status == "cancelled" }
        val removed = before - tasks.size
        if (removed > 0) persist(tasks)
        return removed
    }

    companion object {
        private const val KEY = "alarm_tasks"
        @Volatile private var instance: TaskStorage? = null
        fun get(context: Context): TaskStorage =
            instance ?: synchronized(this) {
                instance ?: TaskStorage(context.applicationContext).also { instance = it }
            }
    }
}
