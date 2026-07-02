package com.efa.assistant.core.model.repository

import com.efa.assistant.core.model.FocusRecord
import com.efa.assistant.core.model.Mission
import kotlinx.coroutines.flow.Flow

/**
 * 行为分析数据仓储接口 (Domain Layer Contract)。
 */
interface AnalyticsRepository {
    /**
     * 记录一次专注计时历史。
     */
    suspend fun recordFocusSession(
        missionId: String,
        actionId: String,
        durationSeconds: Long,
        isCompleted: Boolean
    )

    /**
     * 观察总启动行动次数。
     */
    fun getTotalStartCount(): Flow<Int>

    /**
     * 获取历史任务完成率 (已完成任务 / 总任务，以 0.0 ~ 1.0 的百分比返回)。
     */
    suspend fun getCompletionRate(): Double

    /**
     * 观察最易拖延的前几个任务。
     */
    fun getMostProcrastinatedMissions(): Flow<List<Mission>>

    /**
     * 观察单次最长专注时间（秒）。
     */
    fun getLongestFocusDurationSeconds(): Flow<Long>

    /**
     * 观察最佳专注时段（如 14 表示下午两点最专注）。
     */
    fun getBestWorkingHour(): Flow<Int?>

    /**
     * 观察最佳专注星期（1=星期一, ..., 7=星期日）。
     */
    fun getBestWorkingDayOfWeek(): Flow<Int?>

    /**
     * 获取从指定时间戳以来的所有专注日志。
     */
    fun getFocusRecordsSince(since: Long): Flow<List<FocusRecord>>
}
