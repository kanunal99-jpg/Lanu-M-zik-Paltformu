package com.example.di

import android.content.Context
import com.example.data.MusicRepository
import com.example.service.AudioPlayerController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.android.components.ViewModelComponent

/** One repository and one player controller per screen ViewModel, never global Activity references. */
@Module
@InstallIn(ViewModelComponent::class)
object LanuViewModelModule {
    @Provides
    @ViewModelScoped
    fun provideMusicRepository(@ApplicationContext context: Context): MusicRepository =
        MusicRepository(context)

    @Provides
    @ViewModelScoped
    fun provideAudioPlayerController(@ApplicationContext context: Context): AudioPlayerController =
        AudioPlayerController(context)
}
