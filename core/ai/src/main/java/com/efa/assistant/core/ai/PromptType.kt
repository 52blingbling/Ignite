package com.efa.assistant.core.ai

/**
 * AI 提示词类型。
 * 每一个枚举值对应 assets/prompts/ 目录下具体的一个提示词模版文件。
 */
enum class PromptType(val fileName: String) {
    /**
     * 任务拆分
     */
    TASK_SPLIT("task_split_prompt.txt"),

    /**
     * 每日总结
     */
    SUMMARY("summary_prompt.txt"),

    /**
     * 执行教练
     */
    COACH("coach_prompt.txt"),

    /**
     * 任务审查
     */
    REVIEW("review_prompt.txt"),

    /**
     * 任务规划/减法
     */
    PLANNING("planning_prompt.txt"),

    /**
     * 情绪分析与引导
     */
    EMOTION("emotion_prompt.txt"),

    /**
     * 周报分析
     */
    WEEKLY_REPORT("weekly_report_prompt.txt"),

    /**
     * 月报分析
     */
    MONTHLY_REPORT("monthly_report_prompt.txt")
}
