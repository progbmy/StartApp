package ru.mobile.startapp.data.source.network

import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Аналог `AppApi` для рейтингов: получение рейтинга приложения по его id.
 *
 * Интерфейс описывает контракт сети, реализация — эмуляция:
 * настоящего сервера рейтингов пока нет, поэтому запрос «висит»
 * [EmulatedRatingApi.NETWORK_DELAY_MS] миллисекунд, как при реальной сети.
 */
interface RatingApi {

    /** Рейтинг приложения [id] (0.0..5.0). */
    suspend fun getRating(id: String): Float
}

/**
 * Эмуляция сетевого запроса за рейтингом.
 *
 * `delay` — приостановка, а не блокировка потока: пока один запрос «ждёт сеть»,
 * корутина освобождает поток, поэтому параллельные запросы (async) выполняются
 * одновременно и весь пакет занимает примерно столько же времени, сколько один запрос.
 */
class EmulatedRatingApi @Inject constructor() : RatingApi {

    override suspend fun getRating(id: String): Float {
        delay(NETWORK_DELAY_MS) // эмуляция сетевого запроса
        return (30..50).random() / 10f // 3.0..5.0
    }

    companion object {

        /** Эмулируемая задержка ответа сети, мс. */
        const val NETWORK_DELAY_MS = 1_500L
    }
}
