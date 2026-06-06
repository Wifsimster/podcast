package com.carne.podcast.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.carne.podcast.data.local.CarneDatabase
import com.carne.podcast.data.local.EpisodeDao
import com.carne.podcast.data.local.PodcastDao
import com.carne.podcast.data.local.QueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CarneDatabase =
        Room.databaseBuilder(context, CarneDatabase::class.java, "carne.db")
            // Real migrations preserve the user's library & listening history
            // across schema bumps; destructive fallback is only a last resort.
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePodcastDao(db: CarneDatabase): PodcastDao = db.podcastDao()

    @Provides
    fun provideEpisodeDao(db: CarneDatabase): EpisodeDao = db.episodeDao()

    @Provides
    fun provideQueueDao(db: CarneDatabase): QueueDao = db.queueDao()

    /** v2 adds the persistent Up-Next queue table. */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `queue` (" +
                    "`episodeId` TEXT NOT NULL, " +
                    "`sortIndex` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`episodeId`))"
            )
        }
    }
}
