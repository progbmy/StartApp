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
 * Подписки:
 * - [observeApps] — весь каталог;
 * - [observeFavorites] — только «Понравившиеся»;
 * - [observeApp] — одно приложение по id (карточка).
 *
 * Обновление нового поля — [updateFavorite]. Вызывается из репозитория
 * по нажатию кнопки в интерфейсе; Room после UPDATE сам эмитит новые
 * значения во все активные Flow.
 */
@Dao
abstract class AppDao {

    @Query("SELECT * FROM ${AppEntity.TABLE} ORDER BY position ASC")
    abstract fun observeApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM ${AppEntity.TABLE} WHERE isFavorite = 1 ORDER BY position ASC")
    abstract fun observeFavorites(): Flow<List<AppEntity>>

    @Query("SELECT * FROM ${AppEntity.TABLE} WHERE id = :appId LIMIT 1")
    abstract fun observeApp(appId: String): Flow<AppEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertNew(apps: List<AppEntity>)

    /**
     * Обновляет контентные поля уже существующей строки.
     *
     * `isFavorite` намеренно не входит в SET: перезагрузка каталога из сети
     * не должна сбрасывать отметки «Понравилось».
     */
    @Query(
        """
        UPDATE ${AppEntity.TABLE} SET
            name = :name,
            description = :description,
            category = :category,
            developer = :developer,
            version = :version,
            size = :size,
            fullDescription = :fullDescription,
            position = :position
        WHERE id = :id
        """
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

    /** Метод обновления нового поля — отметка «Понравилось». */
    @Query("UPDATE ${AppEntity.TABLE} SET isFavorite = :isFavorite WHERE id = :appId")
    abstract suspend fun updateFavorite(appId: String, isFavorite: Boolean)

    /** Удаляет приложения, которых больше нет в каталоге сервера. */
    @Query("DELETE FROM ${AppEntity.TABLE} WHERE id NOT IN (:ids)")
    abstract suspend fun deleteMissing(ids: List<String>)

    /**
     * Сохраняет каталог из сети: новые строки добавляет, существующим
     * обновляет контент, `isFavorite` не трогает.
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
