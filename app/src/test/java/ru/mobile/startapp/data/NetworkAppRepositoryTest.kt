package ru.mobile.startapp.data

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import ru.mobile.startapp.data.source.network.CatalogApiService
import ru.mobile.startapp.data.source.network.CatalogAppDto
import ru.mobile.startapp.testing.FakeAppDao

/**
 * Репозиторий «сеть + БД»: успешный ответ и сохранение в DAO, HTTP-ошибка,
 * падение сети, отмена и сохранность отметки «Понравилось» при перезагрузке.
 *
 * [NetworkAppRepository.loadCatalog] вызывается напрямую — иначе загрузку из
 * `init` нельзя дождаться детерминированно (она идёт на `Dispatchers.IO`).
 * Вместо Room подставляется [FakeAppDao] в памяти.
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
    fun loadCatalog_success_savesAndMapsApps() = runTest {
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.success(catalog) },
            FakeAppDao()
        )

        repository.loadCatalog()

        val apps = repository.apps.first()
        assertEquals(catalog.map { it.id }, apps.map { it.id })
        assertEquals("Браузер", repository.observeApp("browser").first()?.name)
        assertEquals("Разработчик не указан", apps.first().developer)
        assertFalse(apps.first().isFavorite)
    }

    @Test
    fun loadCatalog_respectsServerOrder() = runTest {
        val shuffled = listOf(catalog[2], catalog[0], catalog[4], catalog[1], catalog[3])
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.success(shuffled) },
            FakeAppDao()
        )

        repository.loadCatalog()

        assertEquals(shuffled.map { it.id }, repository.apps.first().map { it.id })
    }

    @Test
    fun loadCatalog_httpError_keepsDatabaseEmpty() = runTest {
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.error(500, ResponseBody.create(null, "boom")) },
            FakeAppDao()
        )

        repository.loadCatalog()

        assertTrue(repository.apps.first().isEmpty())
        assertNull(repository.observeApp("browser").first())
    }

    @Test
    fun loadCatalog_networkFailure_doesNotCrashAndKeepsCache() = runTest {
        val dao = FakeAppDao()
        val repository = NetworkAppRepository(FakeCatalogApi { throw IOException("нет сети") }, dao)

        repository.loadCatalog()

        assertTrue(repository.apps.first().isEmpty())
    }

    @Test
    fun loadCatalog_cancellation_isRethrown() = runTest {
        val repository = NetworkAppRepository(
            FakeCatalogApi { throw CancellationException("отмена загрузки") },
            FakeAppDao()
        )

        val error = runCatching { repository.loadCatalog() }.exceptionOrNull()

        assertTrue("ожидали CancellationException, получили $error", error is CancellationException)
    }

    @Test
    fun emptyBody_keepsExistingCatalog() = runTest {
        val dao = FakeAppDao()
        val repository = NetworkAppRepository(FakeCatalogApi { Response.success(catalog) }, dao)
        repository.loadCatalog()

        val emptyApi = NetworkAppRepository(
            FakeCatalogApi { Response.success(emptyList<CatalogAppDto>()) },
            dao
        )
        emptyApi.loadCatalog()

        assertEquals(catalog.size, repository.apps.first().size)
    }

    @Test
    fun setFavorite_updatesNewFieldAndFavoritesSubscription() = runTest {
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.success(catalog) },
            FakeAppDao()
        )
        repository.loadCatalog()

        repository.setFavorite("browser", true)

        assertTrue(repository.observeApp("browser").first()!!.isFavorite)
        assertEquals(listOf("browser"), repository.observeFavorites().first().map { it.id })

        repository.setFavorite("browser", false)

        assertTrue(repository.observeFavorites().first().isEmpty())
    }

    @Test
    fun reloadCatalog_doesNotResetFavoriteMark() = runTest {
        val dao = FakeAppDao()
        val repository = NetworkAppRepository(FakeCatalogApi { Response.success(catalog) }, dao)
        repository.loadCatalog()
        repository.setFavorite("mail", true)

        // повторная загрузка того же каталога: контент обновился, отметка осталась
        repository.loadCatalog()

        val mail = repository.observeApp("mail").first()
        assertTrue(mail!!.isFavorite)
        assertEquals(listOf("mail"), repository.observeFavorites().first().map { it.id })
    }

    @Test
    fun reloadCatalog_updatesContentAndDeletesMissingApps() = runTest {
        val dao = FakeAppDao()
        val repository = NetworkAppRepository(FakeCatalogApi { Response.success(catalog) }, dao)
        repository.loadCatalog()
        repository.setFavorite("browser", true)

        val updatedApi = NetworkAppRepository(
            FakeCatalogApi {
                Response.success(
                    listOf(
                        CatalogAppDto("browser", "Браузер 2", "Новое описание", "Интернет"),
                        CatalogAppDto("maps", "Карты", "Карты мира", "Путешествия")
                    )
                )
            },
            dao
        )
        updatedApi.loadCatalog()

        val apps = repository.apps.first()
        assertEquals(listOf("browser", "maps"), apps.map { it.id })
        assertEquals("Браузер 2", apps.first().name)
        assertEquals("Новое описание", apps.first().description)
        // id тот же — отметка «Понравилось» сохранилась после обновления контента
        assertTrue(apps.first().isFavorite)
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
