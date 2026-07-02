package com.efa.assistant.core.model

data class Mission(
    val id: String,
    val title: String,
    val durationMinutes: Int,
    val actions: List<Action> = emptyList(),
    val deferredCount: Int = 0,
    val createdAt: Long = 0L,
    val isCompleted: Boolean = false
)

data class Action(
    val id: String,
    val title: String,
    val durationMinutes: Int,
    val isCompleted: Boolean = false
)
