package ru.mobile.startapp.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.mobile.startapp.domain.App

/**
 * Порт (контракт) источника данных каталога приложений.
 *
 * Каталог отдаётся потоком из БД: ViewModel подписывается на изменения
 * и не запрашивает данные вручную. Методы подписки:
 * - [apps] — весь каталог;
 * - [observeFavorites] — «Понравившиеся»;
 * - [observeApp] — одно приложение (карточка).
 *
 * Изменение нового поля «Понравилось» — [setFavorite].
 * Зависит только от доменных моделей — это правило Clean Architecture.
 */
interface AppRepository {

    /** Подписка на весь каталог из БД. */
    val apps: Flow<List<App>>

    /** Подписка на приложения, отмеченные «Понравилось». */
    fun observeFavorites(): Flow<List<App>>

    /** Подписка на приложение по id: null, если такого нет. */
    fun observeApp(appId: String): Flow<App?>

    /** Обновляет новое поле БД: отметку «Понравилось». */
    suspend fun setFavorite(appId: String, isFavorite: Boolean)

    companion object {
        /** Сколько приложений показываем в блоке «Вам может понравиться». */
        const val SIMILAR_APPS_LIMIT = 3
    }
}
