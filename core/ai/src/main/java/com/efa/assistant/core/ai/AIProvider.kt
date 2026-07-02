package com.efa.assistant.core.ai

import androidx.annotation.Keep
import com.efa.assistant.core.model.Action
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Pluggable AI 提供商接口合约。
 * 默认提供基于 JSON 解析的任务智能拆分处理。
 */
interface AIProvider {

    /**
     * 调用大语言模型进行单次文本生成。
     * @param prompt 格式化后的提示词。
     * @return 接口返回的原始文本内容。
     */
    suspend fun generateText(prompt: String): String

    /**
     * 根据任务标题，调用 AI 并自动解析为标准的 Action 列表。
     */
    suspend fun splitTask(
        missionTitle: String,
        promptManager: PromptManager,
        extraInstructions: String = ""
    ): List<Action> {
        val instructions = if (extraInstructions.isNotBlank()) "用户附加需求：$extraInstructions" else ""
        val prompt = promptManager.getPrompt(PromptType.TASK_SPLIT, mapOf(
            "mission" to missionTitle,
            "extra_instructions" to instructions
        ))
        val rawResponse = generateText(prompt)
        return parseActionsJson(rawResponse)
    }

    /**
     * 解析 AI 返回的 JSON 字符串。
     * 具备格式纠错能力，即使大模型违背 Prompt 输出了 Markdown 代码块包裹（如 ```json ... ```），也能正常兼容解析。
     */
    private fun parseActionsJson(rawJson: String): List<Action> {
        val cleanedJson = cleanJson(rawJson)
        val gson = Gson()
        val payload = gson.fromJson(cleanedJson, TaskSplitPayload::class.java)
        return payload.actions.map {
            Action(
                id = UUID.randomUUID().toString(),
                title = it.title,
                durationMinutes = it.minutes,
                isCompleted = false
            )
        }
    }

    /**
     * 清理 Markdown 代码块包裹，只保留 JSON 原文。
     */
    private fun cleanJson(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```")) {
            // 清除开头的 ```json 或 ``` 标记
            cleaned = cleaned.replace(Regex("^```[a-zA-Z]*\\s*"), "")
            // 清除结尾的 ``` 标记
            cleaned = cleaned.replace(Regex("\\s*```$"), "")
        }
        return cleaned.trim()
    }
}

/**
 * 对应 Prompt 返回的 JSON 根结构。
 */
@Keep
private data class TaskSplitPayload(
    @SerializedName("actions") val actions: List<ActionPayload>
)

/**
 * 对应 JSON 中单个行动项的属性。
 */
@Keep
private data class ActionPayload(
    @SerializedName("title") val title: String,
    @SerializedName("minutes") val minutes: Int
)
