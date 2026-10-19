package ru.mobile.startapp.data

/**
 * Доменная модель приложения каталога.
 *
 * Не зависит от UI: иконку подбирает слой представления по [id],
 * поэтому модель можно переиспользовать в тестах и в любом источнике данных.
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
