package de.maxisma.allaboutsamsung.utils

import android.content.res.TypedArray

inline fun <T> TypedArray.use(f: (TypedArray) -> T): T = try {
    f(this)
} finally {
    recycle()
}