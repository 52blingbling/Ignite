package com.efa.assistant.core.ai.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * 针对 Anthropic Claude 的 API 请求服务。
 */
interface ClaudeApiService {
    @POST
    suspend fun getMessages(
        @Url url: String,
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: ClaudeChatRequest
    ): ClaudeChatResponse
}

data class ClaudeChatRequest(
    val model: String,
    val max_tokens: Int = 1024,
    val messages: List<ClaudeMessage>
)

data class ClaudeMessage(
    val role: String,
    val content: String
)

data class ClaudeChatResponse(
    val content: List<ClaudeContentBlock>
)

data class ClaudeContentBlock(
    val text: String
)
