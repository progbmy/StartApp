package ru.mobile.startapp.data

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import ru.mobile.startapp.data.source.network.CatalogApiService
import ru.mobile.startapp.data.source.network.CatalogAppDto
import ru.mobile.startapp.domain.repository.AppRepository

/**
 * Сетевой репозиторий каталога: успешный ответ, HTTP-ошибка, падение сети и отмена.
 *
 * [NetworkAppRepository.loadCatalog] вызывается напрямую — иначе загрузку из
 * `init` нельзя дождаться детерминированно (она идёт на `Dispatchers.IO`).
 */
class NetworkAppRepositoryTest {

    private val catalog = listOf(
        CatalogAppDto("browser", "Браузер", "Поиск в интернете", "Интернет"),
        CatalogAppDto("mail", "Почта", "Письма и уведомления", "Связь"),
        CatalogAppDto("gallery", "Галерея", "Фото и видео", "Медиа"),
        CatalogAppDto("music", "Музыка", "Плеер и плейлисты", "Медиа"),
        CatalogAppDto("navigator", "Навигатор", "Маршруты и пробки", "Путешествия")
    )

    @Test
    fun loadCatalog_success_mapsAndCachesApps() = runTest {
        val repository = NetworkAppRepository(FakeCatalogApi { Response.success(catalog) })

        repository.loadCatalog()

        assertEquals(catalog.map { it.id }, repository.apps.value.map { it.id })
        assertEquals("Браузер", repository.appById("browser")?.name)
        assertEquals("Разработчик не указан", repository.apps.value.first().developer)
    }

    @Test
    fun loadCatalog_httpError_givesEmptyCatalog() = runTest {
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.error(500, ResponseBody.create(null, "boom")) }
        )

        repository.loadCatalog()

        assertTrue(repository.apps.value.isEmpty())
        assertNull(repository.appById("browser"))
    }

    @Test
    fun loadCatalog_networkFailure_givesEmptyCatalogWithoutCrash() = runTest {
        val repository = NetworkAppRepository(FakeCatalogApi { throw IOException("нет сети") })

        repository.loadCatalog()

        assertTrue(repository.apps.value.isEmpty())
    }

    @Test
    fun loadCatalog_cancellation_isRethrown() = runTest {
        val repository = NetworkAppRepository(
            FakeCatalogApi { throw CancellationException("отмена загрузки") }
        )

        val error = runCatching { repository.loadCatalog() }.exceptionOrNull()

        assertTrue("ожидали CancellationException, получили $error", error is CancellationException)
    }

    @Test
    fun emptyBody_givesEmptyCatalog() = runTest {
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.success(emptyList<CatalogAppDto>()) }
        )

        repository.loadCatalog()

        assertTrue(repository.apps.value.isEmpty())
    }

    @Test
    fun appById_returnsNullForNullOrUnknownId() = runTest {
        val repository = NetworkAppRepository(FakeCatalogApi { Response.success(catalog) })
        repository.loadCatalog()

        assertNull(repository.appById(null))
        assertNull(repository.appById("unknown"))
    }

    @Test
    fun similarApps_putsSameCategoryFirst_andExcludesCurrent() = runTest {
        val repository = NetworkAppRepository(FakeCatalogApi { Response.success(catalog) })
        repository.loadCatalog()

        val similar = repository.similarApps("gallery", AppRepository.SIMILAR_APPS_LIMIT)

        assertEquals(listOf("music", "browser", "mail"), similar.map { it.id })
        assertTrue(similar.none { it.id == "gallery" })
    }

    @Test
    fun similarApps_forUnknownApp_givesEmptyList() = runTest {
        val repository = NetworkAppRepository(FakeCatalogApi { Response.success(catalog) })
        repository.loadCatalog()

        assertTrue(repository.similarApps("unknown").isEmpty())
    }

    /** Retrofit-сервис без сети: ответ задаётся лямбдой конкретного теста. */
    private class FakeCatalogApi(
        private val catalogHandler: suspend () -> Response<List<CatalogAppDto>>
    ) : CatalogApiService {

        override suspend fun catalog(): Response<List<CatalogAppDto>> = catalogHandler()

        override suspend fun appById(appId: String): Response<CatalogAppDto> =
            Response.error(404, ResponseBody.create(null, "not found"))
    }
}
