package ovh.battistella.ondes.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ovh.battistella.ondes.data.local.OndesDatabase
import ovh.battistella.ondes.data.local.EpisodeDao
import ovh.battistella.ondes.data.local.PodcastDao
import ovh.battistella.ondes.data.local.QueueDao
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
    fun provideDatabase(@ApplicationContext context: Context): OndesDatabase =
        Room.databaseBuilder(context, OndesDatabase::class.java, "ondes.db")
            // Real migrations preserve the user's library & listening history
            // across schema bumps; destructive fallback is only a last resort.
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePodcastDao(db: OndesDatabase): PodcastDao = db.podcastDao()

    @Provides
    fun provideEpisodeDao(db: OndesDatabase): EpisodeDao = db.episodeDao()

    @Provides
    fun provideQueueDao(db: OndesDatabase): QueueDao = db.queueDao()

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

    /** v3 adds a nullable chapters-JSON URL to episodes (Podcasting 2.0 chapters). */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `episodes` ADD COLUMN `chaptersUrl` TEXT")
        }
    }

    /** v4 adds per-podcast playback-speed override and auto-download flag. */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `podcasts` ADD COLUMN `overrideSpeed` REAL")
            db.execSQL("ALTER TABLE `podcasts` ADD COLUMN `autoDownload` INTEGER NOT NULL DEFAULT 0")
        }
    }

    /** v5 indexes episodes.downloadState so the Downloads query stops table-scanning. */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_episodes_downloadState` " +
                    "ON `episodes` (`downloadState`)"
            )
        }
    }
}
