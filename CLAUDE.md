# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Memento is a minimalist Android launcher app (HOME replacement) that displays a "life calendar" wallpaper — a dot-matrix grid showing weeks lived vs. remaining. Built with Kotlin, Jetpack Compose, and Material 3.

- **Package**: `com.optimistswe.mementolauncher`
- **Min SDK**: 26 (Android 8.0), **Target SDK**: 35
- **JDK**: 17

## Build & Test Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Install on connected device
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Instrumented tests (requires device/emulator)

# Run a single test class
./gradlew test --tests "com.example.memento.data.PreferencesRepositoryTest"

# Run a single test method
./gradlew test --tests "com.example.memento.data.PreferencesRepositoryTest.testMethodName"
```

## Architecture

MVVM + Clean Architecture with three layers under `app/src/main/java/com/optimistswe/mementolauncher/`:

- **data/** — Repositories backed by DataStore (Preferences). Four separate DataStore instances: preferences, favorites, appLabels, folders. `AppRepository` queries `PackageManager` for installed apps.
- **domain/** — Pure business logic (`LifeCalendarCalculator` for week computations).
- **ui/** — Jetpack Compose screens, components, ViewModels, theme. Two ViewModels: `LauncherViewModel` (launcher state, 561 LOC) and `MainViewModel` (onboarding/setup).
- **generator/** — `CalendarImageGenerator` draws the life-grid wallpaper via Canvas API.
- **worker/** — `WallpaperUpdateWorker` auto-updates wallpaper weekly via WorkManager.
- **service/** — `ShortsBlockerService` accessibility service for blocking short-form content.
- **di/** — Hilt DI module (`AppModule`) providing singleton repositories and managers.

### Two Entry Points

- `MainActivity` — Setup/onboarding flow (3-step: birth date, default launcher, wallpaper).
- `LauncherActivity` — The actual HOME launcher screen with dock, app drawer, and settings panel.

### State Management

StateFlow throughout. Repositories expose `Flow<T>`, ViewModels combine flows with `combine()`, Compose collects via `collectAsState()`.

### Custom Dot-Matrix Icon System

`DotIcon.kt` renders dock icons (Phone, Camera, Messages, Calendar, Maps, Photos) as dot-matrix patterns using Canvas. `DotText.kt` renders text in the same dot style.

## Key Dependencies

- **Compose BOM** 2024.12.01, **Navigation** 2.8.5
- **Hilt** 2.51.1 (DI) with KSP for code generation
- **DataStore** 1.1.1 (persistence)
- **WorkManager** 2.10.0 (background scheduling)
- **Testing**: JUnit 4, MockK 1.13.12, Turbine 1.1.0 (Flow testing), kotlinx-coroutines-test

## Conventions

- **Commit messages**: Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`, `style:`)
- **Test naming**: `functionName_whenCondition_expectedResult`
- **Kotlin style**: Official Kotlin conventions (enforced via `gradle.properties`)
- **Compose**: State hoisting to ViewModels, small reusable composables, Material 3 theming
- **Dependencies**: Managed via version catalog at `gradle/libs.versions.toml`

## Post-Task Workflow

After completing any task (feature, fix, refactor, etc.), always perform these steps:

1. **Install on device**: Run `./gradlew installDebug` to deploy the debug APK to the connected ADB device.
2. **Commit & push to GitHub**: Create a commit with a Conventional Commits message and push to the remote (`git push`).
