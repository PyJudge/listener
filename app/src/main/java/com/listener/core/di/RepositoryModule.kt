package com.listener.core.di

import com.listener.data.repository.PodcastRepositoryImpl
import com.listener.data.repository.TranscriptionRepositoryImpl
import com.listener.domain.repository.PodcastRepository
import com.listener.domain.repository.TranscriptionRepository
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
    abstract fun bindTranscriptionRepository(
        impl: TranscriptionRepositoryImpl
    ): TranscriptionRepository

    @Binds
    @Singleton
    abstract fun bindPodcastRepository(
        impl: PodcastRepositoryImpl
    ): PodcastRepository
}
