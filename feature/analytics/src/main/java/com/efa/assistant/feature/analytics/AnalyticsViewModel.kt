package com.efa.assistant.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efa.assistant.core.common.DispatcherProvider
import com.efa.assistant.core.common.UiState
import com.efa.assistant.core.common.stateInViewModel
import com.efa.assistant.core.model.FocusRecord
import com.efa.assistant.core.model.Mission
import com.efa.assistant.core.model.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DailyStats(
    val dayLabel: String,
    val focusMinutes: Int,
    val startCount: Int
)

data class BehaviorMetrics(
    val totalStartCount: Int,
    val completionRate: Double,
    val longestFocusMinutes: Int,
    val bestWorkingHour: Int?,
    val bestWorkingDayOfWeek: Int?,
    val currentStreak: Int,
    val recentDailyStats: List<DailyStats>,
    val mostProcrastinated: List<Mission>
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    // 定期刷新的完成率状态（由于需要异步计算总数，采用 StateFlow 进行存储和刷新）
    private val _completionRate = MutableStateFlow(0.0)
    val completionRate = _completionRate.asStateFlow()

    init {
        refreshCompletionRate()
    }

    fun refreshCompletionRate() {
        viewModelScope.launch(dispatcherProvider.io()) {
            _completionRate.value = analyticsRepository.getCompletionRate()
        }
    }

    /**
     * 获取最近7天的每日行为汇总
     */
    private val recentDailyStatsFlow = analyticsRepository.getFocusRecordsSince(
        System.currentTimeMillis() - 7 * 24 * 3600 * 1000L
    ).map { records ->
        val sdfLabel = SimpleDateFormat("E", Locale.getDefault()) // 周一, 周二...
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayMillis = 24 * 3600 * 1000L
        
        List(7) { i ->
            val targetTime = System.currentTimeMillis() - (6 - i) * dayMillis
            val targetDateStr = sdfDate.format(Date(targetTime))
            val dayLabel = sdfLabel.format(Date(targetTime))
            
            val dayRecords = records.filter { 
                sdfDate.format(Date(it.startTime)) == targetDateStr 
            }
            
            DailyStats(
                dayLabel = dayLabel,
                focusMinutes = (dayRecords.sumOf { it.durationSeconds } / 60).toInt(),
                startCount = dayRecords.size
            )
        }
    }

    /**
     * 观察自最开始以来的连续启动天数 (Streak)。
     * 专注系统核心设计：只奖励“开始启动行动”，不以“任务完成”施加压力。
     */
    private val currentStreakFlow = analyticsRepository.getFocusRecordsSince(0)
        .map { records -> calculateStreak(records) }

    /**
     * 聚合所有统计指标并暴露为一个 UiState 流。
     */
    val metricsState: StateFlow<UiState<BehaviorMetrics>> = combine(
        combine(
            analyticsRepository.getTotalStartCount(),
            analyticsRepository.getLongestFocusDurationSeconds(),
            analyticsRepository.getBestWorkingHour(),
            analyticsRepository.getBestWorkingDayOfWeek(),
            analyticsRepository.getMostProcrastinatedMissions()
        ) { a, b, c, d, e -> listOf(a, b, c, d, e) },
        recentDailyStatsFlow,
        currentStreakFlow,
        completionRate
    ) { p1, dailyStats, streak, rate ->
        BehaviorMetrics(
            totalStartCount = p1[0] as Int,
            longestFocusMinutes = (p1[1] as Int) / 60,
            bestWorkingHour = p1[2] as Int?,
            bestWorkingDayOfWeek = p1[3] as Int?,
            mostProcrastinated = p1[4] as List<Mission>,
            recentDailyStats = dailyStats,
            currentStreak = streak,
            completionRate = rate
        )
    }.map { 
        UiState.Success(it) as UiState<BehaviorMetrics> 
    }.catch { 
        emit(UiState.Error(it)) 
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
    )

    /**
     * 连续天数算法：计算用户今日或昨日有专注记录的连续天数。
     */
    private fun calculateStreak(records: List<FocusRecord>): Int {
        if (records.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        // 获取所有专注日期的唯一字符串列表，并按时间递增排序
        val activeDates = records.map { sdf.format(Date(it.startTime)) }.distinct().sorted()

        val todayStr = sdf.format(Date())
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            Date(System.currentTimeMillis() - 24 * 3600 * 1000L)
        )

        // 如果今天和昨天都没有开始行动，说明连续行动已中断
        if (todayStr !in activeDates && yesterdayStr !in activeDates) {
            return 0
        }

        var streak = 0
        // 如果今天有记录，则从今天向前追溯；否则从昨天向前追溯
        var checkTime = if (todayStr in activeDates) System.currentTimeMillis() else System.currentTimeMillis() - 24 * 3600 * 1000L

        while (true) {
            val checkStr = sdf.format(Date(checkTime))
            if (checkStr in activeDates) {
                streak++
                checkTime -= 24 * 3600 * 1000L // 往前推一天
            } else {
                break
            }
        }
        return streak
    }
}
