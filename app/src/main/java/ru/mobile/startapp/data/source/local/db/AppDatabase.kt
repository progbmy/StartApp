package ru.mobile.startapp.data.source.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * База данных приложения: одна таблица каталога.
 *
 * `exportSchema = false` — схема ещё учебная. Версия 2: предыдущая ветка
 * с «Понравившимися» успела создать version-1 БД с колонкой `isFavorite`,
 * поэтому при обновлении поверх неё Room пересоздаёт таблицу
 * (см. `fallbackToDestructiveMigration` в [ru.mobile.startapp.di.DatabaseModule]).
 */
@Database(
    entities = [AppEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao
}
