package com.efa.assistant.core.ai

import com.efa.assistant.core.ai.network.ClaudeApiService
import com.efa.assistant.core.ai.network.ClaudeChatRequest
import com.efa.assistant.core.ai.network.ClaudeMessage

/**
 * Anthropic Claude 官方接口提供商。
 */
class ClaudeProvider(
    private val apiService: ClaudeApiService,
    private val apiKey: String,
    private val model: String = "claude-3-5-sonnet-20240620"
) : AIProvider {

    override suspend fun generateText(prompt: String): String {
        if (apiKey.isEmpty()) {
            throw IllegalArgumentException("Claude API Key 不能为空")
        }
        
        val request = ClaudeChatRequest(
            model = model,
            messages = listOf(ClaudeMessage(role = "user", content = prompt))
        )
        
        val response = apiService.getMessages(
            url = "https://api.anthropic.com/v1/messages",
            apiKey = apiKey,
            request = request
        )
        
        return response.content.firstOrNull()?.text 
            ?: throw Exception("Claude 未返回任何文本内容")
    }
}
