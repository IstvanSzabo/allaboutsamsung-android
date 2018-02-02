package de.maxisma.allaboutsamsung.rest

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import de.maxisma.allaboutsamsung.BuildConfig
import kotlinx.coroutines.experimental.Deferred
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

interface AdApi {
    @GET("ad")
    fun adForPost(): Deferred<AdDto>
}

data class AdDto(val html: String)

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val adRetrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.AD_BASE_URL)
    .client(httpClient)
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()

val adApi: AdApi = adRetrofit.create(AdApi::class.java)