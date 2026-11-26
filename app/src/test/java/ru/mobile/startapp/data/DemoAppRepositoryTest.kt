package ru.mobile.startapp.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mobile.startapp.data.mapper.toDomainList
import ru.mobile.startapp.data.source.local.DemoAppDataSource
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.domain.repository.AppRepository
import ru.mobile.startapp.domain.similarApps

class DemoAppRepositoryTest {

    private val repository = DemoAppRepositoryImpl()

    private val demoApps: List<App> =
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
    fun similarApps_doesNotContainCurrentApp() = runTest {
        val similar = repository.apps.first().similarApps("gallery")
        assertTrue(similar.none { it.id == "gallery" })
    }

    @Test
    fun similarApps_putsSameCategoryFirst() = runTest {
        val similar = repository.apps.first().similarApps("gallery")
        assertEquals("music", similar.first().id)
    }

    @Test
    fun similarApps_respectsLimit() = runTest {
        val apps = repository.apps.first()
        assertEquals(AppRepository.SIMILAR_APPS_LIMIT, apps.similarApps("gallery").size)
        assertEquals(2, apps.similarApps("gallery", limit = 2).size)
    }

    @Test
    fun similarApps_unknownAppGivesEmptyList() = runTest {
        assertTrue(repository.apps.first().similarApps("unknown").isEmpty())
    }

    @Test
    fun toggleWishlist_marksOnlySelectedApp_andFlipsBack() = runTest {
        assertFalse(repository.observeApp("gallery").first()!!.isInWishlist)

        repository.toggleWishlist("gallery")

        val gallery = repository.observeApp("gallery").first()!!
        assertTrue(gallery.isInWishlist)
        // остальные поля не пострадали
        assertEquals(demoApps.first { it.id == "gallery" }.name, gallery.name)
        assertFalse(repository.observeApp("music").first()!!.isInWishlist)

        repository.toggleWishlist("gallery")
        assertFalse(repository.observeApp("gallery").first()!!.isInWishlist)
    }

    @Test
    fun toggleWishlist_unknownApp_doesNothing() = runTest {
        repository.toggleWishlist("unknown")

        assertEquals(demoApps, repository.apps.first())
    }
}
