package ru.mobile.startapp.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.mobile.startapp.data.EmulatedRatingRepository
import ru.mobile.startapp.data.NetworkAppRepository
import ru.mobile.startapp.data.source.network.EmulatedRatingApi
import ru.mobile.startapp.data.source.network.RatingApi
import ru.mobile.startapp.domain.repository.AppRepository
import ru.mobile.startapp.domain.repository.RatingRepository

/**
 * Hilt-модуль для биндинга [AppRepository].
 *
 * Связывает интерфейс [AppRepository] с реализацией [NetworkAppRepository]
 * через @Binds — при запросе AppRepository Hilt подставит сетевую реализацию.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindAppRepository(impl: NetworkAppRepository): AppRepository

    /** Рейтинги: эмулированный API → репозиторий. */
    @Binds
    abstract fun bindRatingApi(impl: EmulatedRatingApi): RatingApi

    @Binds
    abstract fun bindRatingRepository(impl: EmulatedRatingRepository): RatingRepository
}
