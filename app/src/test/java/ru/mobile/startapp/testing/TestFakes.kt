package ru.mobile.startapp.testing

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
        category: String = "Интернет"
    ): App = App(
        id = id,
        name = name,
        description = "Короткое описание $id",
        category = category,
        developer = "StartApp Studio",
        version = "1.0",
        size = "10 МБ",
        fullDescription = "Полное описание $id"
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

    override fun observeApp(appId: String): Flow<App?> =
        _apps.map { apps -> apps.firstOrNull { it.id == appId } }

    override fun observeFavorites(): Flow<List<App>> =
        _apps.map { apps -> apps.filter { it.isFavorite } }

    override suspend fun toggleWishlist(appId: String) {
        _apps.update { apps ->
            apps.map { app ->
                if (app.id == appId) app.copy(isInWishlist = !app.isInWishlist) else app
            }
        }
    }

    override suspend fun setFavorite(appId: String, isFavorite: Boolean) {
        _apps.update { apps ->
            apps.map { app ->
                if (app.id == appId) app.copy(isFavorite = isFavorite) else app
            }
        }
    }
}

/**
 * Room-DAO в памяти: те же операции, что у настоящего [AppDao], но без Android.
 *
 * Наследует [AppDao], поэтому [AppDao.upsertCatalog] проверяется на той же
 * логике «вставка новых + обновление контента», что работает в приложении.
 */
class FakeAppDao(initial: List<AppEntity> = emptyList()) : AppDao() {

    private val rows = MutableStateFlow(initial)

    override fun observeApps(): Flow<List<AppEntity>> =
        rows.map { apps -> apps.sortedBy { it.position } }

    override fun getAppDetails(id: String): Flow<AppEntity?> =
        rows.map { apps -> apps.firstOrNull { it.id == id } }

    override fun observeFavorites(): Flow<List<AppEntity>> =
        rows.map { apps -> apps.filter { it.isFavorite }.sortedBy { it.position } }

    override suspend fun insertNew(apps: List<AppEntity>) {
        rows.update { current ->
            val existingIds = current.map { it.id }.toSet()
            current + apps.filter { it.id !in existingIds }
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
        rows.update { apps ->
            apps.map { app ->
                if (app.id == id) {
                    app.copy(
                        name = name,
                        description = description,
                        category = category,
                        developer = developer,
                        version = version,
                        size = size,
                        fullDescription = fullDescription,
                        position = position
                    )
                } else {
                    app
                }
            }
        }
    }

    override suspend fun updateWishlistStatus(id: String, isInWishlist: Boolean) {
        rows.update { apps ->
            apps.map { app ->
                if (app.id == id) app.copy(isInWishlist = isInWishlist) else app
            }
        }
    }

    override suspend fun updateFavorite(id: String, isFavorite: Boolean) {
        rows.update { apps ->
            apps.map { app ->
                if (app.id == id) app.copy(isFavorite = isFavorite) else app
            }
        }
    }

    override suspend fun deleteMissing(ids: List<String>) {
        // SQLite: `NOT IN ()` истинно для всех строк, поэтому пустой список удаляет всё
        rows.update { apps -> if (ids.isEmpty()) emptyList() else apps.filter { it.id in ids } }
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
