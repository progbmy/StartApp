package ru.mobile.startapp

import android.R.attr.text
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.mobile.startapp.ui.theme.StartAppTheme

/**
 * Модель одного приложения в списке.
 *
 * @param icon        иконка приложения
 * @param name        название приложения
 * @param description краткое описание приложения
 * @param category    категория приложения
 */
data class App(val icon: ImageVector, val name: String, val description: String, val category: String)

/**
 * Экран целиком: тулбар + вертикальный список приложений.
 */
@Composable
fun ReadApp(apps: List<App>, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { ToolBar() }
    ) { innerPadding ->
        ListApp(
            listApp = apps,
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
 * Вертикальный список приложений.
 */
@Composable
private fun ListApp(listApp: List<App>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = listApp) { app ->
            AppCard(app = app)
        }
    }
}

/**
 * Карточка одного приложения: иконка, название, описание, категория.
 */
@Composable
private fun AppCard(app: App, modifier: Modifier = Modifier) {
    Card(
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
        }
    }
}

private val demoApps = listOf(
    App(Icons.Default.Search, "Браузер", "Быстрый поиск в интернете и просмотр сайтов", "Интернет"),
    App(Icons.Default.Email, "Почта", "Просмотр писем, уведомления о новых сообщениях", "Связь"),
    App(Icons.Default.Photo, "Галерея", "Все фото и видео на устройстве в одном месте", "Медиа"),
    App(Icons.Default.MusicNote, "Музыка", "Плеер, плейлисты и любимые исполнители", "Медиа"),
    App(Icons.Default.DirectionsCar, "Навигатор", "Маршруты, пробки и поиск адресов", "Путешествия"),
    App(Icons.Default.Place, "Карты", "Карты мира, места рядом и отзывы", "Путешествия"),
    App(Icons.Default.ShoppingCart, "Магазин", "Покупки, заказы и доставка", "Покупки"),
    App(Icons.Default.AccountBalanceWallet, "Кошелёк", "Платежи, переводы и история операций", "Финансы")
)

/** Демо-данные для запуска экрана из MainActivity. */
val sampleApps: List<App> get() = demoApps
