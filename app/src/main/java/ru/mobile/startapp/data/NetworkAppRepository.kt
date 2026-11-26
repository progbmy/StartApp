package ru.mobile.startapp.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.mobile.startapp.data.mapper.toDomain
import ru.mobile.startapp.data.source.network.CatalogApiService
import ru.mobile.startapp.data.source.network.CatalogAppDto
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.domain.repository.AppRepository
import javax.inject.Inject

/**
 * Реализация [AppRepository], использующая Retrofit для загрузки каталога с сервера.
 *
 * Запросы:
 * - GET /catalog — полный каталог приложений
 * - GET /{id} — отдельное приложение (заготовка)
 *
 * Данные кэшируются в [StateFlow] после первой загрузки.
 */
class NetworkAppRepository @Inject constructor(
    private val apiService: CatalogApiService
) : AppRepository {

    private val _apps = MutableStateFlow<List<App>>(emptyList())
    override val apps: StateFlow<List<App>> = _apps.asStateFlow()

    /**
     * Скоуп загрузки каталога: `collect` — приостанавливающая функция,
     * поэтому вызывать её из `init` напрямую нельзя.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch { loadCatalog() }
    }

    /**
     * Загружает каталог с сервера и кэширует результат в [apps].
     *
     * Неуспешный HTTP-ответ и сетевые ошибки дают пустой каталог — приложение
     * не падает. Отмена загрузки (например, при закрытии приложения) не
     * превращается в «ошибку сети»: [CancellationException] пробрасывается дальше.
     */
    suspend fun loadCatalog() {
        try {
            val response = apiService.catalog()
            _apps.value = if (response.isSuccessful) {
                response.body()?.map(CatalogAppDto::toDomain).orEmpty()
            } else {
                emptyList()
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            // Ошибка сети — отдаём пустой список, приложение не падает
            Log.w(TAG, "Каталог не загрузился: ${error.message}", error)
            _apps.value = emptyList()
        }
    }

    private companion object {

        private const val TAG = "NetworkAppRepository"
    }

    override fun appById(appId: String?): App? {
        if (appId == null) return null
        return _apps.value.find { it.id == appId }
    }

    override fun similarApps(appId: String, limit: Int): List<App> {
        val current = appById(appId) ?: return emptyList()

        val (sameCategory, others) = _apps.value
            .filter { it.id != current.id }
            .partition { it.category == current.category }

        return (sameCategory + others).take(limit)
    }
}
