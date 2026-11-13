package ru.mobile.startapp.data.source.network

/**
 * DTO ответа API для каталога приложений.
 *
 * Соответствует JSON от http://185.103.109.134/catalog.
 * Конвертируется в доменную модель через [ru.mobile.startapp.data.mapper.AppMapper].
 */
data class CatalogAppDto(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val iconUrl: String? = null
)
