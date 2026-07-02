package com.efa.assistant.core.ai

/**
 * Prompt 集中管理器接口。
 * 屏蔽底层的加载逻辑（如 Assets、SharedPreferences 或本地数据库），支持参数动态填充。
 */
interface PromptManager {
    /**
     * 获取未填充参数的原始提示词模板。
     * @param type 提示词类型。
     * @return 提示词文本。
     */
    fun getPrompt(type: PromptType): String

    /**
     * 获取参数填充后的提示词文本。
     * @param type 提示词类型。
     * @param params 键值对映射，用于替换模板中的 {key} 占位符。
     * @return 填充后的提示词文本。
     */
    fun getPrompt(type: PromptType, params: Map<String, String>): String
}
