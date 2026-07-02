package com.efa.assistant.feature.analytics

import com.efa.assistant.core.common.DispatcherProvider
import com.efa.assistant.core.common.UiState
import com.efa.assistant.core.model.FocusRecord
import com.efa.assistant.core.model.repository.AnalyticsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class StreakCalculationTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val testDispatcherProvider = object : DispatcherProvider {
        override fun main(): CoroutineDispatcher = testDispatcher
        override fun io(): CoroutineDispatcher = testDispatcher
        override fun default(): CoroutineDispatcher = testDispatcher
    }

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayMillis = 24 * 3600 * 1000L

    @Test
    fun testMetricsState_withEmptyRecords_shouldReturnZeroStreak() = runTest {
        val mockRepo = mockk<AnalyticsRepository>(relaxed = true)
        
        every { mockRepo.getTotalStartCount() } returns flowOf(0)
        every { mockRepo.getLongestFocusDurationSeconds() } returns flowOf(0L)
        every { mockRepo.getBestWorkingHour() } returns flowOf(null)
        every { mockRepo.getBestWorkingDayOfWeek() } returns flowOf(null)
        every { mockRepo.getMostProcrastinatedMissions() } returns flowOf(emptyList())
        every { mockRepo.getFocusRecordsSince(any()) } returns flowOf(emptyList())

        val viewModel = AnalyticsViewModel(mockRepo, testDispatcherProvider)
        val state = viewModel.metricsState.first { it is UiState.Success }

        assertTrue(state is UiState.Success)
        val metrics = (state as UiState.Success).data
        assertEquals(0, metrics.currentStreak)
    }

    @Test
    fun testMetricsState_withConsecutiveDailyRecords_shouldReturnCorrectStreak() = runTest {
        val mockRepo = mockk<AnalyticsRepository>(relaxed = true)

        val todayTime = System.currentTimeMillis()
        val yesterdayTime = todayTime - dayMillis
        val twoDaysAgoTime = todayTime - 2 * dayMillis

        val mockRecords = listOf(
            createDummyRecord(todayTime),
            createDummyRecord(yesterdayTime),
            createDummyRecord(twoDaysAgoTime)
        )

        every { mockRepo.getTotalStartCount() } returns flowOf(3)
        every { mockRepo.getFocusRecordsSince(any()) } returns flowOf(mockRecords)

        val viewModel = AnalyticsViewModel(mockRepo, testDispatcherProvider)
        val state = viewModel.metricsState.first { it is UiState.Success }

        assertTrue(state is UiState.Success)
        val metrics = (state as UiState.Success).data
        assertEquals(3, metrics.currentStreak)
    }

    @Test
    fun testMetricsState_withGapInRecords_shouldBreakStreak() = runTest {
        val mockRepo = mockk<AnalyticsRepository>(relaxed = true)

        val todayTime = System.currentTimeMillis()
        // 昨天没有行动
        val twoDaysAgoTime = todayTime - 2 * dayMillis

        val mockRecords = listOf(
            createDummyRecord(todayTime),
            createDummyRecord(twoDaysAgoTime)
        )

        every { mockRepo.getTotalStartCount() } returns flowOf(2)
        every { mockRepo.getFocusRecordsSince(any()) } returns flowOf(mockRecords)

        val viewModel = AnalyticsViewModel(mockRepo, testDispatcherProvider)
        val state = viewModel.metricsState.first { it is UiState.Success }

        assertTrue(state is UiState.Success)
        val metrics = (state as UiState.Success).data
        // 昨天断了，所以只有今天连续计 1 天
        assertEquals(1, metrics.currentStreak)
    }

    @Test
    fun testMetricsState_withRecordsOnlyInPast_shouldReturnZeroStreak() = runTest {
        val mockRepo = mockk<AnalyticsRepository>(relaxed = true)

        // 仅在 3 天前和 4 天前有专注，今天和昨天都中断了
        val threeDaysAgoTime = System.currentTimeMillis() - 3 * dayMillis
        val fourDaysAgoTime = System.currentTimeMillis() - 4 * dayMillis

        val mockRecords = listOf(
            createDummyRecord(threeDaysAgoTime),
            createDummyRecord(fourDaysAgoTime)
        )

        every { mockRepo.getTotalStartCount() } returns flowOf(2)
        every { mockRepo.getFocusRecordsSince(any()) } returns flowOf(mockRecords)

        val viewModel = AnalyticsViewModel(mockRepo, testDispatcherProvider)
        val state = viewModel.metricsState.first { it is UiState.Success }

        assertTrue(state is UiState.Success)
        val metrics = (state as UiState.Success).data
        // 今天昨天都没开始行动，所以连续天数为 0
        assertEquals(0, metrics.currentStreak)
    }

    private fun createDummyRecord(time: Long): FocusRecord {
        return FocusRecord(
            id = "id_${time}",
            missionId = "m_id",
            actionId = "a_id",
            startTime = time,
            durationSeconds = 300L,
            isCompleted = true,
            dayOfWeek = 1,
            hourOfDay = 10
        )
    }
}
