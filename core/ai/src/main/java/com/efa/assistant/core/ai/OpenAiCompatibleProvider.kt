package com.efa.assistant.core.ai

import com.efa.assistant.core.ai.network.ChatMessage
import com.efa.assistant.core.ai.network.OpenAiApiService
import com.efa.assistant.core.ai.network.OpenAiChatRequest

/**
 * 通用 OpenAI 规范接口提供商。
 * 用于 OpenAI 官方、DeepSeek 官方、阿里千问、本地 Ollama 以及其他第三方自研兼容接口。
 */
class OpenAiCompatibleProvider(
    private val apiService: OpenAiApiService,
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String
) : AIProvider {

    override suspend fun generateText(prompt: String): String {
        if (apiKey.isEmpty() && !baseUrl.contains("localhost") && !baseUrl.contains("10.0.2.2")) {
            throw IllegalArgumentException("API Key 不能为空")
        }

        val authHeader = "Bearer $apiKey"
        val request = OpenAiChatRequest(
            model = model,
            messages = listOf(ChatMessage(role = "user", content = prompt))
        )

        // 动态构建完整 Chat 路径，支持以 / 结尾或不以 / 结尾的 base_url
        val fullUrl = if (baseUrl.endsWith("/")) {
            "${baseUrl}chat/completions"
        } else {
            "$baseUrl/chat/completions"
        }

        val response = apiService.chatCompletions(fullUrl, authHeader, request)
        return response.choices.firstOrNull()?.message?.content 
            ?: throw Exception("AI 提供商未返回任何文本内容")
    }
}
