package ru.mobile.startapp.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.mobile.startapp.data.DemoAppRepositoryImpl
import ru.mobile.startapp.domain.repository.AppRepository

/**
 * Hilt-модуль для绑定 [AppRepository].
 *
 * Связывает интерфейс [AppRepository] с реализацией [DemoAppRepositoryImpl]
 * через @Binds — при запросе AppRepository Hilt подставит DemoAppRepositoryImpl.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindAppRepository(impl: DemoAppRepositoryImpl): AppRepository
}
