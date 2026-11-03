package ru.mobile.startapp.data.source.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * API-интерфейс каталога приложений.
 *
 * Эндпоинты:
 * - [catalog] — получить весь каталог
 * - [appById] — получить отдельное приложение (заготовка для DZ-9)
 */
interface CatalogApiService {

    /** Полный каталог приложений. */
    @GET("catalog")
    suspend fun catalog(): Response<List<CatalogAppDto>>

    /** Информация о конкретном приложении по id. */
    @GET("{id}")
    suspend fun appById(@Path("id") appId: String): Response<CatalogAppDto>
}
