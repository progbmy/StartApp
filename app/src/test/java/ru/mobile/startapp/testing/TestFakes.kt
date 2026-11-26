package ru.mobile.startapp.testing

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    override val apps: StateFlow<List<App>> = _apps.asStateFlow()

    fun emit(apps: List<App>) {
        _apps.value = apps
    }

    override fun appById(appId: String?): App? =
        appId?.let { id -> _apps.value.firstOrNull { it.id == id } }

    override fun similarApps(appId: String, limit: Int): List<App> {
        val current = appById(appId) ?: return emptyList()

        val (sameCategory, others) = _apps.value
            .filter { it.id != current.id }
            .partition { it.category == current.category }

        return (sameCategory + others).take(limit)
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
