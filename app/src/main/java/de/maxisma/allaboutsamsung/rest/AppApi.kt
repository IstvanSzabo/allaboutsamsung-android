package de.maxisma.allaboutsamsung.rest

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import de.maxisma.allaboutsamsung.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AppApi {
    @GET("ad")
    suspend fun adForPost(): AdDto

    @POST("url-to-id")
    suspend fun urlToId(@Body urlDto: UrlDto): IdDto
}

suspend fun AppApi.urlToId(url: String) = urlToId(UrlDto(url)).id

@JsonClass(generateAdapter = true)
data class AdDto(val html: String)

@JsonClass(generateAdapter = true)
data class UrlDto(val url: String)

@JsonClass(generateAdapter = true)
data class IdDto(val id: Long)

private val moshi = Moshi.Builder()
    .build()

private val appApiRetrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.APP_API_BASE_URL)
    .client(httpClient)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()

val appApi = appApiRetrofit.create<AppApi>()