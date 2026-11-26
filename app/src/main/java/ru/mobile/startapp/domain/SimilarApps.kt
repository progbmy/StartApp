package ru.mobile.startapp.domain

import ru.mobile.startapp.domain.repository.AppRepository

/**
 * Похожие приложения для карточки [appId]: сначала та же категория,
 * затем остальные, в исходном порядке каталога, без самого приложения.
 *
 * Расширение над списком, а не метод репозитория: каталог живёт в Room,
 * и ViewModel считает блок «Вам может понравиться» из уже полученного списка.
 */
fun List<App>.similarApps(
    appId: String,
    limit: Int = AppRepository.SIMILAR_APPS_LIMIT
): List<App> {
    val current = firstOrNull { it.id == appId } ?: return emptyList()

    val (sameCategory, others) = filter { it.id != current.id }
        .partition { it.category == current.category }

    return (sameCategory + others).take(limit)
}
