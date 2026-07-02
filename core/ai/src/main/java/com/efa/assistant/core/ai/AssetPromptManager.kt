package com.efa.assistant.core.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 基于 Android Assets 的 Prompt 管理器实现。
 * 读取 assets/prompts 目录下的提示词，并缓存至内存中。
 */
@Singleton
class AssetPromptManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PromptManager {

    private val cache = mutableMapOf<PromptType, String>()

    override fun getPrompt(type: PromptType): String {
        return cache.getOrPut(type) {
            try {
                context.assets.open("prompts/${type.fileName}").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                        reader.readText()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 读取出错时使用默认降级策略，保证应用稳定性
                getDefaultPromptFallback(type)
            }
        }
    }

    override fun getPrompt(type: PromptType, params: Map<String, String>): String {
        var prompt = getPrompt(type)
        params.forEach { (key, value) ->
            prompt = prompt.replace("{$key}", value)
        }
        return prompt
    }

    /**
     * 当外部文件读取失败时的硬编码默认提示词兜底方案。
     */
    private fun getDefaultPromptFallback(type: PromptType): String {
        return when (type) {
            PromptType.TASK_SPLIT -> {
                "你是一名专业的执行教练。请将任务拆分为 1~5 分钟的微小具体步骤：\"{mission}\"。" +
                        "请严格返回以下格式的 JSON：\n" +
                        "{\n" +
                        "  \"actions\": [\n" +
                        "    { \"title\": \"行动名称\", \"minutes\": 2 }\n" +
                        "  ]\n" +
                        "}"
            }
            PromptType.COACH -> {
                "你是一名温和的 AI 执行教练。用户当前状态是：\"{status}\"。请用温和口吻给出一两句鼓励，重点是降低启动阻力。"
            }
            PromptType.SUMMARY -> {
                "你是一名有温度的 AI 伴侣。用户今日行动记录：\"{log}\"。请作简短的同理心日总结，强调尝试就是胜利。"
            }
            PromptType.REVIEW -> {
                "请评估并优化以下步骤：\"{actions}\"，确保每步在 1~5 分钟内，且动作具体。"
            }
            PromptType.PLANNING -> {
                "用户任务池：\"{tasks}\"。请帮助用户做“减法”，只留最重要的唯一任务（Current Mission），缓解焦虑。"
            }
            PromptType.EMOTION -> {
                "用户感到焦虑挫败：\"{emotion}\"。请接纳其情绪并告诉他们“先启动一小步”，降低其心理防御。"
            }
            PromptType.WEEKLY_REPORT -> {
                "根据本周启动次数：{start_count}，专注时间：{focus_time} 分钟，完成步骤：{completed_steps} 个。生成温暖正向的成长周报。"
            }
            PromptType.MONTHLY_REPORT -> {
                "根据本月启动次数：{start_count}，平均启动：{avg_start_time}。生成正向成长的成长月报。"
            }
        }
    }
}
