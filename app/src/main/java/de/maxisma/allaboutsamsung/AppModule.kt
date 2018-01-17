package de.maxisma.allaboutsamsung

import android.arch.persistence.room.Room
import android.content.Context
import dagger.Module
import dagger.Provides
import de.maxisma.allaboutsamsung.db.Db

@Module
class AppModule(private val app: App) {
    @Provides
    fun app(): App = app

    @Provides
    fun context(): Context = app

    @Provides
    fun room(): Db = Room.databaseBuilder(app, Db::class.java, "db").build()
}