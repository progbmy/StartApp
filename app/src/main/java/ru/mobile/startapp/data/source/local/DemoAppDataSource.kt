package ru.mobile.startapp.data.source.local

/**
 * Жёстко заданные демо-данные каталога приложений.
 *
 * Это слой данных: здесь живут DTO и хардкод-контент.
 * Доменная модель остаётся чистой — она не знает про DTO.
 *
 * Заменить на сетевой или локальный источник легко — достаточно
 * создать новую реализацию [ru.mobile.startapp.domain.repository.AppRepository],
 * которая подтянет данные из другого источника и маппер их сконвертирует.
 */
object DemoAppDataSource {

    /** Полный каталог демо-приложений (список DTO). */
    val apps: List<AppDto> = listOf(
        AppDto(
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
        AppDto(
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
        AppDto(
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
        AppDto(
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
        AppDto(
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
        AppDto(
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
        AppDto(
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
        AppDto(
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
}
