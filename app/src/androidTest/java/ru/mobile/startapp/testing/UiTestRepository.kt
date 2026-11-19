package ru.mobile.startapp.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.domain.repository.AppRepository
import ru.mobile.startapp.domain.repository.RatingRepository

/**
 * Каталог в памяти для сквозных UI-тестов: тот же контракт [AppRepository],
 * но без сети — данные доступны с первого кадра.
 *
 * Hilt-модуль с этой подменой объявлен внутри `NavigationFlowTest.TestBindings`:
 * так он действует только на нужный тест-класс и не конфликтует с продовым
 * `AppModule` в остальных инструментальных тестах.
 */
class UiTestAppRepository(initial: List<App> = UiTestApps.catalog) : AppRepository {

    private val _apps = MutableStateFlow(initial)
    override val apps: StateFlow<List<App>> = _apps.asStateFlow()

    override fun appById(appId: String?): App? =
        appId?.let { id -> _apps.value.firstOrNull { it.id == id } }

    override fun similarApps(appId: String, limit: Int): List<App> {
        val current = appById(appId) ?: return emptyList()

        val (sameCategory, others) = _apps.value
            .filter { it.id != current.id }
            .partition { it.category == current.category }

        return (sameCategory + others).take(limit)
    }
}

/** Рейтинги без задержки: учебный замер на экране не тормозит сквозной тест. */
class InstantRatingRepository(private val rating: Float = 4.4f) : RatingRepository {

    override suspend fun getRating(id: String): Float = rating
}
