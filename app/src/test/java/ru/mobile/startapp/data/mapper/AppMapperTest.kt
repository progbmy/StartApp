package ru.mobile.startapp.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.mobile.startapp.data.source.local.AppDto
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
}
