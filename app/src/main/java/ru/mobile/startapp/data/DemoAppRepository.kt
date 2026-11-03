package ru.mobile.startapp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.mobile.startapp.data.mapper.AppMapper.toDomainList
import ru.mobile.startapp.data.source.local.DemoAppDataSource
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.domain.repository.AppRepository

/**
 * Реализация [AppRepository] на демо-данных в памяти.
 *
 * Слой данных:
 * 1. Читает жёстко заданные DTO из [DemoAppDataSource].
 * 2. Конвертирует их через [ru.mobile.startapp.data.mapper.AppMapper]
 *    в доменные модели.
 * 3. Отдаёт доменные модели через [StateFlow] представлению.
 *
 * Заменить на сетевой или локальный источник — достаточно создать
 * новую реализацию [AppRepository], доменная модель и представление
 * останутся без изменений.
 */
class DemoAppRepository : AppRepository {

    private val _apps = MutableStateFlow(DemoAppDataSource.apps.toDomainList())
    override val apps: StateFlow<List<App>> = _apps.asStateFlow()

    override fun appById(appId: String?): App? {
        if (appId == null) return null
        return _apps.value.find { it.id == appId }
    }

    override fun similarApps(appId: String, limit: Int): List<App> {
        val current = appById(appId) ?: return emptyList()

        val (sameCategory, others) = _apps.value
            .filter { it.id != current.id }
            .partition { it.category == current.category }

        return (sameCategory + others).take(limit)
    }
}
