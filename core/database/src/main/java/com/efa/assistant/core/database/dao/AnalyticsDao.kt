package com.efa.assistant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.efa.assistant.core.database.model.FocusRecordEntity
import com.efa.assistant.core.database.model.MissionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 行为分析统计 DAO。
 * 提供各种 SQL 聚合和原始数据流，用于在 Repository 中计算拖延和专注报表。
 */
@Dao
interface AnalyticsDao {

    /**
     * 插入一条专注记录。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusRecord(record: FocusRecordEntity)

    /**
     * 观察总启动（专注）次数。
     */
    @Query("SELECT COUNT(*) FROM focus_records")
    fun getTotalStartCountFlow(): Flow<Int>

    /**
     * 获取总任务数（计算完成率）。
     */
    @Query("SELECT COUNT(*) FROM missions")
    suspend fun getTotalMissionsCount(): Int

    /**
     * 获取已完成任务数（计算完成率）。
     */
    @Query("SELECT COUNT(*) FROM missions WHERE isCompleted = 1")
    suspend fun getCompletedMissionsCount(): Int

    /**
     * 观察拖延次数最多的前五个任务（最容易拖延任务）。
     */
    @Query("SELECT * FROM missions WHERE deferredCount > 0 ORDER BY deferredCount DESC LIMIT 5")
    fun getMostProcrastinatedMissionsFlow(): Flow<List<MissionEntity>>

    /**
     * 观察单次最长专注时长（以秒为单位）。
     */
    @Query("SELECT MAX(durationSeconds) FROM focus_records WHERE isCompleted = 1")
    fun getLongestFocusDurationSecondsFlow(): Flow<Long?>

    /**
     * 获取所有已完成专注的小时点，便于分析“最佳工作时段”。
     */
    @Query("SELECT hourOfDay FROM focus_records WHERE isCompleted = 1")
    fun getAllFocusHoursFlow(): Flow<List<Int>>

    /**
     * 获取所有已完成专注的星期数，便于分析“最佳工作日”。
     */
    @Query("SELECT dayOfWeek FROM focus_records WHERE isCompleted = 1")
    fun getAllFocusDaysOfWeekFlow(): Flow<List<Int>>

    /**
     * 观察所有专注记录（按时间倒序）。
     */
    @Query("SELECT * FROM focus_records ORDER BY startTime DESC")
    fun getAllFocusRecordsFlow(): Flow<List<FocusRecordEntity>>

    /**
     * 观察自某个时间戳起的专注记录（用于今日、本周、本月趋势图的统计）。
     */
    @Query("SELECT * FROM focus_records WHERE startTime >= :sinceTimestamp ORDER BY startTime ASC")
    fun getFocusRecordsSinceFlow(sinceTimestamp: Long): Flow<List<FocusRecordEntity>>
}
