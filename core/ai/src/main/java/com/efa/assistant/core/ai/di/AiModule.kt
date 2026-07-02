package com.efa.assistant.core.ai.di

import com.efa.assistant.core.ai.AIProvider
import com.efa.assistant.core.ai.AssetPromptManager
import com.efa.assistant.core.ai.DelegatingAIProvider
import com.efa.assistant.core.ai.PromptManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindPromptManager(
        assetPromptManager: AssetPromptManager
    ): PromptManager

    @Binds
    @Singleton
    abstract fun bindAiProvider(
        delegatingAIProvider: DelegatingAIProvider
    ): AIProvider
}
