package ru.mobile.startapp.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.mobile.startapp.testing.FakeRatingApi

/** Репозиторий рейтингов — тонкая обёртка над [ru.mobile.startapp.data.source.network.RatingApi]. */
class EmulatedRatingRepositoryTest {

    @Test
    fun getRating_delegatesToApi() = runTest {
        val repository = EmulatedRatingRepository(FakeRatingApi(ratings = { 3.9f }))

        assertEquals(3.9f, repository.getRating("browser"))
    }

    @Test
    fun getRating_asksForExactId() = runTest {
        val api = FakeRatingApi()
        val repository = EmulatedRatingRepository(api)

        repository.getRating("gallery")

        assertEquals(listOf("gallery"), api.requestedIds)
    }
}
