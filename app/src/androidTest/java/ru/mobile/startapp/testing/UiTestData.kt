package ru.mobile.startapp.testing

import ru.mobile.startapp.domain.App

/**
 * Данные для UI-тестов (androidTest).
 *
 * Source set `androidTest` не видит `test`, поэтому здесь свой набор:
 * имена приложений уникальны, чтобы поиск по тексту на экране был однозначным.
 */
object UiTestApps {

    val browser = App(
        id = "browser",
        name = "Браузер",
        description = "Быстрый поиск в интернете",
        category = "Интернет",
        developer = "StartApp Studio",
        version = "2.4.1",
        size = "38 МБ",
        fullDescription = "Поиск в один тап, вкладки и режим инкогнито."
    )

    val mail = App(
        id = "mail",
        name = "Почта",
        description = "Письма и уведомления",
        category = "Связь",
        developer = "StartApp Studio",
        version = "1.9.0",
        size = "24 МБ",
        fullDescription = "Несколько ящиков, поиск по письмам."
    )

    val gallery = App(
        id = "gallery",
        name = "Галерея",
        description = "Фото и видео в одном месте",
        category = "Медиа",
        developer = "StartApp Studio",
        version = "3.0.2",
        size = "52 МБ",
        fullDescription = "Альбомы, поиск по датам и местам съёмки."
    )

    val music = App(
        id = "music",
        name = "Музыка",
        description = "Плеер и плейлисты",
        category = "Медиа",
        developer = "StartApp Studio",
        version = "2.2.7",
        size = "31 МБ",
        fullDescription = "Плейлисты, очередь и эквалайзер."
    )

    val navigator = App(
        id = "navigator",
        name = "Навигатор",
        description = "Маршруты, пробки и адреса",
        category = "Путешествия",
        developer = "StartApp Maps",
        version = "5.1.3",
        size = "76 МБ",
        fullDescription = "Маршруты с учётом пробок и голосовые подсказки."
    )

    /** Полный каталог: пять приложений, две штуки в категории «Медиа». */
    val catalog: List<App> = listOf(browser, mail, gallery, music, navigator)
}
