package com.efa.assistant.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 专注历史记录表。
 * 用于本地分析引擎计算“总启动次数”、“最长专注时间”、“最佳工作时段/星期/时间段”。
 */
@Entity(tableName = "focus_records")
data class FocusRecordEntity(
    @PrimaryKey 
    val id: String,
    
    /**
     * 关联的 Mission ID
     */
    val missionId: String,
    
    /**
     * 关联的 Action ID
     */
    val actionId: String,
    
    /**
     * 专注开始时间戳
     */
    val startTime: Long,
    
    /**
     * 专注时长（秒数）
     */
    val durationSeconds: Long,
    
    /**
     * 该专注时间段是否被顺利完成。
     * 如果用户在专注期间主动取消，则为 false（表示中断）。
     */
    val isCompleted: Boolean,
    
    /**
     * 星期几 (1 = 星期一, ..., 7 = 星期日)，供 SQL 分组直接查询。
     */
    val dayOfWeek: Int,
    
    /**
     * 开始专注所在的小时 (0 ~ 23)，供 SQL 分组直接查询。
     */
    val hourOfDay: Int
)
