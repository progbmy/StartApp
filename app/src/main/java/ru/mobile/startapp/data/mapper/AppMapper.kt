package ru.mobile.startapp.data.mapper

import ru.mobile.startapp.data.source.local.AppDto
import ru.mobile.startapp.data.source.local.db.AppEntity
import ru.mobile.startapp.data.source.network.CatalogAppDto
import ru.mobile.startapp.domain.App

/**
 * Мапперы между слоями: DTO (сеть/демо) → Entity (Room) → Domain.
 *
 * Следует правилу Clean Architecture — представление работает только
 * с доменными моделями и не знает про DTO и Room.
 *
 * Функции объявлены на верхнем уровне файла: репозитории импортируют их
 * напрямую (`import ru.mobile.startapp.data.mapper.toDomain`).
 */

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

/**
 * Сетевой DTO → Entity для записи в Room.
 *
 * [position] — порядок приложения в ответе сервера. Сервер не отдаёт
 * разработчика/версию/размер, поэтому подставляются те же значения,
 * что и в доменной модели по умолчанию.
 */
fun CatalogAppDto.toEntity(position: Int = 0): AppEntity = AppEntity(
    id = id,
    name = name,
    description = description,
    category = category,
    developer = DEFAULT_DEVELOPER,
    version = DEFAULT_VERSION,
    size = DEFAULT_SIZE,
    fullDescription = description,
    isInWishlist = false,
    isFavorite = false,
    position = position
)

/** Entity → Domain: пользовательские отметки вишлиста и «Понравившихся» копируются. */
fun AppEntity.toDomain(): App = App(
    id = id,
    name = name,
    description = description,
    category = category,
    developer = developer,
    version = version,
    size = size,
    fullDescription = fullDescription,
    isInWishlist = isInWishlist,
    isFavorite = isFavorite
)

/** Domain → Entity: обратное преобразование (демо-данные и тесты). */
fun App.toEntity(position: Int = 0): AppEntity = AppEntity(
    id = id,
    name = name,
    description = description,
    category = category,
    developer = developer,
    version = version,
    size = size,
    fullDescription = fullDescription,
    isInWishlist = isInWishlist,
    isFavorite = isFavorite,
    position = position
)

/** Список Entity → список доменных моделей */
fun List<AppEntity>.toDomainApps(): List<App> = map { it.toDomain() }

/** Список локальных DTO → список доменных моделей */
fun List<AppDto>.toDomainList(): List<App> = map { it.toDomain() }

private const val DEFAULT_DEVELOPER = "Разработчик не указан"
private const val DEFAULT_VERSION = "—"
private const val DEFAULT_SIZE = "—"
