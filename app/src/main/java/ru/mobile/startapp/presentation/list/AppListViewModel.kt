package ru.mobile.startapp.presentation.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.mobile.startapp.domain.App
import ru.mobile.startapp.domain.repository.AppRepository
import ru.mobile.startapp.domain.usecase.LoadRatingsUseCase
import ru.mobile.startapp.domain.usecase.RatingsLoadResult
import javax.inject.Inject

/** Какой способ загрузки рейтингов выполняется прямо сейчас. */
enum class RatingLoadMode { Sequential, Parallel }

/** Состояние экрана списка приложений. */
sealed interface AppListUiState {

    data object Loading : AppListUiState

    data class Content(
        val apps: List<App>,
        val ratings: Map<String, Float> = emptyMap(),
        val sequential: RatingsLoadResult? = null,
        val parallel: RatingsLoadResult? = null,
        val runningMode: RatingLoadMode? = null
    ) : AppListUiState
}

/**
 * ViewModel списка: подписывается на каталог из репозитория,
 * подгружает рейтинги двумя способами и отдаёт экрану готовое состояние.
 *
 * [AppRepository] и [LoadRatingsUseCase] подставляются через Hilt DI.
 */
@HiltViewModel
class AppListViewModel @Inject constructor(
    private val repository: AppRepository,
    private val loadRatings: LoadRatingsUseCase
) : ViewModel() {

    /** Загрузка рейтингов — отдельное состояние: каталог живёт в репозитории. */
    private val _ratings = MutableStateFlow(RatingsState())

    val uiState: StateFlow<AppListUiState> = combine(repository.apps, _ratings) { apps, ratings ->
        if (apps.isEmpty()) {
            AppListUiState.Loading
        } else {
            AppListUiState.Content(
                apps = apps,
                ratings = ratings.ratings,
                sequential = ratings.sequential,
                parallel = ratings.parallel,
                runningMode = ratings.runningMode
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppListUiState.Loading
    )

    /** Текущая загрузка рейтингов: держим ссылку, чтобы перезапуск отменял предыдущий. */
    private var ratingsJob: Job? = null

    init {
        // ждём, пока репозиторий отдаст каталог, и сразу замеряем оба способа
        viewModelScope.launch {
            val apps = repository.apps.first { it.isNotEmpty() }
            runBenchmark(apps)
        }
    }

    /** Кнопка «Замерить заново»: повторяет оба замера на текущем списке. */
    fun onRerunRatings() {
        val apps = (uiState.value as? AppListUiState.Content)?.apps ?: return
        runBenchmark(apps)
    }

    /**
     * Последовательно, затем параллельно — на одном и том же наборе из
     * [RATINGS_SAMPLE_SIZE] приложений, чтобы сравнение было честным.
     *
     * Рейтинги остальных приложений каталога догружаются параллельно уже
     * только для UI: это не замер, а заполнение карточек.
     */
    private fun runBenchmark(apps: List<App>) {
        if (apps.isEmpty()) return

        val sample = apps.take(RATINGS_SAMPLE_SIZE).map(App::id)
        val rest = apps.drop(RATINGS_SAMPLE_SIZE).map(App::id)

        ratingsJob?.cancel()
        ratingsJob = viewModelScope.launch {
            try {
                _ratings.update { it.copy(runningMode = RatingLoadMode.Sequential) }
                val sequential = loadRatings.loadSequentially(sample)
                _ratings.update {
                    it.copy(
                        sequential = sequential,
                        ratings = sequential.ratings,
                        runningMode = RatingLoadMode.Parallel
                    )
                }

                val parallel = loadRatings.loadInParallel(sample)
                _ratings.update {
                    it.copy(
                        parallel = parallel,
                        ratings = parallel.ratings,
                        runningMode = null
                    )
                }

                Log.d(
                    TAG,
                    "Рейтинги (${sample.size} запросов): последовательно " +
                        "${sequential.elapsedMs} мс, параллельно ${parallel.elapsedMs} мс"
                )

                if (rest.isNotEmpty()) {
                    val others = loadRatings.loadInParallel(rest)
                    _ratings.update { it.copy(ratings = parallel.ratings + others.ratings) }
                }
            } finally {
                _ratings.update { it.copy(runningMode = null) }
            }
        }
    }

    /** Внутреннее состояние загрузки рейтингов. */
    private data class RatingsState(
        val ratings: Map<String, Float> = emptyMap(),
        val sequential: RatingsLoadResult? = null,
        val parallel: RatingsLoadResult? = null,
        val runningMode: RatingLoadMode? = null
    )

    companion object {

        private const val TAG = "AppListViewModel"

        /** Сколько приложений участвует в замере (по заданию — 10+). */
        const val RATINGS_SAMPLE_SIZE = 10
    }
}
