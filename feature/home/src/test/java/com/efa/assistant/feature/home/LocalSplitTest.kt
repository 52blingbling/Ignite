package com.efa.assistant.feature.home

import android.content.SharedPreferences
import com.efa.assistant.core.ai.AIProvider
import com.efa.assistant.core.ai.PromptManager
import com.efa.assistant.core.common.DispatcherProvider
import com.efa.assistant.core.model.Action
import com.efa.assistant.core.model.repository.MissionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalSplitTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val testDispatcherProvider = object : DispatcherProvider {
        override fun main(): CoroutineDispatcher = testDispatcher
        override fun io(): CoroutineDispatcher = testDispatcher
        override fun default(): CoroutineDispatcher = testDispatcher
    }

    @Test
    fun testStartNewMission_withShortDuration_shouldGenerateSingleAction() = runTest {
        val mockRepo = mockk<MissionRepository>(relaxed = true)
        val mockAiProvider = mockk<AIProvider>()
        val mockPromptManager = mockk<PromptManager>()
        val mockPrefs = mockk<SharedPreferences>()

        // 模拟没有配置 API Key，从而进入本地拆分机制
        every { mockPrefs.getString("api_key", "") } returns ""
        every { mockPrefs.getString("ai_provider", "Gemini") } returns "Gemini"
        every { mockRepo.getCurrentMission() } returns emptyFlow()

        val viewModel = HomeViewModel(
            missionRepository = mockRepo,
            aiProvider = mockAiProvider,
            promptManager = mockPromptManager,
            sharedPreferences = mockPrefs,
            dispatcherProvider = testDispatcherProvider
        )

        val titleSlot = slot<String>()
        val durationSlot = slot<Int>()
        val actionsSlot = slot<List<Action>>()

        coEvery {
            mockRepo.createMission(
                capture(titleSlot),
                capture(durationSlot),
                capture(actionsSlot)
            )
        } returns Unit

        viewModel.startNewMissionDraft("写一封简短邮件", 5)

        val draft = viewModel.draftMission.first { it != null }!!
        assertEquals("写一封简短邮件", draft.title)
        assertEquals(5, draft.durationMinutes)
        assertEquals(1, draft.actions.size)
        assertEquals("立即开始行动: 写一封简短邮件", draft.actions[0].title)
        assertEquals(5, draft.actions[0].durationMinutes)

        viewModel.confirmDraft()

        assertEquals("写一封简短邮件", titleSlot.captured)
        assertEquals(5, durationSlot.captured)
        assertEquals(1, actionsSlot.captured.size)
        assertEquals("立即开始行动: 写一封简短邮件", actionsSlot.captured[0].title)
        assertEquals(5, actionsSlot.captured[0].durationMinutes)
    }

    @Test
    fun testStartNewMission_withLongDuration_shouldSegmentIntoMultipleActions() = runTest {
        val mockRepo = mockk<MissionRepository>(relaxed = true)
        val mockAiProvider = mockk<AIProvider>()
        val mockPromptManager = mockk<PromptManager>()
        val mockPrefs = mockk<SharedPreferences>()

        every { mockPrefs.getString("api_key", "") } returns ""
        every { mockPrefs.getString("ai_provider", "Gemini") } returns "Gemini"
        every { mockRepo.getCurrentMission() } returns emptyFlow()

        val viewModel = HomeViewModel(
            missionRepository = mockRepo,
            aiProvider = mockAiProvider,
            promptManager = mockPromptManager,
            sharedPreferences = mockPrefs,
            dispatcherProvider = testDispatcherProvider
        )

        val actionsSlot = slot<List<Action>>()

        coEvery {
            mockRepo.createMission(
                any(),
                any(),
                capture(actionsSlot)
            )
        } returns Unit

        viewModel.startNewMissionDraft("完成月度财务表", 15)

        val draft = viewModel.draftMission.first { it != null }!!
        val actions = draft.actions
        // 15分钟本地自动分配为：
        // 1. 准备工作：打开相关工具/软件 (2分钟)
        // 2. 开展第 1 步行动：处理分项内容 (5分钟)
        // 3. 开展第 2 步行动：处理分项内容 (5分钟)
        // 4. 开展第 3 步行动：处理分项内容 (3分钟)
        // 5. 整理结果，存盘/保存并关闭 (2分钟)
        assertEquals(5, actions.size)
        assertEquals("准备工作：打开相关工具/软件", actions[0].title)
        assertEquals(2, actions[0].durationMinutes)
        assertEquals("开展第 1 步行动：处理分项内容", actions[1].title)
        assertEquals(5, actions[1].durationMinutes)
        assertEquals("开展第 2 步行动：处理分项内容", actions[2].title)
        assertEquals(5, actions[2].durationMinutes)
        assertEquals("开展第 3 步行动：处理分项内容", actions[3].title)
        assertEquals(3, actions[3].durationMinutes)
        assertEquals("整理结果，存盘/保存并关闭", actions[4].title)
        assertEquals(2, actions[4].durationMinutes)

        viewModel.confirmDraft()
        assertEquals(5, actionsSlot.captured.size)
    }
}
