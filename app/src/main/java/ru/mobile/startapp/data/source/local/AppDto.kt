package ru.mobile.startapp.data.source.local

/**
 * DTO (Data Transfer Object) для приложения каталога.
 *
 * Используется в слое данных для сериализации, передачи между модулями
 * и хранения жёстко заданных демо-данных. Конвертируется в доменную модель
 * через [ru.mobile.startapp.data.mapper.AppMapper].
 */
data class AppDto(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val developer: String = "Разработчик не указан",
    val version: String = "—",
    val size: String = "—",
    val fullDescription: String = description
)
