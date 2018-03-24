package de.maxisma.allaboutsamsung.categories

import android.arch.lifecycle.LiveData
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import de.maxisma.allaboutsamsung.db.Category
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.KeyValueStore
import de.maxisma.allaboutsamsung.db.importCategoryDtos
import de.maxisma.allaboutsamsung.rest.WordpressApi
import de.maxisma.allaboutsamsung.rest.allCategories
import de.maxisma.allaboutsamsung.utils.retry
import kotlinx.coroutines.experimental.TimeoutCancellationException
import kotlinx.coroutines.experimental.withTimeout
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

private const val CATEGORY_CACHE_EXPIRY_DURATION_MS = 24L * 60 * 60 * 1000

private const val TIMEOUT_MS = 10000

/**
 * Repository for categories. Call [refreshIfNeeded] to make sure that
 * a recent list of categories in present in the database.
 */
class CategoryCache @Inject constructor(private val db: Db, private val keyValueStore: KeyValueStore, private val wordpressApi: WordpressApi) {
    fun categories(): LiveData<List<Category>> = db.categoryDao.categories()

    suspend fun refreshIfNeeded() {
        if (keyValueStore.lastCategoryRefreshMs + CATEGORY_CACHE_EXPIRY_DURATION_MS >= System.currentTimeMillis()) {
            return
        }

        val categoryDtos = try {
            retry(
                HttpException::class,
                JsonDataException::class,
                JsonEncodingException::class,
                IOException::class,
                TimeoutCancellationException::class
            ) {
                withTimeout(TIMEOUT_MS) {
                    wordpressApi.allCategories(onlyIds = null).await()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        keyValueStore.lastCategoryRefreshMs = System.currentTimeMillis()
        db.importCategoryDtos(categoryDtos, deleteAllExcept = true)
    }
}