package ru.mobile.startapp.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.mobile.startapp.data.App
import ru.mobile.startapp.data.AppRepository

/** Состояние экрана списка приложений. */
sealed interface AppListUiState {

    data object Loading : AppListUiState

    data class Content(val apps: List<App>) : AppListUiState
}

/**
 * ViewModel списка: подписывается на каталог из репозитория
 * и отдаёт экрану готовое состояние.
 */
class AppListViewModel(repository: AppRepository) : ViewModel() {

    val uiState: StateFlow<AppListUiState> = repository.apps
        .map { apps -> AppListUiState.Content(apps) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppListUiState.Loading
        )

    companion object {

        fun factory(repository: AppRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { AppListViewModel(repository) }
        }
    }
}
