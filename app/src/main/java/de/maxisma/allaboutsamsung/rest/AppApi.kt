package de.maxisma.allaboutsamsung.rest

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import de.maxisma.allaboutsamsung.BuildConfig
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AppApi {
    @GET("ad")
    fun adForPost(): Deferred<AdDto>

    @POST("url-to-id")
    fun urlToId(@Body urlDto: UrlDto): Deferred<IdDto>
}

fun AppApi.urlToId(url: String) = async {
    urlToId(UrlDto(url)).await().id
}

data class AdDto(val html: String)

data class UrlDto(val url: String)

data class IdDto(val id: Long)

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val appApiRetrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.APP_API_BASE_URL)
    .client(httpClient)
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()

val appApi: AppApi = appApiRetrofit.create(AppApi::class.java)