package com.efa.assistant.core.ai

import android.content.SharedPreferences
import com.efa.assistant.core.ai.network.ClaudeApiService
import com.efa.assistant.core.ai.network.GeminiApiService
import com.efa.assistant.core.ai.network.OpenAiApiService
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 动态代理分发提供商。
 * 从 EncryptedSharedPreferences 读取当前的引擎类型及 API 密钥，
 * 动态组装具体子提供商实现并进行调用。实现“切换引擎免改业务代码”的目标。
 */
@Singleton
class DelegatingAIProvider @Inject constructor(
    private val openAiApiService: OpenAiApiService,
    private val claudeApiService: ClaudeApiService,
    private val geminiApiService: GeminiApiService,
    @Named("encrypted_prefs") private val sharedPreferences: SharedPreferences
) : AIProvider {

    override suspend fun generateText(prompt: String): String {
        val providerStr = sharedPreferences.getString("ai_provider", "Gemini") ?: "Gemini"
        val apiKey = sharedPreferences.getString("api_key", "") ?: ""

        val delegate: AIProvider = when (providerStr) {
            "Gemini" -> {
                GeminiProvider(geminiApiService, apiKey, "gemini-1.5-flash")
            }
            "DeepSeek" -> {
                OpenAiCompatibleProvider(openAiApiService, "https://api.deepseek.com/v1/", "deepseek-chat", apiKey)
            }
            "OpenAI" -> {
                OpenAiCompatibleProvider(openAiApiService, "https://api.openai.com/v1/", "gpt-4o-mini", apiKey)
            }
            "Claude" -> {
                ClaudeProvider(claudeApiService, apiKey, "claude-3-5-sonnet-20240620")
            }
            "Qwen" -> {
                OpenAiCompatibleProvider(openAiApiService, "https://dashscope.aliyuncs.com/compatible-mode/v1/", "qwen-turbo", apiKey)
            }
            "Ollama" -> {
                // 10.0.2.2 为 Android 模拟器环境下指向开发宿主主机的 IP 地址
                OpenAiCompatibleProvider(openAiApiService, "http://10.0.2.2:11434/v1/", "gemma", apiKey)
            }
            "Local LLM" -> {
                OpenAiCompatibleProvider(openAiApiService, "http://10.0.2.2:8080/v1/", "custom", apiKey)
            }
            else -> {
                GeminiProvider(geminiApiService, apiKey, "gemini-1.5-flash")
            }
        }
        
        return delegate.generateText(prompt)
    }
}
