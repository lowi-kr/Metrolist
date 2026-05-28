package com.arubr.smsvcodes.di

import android.content.Context
import com.arubr.smsvcodes.db.DatabaseDao
import com.arubr.smsvcodes.ui.screens.wrapped.WrappedAudioService
import com.arubr.smsvcodes.ui.screens.wrapped.WrappedManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WrappedModule {
    @Provides
    @Singleton
    fun provideWrappedManager(
        databaseDao: DatabaseDao,
        @ApplicationContext context: Context,
    ): WrappedManager = WrappedManager(databaseDao, context)

    @Provides
    @Singleton
    fun provideWrappedAudioService(@ApplicationContext context: Context): WrappedAudioService = WrappedAudioService(context)
}
