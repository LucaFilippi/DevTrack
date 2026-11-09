# DevTrack

DevTrack is an offline first Android app for organizing projects, goals, tasks, and personal productivity. It is a portfolio project developed in small milestones.

## Status

M0 foundation: a dark Material 3 Compose shell, four navigable placeholder screens, English and Brazilian Portuguese resources, and an initial Room project data layer. Project editing and other product features are planned, not yet available.

## Stack

Kotlin, Jetpack Compose, Material 3, Navigation Compose, Room/SQLite, Coroutines/Flow, Gradle Kotlin DSL, and JUnit.

## Build and run

Open this directory in Android Studio, install Android SDK 35 and JDK 17, sync Gradle, then run the `app` configuration on an Android 8.0 (API 26) or newer device. Run `:app:assembleDebug` for a debug APK and `:app:testDebugUnitTest` for local tests. Instrumented Room tests require an emulator or device.

## Roadmap

Project CRUD, tasks, goals and milestones, dashboard statistics, notes, then local reminders. No account or internet connection is required.

See [architecture](docs/ARCHITECTURE.md) for the initial design.

## License

MIT; see [LICENSE](LICENSE).
