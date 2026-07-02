package com.efa.assistant.core.ai

import com.efa.assistant.core.model.Action
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AIProviderTest {

    // Mock 提供商，用于测试默认接口方法
    private class MockAIProvider(private val mockResponse: String) : AIProvider {
        override suspend fun generateText(prompt: String): String {
            return mockResponse
        }
    }

    private val dummyPromptManager = object : PromptManager {
        override fun getPrompt(type: PromptType): String = ""
        override fun getPrompt(type: PromptType, params: Map<String, String>): String = ""
    }

    @Test
    fun testSplitTask_withCleanJson_shouldParseCorrectly() = runBlocking {
        val cleanJson = """
            {
              "actions": [
                { "title": "打开代码编辑器", "minutes": 2 },
                { "title": "编写单元测试", "minutes": 5 }
              ]
            }
        """.trimIndent()

        val provider = MockAIProvider(cleanJson)
        val actions = provider.splitTask("测试任务", dummyPromptManager)

        assertEquals(2, actions.size)
        assertEquals("打开代码编辑器", actions[0].title)
        assertEquals(2, actions[0].durationMinutes)
        assertEquals("编写单元测试", actions[1].title)
        assertEquals(5, actions[1].durationMinutes)
    }

    @Test
    fun testSplitTask_withMarkdownJsonBlock_shouldCleanAndParseCorrectly() = runBlocking {
        val markdownJson = """
            ```json
            {
              "actions": [
                { "title": "分析数据", "minutes": 4 }
              ]
            }
            ```
        """.trimIndent()

        val provider = MockAIProvider(markdownJson)
        val actions = provider.splitTask("测试任务", dummyPromptManager)

        assertEquals(1, actions.size)
        assertEquals("分析数据", actions[0].title)
        assertEquals(4, actions[0].durationMinutes)
    }

    @Test
    fun testSplitTask_withPlainMarkdownBlock_shouldCleanAndParseCorrectly() = runBlocking {
        val plainMarkdown = """
            ```
            {
              "actions": [
                { "title": "保存存盘", "minutes": 1 }
              ]
            }
            ```
        """.trimIndent()

        val provider = MockAIProvider(plainMarkdown)
        val actions = provider.splitTask("测试任务", dummyPromptManager)

        assertEquals(1, actions.size)
        assertEquals("保存存盘", actions[0].title)
        assertEquals(1, actions[0].durationMinutes)
    }
}
