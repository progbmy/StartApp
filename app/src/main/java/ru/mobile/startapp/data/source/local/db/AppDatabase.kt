package ru.mobile.startapp.data.source.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * База данных приложения: одна таблица каталога.
 *
 * `exportSchema = false` — схема ещё учебная. Версия 3: добавлена колонка
 * `isFavorite` («Понравившиеся»); version 2 отличалась от отменённой version-1
 * колонкой `isInWishlist`, поэтому при обновлении поверх старой БД Room
 * пересоздаёт таблицу (см. `fallbackToDestructiveMigration`
 * в [ru.mobile.startapp.di.DatabaseModule]).
 */
@Database(
    entities = [AppEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao
}
