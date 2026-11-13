package ru.mobile.startapp.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.mobile.startapp.data.source.network.CatalogApiService
import javax.inject.Singleton

/**
 * Hilt-модуль для создания сетевого слоя.
 *
 * Создаёт Retrofit-клиент с Gson-конвертером и предоставляет API-сервис.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "http://185.103.109.134/"

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideCatalogApiService(retrofit: Retrofit): CatalogApiService =
        retrofit.create(CatalogApiService::class.java)
}
