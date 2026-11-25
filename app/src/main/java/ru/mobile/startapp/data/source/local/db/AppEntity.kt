package ru.mobile.startapp.data.source.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity каталога приложений в Room.
 *
 * Таблица — единственный источник истины для UI: каталог из сети складывается
 * сюда, а экраны подписываются на [AppDao] и реактивно получают изменения.
 *
 * [isFavorite] — новое поле ДЗ-11 («Понравившиеся»). Оно живёт только в БД:
 * при обновлении каталога из сети значение не затирается (см. [AppDao.upsertCatalog]).
 */
@Entity(tableName = AppEntity.TABLE)
data class AppEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val developer: String,
    val version: String,
    val size: String,
    val fullDescription: String,
    val isFavorite: Boolean = false,
    /** Порядок приложения в каталоге: сервер отдаёт список в нужной последовательности. */
    val position: Int = 0
) {
    companion object {

        const val TABLE = "apps"
    }
}
