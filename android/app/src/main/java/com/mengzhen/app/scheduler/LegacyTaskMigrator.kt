package com.mengzhen.app.scheduler

import android.content.Context
import android.util.Log
import com.mengzhen.app.data.model.ScheduledTask
import com.mengzhen.app.data.model.TaskAudio
import com.mengzhen.app.data.model.TaskRepeatType
import com.mengzhen.app.data.model.TaskStartTime
import com.mengzhen.app.data.model.TaskStatus
import com.mengzhen.app.data.store.TaskStore
import org.json.JSONArray
import java.util.Calendar

/**
 * 把旧版本独立保存的 AlarmManager 任务一次性合并进 TaskStore。
 *
 * 迁移成功后才移除旧副本，避免升级过程中因解析异常丢失任务。
 */
internal class LegacyTaskMigrator(
    context: Context,
    private val taskStore: TaskStore = TaskStore.get(context),
    private val legacyStore: TaskStorage = TaskStorage.get(context),
) {

    fun migrate() {
        val existingIds = taskStore.getAllTasks().mapTo(mutableSetOf()) { it.id }
        legacyStore.getAllTasks().forEach { old ->
            if (old.taskId.isBlank()) return@forEach
            if (old.taskId in existingIds) {
                legacyStore.removeTask(old.taskId)
                return@forEach
            }

            val migrated = old.toScheduledTask()
            taskStore.saveTask(migrated)
            existingIds.add(old.taskId)
            legacyStore.removeTask(old.taskId)
        }
    }

    private fun TaskInfo.toScheduledTask(): ScheduledTask {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = triggerAt.takeIf { it > 0 } ?: System.currentTimeMillis()
        }
        val now = System.currentTimeMillis()
        return ScheduledTask(
            id = taskId,
            name = taskName,
            startTime = TaskStartTime(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH),
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE),
                second = calendar.get(Calendar.SECOND),
            ),
            playDurationMinutes = playDurationMinutes,
            fadeInDuration = fadeInDuration,
            fadeOutDuration = fadeOutDuration,
            enableFade = enableFade,
            enableFadeOut = enableFadeOut,
            volume = volume,
            repeatType = repeatType.toRepeatType(),
            audios = parseAudios(),
            status = status.toTaskStatus(),
            createdAt = createdAt.takeIf { it > 0 } ?: now,
            lastExecutedAt = lastExecutedAt.takeIf { it > 0 },
            nextExecuteAt = nextExecuteAt.takeIf { it > 0 } ?: triggerAt.takeIf { it > 0 },
            completedAt = completedAt.takeIf { it > 0 },
            skipUntil = skipUntil.takeIf { it > 0 },
            executionStartedAt = lastExecutedAt.takeIf { status == "executing" && it > 0 },
            executionEndsAt = endTime.takeIf { status == "executing" && it > 0 },
            updatedAt = updatedAt.takeIf { it > 0 } ?: now,
        )
    }

    private fun Int.toRepeatType(): TaskRepeatType = when (this) {
        ChineseHolidayCalendar.REPEAT_DAILY -> TaskRepeatType.DAILY
        ChineseHolidayCalendar.REPEAT_WORKDAY -> TaskRepeatType.WORKDAY
        ChineseHolidayCalendar.REPEAT_HOLIDAY -> TaskRepeatType.HOLIDAY
        else -> TaskRepeatType.ONCE
    }

    private fun String.toTaskStatus(): TaskStatus = when (this) {
        "executing" -> TaskStatus.EXECUTING
        "completed" -> TaskStatus.COMPLETED
        "cancelled" -> TaskStatus.CANCELLED
        else -> TaskStatus.PENDING
    }

    private fun TaskInfo.parseAudios(): List<TaskAudio> {
        val parsed = mutableListOf<TaskAudio>()
        if (tracksJson.isNotBlank()) {
            try {
                val array = JSONArray(tracksJson)
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val url = item.optString("url")
                    if (url.isNotBlank()) {
                        parsed += TaskAudio(
                            id = "${taskId}_$index",
                            name = item.optString("name", "音频${index + 1}"),
                            serverUrl = url,
                        )
                    }
                }
            } catch (error: Exception) {
                Log.w(TAG, "Failed to migrate tracks for $taskId", error)
            }
        }
        if (parsed.isEmpty() && audioUrl.isNotBlank()) {
            parsed += TaskAudio(
                id = "${taskId}_0",
                name = audioName.ifBlank { "音频" },
                serverUrl = audioUrl,
            )
        }
        return parsed
    }

    private companion object {
        const val TAG = "LegacyTaskMigrator"
    }
}
