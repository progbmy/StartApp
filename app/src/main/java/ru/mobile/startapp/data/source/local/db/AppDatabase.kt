package ru.mobile.startapp.data.source.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Локальная БД каталога.
 *
 * Схема — одна таблица [AppEntity]. Версия 1: добавление поля `isFavorite`
 * к уже существующим полям приложения.
 */
@Database(
    entities = [AppEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {

        const val NAME = "startapp.db"
    }
}
