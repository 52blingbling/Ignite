package com.efa.assistant.core.database.repository

import com.efa.assistant.core.common.DispatcherProvider
import com.efa.assistant.core.database.dao.MissionDao
import com.efa.assistant.core.database.model.ActionEntity
import com.efa.assistant.core.database.model.MissionEntity
import com.efa.assistant.core.model.Action
import com.efa.assistant.core.model.Mission
import com.efa.assistant.core.model.repository.MissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 任务数据仓储的 Room 实现类。
 */
@Singleton
class RoomMissionRepository @Inject constructor(
    private val missionDao: MissionDao,
    private val dispatcherProvider: DispatcherProvider
) : MissionRepository {

    override fun getCurrentMission(): Flow<Mission?> {
        return missionDao.getCurrentMissionFlow()
            .map { it?.toDomain() }
            .flowOn(dispatcherProvider.io())
    }

    override fun getAllMissions(): Flow<List<Mission>> {
        return missionDao.getAllMissionsFlow()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatcherProvider.io())
    }

    override suspend fun getMissionById(id: String): Mission? {
        return withContext(dispatcherProvider.io()) {
            missionDao.getMissionById(id)?.toDomain()
        }
    }

    override suspend fun createMission(title: String, durationMinutes: Int, actions: List<Action>) {
        withContext(dispatcherProvider.io()) {
            val missionId = UUID.randomUUID().toString()
            val missionEntity = MissionEntity(
                id = missionId,
                title = title,
                durationMinutes = durationMinutes,
                createdAt = System.currentTimeMillis()
            )
            val actionEntities = actions.mapIndexed { index, action ->
                ActionEntity(
                    id = if (action.id.isEmpty()) UUID.randomUUID().toString() else action.id,
                    missionId = missionId,
                    title = action.title,
                    durationMinutes = action.durationMinutes,
                    isCompleted = action.isCompleted,
                    sequenceOrder = index
                )
            }
            missionDao.saveMissionWithActions(missionEntity, actionEntities)
        }
    }

    override suspend fun updateActionCompletion(actionId: String, isCompleted: Boolean) {
        withContext(dispatcherProvider.io()) {
            val completedAt = if (isCompleted) System.currentTimeMillis() else null
            missionDao.updateActionStatus(actionId, isCompleted, completedAt)
        }
    }

    override suspend fun updateMissionCompletion(missionId: String, isCompleted: Boolean) {
        withContext(dispatcherProvider.io()) {
            val completedAt = if (isCompleted) System.currentTimeMillis() else null
            missionDao.updateMissionStatus(missionId, isCompleted, completedAt)
        }
    }

    override suspend fun incrementDeferredCount(missionId: String) {
        withContext(dispatcherProvider.io()) {
            missionDao.incrementDeferredCount(missionId)
        }
    }

    override suspend fun deleteMission(missionId: String) {
        withContext(dispatcherProvider.io()) {
            missionDao.deleteMission(missionId)
        }
    }
}
