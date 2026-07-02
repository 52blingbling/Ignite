package com.efa.assistant.core.model.repository

import com.efa.assistant.core.model.Action
import com.efa.assistant.core.model.Mission
import kotlinx.coroutines.flow.Flow

/**
 * 任务数据仓储接口 (Domain Layer Contract)。
 */
interface MissionRepository {
    /**
     * 观察当前正在执行的未完成任务（主页展示）。
     */
    fun getCurrentMission(): Flow<Mission?>

    /**
     * 观察历史任务列表。
     */
    fun getAllMissions(): Flow<List<Mission>>

    /**
     * 根据 ID 获取具体任务详情。
     */
    suspend fun getMissionById(id: String): Mission?

    /**
     * 创建一个新任务及其子行动。
     */
    suspend fun createMission(title: String, durationMinutes: Int, actions: List<Action>)

    /**
     * 标记子行动步骤的完成状态。
     */
    suspend fun updateActionCompletion(actionId: String, isCompleted: Boolean)

    /**
     * 标记整个任务的完成状态。
     */
    suspend fun updateMissionCompletion(missionId: String, isCompleted: Boolean)

    /**
     * 增加任务的拖延（推迟）次数。
     */
    suspend fun incrementDeferredCount(missionId: String)

    /**
     * 删除指定的任务。
     */
    suspend fun deleteMission(missionId: String)
}
