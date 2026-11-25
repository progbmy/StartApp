package ru.mobile.startapp.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
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
 * Реализация [AppRepository]: сеть + Room.
 *
 * Схема данных:
 * 1. `init` загружает каталог с сервера (GET /catalog) и складывает его в БД.
 * 2. Экраны подписываются на Flow из DAO и получают данные уже из БД —
 *    это единый источник истины (Single Source of Truth).
 * 3. Кнопка «Понравившиеся» вызывает [setFavorite]: DAO обновляет поле
 *    `isFavorite`, Room сам эмитит новое значение всем подписчикам.
 *
 * Если сеть недоступна, подписка продолжает отдавать сохранённый каталог.
 */
class NetworkAppRepository @Inject constructor(
    private val apiService: CatalogApiService,
    private val appDao: AppDao
) : AppRepository {

    /** Подписка на весь каталог из БД. */
    override val apps: Flow<List<App>> = appDao.observeApps().map { it.toDomainApps() }

    /** Подписка на «Понравившиеся» из БД. */
    override fun observeFavorites(): Flow<List<App>> =
        appDao.observeFavorites().map { it.toDomainApps() }

    /** Подписка на одно приложение из БД. */
    override fun observeApp(appId: String): Flow<App?> =
        appDao.observeApp(appId).map { it?.toDomain() }

    /** Метод обновления нового поля БД — «Понравилось». */
    override suspend fun setFavorite(appId: String, isFavorite: Boolean) {
        appDao.updateFavorite(appId = appId, isFavorite = isFavorite)
    }

    /**
     * Скоуп загрузки каталога: запрос — приостанавливающая функция,
     * поэтому вызывать его из `init` напрямую нельзя.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch { loadCatalog() }
    }

    /**
     * Загружает каталог с сервера и сохраняет его в БД.
     *
     * [AppDao.upsertCatalog] обновляет контентные поля, но не трогает
     * `isFavorite` — отметки «Понравилось» переживают перезагрузку каталога.
     * Неуспешный HTTP-ответ и сетевые ошибки не затирают сохранённые данные:
     * подписчики продолжают видеть кэш из БД. Отмена загрузки не превращается
     * в «ошибку сети»: [CancellationException] пробрасывается дальше.
     */
    suspend fun loadCatalog() {
        try {
            val response = apiService.catalog()
            if (!response.isSuccessful) {
                Log.w(TAG, "Каталог не загрузился: HTTP ${response.code()}")
                return
            }

            val catalog = response.body().orEmpty()
            if (catalog.isEmpty()) {
                Log.w(TAG, "Сервер вернул пустой каталог — оставляем данные из БД")
                return
            }

            appDao.upsertCatalog(
                catalog.mapIndexed { index, dto -> dto.toEntity(position = index) }
            )
            appDao.deleteMissing(catalog.map { it.id })
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            // Ошибка сети — данные из БД остаются, приложение не падает
            Log.w(TAG, "Каталог не загрузился: ${error.message}", error)
        }
    }

    private companion object {

        private const val TAG = "NetworkAppRepository"
    }
}
