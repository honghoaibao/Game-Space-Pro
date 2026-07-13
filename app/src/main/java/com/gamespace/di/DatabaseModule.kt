package com.gamespace.di

import android.content.Context
import androidx.room.Room
import com.gamespace.logging.LogDatabase
import com.gamespace.packagemanager.GameDatabase
import com.gamespace.protection.ProtectedAppDatabase
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
    fun provideLogDatabase(@ApplicationContext context: Context): LogDatabase =
        Room.databaseBuilder(context, LogDatabase::class.java, "gamespace_log.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideGameDatabase(@ApplicationContext context: Context): GameDatabase =
        Room.databaseBuilder(context, GameDatabase::class.java, "gamespace_games.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideProtectedAppDatabase(@ApplicationContext context: Context): ProtectedAppDatabase =
        Room.databaseBuilder(context, ProtectedAppDatabase::class.java, "gamespace_protected_apps.db")
            .fallbackToDestructiveMigration()
            .build()
}
