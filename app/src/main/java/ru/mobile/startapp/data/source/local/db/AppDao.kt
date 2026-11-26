package ru.mobile.startapp.data.source.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO каталога приложений.
 *
 * Каталог читается потоками: Room сам присылает новое значение после любого
 * изменения таблицы, поэтому UI обновляется без ручного перезапроса.
 */
@Dao
abstract class AppDao {

    /** Весь каталог в порядке сервера. */
    @Query("SELECT * FROM apps ORDER BY position ASC")
    abstract fun observeApps(): Flow<List<AppEntity>>

    /** Одна карточка по id; null — приложения ещё нет в БД. */
    @Query("SELECT * FROM apps WHERE id = :id LIMIT 1")
    abstract fun getAppDetails(id: String): Flow<AppEntity?>

    /**
     * Новые приложения каталога. IGNORE, а не REPLACE: существующая строка
     * сохраняет пользовательскую отметку [AppEntity.isInWishlist].
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertNew(apps: List<AppEntity>)

    /**
     * Обновляет только поля, которые приходят с сервера.
     * `isInWishlist` здесь отсутствует намеренно — вишлист живёт только в БД.
     */
    @Query(
        "UPDATE apps SET name = :name, description = :description, category = :category, " +
            "developer = :developer, version = :version, size = :size, " +
            "fullDescription = :fullDescription, position = :position WHERE id = :id"
    )
    abstract suspend fun updateContent(
        id: String,
        name: String,
        description: String,
        category: String,
        developer: String,
        version: String,
        size: String,
        fullDescription: String,
        position: Int
    )

    /** Переключает отметку «в вишлисте». */
    @Query("UPDATE apps SET isInWishlist = :isInWishlist WHERE id = :id")
    abstract suspend fun updateWishlistStatus(id: String, isInWishlist: Boolean)

    /** Удаляет приложения, которых больше нет в каталоге сервера. */
    @Query("DELETE FROM apps WHERE id NOT IN (:ids)")
    abstract suspend fun deleteMissing(ids: List<String>)

    /**
     * Записывает каталог одной транзакцией: вставка новых строк + обновление
     * контента существующих. Подписчики не увидят наполовину обновлённую таблицу.
     */
    @Transaction
    open suspend fun upsertCatalog(apps: List<AppEntity>) {
        insertNew(apps)
        apps.forEach { app ->
            updateContent(
                id = app.id,
                name = app.name,
                description = app.description,
                category = app.category,
                developer = app.developer,
                version = app.version,
                size = app.size,
                fullDescription = app.fullDescription,
                position = app.position
            )
        }
    }
}
