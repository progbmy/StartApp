package ru.mobile.startapp.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mobile.startapp.data.EmulatedRatingRepository
import ru.mobile.startapp.testing.FailingRatingApi
import ru.mobile.startapp.testing.FakeRatingApi

/**
 * Загрузка рейтингов: оба способа возвращают одинаковые данные,
 * отличаются только планированием корутин.
 */
class LoadRatingsUseCaseTest {

    private val ids = listOf("browser", "mail", "gallery")

    private fun useCase(api: FakeRatingApi) = LoadRatingsUseCase(EmulatedRatingRepository(api))

    private fun useCase(api: FailingRatingApi) = LoadRatingsUseCase(EmulatedRatingRepository(api))

    @Test
    fun loadSequentially_requestsIdsOneByOne_andReturnsEveryRating() = runTest {
        val api = FakeRatingApi(delayMs = 100, ratings = { id -> id.length.toFloat() })
        val useCase = useCase(api)

        val result = useCase.loadSequentially(ids)

        // порядок запросов совпадает с порядком id — это последовательный цикл
        assertEquals(ids, api.requestedIds)
        assertEquals(ids.map { it.length.toFloat() }, result.ratings.values.toList())
        assertEquals(ids.size, result.requests)
    }

    @Test
    fun loadInParallel_requestsEveryIdOnce() = runTest {
        val api = FakeRatingApi(delayMs = 100, ratings = { id -> id.length.toFloat() })
        val useCase = useCase(api)

        val result = useCase.loadInParallel(ids)

        assertNotNull(result.ratings)
        assertEquals(ids.toSet(), api.requestedIds.toSet())
        assertEquals(ids.size, result.requests)
        assertEquals(ids.associateWith { it.length.toFloat() }, result.ratings)
    }

    @Test
    fun emptyList_givesEmptyResultInBothModes() = runTest {
        val useCase = useCase(FakeRatingApi())

        val sequential = useCase.loadSequentially(emptyList())
        val parallel = useCase.loadInParallel(emptyList())

        assertTrue(sequential.ratings.isEmpty())
        assertEquals(0, sequential.requests)
        assertTrue(parallel.ratings.isEmpty())
        assertEquals(0, parallel.requests)
    }

    @Test
    fun loadSequentially_propagatesFailure() = runTest {
        val useCase = useCase(FailingRatingApi(failingId = "mail", delayMs = 10))

        val error = runCatching { useCase.loadSequentially(ids) }.exceptionOrNull()

        assertTrue("ожидали IllegalStateException, получили $error", error is IllegalStateException)
    }

    @Test
    fun loadInParallel_propagatesFailureFromAnyRequest() = runTest {
        // structured concurrency: падение одного async отменяет остальные
        val useCase = useCase(FailingRatingApi(failingId = "mail", delayMs = 50))

        val error = runCatching { useCase.loadInParallel(ids) }.exceptionOrNull()

        assertTrue("ожидали IllegalStateException, получили $error", error is IllegalStateException)
    }

    @Test
    fun parallelIsFasterThanSequential_onRealDelays() = runBlocking {
        val delayMs = 40L
        val useCase = useCase(FakeRatingApi(delayMs = delayMs))

        val sequential = useCase.loadSequentially(ids)
        val parallel = useCase.loadInParallel(ids)

        assertTrue(
            "последовательно ${sequential.elapsedMs} мс < ${ids.size * delayMs} мс",
            sequential.elapsedMs >= ids.size * delayMs - 10
        )
        assertTrue(
            "параллельно ${parallel.elapsedMs} мс должно быть меньше последовательных ${sequential.elapsedMs} мс",
            parallel.elapsedMs < sequential.elapsedMs
        )
    }

    @Test
    fun elapsedSeconds_isElapsedMillisInSeconds() {
        val result = RatingsLoadResult(ratings = emptyMap(), elapsedMs = 1_500, requests = 10)

        assertEquals(1.5f, result.elapsedSeconds, 0.0001f)
    }
}
