package com.efa.assistant.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.efa.assistant.core.model.Mission

/**
 * 带有子行动列表的任务模型（1对多 Room 模型结构）。
 */
data class MissionWithActions(
    @Embedded 
    val mission: MissionEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "missionId"
    )
    val actions: List<ActionEntity>
) {
    /**
     * 将包含关系转换成 Domain 层对应的 Mission（带 actions 列表）。
     */
    fun toDomain(): Mission {
        return mission.toDomain(
            actions = actions.sortedBy { it.sequenceOrder }.map { it.toDomain() }
        )
    }
}
