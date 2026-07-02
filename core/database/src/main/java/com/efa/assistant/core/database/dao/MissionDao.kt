package com.efa.assistant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.efa.assistant.core.database.model.ActionEntity
import com.efa.assistant.core.database.model.MissionEntity
import com.efa.assistant.core.database.model.MissionWithActions
import kotlinx.coroutines.flow.Flow

/**
 * 任务与行动步骤的 Room DAO。
 */
@Dao
interface MissionDao {

    /**
     * 获取当前最新的未完成任务（主页聚焦展示的“Current Mission”）。
     */
    @Transaction
    @Query("SELECT * FROM missions WHERE isCompleted = 0 ORDER BY createdAt DESC LIMIT 1")
    fun getCurrentMissionFlow(): Flow<MissionWithActions?>

    /**
     * 根据 ID 获取具体任务及其行动详情。
     */
    @Transaction
    @Query("SELECT * FROM missions WHERE id = :missionId")
    suspend fun getMissionById(missionId: String): MissionWithActions?

    /**
     * 观察所有任务的列表。
     */
    @Transaction
    @Query("SELECT * FROM missions ORDER BY createdAt DESC")
    fun getAllMissionsFlow(): Flow<List<MissionWithActions>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: MissionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActions(actions: List<ActionEntity>)

    /**
     * 事务性保存任务及其拆分的行动步骤。
     */
    @Transaction
    suspend fun saveMissionWithActions(mission: MissionEntity, actions: List<ActionEntity>) {
        insertMission(mission)
        insertActions(actions)
    }

    @Update
    suspend fun updateMission(mission: MissionEntity)

    /**
     * 更新单个行动步骤的完成状态。
     */
    @Query("UPDATE actions SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :actionId")
    suspend fun updateActionStatus(actionId: String, isCompleted: Boolean, completedAt: Long?)

    /**
     * 标记整个任务完成。
     */
    @Query("UPDATE missions SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :missionId")
    suspend fun updateMissionStatus(missionId: String, isCompleted: Boolean, completedAt: Long?)

    /**
     * 增加该任务的拖延（延期）次数计数。
     */
    @Query("UPDATE missions SET deferredCount = deferredCount + 1 WHERE id = :missionId")
    suspend fun incrementDeferredCount(missionId: String)

    /**
     * 删除任务，对应的 actions 将因外键 CASCADE 级联删除。
     */
    @Query("DELETE FROM missions WHERE id = :missionId")
    suspend fun deleteMission(missionId: String)
}
