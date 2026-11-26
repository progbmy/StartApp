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
 * Hilt-модуль локального хранилища.
 *
 * Создаёт единственный экземпляр [AppDatabase] и предоставляет [AppDao];
 * тот же DAO получает [ru.mobile.startapp.data.NetworkAppRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "startapp.db"

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context = context,
            klass = AppDatabase::class.java,
            name = DATABASE_NAME
        )
            // после revert DZ-11 на устройстве могла остаться version-1 БД
            // с колонкой isFavorite — пересоздаём таблицу вместо падения
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideAppDao(database: AppDatabase): AppDao = database.appDao()
}
