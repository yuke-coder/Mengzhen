package com.mengzhen.app.scheduler

import java.util.Calendar

/**
 * 中国法定节假日工具 - 对标 Web 端 task-types.ts 的 HOLIDAYS 表 + isWorkday/isChineseHoliday
 *
 * Web 端硬编码 2025-2026 年节假日，Android 端同步
 * 节假日调休逻辑：如果周末被调成工作日，需要额外标记
 */
object ChineseHolidayCalendar {

    // 节假日集合（放假日期）
    private val HOLIDAYS: Set<String> = setOf(
        // 2025
        "2025-01-01",
        "2025-01-28", "2025-01-29", "2025-01-30", "2025-01-31",
        "2025-02-01", "2025-02-02", "2025-02-03", "2025-02-04",
        "2025-04-04", "2025-04-05", "2025-04-06",
        "2025-05-01", "2025-05-02", "2025-05-03", "2025-05-04", "2025-05-05",
        "2025-05-31", "2025-06-01", "2025-06-02",
        "2025-10-01", "2025-10-02", "2025-10-03", "2025-10-04",
        "2025-10-05", "2025-10-06", "2025-10-07", "2025-10-08",
        // 2026
        "2026-01-01", "2026-01-02", "2026-01-03",
        "2026-02-16", "2026-02-17", "2026-02-18", "2026-02-19", "2026-02-20",
        "2026-04-04", "2026-04-05", "2026-04-06",
        "2026-05-01", "2026-05-02", "2026-05-03", "2026-05-04", "2026-05-05",
        "2026-06-19", "2026-06-20", "2026-06-21",
        "2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04",
        "2026-10-05", "2026-10-06", "2026-10-07",
    )

    // 调休工作日（周末但需上班）
    private val WORKDAY_OVERRIDE: Set<String> = setOf(
        // 2025 调休
        "2025-01-26", "2025-02-08",
        "2025-04-27",
        "2025-09-28", "2025-10-11",
        // 2026 调休（根据已知安排）
        "2026-02-15", "2026-02-28",
        "2026-06-28",
        "2026-09-27", "2026-10-10",
    )

    private fun dateKey(year: Int, month: Int, day: Int): String {
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    /** 是否中国法定节假日 */
    fun isHoliday(year: Int, month: Int, day: Int): Boolean {
        return HOLIDAYS.contains(dateKey(year, month, day))
    }

    /** 是否调休工作日（周末但需上班） */
    fun isWorkdayOverride(year: Int, month: Int, day: Int): Boolean {
        return WORKDAY_OVERRIDE.contains(dateKey(year, month, day))
    }

    /** 是否工作日（法定工作日 = 周一到周五且非节假日，或调休工作日） */
    fun isWorkday(calendar: Calendar): Boolean {
        val dow = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dow == Calendar.SUNDAY || dow == Calendar.SATURDAY
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)

        if (isWeekend) {
            // 周末但调休工作日 -> 工作日
            return isWorkdayOverride(y, m, d)
        }
        // 工作日但节假日 -> 非工作日
        return !isHoliday(y, m, d)
    }

    /** 是否节假日（含周末，用于 holiday 重复类型） */
    fun isHolidayOrWeekend(calendar: Calendar): Boolean {
        val dow = calendar.get(Calendar.DAY_OF_WEEK)
        if (dow == Calendar.SUNDAY || dow == Calendar.SATURDAY) return true
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        return isHoliday(y, m, d)
    }

    /**
     * 重复类型
     * 0 = 一次性
     * 1 = 每天
     * 2 = 法定工作日
     * 3 = 法定节假日
     */
    const val REPEAT_ONCE = 0
    const val REPEAT_DAILY = 1
    const val REPEAT_WORKDAY = 2
    const val REPEAT_HOLIDAY = 3

    /**
     * 判断给定日期是否符合重复类型
     */
    fun shouldExecuteOnDate(repeatType: Int, calendar: Calendar): Boolean {
        return when (repeatType) {
            REPEAT_DAILY -> true
            REPEAT_WORKDAY -> isWorkday(calendar)
            REPEAT_HOLIDAY -> isHolidayOrWeekend(calendar)
            else -> false
        }
    }

    /**
     * 计算下次触发时间
     * @param repeatType 重复类型
     * @param hour 触发小时
     * @param minute 触发分钟
     * @param fromDate 起始日期
     * @return 下次触发时间戳，或 -1 表示无法计算
     */
    fun getNextTrigger(
        repeatType: Int,
        hour: Int,
        minute: Int,
        fromDate: Calendar = Calendar.getInstance(),
    ): Long {
        if (repeatType == REPEAT_ONCE) return -1

        val cal = fromDate.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // 如果今天的时间还没到，先检查今天
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 最多找 366 天
        for (i in 0..366) {
            if (shouldExecuteOnDate(repeatType, cal)) {
                return cal.timeInMillis
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return -1
    }
}
