package com.carne.podcast.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun toDownloadState(value: String?): DownloadState =
        value?.let { runCatching { DownloadState.valueOf(it) }.getOrNull() } ?: DownloadState.NONE

    @TypeConverter
    fun fromDownloadState(state: DownloadState): String = state.name
}

@Database(
    entities = [PodcastEntity::class, EpisodeEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class CarneDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
}
