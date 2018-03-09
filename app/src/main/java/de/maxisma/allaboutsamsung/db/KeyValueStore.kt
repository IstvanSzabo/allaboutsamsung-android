package de.maxisma.allaboutsamsung.db

import android.content.Context
import jp.takuji31.koreference.KoreferenceModel
import jp.takuji31.koreference.longPreference
import jp.takuji31.koreference.nullableStringPreference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyValueStore @Inject constructor(context: Context) : KoreferenceModel(context, name = "key_value_store") {
    /**
     * Cached HTML of ads to be injected into posts
     */
    var adHtml: String? by nullableStringPreference("adHtml")

    /**
     * Last time the list of categories in the database has been refreshed.
     */
    var lastCategoryRefreshMs: Long by longPreference(default = 0, key = "lastCategoryRefreshMs")
}