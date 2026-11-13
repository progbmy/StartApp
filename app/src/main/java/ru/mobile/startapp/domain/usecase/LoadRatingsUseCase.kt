package ru.mobile.startapp.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import ru.mobile.startapp.domain.repository.RatingRepository
import javax.inject.Inject
import kotlin.system.measureTimeMillis

/**
 * Результат загрузки пачки рейтингов: сами рейтинги и потраченное время.
 *
 * @param ratings рейтинги по id приложения
 * @param elapsedMs время выполнения в миллисекундах ([measureTimeMillis])
 * @param requests сколько запросов было выполнено
 */
data class RatingsLoadResult(
    val ratings: Map<String, Float>,
    val elapsedMs: Long,
    val requests: Int
) {

    /** Время в секундах — для показа на экране. */
    val elapsedSeconds: Float get() = elapsedMs / 1000f
}

/**
 * Загрузка рейтингов двумя способами — учебный замер последовательного
 * и параллельного выполнения корутин.
 *
 * Оба способа делают одно и то же число запросов и возвращают один и тот же
 * набор данных, отличается только планирование: последовательно один за другим
 * или все одновременно.
 */
class LoadRatingsUseCase @Inject constructor(
    private val ratingRepository: RatingRepository
) {

    /**
     * Последовательно: рейтинг следующего приложения запрашивается только после
     * того, как получен предыдущий. Итоговое время ≈ N × задержка сети.
     */
    suspend fun loadSequentially(ids: List<String>): RatingsLoadResult {
        val ratings = LinkedHashMap<String, Float>(ids.size)
        val elapsedMs = measureTimeMillis {
            for (id in ids) {
                ratings[id] = ratingRepository.getRating(id)
            }
        }
        return RatingsLoadResult(ratings = ratings, elapsedMs = elapsedMs, requests = ids.size)
    }

    /**
     * Параллельно без ограничений: на каждый id запускается [async] внутри
     * [coroutineScope], затем все результаты собираются [awaitAll].
     *
     * Время ≈ задержка одного запроса, т.к. `delay` не блокирует поток.
     * [coroutineScope] гарантирует structured concurrency: если один запрос упадёт
     * или всю загрузку отменить, остальные корутины будут отменены,
     * а исключение проброшено наружу.
     */
    suspend fun loadInParallel(ids: List<String>): RatingsLoadResult {
        lateinit var ratings: Map<String, Float>
        val elapsedMs = measureTimeMillis {
            ratings = coroutineScope {
                ids.map { id -> async { id to ratingRepository.getRating(id) } }
                    .awaitAll()
                    .toMap()
            }
        }
        return RatingsLoadResult(ratings = ratings, elapsedMs = elapsedMs, requests = ids.size)
    }
}
