package ru.mobile.startapp.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.mobile.startapp.R
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.domain.usecase.RatingsLoadResult
import ru.mobile.startapp.presentation.components.LoadingState
import ru.mobile.startapp.presentation.icon
import ru.mobile.startapp.ui.theme.StartAppTheme
import java.util.Locale

/**
 * Экран списка приложений: тулбар, блок замера рейтингов и вертикальный список.
 * Состояние приходит снаружи, экран ничего не знает про ViewModel.
 */
@Composable
fun AppListScreen(
    uiState: AppListUiState,
    onAppClick: (App) -> Unit,
    onRerunRatings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { ToolBar() }
    ) { innerPadding ->
        when (uiState) {
            AppListUiState.Loading -> LoadingState(modifier = Modifier.padding(innerPadding))
            is AppListUiState.Content -> ListApp(
                listApp = uiState.apps,
                ratings = uiState.ratings,
                sequential = uiState.sequential,
                parallel = uiState.parallel,
                runningMode = uiState.runningMode,
                onAppClick = onAppClick,
                onRerunRatings = onRerunRatings,
                modifier = Modifier.padding(innerPadding)
            )
        }
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
 * Вертикальный список приложений: сверху блок с результатами замера рейтингов.
 */
@Composable
private fun ListApp(
    listApp: List<App>,
    ratings: Map<String, Float>,
    sequential: RatingsLoadResult?,
    parallel: RatingsLoadResult?,
    runningMode: RatingLoadMode?,
    onAppClick: (App) -> Unit,
    onRerunRatings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RatingsBenchmarkCard(
                sequential = sequential,
                parallel = parallel,
                runningMode = runningMode,
                onRerun = onRerunRatings
            )
        }

        items(items = listApp, key = { it.id }) { app ->
            AppCard(
                app = app,
                rating = ratings[app.id],
                onClick = { onAppClick(app) }
            )
        }
    }
}

/**
 * Блок замера: время последовательной и параллельной загрузки рейтингов.
 *
 * Время снимается в [ru.mobile.startapp.domain.usecase.LoadRatingsUseCase]
 * через `measureTimeMillis`, здесь только показывается.
 */
@Composable
private fun RatingsBenchmarkCard(
    sequential: RatingsLoadResult?,
    parallel: RatingsLoadResult?,
    runningMode: RatingLoadMode?,
    onRerun: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.list_ratings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.size(4.dp))

            Text(
                text = stringResource(R.string.list_ratings_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.size(12.dp))

            RatingResultRow(
                label = stringResource(R.string.list_ratings_sequential_label),
                result = sequential,
                running = runningMode == RatingLoadMode.Sequential
            )

            Spacer(modifier = Modifier.size(6.dp))

            RatingResultRow(
                label = stringResource(R.string.list_ratings_parallel_label),
                result = parallel,
                running = runningMode == RatingLoadMode.Parallel
            )

            if (sequential != null && parallel != null && parallel.elapsedMs > 0) {
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = stringResource(
                        R.string.list_ratings_speedup,
                        formatDecimal(sequential.elapsedMs.toFloat() / parallel.elapsedMs.toFloat())
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            Button(onClick = onRerun, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.list_ratings_retry))
            }
        }
    }
}

/**
 * Строка результата: подпись способа слева, время и число запросов справа.
 */
@Composable
private fun RatingResultRow(
    label: String,
    result: RatingsLoadResult?,
    running: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        when {
            running -> Text(
                text = stringResource(R.string.list_ratings_running),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            result == null -> Text(
                text = "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> Text(
                text = formatSeconds(result.elapsedMs) + " · " +
                    stringResource(R.string.list_ratings_requests, result.requests),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Карточка приложения в списке: иконка, название с рейтингом, описание, категория.
 */
@Composable
private fun AppCard(
    app: App,
    rating: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    imageVector = app.icon(),
                    contentDescription = app.name,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (rating != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = stringResource(R.string.list_rating_content_desc),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = formatDecimal(rating),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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

/** Время в секундах с одним знаком после запятой. */
private fun formatSeconds(elapsedMs: Long): String =
    String.format(Locale.getDefault(), "%.1f с", elapsedMs / 1000f)

/** Число с одним знаком после запятой в текущей локали. */
private fun formatDecimal(value: Float): String =
    String.format(Locale.getDefault(), "%.1f", value)

@Preview(showBackground = true)
@Composable
private fun AppListScreenPreview() {
    StartAppTheme {
        AppListScreen(
            uiState = AppListUiState.Content(
                apps = demoAppsPreview,
                ratings = mapOf("browser" to 4.5f, "mail" to 3.8f),
                sequential = RatingsLoadResult(emptyMap(), 15_100, 10),
                parallel = RatingsLoadResult(emptyMap(), 1_610, 10)
            ),
            onAppClick = {},
            onRerunRatings = {}
        )
    }
}

@Preview(showBackground = true, name = "Список: загрузка")
@Composable
private fun AppListLoadingPreview() {
    StartAppTheme {
        AppListScreen(
            uiState = AppListUiState.Loading,
            onAppClick = {},
            onRerunRatings = {}
        )
    }
}

/** Демо-данные для превью: доменные модели, а не DTO. */
private val demoAppsPreview = listOf(
    ru.mobile.startapp.domain.App(
        id = "browser", name = "Браузер",
        description = "Быстрый поиск в интернете и просмотр сайтов",
        category = "Интернет"
    ),
    ru.mobile.startapp.domain.App(
        id = "mail", name = "Почта",
        description = "Просмотр писем, уведомления о новых сообщениях",
        category = "Связь"
    ),
    ru.mobile.startapp.domain.App(
        id = "gallery", name = "Галерея",
        description = "Все фото и видео на устройстве в одном месте",
        category = "Медиа"
    )
)
