package de.maxisma.allaboutsamsung.rest

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.db.CategoryId
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.db.TagId
import kotlinx.coroutines.Deferred
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Date
import java.util.concurrent.TimeUnit

// This file implements an API client for the WP REST API v2

interface WordpressApi {

    @GET("posts")
    fun postsAsync(
        @Query("page") page: Int,
        @Query("per_page") postsPerPage: Int,
        @Query("search") search: String?,
        @Query("tags") onlyTags: TagIdsDto?,
        @Query("categories") onlyCategories: CategoryIdsDto?,
        @Query("include") onlyIds: PostIdsDto?,
        @Query("before") beforeGmt: Date? = null
    ): Deferred<List<PostDto>>

    /**
     * See [allCategories] to avoid paging
     */
    @GET("categories?per_page=100")
    fun categoriesAsync(
        @Query("page") page: Int,
        @Query("include") onlyIds: CategoryIdsDto?
    ): Deferred<Response<List<CategoryDto>>>

    /**
     * See [allTags] to avoid paging
     */
    @GET("tags?per_page=100")
    fun tagsAsync(
        @Query("page") page: Int,
        @Query("include") onlyIds: TagIdsDto?
    ): Deferred<Response<List<TagDto>>>

    /**
     * See [allUsers] to avoid paging
     */
    @GET("users")
    fun usersAsync(
        @Query("page") page: Int,
        @Query("include") onlyIds: UserIdsDto?
    ): Deferred<Response<List<UserDto>>>

}

private const val TOTAL_PAGES_HEADER = "X-WP-TotalPages"

suspend fun WordpressApi.allCategories(onlyIds: CategoryIdsDto?): List<CategoryDto> =
    fetchAll { page -> categoriesAsync(page, onlyIds) }

suspend fun WordpressApi.allTags(onlyIds: TagIdsDto?): List<TagDto> =
    fetchAll { page -> tagsAsync(page, onlyIds) }

suspend fun WordpressApi.allUsers(onlyIds: UserIdsDto?): List<UserDto> =
    fetchAll { page -> usersAsync(page, onlyIds) }

/**
 * Go through all pages provided by WordPress and combine them
 */
private suspend fun <T> fetchAll(pageFetcher: (Int) -> Deferred<Response<List<T>>>): List<T> {
    var page = 1
    var pages = 1
    val elements = mutableListOf<T>()
    while (page - 1 < pages) {
        val resp = pageFetcher(page).await()
        pages = resp.headers()[TOTAL_PAGES_HEADER]!!.toInt()
        elements += resp.body() ?: break // In case of an error, we simply abort here
        page++
    }
    return elements
}

typealias CategoryIdDto = Int
typealias TagIdDto = Int
typealias UserIdDto = Int

@Suppress("MemberVisibilityCanBePrivate")
@JsonClass(generateAdapter = true)
class TagIdsDto(
    val ids: Set<TagId>
) {
    override fun toString() = ids.joinToString(separator = ",")
}

@Suppress("MemberVisibilityCanBePrivate")
@JsonClass(generateAdapter = true)
class CategoryIdsDto(
    val ids: Set<CategoryId>
) {
    override fun toString() = ids.joinToString(separator = ",")
}

@Suppress("MemberVisibilityCanBePrivate")
@JsonClass(generateAdapter = true)
class PostIdsDto(
    val ids: Set<PostId>
) {
    override fun toString() = ids.joinToString(separator = ",")
}

@Suppress("MemberVisibilityCanBePrivate")
@JsonClass(generateAdapter = true)
class UserIdsDto(
    val ids: Set<UserIdDto>
) {
    override fun toString() = ids.joinToString(separator = ",")
}

@JsonClass(generateAdapter = true)
data class PostDto(
    val id: Long,
    val date_gmt: Date,
    val slug: String,
    val link: String,
    val title: TitleDto,
    val content: ContentDto,
    val author: Int,
    val categories: List<CategoryIdDto>,
    val tags: List<TagIdDto>
)

@JsonClass(generateAdapter = true)
data class TitleDto(
    val rendered: String
)

@JsonClass(generateAdapter = true)
data class ContentDto(
    val rendered: String
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
    val id: Int,
    val count: Int,
    val description: String,
    val name: String,
    val slug: String
)

@JsonClass(generateAdapter = true)
data class TagDto(
    val id: Int,
    val count: Int,
    val description: String,
    val name: String,
    val slug: String
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: UserIdDto,
    val name: String
)

val httpClient: OkHttpClient = OkHttpClient.Builder()
    .apply {
        if (BuildConfig.DEBUG) {
            addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        }
    }
    .callTimeout(15, TimeUnit.SECONDS)
    .build()

// We assume that all dates in DTOs are GMT dates

private val rfc3339Adapter = Rfc3339DateJsonAdapter()

private val moshi = Moshi.Builder()
    .add(Date::class.java, object : JsonAdapter<Date>() {
        override fun toJson(writer: JsonWriter, value: Date?) = rfc3339Adapter.toJson(writer, value)
        override fun fromJson(reader: JsonReader) = rfc3339Adapter.fromJsonValue(reader.nextString() + "+00:00")
    })
    .build()

private val wordpressRetrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.REST_BASE_URL)
    .client(httpClient)
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .addConverterFactory(RetrofitDateStringConverterFactory)
    .build()

val wordpressApi = wordpressRetrofit.create<WordpressApi>()