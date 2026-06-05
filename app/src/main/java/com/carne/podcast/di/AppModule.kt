package com.carne.podcast.di

import android.content.Context
import androidx.room.Room
import com.carne.podcast.data.local.CarneDatabase
import com.carne.podcast.data.local.EpisodeDao
import com.carne.podcast.data.local.PodcastDao
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
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePodcastDao(db: CarneDatabase): PodcastDao = db.podcastDao()

    @Provides
    fun provideEpisodeDao(db: CarneDatabase): EpisodeDao = db.episodeDao()
}
