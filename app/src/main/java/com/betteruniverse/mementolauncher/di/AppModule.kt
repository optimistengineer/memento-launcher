package com.betteruniverse.mementolauncher.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.betteruniverse.mementolauncher.data.*
import com.betteruniverse.mementolauncher.ui.managers.TimeManager
import com.betteruniverse.mementolauncher.ui.managers.WidgetManager
import com.betteruniverse.mementolauncher.wallpaper.WallpaperUpdater
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.inject.Qualifier

/**
 * Qualifier for the main user preferences DataStore.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PreferencesDataStore

/**
 * Qualifier for the favorites (pinned apps) DataStore.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FavoritesDataStore

/**
 * Qualifier for the custom app labels (renames) DataStore.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppLabelsDataStore

/**
 * Qualifier for the custom app folders DataStore.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FoldersDataStore

/**
 * Qualifier for the IO-bound CoroutineDispatcher.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Qualifier for the Main-thread CoroutineDispatcher.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

// Every store gets a ReplaceFileCorruptionHandler. Without one, a truncated or garbled
// preferences_pb (half-written at power loss, mangled by a bad cloud restore) throws
// CorruptionException on EVERY subsequent read and write, forever. The read paths all catch
// IOException (CorruptionException extends it) and emit empty preferences — so the app quietly
// behaved as a fresh install and sent the user back through onboarding — but the WRITE paths
// were unguarded coroutine launches, so the moment the user re-entered their birth date the
// launcher crashed, and would keep crashing on every attempt: a permanently bricked HOME app
// that only a data-clear could fix. Replacing the corrupt file with empty preferences loses that
// one store's settings once, which is exactly what the read path already pretended had happened
// — but now the next write persists instead of killing the process.
private val Context.preferencesDS: DataStore<Preferences> by preferencesDataStore(
    name = "life_calendar_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)
private val Context.favoritesDS: DataStore<Preferences> by preferencesDataStore(
    name = "launcher_favorites",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)
private val Context.appLabelsDS: DataStore<Preferences> by preferencesDataStore(
    name = "app_labels",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)
private val Context.foldersDS: DataStore<Preferences> by preferencesDataStore(
    name = "folders",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

/**
 * Main Hilt module for the Memento application.
 *
 * Provides singleton instances for all core repositories, managers, and system-level
 * dependencies (DataStore, Dispatchers).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Main

    @Provides
    @Singleton
    @PreferencesDataStore
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.preferencesDS

    @Provides
    @Singleton
    @FavoritesDataStore
    fun provideFavoritesDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.favoritesDS

    @Provides
    @Singleton
    @AppLabelsDataStore
    fun provideAppLabelsDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.appLabelsDS

    @Provides
    @Singleton
    @FoldersDataStore
    fun provideFoldersDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.foldersDS

    @Provides
    @Singleton
    fun provideAppRepository(@ApplicationContext context: Context): AppRepository {
        return AppRepository(context)
    }

    @Provides
    @Singleton
    fun provideFavoritesRepository(@FavoritesDataStore dataStore: DataStore<Preferences>): FavoritesRepository {
        return FavoritesRepository(dataStore)
    }

    @Provides
    @Singleton
    fun providePreferencesRepository(@PreferencesDataStore dataStore: DataStore<Preferences>): PreferencesRepository {
        return PreferencesRepository(dataStore)
    }

    @Provides
    @Singleton
    fun provideAppLabelRepository(@AppLabelsDataStore dataStore: DataStore<Preferences>): AppLabelRepository {
        return AppLabelRepository(dataStore)
    }

    @Provides
    @Singleton
    fun provideFolderRepository(@FoldersDataStore dataStore: DataStore<Preferences>): FolderRepository {
        return FolderRepository(dataStore)
    }

    @Provides
    @Singleton
    fun provideBackupManager(
        preferencesRepository: PreferencesRepository,
        favoritesRepository: FavoritesRepository,
        appLabelRepository: AppLabelRepository,
        folderRepository: FolderRepository
    ): BackupManager {
        return BackupManager(preferencesRepository, favoritesRepository, appLabelRepository, folderRepository)
    }

    @Provides
    @Singleton
    fun provideTimeManager(): TimeManager {
        return TimeManager()
    }

    @Provides
    @Singleton
    fun provideWidgetManager(@ApplicationContext context: Context): WidgetManager {
        return WidgetManager(context)
    }

    @Provides
    @Singleton
    fun provideWallpaperUpdater(@ApplicationContext context: Context): WallpaperUpdater {
        return WallpaperUpdater(context)
    }
}
