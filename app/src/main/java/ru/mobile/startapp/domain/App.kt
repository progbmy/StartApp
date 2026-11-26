package ru.mobile.startapp.domain

/**
 * Доменная сущность приложения каталога.
 *
 * Не зависит от UI и источников данных: содержит только бизнес-поле
 * и идентификацию. Иконку подбирает слой представления по [id],
 * поэтому доменную модель можно переиспользовать в тестах и в любом
 * источнике данных.
 */
data class App(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val developer: String = "Разработчик не указан",
    val version: String = "—",
    val size: String = "—",
    val fullDescription: String = description
)
