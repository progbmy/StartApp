package ru.mobile.startapp.testing

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.mobile.startapp.data.source.local.db.AppDao
import ru.mobile.startapp.data.source.local.db.AppEntity
import ru.mobile.startapp.data.source.network.RatingApi
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.domain.repository.AppRepository

/**
 * Общие данные и фейки для JVM-тестов.
 *
 * Фейки реализуют доменные интерфейсы, поэтому тесты ViewModel и use case
 * не поднимают сеть, Hilt и Android.
 */
object TestData {

    fun app(
        id: String,
        name: String = "Приложение $id",
        category: String = "Интернет",
        isFavorite: Boolean = false
    ): App = App(
        id = id,
        name = name,
        description = "Короткое описание $id",
        category = category,
        developer = "StartApp Studio",
        version = "1.0",
        size = "10 МБ",
        fullDescription = "Полное описание $id",
        isFavorite = isFavorite
    )

    /** Каталог из пяти приложений: две штуки в категории «Медиа». */
    val catalog: List<App> = listOf(
        app("browser", name = "Браузер", category = "Интернет"),
        app("mail", name = "Почта", category = "Связь"),
        app("gallery", name = "Галерея", category = "Медиа"),
        app("music", name = "Музыка", category = "Медиа"),
        app("navigator", name = "Навигатор", category = "Путешествия")
    )

    /** Каталог из [size] приложений — для проверки размера выборки замера. */
    fun catalog(size: Int): List<App> =
        List(size) { index -> app("app$index", category = "Категория ${index % 3}") }
}

/**
 * Репозиторий каталога в памяти.
 *
 * Данные не приходят сами: тест сам решает, когда каталог «загрузился»,
 * через [emit] — так проверяются состояния загрузки.
 */
class FakeAppRepository(initial: List<App> = TestData.catalog) : AppRepository {

    private val _apps = MutableStateFlow(initial)
    override val apps: Flow<List<App>> = _apps.asStateFlow()

    fun emit(apps: List<App>) {
        _apps.value = apps
    }

    override fun observeFavorites(): Flow<List<App>> =
        _apps.map { apps -> apps.filter(App::isFavorite) }

    override fun observeApp(appId: String): Flow<App?> =
        _apps.map { apps -> apps.firstOrNull { it.id == appId } }

    override suspend fun setFavorite(appId: String, isFavorite: Boolean) {
        _apps.update { apps ->
            apps.map { app ->
                if (app.id == appId) app.copy(isFavorite = isFavorite) else app
            }
        }
    }
}

/**
 * DAO в памяти для JVM-тестов репозитория: реализует контракт [AppDao],
 * но вместо Room хранит строки в [MutableStateFlow].
 *
 * Наследуется от abstract class, поэтому унаследованный [AppDao.upsertCatalog]
 * работает поверх [insertNew] и [updateContent] — тест проверяет ту же логику,
 * что и продовая реализация DAO.
 */
class FakeAppDao : AppDao() {

    private val _entities = MutableStateFlow<List<AppEntity>>(emptyList())

    override fun observeApps(): Flow<List<AppEntity>> =
        _entities.map { entities -> entities.sortedBy(AppEntity::position) }

    override fun observeFavorites(): Flow<List<AppEntity>> =
        _entities.map { entities ->
            entities.filter(AppEntity::isFavorite).sortedBy(AppEntity::position)
        }

    override fun observeApp(appId: String): Flow<AppEntity?> =
        _entities.map { entities -> entities.firstOrNull { it.id == appId } }

    override suspend fun insertNew(apps: List<AppEntity>) {
        _entities.update { current ->
            val existingIds = current.mapTo(mutableSetOf(), AppEntity::id)
            current + apps.filterNot { it.id in existingIds }
        }
    }

    override suspend fun updateContent(
        id: String,
        name: String,
        description: String,
        category: String,
        developer: String,
        version: String,
        size: String,
        fullDescription: String,
        position: Int
    ) {
        _entities.update { entities ->
            entities.map { entity ->
                if (entity.id != id) {
                    entity
                } else {
                    entity.copy(
                        name = name,
                        description = description,
                        category = category,
                        developer = developer,
                        version = version,
                        size = size,
                        fullDescription = fullDescription,
                        position = position
                    )
                }
            }
        }
    }

    override suspend fun updateFavorite(appId: String, isFavorite: Boolean) {
        _entities.update { entities ->
            entities.map { entity ->
                if (entity.id == appId) entity.copy(isFavorite = isFavorite) else entity
            }
        }
    }

    override suspend fun deleteMissing(ids: List<String>) {
        _entities.update { entities -> entities.filter { it.id in ids } }
    }
}

/**
 * Фейковый [RatingApi] с настраиваемой задержкой и значением рейтинга.
 *
 * Запоминает запрошенные id — по журналу видно, сколько запросов сделал use case
 * и в каком порядке.
 */
class FakeRatingApi(
    private val delayMs: Long = 0,
    private val ratings: (String) -> Float = { DEFAULT_RATING }
) : RatingApi {

    private val lock = Any()
    private val _requestedIds = mutableListOf<String>()

    val requestedIds: List<String> get() = synchronized(lock) { _requestedIds.toList() }

    override suspend fun getRating(id: String): Float {
        synchronized(lock) { _requestedIds += id }
        if (delayMs > 0) delay(delayMs)
        return ratings(id)
    }

    companion object {

        const val DEFAULT_RATING = 4.5f
    }
}

/** [RatingApi], который падает на приложении [failingId] — проверка обработки ошибок. */
class FailingRatingApi(
    private val failingId: String,
    private val delayMs: Long = 0
) : RatingApi {

    override suspend fun getRating(id: String): Float {
        if (delayMs > 0) delay(delayMs)
        if (id == failingId) throw IllegalStateException("Рейтинг для $id недоступен")
        return FakeRatingApi.DEFAULT_RATING
    }
}
