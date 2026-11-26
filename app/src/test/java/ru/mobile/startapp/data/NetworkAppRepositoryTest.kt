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
import ru.mobile.startapp.data.source.local.db.AppEntity
import ru.mobile.startapp.data.source.network.CatalogApiService
import ru.mobile.startapp.data.source.network.CatalogAppDto
import ru.mobile.startapp.domain.repository.AppRepository
import ru.mobile.startapp.domain.similarApps
import ru.mobile.startapp.testing.FakeAppDao

/**
 * Сетевой репозиторий каталога: запись в Room, сохранность вишлиста,
 * HTTP-ошибка, падение сети и отмена.
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

    private fun entity(
        id: String,
        position: Int = 0,
        isInWishlist: Boolean = false
    ): AppEntity = AppEntity(
        id = id,
        name = "Приложение $id",
        description = "Описание $id",
        category = "Интернет",
        developer = "StartApp Studio",
        version = "1.0",
        size = "10 МБ",
        fullDescription = "Полное описание $id",
        isInWishlist = isInWishlist,
        position = position
    )

    @Test
    fun loadCatalog_success_writesCatalogToRoom() = runTest {
        val dao = FakeAppDao()
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.success(catalog) },
            dao
        )

        repository.loadCatalog()

        val apps = repository.apps.first()
        assertEquals(catalog.map { it.id }, apps.map { it.id })
        assertEquals("Браузер", apps.first().name)
        assertEquals("Разработчик не указан", apps.first().developer)
        assertTrue(apps.none { it.isInWishlist })
    }

    @Test
    fun loadCatalog_keepsServerOrder() = runTest {
        val shuffled = listOf(catalog[3], catalog[0], catalog[4], catalog[1], catalog[2])
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.success(shuffled) },
            FakeAppDao()
        )

        repository.loadCatalog()

        assertEquals(shuffled.map { it.id }, repository.apps.first().map { it.id })
    }

    @Test
    fun loadCatalog_removesAppsMissingFromServer() = runTest {
        val dao = FakeAppDao(
            listOf(entity("ghost", position = 5))
        )
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.success(catalog) },
            dao
        )

        repository.loadCatalog()

        val ids = repository.apps.first().map { it.id }
        assertEquals(catalog.map { it.id }, ids)
        assertFalse("ghost должен исчезнуть из каталога", "ghost" in ids)
    }

    @Test
    fun loadCatalog_httpError_keepsExistingRows() = runTest {
        val dao = FakeAppDao(listOf(entity("browser", isInWishlist = true)))
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.error(500, ResponseBody.create(null, "boom")) },
            dao
        )

        repository.loadCatalog()

        val apps = repository.apps.first()
        assertEquals(1, apps.size)
        assertEquals("browser", apps.first().id)
        assertTrue("ошибка сети не должна стирать вишлист", apps.first().isInWishlist)
    }

    @Test
    fun loadCatalog_networkFailure_keepsExistingRows() = runTest {
        val dao = FakeAppDao(listOf(entity("browser")))
        val repository = NetworkAppRepository(
            FakeCatalogApi { throw IOException("нет сети") },
            dao
        )

        repository.loadCatalog()

        assertEquals(listOf("browser"), repository.apps.first().map { it.id })
    }

    @Test
    fun loadCatalog_emptyBody_keepsExistingRows() = runTest {
        val dao = FakeAppDao(listOf(entity("browser")))
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.success(emptyList<CatalogAppDto>()) },
            dao
        )

        repository.loadCatalog()

        assertEquals(listOf("browser"), repository.apps.first().map { it.id })
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
    fun observeApp_returnsNullForUnknownId() = runTest {
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.success(catalog) },
            FakeAppDao()
        )
        repository.loadCatalog()

        assertNull(repository.observeApp("unknown").first())
        assertEquals("Почта", repository.observeApp("mail").first()?.name)
    }

    @Test
    fun toggleWishlist_flipsStatusInRoom() = runTest {
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.success(catalog) },
            FakeAppDao()
        )
        repository.loadCatalog()

        assertFalse(repository.observeApp("browser").first()!!.isInWishlist)

        repository.toggleWishlist("browser")
        assertTrue(repository.observeApp("browser").first()!!.isInWishlist)

        repository.toggleWishlist("browser")
        assertFalse(repository.observeApp("browser").first()!!.isInWishlist)
    }

    @Test
    fun toggleWishlist_unknownId_doesNothing() = runTest {
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.success(catalog) },
            FakeAppDao()
        )
        repository.loadCatalog()

        repository.toggleWishlist("unknown")

        assertNull(repository.observeApp("unknown").first())
    }

    @Test
    fun wishlistMark_survivesCatalogReload() = runTest {
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.success(catalog) },
            FakeAppDao()
        )
        repository.loadCatalog()
        repository.toggleWishlist("gallery")

        repository.loadCatalog() // повторная загрузка каталога с сервера

        val gallery = repository.observeApp("gallery").first()!!
        assertTrue("обновление каталога не должно затирать вишлист", gallery.isInWishlist)
        assertFalse(repository.observeApp("mail").first()!!.isInWishlist)
    }

    @Test
    fun similarApps_putsSameCategoryFirst_andExcludesCurrent() = runTest {
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.success(catalog) },
            FakeAppDao()
        )
        repository.loadCatalog()

        val similar = repository.apps.first().similarApps("gallery", AppRepository.SIMILAR_APPS_LIMIT)

        assertEquals(listOf("music", "browser", "mail"), similar.map { it.id })
        assertTrue(similar.none { it.id == "gallery" })
    }

    @Test
    fun similarApps_forUnknownApp_givesEmptyList() = runTest {
        val repository = NetworkAppRepository(
            FakeCatalogApi { Response.success(catalog) },
            FakeAppDao()
        )
        repository.loadCatalog()

        assertTrue(repository.apps.first().similarApps("unknown").isEmpty())
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
