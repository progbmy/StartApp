package ru.mobile.startapp.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.mobile.startapp.data.source.local.db.AppDao
import ru.mobile.startapp.data.source.local.db.AppDatabase
import javax.inject.Singleton

/**
 * Hilt-модуль локальной БД: синглтон Room-базы и DAO каталога.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.NAME
        ).build()

    @Provides
    fun provideAppDao(database: AppDatabase): AppDao = database.appDao()
}
