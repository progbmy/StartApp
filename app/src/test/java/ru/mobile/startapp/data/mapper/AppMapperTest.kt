package ru.mobile.startapp.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.mobile.startapp.data.source.local.AppDto
import ru.mobile.startapp.data.source.local.db.AppEntity
import ru.mobile.startapp.data.source.network.CatalogAppDto
import ru.mobile.startapp.domain.App

/** Маппинг DTO ↔ Entity ↔ доменная модель: все поля, значения по умолчанию и вишлист. */
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
    fun networkDto_toEntity_fillsServiceDefaultsAndPosition() {
        val entity = CatalogAppDto(
            id = "mail",
            name = "Почта",
            description = "Просмотр писем",
            category = "Связь",
            iconUrl = "http://185.103.109.134/icons/mail.png"
        ).toEntity(position = 4)

        assertEquals("mail", entity.id)
        assertEquals("Почта", entity.name)
        assertEquals("Просмотр писем", entity.fullDescription)
        assertEquals("Разработчик не указан", entity.developer)
        assertEquals("—", entity.version)
        assertEquals("—", entity.size)
        assertEquals(4, entity.position)
        assertEquals(false, entity.isInWishlist)
        assertEquals(false, entity.isFavorite)
    }

    @Test
    fun entityToDomain_copiesWishlistFlagAndAllFields() {
        val entity = AppEntity(
            id = "gallery",
            name = "Галерея",
            description = "Фото и видео",
            category = "Медиа",
            developer = "StartApp Studio",
            version = "3.0.2",
            size = "52 МБ",
            fullDescription = "Полное описание галереи",
            isInWishlist = true,
            isFavorite = true,
            position = 2
        )

        val app = entity.toDomain()

        assertEquals("gallery", app.id)
        assertEquals("Галерея", app.name)
        assertEquals("Фото и видео", app.description)
        assertEquals("Медиа", app.category)
        assertEquals("StartApp Studio", app.developer)
        assertEquals("3.0.2", app.version)
        assertEquals("52 МБ", app.size)
        assertEquals("Полное описание галереи", app.fullDescription)
        assertEquals(true, app.isInWishlist)
        assertEquals(true, app.isFavorite)
    }

    @Test
    fun domainToEntity_roundTripsWishlistFlag() {
        val app = App(
            id = "browser",
            name = "Браузер",
            description = "Поиск",
            category = "Интернет",
            fullDescription = "Полное описание",
            isInWishlist = true,
            isFavorite = true
        )

        val entity = app.toEntity(position = 3)

        assertEquals(true, entity.isInWishlist)
        assertEquals(true, entity.isFavorite)
        assertEquals(3, entity.position)
        assertEquals(app, entity.toDomain())
    }

    @Test
    fun entityList_mapsToDomainAppsKeepingWishlistAndOrder() {
        val entities = listOf(
            AppEntity(
                id = "a", name = "A", description = "a", category = "cat",
                developer = "dev", version = "1", size = "1 МБ",
                fullDescription = "a", position = 0
            ),
            AppEntity(
                id = "b", name = "B", description = "b", category = "cat",
                developer = "dev", version = "1", size = "1 МБ",
                fullDescription = "b", isInWishlist = true, isFavorite = true, position = 1
            )
        )

        val apps = entities.toDomainApps()

        assertEquals(listOf("a", "b"), apps.map(App::id))
        assertEquals(false, apps.first().isInWishlist)
        assertEquals(true, apps.last().isInWishlist)
        assertEquals(true, apps.last().isFavorite)
    }
}
