package com.efa.assistant.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.efa.assistant.core.database.dao.AnalyticsDao
import com.efa.assistant.core.database.dao.MissionDao
import com.efa.assistant.core.database.model.ActionEntity
import com.efa.assistant.core.database.model.FocusRecordEntity
import com.efa.assistant.core.database.model.MissionEntity

/**
 * EFA App 的 Room 本地数据库实例。
 * 离线优先，不保存任何 API Key 敏感信息。
 */
@Database(
    entities = [
        MissionEntity::class,
        ActionEntity::class,
        FocusRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EFADatabase : RoomDatabase() {

    /**
     * 任务与子行动步骤数据访问接口。
     */
    abstract fun missionDao(): MissionDao

    /**
     * 专注日志与行为统计数据访问接口。
     */
    abstract fun analyticsDao(): AnalyticsDao
}
