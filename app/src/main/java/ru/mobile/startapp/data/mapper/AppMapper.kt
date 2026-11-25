package ru.mobile.startapp.data.mapper

import ru.mobile.startapp.data.source.local.AppDto
import ru.mobile.startapp.data.source.local.db.AppEntity
import ru.mobile.startapp.data.source.network.CatalogAppDto
import ru.mobile.startapp.domain.App

/**
 * Мапперы: DTO/Entity ↔ доменная модель.
 *
 * Данные текут от внешних источников (сеть, БД) к ядру (domain).
 * Представление работает только с доменными моделями и не знает про DTO/Entity.
 *
 * Функции объявлены на верхнем уровне файла: репозитории импортируют их
 * напрямую (`import ru.mobile.startapp.data.mapper.toDomain`).
 */

/** Сетевое DTO → Entity БД (новое поле «Понравилось» по умолчанию выключено). */
fun CatalogAppDto.toEntity(position: Int): AppEntity = AppEntity(
    id = id,
    name = name,
    description = description,
    category = category,
    developer = DEFAULT_DEVELOPER,
    version = DEFAULT_VERSION,
    size = DEFAULT_SIZE,
    fullDescription = description,
    isFavorite = false,
    position = position
)

/** Entity БД → Domain */
fun AppEntity.toDomain(): App = App(
    id = id,
    name = name,
    description = description,
    category = category,
    developer = developer,
    version = version,
    size = size,
    fullDescription = fullDescription,
    isFavorite = isFavorite
)

/** Domain → Entity БД (например, для демо-репозитория в памяти). */
fun App.toEntity(position: Int = 0): AppEntity = AppEntity(
    id = id,
    name = name,
    description = description,
    category = category,
    developer = developer,
    version = version,
    size = size,
    fullDescription = fullDescription,
    isFavorite = isFavorite,
    position = position
)

/**
 * Список Entity БД → список доменных моделей.
 *
 * Имя отличается от [List<AppDto>.toDomainList], потому что после стирания
 * дженериков обе функции имели бы одну JVM-сигнатуру.
 */
fun List<AppEntity>.toDomainApps(): List<App> = map { it.toDomain() }

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
fun List<AppDto>.toDomainList(): List<App> = map { it.toDomain() }

/** Значения полей, которых нет в ответе сети. */
private const val DEFAULT_DEVELOPER = "Разработчик не указан"
private const val DEFAULT_VERSION = "—"
private const val DEFAULT_SIZE = "—"
