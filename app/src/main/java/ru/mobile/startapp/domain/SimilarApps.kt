package ru.mobile.startapp.domain

import ru.mobile.startapp.domain.repository.AppRepository

/**
 * Похожие приложения для блока «Вам может понравиться».
 *
 * Сначала идут приложения той же категории, затем остальные; само приложение
 * [appId] в подборку не попадает. Логика живёт в домене: раньше она была
 * в репозитории, но теперь каталог приходит подпиской из БД, а репозиторий
 * не хранит список в памяти.
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
