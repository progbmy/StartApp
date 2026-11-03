package ru.mobile.startapp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Каталог на демо-данных: пока нет сети и базы, приложения лежат в памяти.
 *
 * Замена на сетевой или локальный источник — отдельная реализация [AppRepository],
 * слой представления при этом не меняется.
 */
class DemoAppRepository(initialApps: List<App> = demoApps) : AppRepository {

    private val _apps = MutableStateFlow(initialApps)
    override val apps: StateFlow<List<App>> = _apps.asStateFlow()

    override fun appById(appId: String?): App? {
        if (appId == null) return null
        return _apps.value.firstOrNull { it.id == appId }
    }

    override fun similarApps(appId: String, limit: Int): List<App> {
        val current = appById(appId) ?: return emptyList()

        val (sameCategory, others) = _apps.value
            .filter { it.id != current.id }
            .partition { it.category == current.category }

        return (sameCategory + others).take(limit)
    }
}

/** Демо-каталог приложений. */
val demoApps: List<App> = listOf(
    App(
        id = "browser",
        name = "Браузер",
        description = "Быстрый поиск в интернете и просмотр сайтов",
        category = "Интернет",
        developer = "StartApp Studio",
        version = "2.4.1",
        size = "38 МБ",
        fullDescription = "Браузер с поиском в один тап, вкладками, режимом " +
            "инкогнито и блокировкой всплывающих окон."
    ),
    App(
        id = "mail",
        name = "Почта",
        description = "Просмотр писем, уведомления о новых сообщениях",
        category = "Связь",
        developer = "StartApp Studio",
        version = "1.9.0",
        size = "24 МБ",
        fullDescription = "Почтовый клиент с поддержкой нескольких ящиков, " +
            "поиском по письмам и уведомлениями о новой почте."
    ),
    App(
        id = "gallery",
        name = "Галерея",
        description = "Все фото и видео на устройстве в одном месте",
        category = "Медиа",
        developer = "StartApp Studio",
        version = "3.0.2",
        size = "52 МБ",
        fullDescription = "Собирает фото и видео по альбомам, умеет искать " +
            "по датам и местам съёмки, поддерживает обрезку и поворот."
    ),
    App(
        id = "music",
        name = "Музыка",
        description = "Плеер, плейлисты и любимые исполнители",
        category = "Медиа",
        developer = "StartApp Studio",
        version = "2.2.7",
        size = "31 МБ",
        fullDescription = "Плеер для локальной музыки: плейлисты, очередь " +
            "воспроизведения и эквалайзер."
    ),
    App(
        id = "navigator",
        name = "Навигатор",
        description = "Маршруты, пробки и поиск адресов",
        category = "Путешествия",
        developer = "StartApp Maps",
        version = "5.1.3",
        size = "76 МБ",
        fullDescription = "Прокладывает маршруты с учётом пробок, ведёт " +
            "голосовые подсказки и предупреждает о камерах."
    ),
    App(
        id = "maps",
        name = "Карты",
        description = "Карты мира, места рядом и отзывы",
        category = "Путешествия",
        developer = "StartApp Maps",
        version = "5.1.3",
        size = "68 МБ",
        fullDescription = "Подробные карты, поиск мест рядом, отзывы " +
            "и сохранённые точки."
    ),
    App(
        id = "shop",
        name = "Магазин",
        description = "Покупки, заказы и доставка",
        category = "Покупки",
        developer = "StartApp Market",
        version = "4.6.0",
        size = "44 МБ",
        fullDescription = "Каталог товаров, корзина, отслеживание заказов " +
            "и доставка по городу."
    ),
    App(
        id = "wallet",
        name = "Кошелёк",
        description = "Платежи, переводы и история операций",
        category = "Финансы",
        developer = "StartApp Pay",
        version = "1.4.5",
        size = "29 МБ",
        fullDescription = "Платежи и переводы, история операций и напоминания " +
            "о регулярных списаниях."
    )
)
