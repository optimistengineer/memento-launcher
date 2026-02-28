package com.optimistswe.mementolauncher.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.optimistswe.mementolauncher.data.*
import com.optimistswe.mementolauncher.ui.managers.TimeManager
import com.optimistswe.mementolauncher.ui.managers.WidgetManager
import com.optimistswe.mementolauncher.wallpaper.WallpaperUpdater
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

private val Context.preferencesDS: DataStore<Preferences> by preferencesDataStore(name = "life_calendar_preferences")
private val Context.favoritesDS: DataStore<Preferences> by preferencesDataStore(name = "launcher_favorites")
private val Context.appLabelsDS: DataStore<Preferences> by preferencesDataStore(name = "app_labels")
private val Context.foldersDS: DataStore<Preferences> by preferencesDataStore(name = "folders")

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
    fun provideTimeManager(): TimeManager {
        return TimeManager()
    }

    @Provides
    fun provideWidgetManager(@ApplicationContext context: Context): WidgetManager {
        return WidgetManager(context)
    }

    @Provides
    @Singleton
    fun provideWallpaperUpdater(@ApplicationContext context: Context): WallpaperUpdater {
        return WallpaperUpdater(context)
    }
}
