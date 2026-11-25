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
 * Повторяет контракт БД-реализации без Room: хранит каталог в [MutableStateFlow],
 * отдаёт подписки на весь список и на «Понравившиеся», умеет менять отметку
 * «Понравилось». Используется в юнит-тестах и как запасной источник данных.
 */
class DemoAppRepositoryImpl @Inject constructor() : AppRepository {

    private val _apps = MutableStateFlow(DemoAppDataSource.apps.toDomainList())

    override val apps: Flow<List<App>> = _apps.asStateFlow()

    override fun observeFavorites(): Flow<List<App>> =
        _apps.map { apps -> apps.filter(App::isFavorite) }

    override fun observeApp(appId: String): Flow<App?> =
        _apps.map { apps -> apps.firstOrNull { it.id == appId } }

    override suspend fun setFavorite(appId: String, isFavorite: Boolean) {
        _apps.update { apps ->
            apps.map { app ->
                if (app.id == appId) app.copy(isFavorite = isFavorite) else app
            }
        }
    }
}
