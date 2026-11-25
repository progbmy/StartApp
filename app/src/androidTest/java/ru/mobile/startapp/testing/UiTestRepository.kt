package ru.mobile.startapp.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.domain.repository.AppRepository
import ru.mobile.startapp.domain.repository.RatingRepository

/**
 * Каталог в памяти для сквозных UI-тестов: тот же контракт [AppRepository],
 * но без сети и Room — данные доступны с первого кадра.
 *
 * Hilt-модуль с этой подменой объявлен внутри `NavigationFlowTest.TestBindings`:
 * так он действует только на нужный тест-класс и не конфликтует с продовым
 * `AppModule` в остальных инструментальных тестах.
 */
class UiTestAppRepository(initial: List<App> = UiTestApps.catalog) : AppRepository {

    private val _apps = MutableStateFlow(initial)
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

/** Рейтинги без задержки: учебный замер на экране не тормозит сквозной тест. */
class InstantRatingRepository(private val rating: Float = 4.4f) : RatingRepository {

    override suspend fun getRating(id: String): Float = rating
}
