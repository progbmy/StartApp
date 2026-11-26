package ru.mobile.startapp.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.mobile.startapp.domain.App

/**
 * Порт (контракт) источника данных каталога приложений.
 *
 * Каталог и отдельная карточка отдаются потоками: ViewModel подписывается
 * на изменения, а экраны не запрашивают данные вручную. Room — единственный
 * источник правды, поэтому отметка «в вишлисте» прилетает подписчикам сама.
 *
 * Зависит только от доменных моделей — это правило Clean Architecture.
 */
interface AppRepository {

    /** Полный каталог приложений. */
    val apps: Flow<List<App>>

    /**
     * Поток одной карточки: любое изменение строки в БД обновляет значение.
     * null — приложения ещё нет в каталоге (загрузка) или его нет вовсе.
     */
    fun observeApp(appId: String): Flow<App?>

    /** Поток приложений, отмеченных «Понравившиеся». */
    fun observeFavorites(): Flow<List<App>>

    /** Добавляет приложение в вишлист или убирает его оттуда. */
    suspend fun toggleWishlist(appId: String)

    /** Обновляет отметку «Понравившиеся»: true — добавить, false — убрать. */
    suspend fun setFavorite(appId: String, isFavorite: Boolean)

    companion object {
        /** Сколько приложений показываем в блоке «Вам может понравиться». */
        const val SIMILAR_APPS_LIMIT = 3
    }
}
