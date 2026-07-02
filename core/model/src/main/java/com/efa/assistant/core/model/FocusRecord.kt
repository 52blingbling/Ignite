package com.efa.assistant.core.model

/**
 * 专注日志的 Domain 层模型。
 * 脱离 SQLite / Room 依赖，规范行为数据分析。
 */
data class FocusRecord(
    val id: String,
    val missionId: String,
    val actionId: String,
    val startTime: Long,
    val durationSeconds: Long,
    val isCompleted: Boolean,
    val dayOfWeek: Int,
    val hourOfDay: Int
)
