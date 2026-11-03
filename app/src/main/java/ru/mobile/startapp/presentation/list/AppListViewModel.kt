package ru.mobile.startapp.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.domain.repository.AppRepository
import javax.inject.Inject

/** Состояние экрана списка приложений. */
sealed interface AppListUiState {

    data object Loading : AppListUiState

    data class Content(val apps: List<App>) : AppListUiState
}

/**
 * ViewModel списка: подписывается на каталог из репозитория
 * и отдаёт экрану готовое состояние.
 *
 * [AppRepository] подставляется через Hilt DI.
 */
@HiltViewModel
class AppListViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    val uiState: StateFlow<AppListUiState> = repository.apps
        .map { apps -> AppListUiState.Content(apps) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppListUiState.Loading
        )
}
