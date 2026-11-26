package ru.mobile.startapp.domain

/**
 * Доменная сущность приложения каталога.
 *
 * Не зависит от UI и источников данных: содержит только бизнес-поля
 * и идентификацию. Иконку подбирает слой представления по [id],
 * поэтому доменную модель можно переиспользовать в тестах и в любом
 * источнике данных.
 *
 * [isInWishlist] — отметка пользователя «добавлено в вишлист». Хранится
 * в БД и приезжает в UI потоком репозитория.
 */
data class App(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val developer: String = "Разработчик не указан",
    val version: String = "—",
    val size: String = "—",
    val fullDescription: String = description,
    val isInWishlist: Boolean = false
)
