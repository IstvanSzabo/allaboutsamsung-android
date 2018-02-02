package de.maxisma.allaboutsamsung.db

import android.content.Context
import jp.takuji31.koreference.KoreferenceModel
import jp.takuji31.koreference.nullableStringPreference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyValueStore @Inject constructor(context: Context) : KoreferenceModel(context, name = "key_value_store") {
    var adHtml: String? by nullableStringPreference("adHtml")
}