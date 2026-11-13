package ru.mobile.startapp.domain.repository

/**
 * Порт получения рейтинга приложения.
 *
 * Домен не знает, откуда берётся рейтинг: из сети, кэша или эмуляции.
 * Реализация живёт в слое данных — [ru.mobile.startapp.data.EmulatedRatingRepository].
 */
interface RatingRepository {

    /** Рейтинг приложения [id]. */
    suspend fun getRating(id: String): Float
}
