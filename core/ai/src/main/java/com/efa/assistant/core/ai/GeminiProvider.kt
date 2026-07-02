package com.efa.assistant.core.ai

import com.efa.assistant.core.ai.network.GeminiApiService
import com.efa.assistant.core.ai.network.GeminiChatRequest
import com.efa.assistant.core.ai.network.GeminiContent
import com.efa.assistant.core.ai.network.GeminiPart

/**
 * Google Gemini Developer API 提供商。
 */
class GeminiProvider(
    private val apiService: GeminiApiService,
    private val apiKey: String,
    private val model: String = "gemini-1.5-flash"
) : AIProvider {

    override suspend fun generateText(prompt: String): String {
        if (apiKey.isEmpty()) {
            throw IllegalArgumentException("Gemini API Key 不能为空")
        }
        
        val request = GeminiChatRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )
        
        // 拼接模型名称，默认使用高速度、低延迟的 gemini-1.5-flash
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        
        val response = apiService.generateContent(
            url = url,
            apiKey = apiKey,
            request = request
        )
        
        val candidateText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        return candidateText ?: throw Exception("Gemini 未返回任何文本内容")
    }
}
