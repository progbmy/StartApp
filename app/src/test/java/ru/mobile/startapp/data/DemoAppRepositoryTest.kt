package ru.mobile.startapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoAppRepositoryTest {

    private val repository = DemoAppRepository()

    @Test
    fun appById_returnsAppByIdOrNull() {
        assertEquals("Браузер", repository.appById("browser")?.name)
        assertNull(repository.appById("unknown"))
        assertNull(repository.appById(null))
    }

    @Test
    fun apps_exposesFullDemoCatalog() {
        assertEquals(demoApps, repository.apps.value)
    }

    @Test
    fun similarApps_doesNotContainCurrentApp() {
        val similar = repository.similarApps("gallery")
        assertTrue(similar.none { it.id == "gallery" })
    }

    @Test
    fun similarApps_putsSameCategoryFirst() {
        val similar = repository.similarApps("gallery")
        assertEquals("music", similar.first().id)
    }

    @Test
    fun similarApps_respectsLimit() {
        assertEquals(AppRepository.SIMILAR_APPS_LIMIT, repository.similarApps("gallery").size)
        assertEquals(2, repository.similarApps("gallery", limit = 2).size)
    }

    @Test
    fun similarApps_unknownAppGivesEmptyList() {
        assertTrue(repository.similarApps("unknown").isEmpty())
    }
}
