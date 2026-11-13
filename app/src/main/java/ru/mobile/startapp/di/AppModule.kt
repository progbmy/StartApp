package ru.mobile.startapp.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.mobile.startapp.data.NetworkAppRepository
import ru.mobile.startapp.domain.repository.AppRepository

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
}
