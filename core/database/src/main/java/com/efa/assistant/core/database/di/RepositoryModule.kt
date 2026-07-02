package com.efa.assistant.core.database.di

import com.efa.assistant.core.database.repository.RoomAnalyticsRepository
import com.efa.assistant.core.database.repository.RoomMissionRepository
import com.efa.assistant.core.model.repository.AnalyticsRepository
import com.efa.assistant.core.model.repository.MissionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMissionRepository(
        roomMissionRepository: RoomMissionRepository
    ): MissionRepository

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        roomAnalyticsRepository: RoomAnalyticsRepository
    ): AnalyticsRepository
}
