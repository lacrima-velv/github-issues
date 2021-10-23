package com.example.githubissues.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.githubissues.model.Issue

@Database(
    entities = [Issue::class, RemoteKeys::class],
    version = 1,
    exportSchema = false
)
abstract class IssuesDatabase : RoomDatabase() {

    abstract fun issuesDao(): IssuesDao
    abstract fun remoteKeysDao(): RemoteKeysDao

    companion object {
        /*
        INSTANCE will keep a reference to any database returned via getInstance
        Volatile value will never be cached.
        Changes made by one thread to shared data are visible to other threads.
         */
        @Volatile
        private var INSTANCE: IssuesDatabase? = null
        /*
        Helper function to get the database. If it has already been retrieved, the previous database
        will be returned.
         */
        fun getInstance(context: Context): IssuesDatabase =
            /*
             Use synchronized as it may be called by multiple threads, but we need to be sure that
             the database is initialized only once
             */
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: buildDatabase(context).also { INSTANCE = it }
            }
        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                IssuesDatabase::class.java,
                "GithubIssuesDatabase")
                // Wipes and rebuilds instead of migrating if no Migration object.
                .fallbackToDestructiveMigration()
                .build()
    }
}