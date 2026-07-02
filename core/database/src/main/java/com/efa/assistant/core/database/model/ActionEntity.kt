package com.efa.assistant.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.efa.assistant.core.model.Action

/**
 * 行动步骤 (Action) 数据库实体表。
 * 代表拆分后 1~5 分钟的具体动作。与 MissionEntity 为多对一关系。
 */
@Entity(
    tableName = "actions",
    foreignKeys = [
        ForeignKey(
            entity = MissionEntity::class,
            parentColumns = ["id"],
            childColumns = ["missionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["missionId"])]
)
data class ActionEntity(
    @PrimaryKey 
    val id: String,
    
    /**
     * 外键关联的 Mission ID
     */
    val missionId: String,
    
    /**
     * 步骤动作内容 (如：打开PPT)
     */
    val title: String,
    
    /**
     * 步骤执行时长（1~5分钟）
     */
    val durationMinutes: Int,
    
    /**
     * 步骤是否已完成
     */
    val isCompleted: Boolean = false,
    
    /**
     * 行动顺序，按此排序展示
     */
    val sequenceOrder: Int,
    
    /**
     * 完成时间戳
     */
    val completedAt: Long? = null
) {
    /**
     * 映射为 Domain 层的 Action 模型。
     */
    fun toDomain(): Action {
        return Action(
            id = id,
            title = title,
            durationMinutes = durationMinutes,
            isCompleted = isCompleted
        )
    }

    companion object {
        /**
         * 从 Domain 模型转换回 Database 实体模型。
         */
        fun fromDomain(domain: Action, missionId: String, sequenceOrder: Int): ActionEntity {
            return ActionEntity(
                id = domain.id,
                missionId = missionId,
                title = domain.title,
                durationMinutes = domain.durationMinutes,
                isCompleted = domain.isCompleted,
                sequenceOrder = sequenceOrder
            )
        }
    }
}
