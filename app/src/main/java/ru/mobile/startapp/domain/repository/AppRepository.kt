package ru.mobile.startapp.domain.repository

import kotlinx.coroutines.flow.StateFlow
import ru.mobile.startapp.domain.App

/**
 * Порт (контракт) источника данных каталога приложений.
 *
 * Каталог отдаётся потоком, чтобы ViewModel подписывалась на изменения,
 * а экраны не запрашивали данные вручную.
 *
 * Зависит только от доменных моделей — это правило Clean Architecture.
 */
interface AppRepository {

    val apps: StateFlow<List<App>>

    fun appById(appId: String?): App?

    fun similarApps(appId: String, limit: Int = SIMILAR_APPS_LIMIT): List<App>

    companion object {
        /** Сколько приложений показываем в блоке «Вам может понравиться». */
        const val SIMILAR_APPS_LIMIT = 3
    }
}
