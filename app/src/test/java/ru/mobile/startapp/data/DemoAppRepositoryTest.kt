package ru.mobile.startapp.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mobile.startapp.data.mapper.toDomainList
import ru.mobile.startapp.data.source.local.DemoAppDataSource

/** Демо-репозиторий в памяти: каталог, подписки и отметка «Понравилось». */
class DemoAppRepositoryTest {

    private val repository = DemoAppRepositoryImpl()

    private val demoApps: List<ru.mobile.startapp.domain.App> =
        DemoAppDataSource.apps.toDomainList()

    @Test
    fun apps_exposesFullDemoCatalog() = runTest {
        assertEquals(demoApps, repository.apps.first())
    }

    @Test
    fun observeApp_returnsAppByIdOrNull() = runTest {
        assertEquals("Браузер", repository.observeApp("browser").first()?.name)
        assertNull(repository.observeApp("unknown").first())
    }

    @Test
    fun observeFavorites_isEmptyInitially() = runTest {
        assertTrue(repository.observeFavorites().first().isEmpty())
    }

    @Test
    fun setFavorite_addsAndRemovesAppFromFavorites() = runTest {
        repository.setFavorite("browser", true)

        val favorites = repository.observeFavorites().first()
        assertEquals(listOf("browser"), favorites.map { it.id })
        assertTrue(favorites.single().isFavorite)

        repository.setFavorite("browser", false)

        assertTrue(repository.observeFavorites().first().isEmpty())
    }

    @Test
    fun setFavorite_doesNotTouchOtherApps() = runTest {
        repository.setFavorite("browser", true)

        val browser = repository.observeApp("browser").first()
        val mail = repository.observeApp("mail").first()

        assertTrue(browser!!.isFavorite)
        assertTrue(!mail!!.isFavorite)
        assertEquals(demoApps.size, repository.apps.first().size)
    }

    @Test
    fun setFavorite_unknownApp_isNoOp() = runTest {
        repository.setFavorite("unknown", true)

        assertEquals(demoApps, repository.apps.first())
    }
}
