package ru.mobile.startapp.data.source.network

import kotlin.math.abs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Эмуляция сети для рейтингов: диапазон значений, шаг и задержка ответа. */
class EmulatedRatingApiTest {

    private val api = EmulatedRatingApi()

    @Test
    fun getRating_isBetweenThreeAndFive() = runTest {
        repeat(100) { attempt ->
            val rating = api.getRating("browser")
            assertTrue("попытка $attempt: рейтинг $rating вне 3.0..5.0", rating in 3.0f..5.0f)
        }
    }

    @Test
    fun getRating_hasOneDecimalDigit() = runTest {
        repeat(100) { attempt ->
            val rating = api.getRating("browser")
            val fraction = abs((rating * 10f) % 1f)
            assertTrue("попытка $attempt: рейтинг $rating не кратен 0.1", fraction < 0.0001f)
        }
    }

    @Test
    fun getRating_spendsExactlyNetworkDelay() = runTest {
        val startedAt = testScheduler.currentTime

        api.getRating("browser")

        assertEquals(
            EmulatedRatingApi.NETWORK_DELAY_MS,
            testScheduler.currentTime - startedAt
        )
    }
}
