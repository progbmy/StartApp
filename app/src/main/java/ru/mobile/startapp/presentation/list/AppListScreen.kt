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
import ru.mobile.startapp.data.App
import ru.mobile.startapp.data.demoApps
import ru.mobile.startapp.presentation.components.LoadingState
import ru.mobile.startapp.presentation.icon
import ru.mobile.startapp.ui.theme.StartAppTheme

/**
 * Экран списка приложений: тулбар и вертикальный список.
 * Состояние приходит снаружи, экран ничего не знает про ViewModel.
 */
@Composable
fun AppListScreen(
    uiState: AppListUiState,
    onAppClick: (App) -> Unit,
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
                onAppClick = onAppClick,
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
 * Карточка приложения в списке: иконка, название, описание, категория.
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

@Preview(showBackground = true)
@Composable
private fun AppListScreenPreview() {
    StartAppTheme {
        AppListScreen(
            uiState = AppListUiState.Content(demoApps),
            onAppClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Список: загрузка")
@Composable
private fun AppListLoadingPreview() {
    StartAppTheme {
        AppListScreen(
            uiState = AppListUiState.Loading,
            onAppClick = {}
        )
    }
}
