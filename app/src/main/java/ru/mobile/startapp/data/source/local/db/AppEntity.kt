package ru.mobile.startapp.data.source.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Строка таблицы `apps` — кэш каталога в Room.
 *
 * [isInWishlist] — пользовательская отметка «в вишлисте»: сеть её не присылает,
 * поэтому обновление каталога не должно её затирать (см. [AppDao.upsertCatalog]).
 * [isFavorite] — отдельная отметка «Понравившиеся», тоже живёт только в БД.
 * [position] хранит порядок приложений из ответа сервера, чтобы после перезапуска
 * список не пересортировался по id.
 */
@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val developer: String,
    val version: String,
    val size: String,
    val fullDescription: String,
    val isInWishlist: Boolean = false,
    val isFavorite: Boolean = false,
    val position: Int = 0
)
