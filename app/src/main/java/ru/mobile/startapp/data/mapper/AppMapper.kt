package ru.mobile.startapp.data.mapper

import ru.mobile.startapp.data.source.local.AppDto
import ru.mobile.startapp.data.source.network.CatalogAppDto
import ru.mobile.startapp.domain.App

/**
 * Маппер: преобразует DTO из слоя данных в доменную модель.
 *
 * Следует правилу Clean Architecture — данные текут от внешних
 * источников (data) к ядру (domain). Представление работает только
 * с доменными моделями и не знает про DTO.
 */
object AppMapper {

    /** Локальный DTO → Domain */
    fun AppDto.toDomain(): App = App(
        id = id,
        name = name,
        description = description,
        category = category,
        developer = developer,
        version = version,
        size = size,
        fullDescription = fullDescription
    )

    /** Сетевой DTO → Domain */
    fun CatalogAppDto.toDomain(): App = App(
        id = id,
        name = name,
        description = description,
        category = category
    )

    /** Список локальных DTO → список доменных моделей */
    fun List<AppDto>.toDomainList(): List<App> = map(AppDto::toDomain)
}
