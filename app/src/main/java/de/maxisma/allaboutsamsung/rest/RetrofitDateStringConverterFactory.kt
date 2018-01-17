package de.maxisma.allaboutsamsung.rest

import de.maxisma.allaboutsamsung.utils.Iso8601Utils
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import java.util.Date

private object DateStringConverter: Converter<Date, String> {
    override fun convert(value: Date?): String? = if (value == null) null else Iso8601Utils.format(value)
}

/**
 * Needed for query parameter serialization. See [https://stackoverflow.com/a/42459356/1502352].
 */
object RetrofitDateStringConverterFactory : Converter.Factory() {
    override fun stringConverter(type: Type, annotations: Array<out Annotation>, retrofit: Retrofit): Converter<*, String>? {
        if (type == Date::class.java) {
            return DateStringConverter
        }

        return super.stringConverter(type, annotations, retrofit)
    }
}