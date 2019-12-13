package de.maxisma.allaboutsamsung.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration1_2: Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Nothing to do
    }
}