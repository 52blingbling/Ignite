package com.efa.assistant.core.database.di

import android.content.Context
import androidx.room.Room
import com.efa.assistant.core.database.EFADatabase
import com.efa.assistant.core.database.dao.AnalyticsDao
import com.efa.assistant.core.database.dao.MissionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideEFADatabase(
        @ApplicationContext context: Context
    ): EFADatabase {
        return Room.databaseBuilder(
            context,
            EFADatabase::class.java,
            "efa_database.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideMissionDao(database: EFADatabase): MissionDao {
        return database.missionDao()
    }

    @Provides
    @Singleton
    fun provideAnalyticsDao(database: EFADatabase): AnalyticsDao {
        return database.analyticsDao()
    }
}
