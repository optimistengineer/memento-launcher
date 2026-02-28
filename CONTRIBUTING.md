# Contributing to Memento

Thanks for considering a contribution. This guide covers setup, conventions, and the PR process.

## Setup

**Requirements:** JDK 17, Android SDK 35, Android Studio Hedgehog+ (or just Gradle from the CLI).

```bash
git clone https://github.com/YOUR_USERNAME/memento.git
cd memento

./gradlew assembleDebug       # Build
./gradlew installDebug        # Install on connected device
./gradlew test                # Unit tests
./gradlew connectedAndroidTest # Instrumented tests (needs device/emulator)
```

## Project Structure

```
app/src/main/java/com/optimistswe/mementolauncher/
├── data/          # Repositories, DataStore persistence
├── domain/        # Pure business logic (life calendar calculations)
├── generator/     # Canvas-based wallpaper image generation
├── ui/
│   ├── screens/   # Full screens (Home, App Drawer, Wallpaper, Settings)
│   ├── components/# Reusable composables (DotText, DotIcon, dialogs, lists)
│   ├── managers/  # TimeManager, WidgetManager
│   └── theme/     # Colors, typography, Material 3 theming
├── worker/        # WorkManager wallpaper refresh
├── service/       # Accessibility service (usage nudge)
├── wallpaper/     # WallpaperManager wrapper
└── di/            # Hilt module (AppModule)
```

| Type of change | Where it goes |
|---|---|
| Calculation / business logic | `domain/` |
| New screen | `ui/screens/` |
| Reusable UI element | `ui/components/` |
| Preferences / storage | `data/` |
| Wallpaper generation | `generator/` |
| Background task | `worker/` |

## Code Style

- **Kotlin:** Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html). Prefer `val` over `var`, immutable data classes, extension functions over utility classes.
- **Compose:** State hoisting to ViewModels. Small, single-purpose composables. Material 3 theming.
- **Tests:** Name tests `functionName_whenCondition_expectedResult`. Use MockK for mocking, Turbine for Flow assertions.
- **KDoc:** Public classes and functions should have doc comments. Skip obvious getters/setters.

## Commits

[Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add dark mode toggle
fix: correct week calculation for leap years
refactor: extract calendar grid drawing logic
test: add WidgetManager screen time tests
docs: update architecture section in README
chore: bump Compose BOM to 2025.01
```

## Pull Requests

1. Fork and create a branch from `main`:
   ```bash
   git checkout -b feat/your-feature
   ```

2. Make your changes. Run `./gradlew test` before pushing.

3. Push and open a PR against `main`. Include:
   - A clear title (< 70 chars)
   - What changed and why in the description
   - Screenshots for any UI changes

4. Address review feedback in follow-up commits (don't force-push during review).

## Reporting Issues

- **Bugs:** Open an [issue](https://github.com/dharmveerjakhar/memento/issues) with steps to reproduce, device info, and Android version.
- **Feature requests:** Open an issue with the `enhancement` label. Describe the use case, not just the solution.
- **Questions:** Use [Discussions](https://github.com/dharmveerjakhar/memento/discussions).

## License

By contributing, you agree that your work will be licensed under the [MIT License](LICENSE).
