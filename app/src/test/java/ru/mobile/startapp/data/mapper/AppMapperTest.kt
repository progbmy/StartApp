package ru.mobile.startapp.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mobile.startapp.data.source.local.AppDto
import ru.mobile.startapp.data.source.local.db.AppEntity
import ru.mobile.startapp.data.source.network.CatalogAppDto
import ru.mobile.startapp.domain.App

/** Маппинг DTO → доменная модель: все поля и значения по умолчанию. */
class AppMapperTest {

    @Test
    fun localDto_mapsEveryField() {
        val dto = AppDto(
            id = "browser",
            name = "Браузер",
            description = "Короткое описание",
            category = "Интернет",
            developer = "StartApp Studio",
            version = "2.4.1",
            size = "38 МБ",
            fullDescription = "Полное описание"
        )

        assertEquals(
            App(
                id = "browser",
                name = "Браузер",
                description = "Короткое описание",
                category = "Интернет",
                developer = "StartApp Studio",
                version = "2.4.1",
                size = "38 МБ",
                fullDescription = "Полное описание"
            ),
            dto.toDomain()
        )
    }

    @Test
    fun localDto_usesDefaultsForUnknownFields() {
        val app = AppDto(
            id = "id",
            name = "Имя",
            description = "Описание",
            category = "Категория"
        ).toDomain()

        assertEquals("Разработчик не указан", app.developer)
        assertEquals("—", app.version)
        assertEquals("—", app.size)
        assertEquals("Описание", app.fullDescription)
    }

    @Test
    fun networkDto_mapsFieldsAndKeepsDomainDefaults() {
        val dto = CatalogAppDto(
            id = "mail",
            name = "Почта",
            description = "Просмотр писем",
            category = "Связь",
            iconUrl = "http://185.103.109.134/icons/mail.png"
        )

        val app = dto.toDomain()

        assertEquals("mail", app.id)
        assertEquals("Почта", app.name)
        assertEquals("Просмотр писем", app.description)
        assertEquals("Связь", app.category)
        // Сервер не отдаёт эти поля — подставляются значения доменной модели.
        assertEquals("Разработчик не указан", app.developer)
        assertEquals("—", app.version)
        assertEquals("—", app.size)
        assertEquals("Просмотр писем", app.fullDescription)
    }

    @Test
    fun localDtoList_keepsOrderAndSize() {
        val dtos = listOf(
            AppDto(id = "a", name = "A", description = "a", category = "cat"),
            AppDto(id = "b", name = "B", description = "b", category = "cat"),
            AppDto(id = "c", name = "C", description = "c", category = "cat")
        )

        val apps = dtos.toDomainList()

        assertEquals(listOf("a", "b", "c"), apps.map(App::id))
        assertEquals(3, apps.size)
    }

    @Test
    fun emptyList_mapsToEmptyDomainList() {
        assertEquals(emptyList<App>(), emptyList<AppDto>().toDomainList())
    }

    @Test
    fun networkDto_toEntity_fillsNewFavoriteFieldAndPosition() {
        val entity = CatalogAppDto(
            id = "mail",
            name = "Почта",
            description = "Просмотр писем",
            category = "Связь"
        ).toEntity(position = 3)

        assertEquals("mail", entity.id)
        assertEquals("Почта", entity.name)
        assertEquals("Просмотр писем", entity.description)
        assertEquals("Связь", entity.category)
        assertEquals("Разработчик не указан", entity.developer)
        assertEquals("—", entity.version)
        assertEquals("—", entity.size)
        assertEquals("Просмотр писем", entity.fullDescription)
        assertEquals(3, entity.position)
        assertFalse("новое приложение не должно быть в понравившихся", entity.isFavorite)
    }

    @Test
    fun entity_mapsEveryFieldIncludingFavorite() {
        val entity = AppEntity(
            id = "browser",
            name = "Браузер",
            description = "Короткое описание",
            category = "Интернет",
            developer = "StartApp Studio",
            version = "2.4.1",
            size = "38 МБ",
            fullDescription = "Полное описание",
            isFavorite = true,
            position = 0
        )

        assertEquals(
            App(
                id = "browser",
                name = "Браузер",
                description = "Короткое описание",
                category = "Интернет",
                developer = "StartApp Studio",
                version = "2.4.1",
                size = "38 МБ",
                fullDescription = "Полное описание",
                isFavorite = true
            ),
            entity.toDomain()
        )
    }

    @Test
    fun domain_toEntity_keepsFavorite() {
        val entity = App(
            id = "music",
            name = "Музыка",
            description = "Плеер",
            category = "Медиа",
            isFavorite = true
        ).toEntity(position = 5)

        assertEquals("music", entity.id)
        assertTrue(entity.isFavorite)
        assertEquals(5, entity.position)
    }

    @Test
    fun entityList_mapsToDomainListInOrder() {
        val entities = listOf(
            AppEntity(
                id = "a", name = "A", description = "a", category = "cat",
                developer = "dev", version = "1", size = "1 МБ",
                fullDescription = "a", isFavorite = false, position = 0
            ),
            AppEntity(
                id = "b", name = "B", description = "b", category = "cat",
                developer = "dev", version = "1", size = "1 МБ",
                fullDescription = "b", isFavorite = true, position = 1
            )
        )

        val apps = entities.toDomainApps()

        assertEquals(listOf("a", "b"), apps.map(App::id))
        assertFalse(apps.first().isFavorite)
        assertTrue(apps.last().isFavorite)
    }
}
