package ru.mobile.startapp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import ru.mobile.startapp.data.mapper.toDomain
import ru.mobile.startapp.data.source.network.CatalogApiService
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.domain.repository.AppRepository
import java.io.IOException
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

    init {
        flow {
            val response = apiService.catalog()
            if (response.isSuccessful) {
                val apps = response.body()?.map(CatalogAppDto::toDomain) ?: emptyList()
                emit(apps)
            } else {
                emit(emptyList())
            }
        }
            .onStart { emit(_apps.value) }
            .catch { _ ->
                // Ошибка сети — отдаём пустой список, приложение не падает
                emit(emptyList())
            }
            .collect { _apps.value = it }
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
