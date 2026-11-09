# DevTrack

DevTrack is an offline first Android app for organizing projects, goals, tasks, and personal productivity. It is a portfolio project developed in small milestones.

## Status

Version 0.1.0 includes local project management and a project dashboard. Create, edit, complete, archive, and delete projects, add optional deadlines, and find projects by name, description, or status. The dashboard summarizes saved projects and upcoming deadlines. The interface supports English and Brazilian Portuguese.

## Stack

Kotlin, Jetpack Compose, Material 3, Navigation Compose, Room/SQLite, Coroutines/Flow, Gradle Kotlin DSL, and JUnit.

## Build and run

Open this directory in Android Studio, install Android SDK 35 and JDK 17, sync Gradle, then run the `app` configuration on an Android 8.0 (API 26) or newer device. Run `:app:assembleDebug` for a debug APK and `:app:testDebugUnitTest` for local tests. Instrumented Room tests require an emulator or device.

## Roadmap

Tasks, goals and milestones, expanded dashboard statistics, notes, and local reminders are planned. No account or internet connection is required.

See [architecture](docs/ARCHITECTURE.md) for the initial design.

## License

MIT; see [LICENSE](LICENSE).
