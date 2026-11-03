package ru.mobile.startapp.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.mobile.startapp.R
import ru.mobile.startapp.data.App
import ru.mobile.startapp.data.demoApps
import ru.mobile.startapp.presentation.components.LoadingState
import ru.mobile.startapp.presentation.components.MessageState
import ru.mobile.startapp.presentation.icon
import ru.mobile.startapp.ui.theme.StartAppTheme

/**
 * Карточка приложения: состояние приходит снаружи, экран stateless.
 */
@Composable
fun AppDetailScreen(
    uiState: AppDetailUiState,
    snackbarHostState: SnackbarHostState,
    onInstallClick: () -> Unit,
    onSimilarAppClick: (App) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = (uiState as? AppDetailUiState.Content)?.app?.name.orEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { DetailToolBar(title = title, onBack = onBack) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        when (uiState) {
            AppDetailUiState.Loading ->
                LoadingState(modifier = Modifier.padding(innerPadding))

            AppDetailUiState.NotFound -> MessageState(
                text = stringResource(R.string.app_details_not_found),
                modifier = Modifier.padding(innerPadding)
            )

            is AppDetailUiState.Content -> DetailContent(
                app = uiState.app,
                similarApps = uiState.similarApps,
                onInstallClick = onInstallClick,
                onSimilarAppClick = onSimilarAppClick,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

/**
 * Тулбар карточки: кнопка «назад» и название приложения.
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
 * Содержимое карточки: иконка, название, описание, кнопка установки,
 * сведения о приложении и блок «Вам может понравиться».
 */
@Composable
private fun DetailContent(
    app: App,
    similarApps: List<App>,
    onInstallClick: () -> Unit,
    onSimilarAppClick: (App) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // при переходе между похожими приложениями карточка открывается с начала
    LaunchedEffect(app.id) { scrollState.scrollTo(0) }

    Column(
        modifier = modifier
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
                imageVector = app.icon(),
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
            onClick = onInstallClick,
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
                    imageVector = app.icon(),
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

@Preview(showBackground = true)
@Composable
private fun AppDetailContentPreview() {
    StartAppTheme {
        AppDetailScreen(
            uiState = AppDetailUiState.Content(
                app = demoApps.first(),
                similarApps = demoApps.drop(1).take(4)
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onInstallClick = {},
            onSimilarAppClick = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Карточка: не найдено")
@Composable
private fun AppDetailNotFoundPreview() {
    StartAppTheme {
        AppDetailScreen(
            uiState = AppDetailUiState.NotFound,
            snackbarHostState = remember { SnackbarHostState() },
            onInstallClick = {},
            onSimilarAppClick = {},
            onBack = {}
        )
    }
}
