package com.efa.assistant.core.database.repository

import com.efa.assistant.core.common.DispatcherProvider
import com.efa.assistant.core.database.dao.AnalyticsDao
import com.efa.assistant.core.database.model.FocusRecordEntity
import com.efa.assistant.core.model.FocusRecord
import com.efa.assistant.core.model.Mission
import com.efa.assistant.core.model.repository.AnalyticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 行为统计与分析数据仓储的 Room 实现类。
 */
@Singleton
class RoomAnalyticsRepository @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    private val dispatcherProvider: DispatcherProvider
) : AnalyticsRepository {

    override suspend fun recordFocusSession(
        missionId: String,
        actionId: String,
        durationSeconds: Long,
        isCompleted: Boolean
    ) {
        withContext(dispatcherProvider.io()) {
            val startTime = System.currentTimeMillis()
            val calendar = Calendar.getInstance().apply { timeInMillis = startTime }
            
            val calendarDay = calendar.get(Calendar.DAY_OF_WEEK)
            val dayOfWeek = if (calendarDay == Calendar.SUNDAY) 7 else calendarDay - 1
            val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

            val record = FocusRecordEntity(
                id = UUID.randomUUID().toString(),
                missionId = missionId,
                actionId = actionId,
                startTime = startTime,
                durationSeconds = durationSeconds,
                isCompleted = isCompleted,
                dayOfWeek = dayOfWeek,
                hourOfDay = hourOfDay
            )
            analyticsDao.insertFocusRecord(record)
        }
    }

    override fun getTotalStartCount(): Flow<Int> {
        return analyticsDao.getTotalStartCountFlow()
            .flowOn(dispatcherProvider.io())
    }

    override suspend fun getCompletionRate(): Double {
        return withContext(dispatcherProvider.io()) {
            val total = analyticsDao.getTotalMissionsCount()
            if (total == 0) 0.0
            else {
                val completed = analyticsDao.getCompletedMissionsCount()
                completed.toDouble() / total
            }
        }
    }

    override fun getMostProcrastinatedMissions(): Flow<List<Mission>> {
        return analyticsDao.getMostProcrastinatedMissionsFlow()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatcherProvider.io())
    }

    override fun getLongestFocusDurationSeconds(): Flow<Long> {
        return analyticsDao.getLongestFocusDurationSecondsFlow()
            .map { it ?: 0L }
            .flowOn(dispatcherProvider.io())
    }

    override fun getBestWorkingHour(): Flow<Int?> {
        return analyticsDao.getAllFocusHoursFlow()
            .map { list ->
                if (list.isEmpty()) null
                else list.groupBy { it }.maxByOrNull { it.value.size }?.key
            }
            .flowOn(dispatcherProvider.io())
    }

    override fun getBestWorkingDayOfWeek(): Flow<Int?> {
        return analyticsDao.getAllFocusDaysOfWeekFlow()
            .map { list ->
                if (list.isEmpty()) null
                else list.groupBy { it }.maxByOrNull { it.value.size }?.key
            }
            .flowOn(dispatcherProvider.io())
    }

    override fun getFocusRecordsSince(since: Long): Flow<List<FocusRecord>> {
        return analyticsDao.getFocusRecordsSinceFlow(since)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatcherProvider.io())
    }

    private fun FocusRecordEntity.toDomain(): FocusRecord {
        return FocusRecord(
            id = id,
            missionId = missionId,
            actionId = actionId,
            startTime = startTime,
            durationSeconds = durationSeconds,
            isCompleted = isCompleted,
            dayOfWeek = dayOfWeek,
            hourOfDay = hourOfDay
        )
    }
}
