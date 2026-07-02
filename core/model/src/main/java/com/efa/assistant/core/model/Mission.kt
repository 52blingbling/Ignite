package com.efa.assistant.core.model

data class Mission(
    val id: String,
    val title: String,
    val durationMinutes: Int,
    val actions: List<Action> = emptyList(),
    val deferredCount: Int = 0
)

data class Action(
    val id: String,
    val title: String,
    val durationMinutes: Int,
    val isCompleted: Boolean = false
)
