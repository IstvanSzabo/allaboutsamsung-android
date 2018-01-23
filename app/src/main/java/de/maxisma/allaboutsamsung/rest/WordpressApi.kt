package de.maxisma.allaboutsamsung.rest

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Rfc3339DateJsonAdapter
import de.maxisma.allaboutsamsung.BuildConfig
import de.maxisma.allaboutsamsung.db.CategoryId
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.db.TagId
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Date

interface WordpressApi {

    @GET("posts")
    fun posts(
        @Query("page") page: Int,
        @Query("per_page") postsPerPage: Int,
        @Query("search") search: String?,
        @Query("tags") onlyTags: TagIdsDto?,
        @Query("categories") onlyCategories: CategoryIdsDto?,
        @Query("include") onlyIds: PostIdsDto?,
        @Query("before") beforeGmt: Date? = null
    ): Deferred<List<PostDto>>

    @GET("categories?per_page=100")
    fun categories(
        @Query("page") page: Int,
        @Query("include") onlyIds: CategoryIdsDto?
    ): Deferred<Response<List<CategoryDto>>>

    @GET("tags?per_page=100")
    fun tags(
        @Query("page") page: Int,
        @Query("include") onlyIds: TagIdsDto?
    ): Deferred<Response<List<TagDto>>>

    @GET("users")
    fun users(
        @Query("page") page: Int,
        @Query("include") onlyIds: UserIdsDto?
    ): Deferred<Response<List<UserDto>>>

}

private const val TOTAL_PAGES_HEADER = "X-WP-TotalPages"

fun WordpressApi.allCategories(onlyIds: CategoryIdsDto?): Deferred<List<CategoryDto>> =
    fetchAll { page -> categories(page, onlyIds) }

fun WordpressApi.allTags(onlyIds: TagIdsDto?): Deferred<List<TagDto>> =
    fetchAll { page -> tags(page, onlyIds) }

fun WordpressApi.allUsers(onlyIds: UserIdsDto?): Deferred<List<UserDto>> =
    fetchAll { page -> users(page, onlyIds) }

private fun <T> fetchAll(pageFetcher: (Int) -> Deferred<Response<List<T>>>): Deferred<List<T>> = async {
    var page = 1
    var pages = 1
    val elements = mutableListOf<T>()
    while (page - 1 < pages) {
        val resp = pageFetcher(page).await()
        pages = resp.headers()[TOTAL_PAGES_HEADER]!!.toInt()
        elements += resp.body()!!
        page++
    }
    elements
}

typealias CategoryIdDto = Int
typealias TagIdDto = Int
typealias UserIdDto = Int

class TagIdsDto(
    val ids: Set<TagId>
) {
    override fun toString() = ids.joinToString(separator = ",")
}

class CategoryIdsDto(
    val ids: Set<CategoryId>
) {
    override fun toString() = ids.joinToString(separator = ",")
}

class PostIdsDto(
    val ids: Set<PostId>
) {
    override fun toString() = ids.joinToString(separator = ",")
}

class UserIdsDto(
    val ids: Set<UserIdDto>
) {
    override fun toString() = ids.joinToString(separator = ",")
}

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

data class TitleDto(
    val rendered: String
)

data class ContentDto(
    val rendered: String
)

data class CategoryDto(
    val id: Int,
    val count: Int,
    val description: String,
    val name: String,
    val slug: String
)

data class TagDto(
    val id: Int,
    val count: Int,
    val description: String,
    val name: String,
    val slug: String
)

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
    .build()

// We assume that all dates in DTOs are GMT dates

private val rfc3339Adapter = Rfc3339DateJsonAdapter()

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
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

val wordpressApi: WordpressApi = wordpressRetrofit.create(WordpressApi::class.java)