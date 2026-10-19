package ru.mobile.startapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.mobile.startapp.ui.theme.StartAppTheme

/**
 * Модель одного приложения в списке.
 *
 * @param id              идентификатор для маршрута навигации
 * @param icon            иконка приложения
 * @param name            название приложения
 * @param description     краткое описание приложения
 * @param category        категория приложения
 * @param developer       разработчик приложения
 * @param version         версия приложения
 * @param size            размер приложения
 * @param fullDescription подробное описание для карточки приложения
 */
data class App(
    val id: String,
    val icon: ImageVector,
    val name: String,
    val description: String,
    val category: String,
    val developer: String = "Разработчик не указан",
    val version: String = "—",
    val size: String = "—",
    val fullDescription: String = description
)

/**
 * Экран списка приложений. Навигация живёт снаружи, в [StartAppNavHost].
 */
@Composable
fun AppListScreen(
    apps: List<App>,
    onAppClick: (App) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { ToolBar() }
    ) { innerPadding ->
        ListApp(
            listApp = apps,
            onAppClick = onAppClick,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * Тулбар с иконкой слева (кнопка меню) и заголовком.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolBar(modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.tool_bar_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = { /* TODO: открыть меню */ }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Меню"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/**
 * Тулбар карточки приложения: кнопка «назад» и название приложения.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailToolBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/**
 * Вертикальный список приложений.
 */
@Composable
private fun ListApp(
    listApp: List<App>,
    onAppClick: (App) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = listApp) { app ->
            AppCard(
                app = app,
                onClick = { onAppClick(app) }
            )
        }
    }
}

/**
 * Карточка одного приложения в списке: иконка, название, описание, категория.
 * Нажатие открывает карточку приложения.
 */
@Composable
private fun AppCard(app: App, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = app.icon,
                    contentDescription = app.name,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = app.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = app.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Карточка приложения: крупная иконка, название, категория, описание
 * и сведения о разработчике, версии и размере.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    app: App,
    similarApps: List<App>,
    onSimilarAppClick: (App) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val downloadStarted = stringResource(R.string.app_details_download_started, app.name)
    val scrollState = rememberScrollState()

    // при переходе между похожими приложениями карточка открывается с начала
    LaunchedEffect(app) { scrollState.scrollTo(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { DetailToolBar(title = app.name, onBack = onBack) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = app.icon,
                    contentDescription = app.name,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(24.dp)
                        .size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = app.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = app.category,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = app.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    scope.launch { snackbarHostState.showSnackbar(downloadStarted) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.app_details_download))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.app_details_about),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = app.fullDescription,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    InfoRow(stringResource(R.string.app_details_developer), app.developer)
                    InfoRow(stringResource(R.string.app_details_version), app.version)
                    InfoRow(stringResource(R.string.app_details_size), app.size)
                    InfoRow(stringResource(R.string.app_details_category), app.category)
                }
            }

            if (similarApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                SimilarAppsRow(
                    apps = similarApps,
                    onAppClick = onSimilarAppClick
                )
            }
        }
    }
}

/**
 * Блок «Вам может понравиться»: горизонтальный список похожих приложений.
 */
@Composable
private fun SimilarAppsRow(
    apps: List<App>,
    onAppClick: (App) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.app_details_similar),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = apps) { app ->
                SimilarAppCard(
                    app = app,
                    onClick = { onAppClick(app) }
                )
            }
        }
    }
}

/**
 * Компактная карточка похожего приложения в горизонтальном списке.
 */
@Composable
private fun SimilarAppCard(app: App, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.width(140.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = app.icon,
                    contentDescription = app.name,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = app.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = app.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Строка сведений: подпись слева, значение справа.
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End
        )
    }
}

private val demoApps = listOf(
    App(
        id = "browser",
        Icons.Default.Search,
        "Браузер",
        "Быстрый поиск в интернете и просмотр сайтов",
        "Интернет",
        developer = "StartApp Studio",
        version = "2.4.1",
        size = "38 МБ",
        fullDescription = "Браузер с поиском в один тап, вкладками, режимом " +
            "инкогнито и блокировкой всплывающих окон."
    ),
    App(
        id = "mail",
        Icons.Default.Email,
        "Почта",
        "Просмотр писем, уведомления о новых сообщениях",
        "Связь",
        developer = "StartApp Studio",
        version = "1.9.0",
        size = "24 МБ",
        fullDescription = "Почтовый клиент с поддержкой нескольких ящиков, " +
            "поиском по письмам и уведомлениями о новой почте."
    ),
    App(
        id = "gallery",
        Icons.Default.Photo,
        "Галерея",
        "Все фото и видео на устройстве в одном месте",
        "Медиа",
        developer = "StartApp Studio",
        version = "3.0.2",
        size = "52 МБ",
        fullDescription = "Собирает фото и видео по альбомам, умеет искать " +
            "по датам и местам съёмки, поддерживает обрезку и поворот."
    ),
    App(
        id = "music",
        Icons.Default.MusicNote,
        "Музыка",
        "Плеер, плейлисты и любимые исполнители",
        "Медиа",
        developer = "StartApp Studio",
        version = "2.2.7",
        size = "31 МБ",
        fullDescription = "Плеер для локальной музыки: плейлисты, очередь " +
            "воспроизведения и эквалайзер."
    ),
    App(
        id = "navigator",
        Icons.Default.DirectionsCar,
        "Навигатор",
        "Маршруты, пробки и поиск адресов",
        "Путешествия",
        developer = "StartApp Maps",
        version = "5.1.3",
        size = "76 МБ",
        fullDescription = "Прокладывает маршруты с учётом пробок, ведёт " +
            "голосовые подсказки и предупреждает о камерах."
    ),
    App(
        id = "maps",
        Icons.Default.Place,
        "Карты",
        "Карты мира, места рядом и отзывы",
        "Путешествия",
        developer = "StartApp Maps",
        version = "5.1.3",
        size = "68 МБ",
        fullDescription = "Подробные карты, поиск мест рядом, отзывы " +
            "и сохранённые точки."
    ),
    App(
        id = "shop",
        Icons.Default.ShoppingCart,
        "Магазин",
        "Покупки, заказы и доставка",
        "Покупки",
        developer = "StartApp Market",
        version = "4.6.0",
        size = "44 МБ",
        fullDescription = "Каталог товаров, корзина, отслеживание заказов " +
            "и доставка по городу."
    ),
    App(
        id = "wallet",
        Icons.Default.AccountBalanceWallet,
        "Кошелёк",
        "Платежи, переводы и история операций",
        "Финансы",
        developer = "StartApp Pay",
        version = "1.4.5",
        size = "29 МБ",
        fullDescription = "Платежи и переводы, история операций и напоминания " +
            "о регулярных списаниях."
    )
)

/** Демо-данные для запуска экрана из MainActivity. */
val sampleApps: List<App> get() = demoApps

@Preview(showBackground = true)
@Composable
private fun ReadAppPreview() {
    StartAppTheme {
        AppListScreen(apps = demoApps, onAppClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun AppDetailPreview() {
    StartAppTheme {
        AppDetailScreen(
            app = demoApps.first(),
            similarApps = demoApps.drop(1).take(4),
            onSimilarAppClick = {},
            onBack = {}
        )
    }
}
