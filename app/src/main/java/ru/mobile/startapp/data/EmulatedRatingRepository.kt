package ru.mobile.startapp.data

import ru.mobile.startapp.data.source.network.RatingApi
import ru.mobile.startapp.domain.repository.RatingRepository
import javax.inject.Inject

/**
 * Реализация [RatingRepository] поверх [RatingApi].
 *
 * Сейчас [RatingApi] эмулирует сеть задержкой `delay`, поэтому заменить
 * эмуляцию на настоящий HTTP-запрос можно, не трогая домен и представление.
 */
class EmulatedRatingRepository @Inject constructor(
    private val ratingApi: RatingApi
) : RatingRepository {

    override suspend fun getRating(id: String): Float = ratingApi.getRating(id)
}
