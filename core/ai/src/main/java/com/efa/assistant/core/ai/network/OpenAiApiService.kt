package com.efa.assistant.core.ai.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * 通用 OpenAI 兼容的 Retrofit 请求服务。
 */
interface OpenAiApiService {
    /**
     * 发送聊天补全请求。
     * @param url 具体的相对或绝对端点 URL (如 "chat/completions")。
     * @param authHeader Authorization 请求头，格式为 "Bearer API_KEY"。
     * @param request 请求体。
     */
    @POST
    suspend fun chatCompletions(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse
}

data class OpenAiChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.5f
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class OpenAiChatResponse(
    val choices: List<ChatChoice>
)

data class ChatChoice(
    val message: ChatMessage
)
