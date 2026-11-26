package ru.mobile.startapp.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.mobile.startapp.data.mapper.toDomain
import ru.mobile.startapp.data.mapper.toDomainApps
import ru.mobile.startapp.data.mapper.toEntity
import ru.mobile.startapp.data.source.local.db.AppDao
import ru.mobile.startapp.data.source.network.CatalogApiService
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.domain.repository.AppRepository
import javax.inject.Inject

/**
 * Реализация [AppRepository]: Retrofit загружает каталог, Room хранит его
 * и является единственным источником правды.
 *
 * Поток данных:
 * 1. `init` запускает [loadCatalog] — ответ сервера пишется в таблицу `apps`;
 * 2. UI подписывается на [apps] и [observeApp] (Flow из DAO);
 * 3. [toggleWishlist] меняет только поле `isInWishlist` — Room присылает
 *    новое значение всем подписчикам, и экран обновляется сам.
 */
class NetworkAppRepository @Inject constructor(
    private val apiService: CatalogApiService,
    private val appDetailsDao: AppDao
) : AppRepository {

    /** Каталог из Room: любое изменение таблицы прилетает подписчикам. */
    override val apps: Flow<List<App>> =
        appDetailsDao.observeApps().map { entities -> entities.toDomainApps() }

    /**
     * Скоуп загрузки каталога: `collect` — приостанавливающая функция,
     * поэтому вызывать её из `init` напрямую нельзя.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch { loadCatalog() }
    }

    /** Поток карточки: изменения строки в Room сразу видны ViewModel. */
    override fun observeApp(appId: String): Flow<App?> =
        appDetailsDao.getAppDetails(appId).map { entity -> entity?.toDomain() }

    /**
     * Добавляет приложение в вишлист или убирает его оттуда.
     *
     * Читаем текущее значение из БД, а не из UI-состояния: источник правды —
     * строка Room, поэтому двойной клик не «залипает» в одном состоянии.
     */
    override suspend fun toggleWishlist(appId: String) {
        val currentEntity = appDetailsDao.getAppDetails(appId).first()
        currentEntity?.let { entity ->
            appDetailsDao.updateWishlistStatus(appId, !entity.isInWishlist)
        }
    }

    /**
     * Загружает каталог с сервера и записывает его в Room.
     *
     * Неуспешный HTTP-ответ, пустой ответ и сетевая ошибка оставляют прежние
     * строки в БД — приложение не падает и не теряет отметки вишлиста.
     * Отмена загрузки не превращается в «ошибку сети»:
     * [CancellationException] пробрасывается дальше.
     */
    suspend fun loadCatalog() {
        try {
            val response = apiService.catalog()
            if (!response.isSuccessful) {
                Log.w(TAG, "Каталог не загрузился: HTTP ${response.code()}")
                return
            }

            val catalog = response.body().orEmpty()
            if (catalog.isEmpty()) return

            // position сохраняет порядок сервера; upsert не затирает isInWishlist
            val entities = catalog.mapIndexed { index, dto -> dto.toEntity(position = index) }
            appDetailsDao.upsertCatalog(entities)
            appDetailsDao.deleteMissing(entities.map { it.id })
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.w(TAG, "Каталог не загрузился: ${error.message}", error)
        }
    }

    private companion object {

        private const val TAG = "NetworkAppRepository"
    }
}
