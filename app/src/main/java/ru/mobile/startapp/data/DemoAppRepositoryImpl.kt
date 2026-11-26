package ru.mobile.startapp.data

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.mobile.startapp.data.mapper.toDomainList
import ru.mobile.startapp.data.source.local.DemoAppDataSource
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.domain.repository.AppRepository

/**
 * Реализация [AppRepository] на демо-данных в памяти.
 *
 * Слой данных:
 * 1. Читает жёстко заданные DTO из [DemoAppDataSource].
 * 2. Конвертирует их через [ru.mobile.startapp.data.mapper.AppMapper]
 *    в доменные модели.
 * 3. Отдаёт доменные модели через [MutableStateFlow] представлению.
 *
 * Используется в юнит-тестах и как пример альтернативного источника.
 * Вишлист здесь тоже живёт в состоянии репозитория и переживает пересоздание
 * ViewModel, но не перезапуск процесса — этим отличается от Room-реализации.
 */
class DemoAppRepositoryImpl @Inject constructor() : AppRepository {

    private val _apps = MutableStateFlow(DemoAppDataSource.apps.toDomainList())

    override val apps: Flow<List<App>> = _apps.asStateFlow()

    override fun observeApp(appId: String): Flow<App?> =
        _apps.map { apps -> apps.firstOrNull { it.id == appId } }

    override fun observeFavorites(): Flow<List<App>> =
        _apps.map { apps -> apps.filter { it.isFavorite } }

    override suspend fun toggleWishlist(appId: String) {
        _apps.update { apps ->
            apps.map { app ->
                if (app.id == appId) app.copy(isInWishlist = !app.isInWishlist) else app
            }
        }
    }

    override suspend fun setFavorite(appId: String, isFavorite: Boolean) {
        _apps.update { apps ->
            apps.map { app ->
                if (app.id == appId) app.copy(isFavorite = isFavorite) else app
            }
        }
    }
}
