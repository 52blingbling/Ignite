package com.efa.assistant.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.efa.assistant.core.model.Mission

/**
 * 任务 (Mission) 数据库实体表。
 * 代表用户发起的大目标（如：准备年中汇报）。
 */
@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey 
    val id: String,
    
    /**
     * 任务标题
     */
    val title: String,
    
    /**
     * 预估耗时（分钟）
     */
    val durationMinutes: Int,
    
    /**
     * 创建时间戳
     */
    val createdAt: Long,
    
    /**
     * 完成时间戳，未完成为 null
     */
    val completedAt: Long? = null,
    
    /**
     * 是否完成
     */
    val isCompleted: Boolean = false,
    
    /**
     * 延期/拖延次数，用于计算“最容易拖延的任务”
     */
    val deferredCount: Int = 0
) {
    /**
     * 映射为 Domain 层的纯 Kotlin 数据模型。
     */
    fun toDomain(actions: List<com.efa.assistant.core.model.Action> = emptyList()): Mission {
        return Mission(
            id = id,
            title = title,
            durationMinutes = durationMinutes,
            actions = actions,
            deferredCount = deferredCount,
            createdAt = createdAt,
            isCompleted = isCompleted
        )
    }

    companion object {
        /**
         * 从 Domain 模型转换回 Database 实体模型。
         */
        fun fromDomain(domain: Mission, defaultCreatedAt: Long = System.currentTimeMillis()): MissionEntity {
            return MissionEntity(
                id = domain.id,
                title = domain.title,
                durationMinutes = domain.durationMinutes,
                createdAt = if (domain.createdAt > 0L) domain.createdAt else defaultCreatedAt,
                isCompleted = domain.isCompleted,
                deferredCount = domain.deferredCount
            )
        }
    }
}
